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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openscanner.app.R
import org.openscanner.app.OpenScannerUiState
import org.openscanner.app.ui.components.AppHeader
import org.openscanner.app.ui.components.InformationBanner
import org.openscanner.app.ui.components.PrimaryAction
import org.openscanner.app.ui.displayDescription
import org.openscanner.app.ui.displayLabel
import org.openscanner.app.ui.displayName
import org.openscanner.app.ui.displayNetworkName
import org.openscanner.app.ui.theme.ScannerAmber
import org.openscanner.app.ui.theme.ScannerBorder
import org.openscanner.app.ui.theme.ScannerCyan
import org.openscanner.app.ui.theme.ScannerIconWell
import org.openscanner.app.ui.theme.ScannerMuted
import org.openscanner.app.ui.theme.ScannerSpacing
import org.openscanner.app.ui.theme.ScannerSurface
import org.openscanner.app.ui.theme.ScannerSurfaceRaised
import org.openscanner.app.ui.theme.ScannerText
import org.openscanner.core.export.ExportFormat
import org.openscanner.core.export.WifiLogField
import org.openscanner.core.export.WifiLogFieldCategory
import org.openscanner.core.export.WifiLogFormat
import org.openscanner.core.export.WifiLogRecorder
import org.openscanner.core.export.WifiLogStopReason
import org.openscanner.core.model.ScannerPhase
import org.openscanner.core.model.SecurityType
import org.openscanner.core.model.WifiChannelGroup

