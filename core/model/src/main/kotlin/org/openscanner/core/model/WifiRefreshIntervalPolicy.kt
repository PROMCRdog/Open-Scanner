package org.openscanner.core.model

/** Shared validation and capability gating for user-selectable Wi-Fi scan cadences. */
object WifiRefreshIntervalPolicy {
    const val FAST_SECONDS: Int = 5
    const val DEFAULT_SECONDS: Int = 30

    val SUPPORTED_SECONDS: List<Int> = listOf(FAST_SECONDS, 10, 15, DEFAULT_SECONDS, 60)

    fun sanitize(seconds: Int): Int = seconds.takeIf(SUPPORTED_SECONDS::contains) ?: DEFAULT_SECONDS

    fun isSelectable(
        seconds: Int,
        wifiScanThrottleEnabled: Boolean?,
    ): Boolean = seconds in SUPPORTED_SECONDS &&
        (seconds != FAST_SECONDS || wifiScanThrottleEnabled == false)

    fun effectiveSeconds(
        seconds: Int,
        wifiScanThrottleEnabled: Boolean?,
    ): Int {
        val sanitized = sanitize(seconds)
        return if (isSelectable(sanitized, wifiScanThrottleEnabled)) sanitized else DEFAULT_SECONDS
    }
}
