package org.openscanner.core.export

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class WifiLogFormat(
    val label: String,
    val extension: String,
    val mimeType: String,
) {
    TEXT("Text report", "txt", "text/plain"),
    JSON("JSON", "json", "application/json"),
    CSV("CSV", "csv", "text/csv"),
}

object WifiLogExporter {
    private val fileTimestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").withZone(ZoneOffset.UTC)

    fun export(session: WifiLogSession, format: WifiLogFormat): ExportDocument {
        val content = when (format) {
            WifiLogFormat.TEXT -> toText(session)
            WifiLogFormat.JSON -> toJson(session)
            WifiLogFormat.CSV -> toCsv(session)
        }
        return ExportDocument(
            title = "Redacted Wi-Fi log · ${format.label}",
            fileName = "open-scanner-wifi-log-${fileTimestamp.format(Instant.ofEpochMilli(session.startedAtEpochMinuteMs))}.${format.extension}",
            mimeType = format.mimeType,
            shareSubject = "Open Scanner redacted Wi-Fi log",
            content = content,
        )
    }

    private fun toText(session: WifiLogSession): String = buildString {
        appendLine("OPEN SCANNER WI-FI LOG")
        appendLine("========================")
        appendLine("Schema: open-scanner.wifi-log.v1")
        appendLine("Started (UTC, minute precision): ${Instant.ofEpochMilli(session.startedAtEpochMinuteMs)}")
        appendLine("Duration: ${formatDuration(session.durationMs)}")
        appendLine("Records: ${session.records.size}")
        appendLine("Access-point rows: ${session.networkRowCount}")
        appendLine("Status: ${session.stopReason?.label ?: "Recording snapshot"}")
        appendLine("Privacy: redacted before capture; raw SSIDs, BSSIDs, and local addresses are absent")
        appendLine("Limits: passive Android evidence may be cached or throttled; RSSI is not distance or throughput")
        appendLine()
        WifiLogFieldCategory.entries.forEach { category ->
            val fields = orderedFields(session, category)
            if (fields.isNotEmpty()) {
                appendLine("${category.label}: ${fields.joinToString { it.label }}")
            }
        }
        session.records.forEach { record ->
            appendLine()
            appendLine("RECORD ${record.index}  +${formatDuration(record.elapsedMs)}")
            appendLine("-".repeat(28))
            appendValues("Scan", record.scanValues)
            appendValues("Connection", record.connectionValues)
            if (record.networkValues.isNotEmpty()) {
                appendLine("Access points (${record.networkValues.size}):")
                record.networkValues.forEachIndexed { index, values ->
                    appendLine("  #${index + 1}")
                    values.forEach { (field, value) ->
                        appendLine("    ${field.label}: ${displayValue(field, value)}")
                    }
                }
            }
        }
    }

    private fun StringBuilder.appendValues(
        title: String,
        values: Map<WifiLogField, String?>,
    ) {
        if (values.isEmpty()) return
        appendLine("$title:")
        values.forEach { (field, value) ->
            appendLine("  ${field.label}: ${displayValue(field, value)}")
        }
    }

    private fun toJson(session: WifiLogSession): String = buildString {
        appendLine("{")
        appendLine("  \"schema\": \"open-scanner.wifi-log.v1\",")
        appendLine("  \"redacted\": true,")
        appendLine("  \"started_at_utc_minute\": \"${Instant.ofEpochMilli(session.startedAtEpochMinuteMs)}\",")
        appendLine("  \"duration_ms\": ${session.durationMs},")
        appendLine("  \"stop_reason\": ${jsonStringOrNull(session.stopReason?.name)},")
        appendLine("  \"limitations\": \"Passive Android evidence may be cached or throttled; RSSI is not distance or throughput\",")
        appendLine("  \"selected_fields\": [${orderedFields(session).joinToString { "\"${it.key}\"" }}],")
        appendLine("  \"records\": [")
        session.records.forEachIndexed { recordIndex, record ->
            appendLine("    {")
            appendLine("      \"index\": ${record.index},")
            appendLine("      \"elapsed_ms\": ${record.elapsedMs},")
            append("      \"scan\": ")
            appendJsonValues(record.scanValues, "      ")
            appendLine(",")
            append("      \"connection\": ")
            appendJsonValues(record.connectionValues, "      ")
            appendLine(",")
            appendLine("      \"networks\": [")
            record.networkValues.forEachIndexed { networkIndex, values ->
                append("        ")
                appendJsonValues(values, "        ")
                if (networkIndex != record.networkValues.lastIndex) append(',')
                appendLine()
            }
            appendLine("      ]")
            append("    }")
            if (recordIndex != session.records.lastIndex) append(',')
            appendLine()
        }
        appendLine("  ]")
        append('}')
    }

    private fun StringBuilder.appendJsonValues(
        values: Map<WifiLogField, String?>,
        indent: String,
    ) {
        if (values.isEmpty()) {
            append("{}")
            return
        }
        appendLine("{")
        values.entries.forEachIndexed { index, (field, value) ->
            append(indent)
            append("  \"")
            append(field.key)
            append("\": ")
            append(jsonValue(field, value))
            if (index != values.size - 1) append(',')
            appendLine()
        }
        append(indent)
        append('}')
    }

    private fun toCsv(session: WifiLogSession): String = buildString {
        val fields = orderedFields(session)
        val headers = listOf("schema", "record_index", "elapsed_ms", "record_type", "network_index") +
            fields.map { it.key }
        appendLine(headers.joinToString(",", transform = ::escapeCsv))
        session.records.forEach { record ->
            if (record.scanValues.isNotEmpty()) appendCsvRow(record, "scan", null, record.scanValues, fields)
            if (record.connectionValues.isNotEmpty()) {
                appendCsvRow(record, "connection", null, record.connectionValues, fields)
            }
            record.networkValues.forEachIndexed { index, values ->
                appendCsvRow(record, "network", index + 1, values, fields)
            }
        }
    }

    private fun StringBuilder.appendCsvRow(
        record: WifiLogRecord,
        recordType: String,
        networkIndex: Int?,
        values: Map<WifiLogField, String?>,
        fields: List<WifiLogField>,
    ) {
        val row = listOf(
            "open-scanner.wifi-log.v1",
            record.index.toString(),
            record.elapsedMs.toString(),
            recordType,
            networkIndex?.toString().orEmpty(),
        ) + fields.map { values[it].orEmpty() }
        appendLine(row.joinToString(",", transform = ::escapeCsv))
    }

    private fun orderedFields(
        session: WifiLogSession,
        category: WifiLogFieldCategory? = null,
    ): List<WifiLogField> = WifiLogField.entries.filter {
        it in session.selectedFields && (category == null || it.category == category)
    }

    private fun jsonValue(field: WifiLogField, value: String?): String = when {
        value == null -> "null"
        field.valueKind == WifiLogValueKind.INTEGER -> value.toLongOrNull()?.toString() ?: jsonStringOrNull(value)
        field.valueKind == WifiLogValueKind.BOOLEAN && value in setOf("true", "false") -> value
        else -> jsonStringOrNull(value)
    }

    private fun jsonStringOrNull(value: String?): String = value?.let { "\"${escapeJson(it)}\"" } ?: "null"

    private fun displayValue(field: WifiLogField, value: String?): String = when {
        value == null -> "Unavailable"
        field.unit != null -> "$value ${field.unit}"
        else -> value
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = totalSeconds % 3_600L / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
    }

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
