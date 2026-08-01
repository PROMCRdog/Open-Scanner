package org.openscanner.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openscanner.core.model.SignalSample

class SignalHistoryPolicyTest {
    @Test
    fun prunesDisappearedNetworksAndDeduplicatesSourceTime() {
        val result = SignalHistoryPolicy.update(
            existing = mapOf(
                "selected" to listOf(SignalSample(100, -60), SignalSample(200, -58)),
                "disappeared" to listOf(SignalSample(50, -80)),
            ),
            incoming = mapOf("selected" to SignalSample(200, -55)),
            nowElapsedMs = 1_000,
            retentionMs = 900,
            maxPointsPerNetwork = 60,
            maxNetworks = 128,
        )

        assertFalse("disappeared" in result)
        assertEquals(listOf(100L, 200L), result.getValue("selected").map { it.elapsedRealtimeMs })
        assertEquals(-55, result.getValue("selected").last().rssiDbm)
    }

    @Test
    fun enforcesPointAndNetworkCapsByMostRecentEvidence() {
        val result = SignalHistoryPolicy.update(
            existing = emptyMap(),
            incoming = mapOf(
                "old" to SignalSample(100, -80),
                "middle" to SignalSample(200, -70),
                "new" to SignalSample(300, -60),
            ),
            nowElapsedMs = 300,
            retentionMs = 1_000,
            maxPointsPerNetwork = 1,
            maxNetworks = 2,
        )

        assertEquals(2, result.size)
        assertTrue("new" in result)
        assertTrue("middle" in result)
    }

    @Test
    fun clockOnlyUpdatePrunesPausedHistoryPastRetention() {
        val result = SignalHistoryPolicy.update(
            existing = mapOf("paused" to listOf(SignalSample(100, -60))),
            incoming = emptyMap(),
            nowElapsedMs = 1_001,
            retentionMs = 900,
        )

        assertTrue(result.isEmpty())
    }
}
