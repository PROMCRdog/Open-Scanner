package org.openscanner.core.domain

enum class Freshness(val label: String) {
    FRESH("Live"),
    AGING("Aging"),
    STALE("Stale"),
}

object FreshnessPolicy {
    fun classify(ageMs: Long, observedCadenceMs: Long? = null): Freshness {
        val freshThreshold = maxOf(90_000L, (observedCadenceMs ?: 0L) * 2L)
        val staleThreshold = maxOf(300_000L, (observedCadenceMs ?: 0L) * 4L)
        return when {
            ageMs <= freshThreshold -> Freshness.FRESH
            ageMs > staleThreshold -> Freshness.STALE
            else -> Freshness.AGING
        }
    }
}
