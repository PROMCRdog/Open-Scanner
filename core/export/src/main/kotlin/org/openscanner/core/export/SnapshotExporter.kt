package org.openscanner.core.export

import org.openscanner.core.domain.WifiChannelMapper
import org.openscanner.core.model.ScanSnapshot
import org.openscanner.core.privacy.PrivacyRedactor

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    JSON("JSON", "json", "application/json"),
    CSV("CSV", "csv", "text/csv"),
}

object SnapshotExporter {
    fun exportRedacted(snapshot: ScanSnapshot, format: ExportFormat): String = when (format) {
        ExportFormat.JSON -> toJson(snapshot)
        ExportFormat.CSV -> toCsv(snapshot)
    }

    fun exportDocument(snapshot: ScanSnapshot, format: ExportFormat): ExportDocument = ExportDocument(
        title = "Redacted snapshot · ${format.label}",
        fileName = "open-scanner-snapshot-${coarsenTimestamp(snapshot.capturedAtEpochMs)}.${format.extension}",
        mimeType = format.mimeType,
        shareSubject = "Open Scanner redacted snapshot",
        content = exportRedacted(snapshot, format),
    )

    private fun toJson(snapshot: ScanSnapshot): String {
        val networks = snapshot.observations
            .sortedBy { it.id }
            .mapIndexed { index, observation -> PrivacyRedactor.redact(observation, index + 1) }
        return buildString {
            append("{\n")
            append("  \"schema\": \"open-scanner.snapshot.v1\",\n")
            append("  \"sequence\": ${snapshot.sequenceId},\n")
            append("  \"captured_at_epoch_ms\": ${coarsenTimestamp(snapshot.capturedAtEpochMs)},\n")
            append("  \"redacted\": true,\n")
            append("  \"limitations\": \"Passive Android scan; results may be cached or throttled\",\n")
            append("  \"networks\": [\n")
            networks.forEachIndexed { index, network ->
                append("    {")
                append("\"name\": \"${escapeJson(network.ssid)}\", ")
                append("\"bssid\": \"${escapeJson(network.bssid)}\", ")
                append("\"band\": \"${escapeJson(network.channel.band.label)}\", ")
                append("\"channel_group\": \"${escapeJson(WifiChannelMapper.group(network.channel).label)}\", ")
                append("\"channel\": ${network.channel.number ?: "null"}, ")
                append("\"frequency_mhz\": ${network.channel.centerFrequencyMhz}, ")
                append("\"footprint_center_frequency_mhz\": ${network.footprintCenterFrequencyMhz ?: "null"}, ")
                append("\"width_mhz\": ${network.channelWidthMhz ?: "null"}, ")
                append("\"rssi_dbm\": ${network.rssiDbm}, ")
                append("\"security\": \"${escapeJson(network.security.joinToString(" + ") { it.label })}\"")
                append("}")
                if (index != networks.lastIndex) append(',')
                append('\n')
            }
            append("  ]\n")
            append('}')
        }
    }

    private fun toCsv(snapshot: ScanSnapshot): String = buildString {
        append("schema,sequence,captured_at_epoch_ms,name,bssid,band,channel_group,channel,frequency_mhz,footprint_center_frequency_mhz,width_mhz,rssi_dbm,security\n")
        snapshot.observations
            .sortedBy { it.id }
            .mapIndexed { index, observation -> PrivacyRedactor.redact(observation, index + 1) }
            .forEach { network ->
                val fields = listOf(
                    "open-scanner.snapshot.v1",
                    snapshot.sequenceId.toString(),
                    coarsenTimestamp(snapshot.capturedAtEpochMs).toString(),
                    network.ssid,
                    network.bssid,
                    network.channel.band.label,
                    WifiChannelMapper.group(network.channel).label,
                    network.channel.number?.toString().orEmpty(),
                    network.channel.centerFrequencyMhz.toString(),
                    network.footprintCenterFrequencyMhz?.toString().orEmpty(),
                    network.channelWidthMhz?.toString().orEmpty(),
                    network.rssiDbm.toString(),
                    network.security.joinToString(" + ") { it.label },
                )
                append(fields.joinToString(",", transform = ::escapeCsv))
                append('\n')
            }
    }

    private fun coarsenTimestamp(epochMs: Long): Long = epochMs - epochMs % 60_000L

    private fun escapeJson(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
    }

    private fun escapeCsv(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
}
