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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openscanner.app.OpenScannerUiState
import org.openscanner.app.ui.components.AppHeader
import org.openscanner.app.ui.components.InformationBanner
import org.openscanner.app.ui.components.PrimaryAction
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
        state.phase == ScannerPhase.PAUSED -> "Scanning is paused; connection evidence is from the last snapshot"
        state.phase != ScannerPhase.LIVE -> "Current connection evidence is unavailable until scanning recovers"
        !state.connection.connected -> "No active Wi-Fi connection reported"
        state.connection.captivePortal == true -> "Captive portal sign-in may be required"
        state.connection.validated == true -> "Android reports this Wi-Fi connection as validated"
        state.connection.validated == false -> "Android has not validated internet access"
        else -> "Wi-Fi is connected; validation state is unavailable"
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Md),
    ) {
        AppHeader(eyebrow = "Local-only diagnostics", phase = state.phase, freshness = state.freshness)
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
                label = "Connection evidence",
                detail = "Android validation, link speed and local addressing",
            ) { dialog = ToolDialog.CONNECTION }
            ToolRow(
                icon = Icons.Rounded.Security,
                label = "Security summary",
                detail = "Encryption class and passive risk signals",
            ) { dialog = ToolDialog.SECURITY }
            ToolRow(
                icon = Icons.Rounded.Info,
                label = "Neighborhood posture",
                detail = "Observed counts by channel group, security, and Wi-Fi generation",
            ) { dialog = ToolDialog.NEIGHBORHOOD_POSTURE }
            ToolRow(
                icon = Icons.Rounded.Share,
                label = "Export current snapshot",
                detail = if (state.snapshotSequence == null) {
                    "A completed scan is required before export"
                } else {
                    if (state.redactExports) {
                        "Preview redacted JSON or CSV before sharing"
                    } else {
                        "Preview UNREDACTED JSON or CSV before sharing"
                    }
                },
            ) {
                if (state.snapshotSequence == null) dialog = ToolDialog.NO_SNAPSHOT else snapshotExportChooser = true
            }
            ToolRow(
                icon = Icons.Rounded.Description,
                label = "Measurement limits",
                detail = "Scan throttling, RSSI and channel evidence explained",
            ) { dialog = ToolDialog.MEASUREMENT_LIMITS }
        }
        InformationBanner(
            icon = Icons.Rounded.Lock,
            text = "No account, ads, telemetry, or Internet permission. Diagnostics and logging run on-device.",
            modifier = Modifier.padding(top = ScannerSpacing.Sm),
        )
    }

    dialog?.let { dialogKind ->
        val (title, body) = dialogContent(dialogKind, state)
        ToolAlertDialog(
            title = title,
            onDismiss = { dialog = null },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Done") } },
            text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        )
    }
    if (snapshotExportChooser) {
        FormatChooserDialog(
            title = "Choose snapshot format",
            explanation = if (state.redactExports) {
                "A redacted preview is shown before a temporary file is shared."
            } else {
                "The preview is UNREDACTED and may contain sensitive network identifiers and local addresses."
            },
            labels = ExportFormat.entries.map { it.label },
            onChoose = { index ->
                snapshotExportChooser = false
                onPrepareSnapshotExport(ExportFormat.entries[index])
            },
            onDismiss = { snapshotExportChooser = false },
        )
    }
    if (logExportChooser) {
        FormatChooserDialog(
            title = "Choose log format",
            explanation = if (state.logging.redacted == false) {
                "Text is human-readable; JSON and CSV are structured. The exact preview is UNREDACTED."
            } else {
                "Text is human-readable; JSON and CSV are structured. The preview is the exact redacted payload."
            },
            labels = WifiLogFormat.entries.map { it.label },
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
            title = "Replace the previous log?",
            onDismiss = { replaceLogConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    replaceLogConfirm = false
                    onStartLogging()
                }) { Text("Replace and start", color = ScannerAmber) }
            },
            dismissButton = {
                TextButton(onClick = { replaceLogConfirm = false }) { Text("Cancel") }
            },
            text = {
                Text(
                    "The memory-only session will be replaced. Export it first if you want to keep it.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
    }
    if (clearLogConfirm) {
        ToolAlertDialog(
            title = "Clear the Wi-Fi log?",
            onDismiss = { clearLogConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    clearLogConfirm = false
                    onClearLog()
                }) { Text("Clear", color = ScannerAmber) }
            },
            dismissButton = {
                TextButton(onClick = { clearLogConfirm = false }) { Text("Cancel") }
            },
            text = {
                Text("This removes the stopped session from memory.", style = MaterialTheme.typography.bodyMedium)
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
        logging.active -> "RECORDING"
        logging.stopReason == WifiLogStopReason.SAFETY_LIMIT -> "LIMIT REACHED"
        logging.hasSession -> "READY TO EXPORT"
        else -> "NOT STARTED"
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
                Text("Wi-Fi session log", color = ScannerText, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (logging.active) {
                        "${logging.recordedFields.size} logged fields · " +
                            if (logging.redacted == false) "UNREDACTED" else "identifiers redacted"
                    } else if (logging.hasSession) {
                        "${logging.recordedFields.size} logged fields · " +
                            "${if (logging.redacted == false) "UNREDACTED" else "redacted"} session · " +
                            "${logging.selectedFields.size} selected for next session"
                    } else {
                        "${logging.selectedFields.size} of ${WifiLogField.entries.size} fields selected"
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
                "${logging.recordCount} records · ${logging.networkRowCount} AP rows · ${formatDuration(logging.durationMs)}",
                color = ScannerText,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            if (logging.active) {
                "Field selection and report redaction are locked for this session. Logging records scanner " +
                    "changes and periodic foreground samples at the requested refresh cadence; Android may reuse evidence."
            } else {
                "Choose fields, then start. The session stays in memory until cleared or the app process ends; stop it before export. " +
                    "The next session will be ${if (state.redactExports) "redacted" else "UNREDACTED"}. " +
                    "Safety limit: ${WifiLogRecorder.MAX_RECORDS} records or " +
                    "${WifiLogRecorder.MAX_NETWORK_ROWS} AP rows."
            },
            color = ScannerMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        PrimaryAction(
            label = if (logging.active) "Stop logging" else "Start logging",
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
                Text("Fields", modifier = Modifier.padding(start = ScannerSpacing.Xs))
            }
            TextButton(
                onClick = onExport,
                enabled = logging.canExport,
                modifier = Modifier.heightIn(min = ScannerSpacing.MinTouchTarget),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Text("Export", modifier = Modifier.padding(start = ScannerSpacing.Xs))
            }
            TextButton(
                onClick = onClear,
                enabled = logging.hasSession && !logging.active,
                modifier = Modifier.heightIn(min = ScannerSpacing.MinTouchTarget),
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
                Text("Clear", modifier = Modifier.padding(start = ScannerSpacing.Xs))
            }
        }
        if (!logging.active && state.snapshotSequence == null) {
            Text(
                "Complete a scan before starting a log.",
                color = ScannerAmber,
                style = MaterialTheme.typography.labelMedium,
            )
        } else if (!logging.active && !logging.canStart) {
            Text(
                "Select at least one field before starting.",
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
        title = "Logging fields",
        onDismiss = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Sm)) {
                Text(
                    if (redacted) {
                        "Record number and elapsed time are always included. Sensitive identifiers are masked."
                    } else {
                        "Record number and elapsed time are always included. The next session will retain selected raw identifiers in memory."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { onSetAll(true) }) { Text("Select all") }
                    TextButton(onClick = { onSetAll(false) }) { Text("Clear all") }
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
                                category.label.uppercase(),
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
                                    .semantics { contentDescription = "${field.label}. ${field.description}" }
                                    .padding(horizontal = ScannerSpacing.Md, vertical = ScannerSpacing.Sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = ScannerCyan),
                                )
                                Column(Modifier.weight(1f).padding(start = ScannerSpacing.Sm)) {
                                    Text(field.label, color = ScannerText, style = MaterialTheme.typography.bodyMedium)
                                    Text(field.description, color = ScannerMuted, style = MaterialTheme.typography.labelSmall)
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
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
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

private fun dialogContent(dialog: ToolDialog, state: OpenScannerUiState): Pair<String, String> = when (dialog) {
    ToolDialog.CONNECTION -> "Connection evidence" to connectionDetails(state)
    ToolDialog.SECURITY -> {
        val network = state.selectedNetwork
        "Security summary" to if (network == null) {
            "Select an access point to inspect its advertised security capabilities."
        } else {
            "${network.name}\n\nAdvertised security: ${network.security}.\n\n" +
                "This describes beacon capabilities only. It does not audit passwords, router firmware, or client isolation."
        }
    }
    ToolDialog.NEIGHBORHOOD_POSTURE -> "Neighborhood posture" to neighborhoodPostureDetails(state)
    ToolDialog.NO_SNAPSHOT -> "No snapshot to export" to
        "Complete a nearby Wi-Fi scan first. Open Scanner will then build an exact " +
        "${if (state.redactExports) "redacted" else "UNREDACTED"} preview before sharing anything."
    ToolDialog.MEASUREMENT_LIMITS -> "Measurement limits" to
        "Open Scanner uses passive Android scan results. Android may cache or throttle scans. " +
        "RSSI is not distance or throughput, and channel overlap is not airtime utilization. " +
        "The app therefore reports freshness and observed evidence without guaranteeing the best legal router channel."
}

private fun neighborhoodPostureDetails(state: OpenScannerUiState): String {
    val posture = state.neighborhoodPosture
    if (state.snapshotSequence == null) {
        return "Complete a scan to summarize the currently observed neighborhood."
    }
    fun section(title: String, values: List<Pair<String, Int>>): String = buildString {
        appendLine(title)
        if (values.isEmpty()) {
            append("  Unavailable")
        } else {
            append(values.joinToString("\n") { (label, count) -> "  $label: $count" })
        }
    }
    return buildString {
        appendLine("Observed access points: ${posture.accessPointCount}")
        appendLine()
        appendLine(section("Channel groups", posture.channelGroupCounts))
        appendLine()
        appendLine(section("Advertised security profiles", posture.securityCounts))
        appendLine()
        appendLine(section("Wi-Fi generations", posture.generationCounts))
        appendLine()
        append("Counts describe this snapshot only; they do not identify devices, measure airtime, or rate safety.")
    }
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

private fun connectionDetails(state: OpenScannerUiState): String = if (state.phase !in setOf(ScannerPhase.LIVE, ScannerPhase.PAUSED)) {
    "Current connection evidence is withheld while scanning is unavailable so an older snapshot is not presented as current. Recover scanning, then open this panel again."
} else buildString {
    if (state.phase == ScannerPhase.PAUSED) {
        append("Evidence status: last snapshot before pause; values may now be stale.\n\n")
    }
    append("Connection: ${if (state.connection.connected) "Wi-Fi" else "Not connected over Wi-Fi"}\n")
    append("Network: ${state.connection.networkName ?: "Unavailable"}\n")
    append("BSSID: ${state.connection.bssid ?: "Unavailable"}\n")
    append("Android validated: ${state.connection.validated?.toString() ?: "Unavailable"}\n")
    append("Captive portal: ${state.connection.captivePortal?.toString() ?: "Unavailable"}\n")
    append("Link speed: ${state.connection.linkSpeedMbps?.let { "$it Mbps" } ?: "Unavailable"}\n")
    append("RX / TX: ${state.connection.rxLinkSpeedMbps ?: "—"} / ${state.connection.txLinkSpeedMbps ?: "—"} Mbps\n")
    append("IP address: ${state.connection.ipAddress ?: "Unavailable"}\n")
    append("Gateway: ${state.connection.gateway ?: "Unavailable"}\n")
    append("DNS: ${state.connection.dnsServers.ifEmpty { listOf("Unavailable") }.joinToString()}\n\n")
    append("No external endpoint was contacted.")
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
