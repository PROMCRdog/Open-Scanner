package org.openscanner.app

import kotlin.math.ceil
import kotlin.math.floor
import org.openscanner.core.model.WifiChannelGroup

internal data class SpectrumFootprintUi(
    val centerFrequencyMhz: Int,
    val widthMhz: Int?,
)

internal fun NetworkUiModel.spectrumFootprint(): SpectrumFootprintUi {
    val reportedWidth = channelWidthMhz?.takeIf { it > 0 }
    return if (reportedWidth == null) {
        SpectrumFootprintUi(
            centerFrequencyMhz = frequencyMhz,
            widthMhz = null,
        )
    } else {
        SpectrumFootprintUi(
            centerFrequencyMhz = footprintCenterFrequencyMhz ?: frequencyMhz,
            widthMhz = reportedWidth,
        )
    }
}

internal fun normalizedSpectrumHalfSpan(
    widthMhz: Int?,
    axisStartMhz: Float,
    axisEndMhz: Float,
): Float? {
    val width = widthMhz?.takeIf { it > 0 } ?: return null
    val axisSpan = axisEndMhz - axisStartMhz
    if (axisSpan <= 0f) return null
    return width / axisSpan / 2f
}

/** Displayed frequency window per channel group; shared by plot and ticks. */
internal fun spectrumAxisRangeMhz(
    group: WifiChannelGroup,
    observedFrequenciesMhz: List<Float> = emptyList(),
): Pair<Float, Float> = when (group) {
    WifiChannelGroup.GHZ_2_4 -> 2_400f to 2_500f
    WifiChannelGroup.GHZ_5_2 -> 5_150f to 5_350f
    WifiChannelGroup.GHZ_5_5_DFS -> 5_470f to 5_740f
    WifiChannelGroup.GHZ_5_8 -> 5_725f to 5_900f
    WifiChannelGroup.GHZ_6 -> 5_925f to 7_125f
    WifiChannelGroup.UNKNOWN -> unknownSpectrumRange(observedFrequenciesMhz)
}

private fun unknownSpectrumRange(observedFrequenciesMhz: List<Float>): Pair<Float, Float> {
    val valid = observedFrequenciesMhz.filter { it.isFinite() && it > 0f }
    if (valid.isEmpty()) return 2_400f to 7_125f
    val minimum = valid.min()
    val maximum = valid.max()
    val margin = maxOf(20f, (maximum - minimum) * 0.10f)
    return (minimum - margin).coerceAtLeast(0f) to (maximum + margin)
}

/**
 * Vertical axis for the spectrum chart. [ticksDbm] is the exact set of values
 * the chart draws gridlines and labels for, ascending, aligned to multiples of
 * [stepDbm]. The spectrum chart uses a fixed honest RSSI window (-100..-30 dBm)
 * because footprints are plotted against absolute signal strength.
 */
internal data class SpectrumYAxis(
    val minDbm: Int,
    val maxDbm: Int,
    val stepDbm: Int,
) {
    val ticksDbm: List<Int> = (minDbm..maxDbm step stepDbm).toList()
}

/**
 * Computes dBm ticks for the spectrum axis. Step is the smallest of 5/10/20/40 dB
 * that keeps the label count within [maxTicks].
 */
internal fun spectrumYAxis(
    minDbm: Int = -100,
    maxDbm: Int = -30,
    maxTicks: Int = 8,
): SpectrumYAxis {
    val lo = minOf(minDbm, maxDbm)
    val hi = maxOf(minDbm, maxDbm)
    val span = hi - lo
    val step = listOf(5, 10, 20, 40).firstOrNull { span / it + 1 <= maxTicks } ?: 40
    return SpectrumYAxis(minDbm = lo, maxDbm = hi, stepDbm = step)
}

/**
 * One labeled tick on the spectrum frequency axis. [label] is a bare channel
 * number for channel-based bands and a GHz value (e.g. "6.2") for
 * frequency-based bands; the axis title from [spectrumXAxisTitle] carries the
 * unit.
 */
