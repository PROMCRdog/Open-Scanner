package org.openscanner.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import org.openscanner.core.model.AccessPointObservation
import org.openscanner.core.model.SecurityType
import org.openscanner.core.model.WifiBand
import org.openscanner.core.model.WifiChannel
import org.openscanner.core.model.WifiChannelGroup
import org.openscanner.core.model.WifiGeneration

class NeighborhoodPostureAnalyzerTest {
    @Test
    fun countsOnlyObservedChannelSecurityAndGenerationFacts() {
        val summary = NeighborhoodPostureAnalyzer.summarize(
            listOf(
                observation("one", 5_180, 36, setOf(SecurityType.WPA2_PERSONAL), WifiGeneration.WIFI_5),
                observation("two", 5_745, 149, setOf(SecurityType.WPA3_PERSONAL), WifiGeneration.WIFI_6),
                observation("three", 5_805, 161, setOf(SecurityType.WPA3_PERSONAL), WifiGeneration.WIFI_6),
            ),
        )

        assertEquals(3, summary.accessPointCount)
        assertEquals(1, summary.channelGroups[WifiChannelGroup.GHZ_5_2])
        assertEquals(2, summary.channelGroups[WifiChannelGroup.GHZ_5_8])
        assertEquals(2, summary.securityProfiles[SecurityType.WPA3_PERSONAL.label])
        assertEquals(2, summary.generations[WifiGeneration.WIFI_6])
    }

    private fun observation(
        id: String,
        frequencyMhz: Int,
        channel: Int,
        security: Set<SecurityType>,
        generation: WifiGeneration,
    ) = AccessPointObservation(
        id = id,
        ssid = id,
        bssid = id,
        channel = WifiChannel(WifiBand.GHZ_5, channel, frequencyMhz),
        channelWidthMhz = 20,
        footprintCenterFrequencyMhz = frequencyMhz,
        rssiDbm = -60,
        security = security,
        generation = generation,
        timestampMicros = 1L,
        isConnected = false,
    )
}