@Composable
fun ToolsScreen(
    state: OpenScannerUiState,
    onPrepareSnapshotExport: (ExportFormat) -> Unit,
    onLogFieldChanged: (WifiLogField, Boolean) -> Unit,
    onSetAllLogFields: (Boolean) -> Unit,
    onStartLogging: () -> Unit,
    onStopLogging: () -> Unit,
    onClearLog: () -> Unit,
    onPrepareLogExport: (WifiLogFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf<ToolDialog?>(null) }
    var snapshotExportChooser by remember { mutableStateOf(false) }
    var logExportChooser by remember { mutableStateOf(false) }
    var fieldChooser by remember { mutableStateOf(false) }
    var replaceLogConfirm by remember { mutableStateOf(false) }
    var clearLogConfirm by remember { mutableStateOf(false) }
    val status = when {
        state.phase == ScannerPhase.PAUSED -> stringResource(R.string.tools_status_paused)
        state.phase != ScannerPhase.LIVE -> stringResource(R.string.tools_status_scan_unavailable)
        !state.connection.connected -> stringResource(R.string.tools_status_no_connection)
        state.connection.captivePortal == true -> stringResource(R.string.tools_status_captive_portal)
        state.connection.validated == true -> stringResource(R.string.tools_status_validated)
        state.connection.validated == false -> stringResource(R.string.tools_status_not_validated)
        else -> stringResource(R.string.tools_status_validation_unavailable)
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
    ) {
        AppHeader(eyebrow = stringResource(R.string.tools_eyebrow), phase = state.phase, freshness = state.freshness)
        InformationBanner(
            icon = if (state.phase == ScannerPhase.LIVE && state.connection.connected) {
                Icons.Rounded.WifiTethering
            } else {
                Icons.Rounded.Info
            },
            text = status,
            positive = state.phase == ScannerPhase.LIVE && state.connection.validated == true,
        )
        WifiLoggingPanel(
            state = state,
            onConfigure = { fieldChooser = true },
            onStart = {
                if (state.logging.hasSession) replaceLogConfirm = true else onStartLogging()
            },
            onStop = onStopLogging,
            onExport = { logExportChooser = true },
            onClear = { clearLogConfirm = true },
        )
        Column(Modifier.fillMaxWidth()) {
            ToolRow(
                icon = Icons.Rounded.WifiTethering,
                label = stringResource(R.string.tools_connection_evidence_label),
                detail = stringResource(R.string.tools_connection_evidence_detail),
            ) { dialog = ToolDialog.CONNECTION }
            ToolRow(
                icon = Icons.Rounded.Security,
                label = stringResource(R.string.tools_security_summary_label),
                detail = stringResource(R.string.tools_security_summary_detail),
            ) { dialog = ToolDialog.SECURITY }
            ToolRow(
                icon = Icons.Rounded.Info,
                label = stringResource(R.string.tools_neighborhood_posture_label),
                detail = stringResource(R.string.tools_neighborhood_posture_detail),
            ) { dialog = ToolDialog.NEIGHBORHOOD_POSTURE }
            ToolRow(
                icon = Icons.Rounded.Share,
                label = stringResource(R.string.tools_export_snapshot_label),
                detail = if (state.snapshotSequence == null) {
                    stringResource(R.string.tools_export_snapshot_requires_scan)
                } else {
                    if (state.redactExports) {
                        stringResource(R.string.tools_export_snapshot_redacted)
                    } else {
                        stringResource(R.string.tools_export_snapshot_unredacted)
                    }
                },
            ) {
                if (state.snapshotSequence == null) dialog = ToolDialog.NO_SNAPSHOT else snapshotExportChooser = true
            }
            ToolRow(
                icon = Icons.Rounded.Description,
                label = stringResource(R.string.tools_measurement_limits_label),
                detail = stringResource(R.string.tools_measurement_limits_detail),
            ) { dialog = ToolDialog.MEASUREMENT_LIMITS }
        }
        InformationBanner(
            icon = Icons.Rounded.Lock,
            text = stringResource(R.string.tools_privacy_banner),
            modifier = Modifier.padding(top = ScannerSpacing.Sm),
        )
    }

    dialog?.let { dialogKind ->
        val (title, body) = dialogContent(dialogKind, state)
        ToolAlertDialog(
            title = title,
            onDismiss = { dialog = null },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.tools_done)) } },
            text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        )
    }
    if (snapshotExportChooser) {
        FormatChooserDialog(
            title = stringResource(R.string.tools_choose_snapshot_format),
            explanation = if (state.redactExports) {
                stringResource(R.string.tools_snapshot_format_redacted)
            } else {
                stringResource(R.string.tools_snapshot_format_unredacted)
            },
            labels = ExportFormat.entries.map { it.displayLabel() },
            onChoose = { index ->
                snapshotExportChooser = false
                onPrepareSnapshotExport(ExportFormat.entries[index])
            },
            onDismiss = { snapshotExportChooser = false },
        )
    }
    if (logExportChooser) {
        FormatChooserDialog(
            title = stringResource(R.string.tools_choose_log_format),
            explanation = if (state.logging.redacted == false) {
                stringResource(R.string.tools_log_format_unredacted)
            } else {
                stringResource(R.string.tools_log_format_redacted)
            },
            labels = WifiLogFormat.entries.map { it.displayLabel() },
            onChoose = { index ->
                logExportChooser = false
                onPrepareLogExport(WifiLogFormat.entries[index])
            },
            onDismiss = { logExportChooser = false },
        )
    }
    if (fieldChooser) {
        LogFieldChooser(
            selected = state.logging.selectedFields,
            redacted = state.redactExports,
            onFieldChanged = onLogFieldChanged,
            onSetAll = onSetAllLogFields,
            onDismiss = { fieldChooser = false },
        )
    }
    if (replaceLogConfirm) {
        ToolAlertDialog(
            title = stringResource(R.string.tools_replace_log_title),
            onDismiss = { replaceLogConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    replaceLogConfirm = false
                    onStartLogging()
                }) { Text(stringResource(R.string.tools_replace_and_start), color = ScannerAmber) }
            },
            dismissButton = {
                TextButton(onClick = { replaceLogConfirm = false }) { Text(stringResource(R.string.tools_cancel)) }
            },
            text = {
                Text(
                    stringResource(R.string.tools_replace_log_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
    }
    if (clearLogConfirm) {
        ToolAlertDialog(
            title = stringResource(R.string.tools_clear_log_title),
            onDismiss = { clearLogConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    clearLogConfirm = false
                    onClearLog()
                }) { Text(stringResource(R.string.tools_clear), color = ScannerAmber) }
            },
            dismissButton = {
                TextButton(onClick = { clearLogConfirm = false }) { Text(stringResource(R.string.tools_cancel)) }
            },
            text = {
                Text(stringResource(R.string.tools_clear_log_body), style = MaterialTheme.typography.bodyMedium)
            },
        )
    }
}

