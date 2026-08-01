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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openscanner.app.NetworkUiModel
import org.openscanner.app.OpenScannerUiState
import org.openscanner.app.ui.components.AppHeader
import org.openscanner.app.ui.components.InformationBanner
import org.openscanner.app.ui.components.PrimaryAction
import org.openscanner.app.ui.components.SignalGlyph
import org.openscanner.app.ui.components.SignalHistoryChart
import org.openscanner.app.ui.theme.ScannerBorder
import org.openscanner.app.ui.theme.ScannerCyan
import org.openscanner.app.ui.theme.ScannerIconWell
import org.openscanner.app.ui.theme.ScannerMuted
import org.openscanner.app.ui.theme.ScannerSpacing
import org.openscanner.app.ui.theme.ScannerSurface
import org.openscanner.app.ui.theme.ScannerSurfaceRaised
import org.openscanner.app.ui.theme.ScannerText
import org.openscanner.core.domain.SignalClassifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(
    state: OpenScannerUiState,
    onSelectNetwork: (String) -> Unit,
    onPauseChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val network = state.selectedNetwork

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
    ) {
        AppHeader(phase = state.phase, freshness = state.freshness)
        if (network == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ScannerSpacing.Xxl + ScannerSpacing.Lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
            ) {
                Box(
                    modifier = Modifier.size(80.dp).background(ScannerIconWell, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Wifi, contentDescription = null, tint = ScannerCyan, modifier = Modifier.size(40.dp))
                }
                Text("No access point selected", color = ScannerText, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Open Scan and choose a network to track.",
                    color = ScannerMuted,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            SelectedNetworkCard(network = network, onClick = { pickerOpen = true })
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = ScannerSpacing.Sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Sm)) {
                    Text(
                        "${network.signalDbm}",
                        color = ScannerText,
                        style = MaterialTheme.typography.displayMedium,
                    )
                    Text(
                        "dBm",
                        color = ScannerMuted,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = ScannerSpacing.Sm),
                    )
                }
                Text(
                    SignalClassifier.classify(network.signalDbm).label,
                    color = ScannerCyan,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            SignalHistoryChart(
                history = state.signalHistory,
                latestDbm = network.signalDbm,
                nowElapsedMs = state.historyNowElapsedMs,
            )
            EvidenceStrip(network, state.ageMs)
            InformationBanner(
                icon = if (state.selectedStability.label == "Steady") {
                    Icons.Rounded.CheckCircle
                } else {
                    Icons.Rounded.Info
                },
                text = stabilityDescription(state),
                positive = state.selectedStability.label == "Steady",
            )
            PrimaryAction(
                label = if (state.paused) "Resume tracking" else "Pause tracking",
                onClick = { onPauseChanged(!state.paused) },
            )
            TextButton(
                onClick = { pickerOpen = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = ScannerSpacing.MinTouchTarget),
            ) {
                Text("Choose network", color = ScannerCyan, style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (pickerOpen) {
        val candidates = state.networks.sortedByDescending { it.signalDbm }.take(30)
        ModalBottomSheet(
            onDismissRequest = { pickerOpen = false },
            containerColor = ScannerSurfaceRaised,
            contentColor = ScannerText,
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = ScannerSpacing.Xl)) {
                Text(
                    "Choose network",
                    color = ScannerText,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = ScannerSpacing.Xl),
                )
                Text(
                    "Track one access point without exposing its identifier outside this device.",
                    color = ScannerMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = ScannerSpacing.Xl, vertical = ScannerSpacing.Sm),
                )
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                    items(candidates, key = { it.uiId }) { candidate ->
                        HorizontalDivider(color = ScannerBorder)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = ScannerSpacing.MinTouchTarget + ScannerSpacing.Xl)
                                .background(if (candidate.selected) ScannerIconWell else ScannerSurfaceRaised)
                                .selectable(
                                    selected = candidate.selected,
                                    role = Role.RadioButton,
                                ) {
                                    onSelectNetwork(candidate.uiId)
                                    pickerOpen = false
                                }
                                .semantics {
                                    role = Role.RadioButton
                                    selected = candidate.selected
                                }
                                .padding(horizontal = ScannerSpacing.Lg, vertical = ScannerSpacing.Sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
                        ) {
                            SignalGlyph(candidate.signalDbm)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    candidate.name,
                                    color = ScannerText,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${candidate.channelGroup.label} · Ch ${candidate.channel ?: "—"}",
                                    color = ScannerMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            Text(
                                "${candidate.signalDbm} dBm",
                                color = ScannerMuted,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun stabilityDescription(state: OpenScannerUiState): String {
    val stability = state.selectedStability
    if (!stability.sufficient) {
        return "Stability: insufficient history. ${stability.presentSnapshots} of " +
            "${stability.assessedSnapshots} assessed snapshots contain this AP; at least four recent snapshots are needed."
    }
    return buildString {
        append("Stability: ${stability.label.lowercase()}. ")
        append("RSSI range ${stability.rssiRangeDb ?: 0} dB; ")
        append("present in ${stability.presentSnapshots} of ${stability.assessedSnapshots} recent snapshots")
        stability.absentPercent?.let { append(" ($it% absent)") }
        append(". This describes recent scan consistency, not connection quality.")
    }
}

@Composable
private fun SelectedNetworkCard(network: NetworkUiModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .border(1.dp, ScannerBorder, MaterialTheme.shapes.small)
            .background(ScannerSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "Choose network. Currently ${network.name}"
            }
            .padding(horizontal = ScannerSpacing.Lg, vertical = ScannerSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
    ) {
        SignalGlyph(network.signalDbm)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Xs)) {
            Text(network.name, color = ScannerText, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${network.channelGroup.label} · Channel ${network.channel ?: "unknown"} · ${network.signalDbm} dBm",
                color = ScannerMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = ScannerCyan, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun EvidenceStrip(network: NetworkUiModel, ageMs: Long?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(1.dp, ScannerBorder, MaterialTheme.shapes.small)
            .background(ScannerSurface),
    ) {
        EvidenceCell("Channel", network.channel?.toString() ?: "—", Modifier.weight(1f))
        Box(Modifier.width(1.dp).fillMaxHeight().background(ScannerBorder))
        EvidenceCell("Width", network.channelWidthMhz?.let { "$it MHz" } ?: "Unknown", Modifier.weight(1f))
        Box(Modifier.width(1.dp).fillMaxHeight().background(ScannerBorder))
        EvidenceCell("Updated", compactAge(ageMs), Modifier.weight(1f))
    }
}

@Composable
private fun EvidenceCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = ScannerSpacing.Xs, vertical = ScannerSpacing.Md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Xs),
    ) {
        Text(label, color = ScannerMuted, style = MaterialTheme.typography.labelSmall)
        Text(value, color = ScannerText, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, maxLines = 1)
    }
}

private fun compactAge(ageMs: Long?): String = when {
    ageMs == null -> "—"
    ageMs < 1_000L -> "Now"
    ageMs < 60_000L -> "${ageMs / 1_000L}s"
    else -> "${ageMs / 60_000L}m"
}
