package org.openscanner.data.wifi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanCadenceControllerTest {
    @Test
    fun activationRequestsImmediatelyAndDoesNotOverlap() {
        val controller = ScanCadenceController()
        controller.setActive(true)

        assertEquals(1_000L, controller.nextWakeElapsedMs(1_000L))
        assertTrue(controller.beginRequestIfDue(1_000L))
        assertTrue(controller.isRequestInFlight)
        assertFalse(controller.beginRequestIfDue(6_000L))
        assertEquals(16_000L, controller.nextWakeElapsedMs(6_000L))
    }

    @Test
    fun fiveSecondModeTargetsLastRequestStartOnMonotonicClock() {
        val controller = ScanCadenceController()
        controller.updateConfiguration(5, wifiScanThrottleEnabled = false)
        controller.setActive(true)
        assertTrue(controller.beginRequestIfDue(10_000L))

        assertTrue(controller.markScanResultsAvailable())
        controller.markResultProcessingCompleted()
        assertEquals(15_000L, controller.nextWakeElapsedMs(12_000L))
        assertFalse(controller.beginRequestIfDue(14_999L))
        assertTrue(controller.beginRequestIfDue(15_000L))
    }

    @Test
    fun lateCompletionMakesOverdueRequestImmediatelyEligible() {
        val controller = ScanCadenceController()
        controller.updateConfiguration(5, wifiScanThrottleEnabled = false)
        controller.setActive(true)
        assertTrue(controller.beginRequestIfDue(1_000L))

        assertTrue(controller.markScanResultsAvailable())
        controller.markResultProcessingCompleted()
        assertEquals(6_000L, controller.nextWakeElapsedMs(8_000L))
        assertTrue(controller.beginRequestIfDue(8_000L))
    }

    @Test
    fun manualRequestsCoalesceWhileRequestIsInFlight() {
        val controller = ScanCadenceController()
        controller.setActive(true)
        assertTrue(controller.beginRequestIfDue(1_000L))

        controller.enqueueManualRequest()
        controller.enqueueManualRequest()
        assertFalse(controller.beginRequestIfDue(2_000L))

        assertTrue(controller.markScanResultsAvailable())
        controller.markResultProcessingCompleted()
        assertEquals(2_000L, controller.nextWakeElapsedMs(2_000L))
        assertTrue(controller.beginRequestIfDue(2_000L))
        assertTrue(controller.markScanResultsAvailable())
        controller.markResultProcessingCompleted()
        assertEquals(32_000L, controller.nextWakeElapsedMs(3_000L))
    }

    @Test
    fun timeoutReleasesInFlightRequestAfterFifteenSeconds() {
        val controller = ScanCadenceController()
        controller.updateConfiguration(5, wifiScanThrottleEnabled = false)
        controller.setActive(true)
        assertTrue(controller.beginRequestIfDue(5_000L))

        assertFalse(controller.consumeTimeoutIfDue(19_999L))
        assertTrue(controller.consumeTimeoutIfDue(20_000L))
        assertFalse(controller.isRequestInFlight)
        assertTrue(controller.beginRequestIfDue(20_000L))
    }

    @Test
    fun delayedResultCommitBlocksAnOverdueRequestAndQueuedManualRefresh() {
        val controller = ScanCadenceController()
        controller.updateConfiguration(5, wifiScanThrottleEnabled = false)
        controller.setActive(true)
        assertTrue(controller.beginRequestIfDue(1_000L))

        assertTrue(controller.markScanResultsAvailable())
        controller.enqueueManualRequest()
        assertTrue(controller.isResultProcessing)
        assertNull(controller.nextWakeElapsedMs(8_000L))
        assertFalse(controller.beginRequestIfDue(8_000L))

        controller.markResultProcessingCompleted()
        assertEquals(8_000L, controller.nextWakeElapsedMs(8_000L))
        assertTrue(controller.beginRequestIfDue(8_000L))
    }

    @Test
    fun rejectedRequestWaitsUntilTheNextStartBasedDeadline() {
        val controller = ScanCadenceController()
        controller.updateConfiguration(10, wifiScanThrottleEnabled = false)
        controller.setActive(true)
        assertTrue(controller.beginRequestIfDue(2_000L))

        controller.markRequestRejected()
        assertFalse(controller.beginRequestIfDue(11_999L))
        assertEquals(12_000L, controller.nextWakeElapsedMs(3_000L))
        assertTrue(controller.beginRequestIfDue(12_000L))
    }

    @Test
    fun requestedFiveSecondsIsRetainedUntilCapabilityResolvesOff() {
        val controller = ScanCadenceController()
        controller.updateConfiguration(5, wifiScanThrottleEnabled = null)
        assertEquals(5, controller.requestedIntervalSeconds)
        assertEquals(30_000L, controller.effectiveIntervalMs)

        controller.updateConfiguration(wifiScanThrottleEnabled = false)
        assertEquals(5, controller.requestedIntervalSeconds)
        assertEquals(5_000L, controller.effectiveIntervalMs)
    }

    @Test
    fun configurationChangeRecomputesExistingDeadline() {
        val controller = ScanCadenceController()
        controller.updateConfiguration(30, wifiScanThrottleEnabled = false)
        controller.setActive(true)
        assertTrue(controller.beginRequestIfDue(1_000L))
        assertTrue(controller.markScanResultsAvailable())
        controller.markResultProcessingCompleted()

        controller.updateConfiguration(5, wifiScanThrottleEnabled = false)
        assertEquals(6_000L, controller.nextWakeElapsedMs(2_000L))
        assertTrue(controller.beginRequestIfDue(6_000L))
    }

    @Test
    fun deactivationCancelsPendingAndResumeStartsImmediately() {
        val controller = ScanCadenceController()
        controller.setActive(true)
        assertTrue(controller.beginRequestIfDue(1_000L))
        assertTrue(controller.markScanResultsAvailable())
        controller.enqueueManualRequest()
        assertTrue(controller.isResultProcessing)

        controller.setActive(false)
        assertFalse(controller.isRequestInFlight)
        assertFalse(controller.isResultProcessing)
        assertNull(controller.nextWakeElapsedMs(2_000L))

        controller.setActive(true)
        assertEquals(3_000L, controller.nextWakeElapsedMs(3_000L))
        assertTrue(controller.beginRequestIfDue(3_000L))
    }

    @Test
    fun invalidConfigurationResetsToThirtySeconds() {
        val controller = ScanCadenceController()
        controller.updateConfiguration(20, wifiScanThrottleEnabled = false)
        assertEquals(30, controller.requestedIntervalSeconds)
        assertEquals(30_000L, controller.effectiveIntervalMs)
    }
}
