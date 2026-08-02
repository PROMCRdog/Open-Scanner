package org.openscanner.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openscanner.app.NetworkUiModel
import org.openscanner.app.R
import org.openscanner.app.compactDurationLabel
import org.openscanner.app.normalizedSpectrumHalfSpan
import org.openscanner.app.signalHistoryTimeTicks
import org.openscanner.app.signalHistoryWindow
import org.openscanner.app.signalHistoryYAxis
import org.openscanner.app.spectrumAxisRangeMhz
import org.openscanner.app.spectrumFootprint
import org.openscanner.app.spectrumXTicks
import org.openscanner.app.spectrumYAxis
import org.openscanner.app.ui.displayLabel
import org.openscanner.app.ui.displayName
import org.openscanner.app.ui.spectrumAxisTitleLabel
import org.openscanner.app.ui.theme.ScannerBorder
import org.openscanner.app.ui.theme.ScannerCyan
import org.openscanner.app.ui.theme.ScannerGreen
import org.openscanner.app.ui.theme.ScannerMuted
import org.openscanner.app.ui.theme.ScannerOrange
import org.openscanner.app.ui.theme.ScannerPurple
import org.openscanner.app.ui.theme.ScannerSpacing
import org.openscanner.app.ui.theme.ScannerSurface
import org.openscanner.app.ui.theme.ScannerText
import org.openscanner.core.model.SignalSample
import org.openscanner.core.model.WifiChannelGroup

private const val StaleTailThresholdMs = 2_000L

// Canvas gutter layout shared by both charts: left gutter holds Y tick labels,
// bottom gutter holds X tick labels. Keep tick text at labelSmall (11sp) minimum.
private val PlotLeftGutter = 40.dp
private val PlotRightPad = 10.dp
private val PlotTopPad = 6.dp
private val PlotBottomPad = 20.dp

// Height of the plot area shared by both charts. Tall enough that gridlines,
// tick labels, and series shapes stay legible at a glance.
private val PlotHeight = 320.dp

