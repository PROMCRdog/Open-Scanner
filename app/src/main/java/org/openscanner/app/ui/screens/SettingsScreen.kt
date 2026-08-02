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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openscanner.app.BuildConfig
import org.openscanner.app.AppLanguage
import org.openscanner.app.OpenScannerUiState
import org.openscanner.app.R
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
import org.openscanner.core.model.WifiRefreshIntervalPolicy

@Composable
fun SettingsScreen(
    state: OpenScannerUiState,
    onPrivacyChanged: (Boolean) -> Unit,
    onRedactExportsChanged: (Boolean) -> Unit,
    onRefreshIntervalChanged: (Int) -> Unit,
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onOpenWifiSettings: () -> Unit,
    onResetSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var aboutOpen by remember { mutableStateOf(false) }
    var resetOpen by remember { mutableStateOf(false) }
    var allowUnredactedOpen by remember { mutableStateOf(false) }
    var languageOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Sm),
    ) {
        AppHeader(
            eyebrow = stringResource(R.string.settings_eyebrow),
            phase = state.phase,
            freshness = state.freshness,
        )
        SectionLabel(stringResource(R.string.settings_section_privacy))
        ToggleRow(
            icon = Icons.Rounded.VisibilityOff,
            label = stringResource(R.string.settings_privacy_mode_label),
            detail = stringResource(R.string.settings_privacy_mode_detail),
            checked = state.privacyMode,
            enabled = true,
            onCheckedChange = onPrivacyChanged,
        )
        ToggleRow(
            icon = Icons.Rounded.Lock,
            label = stringResource(R.string.settings_redact_reports_label),
            detail = if (state.redactExports) {
                stringResource(R.string.settings_redact_reports_detail_on)
            } else {
                stringResource(R.string.settings_redact_reports_detail_off)
            },
            checked = state.redactExports,
            enabled = true,
            onCheckedChange = { enabled ->
                if (enabled) onRedactExportsChanged(true) else allowUnredactedOpen = true
            },
        )
        SectionLabel(stringResource(R.string.settings_section_display))
        SettingsRow(
            icon = Icons.Rounded.Language,
            label = stringResource(R.string.settings_language_label),
            detail = languageSummary(selectedLanguage),
            onClick = { languageOpen = true },
        )
        SectionLabel(stringResource(R.string.settings_section_scanning))
        SettingsRow(
            icon = Icons.Rounded.Wifi,
            label = stringResource(R.string.settings_android_wifi_label),
            detail = stringResource(R.string.settings_android_wifi_detail),
            onClick = onOpenWifiSettings,
        )
        WifiScanThrottleStatusRow(
            enabled = state.capabilities.wifiScanThrottleEnabled,
            resolved = state.capabilities.wifiScanThrottleStateResolved,
        )
        SettingsRow(
            icon = Icons.Rounded.Schedule,
            label = stringResource(R.string.settings_refresh_interval_label),
            detail = if (state.refreshIntervalSeconds == WifiRefreshIntervalPolicy.FAST_SECONDS) {
                stringResource(R.string.settings_refresh_interval_fast_detail)
            } else {
                stringResource(R.string.settings_refresh_interval_detail, state.refreshIntervalSeconds)
            },
            onClick = {},
            trailing = null,
        )
        RefreshIntervalSelector(
            selectedSeconds = state.refreshIntervalSeconds,
            wifiScanThrottleEnabled = state.capabilities.wifiScanThrottleEnabled,
            throttleStatusChecking = !state.capabilities.wifiScanThrottleStateResolved,
            onSelect = onRefreshIntervalChanged,
        )
        SectionLabel(stringResource(R.string.settings_section_project))
        SettingsRow(
            icon = Icons.Rounded.Info,
            label = stringResource(R.string.settings_about_label),
            detail = stringResource(R.string.settings_about_detail, BuildConfig.VERSION_NAME),
            onClick = { aboutOpen = true },
        )
        SettingsRow(
            icon = Icons.Rounded.RestartAlt,
            label = stringResource(R.string.settings_reset_label),
            detail = stringResource(R.string.settings_reset_detail),
            onClick = { resetOpen = true },
        )
    }

    if (aboutOpen) {
        SettingsAlertDialog(
            title = stringResource(R.string.settings_about_dialog_title, BuildConfig.VERSION_NAME),
            onDismiss = { aboutOpen = false },
            icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
            text = {
                Text(
                    stringResource(R.string.settings_about_dialog_text),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { aboutOpen = false }) { Text(stringResource(R.string.settings_done)) }
            },
        )
    }
    if (languageOpen) {
        SettingsAlertDialog(
            title = stringResource(R.string.settings_language_dialog_title),
            onDismiss = { languageOpen = false },
            icon = { Icon(Icons.Rounded.Language, contentDescription = null) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Xs)) {
                    AppLanguage.entries.forEach { language ->
                        LanguageOptionRow(
                            language = language,
                            selected = language == selectedLanguage,
                            onSelect = {
                                languageOpen = false
                                onLanguageSelected(language)
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { languageOpen = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }
    if (allowUnredactedOpen) {
        SettingsAlertDialog(
            title = stringResource(R.string.settings_unredacted_dialog_title),
            onDismiss = { allowUnredactedOpen = false },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = ScannerAmber) },
            text = {
                Text(
                    stringResource(R.string.settings_unredacted_dialog_text),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    allowUnredactedOpen = false
                    onRedactExportsChanged(false)
                }) { Text(stringResource(R.string.settings_allow_unredacted), color = ScannerAmber) }
            },
            dismissButton = {
                TextButton(onClick = { allowUnredactedOpen = false }) { Text(stringResource(R.string.settings_keep_redaction_on)) }
            },
        )
    }
    if (resetOpen) {
        SettingsAlertDialog(
            title = stringResource(R.string.settings_reset_dialog_title),
            onDismiss = { resetOpen = false },
            text = {
                Text(
                    stringResource(R.string.settings_reset_dialog_text),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    resetOpen = false
                    onResetSettings()
                }) { Text(stringResource(R.string.settings_reset)) }
            },
            dismissButton = { TextButton(onClick = { resetOpen = false }) { Text(stringResource(R.string.settings_cancel)) } },
        )
    }
}

