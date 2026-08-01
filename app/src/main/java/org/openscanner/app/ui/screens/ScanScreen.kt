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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openscanner.app.NetworkUiModel
import org.openscanner.app.OpenScannerUiState
import org.openscanner.app.R
import org.openscanner.app.ui.components.AppHeader
import org.openscanner.app.ui.components.ChannelGroupSelector
import org.openscanner.app.ui.components.InformationBanner
import org.openscanner.app.ui.components.SignalGlyph
import org.openscanner.app.ui.displayLabel
import org.openscanner.app.ui.theme.ScannerBorder
import org.openscanner.app.ui.theme.ScannerCyan
import org.openscanner.app.ui.theme.ScannerIconWell
import org.openscanner.app.ui.theme.ScannerMuted
import org.openscanner.app.ui.theme.ScannerOnCyan
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
    val accessPointCountNoun = stringResource(
        if (visible.size == 1) R.string.scan_access_point else R.string.scan_access_points,
    )

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Md)) {
        AppHeader(
            eyebrow = stringResource(
                R.string.scan_header_eyebrow,
                visible.size,
                accessPointCountNoun,
                state.selectedChannelGroup.displayLabel(),
            ),
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
                Text(stringResource(R.string.scan_nearby_networks), color = ScannerText, style = MaterialTheme.typography.titleLarge)
                Text(
                    ageLabel(state.ageMs, state.likelyThrottled),
                    color = ScannerMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Sm)) {
                ToolbarButton(Icons.Rounded.Refresh, stringResource(R.string.scan_refresh_scan), onRefresh)
                ToolbarButton(
                    if (searchVisible) Icons.Rounded.Close else Icons.Rounded.Search,
                    stringResource(if (searchVisible) R.string.scan_close_search else R.string.scan_search_networks),
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
                label = { Text(stringResource(R.string.scan_search_hint)) },
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
                    stringResource(R.string.scan_empty_title, state.selectedChannelGroup.displayLabel()),
                    color = ScannerText,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = ScannerSpacing.Lg),
                )
                Text(
                    stringResource(R.string.scan_empty_subtitle),
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
            text = stringResource(
                if (state.privacyMode) R.string.scan_privacy_banner_on else R.string.scan_privacy_banner_off,
            ),
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
private fun StateBadge(
    text: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val shape = MaterialTheme.shapes.extraSmall
    Box(
        modifier = modifier
            .background(if (emphasized) ScannerCyan else Color.Transparent, shape)
            .border(1.dp, ScannerCyan, shape)
            .padding(horizontal = ScannerSpacing.Sm, vertical = ScannerSpacing.Xs),
    ) {
        Text(
            text,
            color = if (emphasized) ScannerOnCyan else ScannerCyan,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun NetworkRow(network: NetworkUiModel, onClick: () -> Unit) {
    val rowShape = MaterialTheme.shapes.small
    val securityLabel = network.securityTypes.displayLabel()
    val connectionHighlight = if (network.connected) {
        Modifier
            .background(ScannerIconWell, rowShape)
            .border(1.dp, ScannerCyan, rowShape)
    } else {
        Modifier
    }
    val rowDescription = buildString {
        append(stringResource(R.string.scan_row_description_base, network.name, network.signalDbm, network.channelGroup.displayLabel()))
        network.channel?.let { append(stringResource(R.string.scan_row_description_channel, it)) }
        append(stringResource(R.string.scan_row_description_security, securityLabel))
        if (network.connected) append(stringResource(R.string.scan_row_description_connected))
        if (network.selected) append(stringResource(R.string.scan_row_description_selected))
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(connectionHighlight)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    role = Role.Button
                    contentDescription = rowDescription
                }
                .padding(horizontal = ScannerSpacing.Xs, vertical = ScannerSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
        ) {
            SignalGlyph(network.signalDbm)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Xs)) {
                Text(
                    network.name,
                    color = ScannerText,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Sm),
                ) {
                    if (network.connected) {
                        StateBadge(stringResource(R.string.scan_badge_current_wifi), emphasized = true)
                    }
                    Text(
                        buildString {
                            append(network.channelGroup.displayLabel())
                            network.channel?.let { append(stringResource(R.string.scan_row_channel, it)) }
                            network.channelWidthMhz?.let { append(stringResource(R.string.scan_row_width, it)) }
                            append(stringResource(R.string.scan_row_security, securityLabel))
                        },
                        color = ScannerMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
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
                        stringResource(R.string.scan_dbm_unit),
                        color = ScannerMuted,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = ScannerSpacing.Xs),
                    )
                }
                if (network.selected) {
                    StateBadge(stringResource(R.string.scan_badge_tracking))
                }
            }
        }
        HorizontalDivider(color = ScannerBorder)
    }
}

@Composable
private fun ageLabel(ageMs: Long?, likelyThrottled: Boolean): String {
    val age = when {
        ageMs == null -> stringResource(R.string.scan_age_waiting)
        ageMs < 1_000L -> stringResource(R.string.scan_age_just_now)
        ageMs < 60_000L -> stringResource(R.string.scan_age_seconds, ageMs / 1_000L)
        else -> stringResource(R.string.scan_age_minutes, ageMs / 60_000L)
    }
    return if (likelyThrottled) stringResource(R.string.scan_age_cached, age) else age
}
