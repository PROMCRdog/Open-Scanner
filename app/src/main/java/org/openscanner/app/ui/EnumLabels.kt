package org.openscanner.app.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.openscanner.app.ConnectionUiModel
import org.openscanner.app.NetworkNameKind
import org.openscanner.app.NetworkUiModel
import org.openscanner.app.R
import org.openscanner.core.domain.AccessPointStabilityLevel
import org.openscanner.core.domain.ObservedCongestion
import org.openscanner.core.domain.SignalQuality
import org.openscanner.core.model.SecurityType
import org.openscanner.core.model.WifiGeneration

/**
 * Resolves the user-facing display label for core-module enums at the
 * composable display site. The enum `label` properties stay English because
 * they also feed exported report files; UI logic must key off the enum, never
 * off these resolved strings.
 */

@StringRes
private fun SignalQuality.labelRes(): Int = when (this) {
    SignalQuality.EXCELLENT -> R.string.label_signal_quality_excellent
    SignalQuality.GOOD -> R.string.label_signal_quality_good
    SignalQuality.FAIR -> R.string.label_signal_quality_fair
    SignalQuality.WEAK -> R.string.label_signal_quality_weak
}

@Composable
fun SignalQuality.displayLabel(): String = stringResource(labelRes())

@StringRes
private fun AccessPointStabilityLevel.labelRes(): Int = when (this) {
    AccessPointStabilityLevel.STEADY -> R.string.label_stability_steady
    AccessPointStabilityLevel.VARIABLE -> R.string.label_stability_variable
    AccessPointStabilityLevel.FLAPPING -> R.string.label_stability_flapping
    AccessPointStabilityLevel.INSUFFICIENT -> R.string.label_stability_insufficient
}

@Composable
fun AccessPointStabilityLevel.displayLabel(): String = stringResource(labelRes())

@StringRes
private fun ObservedCongestion.labelRes(): Int = when (this) {
    ObservedCongestion.LOW -> R.string.label_congestion_low
    ObservedCongestion.MODERATE -> R.string.label_congestion_moderate
    ObservedCongestion.HIGH -> R.string.label_congestion_high
}

@Composable
fun ObservedCongestion.displayLabel(): String = stringResource(labelRes())

@StringRes
private fun SecurityType.labelRes(): Int = when (this) {
    SecurityType.OPEN -> R.string.label_security_open
    SecurityType.OWE -> R.string.label_security_enhanced_open
    SecurityType.WEP -> R.string.label_security_wep
    SecurityType.WPA_PERSONAL -> R.string.label_security_wpa_personal
    SecurityType.WPA2_PERSONAL -> R.string.label_security_wpa2_personal
    SecurityType.WPA3_PERSONAL -> R.string.label_security_wpa3_personal
    SecurityType.WPA2_WPA3_PERSONAL -> R.string.label_security_wpa2_wpa3_personal
    SecurityType.ENTERPRISE -> R.string.label_security_enterprise
    SecurityType.WPA3_ENTERPRISE -> R.string.label_security_wpa3_enterprise
    SecurityType.WAPI -> R.string.label_security_wapi
    SecurityType.UNKNOWN -> R.string.label_security_unknown
}

@Composable
fun SecurityType.displayLabel(): String = stringResource(labelRes())

/** Joined security summary for a network, matching the previous "A + B" rendering. */
@Composable
fun Set<SecurityType>.displayLabel(): String =
    map { it.displayLabel() }.joinToString(" + ")

@StringRes
private fun WifiGeneration.labelRes(): Int = when (this) {
    WifiGeneration.LEGACY -> R.string.label_wifi_generation_legacy
    WifiGeneration.WIFI_4 -> R.string.label_wifi_generation_wifi_4
    WifiGeneration.WIFI_5 -> R.string.label_wifi_generation_wifi_5
    WifiGeneration.WIFI_6 -> R.string.label_wifi_generation_wifi_6
    WifiGeneration.WIFI_6E -> R.string.label_wifi_generation_wifi_6e
    WifiGeneration.WIFI_7 -> R.string.label_wifi_generation_wifi_7
    WifiGeneration.UNKNOWN -> R.string.label_wifi_generation_unknown
}

@Composable
fun WifiGeneration.displayLabel(): String = stringResource(labelRes())

/** Resolves semantic placeholders without translating a real SSID by pattern. */
@Composable
fun NetworkUiModel.displayName(): String = when (nameKind) {
    NetworkNameKind.OBSERVED -> name
    NetworkNameKind.HIDDEN -> stringResource(R.string.common_hidden_network)
    NetworkNameKind.PRIVACY_ALIAS -> privacyAliasNumber?.let {
        stringResource(R.string.common_private_network_alias, it)
    } ?: name
}

@Composable
fun ConnectionUiModel.displayNetworkName(unavailable: String): String = when {
    networkName == null -> unavailable
    networkNameRedacted -> stringResource(R.string.common_connected_network_alias)
    else -> networkName
}

@Composable
fun Boolean?.displayLabel(unavailable: String): String = when (this) {
    true -> stringResource(R.string.common_yes)
    false -> stringResource(R.string.common_no)
    null -> unavailable
}
