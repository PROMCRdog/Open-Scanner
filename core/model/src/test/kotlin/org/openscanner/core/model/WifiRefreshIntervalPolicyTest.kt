package org.openscanner.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiRefreshIntervalPolicyTest {
    @Test
    fun supportedIntervalsAreExactAndDefaultIsThirtySeconds() {
        assertEquals(listOf(5, 10, 15, 30, 60), WifiRefreshIntervalPolicy.SUPPORTED_SECONDS)
        assertEquals(30, WifiRefreshIntervalPolicy.DEFAULT_SECONDS)
    }

    @Test
    fun invalidValuesResetToDefaultRatherThanClamp() {
        listOf(Int.MIN_VALUE, 0, 4, 6, 20, 59, 61, Int.MAX_VALUE).forEach { value ->
            assertEquals(30, WifiRefreshIntervalPolicy.sanitize(value))
        }
        WifiRefreshIntervalPolicy.SUPPORTED_SECONDS.forEach { value ->
            assertEquals(value, WifiRefreshIntervalPolicy.sanitize(value))
        }
    }

    @Test
    fun fiveSecondsRequiresThrottleToBeExplicitlyDisabled() {
        assertTrue(WifiRefreshIntervalPolicy.isSelectable(5, wifiScanThrottleEnabled = false))
        assertFalse(WifiRefreshIntervalPolicy.isSelectable(5, wifiScanThrottleEnabled = true))
        assertFalse(WifiRefreshIntervalPolicy.isSelectable(5, wifiScanThrottleEnabled = null))

        assertEquals(5, WifiRefreshIntervalPolicy.effectiveSeconds(5, wifiScanThrottleEnabled = false))
        assertEquals(30, WifiRefreshIntervalPolicy.effectiveSeconds(5, wifiScanThrottleEnabled = true))
        assertEquals(30, WifiRefreshIntervalPolicy.effectiveSeconds(5, wifiScanThrottleEnabled = null))
    }

    @Test
    fun normalSupportedIntervalsDoNotDependOnThrottleState() {
        listOf(10, 15, 30, 60).forEach { seconds ->
            listOf<Boolean?>(false, true, null).forEach { throttleEnabled ->
                assertTrue(WifiRefreshIntervalPolicy.isSelectable(seconds, throttleEnabled))
                assertEquals(seconds, WifiRefreshIntervalPolicy.effectiveSeconds(seconds, throttleEnabled))
            }
        }
    }

    @Test
    fun invalidValueAlwaysUsesTheDefaultEffectiveInterval() {
        assertFalse(WifiRefreshIntervalPolicy.isSelectable(20, wifiScanThrottleEnabled = false))
        assertEquals(30, WifiRefreshIntervalPolicy.effectiveSeconds(20, wifiScanThrottleEnabled = false))
    }
}
