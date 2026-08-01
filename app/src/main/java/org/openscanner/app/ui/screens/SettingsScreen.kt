package org.openscanner.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openscanner.app.OpenScannerUiState
import org.openscanner.app.ui.components.AppHeader
import org.openscanner.app.ui.theme.ScannerAmber
import org.openscanner.app.ui.theme.ScannerBorder
import org.openscanner.app.ui.theme.ScannerCyan
import org.openscanner.app.ui.theme.ScannerIconWell
import org.openscanner.app.ui.theme.ScannerMuted
import org.openscanner.app.ui.theme.ScannerOnCyan
import org.openscanner.app.ui.theme.ScannerSpacing
import org.openscanner.app.ui.theme.ScannerSurface
import org.openscanner.app.ui.theme.ScannerSurfaceRaised
import org.openscanner.app.ui.theme.ScannerText

@Composable
fun SettingsScreen(
    state: OpenScannerUiState,
    onPrivacyChanged: (Boolean) -> Unit,
    onRefreshIntervalChanged: (Int) -> Unit,
    onOpenWifiSettings: () -> Unit,
    onResetSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var aboutOpen by remember { mutableStateOf(false) }
    var resetOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Sm),
    ) {
        AppHeader(eyebrow = "Privacy and display", phase = state.phase, freshness = state.freshness)
        SectionLabel("Privacy")
        ToggleRow(
            icon = Icons.Rounded.VisibilityOff,
            label = "Privacy mode",
            detail = "Mask network names and hardware addresses on screen",
            checked = state.privacyMode,
            enabled = true,
            onCheckedChange = onPrivacyChanged,
        )
        ToggleRow(
            icon = Icons.Rounded.Lock,
            label = "Redact exports",
            detail = "Locked on in version 0.1; every shared report is previewed",
            checked = true,
            enabled = false,
            onCheckedChange = {},
        )
        SectionLabel("Scanning")
        SettingsRow(
            icon = Icons.Rounded.Wifi,
            label = "Android Wi-Fi settings",
            detail = "Joining networks and passwords stay in the system UI",
            onClick = onOpenWifiSettings,
        )
        WifiScanThrottleStatusRow(state.capabilities.wifiScanThrottleEnabled)
        SettingsRow(
            icon = Icons.Rounded.Schedule,
            label = "Refresh request interval",
            detail = "Requested every ${state.refreshIntervalSeconds}s; Android may throttle or reuse results",
            onClick = {},
            trailing = null,
        )
        RefreshIntervalSelector(
            selectedSeconds = state.refreshIntervalSeconds,
            onSelect = onRefreshIntervalChanged,
        )
        SectionLabel("Project")
        SettingsRow(
            icon = Icons.Rounded.Info,
            label = "About Open Scanner",
            detail = "Version 0.1.0 · Apache License 2.0 · open source",
            onClick = { aboutOpen = true },
        )
        SettingsRow(
            icon = Icons.Rounded.RestartAlt,
            label = "Reset settings",
            detail = "Restore privacy and refresh preferences to defaults",
            onClick = { resetOpen = true },
        )
    }

    if (aboutOpen) {
        SettingsAlertDialog(
            title = "Open Scanner 0.1.0",
            onDismiss = { aboutOpen = false },
            icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
            text = {
                Text(
                    "A passive, local-first Wi-Fi analysis toolkit. No account, ads, telemetry, or Internet permission. " +
                        "Source is licensed under Apache License 2.0. Android scan freshness and hardware support vary by device.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = { TextButton(onClick = { aboutOpen = false }) { Text("Done") } },
        )
    }
    if (resetOpen) {
        SettingsAlertDialog(
            title = "Reset settings?",
            onDismiss = { resetOpen = false },
            text = {
                Text(
                    "This restores default privacy and refresh preferences. Scan history already remains memory-only.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    resetOpen = false
                    onResetSettings()
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { resetOpen = false }) { Text("Cancel") } },
        )
    }
}

/**
 * Token-styled dialog wrapper mirroring ToolAlertDialog in ToolsScreen: raised
 * surface, themed content colors, titleLarge title, no tonal elevation.
 */
@Composable
private fun SettingsAlertDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    text: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        containerColor = ScannerSurfaceRaised,
        titleContentColor = ScannerText,
        textContentColor = ScannerMuted,
        tonalElevation = 0.dp,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = ScannerMuted,
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = ScannerSpacing.Md, bottom = ScannerSpacing.Xs),
    )
}

