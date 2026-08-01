package org.openscanner.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openscanner.app.ui.screens.networkMatchesSearch

class ScanSearchTest {
    @Test
    fun hiddenStateUsesAndroidEvidenceRatherThanSsidText() {
        assertEquals(NetworkNameKind.OBSERVED, networkNameKind(privacyMode = false, ssidHidden = false))
        assertEquals(NetworkNameKind.HIDDEN, networkNameKind(privacyMode = false, ssidHidden = true))
        assertEquals(NetworkNameKind.PRIVACY_ALIAS, networkNameKind(privacyMode = true, ssidHidden = false))
    }

    @Test
    fun localizedVisibleNetworkNameIsSearchable() {
        assertTrue(networkMatchesSearch("隐藏", "隐藏网络", null))
        assertTrue(networkMatchesSearch("网络 7", "网络 7", "••:••:••:••:••:••"))
        assertFalse(networkMatchesSearch("Hidden network", "隐藏网络", null))
    }

    @Test
    fun observedNameAndBssidRemainSearchable() {
        assertTrue(networkMatchesSearch("Office", "Office Wi-Fi", "aa:bb:cc:dd:ee:ff"))
        assertTrue(networkMatchesSearch("CC:DD", "Office Wi-Fi", "aa:bb:cc:dd:ee:ff"))
        assertTrue(networkMatchesSearch("", "Office Wi-Fi", null))
    }
}
