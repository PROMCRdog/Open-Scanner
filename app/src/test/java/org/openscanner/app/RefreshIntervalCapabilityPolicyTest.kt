package org.openscanner.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openscanner.core.model.PlatformCapabilities
import org.openscanner.core.model.ScannerPhase
import org.openscanner.core.model.ScannerState
import org.openscanner.core.model.WifiRefreshIntervalPolicy

class RefreshIntervalCapabilityPolicyTest {
    @Test
    fun savedFastIntervalIsNotResetWhileCapabilityDiscoveryIsUnresolved() {
        assertFalse(
            RefreshIntervalCapabilityPolicy.shouldResetSavedFastInterval(
                refreshIntervalSeconds = WifiRefreshIntervalPolicy.FAST_SECONDS,
                scannerState = scannerState(
                    phase = ScannerPhase.CHECKING,
                    wifiScanThrottleEnabled = null,
                    wifiScanThrottleStateResolved = false,
                ),
            ),
        )
    }

    @Test
    fun resolvedUnavailableStateResetsEvenWhileInitialScanIsChecking() {
        assertTrue(
            RefreshIntervalCapabilityPolicy.shouldResetSavedFastInterval(
                refreshIntervalSeconds = WifiRefreshIntervalPolicy.FAST_SECONDS,
                scannerState = scannerState(
                    phase = ScannerPhase.CHECKING,
                    wifiScanThrottleEnabled = null,
                    wifiScanThrottleStateResolved = true,
                ),
            ),
        )
    }

    @Test
    fun savedFastIntervalRemainsWhenThrottlingIsExplicitlyOff() {
        assertFalse(
            RefreshIntervalCapabilityPolicy.shouldResetSavedFastInterval(
                refreshIntervalSeconds = WifiRefreshIntervalPolicy.FAST_SECONDS,
                scannerState = scannerState(ScannerPhase.LIVE, wifiScanThrottleEnabled = false),
            ),
        )
    }

    @Test
    fun savedFastIntervalResetsWhenThrottlingIsOn() {
        assertTrue(
            RefreshIntervalCapabilityPolicy.shouldResetSavedFastInterval(
                refreshIntervalSeconds = WifiRefreshIntervalPolicy.FAST_SECONDS,
                scannerState = scannerState(ScannerPhase.LIVE, wifiScanThrottleEnabled = true),
            ),
        )
    }

    @Test
    fun savedFastIntervalResetsWhenResolvedStateCannotReportThrottling() {
        assertTrue(
            RefreshIntervalCapabilityPolicy.shouldResetSavedFastInterval(
                refreshIntervalSeconds = WifiRefreshIntervalPolicy.FAST_SECONDS,
                scannerState = scannerState(ScannerPhase.LIVE, wifiScanThrottleEnabled = null),
            ),
        )
    }

    @Test
    fun standardIntervalIsNeverCapabilityReset() {
        assertFalse(
            RefreshIntervalCapabilityPolicy.shouldResetSavedFastInterval(
                refreshIntervalSeconds = WifiRefreshIntervalPolicy.DEFAULT_SECONDS,
                scannerState = scannerState(ScannerPhase.LIVE, wifiScanThrottleEnabled = true),
            ),
        )
    }

    private fun scannerState(
        phase: ScannerPhase,
        wifiScanThrottleEnabled: Boolean?,
        wifiScanThrottleStateResolved: Boolean = true,
    ) = ScannerState(
        phase = phase,
        capabilities = PlatformCapabilities(
            hasWifiHardware = true,
            supports5Ghz = true,
            supports6Ghz = false,
            wifiScanThrottleEnabled = wifiScanThrottleEnabled,
            wifiScanThrottleStateResolved = wifiScanThrottleStateResolved,
        ),
    )
}
