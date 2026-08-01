package org.openscanner.data.wifi

import kotlinx.coroutines.flow.StateFlow
import org.openscanner.core.model.ScannerState

interface WifiScanRepository {
    val state: StateFlow<ScannerState>

    fun start()

    fun stop()

    fun requestScan()

    fun refreshCapabilityState()

    fun setPaused(paused: Boolean)

    fun setRefreshIntervalSeconds(seconds: Int)
}
