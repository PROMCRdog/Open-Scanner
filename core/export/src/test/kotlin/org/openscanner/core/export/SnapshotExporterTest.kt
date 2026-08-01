package org.openscanner.core.export

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

class SnapshotExporterTest {
    @Test
    fun everySupportedFormatRedactsRawIdentifiers() {
        ExportFormat.entries.forEach { format ->
            val output = SnapshotExporter.exportRedacted(fixture(), format)
            assertFalse("Secret Lab" in output)
            assertFalse("aa:bb:cc:dd:ee:ff" in output)
            assertTrue("redacted" in output.lowercase() || "Network 1" in output)
        }
    }

    private fun fixture(): ScanSnapshot = ScanSnapshot(
        sequenceId = 7,
        capturedAtEpochMs = 100_234,
        capturedAtElapsedMs = 50_000,
        sourceTimestampMicros = 40_000,
        requestAccepted = true,
        resultsUpdated = true,
        likelyThrottled = false,
        observations = listOf(
            AccessPointObservation(
                id = "aa:bb:cc:dd:ee:ff",
                ssid = "Secret Lab",
                bssid = "aa:bb:cc:dd:ee:ff",
                channel = WifiChannel(WifiBand.GHZ_5, 36, 5180),
                channelWidthMhz = 80,
                footprintCenterFrequencyMhz = 5210,
                rssiDbm = -48,
                security = setOf(SecurityType.WPA3_PERSONAL),
                generation = WifiGeneration.WIFI_6,
                timestampMicros = 40_000,
                isConnected = true,
            ),
        ),
        connection = ConnectionEvidence(
            connected = true,
            bssid = "aa:bb:cc:dd:ee:ff",
            ssid = "Secret Lab",
            validated = true,
            captivePortal = false,
            linkSpeedMbps = 1200,
            rxLinkSpeedMbps = 900,
            txLinkSpeedMbps = 800,
            ipAddress = "192.168.1.20",
            gateway = "192.168.1.1",
            dnsServers = listOf("192.168.1.1"),
        ),
    )
}
