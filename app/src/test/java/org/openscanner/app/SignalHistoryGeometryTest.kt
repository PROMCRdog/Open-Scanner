package org.openscanner.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openscanner.core.model.SignalSample

class SignalHistoryGeometryTest {
    @Test
    fun pausedHistoryLeavesAVisibleGapBeforeNow() {
        val window = signalHistoryWindow(
            samples = listOf(SignalSample(0, -60), SignalSample(30_000, -58)),
            nowElapsedMs = 90_000,
        )

        assertEquals(90_000L, window.spanMs)
        assertEquals(60_000L, window.staleTailMs)
        assertEquals(1f / 3f, window.xFraction(30_000), 0.0001f)
        assertTrue(window.xFraction(30_000) < 1f)
    }

    @Test
    fun currentFinalSampleEndsAtNow() {
        val window = signalHistoryWindow(
            samples = listOf(SignalSample(30_000, -60), SignalSample(90_000, -58)),
            nowElapsedMs = 90_000,
        )

        assertEquals(0L, window.staleTailMs)
        assertEquals(1f, window.xFraction(90_000), 0f)
    }

    @Test
    fun emptyRetainedHistoryHasNoLastSampleEvidence() {
        val window = signalHistoryWindow(emptyList(), nowElapsedMs = 90_000)

        assertEquals(0L, window.spanMs)
        assertEquals(0L, window.staleTailMs)
        assertEquals(null, window.lastSampleElapsedMs)
    }

    // ---- Y axis ticks ----

    @Test
    fun emptyHistoryPadsAxisAroundLatestValue() {
        val axis = signalHistoryYAxis(emptyList(), latestDbm = -65)

        assertTrue(axis.minDbm >= -100)
        assertTrue(axis.maxDbm <= -20)
        assertTrue(-65 in axis.minDbm..axis.maxDbm)
        assertTicksAligned(axis)
    }

    @Test
    fun singleSampleGetsPaddedAxisAroundTheValue() {
        val axis = signalHistoryYAxis(listOf(SignalSample(0, -60)), latestDbm = -60)

        assertTrue(axis.minDbm < -60)
        assertTrue(axis.maxDbm >= -60)
        assertTrue(axis.ticksDbm.size >= 3)
        assertTicksAligned(axis)
    }

    @Test
    fun typicalRangeSnapsOutwardToStepMultiples() {
        val samples = listOf(SignalSample(0, -72), SignalSample(1_000, -45))
        val axis = signalHistoryYAxis(samples, latestDbm = -45)

        assertTrue(axis.minDbm <= -72)
        assertTrue(axis.maxDbm >= -45)
        assertEquals(0, axis.minDbm % axis.stepDbm)
        assertEquals(0, axis.maxDbm % axis.stepDbm)
        assertTicksAligned(axis)
    }

    @Test
    fun outOfRangeSamplesAreClampedToHonestRssiWindow() {
        val samples = listOf(SignalSample(0, -140), SignalSample(1_000, 5))
        val axis = signalHistoryYAxis(samples, latestDbm = 5)

        assertEquals(-100, axis.minDbm)
        assertEquals(-20, axis.maxDbm)
        assertTicksAligned(axis)
    }

    @Test
    fun axisNeverProducesMoreThanSixLabels() {
        val samples = listOf(SignalSample(0, -99), SignalSample(1_000, -21))
        val axis = signalHistoryYAxis(samples, latestDbm = -21)

        assertTrue(axis.ticksDbm.size <= 6)
        assertTicksAligned(axis)
    }

    // ---- Time axis ticks ----

    @Test
    fun zeroSpanHasOnlyTheNowTick() {
        val ticks = signalHistoryTimeTicks(0L)

        assertEquals(listOf(SignalHistoryTimeTick(0L, "Now")), ticks)
    }

    @Test
    fun oneMinuteSpanUsesFifteenSecondSteps() {
        val ticks = signalHistoryTimeTicks(60_000L)

        assertEquals(
            listOf(0L, 15_000L, 30_000L, 45_000L, 60_000L),
            ticks.map { it.offsetFromEndMs },
        )
        assertEquals("Now", ticks.first().label)
        assertEquals("-15s", ticks[1].label)
        assertEquals("-1m", ticks.last().label)
    }

