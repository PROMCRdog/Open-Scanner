package org.openscanner.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.openscanner.core.model.SecurityType
import org.openscanner.core.model.WifiBand
import org.openscanner.core.model.WifiChannelGroup

class SpectrumDisplaySelectionTest {
    private val networks = (1..6).map { index -> network("network-$index", selected = index == 1) }

    @Test
    fun allModeDisplaysEveryNetworkWithoutLegacyCap() {
        val displayed = visibleSpectrumNetworks(
            networks = networks,
            customSelectionIds = null,
            focusedNetworkId = "network-1",
        )

        assertEquals(networks.map { it.uiId }, displayed.map { it.uiId })
    }

    @Test
    fun customModeFiltersNetworksButKeepsFocusedNetworkPinned() {
        val displayed = visibleSpectrumNetworks(
            networks = networks,
            customSelectionIds = setOf("network-3", "network-6"),
            focusedNetworkId = "network-1",
        )

        assertEquals(listOf("network-1", "network-3", "network-6"), displayed.map { it.uiId })
    }

    @Test
    fun focusedNetworkCannotBeHidden() {
        val custom = updateSpectrumSelection(
            networks = networks,
            customSelectionIds = setOf("network-1", "network-3"),
            focusedNetworkId = "network-1",
            networkId = "network-1",
            displayed = false,
        )

        assertEquals(setOf("network-1", "network-3"), custom)
    }

    @Test
    fun selectingEveryAvailableNetworkReturnsToAdaptiveAllMode() {
        var custom: Set<String>? = setOf("network-1")
        networks.drop(1).forEach { network ->
            custom = updateSpectrumSelection(
                networks = networks,
                customSelectionIds = custom,
                focusedNetworkId = "network-1",
                networkId = network.uiId,
                displayed = true,
            )
        }

        assertNull(custom)
    }

    @Test
    fun onlyFocusedModeFollowsANewFocus() {
        val custom = updateSpectrumSelectionForFocus(
            networks = networks,
            customSelectionIds = setOf("network-1"),
            previousFocusedNetworkId = "network-1",
            newFocusedNetworkId = "network-4",
        )

        assertEquals(setOf("network-4"), custom)
    }

    @Test
    fun changingFocusKeepsExistingVisibleNetworksAndShowsTheNewFocus() {
        val custom = updateSpectrumSelectionForFocus(
            networks = networks,
            customSelectionIds = setOf("network-1", "network-3"),
            previousFocusedNetworkId = "network-1",
            newFocusedNetworkId = "network-4",
        )

        assertEquals(setOf("network-1", "network-3", "network-4"), custom)
    }

    private fun network(id: String, selected: Boolean): NetworkUiModel = NetworkUiModel(
        uiId = id,
        name = id,
        bssid = null,
        band = WifiBand.GHZ_5,
        channelGroup = WifiChannelGroup.GHZ_5_8,
        channel = 149,
        frequencyMhz = 5_745,
        footprintCenterFrequencyMhz = 5_775,
        channelWidthMhz = 80,
        signalDbm = -50,
        securityTypes = setOf(SecurityType.WPA3_PERSONAL),
        generation = null,
        connected = selected,
        selected = selected,
    )
}
