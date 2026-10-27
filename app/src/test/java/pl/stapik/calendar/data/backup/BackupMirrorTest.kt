package pl.stapik.calendar.data.backup

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupMirrorTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val mirror by lazy { BackupMirror(tmp.root.toPath().resolve("mirror")) }

    @Test
    fun nothingIsStoredAtFirst() {
        assertEquals(emptySet<Int>(), mirror.years())
        assertNull(mirror.read(2026))
        assertFalse(mirror.has(2026))
    }

    @Test
    fun readsBackWhatWasWritten() {
        mirror.write(2026, """{"a":"ąę"}""")

        assertEquals("""{"a":"ąę"}""", mirror.read(2026))
        assertTrue(mirror.has(2026))
    }

    @Test
    fun writingAgainReplacesTheContent() {
        mirror.write(2026, "first")
        mirror.write(2026, "second")

        assertEquals("second", mirror.read(2026))
    }

    @Test
    fun listsTheYearsThatWereWritten() {
        mirror.write(2026, "a")
        mirror.write(2024, "b")

        assertEquals(setOf(2024, 2026), mirror.years())
    }

    @Test
    fun ignoresLeftoverTemporaryFiles() {
        mirror.write(2026, "a")
        Files.write(tmp.root.toPath().resolve("mirror").resolve("calendar@2025.json.tmp"), "half".toByteArray())

        assertEquals(setOf(2026), mirror.years())
    }

    @Test
    fun deleteForgetsTheYear() {
        mirror.write(2026, "a")

        mirror.delete(2026)
        mirror.delete(2026)

        assertFalse(mirror.has(2026))
        assertEquals(emptySet<Int>(), mirror.years())
    }
}
