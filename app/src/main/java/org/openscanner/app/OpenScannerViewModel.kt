package org.openscanner.app

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.security.MessageDigest
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.openscanner.core.domain.AccessPointStabilityAnalyzer
import org.openscanner.core.domain.AccessPointStabilityLevel
import org.openscanner.core.domain.FreshnessPolicy
import org.openscanner.core.domain.InterferenceAnalyzer
import org.openscanner.core.domain.NeighborhoodPostureAnalyzer
import org.openscanner.core.domain.SignalHistoryPolicy
import org.openscanner.core.domain.WifiChannelMapper
import org.openscanner.core.export.ExportDocument
import org.openscanner.core.export.ExportFormat
import org.openscanner.core.export.SnapshotExporter
import org.openscanner.core.export.WifiLogExporter
import org.openscanner.core.export.WifiLogField
import org.openscanner.core.export.WifiLogFormat
import org.openscanner.core.export.WifiLogRecordResult
import org.openscanner.core.export.WifiLogRecorder
import org.openscanner.core.export.WifiLogSession
import org.openscanner.core.model.AccessPointObservation
import org.openscanner.core.model.AppPreferences
import org.openscanner.core.model.ScannerState
import org.openscanner.core.model.SignalSample
import org.openscanner.core.model.UNAVAILABLE_BSSID
import org.openscanner.core.model.WifiBand
import org.openscanner.core.model.WifiChannelGroup
import org.openscanner.core.model.WifiGeneration
import org.openscanner.core.model.WifiRefreshIntervalPolicy
import org.openscanner.core.privacy.PrivacyRedactor
import org.openscanner.data.settings.SettingsRepository
import org.openscanner.data.wifi.WifiScanRepository

