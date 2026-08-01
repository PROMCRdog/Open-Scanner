package org.openscanner.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.openscanner.app.ui.OpenScannerApp
import org.openscanner.app.ui.theme.OpenScannerTheme
import org.openscanner.core.domain.Freshness
import org.openscanner.core.model.PlatformCapabilities
import org.openscanner.core.model.ScannerPhase
import org.openscanner.core.model.WifiBand
import org.openscanner.core.model.WifiChannelGroup

class OpenScannerAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fiveDestinationsIncludeSeparateSpectrumWorkspace() {
        var tab by mutableStateOf(AppTab.TRACK)
        composeRule.setContent {
            OpenScannerTheme {
                OpenScannerApp(
                    state = liveState().copy(activeTab = tab),
                    onTabSelected = { tab = it },
                    onChannelGroupSelected = {},
                    onSelectNetwork = { _, _ -> },
                    onRefresh = {},
                    onPauseChanged = {},
                    onPrivacyChanged = {},
                    onRefreshIntervalChanged = {},
                    onResetSettings = {},
                    onRequestPermission = {},
                    onOpenWifiSettings = {},
                    onOpenLocationSettings = {},
                    onLogFieldChanged = { _, _ -> },
                    onSetAllLogFields = {},
                    onStartLogging = {},
                    onStopLogging = {},
                    onClearLog = {},
                    buildSnapshotExport = { null },
                    buildLogExport = { null },
                    shareExport = {},
                )
            }
        }

