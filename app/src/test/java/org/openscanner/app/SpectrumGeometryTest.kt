package org.openscanner.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openscanner.core.model.SecurityType
import org.openscanner.core.model.WifiBand
import org.openscanner.core.model.WifiChannelGroup

class SpectrumGeometryTest {
    @Test
    fun unknownWidthUsesPrimaryFrequencyWithoutInventingFootprint() {
        val footprint = network(widthMhz = null, primaryMhz = 5_180, centerMhz = 5_210).spectrumFootprint()

        assertEquals(5_180, footprint.centerFrequencyMhz)
        assertNull(footprint.widthMhz)
    }

    @Test
    fun knownWidthUsesReportedFootprintCenter() {
        val footprint = network(widthMhz = 80, primaryMhz = 5_180, centerMhz = 5_210).spectrumFootprint()

        assertEquals(5_210, footprint.centerFrequencyMhz)
        assertEquals(80, footprint.widthMhz)
    }

    @Test
    fun knownWidthsRemainFrequencyScaledWithoutVisualInflation() {
        val axisStart = 5_150f
        val axisEnd = 5_900f

        listOf(20, 40, 80, 160, 320).forEach { width ->
            assertEquals(
                width / (axisEnd - axisStart) / 2f,
                requireNotNull(normalizedSpectrumHalfSpan(width, axisStart, axisEnd)),
                0.000001f,
            )
        }
    }

    @Test
    fun invalidOrUnknownWidthHasNoNormalizedFootprint() {
        assertNull(normalizedSpectrumHalfSpan(null, 5_150f, 5_900f))
        assertNull(normalizedSpectrumHalfSpan(0, 5_150f, 5_900f))
        assertNull(normalizedSpectrumHalfSpan(20, 5_900f, 5_150f))
    }

    // ---- Y axis ticks ----

    @Test
    fun defaultSignalAxisCoversTheHonestRssiWindowInTenDbSteps() {
        val axis = spectrumYAxis()

        assertEquals(-100, axis.minDbm)
        assertEquals(-30, axis.maxDbm)
        assertEquals(10, axis.stepDbm)
        assertEquals(
            listOf(-100, -90, -80, -70, -60, -50, -40, -30),
            axis.ticksDbm,
        )
    }

    @Test
    fun signalAxisWidensStepWhenLabelBudgetIsTight() {
        val axis = spectrumYAxis(maxTicks = 5)

        assertEquals(20, axis.stepDbm)
        assertTrue(axis.ticksDbm.size <= 5)
        axis.ticksDbm.zipWithNext().forEach { (a, b) -> assertEquals(axis.stepDbm, b - a) }
    }

    @Test
    fun signalAxisTicksStayInsideSwappedBounds() {
        val axis = spectrumYAxis(minDbm = -30, maxDbm = -100)

        assertEquals(-100, axis.minDbm)
        assertEquals(-30, axis.maxDbm)
    }

    // ---- X axis ticks per band ----

    @Test
    fun ghz24TicksAreTheCanonicalChannels() {
        val (start, end) = spectrumAxisRangeMhz(WifiChannelGroup.GHZ_2_4)
        val ticks = spectrumXTicks(WifiChannelGroup.GHZ_2_4, start, end)

        assertEquals(listOf(2_412f, 2_437f, 2_462f, 2_484f), ticks.map { it.frequencyMhz })
        assertEquals(listOf("1", "6", "11", "14"), ticks.map { it.label })
        ticks.forEach { tick ->
            assertTrue(tick.xFraction(start, end) in 0f..1f)
        }
    }