class OpenScannerViewModel(
    private val wifiRepository: WifiScanRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val activeTab = MutableStateFlow(AppTab.TRACK)
    private val selectedChannelGroup = MutableStateFlow(WifiChannelGroup.GHZ_5_2)
    private val selectedRawId = MutableStateFlow<String?>(null)
    private val historyState = MutableStateFlow(HistoryState())
    private val elapsedRealtime = MutableStateFlow(SystemClock.elapsedRealtime())
    private val selectedLogFields = MutableStateFlow(WifiLogField.entries.toSet())
    private val logSession = MutableStateFlow<WifiLogSession?>(null)
    private val loggingActive = MutableStateFlow(false)
    private val loggingStartedAtElapsedMs = MutableStateFlow<Long?>(null)
    private val opaqueIds = mutableMapOf<String, String>()
    private var logRecorder: WifiLogRecorder? = null
    private var nextLoggingSampleAtElapsedMs: Long? = null
    private var currentRefreshIntervalSeconds = 30

    private companion object {
        const val HISTORY_RETENTION_MS = 30 * 60_000L
        const val MAX_HISTORY_POINTS_PER_NETWORK = 60
        const val MAX_HISTORY_NETWORKS = 128
        const val MAX_PRESENCE_FRAMES = 60
    }

    private data class ObservationFrame(
        val sequenceId: Long,
        val rssiByNetworkId: Map<String, Int>,
    )

    private data class HistoryState(
        val signals: Map<String, List<SignalSample>> = emptyMap(),
        val frames: List<ObservationFrame> = emptyList(),
    )

    private data class Controls(
        val tab: AppTab,
        val channelGroup: WifiChannelGroup,
        val selectedRawId: String?,
        val history: HistoryState,
        val logging: LoggingControls,
    )

    private data class LoggingControls(
        val selectedFields: Set<WifiLogField>,
        val session: WifiLogSession?,
        val active: Boolean,
        val startedAtElapsedMs: Long?,
    )

    private val loggingControls = combine(
        selectedLogFields,
        logSession,
        loggingActive,
        loggingStartedAtElapsedMs,
    ) { fields, session, active, startedAt -> LoggingControls(fields, session, active, startedAt) }

    private val controls = combine(
        activeTab,
        selectedChannelGroup,
        selectedRawId,
        historyState,
        loggingControls,
    ) { tab, channelGroup, rawId, history, logging ->
        Controls(tab, channelGroup, rawId, history, logging)
    }

    val uiState = combine(
        wifiRepository.state,
        settingsRepository.preferences,
        controls,
        elapsedRealtime,
    ) { scannerState, preferences, currentControls, nowElapsed ->
        scannerState.toUiState(preferences, currentControls, nowElapsed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = OpenScannerUiState(),
    )

    init {
        viewModelScope.launch {
            wifiRepository.state
                .mapNotNull { it.snapshot }
                .distinctUntilChangedBy { it.sequenceId }
                .collect { snapshot ->
                    val samples = snapshot.observations.associate { observation ->
                        val sourceElapsedMs = observation.timestampMicros
                            .takeIf { it > 0L }
                            ?.div(1_000L)
                            ?: snapshot.capturedAtElapsedMs
                        observation.id to SignalSample(sourceElapsedMs, observation.rssiDbm)
                    }
                    val currentHistory = historyState.value
                    val updatedSignals = SignalHistoryPolicy.update(
                        existing = currentHistory.signals,
                        incoming = samples,
                        nowElapsedMs = snapshot.capturedAtElapsedMs,
                        retentionMs = HISTORY_RETENTION_MS,
                        maxPointsPerNetwork = MAX_HISTORY_POINTS_PER_NETWORK,
                        maxNetworks = MAX_HISTORY_NETWORKS,
                    )
                    val frame = ObservationFrame(
                        sequenceId = snapshot.sequenceId,
                        rssiByNetworkId = snapshot.observations.associate { it.id to it.rssiDbm },
                    )
                    historyState.value = HistoryState(
                        signals = updatedSignals,
                        frames = (currentHistory.frames.filterNot { it.sequenceId == snapshot.sequenceId } + frame)
                            .takeLast(MAX_PRESENCE_FRAMES),
                    )

                    val currentSelected = selectedRawId.value
                    if (currentSelected == null || snapshot.observations.none { it.id == currentSelected }) {
                        val replacement = if (currentSelected == null) {
                            snapshot.observations.firstOrNull { it.isConnected }
                                ?: snapshot.observations.firstOrNull {
                                    WifiChannelMapper.group(it.channel) == selectedChannelGroup.value
                                }
                                ?: snapshot.observations.firstOrNull()
                        } else {
                            snapshot.observations.firstOrNull {
                                WifiChannelMapper.group(it.channel) == selectedChannelGroup.value
                            }
                                ?: snapshot.observations.firstOrNull { it.isConnected }
                                ?: snapshot.observations.firstOrNull()
                        }
                        selectedRawId.value = replacement?.id
                        replacement?.channel?.let(WifiChannelMapper::group)
                            ?.let { selectedChannelGroup.value = it }
                    }
                }
        }
        viewModelScope.launch {
            wifiRepository.state.collect { scannerState ->
                if (!loggingActive.value) return@collect
                recordWifiLog(scannerState, SystemClock.elapsedRealtime())
            }
        }
        viewModelScope.launch {
            settingsRepository.preferences.collect { preferences ->
                currentRefreshIntervalSeconds = preferences.refreshIntervalSeconds
                if (loggingActive.value) {
                    nextLoggingSampleAtElapsedMs = SystemClock.elapsedRealtime() +
                        preferences.refreshIntervalSeconds * 1_000L
                }
                wifiRepository.setRefreshIntervalSeconds(preferences.refreshIntervalSeconds)
            }
        }
        viewModelScope.launch {
            combine(settingsRepository.preferences, wifiRepository.state) { preferences, scannerState ->
                preferences.refreshIntervalSeconds to scannerState
            }
                .distinctUntilChangedBy { (refreshIntervalSeconds, scannerState) ->
                    Triple(
                        refreshIntervalSeconds,
                        scannerState.capabilities.wifiScanThrottleEnabled,
                        scannerState.capabilities.wifiScanThrottleStateResolved,
                    )
                }
                .collect { (refreshIntervalSeconds, scannerState) ->
                    if (
                        RefreshIntervalCapabilityPolicy.shouldResetSavedFastInterval(
                            refreshIntervalSeconds = refreshIntervalSeconds,
                            scannerState = scannerState,
                        )
                    ) {
                        settingsRepository.setRefreshIntervalSeconds(
                            WifiRefreshIntervalPolicy.DEFAULT_SECONDS,
                        )
                    }
                }
        }
        viewModelScope.launch {
            while (isActive) {
                val nowElapsed = SystemClock.elapsedRealtime()
                val currentHistory = historyState.value
                historyState.value = currentHistory.copy(
                    signals = SignalHistoryPolicy.update(
                        existing = currentHistory.signals,
                        incoming = emptyMap(),
                        nowElapsedMs = nowElapsed,
                        retentionMs = HISTORY_RETENTION_MS,
                        maxPointsPerNetwork = MAX_HISTORY_POINTS_PER_NETWORK,
                        maxNetworks = MAX_HISTORY_NETWORKS,
                    ),
                )
                val nextLoggingSample = nextLoggingSampleAtElapsedMs
                if (
                    loggingActive.value &&
                    wifiRepository.state.value.phase == org.openscanner.core.model.ScannerPhase.LIVE &&
                    nextLoggingSample != null &&
                    nowElapsed >= nextLoggingSample
                ) {
                    recordWifiLog(wifiRepository.state.value, nowElapsed, force = true)
                }
                elapsedRealtime.value = nowElapsed
                delay(1_000L)
            }
        }
    }

    fun selectTab(tab: AppTab) {
        activeTab.value = tab
    }

    fun selectChannelGroup(group: WifiChannelGroup) {
        val allowed = uiState.value.channelGroups.firstOrNull { it.group == group }?.enabled == true
        if (allowed) {
            selectedChannelGroup.value = group
            val observations = wifiRepository.state.value.snapshot?.observations.orEmpty()
            val selectedInGroup = observations.any {
                it.id == selectedRawId.value && WifiChannelMapper.group(it.channel) == group
            }
            if (!selectedInGroup) {
                selectedRawId.value = observations
                    .asSequence()
                    .filter { WifiChannelMapper.group(it.channel) == group }
                    .sortedWith(compareByDescending<AccessPointObservation> { it.isConnected }.thenByDescending { it.rssiDbm })
                    .firstOrNull()
                    ?.id
            }
        }
    }

    fun selectNetwork(uiId: String, openTracker: Boolean = false) {
        val rawId = wifiRepository.state.value.snapshot?.observations
            ?.firstOrNull { opaqueId(it.id) == uiId }
            ?.id
            ?: return
        selectedRawId.value = rawId
        val selected = wifiRepository.state.value.snapshot?.observations?.firstOrNull { it.id == rawId }
        selected?.channel?.let(WifiChannelMapper::group)?.let { selectedChannelGroup.value = it }
        if (openTracker) activeTab.value = AppTab.TRACK
    }

    fun requestScan() = wifiRepository.requestScan()

    fun refreshAfterPermissionResult() {
        wifiRepository.refreshCapabilityState()
        wifiRepository.requestScan()
    }

    fun setPaused(paused: Boolean) = wifiRepository.setPaused(paused)

    fun setPrivacyMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPrivacyMode(enabled) }
    }

    fun setRedactExports(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRedactExports(enabled) }
    }

    fun setRefreshIntervalSeconds(seconds: Int) {
        val effectiveSeconds = WifiRefreshIntervalPolicy.effectiveSeconds(
            seconds = seconds,
            wifiScanThrottleEnabled = wifiRepository.state.value.capabilities.wifiScanThrottleEnabled,
        )
        viewModelScope.launch { settingsRepository.setRefreshIntervalSeconds(effectiveSeconds) }
    }

    fun resetSettings() {
        viewModelScope.launch { settingsRepository.reset() }
    }

    fun setWifiLogFieldEnabled(field: WifiLogField, enabled: Boolean) {
        if (loggingActive.value) return
        selectedLogFields.value = selectedLogFields.value.toMutableSet().apply {
            if (enabled) add(field) else remove(field)
        }.toSet()
    }

    fun setAllWifiLogFields(enabled: Boolean) {
        if (loggingActive.value) return
        selectedLogFields.value = if (enabled) WifiLogField.entries.toSet() else emptySet()
    }

    fun startWifiLogging() {
        if (loggingActive.value || selectedLogFields.value.isEmpty()) return
        val scannerState = wifiRepository.state.value
        if (scannerState.snapshot == null) return
        val nowElapsed = SystemClock.elapsedRealtime()
        val recorder = WifiLogRecorder(
            selectedFields = selectedLogFields.value,
            startedAtEpochMs = System.currentTimeMillis(),
            startedAtElapsedMs = nowElapsed,
            redacted = uiState.value.redactExports,
        )
        logRecorder = recorder
        loggingStartedAtElapsedMs.value = nowElapsed
        nextLoggingSampleAtElapsedMs = nowElapsed + currentRefreshIntervalSeconds * 1_000L
        loggingActive.value = true
        recorder.record(scannerState, nowElapsed)
        logSession.value = recorder.snapshot()
    }

    fun stopWifiLogging() {
        if (!loggingActive.value) return
        logRecorder?.stop(SystemClock.elapsedRealtime())
        loggingActive.value = false
        nextLoggingSampleAtElapsedMs = null
        logSession.value = logRecorder?.snapshot()
    }

    fun clearWifiLog() {
        if (loggingActive.value) return
        logRecorder = null
        logSession.value = null
        loggingStartedAtElapsedMs.value = null
        nextLoggingSampleAtElapsedMs = null
    }

    fun buildSnapshotExport(format: ExportFormat): ExportDocument? =
        wifiRepository.state.value.snapshot?.let {
            SnapshotExporter.exportDocument(it, format, redacted = uiState.value.redactExports)
        }

    fun buildWifiLogExport(format: WifiLogFormat): ExportDocument? =
        logSession.value?.takeIf { it.records.isNotEmpty() && !loggingActive.value }
            ?.let { WifiLogExporter.export(it, format) }

    private fun recordWifiLog(
        scannerState: ScannerState,
        recordedAtElapsedMs: Long,
        force: Boolean = false,
    ) {
        val recorder = logRecorder ?: return
        when (recorder.record(scannerState, recordedAtElapsedMs, force)) {
            WifiLogRecordResult.ADDED -> {
                logSession.value = recorder.snapshot()
                nextLoggingSampleAtElapsedMs = recordedAtElapsedMs + currentRefreshIntervalSeconds * 1_000L
            }
            WifiLogRecordResult.LIMIT_REACHED -> {
                loggingActive.value = false
                nextLoggingSampleAtElapsedMs = null
                logSession.value = recorder.snapshot()
            }
            WifiLogRecordResult.DUPLICATE,
            WifiLogRecordResult.STOPPED -> Unit
        }
    }

    private fun ScannerState.toUiState(
        preferences: AppPreferences,
        controls: Controls,
        nowElapsed: Long,
    ): OpenScannerUiState {
        val observations = snapshot?.observations.orEmpty()
        val aliasNumbers = observations.sortedBy { it.id }
            .mapIndexed { index, observation -> observation.id to index + 1 }
            .toMap()
        val selected = observations.firstOrNull { it.id == controls.selectedRawId }
            ?: observations.firstOrNull { it.isConnected }
            ?: observations.firstOrNull()
        val uiNetworks = observations.map { observation ->
            observation.toUiModel(
                selected = observation.id == selected?.id,
                privacyMode = preferences.privacyMode,
                aliasNumber = aliasNumbers.getValue(observation.id),
            )
        }
        val age = snapshot?.let { currentSnapshot ->
            val sourceElapsed = currentSnapshot.sourceTimestampMicros?.div(1_000L)
            (nowElapsed - (sourceElapsed ?: currentSnapshot.capturedAtElapsedMs)).coerceAtLeast(0L)
        }
        val connectionEvidence = snapshot?.connection
        val displayedConnection = if (preferences.privacyMode && connectionEvidence != null) {
            PrivacyRedactor.redact(connectionEvidence)
        } else {
            connectionEvidence
        }
        val analysisSelected = selected?.takeIf {
            WifiChannelMapper.group(it.channel) == controls.channelGroup
        } ?: observations.filter {
            WifiChannelMapper.group(it.channel) == controls.channelGroup
        }.maxByOrNull { it.rssiDbm }
        val interference = analysisSelected?.let { InterferenceAnalyzer.summarize(it, observations) }
        val posture = NeighborhoodPostureAnalyzer.summarize(observations)
        val stability = AccessPointStabilityAnalyzer.summarize(
            selected?.id?.let { selectedId ->
                controls.history.frames.map { it.rssiByNetworkId[selectedId] }
            }.orEmpty(),
        )
        val observedGroups = observations.map { WifiChannelMapper.group(it.channel) }.toSet()
        val channelGroupModels = WifiChannelGroup.entries.mapNotNull { group ->
            val observed = group in observedGroups
            val supported = when (group.band) {
                WifiBand.GHZ_2_4 -> capabilities.hasWifiHardware
                WifiBand.GHZ_5 -> capabilities.supports5Ghz
                WifiBand.GHZ_6 -> capabilities.supports6Ghz
                WifiBand.UNKNOWN -> observed
            }
            if (group == WifiChannelGroup.UNKNOWN && !observed) return@mapNotNull null
            ChannelGroupUiModel(
                group = group,
                enabled = supported || observed,
            )
        }

        return OpenScannerUiState(
            phase = phase,
            activeTab = controls.tab,
            capabilities = capabilities,
            channelGroups = channelGroupModels,
            selectedChannelGroup = controls.channelGroup,
            networks = uiNetworks,
            selectedNetwork = uiNetworks.firstOrNull { it.selected },
            signalHistory = selected?.id?.let { controls.history.signals[it] }.orEmpty(),
            selectedStability = StabilityUiModel(
                level = stability.level,
                assessedSnapshots = stability.assessedSnapshots,
                presentSnapshots = stability.presentSnapshots,
                absentPercent = stability.absentShare?.times(100.0)?.roundToInt(),
                rssiRangeDb = stability.rssiRangeDb,
                sufficient = stability.level != AccessPointStabilityLevel.INSUFFICIENT,
            ),
            historyNowElapsedMs = nowElapsed,
            connection = ConnectionUiModel(
                connected = displayedConnection?.connected == true,
                networkName = displayedConnection?.ssid,
                networkNameRedacted = preferences.privacyMode && displayedConnection?.ssid != null,
                bssid = displayedConnection?.bssid,
                validated = displayedConnection?.validated,
                captivePortal = displayedConnection?.captivePortal,
                linkSpeedMbps = displayedConnection?.linkSpeedMbps,
                rxLinkSpeedMbps = displayedConnection?.rxLinkSpeedMbps,
                txLinkSpeedMbps = displayedConnection?.txLinkSpeedMbps,
                ipAddress = displayedConnection?.ipAddress,
                gateway = displayedConnection?.gateway,
                dnsServers = displayedConnection?.dnsServers.orEmpty(),
            ),
            congestion = CongestionUiModel(
                level = interference?.congestion ?: org.openscanner.core.domain.ObservedCongestion.LOW,
                overlappingNetworks = interference?.overlappingNetworks ?: 0,
                coChannelNetworks = interference?.coChannelNetworks ?: 0,
                strongestInterfererDbm = interference?.strongestInterfererDbm,
            ),
            neighborhoodPosture = NeighborhoodPostureUiModel(
                accessPointCount = posture.accessPointCount,
                channelGroupCounts = WifiChannelGroup.entries.mapNotNull { group ->
                    posture.channelGroups[group]?.let { group.label to it }
                },
                securityCounts = posture.securityProfiles.entries
                    .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                    .map { it.key to it.value },
                generationCounts = posture.generations.entries
                    .sortedWith(compareByDescending<Map.Entry<org.openscanner.core.model.WifiGeneration, Int>> { it.value }
                        .thenBy { it.key.name })
                    .map { it.key to it.value },
            ),
            privacyMode = preferences.privacyMode,
            redactExports = preferences.redactExports,
            refreshIntervalSeconds = preferences.refreshIntervalSeconds,
            snapshotSequence = snapshot?.sequenceId,
            ageMs = age,
            freshness = age?.let { FreshnessPolicy.classify(it) },
            likelyThrottled = snapshot?.likelyThrottled == true,
            safeErrorCode = safeErrorCode,
            logging = WifiLoggingUiState(
                active = controls.logging.active,
                selectedFields = controls.logging.selectedFields,
                recordedFields = controls.logging.session?.selectedFields.orEmpty(),
                recordCount = controls.logging.session?.records?.size ?: 0,
                networkRowCount = controls.logging.session?.networkRowCount ?: 0,
                durationMs = if (controls.logging.active) {
                    controls.logging.startedAtElapsedMs?.let { (nowElapsed - it).coerceAtLeast(0L) } ?: 0L
                } else {
                    controls.logging.session?.durationMs ?: 0L
                },
                stopReason = controls.logging.session?.stopReason,
                redacted = controls.logging.session?.redacted,
            ),
        )
    }

    private fun AccessPointObservation.toUiModel(
        selected: Boolean,
        privacyMode: Boolean,
        aliasNumber: Int,
    ): NetworkUiModel {
        val displayed = if (privacyMode) PrivacyRedactor.redact(this, aliasNumber) else this
        return NetworkUiModel(
            uiId = opaqueId(id),
            name = displayed.ssid,
            bssid = displayed.bssid.takeUnless { this.bssid == UNAVAILABLE_BSSID },
            nameKind = networkNameKind(privacyMode, ssidHidden),
            privacyAliasNumber = aliasNumber.takeIf { privacyMode },
            band = channel.band,
            channelGroup = WifiChannelMapper.group(channel),
            channel = channel.number,
            frequencyMhz = channel.centerFrequencyMhz,
            footprintCenterFrequencyMhz = footprintCenterFrequencyMhz,
            channelWidthMhz = channelWidthMhz,
            signalDbm = rssiDbm,
            securityTypes = security,
            generation = generation.takeUnless { it == WifiGeneration.UNKNOWN },
            connected = isConnected,
            selected = selected,
        )
    }

    private fun opaqueId(rawId: String): String {
        return opaqueIds.getOrPut(rawId) {
            val digest = MessageDigest.getInstance("SHA-256").digest(rawId.toByteArray())
            digest.take(6).joinToString("") { byte -> "%02x".format(byte) }
        }
    }

    class Factory(
        private val wifiRepository: WifiScanRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OpenScannerViewModel(wifiRepository, settingsRepository) as T
    }
}

internal fun networkNameKind(privacyMode: Boolean, ssidHidden: Boolean): NetworkNameKind = when {
    privacyMode -> NetworkNameKind.PRIVACY_ALIAS
    ssidHidden -> NetworkNameKind.HIDDEN
    else -> NetworkNameKind.OBSERVED
}
