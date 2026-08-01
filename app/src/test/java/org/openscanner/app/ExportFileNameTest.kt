package org.openscanner.app

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExportFileNameTest {
    @Test
    fun stripsPathTraversalAndUnsafeCharacters() {
        val result = safeExportFileName("../../Wi-Fi log <private>.json")

        assertEquals("Wi-Fi_log__private_.json", result)
        assertFalse('/' in result)
    }

    @Test
    fun suppliesFallbackForAnEmptyName() {
        assertEquals("open-scanner-export.txt", safeExportFileName("../"))
    }

    @Test
    fun choosesANewSuffixInsteadOfOverwritingACachedExport() {
        val directory = Files.createTempDirectory("open-scanner-export-test-").toFile()
        try {
            File(directory, "report.json").createNewFile()
            File(directory, "report-2.json").createNewFile()

            assertEquals("report-3.json", nextAvailableExportFile(directory, "report.json").name)
        } finally {
            directory.deleteRecursively()
        }
    }
}