        composeRule.onNodeWithText("Spectrum").performClick()
        composeRule.onNodeWithText("Channel spectrum").assertIsDisplayed()
        composeRule.onNodeWithText("Scan").performClick()
        composeRule.onNodeWithText("Nearby networks").assertIsDisplayed()
    }

    @Test
    fun permissionStateExplainsLocalProcessingBeforeRequest() {
        var requested = false
        composeRule.setContent {
            OpenScannerTheme {
                OpenScannerApp(
                    state = OpenScannerUiState(phase = ScannerPhase.PERMISSION_REQUIRED),
                    onTabSelected = {},
                    onChannelGroupSelected = {},
                    onSelectNetwork = { _, _ -> },
                    onRefresh = {},
                    onPauseChanged = {},
                    onPrivacyChanged = {},
                    onRefreshIntervalChanged = {},
                    onResetSettings = {},
                    onRequestPermission = { requested = true },
                    onOpenWifiSettings = {},
                    onOpenLocationSettings = {},
                    onLogFieldChanged = { _, _ -> },
                    onSetAllLogFields = {},
                    onStartLogging = {},
                    onStopLogging = {},
                    onClearLog = {},
                    buildSnapshotExport = { null },
                    buildLogExport = { null },
                    shareExport = {},
                )
            }
        }

        composeRule.onNodeWithText("Nearby Wi-Fi permission").assertIsDisplayed()
        composeRule.onNodeWithText("Grant permission").performClick()
        assertTrue(requested)
    }

    @Test
    fun staleSnapshotDoesNotHideCurrentPermissionBlock() {
        composeRule.setContent {
            OpenScannerTheme {
                OpenScannerApp(
                    state = liveState().copy(phase = ScannerPhase.PERMISSION_REQUIRED),
                    onTabSelected = {},
                    onChannelGroupSelected = {},
                    onSelectNetwork = { _, _ -> },
                    onRefresh = {},
                    onPauseChanged = {},
                    onPrivacyChanged = {},
                    onRefreshIntervalChanged = {},
                    onResetSettings = {},
                    onRequestPermission = {},
                    onOpenWifiSettings = {},
                    onOpenLocationSettings = {},
                    onLogFieldChanged = { _, _ -> },
                    onSetAllLogFields = {},
                    onStartLogging = {},
                    onStopLogging = {},
                    onClearLog = {},
                    buildSnapshotExport = { null },
                    buildLogExport = { null },
                    shareExport = {},
                )
            }
        }

        composeRule.onNodeWithText("Nearby Wi-Fi permission").assertIsDisplayed()
    }

    @Test
    fun openConnectionDialogReactsWhenScannerBecomesBlocked() {
        var state by mutableStateOf(liveState().copy(activeTab = AppTab.TOOLS))
        composeRule.setContent {
            OpenScannerTheme {
                OpenScannerApp(
                    state = state,
                    onTabSelected = {},
                    onChannelGroupSelected = {},
                    onSelectNetwork = { _, _ -> },
                    onRefresh = {},
                    onPauseChanged = {},
                    onPrivacyChanged = {},
                    onRefreshIntervalChanged = {},
                    onResetSettings = {},
                    onRequestPermission = {},
                    onOpenWifiSettings = {},
                    onOpenLocationSettings = {},
                    onLogFieldChanged = { _, _ -> },
                    onSetAllLogFields = {},
                    onStartLogging = {},
                    onStopLogging = {},
                    onClearLog = {},
                    buildSnapshotExport = { null },
                    buildLogExport = { null },
                    shareExport = {},
                )
            }
        }

        composeRule.onNodeWithText("Connection evidence").performClick()
        composeRule.runOnIdle { state = state.copy(phase = ScannerPhase.PERMISSION_REQUIRED) }
        composeRule.onNodeWithText(
            "Current connection evidence is withheld while scanning is unavailable so an older snapshot is not presented as current. Recover scanning, then open this panel again.",
        ).assertIsDisplayed()
    }

    @Test
    fun wifiLoggingPanelStartsAndExposesFieldSelection() {
        var startRequested = false
        var clearAllRequested = false
        composeRule.setContent {
            OpenScannerTheme {
                OpenScannerApp(
                    state = liveState().copy(activeTab = AppTab.TOOLS),
                    onTabSelected = {},
                    onChannelGroupSelected = {},
                    onSelectNetwork = { _, _ -> },
                    onRefresh = {},
                    onPauseChanged = {},
                    onPrivacyChanged = {},
                    onRefreshIntervalChanged = {},
                    onResetSettings = {},
                    onRequestPermission = {},
                    onOpenWifiSettings = {},
                    onOpenLocationSettings = {},
                    onLogFieldChanged = { _, _ -> },
                    onSetAllLogFields = { enabled -> if (!enabled) clearAllRequested = true },
                    onStartLogging = { startRequested = true },
                    onStopLogging = {},
                    onClearLog = {},
                    buildSnapshotExport = { null },
                    buildLogExport = { null },
                    shareExport = {},
                )
            }
        }

        composeRule.onNodeWithText("Start logging").performClick()
        assertTrue(startRequested)
        composeRule.onNodeWithText("Fields").performClick()
        composeRule.onNodeWithText("Logging fields").assertIsDisplayed()
        composeRule.onNodeWithText("Clear all").performClick()
        assertTrue(clearAllRequested)
    }

    private fun liveState(): OpenScannerUiState = OpenScannerUiState(
        phase = ScannerPhase.LIVE,
        capabilities = PlatformCapabilities(true, true, true),
        channelGroups = listOf(
            ChannelGroupUiModel(WifiChannelGroup.GHZ_2_4, true),
            ChannelGroupUiModel(WifiChannelGroup.GHZ_5_2, true),
            ChannelGroupUiModel(WifiChannelGroup.GHZ_5_5_DFS, true),
            ChannelGroupUiModel(WifiChannelGroup.GHZ_5_8, true),
            ChannelGroupUiModel(WifiChannelGroup.GHZ_6, true),
        ),
        selectedChannelGroup = WifiChannelGroup.GHZ_5_2,
        networks = listOf(
            NetworkUiModel(
                uiId = "sample",
                name = "Sample network",
                bssid = "••:••:••:••:aa:bb",
                band = WifiBand.GHZ_5,
                channelGroup = WifiChannelGroup.GHZ_5_2,
                channel = 36,
                frequencyMhz = 5180,
                footprintCenterFrequencyMhz = 5210,
                channelWidthMhz = 80,
                signalDbm = -52,
                security = "WPA3 Personal",
                generation = "Wi-Fi 6",
                connected = true,
                selected = true,
            ),
        ),
        selectedNetwork = NetworkUiModel(
            uiId = "sample",
            name = "Sample network",
            bssid = "••:••:••:••:aa:bb",
            band = WifiBand.GHZ_5,
            channelGroup = WifiChannelGroup.GHZ_5_2,
            channel = 36,
            frequencyMhz = 5180,
            footprintCenterFrequencyMhz = 5210,
            channelWidthMhz = 80,
            signalDbm = -52,
            security = "WPA3 Personal",
            generation = "Wi-Fi 6",
            connected = true,
            selected = true,
        ),
        snapshotSequence = 1,
        ageMs = 2_000,
        freshness = Freshness.FRESH,
    )
}