@Composable
fun SignalHistoryChart(
    history: List<SignalSample>,
    latestDbm: Int,
    nowElapsedMs: Long,
    modifier: Modifier = Modifier,
) {
    val values = history.takeLast(60).sortedBy { it.elapsedRealtimeMs }
    val minimum = values.minOfOrNull { it.rssiDbm } ?: latestDbm
    val maximum = values.maxOfOrNull { it.rssiDbm } ?: latestDbm
    val window = signalHistoryWindow(values, nowElapsedMs)
    val yAxis = signalHistoryYAxis(values, latestDbm)
    val durationStartLabel = stringResource(R.string.geo_duration_start)
    val timeTicks = signalHistoryTimeTicks(
        window.spanMs,
        nowLabel = stringResource(R.string.geo_now),
    )
    val stale = values.isNotEmpty() && window.staleTailMs >= StaleTailThresholdMs
    val durationLabel = if (values.isEmpty()) {
        stringResource(R.string.chart_no_history)
    } else {
        compactDurationLabel(window.spanMs, startLabel = durationStartLabel)
    }
    val axisDescription = stringResource(
        R.string.chart_axis_description,
        yAxis.minDbm,
        yAxis.maxDbm,
        yAxis.stepDbm,
    )
    val staleTailLabel = compactDurationLabel(window.staleTailMs, startLabel = durationStartLabel)
    val description = when {
        values.isEmpty() -> stringResource(R.string.chart_history_description_empty, axisDescription)
        stale -> stringResource(
            R.string.chart_history_description_stale,
            durationLabel,
            staleTailLabel,
            latestDbm,
            minimum,
            maximum,
            values.size,
            axisDescription,
        )
        else -> stringResource(
            R.string.chart_history_description_current,
            durationLabel,
            latestDbm,
            minimum,
            maximum,
            values.size,
            axisDescription,
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ScannerBorder, MaterialTheme.shapes.small)
            .background(ScannerSurface)
            .semantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ScannerSpacing.Md, end = ScannerSpacing.Md, top = ScannerSpacing.Sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.chart_signal_dbm_axis),
                color = ScannerMuted,
                style = MaterialTheme.typography.labelMedium,
            )
            if (stale) {
                Text(
                    stringResource(R.string.chart_stale_last_sample, staleTailLabel),
                    color = ScannerOrange,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        val textMeasurer = rememberTextMeasurer()
        val tickStyle = MaterialTheme.typography.labelSmall
        val valueStyle = MaterialTheme.typography.labelMedium
        Box(Modifier.fillMaxWidth().height(PlotHeight)) {
            Canvas(Modifier.fillMaxSize()) {
                val plotLeft = PlotLeftGutter.toPx()
                val plotRight = size.width - PlotRightPad.toPx()
                val plotTop = PlotTopPad.toPx()
                val plotBottom = size.height - PlotBottomPad.toPx()
                val plotWidth = plotRight - plotLeft
                val plotHeight = plotBottom - plotTop
                if (plotWidth <= 0f || plotHeight <= 0f) return@Canvas

                fun yForDbm(dbm: Int): Float {
                    val clamped = dbm.coerceIn(yAxis.minDbm, yAxis.maxDbm)
                    val fraction = (clamped - yAxis.minDbm).toFloat() / (yAxis.maxDbm - yAxis.minDbm).toFloat()
                    return plotTop + plotHeight * (1f - fraction)
                }

                fun xForElapsed(elapsedMs: Long): Float = plotLeft + plotWidth * window.xFraction(elapsedMs)

                val gridColor = ScannerBorder.copy(alpha = 0.9f)
                yAxis.ticksDbm.forEach { dbm ->
                    val y = yForDbm(dbm)
                    drawLine(gridColor, Offset(plotLeft, y), Offset(plotRight, y), strokeWidth = 1.dp.toPx())
                    val label = textMeasurer.measure("$dbm", tickStyle)
                    drawText(
                        label,
                        color = ScannerMuted,
                        topLeft = Offset(plotLeft - 6.dp.toPx() - label.size.width, y - label.size.height / 2f),
                    )
                }
                timeTicks.forEach { tick ->
                    val fraction = if (window.spanMs <= 0L) {
                        1f
                    } else {
                        1f - (tick.offsetFromEndMs.toFloat() / window.spanMs.toFloat()).coerceIn(0f, 1f)
                    }
                    val x = plotLeft + plotWidth * fraction
                    drawLine(gridColor.copy(alpha = 0.55f), Offset(x, plotTop), Offset(x, plotBottom), strokeWidth = 1.dp.toPx())
                    val label = textMeasurer.measure(tick.label, tickStyle)
                    val tx = (x - label.size.width / 2f)
                        .coerceIn(0f, (size.width - label.size.width).coerceAtLeast(0f))
                    drawText(label, color = ScannerMuted, topLeft = Offset(tx, plotBottom + 5.dp.toPx()))
                }

                if (values.isNotEmpty()) {
                    val lastX = xForElapsed(values.last().elapsedRealtimeMs)
                    if (stale && lastX < plotRight) {
                        drawRect(
                            color = ScannerOrange.copy(alpha = 0.08f),
                            topLeft = Offset(lastX, plotTop),
                            size = Size(plotRight - lastX, plotHeight),
                        )
                        drawLine(
                            color = ScannerOrange.copy(alpha = 0.7f),
                            start = Offset(lastX, plotTop),
                            end = Offset(lastX, plotBottom),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    val line = Path()
                    values.forEachIndexed { index, sample ->
                        val x = xForElapsed(sample.elapsedRealtimeMs)
                        val y = yForDbm(sample.rssiDbm)
                        if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
                    }
                    val fill = Path().apply {
                        addPath(line)
                        lineTo(lastX, plotBottom)
                        lineTo(xForElapsed(values.first().elapsedRealtimeMs), plotBottom)
                        close()
                    }
                    drawPath(fill, ScannerCyan.copy(alpha = 0.22f))
                    drawPath(line, ScannerCyan, style = Stroke(width = 2.5.dp.toPx()))
                    val lastY = yForDbm(values.last().rssiDbm)
                    drawCircle(ScannerCyan, radius = 4.dp.toPx(), center = Offset(lastX, lastY))
                    val valueLabel = textMeasurer.measure("$latestDbm dBm", valueStyle)
                    val valueX = (lastX - valueLabel.size.width / 2f)
                        .coerceIn(plotLeft, (plotRight - valueLabel.size.width).coerceAtLeast(plotLeft))
                    val valueY = (lastY - 10.dp.toPx() - valueLabel.size.height).coerceAtLeast(plotTop)
                    drawText(valueLabel, color = ScannerCyan, topLeft = Offset(valueX, valueY))
                }
            }
            if (values.isEmpty()) {
                Text(
                    stringResource(R.string.chart_no_retained_history),
                    color = ScannerMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
        Text(
            stringResource(R.string.chart_time_axis),
            color = ScannerMuted,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScannerSpacing.Md, vertical = ScannerSpacing.Sm),
            horizontalArrangement = Arrangement.spacedBy(ScannerSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChartLegendEntry(
                swatch = LegendSwatch.Line,
                color = ScannerCyan,
                label = stringResource(R.string.chart_legend_signal),
                emphasized = true,
            )
            ChartLegendEntry(
                swatch = LegendSwatch.Dot,
                color = ScannerCyan,
                label = stringResource(R.string.chart_legend_latest_value),
            )
            if (stale) {
                ChartLegendEntry(
                    swatch = LegendSwatch.StaleRegion,
                    color = ScannerOrange,
                    label = stringResource(R.string.chart_legend_stale_gap),
                )
            }
        }
    }
}

@Composable
fun SpectrumChart(
    channelGroup: WifiChannelGroup,
    networks: List<NetworkUiModel>,
    modifier: Modifier = Modifier,
) {
    val selected = networks.firstOrNull { it.selected }
    val plotted = buildList {
        selected?.let(::add)
        addAll(networks.filterNot { it.uiId == selected?.uiId }.sortedByDescending { it.signalDbm })
    }.distinctBy { it.uiId }
    val connected = plotted.firstOrNull { it.connected }
    val observedFootprintEdges = networks.flatMap { network ->
        val footprint = network.spectrumFootprint()
        val halfWidth = footprint.widthMhz?.div(2f)
        if (halfWidth == null) {
            listOf(footprint.centerFrequencyMhz.toFloat())
        } else {
            listOf(footprint.centerFrequencyMhz - halfWidth, footprint.centerFrequencyMhz + halfWidth)
        }
    }
    val (axisStart, axisEnd) = spectrumAxisRangeMhz(channelGroup, observedFootprintEdges)
    val yAxis = spectrumYAxis()
    val xTicks = spectrumXTicks(channelGroup, axisStart, axisEnd)
    val widthUnknownLabel = stringResource(R.string.chart_width_unknown)
    val channelUnknownLabel = stringResource(R.string.chart_channel_unknown)
    val displayNames = mutableMapOf<String, String>()
    for (network in plotted) displayNames[network.uiId] = network.displayName()
    val summarizedNetworks = plotted.take(12)
    val summary = summarizedNetworks.map { network ->
        val width = network.channelWidthMhz?.takeIf { it > 0 }
            ?.let { stringResource(R.string.chart_megahertz_wide, it) } ?: widthUnknownLabel
        stringResource(
            R.string.chart_network_summary,
            displayNames.getValue(network.uiId),
            network.channel?.toString() ?: channelUnknownLabel,
            network.signalDbm,
            width,
        )
    }.joinToString().let { listed ->
        val remaining = plotted.size - summarizedNetworks.size
        if (remaining > 0) {
            "$listed ${stringResource(R.string.chart_network_summary_more, remaining)}"
        } else {
            listed
        }
    }
    val overlapDescription = stringResource(
        R.string.chart_overlap_description,
        channelGroup.displayLabel(),
        yAxis.minDbm,
        yAxis.maxDbm,
        plotted.size,
        summary,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ScannerBorder, MaterialTheme.shapes.small)
            .background(ScannerSurface)
            .semantics {
                contentDescription = overlapDescription
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = ScannerSpacing.Md),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.chart_selected_label),
                    color = ScannerMuted,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    selected?.let { displayNames.getValue(it.uiId) } ?: stringResource(R.string.chart_no_selection),
                    color = ScannerText,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selected?.connected == true) {
                    CurrentWifiBadge(modifier = Modifier.padding(top = ScannerSpacing.Xs))
                }
                selected?.let { network ->
                    val footprint = network.spectrumFootprint()
                    val detail = if (
                        footprint.widthMhz != null &&
                        footprint.centerFrequencyMhz != network.frequencyMhz
                    ) {
                        stringResource(
                            R.string.chart_selected_primary_and_center,
                            network.channel?.toString() ?: channelUnknownLabel,
                            footprint.widthMhz,
                            footprint.centerFrequencyMhz,
                        )
                    } else {
                        stringResource(
                            R.string.chart_selected_channel_and_width,
                            network.channel?.toString() ?: channelUnknownLabel,
                            footprint.widthMhz
                                ?.let { stringResource(R.string.chart_megahertz_wide, it) }
                                ?: widthUnknownLabel,
                        )
                    }
                    Text(
                        detail,
                        color = ScannerMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(R.string.chart_signal_label),
                    color = ScannerMuted,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    selected?.let { "${it.signalDbm} dBm" } ?: "—",
                    color = ScannerText,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Text(
            stringResource(R.string.chart_signal_dbm_axis),
            color = ScannerMuted,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 14.dp),
        )
        val textMeasurer = rememberTextMeasurer()
        val tickStyle = MaterialTheme.typography.labelSmall
        Box(Modifier.fillMaxWidth().height(PlotHeight)) {
            Canvas(Modifier.fillMaxSize()) {
                val plotLeft = PlotLeftGutter.toPx()
                val plotRight = size.width - PlotRightPad.toPx()
                val plotTop = PlotTopPad.toPx()
                val plotBottom = size.height - PlotBottomPad.toPx()
                val plotWidth = plotRight - plotLeft
                val plotHeight = plotBottom - plotTop
                if (plotWidth <= 0f || plotHeight <= 0f) return@Canvas

                fun yForDbm(dbm: Int): Float {
                    val clamped = dbm.coerceIn(yAxis.minDbm, yAxis.maxDbm)
                    val fraction = (clamped - yAxis.minDbm).toFloat() / (yAxis.maxDbm - yAxis.minDbm).toFloat()
                    return plotTop + plotHeight * (1f - fraction)
                }

                val gridColor = ScannerBorder.copy(alpha = 0.9f)
                yAxis.ticksDbm.forEach { dbm ->
                    val y = yForDbm(dbm)
                    drawLine(gridColor, Offset(plotLeft, y), Offset(plotRight, y), strokeWidth = 1.dp.toPx())
                    val label = textMeasurer.measure("$dbm", tickStyle)
                    drawText(
                        label,
                        color = ScannerMuted,
                        topLeft = Offset(plotLeft - 6.dp.toPx() - label.size.width, y - label.size.height / 2f),
                    )
                }
                xTicks.forEach { tick ->
                    val x = plotLeft + plotWidth * tick.xFraction(axisStart, axisEnd)
                    drawLine(gridColor.copy(alpha = 0.55f), Offset(x, plotTop), Offset(x, plotBottom), strokeWidth = 1.dp.toPx())
                    val label = textMeasurer.measure(tick.label, tickStyle)
                    val tx = (x - label.size.width / 2f)
                        .coerceIn(0f, (size.width - label.size.width).coerceAtLeast(0f))
                    drawText(label, color = ScannerMuted, topLeft = Offset(tx, plotBottom + 5.dp.toPx()))
                }

                plotted.forEach { network ->
                    val color = when {
                        network.selected -> ScannerCyan
                        network.connected -> ScannerGreen
                        else -> ScannerPurple
                    }
                    val footprint = network.spectrumFootprint()
                    val normalizedHalfSpan = normalizedSpectrumHalfSpan(footprint.widthMhz, axisStart, axisEnd)
                    val centerX = (plotLeft + plotWidth * ((footprint.centerFrequencyMhz - axisStart) / (axisEnd - axisStart)))
                        .coerceIn(plotLeft, plotRight)
                    val baseY = plotBottom
                    val peakY = yForDbm(network.signalDbm)
                    if (normalizedHalfSpan == null) {
                        // Unknown channel width: bare vertical line + peak dot, never an invented footprint.
                        drawLine(
                            color = color,
                            start = Offset(centerX, baseY),
                            end = Offset(centerX, peakY),
                            strokeWidth = if (network.selected) 2.8.dp.toPx() else 1.3.dp.toPx(),
                        )
                        drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(centerX, peakY))
                    } else {
                        val halfWidth = normalizedHalfSpan * plotWidth
                        val path = Path().apply {
                            moveTo((centerX - halfWidth).coerceAtLeast(plotLeft), baseY)
                            quadraticTo(centerX - halfWidth * 0.4f, peakY, centerX, peakY)
                            quadraticTo(centerX + halfWidth * 0.4f, peakY, (centerX + halfWidth).coerceAtMost(plotRight), baseY)
                            close()
                        }
                        drawPath(path, color.copy(alpha = if (network.selected) 0.20f else 0.065f))
                        drawPath(
                            path,
                            color,
                            style = Stroke(width = if (network.selected) 2.8.dp.toPx() else 1.3.dp.toPx()),
                        )
                        drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(centerX, peakY))
                    }
                    if (network.selected && network.frequencyMhz != footprint.centerFrequencyMhz) {
                        val primaryX = (
                            plotLeft + plotWidth * ((network.frequencyMhz - axisStart) / (axisEnd - axisStart))
                            ).coerceIn(plotLeft, plotRight)
                        drawLine(
                            color = ScannerCyan.copy(alpha = 0.85f),
                            start = Offset(primaryX, plotTop),
                            end = Offset(primaryX, plotBottom),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                        )
                    }
                    if (network.connected) {
                        drawCircle(
                            color = ScannerGreen,
                            radius = 4.5.dp.toPx(),
                            center = Offset(centerX, peakY),
                        )
                        if (network.selected) {
                            drawCircle(
                                color = ScannerCyan,
                                radius = 2.2.dp.toPx(),
                                center = Offset(centerX, peakY),
                            )
                        }
                    }
                }
            }
        }
        Text(
            channelGroup.spectrumAxisTitleLabel(),
            color = ScannerMuted,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        )
        if (plotted.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScannerSpacing.Md, vertical = ScannerSpacing.Sm),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                selected?.let { focused ->
                    ChartLegendEntry(
                        swatch = LegendSwatch.Line,
                        color = ScannerCyan,
                        label = stringResource(R.string.chart_legend_selected_network, displayNames.getValue(focused.uiId)),
                        emphasized = true,
                    )
                    if (focused.frequencyMhz != focused.spectrumFootprint().centerFrequencyMhz) {
                        ChartLegendEntry(
                            swatch = LegendSwatch.PrimaryMarker,
                            color = ScannerCyan,
                            label = stringResource(R.string.chart_legend_primary_channel),
                        )
                    }
                }
                connected?.let { current ->
                    ChartLegendEntry(
                        swatch = if (current.selected) LegendSwatch.Dot else LegendSwatch.Line,
                        color = ScannerGreen,
                        label = stringResource(
                            R.string.chart_legend_connected_network,
                            displayNames.getValue(current.uiId),
                        ),
                    )
                }
                val otherCount = plotted.count { !it.selected && !it.connected }
                if (otherCount > 0) {
                    ChartLegendEntry(
                        swatch = LegendSwatch.Line,
                        color = ScannerPurple,
                        label = stringResource(R.string.chart_legend_other_networks, otherCount),
                    )
                }
                val unknownWidthCount = plotted.count { it.channelWidthMhz?.takeIf { width -> width > 0 } == null }
                if (unknownWidthCount > 0) {
                    ChartLegendEntry(
                        swatch = LegendSwatch.UnknownWidth,
                        color = ScannerMuted,
                        label = stringResource(R.string.chart_legend_unknown_width_count, unknownWidthCount),
                    )
                }
            }
        }
    }
}

