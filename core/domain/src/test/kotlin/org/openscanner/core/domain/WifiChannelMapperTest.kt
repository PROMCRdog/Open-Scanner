package org.openscanner.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.openscanner.core.model.WifiBand
import org.openscanner.core.model.WifiChannel
import org.openscanner.core.model.WifiChannelGroup

class WifiChannelMapperTest {
    @Test
    fun mapsTwoPointFourGhzIncludingChannelFourteen() {
        assertEquals(1, WifiChannelMapper.fromFrequency(2412).number)
        assertEquals(13, WifiChannelMapper.fromFrequency(2472).number)
        assertEquals(14, WifiChannelMapper.fromFrequency(2484).number)
    }

    @Test
    fun mapsFiveGhzPrimaryCenters() {
        assertEquals(36, WifiChannelMapper.fromFrequency(5180).number)
        assertEquals(149, WifiChannelMapper.fromFrequency(5745).number)
        assertEquals(WifiBand.GHZ_5, WifiChannelMapper.fromFrequency(5745).band)
    }

    @Test
    fun mapsSixGhzAndSpecialCenter() {
        assertEquals(2, WifiChannelMapper.fromFrequency(5935).number)
        assertEquals(1, WifiChannelMapper.fromFrequency(5955).number)
        assertEquals(233, WifiChannelMapper.fromFrequency(7115).number)
    }

    @Test
    fun rejectsMalformedOrNonCenterFrequencies() {
        assertNull(WifiChannelMapper.fromFrequency(2413).number)
        assertNull(WifiChannelMapper.fromFrequency(5960).number)
        assertNull(WifiChannelMapper.fromFrequency(-1).number)
    }

    @Test
    fun groupsValidatedChannelsIntoProductRanges() {
        assertEquals(WifiChannelGroup.GHZ_2_4, WifiChannelMapper.groupFromFrequency(2_412))
        assertEquals(WifiChannelGroup.GHZ_5_2, WifiChannelMapper.groupFromFrequency(5_180))
        assertEquals(WifiChannelGroup.GHZ_5_2, WifiChannelMapper.groupFromFrequency(5_240))
        assertEquals(WifiChannelGroup.GHZ_5_5_DFS, WifiChannelMapper.groupFromFrequency(5_500))
        assertEquals(WifiChannelGroup.GHZ_5_8, WifiChannelMapper.groupFromFrequency(5_745))
        assertEquals(WifiChannelGroup.GHZ_5_8, WifiChannelMapper.groupFromFrequency(5_805))
        assertEquals(WifiChannelGroup.GHZ_5_8, WifiChannelMapper.groupFromFrequency(5_865))
        assertEquals(WifiChannelGroup.GHZ_6, WifiChannelMapper.groupFromFrequency(5_955))
    }

    @Test
    fun unsupportedOrInconsistentChannelsStayUnknown() {
        assertEquals(WifiChannelGroup.UNKNOWN, WifiChannelMapper.groupFromFrequency(5_735))
        assertEquals(WifiChannelGroup.UNKNOWN, WifiChannelMapper.groupFromFrequency(5_960))
        assertEquals(
            WifiChannelGroup.UNKNOWN,
            WifiChannelMapper.group(WifiChannel(WifiBand.GHZ_5, 149, 5_180)),
        )
    }
}
