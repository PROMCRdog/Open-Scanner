package org.openscanner.data.wifi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WifiCandidateSelectionTest {
    @Test
    fun matchesThePlatformReportedPrimaryBssid() {
        assertEquals(
            1,
            choosePhysicalWifiCandidateIndex(
                candidateBssids = listOf("aa:aa:aa:aa:aa:aa", "BB:BB:BB:BB:BB:BB"),
                reportedPrimaryBssid = "bb:bb:bb:bb:bb:bb",
            ),
        )
    }

    @Test
    fun acceptsOneCandidateButAbstainsWhenMultipleAreAmbiguous() {
        assertEquals(0, choosePhysicalWifiCandidateIndex(listOf(null), null))
        assertNull(choosePhysicalWifiCandidateIndex(listOf(null, null), null))
        assertNull(
            choosePhysicalWifiCandidateIndex(
                candidateBssids = listOf("aa:aa:aa:aa:aa:aa", "bb:bb:bb:bb:bb:bb"),
                reportedPrimaryBssid = "cc:cc:cc:cc:cc:cc",
            ),
        )
    }

    @Test
    fun fallsBackToPermissionGatedIdentifiersWhenTransportInfoIsRedacted() {
        assertEquals(
            "aa:bb:cc:dd:ee:ff",
            selectConnectionBssid(
                transportBssid = "02:00:00:00:00:00",
                legacyBssid = "AA:BB:CC:DD:EE:FF",
            ),
        )
        assertEquals(
            "Office Wi-Fi",
            selectConnectionSsid(
                transportSsid = "<unknown ssid>",
                legacySsid = "\"Office Wi-Fi\"",
            ),
        )
    }
}
