package org.openscanner.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openscanner.core.model.AccessPointObservation
import org.openscanner.core.model.SecurityType
import org.openscanner.core.model.WifiGeneration

class InterferenceAnalyzerTest {
    @Test
    fun reportsCoChannelInterferenceWithoutTreatingOtherBandsAsOverlap() {
        val selected = fixture("one", 5180, -50)
        val summary = InterferenceAnalyzer.summarize(
            selected = selected,
            observations = listOf(
                selected,
                fixture("two", 5180, -65),
                fixture("three", 5190, -70),
                fixture("four", 2412, -30),
            ),
        )

        assertEquals(2, summary.overlappingNetworks)
        assertEquals(1, summary.coChannelNetworks)
        assertTrue(summary.weightedOverlap > 0.0)
    }

    private fun fixture(id: String, frequency: Int, rssi: Int): AccessPointObservation =
        AccessPointObservation(
            id = id,
            ssid = id,
            bssid = "00:11:22:33:44:55",
            channel = WifiChannelMapper.fromFrequency(frequency),
            channelWidthMhz = 20,
            footprintCenterFrequencyMhz = frequency,
            rssiDbm = rssi,
            security = setOf(SecurityType.WPA2_PERSONAL),
            generation = WifiGeneration.WIFI_5,
            timestampMicros = 1L,
            isConnected = false,
        )
}
