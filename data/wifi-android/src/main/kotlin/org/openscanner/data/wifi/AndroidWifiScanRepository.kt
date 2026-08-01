package org.openscanner.data.wifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.openscanner.core.domain.SecurityParser
import org.openscanner.core.domain.WifiChannelMapper
import org.openscanner.core.model.AccessPointObservation
import org.openscanner.core.model.ConnectionEvidence
import org.openscanner.core.model.HIDDEN_NETWORK_SSID
import org.openscanner.core.model.PlatformCapabilities
import org.openscanner.core.model.ScanSnapshot
import org.openscanner.core.model.ScannerPhase
import org.openscanner.core.model.UNAVAILABLE_BSSID
import org.openscanner.core.model.ScannerState
import org.openscanner.core.model.SecurityType
import org.openscanner.core.model.WifiGeneration
import org.openscanner.core.model.WifiRefreshIntervalPolicy

class AndroidWifiScanRepository(
    context: Context,
) : WifiScanRepository {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(WifiManager::class.java)
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
    private val locationManager = appContext.getSystemService(LocationManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sequence = AtomicLong(0L)
    private val snapshotGeneration = AtomicLong(0L)
    private val stateLock = Any()
    private val mutableState = MutableStateFlow(ScannerState())

    override val state: StateFlow<ScannerState> = mutableState.asStateFlow()

    @Volatile
    private var receiverRegistered = false

    @Volatile
    private var paused = false
    private var requestedRefreshIntervalSeconds = WifiRefreshIntervalPolicy.DEFAULT_SECONDS
    private var newestAcceptedSourceTimestampMicros: Long? = null
    private var latestCycleEventId = 0L
    private val cadenceController = ScanCadenceController()
    private val cadenceWakeups = Channel<Unit>(Channel.CONFLATED)
    private var cadenceJob: Job? = null
    private var snapshotJob: Job? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> refreshFromSystem(
                    resultsUpdated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false),
                )
                WifiManager.WIFI_STATE_CHANGED_ACTION -> refreshCapabilityState()
            }
        }
    }

    override fun start() {
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            }
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            receiverRegistered = true
        }
        ensureCadenceLoop()
        refreshCapabilityState()
    }

    override fun stop() {
        val shouldUnregister = synchronized(stateLock) {
            val wasRegistered = receiverRegistered
            receiverRegistered = false
            cadenceController.setActive(false)
            cancelSnapshotWorkLocked()
            mutableState.value = mutableState.value.copy(phase = ScannerPhase.PAUSED)
            wasRegistered
        }
        cadenceWakeups.trySend(Unit)
        cadenceJob?.cancel()
        cadenceJob = null
        if (shouldUnregister) {
            runCatching { appContext.unregisterReceiver(receiver) }
        }
    }

    override fun setPaused(paused: Boolean) {
        if (paused) {
            synchronized(stateLock) {
                this.paused = true
                cadenceController.setActive(false)
                cancelSnapshotWorkLocked()
                mutableState.value = mutableState.value.copy(phase = ScannerPhase.PAUSED)
            }
            cadenceWakeups.trySend(Unit)
        } else {
            synchronized(stateLock) { this.paused = false }
            refreshCapabilityState()
        }
    }

    override fun setRefreshIntervalSeconds(seconds: Int) {
        synchronized(stateLock) {
            requestedRefreshIntervalSeconds = WifiRefreshIntervalPolicy.sanitize(seconds)
            cadenceController.updateConfiguration(
                requestedIntervalSeconds = requestedRefreshIntervalSeconds,
            )
        }
        cadenceWakeups.trySend(Unit)
    }

    override fun refreshCapabilityState() {
        val capabilities = platformCapabilities()
        applyCapabilityState(capabilities, currentBlockedPhase(capabilities))
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    override fun requestScan() {
        val capabilities = platformCapabilities()
        val blockedPhase = currentBlockedPhase(capabilities)
        if (blockedPhase != null) {
            applyCapabilityState(capabilities, blockedPhase)
            return
        }
        if (paused) {
            synchronized(stateLock) {
                cancelSnapshotWorkLocked()
                mutableState.value = mutableState.value.copy(phase = ScannerPhase.PAUSED)
            }
            return
        }

        synchronized(stateLock) {
            if (receiverRegistered && !paused) cadenceController.enqueueManualRequest()
        }
        cadenceWakeups.trySend(Unit)
    }

    private fun currentBlockedPhase(
        capabilities: PlatformCapabilities = platformCapabilities(),
    ): ScannerPhase? = when {
        !capabilities.hasWifiHardware -> ScannerPhase.UNSUPPORTED
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED -> ScannerPhase.PERMISSION_REQUIRED
        wifiManager?.isWifiEnabled != true -> ScannerPhase.WIFI_DISABLED
        !isLocationEnabled() -> ScannerPhase.LOCATION_DISABLED
        else -> null
    }

    private fun platformCapabilities(): PlatformCapabilities {
        val hasWifi = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI) &&
            wifiManager != null
        return PlatformCapabilities(
            hasWifiHardware = hasWifi,
            supports5Ghz = hasWifi && runCatching { wifiManager?.is5GHzBandSupported == true }.getOrDefault(false),
            supports6Ghz = hasWifi && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                runCatching { wifiManager?.is6GHzBandSupported == true }.getOrDefault(false),
            wifiScanThrottleEnabled = if (hasWifi) readWifiScanThrottleEnabled() else null,
            wifiScanThrottleStateResolved = true,
        )
    }

    /**
     * Reads the persisted switch shown by Android's Developer Options UI.
     *
     * Android 11 added the public WifiManager query. Android 10 already had
     * the switch, but exposed its readable Settings.Global key only as a
     * hidden constant, so that one release uses the stable AOSP key directly.
     */
    @SuppressLint("MissingPermission")
    private fun readWifiScanThrottleEnabled(): Boolean? {
        val manager = wifiManager ?: return null
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                runCatching { manager.isScanThrottleEnabled }.getOrNull()
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q ->
                runCatching {
                    Settings.Global.getInt(
                        appContext.contentResolver,
                        WIFI_SCAN_THROTTLE_ENABLED_SETTING,
                        1,
                    ) != 0
                }.getOrNull()
            else -> null
        }
    }

    @Suppress("DEPRECATION")
    private fun isLocationEnabled(): Boolean {
        val manager = locationManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.isLocationEnabled
        } else {
            runCatching {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }.getOrDefault(false)
        }
    }

    private fun ensureCadenceLoop() {
        synchronized(stateLock) {
            if (cadenceJob?.isActive == true) return
            cadenceJob = scope.launch { runCadenceLoop() }
        }
    }

    private suspend fun runCadenceLoop() {
        while (currentCoroutineContext().isActive) {
            val nowElapsed = SystemClock.elapsedRealtime()
            val wakeAtElapsed = synchronized(stateLock) {
                cadenceController.nextWakeElapsedMs(nowElapsed)
            }
            if (wakeAtElapsed == null) {
                cadenceWakeups.receive()
                continue
            }

            val waitMs = (wakeAtElapsed - nowElapsed).coerceAtLeast(0L)
            if (waitMs > 0L) {
                withTimeoutOrNull(waitMs) { cadenceWakeups.receive() }
            }
            handleCadenceWake()
        }
    }

    private fun handleCadenceWake() {
        val timeoutEventId = synchronized(stateLock) {
            if (cadenceController.consumeTimeoutIfDue(SystemClock.elapsedRealtime())) {
                nextCycleEventIdLocked()
            } else {
                null
            }
        }
        if (timeoutEventId != null) {
            publishNonFreshCycle(
                eventId = timeoutEventId,
                requestAccepted = true,
                resultsUpdated = false,
                safeErrorCode = null,
            )
        }

        val shouldRequest = synchronized(stateLock) {
            cadenceController.beginRequestIfDue(SystemClock.elapsedRealtime())
        }
        if (shouldRequest) executeScanRequest()
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun executeScanRequest() {
        val capabilities = platformCapabilities()
        val blockedPhase = currentBlockedPhase(capabilities)
        applyCapabilityState(capabilities, blockedPhase)
        if (blockedPhase != null) {
            synchronized(stateLock) { cadenceController.markRequestRejected() }
            return
        }

        var rejectionEventId: Long? = null
        val attempt = runWhenScannerActive(
            lock = stateLock,
            isActive = {
                receiverRegistered && !paused && cadenceController.isActive &&
                    cadenceController.isRequestInFlight
            },
        ) {
            runCatching { wifiManager?.startScan() ?: false }.also { outcome ->
                if (outcome.getOrNull() != true) {
                    cadenceController.markRequestRejected()
                    rejectionEventId = nextCycleEventIdLocked()
                }
            }
        } ?: return

        val accepted = attempt.getOrElse {
            publishNonFreshCycle(
                eventId = checkNotNull(rejectionEventId),
                requestAccepted = false,
                resultsUpdated = null,
                safeErrorCode = "SCAN_REQUEST_FAILED",
            )
            cadenceWakeups.trySend(Unit)
            return
        }
        if (!accepted) {
            publishNonFreshCycle(
                eventId = checkNotNull(rejectionEventId),
                requestAccepted = false,
                resultsUpdated = null,
                safeErrorCode = null,
            )
            cadenceWakeups.trySend(Unit)
        }
    }

    private fun applyCapabilityState(
        capabilities: PlatformCapabilities,
        blockedPhase: ScannerPhase?,
    ) {
        synchronized(stateLock) {
            cadenceController.updateConfiguration(
                requestedIntervalSeconds = requestedRefreshIntervalSeconds,
                wifiScanThrottleEnabled = capabilities.wifiScanThrottleEnabled,
            )
            cadenceController.setActive(receiverRegistered && !paused && blockedPhase == null)
            if (!receiverRegistered || blockedPhase != null || paused) cancelSnapshotWorkLocked()

            val previous = mutableState.value
            mutableState.value = previous.copy(
                phase = scannerPhaseForState(
                    receiverRegistered = receiverRegistered,
                    paused = paused,
                    blockedPhase = blockedPhase,
                    hasSnapshot = previous.snapshot != null,
                ),
                capabilities = capabilities,
                safeErrorCode = null,
            )
        }
        cadenceWakeups.trySend(Unit)
    }

    @SuppressLint("MissingPermission")
    private fun refreshFromSystem(
        resultsUpdated: Boolean,
    ) {
        val resultWork = synchronized(stateLock) {
            cancelSnapshotWorkLocked()
            val completedRequest = cadenceController.markScanResultsAvailable()
            ScanResultWork(
                requestAccepted = true.takeIf { completedRequest },
                generation = snapshotGeneration.get(),
                eventId = nextCycleEventIdLocked(),
            )
        }
        val capabilities = platformCapabilities()
        val blockedPhase = currentBlockedPhase(capabilities)
        applyCapabilityState(capabilities, blockedPhase)
        val canProcess = synchronized(stateLock) {
            resultWork.generation == snapshotGeneration.get() && receiverRegistered && !paused &&
                blockedPhase == null && cadenceController.isResultProcessing
        }
        if (!canProcess) return

        val job = scope.launch {
            try {
                val scanResults = runCatching { wifiManager?.scanResults.orEmpty() }
                    .getOrElse {
                        publishNonFreshCycle(
                            eventId = resultWork.eventId,
                            requestAccepted = resultWork.requestAccepted,
                            resultsUpdated = false,
                            safeErrorCode = "SCAN_RESULTS_UNAVAILABLE",
                        )
                        return@launch
                    }
                val connection = readConnectionEvidence()
                val observations = scanResults
                    .map { it.toObservation(connection.bssid) }
                    .sortedWith(
                        compareByDescending<AccessPointObservation> { it.isConnected }
                            .thenByDescending { it.rssiDbm }
                            .thenBy { it.ssid.lowercase() },
                    )
                synchronized(stateLock) {
                    if (
                        resultWork.generation != snapshotGeneration.get() ||
                        resultWork.eventId != latestCycleEventId ||
                        !receiverRegistered ||
                        paused
                    ) {
                        return@launch
                    }
                    val current = mutableState.value
                    val previousSnapshot = current.snapshot
                    val fresh = hasFreshScanEvidence(
                        previous = previousSnapshot,
                        observations = observations,
                        resultsUpdated = resultsUpdated,
                        newestAcceptedSourceTimestampMicros = newestAcceptedSourceTimestampMicros,
                    )
                    val snapshot = if (!fresh && previousSnapshot != null) {
                        refreshNonFreshSnapshot(
                            previous = previousSnapshot,
                            connection = connection,
                            requestAccepted = resultWork.requestAccepted,
                            resultsUpdated = resultsUpdated,
                            likelyThrottled = true,
                        )
                    } else if (fresh) {
                        val nowElapsed = SystemClock.elapsedRealtime()
                        val newestSourceTimestampMicros = observations
                            .maxOfOrNull { it.timestampMicros }
                            ?.takeIf { it > 0L }
                        if (newestSourceTimestampMicros != null) {
                            newestAcceptedSourceTimestampMicros = newestSourceTimestampMicros
                        }
                        ScanSnapshot(
                            sequenceId = sequence.incrementAndGet(),
                            capturedAtEpochMs = System.currentTimeMillis(),
                            capturedAtElapsedMs = nowElapsed,
                            sourceTimestampMicros = newestSourceTimestampMicros,
                            requestAccepted = resultWork.requestAccepted,
                            resultsUpdated = resultsUpdated,
                            likelyThrottled = false,
                            observations = observations,
                            connection = connection,
                        )
                    } else {
                        null
                    }
                    mutableState.value = current.copy(
                        phase = ScannerPhase.LIVE,
                        snapshot = snapshot,
                        safeErrorCode = null,
                    )
                }
            } finally {
                val committedCurrentGeneration = synchronized(stateLock) {
                    if (resultWork.generation != snapshotGeneration.get()) return@synchronized false
                    cadenceController.markResultProcessingCompleted()
                    snapshotJob = null
                    true
                }
                if (committedCurrentGeneration) cadenceWakeups.trySend(Unit)
            }
        }
        synchronized(stateLock) {
            if (
                resultWork.generation == snapshotGeneration.get() &&
                resultWork.eventId == latestCycleEventId &&
                receiverRegistered &&
                !paused
            ) {
                snapshotJob = job
            } else {
                job.cancel()
            }
        }
    }

    private fun publishNonFreshCycle(
        eventId: Long,
        requestAccepted: Boolean?,
        resultsUpdated: Boolean?,
        safeErrorCode: String?,
    ) {
        val (generation, previous) = synchronized(stateLock) {
            if (eventId != latestCycleEventId) return
            snapshotGeneration.get() to mutableState.value.snapshot
        }
        val connection = previous?.let {
            runCatching { readConnectionEvidence() }.getOrDefault(it.connection)
        }
        synchronized(stateLock) {
            if (
                eventId != latestCycleEventId ||
                generation != snapshotGeneration.get() ||
                !receiverRegistered ||
                paused
            ) {
                return
            }
            val current = mutableState.value
            val previousSnapshot = current.snapshot
            val refreshedSnapshot = if (previousSnapshot != null && connection != null) {
                refreshNonFreshSnapshot(
                    previous = previousSnapshot,
                    connection = connection,
                    requestAccepted = requestAccepted,
                    resultsUpdated = resultsUpdated,
                    likelyThrottled = true,
                )
            } else {
                previousSnapshot
            }
            mutableState.value = current.copy(
                phase = when {
                    safeErrorCode != null -> ScannerPhase.ERROR
                    refreshedSnapshot != null -> ScannerPhase.LIVE
                    else -> current.phase
                },
                snapshot = refreshedSnapshot,
                safeErrorCode = safeErrorCode,
            )
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun readConnectionEvidence(): ConnectionEvidence {
        val manager = connectivityManager
        val legacyWifiInfo = wifiManager?.connectionInfo
        val reportedPrimaryBssid = normalizeWifiBssid(legacyWifiInfo?.bssid)
        val activeNetwork = manager.activeNetwork
        val activeCapabilities = activeNetwork?.let(manager::getNetworkCapabilities)
        val activeIsPhysicalWifi = activeCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true &&
            activeCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == false
        val wifiNetwork = if (activeIsPhysicalWifi) {
            activeNetwork
        } else {
            val candidates = manager.allNetworks.mapNotNull { network ->
                val capabilities = manager.getNetworkCapabilities(network)
                if (
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true &&
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == false
                ) {
                    WifiNetworkCandidate(network, capabilities)
                } else {
                    null
                }
            }
            val candidateBssids = candidates.map { candidate ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    normalizeWifiBssid((candidate.capabilities.transportInfo as? WifiInfo)?.bssid)
                } else {
                    null
                }
            }
            choosePhysicalWifiCandidateIndex(candidateBssids, reportedPrimaryBssid)
                ?.let { candidates[it].network }
        }
        val networkCapabilities = wifiNetwork?.let(manager::getNetworkCapabilities)
        val linkProperties = wifiNetwork?.let(manager::getLinkProperties)
        val isWifi = wifiNetwork != null &&
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true &&
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == false
        val wifiInfo = if (!isWifi) {
            null
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (networkCapabilities.transportInfo as? WifiInfo) ?: legacyWifiInfo
        } else {
            legacyWifiInfo
        }
        // Synchronous NetworkCapabilities intentionally redacts location-sensitive
        // WifiInfo fields. Prefer it for connection metrics, but recover identifiers
        // from the permission-gated legacy result when those fields are redacted.
        val bssid = selectConnectionBssid(
            transportBssid = wifiInfo?.bssid,
            legacyBssid = reportedPrimaryBssid,
        )
        val ssid = selectConnectionSsid(
            transportSsid = wifiInfo?.ssid,
            legacySsid = legacyWifiInfo?.ssid,
        )
        val defaultGateway = linkProperties?.routes
            ?.firstOrNull { it.isDefaultRoute }
            ?.gateway
            ?.hostAddress
        val ipAddress = linkProperties?.linkAddresses
            ?.firstOrNull { it.address.address.size == 4 }
            ?.address
            ?.hostAddress

        return ConnectionEvidence(
            connected = isWifi,
            bssid = bssid,
            ssid = ssid,
            validated = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            captivePortal = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL),
            linkSpeedMbps = wifiInfo?.linkSpeed?.takeIf { it >= 0 },
            rxLinkSpeedMbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiInfo?.rxLinkSpeedMbps?.takeIf { it >= 0 }
            } else {
                null
            },
            txLinkSpeedMbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiInfo?.txLinkSpeedMbps?.takeIf { it >= 0 }
            } else {
                null
            },
            ipAddress = ipAddress,
            gateway = defaultGateway,
            dnsServers = linkProperties?.dnsServers.orEmpty().mapNotNull { it.hostAddress },
        )
    }

    private data class WifiNetworkCandidate(
        val network: Network,
        val capabilities: NetworkCapabilities,
    )

    private data class ScanResultWork(
        val requestAccepted: Boolean?,
        val generation: Long,
        val eventId: Long,
    )

    private fun nextCycleEventIdLocked(): Long {
        latestCycleEventId += 1L
        return latestCycleEventId
    }

    private fun cancelSnapshotWorkLocked() {
        snapshotGeneration.incrementAndGet()
        snapshotJob?.cancel()
        snapshotJob = null
    }

    @Suppress("DEPRECATION")
    private fun ScanResult.toObservation(connectedBssid: String?): AccessPointObservation {
        val normalizedBssid = BSSID.orEmpty().lowercase()
        val securityTypes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mapStructuredSecurity(securityTypes)
        } else {
            emptySet()
        }
        return AccessPointObservation(
            id = normalizedBssid.ifBlank { "unknown-${frequency}-${timestamp}" },
            ssid = SSID.takeUnless { it.isNullOrBlank() } ?: HIDDEN_NETWORK_SSID,
            bssid = normalizedBssid.ifBlank { UNAVAILABLE_BSSID },
            channel = WifiChannelMapper.fromFrequency(frequency),
            channelWidthMhz = mapPlatformChannelWidth(channelWidth),
            footprintCenterFrequencyMhz = centerFreq0.takeIf { it > 0 } ?: frequency,
            rssiDbm = level,
            security = securityTypes.ifEmpty { SecurityParser.fromCapabilities(capabilities) },
            generation = mapWifiGeneration(),
            timestampMicros = timestamp,
            isConnected = connectedBssid?.equals(BSSID, ignoreCase = true) == true,
        )
    }

    private fun mapStructuredSecurity(values: IntArray): Set<SecurityType> = values.mapTo(linkedSetOf()) {
        when (it) {
            WifiInfo.SECURITY_TYPE_OPEN -> SecurityType.OPEN
            WifiInfo.SECURITY_TYPE_OWE -> SecurityType.OWE
            WifiInfo.SECURITY_TYPE_WEP -> SecurityType.WEP
            WifiInfo.SECURITY_TYPE_PSK -> SecurityType.WPA2_PERSONAL
            WifiInfo.SECURITY_TYPE_SAE -> SecurityType.WPA3_PERSONAL
            WifiInfo.SECURITY_TYPE_EAP -> SecurityType.ENTERPRISE
            WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE,
            WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT -> SecurityType.WPA3_ENTERPRISE
            WifiInfo.SECURITY_TYPE_WAPI_PSK,
            WifiInfo.SECURITY_TYPE_WAPI_CERT -> SecurityType.WAPI
            else -> SecurityType.UNKNOWN
        }
    }

    private fun ScanResult.mapWifiGeneration(): WifiGeneration {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return WifiGeneration.UNKNOWN
        return when (wifiStandard) {
            ScanResult.WIFI_STANDARD_LEGACY -> WifiGeneration.LEGACY
            ScanResult.WIFI_STANDARD_11N -> WifiGeneration.WIFI_4
            ScanResult.WIFI_STANDARD_11AC -> WifiGeneration.WIFI_5
            ScanResult.WIFI_STANDARD_11AX ->
                if (frequency >= 5925) WifiGeneration.WIFI_6E else WifiGeneration.WIFI_6
            ScanResult.WIFI_STANDARD_11BE -> WifiGeneration.WIFI_7
            else -> WifiGeneration.UNKNOWN
        }
    }

}

