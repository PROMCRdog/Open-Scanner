package org.openscanner.data.wifi

import org.openscanner.core.model.WifiRefreshIntervalPolicy

/**
 * Pure monotonic-clock state for the foreground scan cadence.
 *
 * Android broadcasts cannot be correlated to a particular startScan call, so
 * the controller intentionally models at most one outstanding request. A
 * completion broadcast or the bounded timeout releases that request.
 */
internal class ScanCadenceController(
    private val requestTimeoutMs: Long = REQUEST_TIMEOUT_MS,
) {
    var requestedIntervalSeconds: Int = WifiRefreshIntervalPolicy.DEFAULT_SECONDS
        private set

    var wifiScanThrottleEnabled: Boolean? = null
        private set

    var isActive: Boolean = false
        private set

    var isRequestInFlight: Boolean = false
        private set

    var isResultProcessing: Boolean = false
        private set

    private var lastRequestStartedAtElapsedMs: Long? = null
    private var manualRequestPending: Boolean = false

    val effectiveIntervalMs: Long
        get() = WifiRefreshIntervalPolicy.effectiveSeconds(
            seconds = requestedIntervalSeconds,
            wifiScanThrottleEnabled = wifiScanThrottleEnabled,
        ) * 1_000L

    fun updateConfiguration(
        requestedIntervalSeconds: Int = this.requestedIntervalSeconds,
        wifiScanThrottleEnabled: Boolean? = this.wifiScanThrottleEnabled,
    ) {
        this.requestedIntervalSeconds = WifiRefreshIntervalPolicy.sanitize(requestedIntervalSeconds)
        this.wifiScanThrottleEnabled = wifiScanThrottleEnabled
    }

    fun setActive(active: Boolean) {
        if (isActive == active) return
        isActive = active
        if (!active) {
            isRequestInFlight = false
            isResultProcessing = false
            lastRequestStartedAtElapsedMs = null
            manualRequestPending = false
        }
    }

    fun enqueueManualRequest() {
        if (isActive) manualRequestPending = true
    }

    fun nextWakeElapsedMs(nowElapsedMs: Long): Long? {
        if (!isActive || isResultProcessing) return null
        val lastStart = lastRequestStartedAtElapsedMs
        return when {
            isRequestInFlight && lastStart != null -> lastStart + requestTimeoutMs
            manualRequestPending || lastStart == null -> nowElapsedMs
            else -> lastStart + effectiveIntervalMs
        }
    }

    fun beginRequestIfDue(nowElapsedMs: Long): Boolean {
        if (!isActive || isRequestInFlight || isResultProcessing) return false
        val lastStart = lastRequestStartedAtElapsedMs
        val due = manualRequestPending || lastStart == null ||
            nowElapsedMs >= lastStart + effectiveIntervalMs
        if (!due) return false

        manualRequestPending = false
        lastRequestStartedAtElapsedMs = nowElapsedMs
        isRequestInFlight = true
        return true
    }

    fun markRequestRejected() {
        isRequestInFlight = false
    }

    /**
     * Releases the platform request at broadcast arrival but keeps cadence
     * blocked until the corresponding result state has committed.
     *
     * @return true when the broadcast completed the modeled request, false
     * for an unsolicited broadcast.
     */
    fun markScanResultsAvailable(): Boolean {
        val completedRequest = isRequestInFlight
        isRequestInFlight = false
        isResultProcessing = isActive
        return completedRequest
    }

    fun markResultProcessingCompleted() {
        isResultProcessing = false
    }

    fun consumeTimeoutIfDue(nowElapsedMs: Long): Boolean {
        val lastStart = lastRequestStartedAtElapsedMs ?: return false
        if (!isActive || !isRequestInFlight || nowElapsedMs < lastStart + requestTimeoutMs) {
            return false
        }
        isRequestInFlight = false
        return true
    }

    companion object {
        const val REQUEST_TIMEOUT_MS: Long = 15_000L
    }
}
