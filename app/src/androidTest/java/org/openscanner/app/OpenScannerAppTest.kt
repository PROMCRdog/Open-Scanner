package org.openscanner.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.openscanner.app.ui.OpenScannerApp
import org.openscanner.app.ui.theme.OpenScannerTheme
import org.openscanner.core.domain.Freshness
import org.openscanner.core.export.ExportDocument
import org.openscanner.core.model.PlatformCapabilities
import org.openscanner.core.model.ScannerPhase
import org.openscanner.core.model.SecurityType
import org.openscanner.core.model.WifiBand
import org.openscanner.core.model.WifiChannelGroup
import org.openscanner.core.model.WifiGeneration

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
                    onRedactExportsChanged = {},
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
    fun scanHighlightsCurrentSystemWifi() {
        composeRule.setContent {
            OpenScannerTheme {
                OpenScannerApp(
                    state = liveState().copy(activeTab = AppTab.SCAN),
                    onTabSelected = {},
                    onChannelGroupSelected = {},
                    onSelectNetwork = { _, _ -> },
                    onRefresh = {},
                    onPauseChanged = {},
                    onPrivacyChanged = {},
                    onRedactExportsChanged = {},
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

        composeRule.onNodeWithText("CURRENT WI-FI").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Sample network, -52 dBm, 5.2 GHz, channel 36, WPA3 Personal, " +
                "current system Wi-Fi connection, selected for tracking",
        ).assertIsDisplayed()
    }

    @Test
    fun spectrumSelectorShowsAllNetworksAndCanFocusTheChart() {
        val base = liveState()
        var focusedId by mutableStateOf("spectrum-1")
        val networks = (1..6).map { index ->
            base.networks.single().copy(
                uiId = "spectrum-$index",
                name = "Spectrum network $index",
                signalDbm = -40 - index,
                connected = index == 3,
                selected = false,
            )
        }
        composeRule.setContent {
            val currentNetworks = networks.map { it.copy(selected = it.uiId == focusedId) }
            OpenScannerTheme {
                OpenScannerApp(
                    state = base.copy(
                        activeTab = AppTab.SPECTRUM,
                        networks = currentNetworks,
                        selectedNetwork = currentNetworks.first { it.selected },
                    ),
                    onTabSelected = {},
                    onChannelGroupSelected = {},
                    onSelectNetwork = { networkId, _ -> focusedId = networkId },
                    onRefresh = {},
                    onPauseChanged = {},
                    onPrivacyChanged = {},
                    onRedactExportsChanged = {},
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

        composeRule.onNodeWithText("Showing 6 of 6 in this channel group").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Spectrum network 6", substring = true).assertIsDisplayed()

        composeRule.onNodeWithText("Choose").performClick()
        composeRule.onNodeWithText("Choose displayed networks").assertIsDisplayed()
        composeRule.onNodeWithText("Spectrum network 6").assertIsDisplayed()
        composeRule.onNodeWithText("CURRENT WI-FI").assertIsDisplayed()
        composeRule.onNodeWithText("FOCUS · ONE").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Spectrum network 1 is focused").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Focus Spectrum network 1").assertIsSelected()
        composeRule.onNodeWithContentDescription("Focus Spectrum network 2")
            .assertIsEnabled()
            .assertIsNotSelected()
            .performClick()
        composeRule.onNodeWithContentDescription("Focus Spectrum network 1").assertIsNotSelected()
        composeRule.onNodeWithContentDescription("Focus Spectrum network 2").assertIsSelected()
        composeRule.onNodeWithContentDescription("Spectrum network 2 is focused").assertIsDisplayed()
        composeRule.onNodeWithText("Only focused").performClick()
        composeRule.onNodeWithText("Done").performClick()

        composeRule.onNodeWithText("Showing 1 of 6 in this channel group").assertIsDisplayed()

        composeRule.onNodeWithText("Choose").performClick()
        composeRule.onNodeWithText("Show all").performClick()
        composeRule.onNodeWithText("Done").performClick()
        composeRule.onNodeWithText("Showing 6 of 6 in this channel group").assertIsDisplayed()
        composeRule.onNodeWithText("Spectrum network 3 · CURRENT WI-FI").assertIsDisplayed()
    }

    @Test
    fun trackSelectorCarriesTheCurrentWifiTag() {
        val base = liveState()
        val focused = base.networks.single().copy(
            uiId = "focused",
            name = "Focused network",
            connected = false,
            selected = true,
        )
        val connected = focused.copy(
            uiId = "connected",
            name = "Connected network",
            connected = true,
            selected = false,
        )
        composeRule.setContent {
            OpenScannerTheme {
                OpenScannerApp(
                    state = base.copy(
                        activeTab = AppTab.TRACK,
                        networks = listOf(focused, connected),
                        selectedNetwork = focused,
                    ),
                    onTabSelected = {},
                    onChannelGroupSelected = {},
                    onSelectNetwork = { _, _ -> },
                    onRefresh = {},
                    onPauseChanged = {},
                    onPrivacyChanged = {},
                    onRedactExportsChanged = {},
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

        composeRule.onNodeWithText("Choose network").performScrollTo().performClick()
        composeRule.onNodeWithText("CURRENT WI-FI").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Connected network").assertIsDisplayed()
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
                    onRedactExportsChanged = {},
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
                    onRedactExportsChanged = {},
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
                    onRedactExportsChanged = {},
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
                    onRedactExportsChanged = {},
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

    @Test
    fun settingsReflectsWifiScanThrottleStateAndShowsDisableHint() {
        val initialState = liveState()
        var state by mutableStateOf(
            initialState.copy(
                activeTab = AppTab.SETTINGS,
                capabilities = initialState.capabilities.copy(wifiScanThrottleEnabled = true),
            ),
        )
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
                    onRedactExportsChanged = {},
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

        composeRule.onNodeWithContentDescription(
            "Wi-Fi scan throttling: ON. Disable for local testing: Developer options › Networking › " +
                "Wi-Fi scan throttling. May increase battery use.",
        ).assertIsDisplayed()

        composeRule.runOnIdle {
            state = state.copy(
                capabilities = state.capabilities.copy(wifiScanThrottleEnabled = false),
            )
        }
        composeRule.onNodeWithContentDescription(
            "Wi-Fi scan throttling: OFF. Disabled in Developer options; Android may scan more often and use more battery.",
        ).assertIsDisplayed()
    }

    @Test
    fun fiveSecondRefreshIsVisibleButDisabledWhileScanThrottlingIsOn() {
        var requestedInterval: Int? = null
        val reason = "5 s request mode requires Wi-Fi scan throttling to be OFF in Developer options."
        showSettings(
            state = liveState().copy(
                capabilities = liveState().capabilities.copy(wifiScanThrottleEnabled = true),
            ),
            onRefreshIntervalChanged = { requestedInterval = it },
        )

        composeRule.onNodeWithText("5 s").assertIsDisplayed()
        composeRule.onNodeWithText(reason).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("5 s option disabled. $reason").performScrollTo().assertIsNotEnabled()
        assertEquals(null, requestedInterval)
    }

    @Test
    fun fiveSecondRefreshCanBeSelectedWhenScanThrottlingIsExplicitlyOff() {
        var requestedInterval: Int? = null
        showSettings(
            state = liveState().copy(
                capabilities = liveState().capabilities.copy(wifiScanThrottleEnabled = false),
            ),
            onRefreshIntervalChanged = { requestedInterval = it },
        )

        composeRule.onNodeWithText(
            "5 s request mode is available. It may use more battery; fresh-result cadence still depends on Android and the device.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("5 s").performScrollTo().performClick()
        assertEquals(5, requestedInterval)
    }

    @Test
    fun fiveSecondRefreshExplainsResolvedUnavailableThrottleState() {
        val reason = "5 s request mode is unavailable because Android cannot report the scan-throttling state."
        showSettings(
            state = liveState().copy(
                capabilities = liveState().capabilities.copy(wifiScanThrottleEnabled = null),
            ),
        )

        composeRule.onNodeWithText(reason).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("5 s option disabled. $reason").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun fiveSecondRefreshExplainsUnresolvedThrottleState() {
        val reason = "Checking Wi-Fi scan throttling before enabling 5 s request mode."
        showSettings(
            state = liveState().copy(
                capabilities = liveState().capabilities.copy(
                    wifiScanThrottleEnabled = null,
                    wifiScanThrottleStateResolved = false,
                ),
            ),
        )

        composeRule.onNodeWithText(reason).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("5 s option disabled. $reason").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Checking").assertIsDisplayed()
    }

    @Test
    fun settingsAboutUsesBuildVersion() {
        showSettings(state = liveState())

        composeRule.onNodeWithText("Version 0.2.1 · Apache License 2.0 · open source")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("About Open Scanner").performScrollTo().performClick()
        composeRule.onNodeWithText("Open Scanner 0.2.1").assertIsDisplayed()
    }

    @Test
    fun appLanguageDefaultsToSystemAndOffersEnglishAndSimplifiedChinese() {
        var requestedLanguage: AppLanguage? = null
        showSettings(
            state = liveState(),
            selectedLanguage = AppLanguage.SYSTEM_DEFAULT,
            onLanguageSelected = { requestedLanguage = it },
        )

        composeRule.onNodeWithText("App language").assertIsDisplayed()
        composeRule.onNodeWithText("System default · follows device language").assertIsDisplayed()
        composeRule.onNodeWithText("App language").performClick()
        composeRule.onNodeWithText("Choose app language").assertIsDisplayed()
        composeRule.onNodeWithText("English").assertIsDisplayed()
        composeRule.onNodeWithText("简体中文 (Simplified Chinese)").performClick()

        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, requestedLanguage)
    }

    @Test
    fun disablingReportRedactionRequiresExplicitConfirmation() {
        var requested: Boolean? = null
        composeRule.setContent {
            OpenScannerTheme {
                OpenScannerApp(
                    state = liveState().copy(activeTab = AppTab.SETTINGS, redactExports = true),
                    onTabSelected = {},
                    onChannelGroupSelected = {},
                    onSelectNetwork = { _, _ -> },
                    onRefresh = {},
                    onPauseChanged = {},
                    onPrivacyChanged = {},
                    onRedactExportsChanged = { requested = it },
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

        composeRule.onNodeWithContentDescription("Redact reports").performClick()
        composeRule.onNodeWithText("Allow unredacted reports?").assertIsDisplayed()
        assertEquals(null, requested)
        composeRule.onNodeWithText("Allow unredacted").performClick()
        assertEquals(false, requested)
    }

    @Test
    fun unredactedExportHasSensitivePreviewAndExplicitShareAction() {
        var shared = false
        val rawDocument = ExportDocument(
            title = "Unredacted snapshot · JSON",
            fileName = "open-scanner-snapshot-unredacted.json",
            mimeType = "application/json",
            shareSubject = "Open Scanner unredacted snapshot",
            redacted = false,
            content = "{\"redacted\":false,\"name\":\"Secret Lab\"}",
        )
        composeRule.setContent {
            OpenScannerTheme {
                OpenScannerApp(
                    state = liveState().copy(activeTab = AppTab.TOOLS, redactExports = false),
                    onTabSelected = {},
                    onChannelGroupSelected = {},
                    onSelectNetwork = { _, _ -> },
                    onRefresh = {},
                    onPauseChanged = {},
                    onPrivacyChanged = {},
                    onRedactExportsChanged = {},
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
                    buildSnapshotExport = { rawDocument },
                    buildLogExport = { null },
                    shareExport = { shared = true },
                )
            }
        }

        composeRule.onNodeWithText("Export current snapshot").performClick()
        composeRule.onNodeWithText("JSON").performClick()
        composeRule.onNodeWithText("UNREDACTED REPORT", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Share unredacted").performClick()
        assertTrue(shared)
    }

    private fun showSettings(
        state: OpenScannerUiState,
        onRefreshIntervalChanged: (Int) -> Unit = {},
        selectedLanguage: AppLanguage = AppLanguage.SYSTEM_DEFAULT,
        onLanguageSelected: (AppLanguage) -> Unit = {},
    ) {
        composeRule.setContent {
            OpenScannerTheme {
                OpenScannerApp(
                    state = state.copy(activeTab = AppTab.SETTINGS),
                    onTabSelected = {},
                    onChannelGroupSelected = {},
                    onSelectNetwork = { _, _ -> },
                    onRefresh = {},
                    onPauseChanged = {},
                    onPrivacyChanged = {},
                    onRedactExportsChanged = {},
                    onRefreshIntervalChanged = onRefreshIntervalChanged,
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
                    selectedLanguage = selectedLanguage,
                    onLanguageSelected = onLanguageSelected,
                )
            }
        }
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
                securityTypes = setOf(SecurityType.WPA3_PERSONAL),
                generation = WifiGeneration.WIFI_6,
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
            securityTypes = setOf(SecurityType.WPA3_PERSONAL),
            generation = WifiGeneration.WIFI_6,
            connected = true,
            selected = true,
        ),
        snapshotSequence = 1,
        ageMs = 2_000,
        freshness = Freshness.FRESH,
    )
}
