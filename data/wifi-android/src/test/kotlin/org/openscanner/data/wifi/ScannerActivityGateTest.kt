package org.openscanner.data.wifi

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openscanner.core.model.ScannerPhase

class ScannerActivityGateTest {
    @Test
    fun foregroundAndLifecycleStateTakePrecedenceWhenResolvingThePhase() {
        assertEquals(
            ScannerPhase.PAUSED,
            scannerPhaseForState(
                receiverRegistered = false,
                paused = false,
                blockedPhase = ScannerPhase.PERMISSION_REQUIRED,
                hasSnapshot = true,
            ),
        )
        assertEquals(
            ScannerPhase.PAUSED,
            scannerPhaseForState(
                receiverRegistered = true,
                paused = true,
                blockedPhase = ScannerPhase.WIFI_DISABLED,
                hasSnapshot = true,
            ),
        )
        assertEquals(
            ScannerPhase.LOCATION_DISABLED,
            scannerPhaseForState(
                receiverRegistered = true,
                paused = false,
                blockedPhase = ScannerPhase.LOCATION_DISABLED,
                hasSnapshot = true,
            ),
        )
        assertEquals(
            ScannerPhase.LIVE,
            scannerPhaseForState(
                receiverRegistered = true,
                paused = false,
                blockedPhase = null,
                hasSnapshot = true,
            ),
        )
        assertEquals(
            ScannerPhase.CHECKING,
            scannerPhaseForState(
                receiverRegistered = true,
                paused = false,
                blockedPhase = null,
                hasSnapshot = false,
            ),
        )
    }

    @Test
    fun inactiveCoordinatorCannotStartRadioAction() {
        var invoked = false

        val result = runWhenScannerActive(
            lock = Any(),
            isActive = { false },
        ) {
            invoked = true
            true
        }

        assertNull(result)
        assertFalse(invoked)
    }

    @Test
    fun deactivationCannotOvertakeAnAuthorizedRadioAction() {
        val lock = Any()
        var active = true
        val actionEntered = CountDownLatch(1)
        val releaseAction = CountDownLatch(1)
        val actionCompleted = CountDownLatch(1)
        val deactivationAttempted = CountDownLatch(1)
        val deactivated = CountDownLatch(1)

        val requestThread = thread(start = true) {
            runWhenScannerActive(lock, isActive = { active }) {
                actionEntered.countDown()
                if (releaseAction.await(1, TimeUnit.SECONDS)) actionCompleted.countDown()
            }
        }
        assertTrue(actionEntered.await(1, TimeUnit.SECONDS))

        val stopThread = thread(start = true) {
            deactivationAttempted.countDown()
            synchronized(lock) { active = false }
            deactivated.countDown()
        }
        assertTrue(deactivationAttempted.await(1, TimeUnit.SECONDS))
        assertFalse(deactivated.await(100, TimeUnit.MILLISECONDS))

        releaseAction.countDown()
        requestThread.join(1_000)
        stopThread.join(1_000)
        assertTrue(actionCompleted.await(1, TimeUnit.SECONDS))
        assertTrue(deactivated.await(1, TimeUnit.SECONDS))
        assertFalse(requestThread.isAlive)
        assertFalse(stopThread.isAlive)
        assertFalse(active)
    }
}
