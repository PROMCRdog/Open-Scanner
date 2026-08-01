package org.openscanner.core.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openscanner.core.model.AccessPointObservation
import org.openscanner.core.model.ConnectionEvidence
import org.openscanner.core.model.PlatformCapabilities
import org.openscanner.core.model.ScanSnapshot
import org.openscanner.core.model.ScannerPhase
import org.openscanner.core.model.ScannerState
import org.openscanner.core.model.SecurityType
import org.openscanner.core.model.WifiBand
import org.openscanner.core.model.WifiChannel
import org.openscanner.core.model.WifiGeneration

class WifiLogExporterTest {
    @Test
    fun recorderRedactsBeforeCreatingRecordsAndKeepsAliasesStable() {
        val recorder = WifiLogRecorder(
            selectedFields = WifiLogField.entries.toSet(),
            startedAtEpochMs = 120_999L,
            startedAtElapsedMs = 1_000L,
        )

        assertEquals(WifiLogRecordResult.ADDED, recorder.record(state(sequence = 1), 2_000L))
        assertEquals(WifiLogRecordResult.DUPLICATE, recorder.record(state(sequence = 1), 3_000L))
        assertEquals(WifiLogRecordResult.ADDED, recorder.record(state(sequence = 1), 3_000L, force = true))
        assertEquals(WifiLogRecordResult.ADDED, recorder.record(state(sequence = 2), 4_000L))
        recorder.stop(5_000L)

        val session = recorder.snapshot()
        assertEquals(120_000L, session.startedAtEpochMs)
        assertTrue(session.redacted)
        assertEquals(3, session.records.size)
        assertEquals("Network 1", session.records[0].networkValues[0][WifiLogField.NETWORK_NAME])
        assertEquals("Network 1", session.records[2].networkValues[0][WifiLogField.NETWORK_NAME])
        assertEquals("Network 1", session.records[0].connectionValues[WifiLogField.CONNECTION_NETWORK])
        assertEquals("AP-001", session.records[0].networkValues[0][WifiLogField.BSSID])
        assertEquals("AP-001", session.records[0].connectionValues[WifiLogField.CONNECTION_BSSID])
        assertEquals("•••.•••.•••.•••", session.records[0].connectionValues[WifiLogField.IP_ADDRESS])
    }

    @Test
    fun onlySelectedFieldsAreRetainedAndUnavailableValuesStayExplicit() {
        val recorder = WifiLogRecorder(
            selectedFields = setOf(WifiLogField.RSSI_DBM, WifiLogField.CHANNEL_WIDTH_MHZ),
            startedAtEpochMs = 0L,
            startedAtElapsedMs = 0L,
        )

        recorder.record(state(sequence = 1), 1_000L)
        val values = recorder.snapshot().records.single().networkValues.single()

        assertEquals(setOf(WifiLogField.RSSI_DBM, WifiLogField.CHANNEL_WIDTH_MHZ), values.keys)
        assertEquals("-48", values[WifiLogField.RSSI_DBM])
        assertNull(values[WifiLogField.CHANNEL_WIDTH_MHZ])
    }

    @Test
    fun omitsNetworkRowsWhenNoAccessPointFieldWasSelected() {
        val recorder = WifiLogRecorder(
            selectedFields = setOf(WifiLogField.SCANNER_STATE),
            startedAtEpochMs = 0L,
            startedAtElapsedMs = 0L,
        )

        recorder.record(state(sequence = 1), 1_000L)

        assertTrue(recorder.snapshot().records.single().networkValues.isEmpty())
        assertEquals(0, recorder.snapshot().networkRowCount)
    }

    @Test
    fun stopsCleanlyAtTheRecordSafetyLimit() {
        val recorder = WifiLogRecorder(
            selectedFields = setOf(WifiLogField.SCANNER_STATE),
            startedAtEpochMs = 0L,
            startedAtElapsedMs = 0L,
        )
        repeat(WifiLogRecorder.MAX_RECORDS) { index ->
            assertEquals(
                WifiLogRecordResult.ADDED,
                recorder.record(state(sequence = 1), index.toLong(), force = true),
            )
        }

        assertEquals(
            WifiLogRecordResult.LIMIT_REACHED,
            recorder.record(state(sequence = 1), 1_000L, force = true),
        )
        assertEquals(WifiLogStopReason.SAFETY_LIMIT, recorder.snapshot().stopReason)
    }

