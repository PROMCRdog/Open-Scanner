package org.openscanner.app

import org.openscanner.core.model.ScannerState
import org.openscanner.core.model.WifiRefreshIntervalPolicy

/** ViewModel policy for repairing a saved fast interval after capability discovery. */
internal object RefreshIntervalCapabilityPolicy {
    fun shouldResetSavedFastInterval(
        refreshIntervalSeconds: Int,
        scannerState: ScannerState,
    ): Boolean =
        refreshIntervalSeconds == WifiRefreshIntervalPolicy.FAST_SECONDS &&
            scannerState.capabilities.wifiScanThrottleStateResolved &&
            !WifiRefreshIntervalPolicy.isSelectable(
                seconds = refreshIntervalSeconds,
                wifiScanThrottleEnabled = scannerState.capabilities.wifiScanThrottleEnabled,
            )
}
