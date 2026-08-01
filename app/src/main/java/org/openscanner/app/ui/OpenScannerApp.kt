package org.openscanner.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openscanner.app.AppTab
import org.openscanner.app.OpenScannerUiState
import org.openscanner.app.ui.components.InformationBanner
import org.openscanner.app.ui.components.ScannerUnavailable
import org.openscanner.app.ui.screens.ScanScreen
import org.openscanner.app.ui.screens.SettingsScreen
import org.openscanner.app.ui.screens.SpectrumScreen
import org.openscanner.app.ui.screens.ToolsScreen
import org.openscanner.app.ui.screens.TrackScreen
import org.openscanner.app.ui.theme.ScannerAmber
import org.openscanner.app.ui.theme.ScannerBackground
import org.openscanner.app.ui.theme.ScannerBorder
import org.openscanner.app.ui.theme.ScannerCyan
import org.openscanner.app.ui.theme.ScannerIconWell
import org.openscanner.app.ui.theme.ScannerMuted
import org.openscanner.app.ui.theme.ScannerSpacing
import org.openscanner.app.ui.theme.ScannerSurface
import org.openscanner.app.ui.theme.ScannerSurfaceRaised
import org.openscanner.app.ui.theme.ScannerText
import org.openscanner.core.export.ExportDocument
import org.openscanner.core.export.ExportFormat
import org.openscanner.core.export.WifiLogField
import org.openscanner.core.export.WifiLogFormat
import org.openscanner.core.model.WifiChannelGroup

private data class ExportPreview(
    val document: ExportDocument,
)