    @Test
    fun ninetySecondSpanUsesThirtySecondSteps() {
        val ticks = signalHistoryTimeTicks(90_000L)

        assertEquals(
            listOf(0L, 30_000L, 60_000L, 90_000L),
            ticks.map { it.offsetFromEndMs },
        )
        assertEquals("-1m 30s", ticks.last().label)
    }

    @Test
    fun fiveMinuteSpanUsesTwoMinuteStepsAndLabelsTheWindowStart() {
        val ticks = signalHistoryTimeTicks(300_000L)

        assertEquals(
            listOf(0L, 120_000L, 240_000L, 300_000L),
            ticks.map { it.offsetFromEndMs },
        )
        assertEquals("-2m", ticks[1].label)
        assertEquals("-5m", ticks.last().label)
    }

    @Test
    fun oneHourSpanUsesFifteenMinuteSteps() {
        val ticks = signalHistoryTimeTicks(3_600_000L)

        assertEquals(
            listOf(0L, 900_000L, 1_800_000L, 2_700_000L, 3_600_000L),
            ticks.map { it.offsetFromEndMs },
        )
        assertEquals("-1h", ticks.last().label)
    }

    @Test
    fun windowStartJustAfterARoundTickDropsTheCrowdingTick() {
        // 3m 18s span: the -3m tick would sit 18s from the window start and
        // its label would collide with the "-3m 18s" window-start label.
        val ticks = signalHistoryTimeTicks(198_000L)

        assertEquals(
            listOf(0L, 60_000L, 120_000L, 198_000L),
            ticks.map { it.offsetFromEndMs },
        )
        assertEquals("-3m 18s", ticks.last().label)
    }

    @Test
    fun windowStartJustBeforeARoundTickKeepsEvenSteps() {
        // 1m 59s span: the -1m 30s tick is 29s (>= half the 30s step) from the
        // window start, so it stays and the steps remain even.
        val ticks = signalHistoryTimeTicks(119_000L)

        assertEquals(
            listOf(0L, 30_000L, 60_000L, 90_000L, 119_000L),
            ticks.map { it.offsetFromEndMs },
        )
        assertEquals("-1m 59s", ticks.last().label)
    }

    @Test
    fun shortSpanDropsTickThatCrowdsWindowStart() {
        // 50s span: -45s would leave a cramped 5s interval before "-50s".
        val ticks = signalHistoryTimeTicks(50_000L)

        assertEquals(
            listOf(0L, 15_000L, 30_000L, 50_000L),
            ticks.map { it.offsetFromEndMs },
        )
        assertEquals("-50s", ticks.last().label)
    }

    @Test
    fun windowStartNeverCrowdsTheLastTickAndStepsStayEven() {
        listOf(50_000L, 119_000L, 150_000L, 198_000L, 210_000L, 777_000L, 36_000_000L).forEach { span ->
            val offsets = signalHistoryTimeTicks(span).map { it.offsetFromEndMs }
            val gaps = offsets.zipWithNext { a, b -> b - a }
            val step = gaps.first()
            gaps.dropLast(1).forEach { gap ->
                assertEquals("span $span broke the even step", step, gap)
            }
            if (gaps.size > 1) {
                assertTrue(
                    "span $span left only ${gaps.last()}ms before the window start (step $step)",
                    gaps.last() >= step / 2,
                )
            }
            assertEquals(0L, offsets.first())
            assertEquals(span, offsets.last())
        }
    }

    @Test
    fun ticksNeverExceedMaxAndAlwaysCoverWindowStart() {
        listOf(1_000L, 45_000L, 60_000L, 90_000L, 300_000L, 777_000L, 3_600_000L, 36_000_000L).forEach { span ->
            val ticks = signalHistoryTimeTicks(span)
            assertTrue("span $span produced ${ticks.size} ticks", ticks.size <= 5)
            assertEquals(0L, ticks.first().offsetFromEndMs)
            assertEquals(span, ticks.last().offsetFromEndMs)
            assertEquals("Now", ticks.first().label)
        }
    }

    private fun assertTicksAligned(axis: SignalHistoryYAxis) {
        assertEquals(axis.minDbm, axis.ticksDbm.first())
        assertEquals(axis.maxDbm, axis.ticksDbm.last())
        axis.ticksDbm.zipWithNext().forEach { (a, b) -> assertEquals(axis.stepDbm, b - a) }
        assertTrue(axis.ticksDbm.size >= 3)
    }
}
