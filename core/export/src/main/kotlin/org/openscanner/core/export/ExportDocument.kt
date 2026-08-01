package org.openscanner.core.export

/** A complete, previewable document that can be shared as a temporary file. */
data class ExportDocument(
    val title: String,
    val fileName: String,
    val mimeType: String,
    val shareSubject: String,
    val redacted: Boolean,
    val content: String,
)
