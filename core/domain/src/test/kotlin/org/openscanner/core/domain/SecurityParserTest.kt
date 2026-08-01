package org.openscanner.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openscanner.core.model.SecurityType

class SecurityParserTest {
    @Test
    fun emptyCapabilitiesAreOpen() {
        assertEquals(setOf(SecurityType.OPEN), SecurityParser.fromCapabilities(""))
        assertEquals(setOf(SecurityType.OPEN), SecurityParser.fromCapabilities("[ESS]"))
    }

    @Test
    fun parsesWpaTwoWpaThreeTransitionMode() {
        val result = SecurityParser.fromCapabilities("[WPA2-PSK-CCMP][RSN-SAE-CCMP][ESS]")
        assertTrue(SecurityType.WPA2_WPA3_PERSONAL in result)
    }

    @Test
    fun parsesEnhancedOpenAndEnterprise() {
        assertTrue(SecurityType.OWE in SecurityParser.fromCapabilities("[OWE][ESS]"))
        assertTrue(SecurityType.ENTERPRISE in SecurityParser.fromCapabilities("[WPA2-EAP-CCMP][ESS]"))
    }

    @Test
    fun parsesLegacyWepAndWpaPersonal() {
        assertEquals(setOf(SecurityType.WEP), SecurityParser.fromCapabilities("[WEP][ESS]"))
        assertTrue(SecurityType.WPA_PERSONAL in SecurityParser.fromCapabilities("[WPA-PSK-TKIP][ESS]"))
    }

    @Test
    fun unknownRsnCapabilityIsNotMisreportedAsOpen() {
        assertEquals(setOf(SecurityType.UNKNOWN), SecurityParser.fromCapabilities("[RSN-UNKNOWN][ESS]"))
    }
}