@Composable
private fun WifiLoggingPanel(
    state: OpenScannerUiState,
    onConfigure: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
) {
    val logging = state.logging
    val status = when {
        logging.active -> stringResource(R.string.tools_log_status_recording)
        logging.stopReason == WifiLogStopReason.SAFETY_LIMIT -> stringResource(R.string.tools_log_status_limit_reached)
        logging.hasSession -> stringResource(R.string.tools_log_status_ready_to_export)
        else -> stringResource(R.string.tools_log_status_not_started)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ScannerBorder, MaterialTheme.shapes.small)
            .background(ScannerSurface, MaterialTheme.shapes.small)
            .padding(ScannerSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tools_log_title), color = ScannerText, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (logging.active) {
                        if (logging.redacted == false) {
                            stringResource(R.string.tools_log_fields_active_unredacted, logging.recordedFields.size)
                        } else {
                            stringResource(R.string.tools_log_fields_active_redacted, logging.recordedFields.size)
                        }
                    } else if (logging.hasSession) {
                        stringResource(
                            R.string.tools_log_fields_session,
                            logging.recordedFields.size,
                            stringResource(
                                if (logging.redacted == false) R.string.tools_unredacted else R.string.tools_redacted,
                            ),
                            logging.selectedFields.size,
                        )
                    } else {
                        stringResource(
                            R.string.tools_log_fields_selected,
                            logging.selectedFields.size,
                            WifiLogField.entries.size,
                        )
                    },
                    color = ScannerMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                status,
                color = if (logging.active) ScannerAmber else ScannerCyan,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        if (logging.hasSession) {
            Text(
                stringResource(
                    R.string.tools_log_records,
                    logging.recordCount,
                    logging.networkRowCount,
                    formatDuration(logging.durationMs),
                ),
                color = ScannerText,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            if (logging.active) {
                stringResource(R.string.tools_log_description_active)
            } else {
                stringResource(
                    R.string.tools_log_description_inactive,
                    stringResource(if (state.redactExports) R.string.tools_redacted else R.string.tools_unredacted),
                    WifiLogRecorder.MAX_RECORDS,
                    WifiLogRecorder.MAX_NETWORK_ROWS,
                )
            },
            color = ScannerMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        PrimaryAction(
            label = stringResource(if (logging.active) R.string.tools_stop_logging else R.string.tools_start_logging),
            onClick = if (logging.active) onStop else onStart,
            enabled = logging.active || (logging.canStart && state.snapshotSequence != null),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TextButton(
                onClick = onConfigure,
                enabled = !logging.active,
                modifier = Modifier.heightIn(min = ScannerSpacing.MinTouchTarget),
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = null)
                Text(stringResource(R.string.tools_fields), modifier = Modifier.padding(start = ScannerSpacing.Xs))
            }
            TextButton(
                onClick = onExport,
                enabled = logging.canExport,
                modifier = Modifier.heightIn(min = ScannerSpacing.MinTouchTarget),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Text(stringResource(R.string.tools_export), modifier = Modifier.padding(start = ScannerSpacing.Xs))
            }
            TextButton(
                onClick = onClear,
                enabled = logging.hasSession && !logging.active,
                modifier = Modifier.heightIn(min = ScannerSpacing.MinTouchTarget),
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
                Text(stringResource(R.string.tools_clear), modifier = Modifier.padding(start = ScannerSpacing.Xs))
            }
        }
        if (!logging.active && state.snapshotSequence == null) {
            Text(
                stringResource(R.string.tools_log_need_scan),
                color = ScannerAmber,
                style = MaterialTheme.typography.labelMedium,
            )
        } else if (!logging.active && !logging.canStart) {
            Text(
                stringResource(R.string.tools_log_need_fields),
                color = ScannerAmber,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun LogFieldChooser(
    selected: Set<WifiLogField>,
    redacted: Boolean,
    onFieldChanged: (WifiLogField, Boolean) -> Unit,
    onSetAll: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ToolAlertDialog(
        title = stringResource(R.string.tools_logging_fields_title),
        onDismiss = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.tools_done)) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Sm)) {
                Text(
                    stringResource(
                        if (redacted) {
                            R.string.tools_logging_fields_hint_redacted
                        } else {
                            R.string.tools_logging_fields_hint_unredacted
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { onSetAll(true) }) { Text(stringResource(R.string.tools_select_all)) }
                    TextButton(onClick = { onSetAll(false) }) { Text(stringResource(R.string.tools_clear_all)) }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .border(1.dp, ScannerBorder, MaterialTheme.shapes.small),
                ) {
                    WifiLogFieldCategory.entries.forEach { category ->
                        item(key = "header-${category.name}") {
                            Text(
                                category.displayLabel().uppercase(),
                                color = ScannerCyan,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(
                                    start = ScannerSpacing.Md,
                                    end = ScannerSpacing.Md,
                                    top = ScannerSpacing.Lg,
                                    bottom = ScannerSpacing.Sm,
                                ),
                            )
                        }
                        items(
                            items = WifiLogField.entries.filter { it.category == category },
                            key = { it.name },
                        ) { field ->
                            val fieldLabel = field.displayLabel()
                            val fieldDescription = field.displayDescription()
                            val checked = field in selected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = ScannerSpacing.MinTouchTarget + ScannerSpacing.Sm)
                                    .toggleable(
                                        value = checked,
                                        role = Role.Checkbox,
                                        onValueChange = { onFieldChanged(field, it) },
                                    )
                                    .semantics { contentDescription = "$fieldLabel. $fieldDescription" }
                                    .padding(horizontal = ScannerSpacing.Md, vertical = ScannerSpacing.Sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = ScannerCyan),
                                )
                                Column(Modifier.weight(1f).padding(start = ScannerSpacing.Sm)) {
                                    Text(fieldLabel, color = ScannerText, style = MaterialTheme.typography.bodyMedium)
                                    Text(fieldDescription, color = ScannerMuted, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            HorizontalDivider(color = ScannerBorder)
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun FormatChooserDialog(
    title: String,
    explanation: String,
    labels: List<String>,
    onChoose: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ToolAlertDialog(
        title = title,
        onDismiss = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.tools_cancel)) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Sm)) {
                Text(explanation, style = MaterialTheme.typography.bodyMedium)
                labels.forEachIndexed { index, label ->
                    TextButton(
                        onClick = { onChoose(index) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = ScannerSpacing.MinTouchTarget)
                            .border(1.dp, ScannerBorder, MaterialTheme.shapes.small),
                        shape = MaterialTheme.shapes.small,
                    ) { Text(label) }
                }
            }
        },
    )
}

@Composable
private fun ToolAlertDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    text: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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

private enum class ToolDialog {
    CONNECTION,
    SECURITY,
    NEIGHBORHOOD_POSTURE,
    NO_SNAPSHOT,
    MEASUREMENT_LIMITS,
}

@Composable
private fun dialogContent(dialog: ToolDialog, state: OpenScannerUiState): Pair<String, String> = when (dialog) {
    ToolDialog.CONNECTION -> stringResource(R.string.tools_connection_evidence_label) to connectionDetails(state)
    ToolDialog.SECURITY -> {
        val network = state.selectedNetwork
        stringResource(R.string.tools_security_summary_label) to if (network == null) {
            stringResource(R.string.tools_security_no_network)
        } else {
            stringResource(R.string.tools_security_body, network.displayName(), network.securityTypes.displayLabel())
        }
    }
    ToolDialog.NEIGHBORHOOD_POSTURE ->
        stringResource(R.string.tools_neighborhood_posture_label) to neighborhoodPostureDetails(state)
    ToolDialog.NO_SNAPSHOT -> stringResource(R.string.tools_no_snapshot_title) to
        stringResource(
            R.string.tools_no_snapshot_body,
            stringResource(if (state.redactExports) R.string.tools_redacted else R.string.tools_unredacted),
        )
    ToolDialog.MEASUREMENT_LIMITS -> stringResource(R.string.tools_measurement_limits_label) to
        stringResource(R.string.tools_measurement_limits_body)
}

@Composable
private fun neighborhoodPostureDetails(state: OpenScannerUiState): String {
    val posture = state.neighborhoodPosture
    if (state.snapshotSequence == null) {
        return stringResource(R.string.tools_posture_need_scan)
    }
    val unavailable = stringResource(R.string.tools_unavailable)
    fun section(title: String, values: List<Pair<String, Int>>): String = buildString {
        appendLine(title)
        if (values.isEmpty()) {
            append("  $unavailable")
        } else {
            append(values.joinToString("\n") { (label, count) -> "  $label: $count" })
        }
    }
    return buildString {
        appendLine(stringResource(R.string.tools_posture_observed_aps, posture.accessPointCount))
        appendLine()
        appendLine(
            section(
                stringResource(R.string.tools_posture_channel_groups),
                posture.channelGroupCounts.map { (key, count) -> localizedPostureChannelGroupKey(key) to count },
            ),
        )
        appendLine()
        appendLine(
            section(
                stringResource(R.string.tools_posture_security_profiles),
                posture.securityCounts.map { (key, count) -> localizedPostureSecurityKey(key) to count },
            ),
        )
        appendLine()
        appendLine(
            section(
                stringResource(R.string.tools_posture_generations),
                posture.generationCounts.map { (generation, count) -> generation.displayLabel() to count },
            ),
        )
        appendLine()
        append(stringResource(R.string.tools_posture_footer))
    }
}

/**
 * Localizes a neighborhood-posture channel-group count key. Keys are the core
 * [WifiChannelGroup.label] values produced by the pure-Kotlin posture analyzer;
 * unknown keys pass through unchanged. Generation counts retain their enum
 * identity and are localized separately at the display site.
 */
@Composable
private fun localizedPostureChannelGroupKey(key: String): String {
    val localizedByLabel = WifiChannelGroup.entries.associate { it.label to it.displayLabel() }
    return localizedByLabel[key] ?: key
}

/**
 * Localizes a neighborhood-posture security-profile count key. Keys are core
 * [SecurityType.label] values joined with " + " by the posture analyzer, so
 * each part is mapped independently; unknown parts pass through unchanged.
 */
@Composable
private fun localizedPostureSecurityKey(key: String): String {
    val localizedByLabel = SecurityType.entries.associate { it.label to it.displayLabel() }
    return key.split(" + ").joinToString(" + ") { localizedByLabel[it] ?: it }
}

@Composable
private fun ToolRow(icon: ImageVector, label: String, detail: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ScannerSpacing.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = ScannerSpacing.Sm, vertical = ScannerSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
    ) {
        Box(
            modifier = Modifier
                .size(ScannerSpacing.MinTouchTarget)
                .background(ScannerIconWell, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = ScannerCyan, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Xs)) {
            Text(
                label,
                color = ScannerText,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(detail, color = ScannerMuted, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = ScannerMuted)
    }
    HorizontalDivider(color = ScannerBorder)
}

@Composable
private fun connectionDetails(state: OpenScannerUiState): String {
    val unavailable = stringResource(R.string.tools_unavailable)
    if (state.phase !in setOf(ScannerPhase.LIVE, ScannerPhase.PAUSED)) {
        return stringResource(R.string.tools_connection_withheld)
    }
    return buildString {
        if (state.phase == ScannerPhase.PAUSED) {
            append(stringResource(R.string.tools_connection_evidence_stale))
        }
        append(
            stringResource(
                R.string.tools_connection_label,
                stringResource(
                    if (state.connection.connected) R.string.tools_connection_wifi else R.string.tools_connection_not_connected,
                ),
            ),
        )
        append(stringResource(R.string.tools_connection_network, state.connection.displayNetworkName(unavailable)))
        append(stringResource(R.string.tools_connection_bssid, state.connection.bssid ?: unavailable))
        append(stringResource(R.string.tools_connection_validated, state.connection.validated.displayLabel(unavailable)))
        append(stringResource(R.string.tools_connection_captive_portal, state.connection.captivePortal.displayLabel(unavailable)))
        append(
            stringResource(
                R.string.tools_connection_link_speed,
                state.connection.linkSpeedMbps?.let { stringResource(R.string.tools_connection_mbps, it) } ?: unavailable,
            ),
        )
        append(
            stringResource(
                R.string.tools_connection_rx_tx,
                state.connection.rxLinkSpeedMbps ?: "—",
                state.connection.txLinkSpeedMbps ?: "—",
            ),
        )
        append(stringResource(R.string.tools_connection_ip, state.connection.ipAddress ?: unavailable))
        append(stringResource(R.string.tools_connection_gateway, state.connection.gateway ?: unavailable))
        append(stringResource(R.string.tools_connection_dns, state.connection.dnsServers.ifEmpty { listOf(unavailable) }.joinToString()))
        append(stringResource(R.string.tools_connection_no_external))
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs.coerceAtLeast(0L) / 1_000L
    val hours = seconds / 3_600L
    val minutes = seconds % 3_600L / 60L
    val remainder = seconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, remainder)
    } else {
        "%02d:%02d".format(minutes, remainder)
    }
}
