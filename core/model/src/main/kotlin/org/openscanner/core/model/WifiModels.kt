package org.openscanner.core.model

enum class WifiBand(val label: String) {
    GHZ_2_4("2.4 GHz"),
    GHZ_5("5 GHz"),
    GHZ_6("6 GHz"),
    UNKNOWN("Unknown"),
}

/**
 * User-facing channel groups. Unlike [WifiBand], the three 5 GHz groups are
 * intentionally split by validated primary-channel membership. The DFS name
 * is descriptive only; it does not assert regulatory availability.
 */
enum class WifiChannelGroup(
    val label: String,
    val selectorLabel: String = label,
    val band: WifiBand,
) {
    GHZ_2_4("2.4 GHz", band = WifiBand.GHZ_2_4),
    GHZ_5_2("5.2 GHz", band = WifiBand.GHZ_5),
    GHZ_5_5_DFS("5.5 GHz / DFS", "5.5 / DFS", WifiBand.GHZ_5),
    GHZ_5_8("5.8 GHz", band = WifiBand.GHZ_5),
    GHZ_6("6 GHz", band = WifiBand.GHZ_6),
    UNKNOWN("Unknown / unsupported frequency", "Unknown", WifiBand.UNKNOWN),
}

data class WifiChannel(
    val band: WifiBand,
    val number: Int?,
    val centerFrequencyMhz: Int,
)

enum class SecurityType(val label: String) {
    OPEN("Open"),
    OWE("Enhanced Open"),
    WEP("WEP"),
    WPA_PERSONAL("WPA Personal"),
    WPA2_PERSONAL("WPA2 Personal"),
    WPA3_PERSONAL("WPA3 Personal"),
    WPA2_WPA3_PERSONAL("WPA2/WPA3 Personal"),
    ENTERPRISE("Enterprise"),
    WPA3_ENTERPRISE("WPA3 Enterprise"),
    WAPI("WAPI"),
    UNKNOWN("Unknown"),
}

enum class WifiGeneration(val label: String) {
    LEGACY("Legacy"),
    WIFI_4("Wi-Fi 4"),
    WIFI_5("Wi-Fi 5"),
    WIFI_6("Wi-Fi 6"),
    WIFI_6E("Wi-Fi 6E"),
    WIFI_7("Wi-Fi 7"),
    UNKNOWN("Unknown"),
}

data class AccessPointObservation(
    val id: String,
    val ssid: String,
    val bssid: String,
    val channel: WifiChannel,
    val channelWidthMhz: Int?,
    val footprintCenterFrequencyMhz: Int?,
    val rssiDbm: Int,
    val security: Set<SecurityType>,
    val generation: WifiGeneration,
    val timestampMicros: Long,
    val isConnected: Boolean,
) {
    override fun toString(): String =
        "AccessPointObservation(id=<redacted>, ssid=<redacted>, bssid=<redacted>, " +
            "channel=$channel, channelWidthMhz=$channelWidthMhz, " +
            "footprintCenterFrequencyMhz=$footprintCenterFrequencyMhz, rssiDbm=$rssiDbm, " +
            "security=$security, generation=$generation, timestampMicros=$timestampMicros, " +
            "isConnected=$isConnected)"
}

data class ConnectionEvidence(
    val connected: Boolean,
    val bssid: String?,
    val ssid: String?,
    val validated: Boolean?,
    val captivePortal: Boolean?,
    val linkSpeedMbps: Int?,
    val rxLinkSpeedMbps: Int?,
    val txLinkSpeedMbps: Int?,
    val ipAddress: String?,
    val gateway: String?,
    val dnsServers: List<String>,
) {
    override fun toString(): String =
        "ConnectionEvidence(connected=$connected, identifiers=<redacted>, validated=$validated, " +
            "captivePortal=$captivePortal, linkSpeedMbps=$linkSpeedMbps, " +
            "rxLinkSpeedMbps=$rxLinkSpeedMbps, txLinkSpeedMbps=$txLinkSpeedMbps)"
}

data class PlatformCapabilities(
    val hasWifiHardware: Boolean,
    val supports5Ghz: Boolean,
    val supports6Ghz: Boolean,
    /** Persisted Developer Options state when Android exposes it; null on older/unsupported devices. */
    val wifiScanThrottleEnabled: Boolean? = null,
)

data class ScanSnapshot(
    val sequenceId: Long,
    val capturedAtEpochMs: Long,
    val capturedAtElapsedMs: Long,
    val sourceTimestampMicros: Long?,
    val requestAccepted: Boolean?,
    val resultsUpdated: Boolean?,
    val likelyThrottled: Boolean,
    val observations: List<AccessPointObservation>,
    val connection: ConnectionEvidence,
) {
    override fun toString(): String =
        "ScanSnapshot(sequenceId=$sequenceId, capturedAtEpochMs=$capturedAtEpochMs, " +
            "capturedAtElapsedMs=$capturedAtElapsedMs, sourceTimestampMicros=$sourceTimestampMicros, " +
            "requestAccepted=$requestAccepted, resultsUpdated=$resultsUpdated, " +
            "likelyThrottled=$likelyThrottled, observationCount=${observations.size}, " +
            "connection=<redacted>)"
}

enum class ScannerPhase {
    CHECKING,
    LIVE,
    PAUSED,
    PERMISSION_REQUIRED,
    WIFI_DISABLED,
    LOCATION_DISABLED,
    UNSUPPORTED,
    ERROR,
}

data class ScannerState(
    val phase: ScannerPhase = ScannerPhase.CHECKING,
    val capabilities: PlatformCapabilities = PlatformCapabilities(
        hasWifiHardware = true,
        supports5Ghz = true,
        supports6Ghz = false,
    ),
    val snapshot: ScanSnapshot? = null,
    val safeErrorCode: String? = null,
) {
    override fun toString(): String =
        "ScannerState(phase=$phase, capabilities=$capabilities, " +
            "snapshotSequence=${snapshot?.sequenceId}, safeErrorCode=$safeErrorCode)"
}

data class SignalSample(
    val elapsedRealtimeMs: Long,
    val rssiDbm: Int,
)

data class AppPreferences(
    val privacyMode: Boolean = false,
    val redactExports: Boolean = true,
    val refreshIntervalSeconds: Int = 30,
)
