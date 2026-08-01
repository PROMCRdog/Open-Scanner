package org.openscanner.data.wifi

import android.net.wifi.ScanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelWidthMapperTest {
    @Test
    fun mapsContiguousPlatformWidths() {
        assertEquals(20, mapPlatformChannelWidth(ScanResult.CHANNEL_WIDTH_20MHZ))
        assertEquals(40, mapPlatformChannelWidth(ScanResult.CHANNEL_WIDTH_40MHZ))
        assertEquals(80, mapPlatformChannelWidth(ScanResult.CHANNEL_WIDTH_80MHZ))
        assertEquals(160, mapPlatformChannelWidth(ScanResult.CHANNEL_WIDTH_160MHZ))
        assertEquals(320, mapPlatformChannelWidth(ScanResult.CHANNEL_WIDTH_320MHZ))
    }

    @Test
    fun preservesUnknownAndNonContiguousWidthsAsUnknown() {
        assertNull(mapPlatformChannelWidth(ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ))
        assertNull(mapPlatformChannelWidth(Int.MIN_VALUE))
    }
}
