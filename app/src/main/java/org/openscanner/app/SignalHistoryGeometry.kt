package org.openscanner.app

import org.openscanner.core.model.SignalSample

internal data class SignalHistoryWindow(
    val startElapsedMs: Long,
    val endElapsedMs: Long,
    val lastSampleElapsedMs: Long?,
) {
    val spanMs: Long = (endElapsedMs - startElapsedMs).coerceAtLeast(0L)
    val staleTailMs: Long = lastSampleElapsedMs
        ?.let { (endElapsedMs - it).coerceAtLeast(0L) }
        ?: 0L

    fun xFraction(elapsedMs: Long): Float = if (spanMs == 0L) {
        1f
    } else {
        ((elapsedMs - startElapsedMs).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
    }
}

internal fun signalHistoryWindow(
    samples: List<SignalSample>,
    nowElapsedMs: Long,
): SignalHistoryWindow {
    val firstSample = samples.minOfOrNull { it.elapsedRealtimeMs }
    val lastSample = samples.maxOfOrNull { it.elapsedRealtimeMs }
    val end = maxOf(nowElapsedMs, lastSample ?: nowElapsedMs)
    return SignalHistoryWindow(
        startElapsedMs = firstSample?.coerceAtMost(end) ?: end,
        endElapsedMs = end,
        lastSampleElapsedMs = lastSample,
    )
}

/**
 * Vertical axis for the signal-history chart. [ticksDbm] is the exact set of
 * values the chart draws gridlines and labels for, always ascending, always
 * aligned to multiples of [stepDbm], and always inside [floorDbm, ceilingDbm]
 * of [signalHistoryYAxis].
 */
internal data class SignalHistoryYAxis(
    val minDbm: Int,
    val maxDbm: Int,
    val stepDbm: Int,
) {
    val ticksDbm: List<Int> = (minDbm..maxDbm step stepDbm).toList()
}

/**
 * Computes a human-readable dBm axis from the displayed data range.
 *
 * - Values are clamped to the honest Wi-Fi RSSI window [floorDbm, ceilingDbm]
 *   (defaults -100..-20 dBm) before the range is measured.
 * - The axis snaps outward to multiples of the step and never spans less than
 *   two steps, so a single sample still gets a usable axis.
 * - Step is 10 dB for narrow ranges and 20 dB for wide ones, doubled again if
 *   that would produce more than six labels.
 */
internal fun signalHistoryYAxis(
    samples: List<SignalSample>,
    latestDbm: Int,
    floorDbm: Int = -100,
    ceilingDbm: Int = -20,
): SignalHistoryYAxis {
    val values = samples.map { it.rssiDbm.coerceIn(floorDbm, ceilingDbm) } +
        latestDbm.coerceIn(floorDbm, ceilingDbm)
    val dataMin = values.min()
    val dataMax = values.max()
    var step = if (dataMax - dataMin <= 50) 10 else 20
    while (true) {
        var min = Math.floorDiv(dataMin, step) * step
        var max = -Math.floorDiv(-dataMax, step) * step
        while (max - min < 2 * step) {
            if (min - step >= floorDbm) {
                min -= step
            } else if (max + step <= ceilingDbm) {
                max += step
            } else {
                break
            }
        }
        if ((max - min) / step + 1 <= 6 || step >= 40) {
            return SignalHistoryYAxis(minDbm = min, maxDbm = max, stepDbm = step)
        }
        step *= 2
    }
}

/**
 * One labeled tick on the signal-history time axis. [offsetFromEndMs] is the
 * distance back from "now" (the window end); the chart converts it to an
 * x-position with `1 - offset / span`.
 */
internal data class SignalHistoryTimeTick(
    val offsetFromEndMs: Long,
    val label: String,
)

/**
 * Computes time-axis ticks at sensible round steps (15s/30s/1m/2m/5m/10m/15m/
 * 30m/1h, then whole hours) so the axis always carries at most [maxTicks]
 * labels. The first tick is always "Now" (offset 0) and the last tick always
 * lands exactly on the window start so the full span stays labeled.
 *
 * Labels are drawn centered on their ticks, so adjacent ticks must keep some
 * clearance: when the window start would land closer than half the step to the
 * last computed tick, that tick is dropped in favor of the window start. Every
 * remaining gap is then either exactly the step or, for the final window-start
 * gap, at least half the step.
 */
internal fun signalHistoryTimeTicks(
    spanMs: Long,
    maxTicks: Int = 5,
): List<SignalHistoryTimeTick> {
    if (spanMs <= 0L || maxTicks < 2) {
        return listOf(SignalHistoryTimeTick(offsetFromEndMs = 0L, label = "Now"))
    }
    val candidates = listOf(
        15_000L, 30_000L, 60_000L, 120_000L, 300_000L,
        600_000L, 900_000L, 1_800_000L, 3_600_000L,
    )
    val step = candidates.firstOrNull { spanMs.toDouble() / it <= (maxTicks - 1).toDouble() }
        ?: ((spanMs / (maxTicks - 1L)) / 3_600_000L + 1L) * 3_600_000L
    val offsets = generateSequence(0L) { it + step }
        .takeWhile { it <= spanMs }
        .toMutableList()
    if (offsets.last() != spanMs) {
        // The window start is always labeled; drop a computed tick that would
        // crowd it (less than half a step away) so labels can never collide.
        if (offsets.size > 1 && spanMs - offsets.last() < step / 2) {
            offsets.removeAt(offsets.lastIndex)
        }
        offsets += spanMs
    }
    return offsets.map { offset ->
        SignalHistoryTimeTick(
            offsetFromEndMs = offset,
            label = if (offset == 0L) "Now" else "-${compactDurationLabel(offset)}",
        )
    }
}

/** Compact duration label shared by axis ticks and stale-state readouts. */
internal fun compactDurationLabel(durationMs: Long): String = when {
    durationMs <= 0L -> "Start"
    durationMs < 60_000L -> "${(durationMs / 1_000L).coerceAtLeast(1L)}s"
    durationMs % 3_600_000L == 0L -> "${durationMs / 3_600_000L}h"
    durationMs % 60_000L == 0L -> "${durationMs / 60_000L}m"
    else -> "${durationMs / 60_000L}m ${(durationMs % 60_000L) / 1_000L}s"
}
