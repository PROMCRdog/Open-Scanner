package org.openscanner.core.domain

import org.openscanner.core.model.AccessPointObservation
import org.openscanner.core.model.WifiChannelGroup
import org.openscanner.core.model.WifiGeneration

data class NeighborhoodPosture(
    val accessPointCount: Int,
    val channelGroups: Map<WifiChannelGroup, Int>,
    val securityProfiles: Map<String, Int>,
    val generations: Map<WifiGeneration, Int>,
)

/** Counts observed facts only; it does not infer safety, identity, or airtime. */
object NeighborhoodPostureAnalyzer {
    fun summarize(observations: List<AccessPointObservation>): NeighborhoodPosture = NeighborhoodPosture(
        accessPointCount = observations.size,
        channelGroups = observations
            .groupingBy { WifiChannelMapper.group(it.channel) }
            .eachCount(),
        securityProfiles = observations
            .groupingBy { observation ->
                observation.security
                    .sortedBy { it.name }
                    .joinToString(" + ") { it.label }
                    .ifBlank { "Unknown" }
            }
            .eachCount(),
        generations = observations
            .groupingBy { it.generation }
            .eachCount(),
    )
}
