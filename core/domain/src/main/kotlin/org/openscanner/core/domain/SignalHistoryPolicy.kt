package org.openscanner.core.domain

import org.openscanner.core.model.SignalSample

object SignalHistoryPolicy {
    fun update(
        existing: Map<String, List<SignalSample>>,
        incoming: Map<String, SignalSample>,
        nowElapsedMs: Long,
        retentionMs: Long = 30 * 60_000L,
        maxPointsPerNetwork: Int = 60,
        maxNetworks: Int = 128,
    ): Map<String, List<SignalSample>> {
        val cutoff = nowElapsedMs - retentionMs.coerceAtLeast(0L)
        val pointLimit = maxPointsPerNetwork.coerceAtLeast(0)
        val networkLimit = maxNetworks.coerceAtLeast(0)
        val updated = existing
            .mapValues { (_, points) -> points.filter { it.elapsedRealtimeMs >= cutoff } }
            .filterValues { it.isNotEmpty() }
            .toMutableMap()

        incoming.forEach { (id, sample) ->
            updated[id] = (updated[id].orEmpty()
                .filterNot { it.elapsedRealtimeMs == sample.elapsedRealtimeMs } + sample)
                .filter { it.elapsedRealtimeMs >= cutoff }
                .sortedBy { it.elapsedRealtimeMs }
                .takeLast(pointLimit)
        }

        return updated.entries
            .filter { (_, points) -> points.isNotEmpty() }
            .sortedByDescending { (_, points) -> points.last().elapsedRealtimeMs }
            .take(networkLimit)
            .associate { it.toPair() }
    }
}
