package org.openscanner.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.openscanner.app.R
import org.openscanner.core.export.ExportFormat
import org.openscanner.core.export.WifiLogField
import org.openscanner.core.export.WifiLogFieldCategory
import org.openscanner.core.export.WifiLogFormat
import org.openscanner.core.export.WifiLogStopReason

/**
 * On-screen labels for the core/export enums. The enum labels themselves stay
 * English because they are also written into exported report files; only the
 * UI presentation is resolved through string resources here.
 */
@Composable
fun WifiLogField.displayLabel(): String = stringResource(
    when (this) {
        WifiLogField.SCANNER_STATE -> R.string.xfield_scanner_state
        WifiLogField.SNAPSHOT_SEQUENCE -> R.string.xfield_snapshot_sequence
        WifiLogField.SCAN_FLAGS -> R.string.xfield_scan_flags
        WifiLogField.SOURCE_AGE_MS -> R.string.xfield_source_age_ms
        WifiLogField.NETWORK_COUNT -> R.string.xfield_network_count
        WifiLogField.DEVICE_CAPABILITIES -> R.string.xfield_device_capabilities
        WifiLogField.NETWORK_NAME -> R.string.xfield_network_name
        WifiLogField.BSSID -> R.string.xfield_bssid
        WifiLogField.CHANNEL_GROUP -> R.string.xfield_channel_group
        WifiLogField.BAND -> R.string.xfield_band
        WifiLogField.CHANNEL -> R.string.xfield_channel
        WifiLogField.FREQUENCY_MHZ -> R.string.xfield_frequency_mhz
        WifiLogField.FOOTPRINT_CENTER_MHZ -> R.string.xfield_footprint_center_mhz
        WifiLogField.CHANNEL_WIDTH_MHZ -> R.string.xfield_channel_width_mhz
        WifiLogField.RSSI_DBM -> R.string.xfield_rssi_dbm
        WifiLogField.OBSERVATION_AGE_MS -> R.string.xfield_observation_age_ms
        WifiLogField.SECURITY -> R.string.xfield_security
        WifiLogField.WIFI_GENERATION -> R.string.xfield_wifi_generation
        WifiLogField.CONNECTED_AP -> R.string.xfield_connected_ap
        WifiLogField.CONNECTION_STATUS -> R.string.xfield_connection_status
        WifiLogField.CONNECTION_NETWORK -> R.string.xfield_connection_network
        WifiLogField.CONNECTION_BSSID -> R.string.xfield_connection_bssid
        WifiLogField.VALIDATION_STATUS -> R.string.xfield_validation_status
        WifiLogField.CAPTIVE_PORTAL -> R.string.xfield_captive_portal
        WifiLogField.LINK_SPEEDS -> R.string.xfield_link_speeds
        WifiLogField.IP_ADDRESS -> R.string.xfield_ip_address
        WifiLogField.GATEWAY -> R.string.xfield_gateway
        WifiLogField.DNS_SERVERS -> R.string.xfield_dns_servers
    },
)

@Composable
fun WifiLogField.displayDescription(): String = stringResource(
    when (this) {
        WifiLogField.SCANNER_STATE -> R.string.xfield_scanner_state_desc
        WifiLogField.SNAPSHOT_SEQUENCE -> R.string.xfield_snapshot_sequence_desc
        WifiLogField.SCAN_FLAGS -> R.string.xfield_scan_flags_desc
        WifiLogField.SOURCE_AGE_MS -> R.string.xfield_source_age_ms_desc
        WifiLogField.NETWORK_COUNT -> R.string.xfield_network_count_desc
        WifiLogField.DEVICE_CAPABILITIES -> R.string.xfield_device_capabilities_desc
        WifiLogField.NETWORK_NAME -> R.string.xfield_network_name_desc
        WifiLogField.BSSID -> R.string.xfield_bssid_desc
        WifiLogField.CHANNEL_GROUP -> R.string.xfield_channel_group_desc
        WifiLogField.BAND -> R.string.xfield_band_desc
        WifiLogField.CHANNEL -> R.string.xfield_channel_desc
        WifiLogField.FREQUENCY_MHZ -> R.string.xfield_frequency_mhz_desc
        WifiLogField.FOOTPRINT_CENTER_MHZ -> R.string.xfield_footprint_center_mhz_desc
        WifiLogField.CHANNEL_WIDTH_MHZ -> R.string.xfield_channel_width_mhz_desc
        WifiLogField.RSSI_DBM -> R.string.xfield_rssi_dbm_desc
        WifiLogField.OBSERVATION_AGE_MS -> R.string.xfield_observation_age_ms_desc
        WifiLogField.SECURITY -> R.string.xfield_security_desc
        WifiLogField.WIFI_GENERATION -> R.string.xfield_wifi_generation_desc
        WifiLogField.CONNECTED_AP -> R.string.xfield_connected_ap_desc
        WifiLogField.CONNECTION_STATUS -> R.string.xfield_connection_status_desc
        WifiLogField.CONNECTION_NETWORK -> R.string.xfield_connection_network_desc
        WifiLogField.CONNECTION_BSSID -> R.string.xfield_connection_bssid_desc
        WifiLogField.VALIDATION_STATUS -> R.string.xfield_validation_status_desc
        WifiLogField.CAPTIVE_PORTAL -> R.string.xfield_captive_portal_desc
        WifiLogField.LINK_SPEEDS -> R.string.xfield_link_speeds_desc
        WifiLogField.IP_ADDRESS -> R.string.xfield_ip_address_desc
        WifiLogField.GATEWAY -> R.string.xfield_gateway_desc
        WifiLogField.DNS_SERVERS -> R.string.xfield_dns_servers_desc
    },
)

@Composable
fun WifiLogFieldCategory.displayLabel(): String = stringResource(
    when (this) {
        WifiLogFieldCategory.SCAN -> R.string.xcat_scan
        WifiLogFieldCategory.NETWORK -> R.string.xcat_network
        WifiLogFieldCategory.CONNECTION -> R.string.xcat_connection
    },
)

@Composable
fun WifiLogStopReason.displayLabel(): String = stringResource(
    when (this) {
        WifiLogStopReason.USER -> R.string.xstop_user
        WifiLogStopReason.SAFETY_LIMIT -> R.string.xstop_safety_limit
    },
)

@Composable
fun ExportFormat.displayLabel(): String = stringResource(
    when (this) {
        ExportFormat.JSON -> R.string.xfmt_json
        ExportFormat.CSV -> R.string.xfmt_csv
    },
)

@Composable
fun WifiLogFormat.displayLabel(): String = stringResource(
    when (this) {
        WifiLogFormat.TEXT -> R.string.xfmt_text_report
        WifiLogFormat.JSON -> R.string.xfmt_json
        WifiLogFormat.CSV -> R.string.xfmt_csv
    },
)