internal fun mapPlatformChannelWidth(value: Int): Int? = when (value) {
    ScanResult.CHANNEL_WIDTH_20MHZ -> 20
    ScanResult.CHANNEL_WIDTH_40MHZ -> 40
    ScanResult.CHANNEL_WIDTH_80MHZ -> 80
    ScanResult.CHANNEL_WIDTH_160MHZ -> 160
    ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> null
    ScanResult.CHANNEL_WIDTH_320MHZ -> 320
    else -> null
}

internal fun <T> runWhenScannerActive(
    lock: Any,
    isActive: () -> Boolean,
    action: () -> T,
): T? = synchronized(lock) {
    if (isActive()) action() else null
}

internal fun refreshNonFreshSnapshot(
    previous: ScanSnapshot,
    connection: ConnectionEvidence,
    requestAccepted: Boolean?,
    resultsUpdated: Boolean?,
    likelyThrottled: Boolean,
): ScanSnapshot = previous.copy(
    requestAccepted = requestAccepted,
    resultsUpdated = resultsUpdated,
    likelyThrottled = likelyThrottled,
    observations = previous.observations.withConnectionMarker(connection.bssid),
    connection = connection,
)

internal fun scannerPhaseForState(
    receiverRegistered: Boolean,
    paused: Boolean,
    blockedPhase: ScannerPhase?,
    hasSnapshot: Boolean,
): ScannerPhase = when {
    !receiverRegistered || paused -> ScannerPhase.PAUSED
    blockedPhase != null -> blockedPhase
    hasSnapshot -> ScannerPhase.LIVE
    else -> ScannerPhase.CHECKING
}

