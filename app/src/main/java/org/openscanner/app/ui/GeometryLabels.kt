package org.openscanner.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.openscanner.app.R
import org.openscanner.core.model.WifiChannelGroup

/**
 * Display-side label mappers for the pure-Kotlin chart geometry helpers.
 * The geometry functions keep their English defaults (unit tests and any
 * non-UI consumers rely on them); composables resolve the localized label
 * here, keyed on the enum rather than on a display string.
 */

/** Localized axis title for the spectrum frequency axis. */
@Composable
fun WifiChannelGroup.spectrumAxisTitleLabel(): String = stringResource(
    when (this) {
        WifiChannelGroup.GHZ_2_4,
        WifiChannelGroup.GHZ_5_2,
        WifiChannelGroup.GHZ_5_5_DFS,
        WifiChannelGroup.GHZ_5_8 -> R.string.geo_spectrum_axis_channel
        WifiChannelGroup.GHZ_6,
        WifiChannelGroup.UNKNOWN -> R.string.geo_spectrum_axis_frequency_ghz
    },
)

/** Localized full label for a channel group, matching the core enum `label`. */
@Composable
fun WifiChannelGroup.displayLabel(): String = stringResource(
    when (this) {
        WifiChannelGroup.GHZ_2_4 -> R.string.label_channel_group_ghz_2_4
        WifiChannelGroup.GHZ_5_2 -> R.string.label_channel_group_ghz_5_2
        WifiChannelGroup.GHZ_5_5_DFS -> R.string.label_channel_group_ghz_5_5_dfs
        WifiChannelGroup.GHZ_5_8 -> R.string.label_channel_group_ghz_5_8
        WifiChannelGroup.GHZ_6 -> R.string.label_channel_group_ghz_6
        WifiChannelGroup.UNKNOWN -> R.string.label_channel_group_unknown
    },
)

/** Localized compact selector label, matching the core enum `selectorLabel`. */
@Composable
fun WifiChannelGroup.displaySelectorLabel(): String = when (this) {
    WifiChannelGroup.GHZ_5_5_DFS -> stringResource(R.string.label_channel_group_selector_ghz_5_5_dfs)
    WifiChannelGroup.UNKNOWN -> stringResource(R.string.label_channel_group_selector_unknown)
    else -> displayLabel()
}