@Composable
private fun languageSummary(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM_DEFAULT -> stringResource(R.string.settings_language_system_default_summary)
    AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
    AppLanguage.SIMPLIFIED_CHINESE -> stringResource(R.string.settings_language_simplified_chinese)
}

@Composable
private fun LanguageOptionRow(
    language: AppLanguage,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val label = when (language) {
        AppLanguage.SYSTEM_DEFAULT -> stringResource(R.string.settings_language_system_default)
        AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
        AppLanguage.SIMPLIFIED_CHINESE -> stringResource(R.string.settings_language_simplified_chinese)
    }
    val detail = when (language) {
        AppLanguage.SYSTEM_DEFAULT -> stringResource(R.string.settings_language_system_default_detail)
        AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english_detail)
        AppLanguage.SIMPLIFIED_CHINESE -> stringResource(R.string.settings_language_simplified_chinese_detail)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ScannerSpacing.MinTouchTarget + 12.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = ScannerSpacing.Sm, vertical = ScannerSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = ScannerCyan),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Xs)) {
            Text(label, color = ScannerText, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = ScannerMuted, style = MaterialTheme.typography.labelSmall)
        }
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
    wifiScanThrottleEnabled: Boolean?,
    throttleStatusChecking: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fastModeNote = when {
        wifiScanThrottleEnabled == false -> stringResource(R.string.settings_refresh_fast_available)
        wifiScanThrottleEnabled == true -> stringResource(R.string.settings_refresh_fast_requires_throttling_off)
        throttleStatusChecking -> stringResource(R.string.settings_refresh_fast_checking)
        else -> stringResource(R.string.settings_refresh_fast_unavailable)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ScannerSpacing.MinTouchTarget)
                .border(1.dp, ScannerBorder, MaterialTheme.shapes.small),
        ) {
            WifiRefreshIntervalPolicy.SUPPORTED_SECONDS.forEachIndexed { index, seconds ->
                val selected = seconds == selectedSeconds
                val enabled = WifiRefreshIntervalPolicy.isSelectable(seconds, wifiScanThrottleEnabled)
                val interactionSource = remember { MutableInteractionSource() }
                val intervalLabel = stringResource(R.string.settings_refresh_interval_seconds, seconds)
                val segmentContentDescription = when {
                    seconds == WifiRefreshIntervalPolicy.FAST_SECONDS && !enabled ->
                        stringResource(R.string.settings_refresh_interval_fast_disabled_cd, fastModeNote)
                    selected -> stringResource(R.string.settings_refresh_interval_seconds_selected, seconds)
                    else -> intervalLabel
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (selected) ScannerCyan else ScannerSurface)
                        .clickable(
                            enabled = enabled,
                            role = Role.RadioButton,
                            interactionSource = interactionSource,
                            indication = null,
                        ) { onSelect(seconds) }
                        .semantics {
                            this.selected = selected
                            role = Role.RadioButton
                            contentDescription = segmentContentDescription
                            if (!enabled) disabled()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        intervalLabel,
                        color = when {
                            selected -> ScannerOnCyan
                            enabled -> ScannerMuted
                            else -> ScannerMuted.copy(alpha = 0.5f)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                if (index != WifiRefreshIntervalPolicy.SUPPORTED_SECONDS.lastIndex) {
                    Box(Modifier.width(1.dp).fillMaxHeight().background(ScannerBorder))
                }
            }
        }
        Text(
            text = fastModeNote,
            color = if (wifiScanThrottleEnabled == false) ScannerCyan else ScannerMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = ScannerSpacing.Sm),
        )
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
            modifier = Modifier.semantics { contentDescription = label },
            colors = SwitchDefaults.colors(checkedTrackColor = ScannerCyan),
        )
    }
    HorizontalDivider(color = ScannerBorder)
}

@Composable
private fun WifiScanThrottleStatusRow(enabled: Boolean?, resolved: Boolean) {
    val status = when {
        !resolved -> stringResource(R.string.settings_status_checking)
        enabled == true -> stringResource(R.string.settings_status_on)
        enabled == false -> stringResource(R.string.settings_status_off)
        else -> stringResource(R.string.settings_status_na)
    }
    val statusColor = when {
        !resolved -> ScannerMuted
        enabled == true -> ScannerAmber
        enabled == false -> ScannerCyan
        else -> ScannerMuted
    }
    val detail = when {
        !resolved -> stringResource(R.string.settings_wifi_scan_throttling_detail_checking)
        enabled == true -> stringResource(R.string.settings_wifi_scan_throttling_detail_on)
        enabled == false -> stringResource(R.string.settings_wifi_scan_throttling_detail_off)
        else -> stringResource(R.string.settings_wifi_scan_throttling_detail_unavailable)
    }
    val throttleLabel = stringResource(R.string.settings_wifi_scan_throttling_label)
    val rowContentDescription = stringResource(R.string.settings_wifi_scan_throttling_cd, status, detail)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ScannerSpacing.MinTouchTarget + 30.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = rowContentDescription
            }
            .padding(horizontal = ScannerSpacing.Sm, vertical = ScannerSpacing.Md - 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
    ) {
        SettingIcon(Icons.Rounded.Speed)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Xs)) {
            Text(throttleLabel, color = ScannerText, style = MaterialTheme.typography.titleMedium)
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
