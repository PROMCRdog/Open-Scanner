package org.openscanner.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openscanner.app.OpenScannerUiState
import org.openscanner.app.R
import org.openscanner.app.NetworkUiModel
import org.openscanner.app.updateSpectrumSelection
import org.openscanner.app.updateSpectrumSelectionForFocus
import org.openscanner.app.visibleSpectrumNetworks
import org.openscanner.app.ui.components.AppHeader
import org.openscanner.app.ui.components.ChannelGroupSelector
import org.openscanner.app.ui.components.CurrentWifiBadge
import org.openscanner.app.ui.components.InformationBanner
import org.openscanner.app.ui.components.PrimaryAction
import org.openscanner.app.ui.components.SpectrumChart
import org.openscanner.app.ui.displayLabel
import org.openscanner.app.ui.displayName
import org.openscanner.app.ui.theme.ScannerBorder
import org.openscanner.app.ui.theme.ScannerCyan
import org.openscanner.app.ui.theme.ScannerIconWell
import org.openscanner.app.ui.theme.ScannerMuted
import org.openscanner.app.ui.theme.ScannerOnCyan
import org.openscanner.app.ui.theme.ScannerSpacing
import org.openscanner.app.ui.theme.ScannerSurface
import org.openscanner.app.ui.theme.ScannerSurfaceRaised
import org.openscanner.app.ui.theme.ScannerText
import org.openscanner.core.model.WifiChannelGroup

@Composable
fun SpectrumScreen(
    state: OpenScannerUiState,
    customSelectionIds: Set<String>?,
    onChannelGroupSelected: (WifiChannelGroup) -> Unit,
    onCustomSelectionChanged: (Set<String>?) -> Unit,
    onFocusedNetworkSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var analysisVisible by remember { mutableStateOf(false) }
    var selectorVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.selectedChannelGroup, state.snapshotSequence) { analysisVisible = false }
    val networks = state.networks.filter { it.channelGroup == state.selectedChannelGroup }
    val selected = networks.firstOrNull { it.selected } ?: networks.maxByOrNull { it.signalDbm }
    val displayedNetworks = visibleSpectrumNetworks(
        networks = networks,
        customSelectionIds = customSelectionIds,
        focusedNetworkId = selected?.uiId,
    )
    val chartNetworks = displayedNetworks.map { it.copy(selected = it.uiId == selected?.uiId) }

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
            SpectrumDisplaySelector(
                displayedCount = chartNetworks.size,
                totalCount = networks.size,
                onClick = { selectorVisible = true },
            )
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

    if (selectorVisible) {
        SpectrumNetworkSelectorDialog(
            networks = networks,
            displayedIds = chartNetworks.mapTo(linkedSetOf()) { it.uiId },
            focusedNetworkId = selected?.uiId,
            onDisplayedChange = { networkId, displayed ->
                val updated = updateSpectrumSelection(
                    networks = networks,
                    customSelectionIds = customSelectionIds,
                    focusedNetworkId = selected?.uiId,
                    networkId = networkId,
                    displayed = displayed,
                )
                onCustomSelectionChanged(updated)
            },
            onShowAll = { onCustomSelectionChanged(null) },
            onOnlySelected = {
                selected?.uiId?.let { onCustomSelectionChanged(setOf(it)) }
            },
            onFocusedChange = { networkId ->
                onCustomSelectionChanged(
                    updateSpectrumSelectionForFocus(
                        networks = networks,
                        customSelectionIds = customSelectionIds,
                        previousFocusedNetworkId = selected?.uiId,
                        newFocusedNetworkId = networkId,
                    ),
                )
                onFocusedNetworkSelected(networkId)
            },
            onDismiss = { selectorVisible = false },
        )
    }
}

