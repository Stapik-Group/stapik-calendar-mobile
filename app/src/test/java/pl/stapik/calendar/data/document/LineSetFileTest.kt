package pl.stapik.calendar.data.document

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LineSetFileTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun file(name: String = "values") = LineSetFile(tmp.root.toPath().resolve(name))

    @Test
    fun aMissingFileIsAnEmptySet() {
        assertEquals(emptySet<String>(), file().load())
    }

    @Test
    fun addedValuesComeBack() {
        val values = file()

        values.add(listOf("b", "a"))
        values.add(listOf("a", "c"))

        assertEquals(setOf("a", "b", "c"), values.load())
    }

    @Test
    fun aValueCanBeRemoved() {
        val values = file()
        values.add(listOf("a", "b"))

        values.remove("a")
        values.remove("missing")

        assertEquals(setOf("b"), values.load())
    }

    @Test
    fun aNewInstanceReadsWhatWasWritten() {
        file().add(listOf("a"))

        assertEquals(setOf("a"), file().load())
    }

    @Test
    fun theDirectoryIsCreatedWhenNeeded() {
        val values = LineSetFile(tmp.root.toPath().resolve("nested").resolve("values"))

        values.add(listOf("a"))

        assertEquals(setOf("a"), values.load())
    }
}
