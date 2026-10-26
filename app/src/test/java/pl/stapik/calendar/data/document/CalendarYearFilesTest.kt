package pl.stapik.calendar.data.document

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CalendarYearFilesTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val directory get() = tmp.root.toPath()

    private fun touch(name: String) {
        Files.createFile(directory.resolve(name))
    }

    @Test
    fun aMissingDirectoryHasNoYears() {
        assertEquals(emptySet<Int>(), CalendarYearFiles(directory.resolve("missing")).years())
    }

    @Test
    fun yearsComeFromTheDocumentsOfTheYears() {
        touch("calendar.json@2025.doc")
        touch("calendar.json@2026.doc")

        assertEquals(setOf(2025, 2026), CalendarYearFiles(directory).years())
    }

    @Test
    fun otherFilesAreNotYears() {
        touch("calendar.json.doc")
        touch("calendar.json@2026.doc.bak")
        touch("calendar.json@2026.doc.tmp")
        touch("calendar.json@abcd.doc")
        touch("notes.json@2026.doc")
        touch("pending-merges")

        assertEquals(emptySet<Int>(), CalendarYearFiles(directory).years())
    }
}