/**
 * Segmented radio control mirroring [org.openscanner.app.ui.components.ChannelGroupSelector]:
 * bordered container, 1 dp dividers, cyan fill for the selected segment.
 */
@Composable
private fun RefreshIntervalSelector(
    selectedSeconds: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(10, 15, 30, 60)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ScannerSpacing.MinTouchTarget)
            .border(1.dp, ScannerBorder, MaterialTheme.shapes.small),
    ) {
        options.forEachIndexed { index, seconds ->
            val selected = seconds == selectedSeconds
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (selected) ScannerCyan else ScannerSurface)
                    .clickable(
                        role = Role.RadioButton,
                        interactionSource = interactionSource,
                        indication = null,
                    ) { onSelect(seconds) }
                    .semantics {
                        this.selected = selected
                        role = Role.RadioButton
                        contentDescription = buildString {
                            append("$seconds s")
                            if (selected) append(", selected")
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$seconds s",
                    color = if (selected) ScannerOnCyan else ScannerMuted,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
            if (index != options.lastIndex) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(ScannerBorder))
            }
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ScannerSpacing.MinTouchTarget + 30.dp)
            .padding(horizontal = ScannerSpacing.Sm, vertical = ScannerSpacing.Md - 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
    ) {
        SettingIcon(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Xs)) {
            Text(label, color = ScannerText, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = ScannerMuted, style = MaterialTheme.typography.labelSmall)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = ScannerCyan),
        )
    }
    HorizontalDivider(color = ScannerBorder)
}

@Composable
private fun WifiScanThrottleStatusRow(enabled: Boolean?) {
    val status = when (enabled) {
        true -> "ON"
        false -> "OFF"
        null -> "N/A"
    }
    val statusColor = when (enabled) {
        true -> ScannerAmber
        false -> ScannerCyan
        null -> ScannerMuted
    }
    val detail = when (enabled) {
        true -> "Disable for local testing: Developer options › Networking › Wi-Fi scan throttling. May increase battery use."
        false -> "Disabled in Developer options; Android may scan more often and use more battery."
        null -> "Status unavailable. On Android 10+, check Developer options › Networking › Wi-Fi scan throttling."
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ScannerSpacing.MinTouchTarget + 30.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Wi-Fi scan throttling: $status. $detail"
            }
            .padding(horizontal = ScannerSpacing.Sm, vertical = ScannerSpacing.Md - 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
    ) {
        SettingIcon(Icons.Rounded.Speed)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Xs)) {
            Text("Wi-Fi scan throttling", color = ScannerText, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = ScannerMuted, style = MaterialTheme.typography.labelSmall)
        }
        Text(
            status,
            color = statusColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .border(1.dp, statusColor, MaterialTheme.shapes.small)
                .padding(horizontal = ScannerSpacing.Sm, vertical = ScannerSpacing.Xs),
        )
    }
    HorizontalDivider(color = ScannerBorder)
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    detail: String,
    onClick: () -> Unit,
    trailing: ImageVector? = Icons.Rounded.ChevronRight,
) {
    val interactionModifier = if (trailing != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick).semantics { role = Role.Button }
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ScannerSpacing.MinTouchTarget + 30.dp)
            .then(interactionModifier)
            .padding(horizontal = ScannerSpacing.Sm, vertical = ScannerSpacing.Md - 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
    ) {
        SettingIcon(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Xs)) {
            Text(label, color = ScannerText, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = ScannerMuted, style = MaterialTheme.typography.labelSmall)
        }
        trailing?.let { Icon(it, contentDescription = null, tint = ScannerMuted) }
    }
    HorizontalDivider(color = ScannerBorder)
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    Box(Modifier.size(40.dp).background(ScannerIconWell, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = ScannerCyan, modifier = Modifier.size(24.dp))
    }
}