internal data class SpectrumXTick(
    val frequencyMhz: Float,
    val label: String,
) {
    fun xFraction(axisStartMhz: Float, axisEndMhz: Float): Float {
        val span = axisEndMhz - axisStartMhz
        return if (span <= 0f) {
            0f
        } else {
            ((frequencyMhz - axisStartMhz) / span).coerceIn(0f, 1f)
        }
    }
}

/**
 * Computes meaningful per-group frequency ticks:
 * - 2.4 GHz: the canonical non-overlapping channels 1/6/11 plus channel 14.
 * - 5 GHz: recognizable channels within the selected validated channel group.
 * - 6 GHz: round 200 MHz steps labeled in GHz.
 * - Unknown band: round 1 GHz steps labeled in GHz.
 *
 * Ticks outside [axisStartMhz, axisEndMhz] are dropped so the result always
 * matches the displayed window from [spectrumAxisRangeMhz].
 */
internal fun spectrumXTicks(
    group: WifiChannelGroup,
    axisStartMhz: Float,
    axisEndMhz: Float,
): List<SpectrumXTick> {
    val ticks: List<Pair<Int, String>> = when (group) {
        WifiChannelGroup.GHZ_2_4 -> listOf(
            2_412 to "1",
            2_437 to "6",
            2_462 to "11",
            2_484 to "14",
        )
        WifiChannelGroup.GHZ_5_2 -> listOf(
            5_170 to "34",
            5_180 to "36",
            5_220 to "44",
            5_260 to "52",
            5_320 to "64",
        )
        WifiChannelGroup.GHZ_5_5_DFS -> listOf(
            5_500 to "100",
            5_560 to "112",
            5_640 to "128",
            5_700 to "140",
            5_720 to "144",
        )
        WifiChannelGroup.GHZ_5_8 -> listOf(
            5_745 to "149",
            5_805 to "161",
            5_825 to "165",
            5_865 to "173",
            5_885 to "177",
        )
        WifiChannelGroup.GHZ_6 -> (6_000..7_000 step 200).map { mhz -> mhz to ghzLabel(mhz) }
        WifiChannelGroup.UNKNOWN -> unknownFrequencyTicks(axisStartMhz, axisEndMhz)
    }
    return ticks
        .filter { it.first.toFloat() >= axisStartMhz && it.first.toFloat() <= axisEndMhz }
        .map { SpectrumXTick(it.first.toFloat(), it.second) }
}

private fun unknownFrequencyTicks(axisStartMhz: Float, axisEndMhz: Float): List<Pair<Int, String>> {
    val candidates = listOf(5, 10, 20, 50, 100, 200, 500, 1_000, 2_000, 5_000, 10_000, 20_000)
    val step = candidates.firstOrNull { candidate ->
        val first = ceil(axisStartMhz / candidate).toInt() * candidate
        val last = floor(axisEndMhz / candidate).toInt() * candidate
        last >= first && (last - first) / candidate + 1 <= 6
    } ?: 20_000
    val first = ceil(axisStartMhz / step).toInt() * step
    val last = floor(axisEndMhz / step).toInt() * step
    if (last < first) return emptyList()
    return (first..last step step).map { mhz -> mhz to preciseGhzLabel(mhz) }
}

/** Axis title carrying the unit for the spectrum frequency axis. */
internal fun spectrumXAxisTitle(group: WifiChannelGroup): String = when (group) {
    WifiChannelGroup.GHZ_2_4,
    WifiChannelGroup.GHZ_5_2,
    WifiChannelGroup.GHZ_5_5_DFS,
    WifiChannelGroup.GHZ_5_8 -> "Channel"
    WifiChannelGroup.GHZ_6,
    WifiChannelGroup.UNKNOWN -> "Frequency (GHz)"
}

/** Locale-stable "6.2"-style label (String.format would use the device locale). */
private fun ghzLabel(mhz: Int): String {
    val tenths = mhz / 100
    return "${tenths / 10}.${tenths % 10}"
}

private fun preciseGhzLabel(mhz: Int): String {
    val whole = mhz / 1_000
    val remainder = mhz % 1_000
    return when {
        remainder % 100 == 0 -> "$whole.${remainder / 100}"
        remainder % 10 == 0 -> "$whole.${(remainder / 10).toString().padStart(2, '0')}"
        else -> "$whole.${remainder.toString().padStart(3, '0')}"
    }
}
