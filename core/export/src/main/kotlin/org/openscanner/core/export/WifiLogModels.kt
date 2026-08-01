package org.openscanner.core.export

enum class WifiLogFieldCategory(val label: String) {
    SCAN("Scan state"),
    NETWORK("Access points"),
    CONNECTION("Current connection"),
}

enum class WifiLogValueKind {
    TEXT,
    INTEGER,
    BOOLEAN,
}

/**
 * Selectable fields for a Wi-Fi logging session. Record number and elapsed
 * session time are mandatory structural metadata and are therefore not in
 * this list.
 */
enum class WifiLogField(
    val key: String,
    val label: String,
    val description: String,
    val category: WifiLogFieldCategory,
    val valueKind: WifiLogValueKind = WifiLogValueKind.TEXT,
    val unit: String? = null,
) {
    SCANNER_STATE(
        "scanner_state",
        "Scanner state",
        "Live, paused, blocked, or error state and its safe error code",
        WifiLogFieldCategory.SCAN,
    ),
    SNAPSHOT_SEQUENCE(
        "snapshot_sequence",
        "Snapshot sequence",
        "Coordinator sequence number for changed radio evidence",
        WifiLogFieldCategory.SCAN,
        WifiLogValueKind.INTEGER,
    ),
    SCAN_FLAGS(
        "scan_flags",
        "Scan outcome flags",
        "Request accepted, results updated, and likely-throttled evidence",
        WifiLogFieldCategory.SCAN,
    ),
    SOURCE_AGE_MS(
        "source_age_ms",
        "Source age",
        "Age of the newest Android scan observation at log time",
        WifiLogFieldCategory.SCAN,
        WifiLogValueKind.INTEGER,
        "ms",
    ),
    NETWORK_COUNT(
        "network_count",
        "Access-point count",
        "Number of access-point observations in the snapshot",
        WifiLogFieldCategory.SCAN,
        WifiLogValueKind.INTEGER,
    ),
    DEVICE_CAPABILITIES(
        "device_capabilities",
        "Device capabilities",
        "Wi-Fi hardware and reported 5/6 GHz support",
        WifiLogFieldCategory.SCAN,
    ),

    NETWORK_NAME(
        "network_name",
        "Network name",
        "Stable session alias; raw SSIDs are never logged",
        WifiLogFieldCategory.NETWORK,
    ),
    BSSID(
        "bssid_alias",
        "BSSID alias",
        "Stable session token; raw BSSIDs are never logged",
        WifiLogFieldCategory.NETWORK,
    ),
    CHANNEL_GROUP(
        "channel_group",
        "Channel group",
        "Channel-validated 2.4, 5.2, 5.5/DFS, 5.8, 6 GHz, or unknown group",
        WifiLogFieldCategory.NETWORK,
    ),
    BAND(
        "band",
        "Radio band",
        "Android observation band",
        WifiLogFieldCategory.NETWORK,
    ),
    CHANNEL(
        "channel",
        "Primary channel",
        "Validated primary Wi-Fi channel when available",
        WifiLogFieldCategory.NETWORK,
        WifiLogValueKind.INTEGER,
    ),
    FREQUENCY_MHZ(
        "frequency_mhz",
        "Primary frequency",
        "Primary center frequency reported by Android",
        WifiLogFieldCategory.NETWORK,
        WifiLogValueKind.INTEGER,
        "MHz",
    ),
    FOOTPRINT_CENTER_MHZ(
        "footprint_center_mhz",
        "Footprint center",
        "Reported channel-footprint center frequency",
        WifiLogFieldCategory.NETWORK,
        WifiLogValueKind.INTEGER,
        "MHz",
    ),
    CHANNEL_WIDTH_MHZ(
        "channel_width_mhz",
        "Channel width",
        "Reported width; unavailable is not replaced with an assumption",
        WifiLogFieldCategory.NETWORK,
        WifiLogValueKind.INTEGER,
        "MHz",
    ),
    RSSI_DBM(
        "rssi_dbm",
        "Signal strength",
        "Received signal strength reported by Android",
        WifiLogFieldCategory.NETWORK,
        WifiLogValueKind.INTEGER,
        "dBm",
    ),
    OBSERVATION_AGE_MS(
        "observation_age_ms",
        "Observation age",
        "Age of this Android access-point observation at log time",
        WifiLogFieldCategory.NETWORK,
        WifiLogValueKind.INTEGER,
        "ms",
    ),
    SECURITY(
        "security",
        "Advertised security",
        "Parsed beacon security capabilities",
        WifiLogFieldCategory.NETWORK,
    ),
    WIFI_GENERATION(
        "wifi_generation",
        "Wi-Fi generation",
        "Reported Wi-Fi standard when Android exposes it",
        WifiLogFieldCategory.NETWORK,
    ),
    CONNECTED_AP(
        "connected_ap",
        "Connected access point",
        "Whether Android evidence matches this BSSID",
        WifiLogFieldCategory.NETWORK,
        WifiLogValueKind.BOOLEAN,
    ),

    CONNECTION_STATUS(
        "connection_status",
        "Wi-Fi connected",
        "Whether a physical Wi-Fi transport is available",
        WifiLogFieldCategory.CONNECTION,
        WifiLogValueKind.BOOLEAN,
    ),
    CONNECTION_NETWORK(
        "connection_network",
        "Connected network",
        "Stable session alias when it matches an observed BSSID",
        WifiLogFieldCategory.CONNECTION,
    ),
    CONNECTION_BSSID(
        "connection_bssid_alias",
        "Connected BSSID alias",
        "Stable session token for the connected hardware address",
        WifiLogFieldCategory.CONNECTION,
    ),
    VALIDATION_STATUS(
        "validated",
        "Android validated",
        "Android network-validation evidence",
        WifiLogFieldCategory.CONNECTION,
        WifiLogValueKind.BOOLEAN,
    ),
    CAPTIVE_PORTAL(
        "captive_portal",
        "Captive portal",
        "Android captive-portal evidence",
        WifiLogFieldCategory.CONNECTION,
        WifiLogValueKind.BOOLEAN,
    ),
    LINK_SPEEDS(
        "link_speeds_mbps",
        "Link speeds",
        "Overall, receive, and transmit link speeds",
        WifiLogFieldCategory.CONNECTION,
    ),
    IP_ADDRESS(
        "ip_address",
        "IP address",
        "Masked local IP address",
        WifiLogFieldCategory.CONNECTION,
    ),
    GATEWAY(
        "gateway",
        "Gateway",
        "Masked default gateway",
        WifiLogFieldCategory.CONNECTION,
    ),
    DNS_SERVERS(
        "dns_servers",
        "DNS servers",
        "Masked DNS server addresses",
        WifiLogFieldCategory.CONNECTION,
    ),
}

enum class WifiLogStopReason(val label: String) {
    USER("Stopped by user"),
    SAFETY_LIMIT("Stopped at the in-memory safety limit"),
}

data class WifiLogRecord(
    val index: Int,
    val elapsedMs: Long,
    val scanValues: Map<WifiLogField, String?>,
    val networkValues: List<Map<WifiLogField, String?>>,
    val connectionValues: Map<WifiLogField, String?>,
)

/** Contains redacted values only. */
data class WifiLogSession(
    val startedAtEpochMinuteMs: Long,
    val selectedFields: Set<WifiLogField>,
    val records: List<WifiLogRecord>,
    val endedAfterMs: Long? = null,
    val stopReason: WifiLogStopReason? = null,
) {
    val networkRowCount: Int get() = records.sumOf { it.networkValues.size }
    val durationMs: Long get() = endedAfterMs ?: records.lastOrNull()?.elapsedMs ?: 0L
}
