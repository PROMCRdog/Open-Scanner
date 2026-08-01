package org.openscanner.data.wifi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openscanner.core.model.AccessPointObservation
import org.openscanner.core.model.ConnectionEvidence
import org.openscanner.core.model.ScanSnapshot
import org.openscanner.core.model.SecurityType
import org.openscanner.core.model.WifiBand
import org.openscanner.core.model.WifiChannel
import org.openscanner.core.model.WifiGeneration

class SnapshotReusePolicyTest {
    @Test
    fun reusedRadioContentRefreshesConnectionAndRowMarkerTogether() {
        val previousObservation = observation(id = "old", connected = true)
        val previous = snapshot(
            observations = listOf(previousObservation),
            connection = connection(bssid = "old"),
        )
        val refreshedObservations = listOf(
            previousObservation.copy(isConnected = false),
            observation(id = "new", connected = true),
        )

        val refreshed = refreshReusedSnapshot(
            previous = previous,
            observations = refreshedObservations,
            connection = connection(bssid = "new"),
            requestAccepted = null,
            resultsUpdated = false,
            likelyThrottled = true,
        )

        assertEquals("new", refreshed.connection.bssid)
        assertFalse(refreshed.observations.first { it.id == "old" }.isConnected)
        assertTrue(refreshed.observations.first { it.id == "new" }.isConnected)
        assertTrue(refreshed.likelyThrottled)
        assertEquals(previous.sequenceId, refreshed.sequenceId)
    }

    private fun observation(id: String, connected: Boolean) = AccessPointObservation(
        id = id,
        ssid = id,
        bssid = id,
        channel = WifiChannel(WifiBand.GHZ_5, 36, 5_180),
        channelWidthMhz = 80,
        footprintCenterFrequencyMhz = 5_210,
        rssiDbm = -55,
        security = setOf(SecurityType.WPA3_PERSONAL),
        generation = WifiGeneration.WIFI_6,
        timestampMicros = 1_000,
        isConnected = connected,
    )

    private fun connection(bssid: String) = ConnectionEvidence(
        connected = true,
        bssid = bssid,
        ssid = bssid,
        validated = true,
        captivePortal = false,
        linkSpeedMbps = 600,
        rxLinkSpeedMbps = 600,
        txLinkSpeedMbps = 600,
        ipAddress = "192.0.2.2",
        gateway = "192.0.2.1",
        dnsServers = listOf("192.0.2.1"),
    )

    private fun snapshot(
        observations: List<AccessPointObservation>,
        connection: ConnectionEvidence,
    ) = ScanSnapshot(
        sequenceId = 7,
        capturedAtEpochMs = 1_000,
        capturedAtElapsedMs = 1_000,
        sourceTimestampMicros = 1_000,
        requestAccepted = true,
        resultsUpdated = true,
        likelyThrottled = false,
        observations = observations,
        connection = connection,
    )
}
