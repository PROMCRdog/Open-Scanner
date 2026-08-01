package org.openscanner.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openscanner.app.NetworkUiModel
import org.openscanner.app.OpenScannerUiState
import org.openscanner.app.ui.components.AppHeader
import org.openscanner.app.ui.components.ChannelGroupSelector
import org.openscanner.app.ui.components.InformationBanner
import org.openscanner.app.ui.components.SignalGlyph
import org.openscanner.app.ui.theme.ScannerBorder
import org.openscanner.app.ui.theme.ScannerCyan
import org.openscanner.app.ui.theme.ScannerIconWell
import org.openscanner.app.ui.theme.ScannerMuted
import org.openscanner.app.ui.theme.ScannerSpacing
import org.openscanner.app.ui.theme.ScannerSurface
import org.openscanner.app.ui.theme.ScannerText
import org.openscanner.core.model.WifiChannelGroup

@Composable
fun ScanScreen(
    state: OpenScannerUiState,
    onChannelGroupSelected: (WifiChannelGroup) -> Unit,
    onRefresh: () -> Unit,
    onSelectNetwork: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchVisible by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val visible = state.networks
        .asSequence()
        .filter { it.channelGroup == state.selectedChannelGroup }
        .filter {
            query.isBlank() || it.name.contains(query, ignoreCase = true) ||
                it.bssid.contains(query, ignoreCase = true)
        }
        .toList()
    val accessPointCountNoun = if (visible.size == 1) "access point" else "access points"

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Md)) {
        AppHeader(
            eyebrow = "${visible.size} $accessPointCountNoun · ${state.selectedChannelGroup.label}",
            phase = state.phase,
            freshness = state.freshness,
        )
        ChannelGroupSelector(
            state.channelGroups,
            state.selectedChannelGroup,
            onChannelGroupSelected,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Nearby networks", color = ScannerText, style = MaterialTheme.typography.titleLarge)
                Text(
                    ageLabel(state.ageMs, state.likelyThrottled),
                    color = ScannerMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Sm)) {
                ToolbarButton(Icons.Rounded.Refresh, "Refresh scan", onRefresh)
                ToolbarButton(
                    if (searchVisible) Icons.Rounded.Close else Icons.Rounded.Search,
                    if (searchVisible) "Close search" else "Search networks",
                ) {
                    searchVisible = !searchVisible
                    if (!searchVisible) query = ""
                }
            }
        }
        if (searchVisible) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = MaterialTheme.shapes.small,
                label = { Text("SSID or BSSID") },
                leadingIcon = { Icon(Icons.Rounded.FilterList, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ScannerText,
                    unfocusedTextColor = ScannerText,
                    cursorColor = ScannerCyan,
                    focusedBorderColor = ScannerCyan,
                    unfocusedBorderColor = ScannerBorder,
                    focusedLabelColor = ScannerCyan,
                    unfocusedLabelColor = ScannerMuted,
                    focusedLeadingIconColor = ScannerCyan,
                    unfocusedLeadingIconColor = ScannerMuted,
                ),
            )
        }
        if (visible.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(ScannerIconWell, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Wifi,
                        contentDescription = null,
                        tint = ScannerCyan,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Text(
                    "No ${state.selectedChannelGroup.label} networks in this snapshot",
                    color = ScannerText,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = ScannerSpacing.Lg),
                )
                Text(
                    "Try another band or request a refresh. Android may return cached results.",
                    color = ScannerMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = ScannerSpacing.Sm),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                items(visible, key = { it.uiId }) { network ->
                    NetworkRow(network = network, onClick = { onSelectNetwork(network.uiId) })
                }
            }
        }
        InformationBanner(
            icon = Icons.Rounded.Lock,
            text = if (state.privacyMode) {
                "Privacy mode is masking network names and addresses on screen."
            } else {
                "Identifiers stay on this device and are redacted from exports by default."
            },
        )
    }
}

@Composable
private fun ToolbarButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(ScannerSpacing.MinTouchTarget)
            .background(ScannerSurface, MaterialTheme.shapes.small)
            .border(1.dp, ScannerBorder, MaterialTheme.shapes.small)
            .semantics { contentDescription = description },
    ) {
        Icon(icon, contentDescription = null, tint = ScannerCyan)
    }
}

@Composable
private fun StateBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, ScannerCyan, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = ScannerSpacing.Sm, vertical = ScannerSpacing.Xs),
    ) {
        Text(text, color = ScannerCyan, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun NetworkRow(network: NetworkUiModel, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    role = Role.Button
                    contentDescription = buildString {
                        append("${network.name}, ${network.signalDbm} dBm, ${network.channelGroup.label}")
                        network.channel?.let { append(", channel $it") }
                        append(", ${network.security}")
                        if (network.connected) append(", connected")
                        if (network.selected) append(", selected for tracking")
                    }
                }
                .padding(horizontal = ScannerSpacing.Xs, vertical = ScannerSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
        ) {
            SignalGlyph(network.signalDbm)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Xs)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Sm),
                ) {
                    Text(
                        network.name,
                        color = ScannerText,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (network.connected) {
                        StateBadge("CONNECTED")
                    }
                }
                Text(
                    buildString {
                        append(network.channelGroup.label)
                        network.channel?.let { append(" · Ch $it") }
                        network.channelWidthMhz?.let { append(" · ${it} MHz") }
                        append(" · ${network.security}")
                    },
                    color = ScannerMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Xs),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${network.signalDbm}",
                        color = ScannerCyan,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        " dBm",
                        color = ScannerMuted,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = ScannerSpacing.Xs),
                    )
                }
                if (network.selected) {
                    StateBadge("TRACKING")
                }
            }
        }
        HorizontalDivider(color = ScannerBorder)
    }
}

private fun ageLabel(ageMs: Long?, likelyThrottled: Boolean): String {
    val age = when {
        ageMs == null -> "Waiting for scan"
        ageMs < 1_000L -> "Updated just now"
        ageMs < 60_000L -> "Updated ${ageMs / 1_000L}s ago"
        else -> "Updated ${ageMs / 60_000L}m ago"
    }
    return if (likelyThrottled) "$age · cached results likely" else age
}
