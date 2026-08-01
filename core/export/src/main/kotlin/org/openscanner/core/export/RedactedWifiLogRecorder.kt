package org.openscanner.core.export

import java.security.MessageDigest
import java.security.SecureRandom
import org.openscanner.core.domain.WifiChannelMapper
import org.openscanner.core.model.AccessPointObservation
import org.openscanner.core.model.ConnectionEvidence
import org.openscanner.core.model.ScannerState
import org.openscanner.core.privacy.PrivacyRedactor

enum class WifiLogRecordResult {
    ADDED,
    DUPLICATE,
    LIMIT_REACHED,
    STOPPED,
}

/**
 * Memory-only session recorder. Raw identifiers are used only long enough to
 * assign a stable alias and are transformed before a [WifiLogRecord] exists.
 */
class RedactedWifiLogRecorder(
    selectedFields: Set<WifiLogField>,
    startedAtEpochMs: Long,
    private val startedAtElapsedMs: Long,
) {
    val selectedFields: Set<WifiLogField> = selectedFields.toSet()
    val startedAtEpochMinuteMs: Long = startedAtEpochMs - startedAtEpochMs % 60_000L

    private val records = mutableListOf<WifiLogRecord>()
    private val aliasSalt = ByteArray(32).also(SecureRandom()::nextBytes)
    private val aliases = linkedMapOf<String, Int>()
    private var lastStateFingerprint: Int? = null
    private var loggedNetworkRows = 0
    private var stoppedAfterMs: Long? = null
    private var stopReason: WifiLogStopReason? = null

    val isStopped: Boolean get() = stopReason != null

    fun record(
        state: ScannerState,
        recordedAtElapsedMs: Long,
        force: Boolean = false,
    ): WifiLogRecordResult {
        if (isStopped) return WifiLogRecordResult.STOPPED
        val fingerprint = state.hashCode()
        if (!force && lastStateFingerprint == fingerprint) return WifiLogRecordResult.DUPLICATE

        val snapshot = state.snapshot
        val incomingNetworkRows = if (selectedFields.any { it.category == WifiLogFieldCategory.NETWORK }) {
            snapshot?.observations?.size ?: 0
        } else {
            0
        }
        if (records.size >= MAX_RECORDS || loggedNetworkRows + incomingNetworkRows > MAX_NETWORK_ROWS) {
            stop(recordedAtElapsedMs, WifiLogStopReason.SAFETY_LIMIT)
            return WifiLogRecordResult.LIMIT_REACHED
        }

        val elapsedMs = (recordedAtElapsedMs - startedAtElapsedMs).coerceAtLeast(0L)
        snapshot?.observations.orEmpty().forEach(::aliasFor)
        val redactedNetworks = if (selectedFields.any { it.category == WifiLogFieldCategory.NETWORK }) {
            snapshot?.observations.orEmpty().map(::redactObservation)
        } else {
            emptyList()
        }
        records += WifiLogRecord(
            index = records.size + 1,
            elapsedMs = elapsedMs,
            scanValues = buildScanValues(state, recordedAtElapsedMs),
            networkValues = redactedNetworks.map { buildNetworkValues(it, recordedAtElapsedMs) },
            connectionValues = buildConnectionValues(snapshot?.connection),
        )
        loggedNetworkRows += redactedNetworks.size
        lastStateFingerprint = fingerprint
        return WifiLogRecordResult.ADDED
    }

    fun stop(recordedAtElapsedMs: Long, reason: WifiLogStopReason = WifiLogStopReason.USER) {
        if (isStopped) return
        stoppedAfterMs = (recordedAtElapsedMs - startedAtElapsedMs).coerceAtLeast(0L)
        stopReason = reason
    }

    fun snapshot(): WifiLogSession = WifiLogSession(
        startedAtEpochMinuteMs = startedAtEpochMinuteMs,
        selectedFields = selectedFields,
        records = records.toList(),
        endedAfterMs = stoppedAfterMs,
        stopReason = stopReason,
    )

    private fun buildScanValues(state: ScannerState, recordedAtElapsedMs: Long): Map<WifiLogField, String?> {
        val snapshot = state.snapshot
        return selectedValues(WifiLogFieldCategory.SCAN) { field ->
            when (field) {
                WifiLogField.SCANNER_STATE -> buildString {
                    append(state.phase.name)
                    state.safeErrorCode?.let { append(" ($it)") }
                }
                WifiLogField.SNAPSHOT_SEQUENCE -> snapshot?.sequenceId?.toString()
                WifiLogField.SCAN_FLAGS -> snapshot?.let {
                    "request_accepted=${it.requestAccepted ?: "unavailable"}; " +
                        "results_updated=${it.resultsUpdated ?: "unavailable"}; " +
                        "likely_throttled=${it.likelyThrottled}"
                }
                WifiLogField.SOURCE_AGE_MS -> snapshot?.let {
                    val sourceElapsedMs = it.sourceTimestampMicros?.takeIf { value -> value > 0L }?.div(1_000L)
                        ?: it.capturedAtElapsedMs
                    (recordedAtElapsedMs - sourceElapsedMs).coerceAtLeast(0L).toString()
                }
                WifiLogField.NETWORK_COUNT -> snapshot?.observations?.size?.toString()
                WifiLogField.DEVICE_CAPABILITIES -> with(state.capabilities) {
                    "wifi_hardware=$hasWifiHardware; 5_ghz=$supports5Ghz; 6_ghz=$supports6Ghz"
                }
                else -> null
            }
        }
    }

    private fun buildNetworkValues(
        network: AccessPointObservation,
        recordedAtElapsedMs: Long,
    ): Map<WifiLogField, String?> =
        selectedValues(WifiLogFieldCategory.NETWORK) { field ->
            when (field) {
                WifiLogField.NETWORK_NAME -> network.ssid
                WifiLogField.BSSID -> network.bssid
                WifiLogField.CHANNEL_GROUP -> WifiChannelMapper.group(network.channel).label
                WifiLogField.BAND -> network.channel.band.label
                WifiLogField.CHANNEL -> network.channel.number?.toString()
                WifiLogField.FREQUENCY_MHZ -> network.channel.centerFrequencyMhz.toString()
                WifiLogField.FOOTPRINT_CENTER_MHZ -> network.footprintCenterFrequencyMhz?.toString()
                WifiLogField.CHANNEL_WIDTH_MHZ -> network.channelWidthMhz?.toString()
                WifiLogField.RSSI_DBM -> network.rssiDbm.toString()
                WifiLogField.OBSERVATION_AGE_MS -> network.timestampMicros
                    .takeIf { it > 0L }
                    ?.div(1_000L)
                    ?.let { (recordedAtElapsedMs - it).coerceAtLeast(0L).toString() }
                WifiLogField.SECURITY -> network.security.sortedBy { it.name }.joinToString(" + ") { it.label }
                WifiLogField.WIFI_GENERATION -> network.generation.label
                WifiLogField.CONNECTED_AP -> network.isConnected.toString()
                else -> null
            }
        }

    private fun buildConnectionValues(connection: ConnectionEvidence?): Map<WifiLogField, String?> {
        if (connection == null) return selectedValues(WifiLogFieldCategory.CONNECTION) { null }
        val redacted = PrivacyRedactor.redact(connection)
        val connectedAliasNumber = connection.bssid?.let(::aliasForIdentifier)
        val connectedAlias = connectedAliasNumber
            ?.let { "Network $it" }
            ?: redacted.ssid
        return selectedValues(WifiLogFieldCategory.CONNECTION) { field ->
            when (field) {
                WifiLogField.CONNECTION_STATUS -> redacted.connected.toString()
                WifiLogField.CONNECTION_NETWORK -> connectedAlias
                WifiLogField.CONNECTION_BSSID -> connectedAliasNumber?.let(::accessPointAlias) ?: redacted.bssid
                WifiLogField.VALIDATION_STATUS -> redacted.validated?.toString()
                WifiLogField.CAPTIVE_PORTAL -> redacted.captivePortal?.toString()
                WifiLogField.LINK_SPEEDS ->
                    "link=${redacted.linkSpeedMbps ?: "unavailable"}; " +
                        "rx=${redacted.rxLinkSpeedMbps ?: "unavailable"}; " +
                        "tx=${redacted.txLinkSpeedMbps ?: "unavailable"}"
                WifiLogField.IP_ADDRESS -> redacted.ipAddress
                WifiLogField.GATEWAY -> redacted.gateway
                WifiLogField.DNS_SERVERS -> redacted.dnsServers.takeIf { it.isNotEmpty() }?.joinToString("; ")
                else -> null
            }
        }
    }

    private fun redactObservation(observation: AccessPointObservation): AccessPointObservation {
        val aliasNumber = aliasFor(observation)
        return PrivacyRedactor.redact(observation, aliasNumber).copy(
            bssid = accessPointAlias(aliasNumber),
        )
    }

    private fun aliasFor(observation: AccessPointObservation): Int =
        aliasForIdentifier(observation.id)

    private fun aliasForIdentifier(identifier: String): Int =
        aliases.getOrPut(aliasKey(identifier)) { aliases.size + 1 }

    private fun accessPointAlias(aliasNumber: Int): String = "AP-${aliasNumber.toString().padStart(3, '0')}"

    private fun aliasKey(identifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(aliasSalt)
        return digest.digest(identifier.lowercase().toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private inline fun selectedValues(
        category: WifiLogFieldCategory,
        value: (WifiLogField) -> String?,
    ): Map<WifiLogField, String?> = buildMap {
        WifiLogField.entries
            .filter { it.category == category && it in selectedFields }
            .forEach { put(it, value(it)) }
    }

    companion object {
        const val MAX_RECORDS = 500
        const val MAX_NETWORK_ROWS = 25_000
    }
}
