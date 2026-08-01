package org.openscanner.core.domain

import org.openscanner.core.model.WifiBand
import org.openscanner.core.model.WifiChannel
import org.openscanner.core.model.WifiChannelGroup

object WifiChannelMapper {
    private val fiveGhzChannels = buildSet {
        addAll(listOf(34, 36, 38, 40, 42, 44, 46, 48, 50, 52, 54, 56, 58, 60, 62, 64))
        addAll((100..144 step 2).toList())
        addAll((149..177 step 2).toList())
    }

    fun fromFrequency(frequencyMhz: Int): WifiChannel = when {
        frequencyMhz in 2412..2472 && (frequencyMhz - 2407) % 5 == 0 -> WifiChannel(
            band = WifiBand.GHZ_2_4,
            number = (frequencyMhz - 2407) / 5,
            centerFrequencyMhz = frequencyMhz,
        )

        frequencyMhz == 2484 -> WifiChannel(
            band = WifiBand.GHZ_2_4,
            number = 14,
            centerFrequencyMhz = frequencyMhz,
        )

        frequencyMhz >= 5000 &&
            (frequencyMhz - 5000) % 5 == 0 &&
            (frequencyMhz - 5000) / 5 in fiveGhzChannels -> WifiChannel(
            band = WifiBand.GHZ_5,
            number = (frequencyMhz - 5000) / 5,
            centerFrequencyMhz = frequencyMhz,
        )

        frequencyMhz == 5935 -> WifiChannel(
            band = WifiBand.GHZ_6,
            number = 2,
            centerFrequencyMhz = frequencyMhz,
        )

        frequencyMhz in 5955..7115 &&
            (frequencyMhz - 5950) % 5 == 0 &&
            ((frequencyMhz - 5950) / 5) % 4 == 1 -> WifiChannel(
            band = WifiBand.GHZ_6,
            number = (frequencyMhz - 5950) / 5,
            centerFrequencyMhz = frequencyMhz,
        )

        else -> WifiChannel(
            band = WifiBand.UNKNOWN,
            number = null,
            centerFrequencyMhz = frequencyMhz,
        )
    }

    /**
     * Groups only a channel that is consistent with this mapper's validated
     * frequency-to-channel result. Inconsistent hand-built values stay
     * unknown instead of being pushed into a nearby range.
     */
    fun group(channel: WifiChannel): WifiChannelGroup {
        val validated = fromFrequency(channel.centerFrequencyMhz)
        if (validated.band != channel.band || validated.number != channel.number) {
            return WifiChannelGroup.UNKNOWN
        }
        return when {
            validated.band == WifiBand.GHZ_2_4 && validated.number in 1..14 ->
                WifiChannelGroup.GHZ_2_4
            validated.band == WifiBand.GHZ_5 && validated.number in 34..64 ->
                WifiChannelGroup.GHZ_5_2
            validated.band == WifiBand.GHZ_5 && validated.number in 100..144 ->
                WifiChannelGroup.GHZ_5_5_DFS
            validated.band == WifiBand.GHZ_5 && validated.number in 149..177 ->
                WifiChannelGroup.GHZ_5_8
            validated.band == WifiBand.GHZ_6 && validated.number in 1..233 ->
                WifiChannelGroup.GHZ_6
            else -> WifiChannelGroup.UNKNOWN
        }
    }

    fun groupFromFrequency(frequencyMhz: Int): WifiChannelGroup = group(fromFrequency(frequencyMhz))
}
