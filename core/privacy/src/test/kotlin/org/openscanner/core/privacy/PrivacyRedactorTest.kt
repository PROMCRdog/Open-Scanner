package org.openscanner.core.privacy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openscanner.core.model.AccessPointObservation
import org.openscanner.core.model.ConnectionEvidence
import org.openscanner.core.model.SecurityType
import org.openscanner.core.model.WifiBand
import org.openscanner.core.model.WifiChannel
import org.openscanner.core.model.WifiGeneration

class PrivacyRedactorTest {
    @Test
    fun redactedObservationContainsNoRawNameOrFullBssid() {
        val raw = AccessPointObservation(
            id = "aa:bb:cc:dd:ee:ff",
            ssid = "Private Home",
            bssid = "aa:bb:cc:dd:ee:ff",
            channel = WifiChannel(WifiBand.GHZ_5, 36, 5180),
            channelWidthMhz = 80,
            footprintCenterFrequencyMhz = 5210,
            rssiDbm = -54,
            security = setOf(SecurityType.WPA3_PERSONAL),
            generation = WifiGeneration.WIFI_6,
            timestampMicros = 10L,
            isConnected = true,
        )

        val redacted = PrivacyRedactor.redact(raw, aliasNumber = 3)
        val rendered = listOf(redacted.id, redacted.ssid, redacted.bssid).joinToString(" ")

        assertFalse("Private Home" in rendered)
        assertFalse("aa:bb:cc:dd:ee:ff" in rendered)
        assertTrue("Network 3" in rendered)
    }

    @Test
    fun redactedConnectionContainsNoRawNetworkIdentifiers() {
        val redacted = PrivacyRedactor.redact(
            ConnectionEvidence(
                connected = true,
                bssid = "aa:bb:cc:dd:ee:ff",
                ssid = "Private Home",
                validated = true,
                captivePortal = false,
                linkSpeedMbps = 600,
                rxLinkSpeedMbps = 500,
                txLinkSpeedMbps = 450,
                ipAddress = "192.168.50.22",
                gateway = "192.168.50.1",
                dnsServers = listOf("192.168.50.1", "2001:db8::53"),
            ),
        )
        val rendered = listOfNotNull(
            redacted.bssid,
            redacted.ssid,
            redacted.ipAddress,
            redacted.gateway,
            *redacted.dnsServers.toTypedArray(),
        ).joinToString(" ")

        assertFalse("Private Home" in rendered)
        assertFalse("aa:bb:cc:dd:ee:ff" in rendered)
        assertFalse("192.168.50" in rendered)
        assertFalse("2001:db8" in rendered)
    }
}