/** Non-color legend encodings; every swatch pairs a distinct shape with a text label. */
private enum class LegendSwatch {
    Line,
    Dot,
    StaleRegion,
    UnknownWidth,
    PrimaryMarker,
}

@Composable
private fun ChartLegendEntry(
    swatch: LegendSwatch,
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 22.dp, height = 14.dp)) {
            val strokeWidth = (if (emphasized) 2.8.dp else 1.7.dp).toPx()
            when (swatch) {
                LegendSwatch.Line -> drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = strokeWidth,
                )
                LegendSwatch.Dot -> drawCircle(
                    color = color,
                    radius = 4.dp.toPx(),
                    center = Offset(size.width / 2f, size.height / 2f),
                )
                LegendSwatch.StaleRegion -> {
                    val edge = size.width * 0.45f
                    drawRect(
                        color = color.copy(alpha = 0.25f),
                        topLeft = Offset(edge, 0f),
                        size = Size(size.width - edge, size.height),
                    )
                    drawLine(
                        color = color,
                        start = Offset(edge, 0f),
                        end = Offset(edge, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                LegendSwatch.UnknownWidth -> {
                    val x = size.width / 2f
                    drawLine(
                        color = color,
                        start = Offset(x, size.height),
                        end = Offset(x, 2.dp.toPx()),
                        strokeWidth = strokeWidth,
                    )
                    drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(x, 2.dp.toPx()))
                }
                LegendSwatch.PrimaryMarker -> {
                    val x = size.width / 2f
                    drawLine(
                        color = color,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                    )
                }
            }
        }
        Text(
            text = label,
            color = if (emphasized) ScannerText else ScannerMuted,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
