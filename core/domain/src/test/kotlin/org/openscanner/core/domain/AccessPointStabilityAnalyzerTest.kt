package org.openscanner.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessPointStabilityAnalyzerTest {
    @Test
    fun ignoresFramesBeforeTheAccessPointWasFirstSeen() {
        val summary = AccessPointStabilityAnalyzer.summarize(listOf(null, null, -50, -52, -49, -51))

        assertEquals(AccessPointStabilityLevel.STEADY, summary.level)
        assertEquals(4, summary.assessedSnapshots)
        assertEquals(4, summary.presentSnapshots)
        assertEquals(3, summary.rssiRangeDb)
    }

    @Test
    fun labelsRepeatedAbsenceAsFlapping() {
        val summary = AccessPointStabilityAnalyzer.summarize(listOf(-50, null, -52, null, -51, -53))

        assertEquals(AccessPointStabilityLevel.FLAPPING, summary.level)
        assertEquals(2.0 / 6.0, requireNotNull(summary.absentShare), 0.0001)
    }

    @Test
    fun labelsWideRssiOscillationAsVariable() {
        val summary = AccessPointStabilityAnalyzer.summarize(listOf(-45, -62, -50, -58))

        assertEquals(AccessPointStabilityLevel.VARIABLE, summary.level)
        assertEquals(17, summary.rssiRangeDb)
    }

    @Test
    fun withholdsClassificationWithoutEnoughHistory() {
        val summary = AccessPointStabilityAnalyzer.summarize(listOf(null, -50, -51))

        assertEquals(AccessPointStabilityLevel.INSUFFICIENT, summary.level)
        assertEquals(2, summary.assessedSnapshots)
        assertEquals(2, summary.presentSnapshots)
    }
}