internal fun hasFreshScanEvidence(
    previous: ScanSnapshot?,
    observations: List<AccessPointObservation>,
    resultsUpdated: Boolean,
    newestAcceptedSourceTimestampMicros: Long? = previous?.sourceTimestampMicros,
): Boolean {
    if (!resultsUpdated) return false
    if (observations.isEmpty()) {
        return previous == null || previous.observations.isNotEmpty()
    }
    val newestTimestamp = observations.maxOfOrNull { it.timestampMicros }?.takeIf { it > 0L }
        ?: return false
    return newestAcceptedSourceTimestampMicros == null ||
        newestTimestamp > newestAcceptedSourceTimestampMicros
}

private fun List<AccessPointObservation>.withConnectionMarker(
    connectedBssid: String?,
): List<AccessPointObservation> = map { observation ->
    observation.copy(
        isConnected = connectedBssid?.equals(observation.bssid, ignoreCase = true) == true,
    )
}

internal fun choosePhysicalWifiCandidateIndex(
    candidateBssids: List<String?>,
    reportedPrimaryBssid: String?,
): Int? {
    val normalizedPrimary = normalizeWifiBssid(reportedPrimaryBssid)
    val matching = if (normalizedPrimary == null) {
        emptyList()
    } else {
        candidateBssids.mapIndexedNotNull { index, bssid ->
            index.takeIf { normalizeWifiBssid(bssid) == normalizedPrimary }
        }
    }
    return when {
        matching.size == 1 -> matching.single()
        candidateBssids.size == 1 -> 0
        else -> null
    }
}

internal fun normalizeWifiBssid(value: String?): String? = value
    ?.lowercase()
    ?.takeUnless { it == "02:00:00:00:00:00" || it.isBlank() }

internal fun selectConnectionBssid(
    transportBssid: String?,
    legacyBssid: String?,
): String? = normalizeWifiBssid(transportBssid) ?: normalizeWifiBssid(legacyBssid)

internal fun selectConnectionSsid(
    transportSsid: String?,
    legacySsid: String?,
): String? = normalizeWifiSsid(transportSsid) ?: normalizeWifiSsid(legacySsid)

private fun normalizeWifiSsid(value: String?): String? = value
    ?.removeSurrounding("\"")
    ?.takeUnless { it == WifiManager.UNKNOWN_SSID || it.isBlank() }

private const val WIFI_SCAN_THROTTLE_ENABLED_SETTING = "wifi_scan_throttle_enabled"
