package org.openscanner.core.domain

enum class AccessPointStabilityLevel(val label: String) {
    STEADY("Steady"),
    VARIABLE("Variable"),
    FLAPPING("Flapping"),
    INSUFFICIENT("Insufficient history"),
}

data class AccessPointStability(
    val level: AccessPointStabilityLevel,
    val assessedSnapshots: Int,
    val presentSnapshots: Int,
    val absentShare: Double?,
    val rssiRangeDb: Int?,
)

/**
 * Summarizes recent per-snapshot evidence. Leading absences are ignored so a
 * newly discovered AP is not treated as missing before it was first seen.
 */
object AccessPointStabilityAnalyzer {
    fun summarize(recentRssiDbm: List<Int?>): AccessPointStability {
        val assessed = recentRssiDbm.dropWhile { it == null }
        val present = assessed.filterNotNull()
        val range = present.takeIf { it.isNotEmpty() }?.let { values ->
            values.max() - values.min()
        }
        val absentShare = assessed.takeIf { it.isNotEmpty() }
            ?.let { values -> values.count { it == null }.toDouble() / values.size }
        val level = when {
            assessed.size < MIN_ASSESSED_SNAPSHOTS || present.size < MIN_PRESENT_SNAPSHOTS ->
                AccessPointStabilityLevel.INSUFFICIENT
            absentShare != null && absentShare >= FLAPPING_ABSENCE_SHARE ->
                AccessPointStabilityLevel.FLAPPING
            absentShare != null && absentShare > STEADY_ABSENCE_SHARE ->
                AccessPointStabilityLevel.VARIABLE
            range != null && range > STEADY_RSSI_RANGE_DB ->
                AccessPointStabilityLevel.VARIABLE
            else -> AccessPointStabilityLevel.STEADY
        }
        return AccessPointStability(
            level = level,
            assessedSnapshots = assessed.size,
            presentSnapshots = present.size,
            absentShare = absentShare,
            rssiRangeDb = range,
        )
    }

    private const val MIN_ASSESSED_SNAPSHOTS = 4
    private const val MIN_PRESENT_SNAPSHOTS = 3
    private const val STEADY_ABSENCE_SHARE = 0.10
    private const val FLAPPING_ABSENCE_SHARE = 0.25
    private const val STEADY_RSSI_RANGE_DB = 10
}
