package org.openscanner.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.openscanner.app.OpenScannerUiState
import org.openscanner.app.R
import org.openscanner.app.ui.components.AppHeader
import org.openscanner.app.ui.components.ChannelGroupSelector
import org.openscanner.app.ui.components.InformationBanner
import org.openscanner.app.ui.components.PrimaryAction
import org.openscanner.app.ui.components.SpectrumChart
import org.openscanner.app.ui.displayLabel
import org.openscanner.app.ui.theme.ScannerBorder
import org.openscanner.app.ui.theme.ScannerCyan
import org.openscanner.app.ui.theme.ScannerMuted
import org.openscanner.app.ui.theme.ScannerSpacing
import org.openscanner.app.ui.theme.ScannerSurface
import org.openscanner.core.model.WifiChannelGroup

@Composable
fun SpectrumScreen(
    state: OpenScannerUiState,
    onChannelGroupSelected: (WifiChannelGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    var analysisVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.selectedChannelGroup, state.snapshotSequence) { analysisVisible = false }
    val networks = state.networks.filter { it.channelGroup == state.selectedChannelGroup }
    val selected = networks.firstOrNull { it.selected } ?: networks.maxByOrNull { it.signalDbm }
    val chartNetworks = networks.map { it.copy(selected = it.uiId == selected?.uiId) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
    ) {
        AppHeader(eyebrow = stringResource(R.string.spectrum_channel_spectrum), phase = state.phase, freshness = state.freshness)
        ChannelGroupSelector(
            state.channelGroups,
            state.selectedChannelGroup,
            onChannelGroupSelected,
        )
        if (state.selectedChannelGroup == WifiChannelGroup.GHZ_5_5_DFS) {
            InformationBanner(
                icon = Icons.Rounded.Info,
                text = stringResource(R.string.spectrum_dfs_banner),
            )
        }
        if (chartNetworks.isEmpty()) {
            InformationBanner(
                icon = Icons.Rounded.Info,
                text = stringResource(R.string.spectrum_no_observations, state.selectedChannelGroup.displayLabel()),
            )
        } else {
            SpectrumChart(state.selectedChannelGroup, chartNetworks)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ScannerBorder, MaterialTheme.shapes.small)
                    .background(ScannerSurface, MaterialTheme.shapes.small)
                    .padding(ScannerSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Sm),
            ) {
                Text(
                    text = stringResource(R.string.spectrum_selected_access_point),
                    color = ScannerMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    SummaryCell(stringResource(R.string.spectrum_channel), selected?.channel?.toString() ?: "—", Modifier.weight(1f))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(ScannerBorder))
                    SummaryCell(stringResource(R.string.spectrum_signal), selected?.let { stringResource(R.string.spectrum_signal_dbm, it.signalDbm) } ?: "—", Modifier.weight(1f))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(ScannerBorder))
                    SummaryCell(stringResource(R.string.spectrum_overlap), state.congestion.level.displayLabel(), Modifier.weight(1f))
                }
            }
            if (analysisVisible) {
                InformationBanner(
                    icon = Icons.Rounded.CheckCircle,
                    text = stringResource(
                        R.string.spectrum_overlap_analysis,
                        state.congestion.level.displayLabel().lowercase(),
                        pluralStringResource(
                            R.plurals.spectrum_overlapping_networks,
                            state.congestion.overlappingNetworks,
                            state.congestion.overlappingNetworks,
                        ),
                        state.congestion.coChannelNetworks,
                    ),
                    positive = state.congestion.level != org.openscanner.core.domain.ObservedCongestion.HIGH,
                )
            }
            PrimaryAction(stringResource(R.string.spectrum_analyze_interference), onClick = { analysisVisible = true })
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = ScannerSpacing.Xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Xs),
    ) {
        Text(label, color = ScannerMuted, style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            color = ScannerCyan,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