@Composable
fun OpenScannerApp(
    state: OpenScannerUiState,
    onTabSelected: (AppTab) -> Unit,
    onChannelGroupSelected: (WifiChannelGroup) -> Unit,
    onSelectNetwork: (String, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onPauseChanged: (Boolean) -> Unit,
    onPrivacyChanged: (Boolean) -> Unit,
    onRedactExportsChanged: (Boolean) -> Unit,
    onRefreshIntervalChanged: (Int) -> Unit,
    onResetSettings: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onLogFieldChanged: (WifiLogField, Boolean) -> Unit,
    onSetAllLogFields: (Boolean) -> Unit,
    onStartLogging: () -> Unit,
    onStopLogging: () -> Unit,
    onClearLog: () -> Unit,
    buildSnapshotExport: (ExportFormat) -> ExportDocument?,
    buildLogExport: (WifiLogFormat) -> ExportDocument?,
    shareExport: (ExportDocument) -> Unit,
) {
    var exportPreview by remember { mutableStateOf<ExportPreview?>(null) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ScannerBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomNavigation(
                activeTab = state.activeTab,
                onTabSelected = onTabSelected,
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .statusBarsPadding()
                .padding(
                    start = ScannerSpacing.Lg,
                    end = ScannerSpacing.Lg,
                    top = ScannerSpacing.Sm,
                    bottom = ScannerSpacing.Md,
                ),
        ) {
            val dataAvailable = state.snapshotSequence != null &&
                (state.phase == org.openscanner.core.model.ScannerPhase.LIVE ||
                    state.phase == org.openscanner.core.model.ScannerPhase.PAUSED)
            when (state.activeTab) {
                AppTab.SCAN -> if (dataAvailable) {
                    ScanScreen(
                        state = state,
                        onChannelGroupSelected = onChannelGroupSelected,
                        onRefresh = onRefresh,
                        onSelectNetwork = { onSelectNetwork(it, true) },
                    )
                } else {
                    ScannerUnavailable(
                        phase = state.phase,
                        safeErrorCode = state.safeErrorCode,
                        onRequestPermission = onRequestPermission,
                        onOpenWifiSettings = onOpenWifiSettings,
                        onOpenLocationSettings = onOpenLocationSettings,
                        onRetry = onRefresh,
                    )
                }
                AppTab.TRACK -> if (dataAvailable) {
                    TrackScreen(
                        state = state,
                        onSelectNetwork = { onSelectNetwork(it, false) },
                        onPauseChanged = onPauseChanged,
                    )
                } else {
                    ScannerUnavailable(
                        phase = state.phase,
                        safeErrorCode = state.safeErrorCode,
                        onRequestPermission = onRequestPermission,
                        onOpenWifiSettings = onOpenWifiSettings,
                        onOpenLocationSettings = onOpenLocationSettings,
                        onRetry = onRefresh,
                    )
                }
                AppTab.SPECTRUM -> if (dataAvailable) {
                    SpectrumScreen(state = state, onChannelGroupSelected = onChannelGroupSelected)
                } else {
                    ScannerUnavailable(
                        phase = state.phase,
                        safeErrorCode = state.safeErrorCode,
                        onRequestPermission = onRequestPermission,
                        onOpenWifiSettings = onOpenWifiSettings,
                        onOpenLocationSettings = onOpenLocationSettings,
                        onRetry = onRefresh,
                    )
                }
                AppTab.TOOLS -> ToolsScreen(
                    state = state,
                    onPrepareSnapshotExport = { format ->
                        buildSnapshotExport(format)?.let { exportPreview = ExportPreview(it) }
                    },
                    onLogFieldChanged = onLogFieldChanged,
                    onSetAllLogFields = onSetAllLogFields,
                    onStartLogging = onStartLogging,
                    onStopLogging = onStopLogging,
                    onClearLog = onClearLog,
                    onPrepareLogExport = { format ->
                        buildLogExport(format)?.let { exportPreview = ExportPreview(it) }
                    },
                )
                AppTab.SETTINGS -> SettingsScreen(
                    state = state,
                    onPrivacyChanged = onPrivacyChanged,
                    onRedactExportsChanged = onRedactExportsChanged,
                    onRefreshIntervalChanged = onRefreshIntervalChanged,
                    onOpenWifiSettings = onOpenWifiSettings,
                    onResetSettings = onResetSettings,
                )
            }
        }
    }

    exportPreview?.let { preview ->
        val previewLines = remember(preview.document.content) { preview.document.content.split('\n') }
        AlertDialog(
            onDismissRequest = { exportPreview = null },
            icon = {
                Icon(
                    if (preview.document.redacted) Icons.Rounded.Lock else Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = if (preview.document.redacted) ScannerCyan else ScannerAmber,
                )
            },
            title = {
                Text(
                    text = "${preview.document.title} preview",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(ScannerSpacing.Md)) {
                    InformationBanner(
                        icon = if (preview.document.redacted) Icons.Rounded.Lock else Icons.Rounded.WarningAmber,
                        text = if (preview.document.redacted) {
                            "This scrollable preview contains the entire exact file payload Android will share. " +
                                "Network names, hardware addresses, and local addresses are redacted."
                        } else {
                            "UNREDACTED REPORT: this exact shared payload may contain network names, hardware " +
                                "addresses, local IP addresses, gateways, DNS servers, and precise timestamps. " +
                                "Review every line and share only with a trusted recipient."
                        },
                        positive = preview.document.redacted,
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .border(1.dp, ScannerBorder, MaterialTheme.shapes.small)
                            .background(ScannerBackground)
                            .padding(ScannerSpacing.Sm),
                    ) {
                        itemsIndexed(previewLines) { _, line ->
                            Text(
                                text = line,
                                color = ScannerMuted,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    exportPreview = null
                    shareExport(preview.document)
                }) {
                    Text(
                        if (preview.document.redacted) "Share" else "Share unredacted",
                        color = ScannerAmber,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { exportPreview = null }) { Text("Cancel", color = ScannerMuted) }
            },
            containerColor = ScannerSurfaceRaised,
            titleContentColor = ScannerText,
            textContentColor = ScannerText,
        )
    }
}

@Composable
private fun BottomNavigation(
    activeTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
) {
    val destinations = listOf(
        Triple(AppTab.SCAN, "Scan", Icons.Filled.Wifi),
        Triple(AppTab.TRACK, "Track", Icons.Filled.TrackChanges),
        Triple(AppTab.SPECTRUM, "Spectrum", Icons.Filled.BarChart),
        Triple(AppTab.TOOLS, "Tools", Icons.Filled.Build),
        Triple(AppTab.SETTINGS, "Settings", Icons.Filled.Settings),
    )
    Column(
        modifier = Modifier.fillMaxWidth().background(ScannerSurface).navigationBarsPadding(),
    ) {
        HorizontalDivider(color = ScannerBorder)
        Row(modifier = Modifier.fillMaxWidth()) {
            destinations.forEach { (tab, label, icon) ->
                NavItem(
                    tab = tab,
                    label = label,
                    icon = icon,
                    selected = tab == activeTab,
                    onSelected = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    tab: AppTab,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) ScannerCyan else ScannerMuted
    Column(
        modifier = modifier
            .heightIn(min = ScannerSpacing.MinTouchTarget + ScannerSpacing.Lg)
            .selectable(selected = selected, role = Role.Tab, onClick = onSelected)
            .semantics {
                this.selected = selected
                role = Role.Tab
                contentDescription = label
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(ScannerSpacing.MinTouchTarget + ScannerSpacing.Sm)
                .height(ScannerSpacing.Xxl)
                .background(
                    if (selected) ScannerIconWell else Color.Transparent,
                    MaterialTheme.shapes.extraLarge,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            label,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(top = ScannerSpacing.Xs),
        )
    }
}