    @Test
    fun everyLogFormatIsStructuredAndContainsNoRawIdentifierOrAddress() {
        val recorder = WifiLogRecorder(
            selectedFields = WifiLogField.entries.toSet(),
            startedAtEpochMs = 120_999L,
            startedAtElapsedMs = 1_000L,
        )
        recorder.record(state(sequence = 1), 2_000L)
        recorder.stop(3_000L)

        WifiLogFormat.entries.forEach { format ->
            val document = WifiLogExporter.export(recorder.snapshot(), format)
            assertFalse("Secret Lab" in document.content)
            assertFalse("aa:bb:cc:dd:ee:ff" in document.content)
            assertFalse("192.168.1.20" in document.content)
            assertTrue("Network 1" in document.content)
            assertTrue(document.fileName.endsWith(".${format.extension}"))
        }
    }

    @Test
    fun jsonUsesNumbersBooleansAndNullsInsteadOfStringifyingEverything() {
        val recorder = WifiLogRecorder(
            selectedFields = setOf(
                WifiLogField.SNAPSHOT_SEQUENCE,
                WifiLogField.RSSI_DBM,
                WifiLogField.CHANNEL_WIDTH_MHZ,
                WifiLogField.CONNECTED_AP,
            ),
            startedAtEpochMs = 0L,
            startedAtElapsedMs = 0L,
        )
        recorder.record(state(sequence = 7), 1_000L)

        val json = WifiLogExporter.export(recorder.snapshot(), WifiLogFormat.JSON).content

        assertTrue("\"snapshot_sequence\": 7" in json)
        assertTrue("\"rssi_dbm\": -48" in json)
        assertTrue("\"channel_width_mhz\": null" in json)
        assertTrue("\"connected_ap\": true" in json)
    }

    @Test
    fun explicitUnredactedSessionRetainsRawValuesAndLabelsEveryFormat() {
        val recorder = WifiLogRecorder(
            selectedFields = WifiLogField.entries.toSet(),
            startedAtEpochMs = 120_999L,
            startedAtElapsedMs = 1_000L,
            redacted = false,
        )
        recorder.record(state(sequence = 1), 2_000L)
        recorder.stop(3_000L)

        val session = recorder.snapshot()
        assertFalse(session.redacted)
        assertEquals(120_999L, session.startedAtEpochMs)
        assertEquals("Secret Lab", session.records.single().networkValues.single()[WifiLogField.NETWORK_NAME])
        assertEquals("aa:bb:cc:dd:ee:ff", session.records.single().networkValues.single()[WifiLogField.BSSID])
        assertEquals("192.168.1.20", session.records.single().connectionValues[WifiLogField.IP_ADDRESS])

        WifiLogFormat.entries.forEach { format ->
            val document = WifiLogExporter.export(session, format)
            assertFalse(document.redacted)
            assertTrue("Secret Lab" in document.content)
            assertTrue("aa:bb:cc:dd:ee:ff" in document.content)
            assertTrue("192.168.1.20" in document.content)
            assertTrue("unredacted" in document.fileName)
        }
        val json = WifiLogExporter.export(session, WifiLogFormat.JSON).content
        assertTrue("\"redacted\": false" in json)
        assertTrue("1970-01-01T00:02:00.999Z" in json)
    }

    private fun state(sequence: Long): ScannerState = ScannerState(
        phase = ScannerPhase.LIVE,
        capabilities = PlatformCapabilities(true, true, true),
        snapshot = ScanSnapshot(
            sequenceId = sequence,
            capturedAtEpochMs = 100_234L + sequence,
            capturedAtElapsedMs = 1_500L + sequence,
            sourceTimestampMicros = 1_400_000L + sequence,
            requestAccepted = true,
            resultsUpdated = true,
            likelyThrottled = false,
            observations = listOf(
                AccessPointObservation(
                    id = "aa:bb:cc:dd:ee:ff",
                    ssid = "Secret Lab",
                    bssid = "aa:bb:cc:dd:ee:ff",
                    channel = WifiChannel(WifiBand.GHZ_5, 36, 5_180),
                    channelWidthMhz = null,
                    footprintCenterFrequencyMhz = 5_210,
                    rssiDbm = -48,
                    security = setOf(SecurityType.WPA3_PERSONAL),
                    generation = WifiGeneration.WIFI_6,
                    timestampMicros = 1_400_000L,
                    isConnected = true,
                ),
            ),
            connection = ConnectionEvidence(
                connected = true,
                bssid = "aa:bb:cc:dd:ee:ff",
                ssid = "Secret Lab",
                validated = true,
                captivePortal = false,
                linkSpeedMbps = 1_200,
                rxLinkSpeedMbps = 900,
                txLinkSpeedMbps = 800,
                ipAddress = "192.168.1.20",
                gateway = "192.168.1.1",
                dnsServers = listOf("192.168.1.1"),
            ),
        ),
    )
}
