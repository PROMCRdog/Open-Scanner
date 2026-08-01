package org.openscanner.core.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.openscanner.core.model.AccessPointObservation

enum class ObservedCongestion(val label: String) {
    LOW("Low"),
    MODERATE("Moderate"),
    HIGH("High"),
}

data class InterferenceSummary(
    val overlappingNetworks: Int,
    val coChannelNetworks: Int,
    val strongestInterfererDbm: Int?,
    val weightedOverlap: Double,
    val congestion: ObservedCongestion,
)

object InterferenceAnalyzer {
    fun summarize(
        selected: AccessPointObservation,
        observations: List<AccessPointObservation>,
    ): InterferenceSummary {
        val selectedWidth = selected.channelWidthMhz
        val selectedCenter = selected.footprintCenterFrequencyMhz ?: selected.channel.centerFrequencyMhz
        val selectedStart = selectedWidth?.let { selectedCenter - it / 2.0 }
        val selectedEnd = selectedWidth?.let { selectedCenter + it / 2.0 }

        val overlaps = observations.asSequence()
            .filter { it.id != selected.id && it.channel.band == selected.channel.band }
            .mapNotNull { candidate ->
                if (selectedWidth == null || candidate.channelWidthMhz == null) {
                    return@mapNotNull if (
                        candidate.channel.centerFrequencyMhz == selected.channel.centerFrequencyMhz
                    ) {
                        candidate to 1.0
                    } else {
                        null
                    }
                }
                val candidateWidth = candidate.channelWidthMhz ?: return@mapNotNull null
                val candidateCenter = candidate.footprintCenterFrequencyMhz ?: candidate.channel.centerFrequencyMhz
                val candidateStart = candidateCenter - candidateWidth / 2.0
                val candidateEnd = candidateCenter + candidateWidth / 2.0
                val overlapMhz = max(0.0, min(selectedEnd!!, candidateEnd) - max(selectedStart!!, candidateStart))
                if (overlapMhz <= 0.0) null else candidate to overlapMhz / selectedWidth
            }
            .toList()

        val weighted = overlaps.sumOf { (candidate, fraction) ->
            10.0.pow(candidate.rssiDbm.coerceIn(-100, -20) / 10.0) * fraction
        }
        val strongest = overlaps.maxOfOrNull { it.first.rssiDbm }
        val congestion = when {
            overlaps.isEmpty() -> ObservedCongestion.LOW
            strongest != null && strongest >= -62 -> ObservedCongestion.HIGH
            overlaps.size >= 4 || (strongest != null && strongest >= -72) -> ObservedCongestion.MODERATE
            else -> ObservedCongestion.LOW
        }

        return InterferenceSummary(
            overlappingNetworks = overlaps.size,
            coChannelNetworks = overlaps.count {
                it.first.channel.centerFrequencyMhz == selected.channel.centerFrequencyMhz
            },
            strongestInterfererDbm = strongest,
            weightedOverlap = weighted,
            congestion = congestion,
        )
    }
}