@Composable
private fun SpectrumDisplaySelector(
    displayedCount: Int,
    totalCount: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ScannerSpacing.MinTouchTarget)
            .border(1.dp, ScannerBorder, MaterialTheme.shapes.small)
            .background(ScannerSurface, MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = ScannerSpacing.Md, vertical = ScannerSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
    ) {
        Icon(Icons.Rounded.FilterList, contentDescription = null, tint = ScannerCyan)
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.spectrum_displayed_networks),
                color = ScannerText,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.spectrum_showing_networks, displayedCount, totalCount),
                color = ScannerMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            stringResource(R.string.spectrum_choose),
            color = ScannerCyan,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun SpectrumNetworkSelectorDialog(
    networks: List<NetworkUiModel>,
    displayedIds: Set<String>,
    focusedNetworkId: String?,
    onDisplayedChange: (String, Boolean) -> Unit,
    onShowAll: () -> Unit,
    onOnlySelected: () -> Unit,
    onFocusedChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val orderedNetworks = networks.sortedWith(
        compareByDescending<NetworkUiModel> { it.uiId == focusedNetworkId }
            .thenByDescending { it.connected }
            .thenByDescending { it.signalDbm },
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ScannerSurfaceRaised,
        title = { Text(stringResource(R.string.spectrum_choose_networks)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Sm)) {
                Text(
                    stringResource(R.string.spectrum_selector_explanation),
                    color = ScannerMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Sm),
                ) {
                    TextButton(onClick = onShowAll, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.spectrum_show_all))
                    }
                    TextButton(onClick = onOnlySelected, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.spectrum_only_selected))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ScannerSpacing.Xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.spectrum_selector_show_column),
                        color = ScannerMuted,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(48.dp),
                    )
                    Box(Modifier.weight(1f))
                    Text(
                        stringResource(R.string.spectrum_selector_focus_column),
                        color = ScannerCyan,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(76.dp),
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Xs),
                ) {
                    items(orderedNetworks, key = { it.uiId }) { network ->
                        val focused = network.uiId == focusedNetworkId
                        val checked = focused || network.uiId in displayedIds
                        val networkName = network.displayName()
                        val displayContentDescription = stringResource(
                            R.string.spectrum_display_network_cd,
                            networkName,
                        )
                        val focusContentDescription = stringResource(
                            R.string.spectrum_focus_network_cd,
                            networkName,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = ScannerSpacing.MinTouchTarget)
                                .background(
                                    color = if (focused) ScannerIconWell else ScannerSurfaceRaised,
                                    shape = MaterialTheme.shapes.small,
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (focused) {
                                        ScannerCyan.copy(alpha = 0.72f)
                                    } else {
                                        ScannerSurfaceRaised
                                    },
                                    shape = MaterialTheme.shapes.small,
                                )
                                .clickable(
                                    enabled = !focused,
                                    role = Role.Checkbox,
                                    onClick = { onDisplayedChange(network.uiId, !checked) },
                                )
                                .padding(horizontal = ScannerSpacing.Xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = if (focused) null else {
                                    { onDisplayedChange(network.uiId, it) }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = ScannerCyan),
                                modifier = Modifier.semantics {
                                    contentDescription = displayContentDescription
                                },
                            )
                            Column(Modifier.weight(1f).padding(start = ScannerSpacing.Sm)) {
                                Text(
                                    networkName,
                                    color = ScannerText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (focused || network.connected) {
                                    Row(
                                        modifier = Modifier.padding(top = ScannerSpacing.Xs),
                                        horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Xs),
                                    ) {
                                        if (focused) {
                                            SpectrumFocusedBadge(networkName)
                                        }
                                        if (network.connected) {
                                            CurrentWifiBadge()
                                        }
                                    }
                                }
                                Text(
                                    stringResource(
                                        R.string.spectrum_selector_channel_signal,
                                        network.channel?.toString() ?: "—",
                                        network.signalDbm,
                                    ),
                                    color = ScannerMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = ScannerSpacing.Xs),
                                )
                            }
                            RadioButton(
                                selected = focused,
                                onClick = { onFocusedChange(network.uiId) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ScannerCyan,
                                    unselectedColor = ScannerMuted.copy(alpha = 0.38f),
                                ),
                                modifier = Modifier
                                    .width(76.dp)
                                    .semantics {
                                        contentDescription = focusContentDescription
                                    },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.spectrum_done)) }
        },
        modifier = Modifier.widthIn(max = 560.dp),
    )
}

@Composable
private fun SpectrumFocusedBadge(networkName: String) {
    val shape = MaterialTheme.shapes.extraSmall
    val focusedContentDescription = stringResource(
        R.string.spectrum_focused_network_cd,
        networkName,
    )
    Box(
        modifier = Modifier
            .background(ScannerCyan, shape)
            .border(1.dp, ScannerCyan, shape)
            .semantics { contentDescription = focusedContentDescription }
            .padding(horizontal = ScannerSpacing.Sm, vertical = ScannerSpacing.Xs),
    ) {
        Text(
            text = stringResource(R.string.spectrum_focused_badge),
            color = ScannerOnCyan,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
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