    @Test
    fun ghz5TicksStayInsideTheirValidatedChannelGroups() {
        val lowRange = spectrumAxisRangeMhz(WifiChannelGroup.GHZ_5_2)
        val lowTicks = spectrumXTicks(WifiChannelGroup.GHZ_5_2, lowRange.first, lowRange.second)
        val midRange = spectrumAxisRangeMhz(WifiChannelGroup.GHZ_5_5_DFS)
        val midTicks = spectrumXTicks(WifiChannelGroup.GHZ_5_5_DFS, midRange.first, midRange.second)
        val highRange = spectrumAxisRangeMhz(WifiChannelGroup.GHZ_5_8)
        val highTicks = spectrumXTicks(WifiChannelGroup.GHZ_5_8, highRange.first, highRange.second)

        assertEquals(listOf("34", "36", "44", "52", "64"), lowTicks.map { it.label })
        assertEquals(listOf("100", "112", "128", "140", "144"), midTicks.map { it.label })
        assertEquals(listOf("149", "161", "165", "173", "177"), highTicks.map { it.label })
    }

    @Test
    fun ghz6TicksAreRoundGigahertzSteps() {
        val (start, end) = spectrumAxisRangeMhz(WifiChannelGroup.GHZ_6)
        val ticks = spectrumXTicks(WifiChannelGroup.GHZ_6, start, end)

        assertEquals(
            listOf(6_000f, 6_200f, 6_400f, 6_600f, 6_800f, 7_000f),
            ticks.map { it.frequencyMhz },
        )
        assertEquals(listOf("6.0", "6.2", "6.4", "6.6", "6.8", "7.0"), ticks.map { it.label })
    }

    @Test
    fun unknownBandTicksAreWholeGigahertzSteps() {
        val (start, end) = spectrumAxisRangeMhz(WifiChannelGroup.UNKNOWN)
        val ticks = spectrumXTicks(WifiChannelGroup.UNKNOWN, start, end)

        assertEquals(
            listOf(3_000f, 4_000f, 5_000f, 6_000f, 7_000f),
            ticks.map { it.frequencyMhz },
        )
        assertEquals(listOf("3.0", "4.0", "5.0", "6.0", "7.0"), ticks.map { it.label })
    }

    @Test
    fun unknownGroupUsesObservedRangeInsteadOfClampingUnsupportedFrequencies() {
        val (start, end) = spectrumAxisRangeMhz(
            WifiChannelGroup.UNKNOWN,
            observedFrequenciesMhz = listOf(59_950f, 60_050f),
        )
        val ticks = spectrumXTicks(WifiChannelGroup.UNKNOWN, start, end)

        assertEquals(59_930f, start)
        assertEquals(60_070f, end)
        assertEquals(listOf("59.95", "60.0", "60.05"), ticks.map { it.label })
    }

    @Test
    fun ticksOutsideTheDisplayedWindowAreDropped() {
        val ticks = spectrumXTicks(WifiChannelGroup.GHZ_2_4, axisStartMhz = 2_420f, axisEndMhz = 2_470f)

        assertEquals(listOf(2_437f, 2_462f), ticks.map { it.frequencyMhz })
    }

    @Test
    fun axisTitlesCarryTheUnitPerBand() {
        assertEquals("Channel", spectrumXAxisTitle(WifiChannelGroup.GHZ_2_4))
        assertEquals("Channel", spectrumXAxisTitle(WifiChannelGroup.GHZ_5_2))
        assertEquals("Channel", spectrumXAxisTitle(WifiChannelGroup.GHZ_5_5_DFS))
        assertEquals("Channel", spectrumXAxisTitle(WifiChannelGroup.GHZ_5_8))
        assertEquals("Frequency (GHz)", spectrumXAxisTitle(WifiChannelGroup.GHZ_6))
        assertEquals("Frequency (GHz)", spectrumXAxisTitle(WifiChannelGroup.UNKNOWN))
    }

    private fun network(widthMhz: Int?, primaryMhz: Int, centerMhz: Int?): NetworkUiModel = NetworkUiModel(
        uiId = "synthetic",
        name = "Synthetic network",
        bssid = "••:••:••:••:••:••",
        band = WifiBand.GHZ_5,
        channelGroup = WifiChannelGroup.GHZ_5_2,
        channel = 36,
        frequencyMhz = primaryMhz,
        footprintCenterFrequencyMhz = centerMhz,
        channelWidthMhz = widthMhz,
        signalDbm = -55,
        securityTypes = setOf(SecurityType.WPA3_PERSONAL),
        generation = "Wi-Fi 6",
        connected = false,
        selected = true,
    )
}
