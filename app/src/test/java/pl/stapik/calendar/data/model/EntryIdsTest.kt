package pl.stapik.calendar.data.model

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryIdsTest {
    private val shape = Regex("[0-9a-f]{16}")

    private fun entry(id: String, name: String = "Dentist") = CalendarEntry(id, "2026-01-02", name)

    private val dentist = entry("")
    private val gym = CalendarEntry("", "2026-01-03", "Gym", "https://x.test", "red", "Clinic", "09:30")

    @Test
    fun generatedIdsAreSixteenLowercaseHexDigits() {
        repeat(100) { assertTrue(shape.matches(EntryIds.generate())) }
    }

    @Test
    fun generatedIdsAreDistinct() {
        assertEquals(1000, List(1000) { EntryIds.generate() }.toSet().size)
    }

    @Test
    fun generationFollowsTheGivenRandomSource() {
        assertEquals(EntryIds.generate(Random(1)), EntryIds.generate(Random(1)))
        assertNotEquals(EntryIds.generate(Random(1)), EntryIds.generate(Random(2)))
    }

    @Test
    fun contentIdsStayTheSameBetweenReleases() {
        assertEquals("2dc102b6ab653229", EntryIds.contentId(dentist))
        assertEquals("e6ff65f329bcd82f", EntryIds.contentId(gym))
    }

    @Test
    fun contentIdsDependOnEveryContentFieldAndNotOnTheIdOrOnWhereTheFieldsSplit() {
        val base = EntryIds.contentId(dentist)

        assertEquals(base, EntryIds.contentId(dentist.copy(id = "other")))
        assertNotEquals(base, EntryIds.contentId(dentist.copy(date = "2026-01-03")))
        assertNotEquals(base, EntryIds.contentId(dentist.copy(name = "Gym")))
        assertNotEquals(base, EntryIds.contentId(dentist.copy(link = "https://x.test")))
        assertNotEquals(base, EntryIds.contentId(dentist.copy(color = "red")))
        assertNotEquals(base, EntryIds.contentId(dentist.copy(location = "Clinic")))
        assertNotEquals(base, EntryIds.contentId(dentist.copy(time = "09:30")))
        assertNotEquals(
            EntryIds.contentId(dentist.copy(name = "ab", link = "c")),
            EntryIds.contentId(dentist.copy(name = "a", link = "bc"))
        )
    }

    @Test
    fun contentIdsHaveTheShapeOfGeneratedOnes() {
        assertTrue(shape.matches(EntryIds.contentId(dentist)))
    }

    @Test
    fun entriesWithDistinctIdsAreReturnedUntouched() {
        val entries = listOf(entry("a"), entry("b"))

        assertEquals(entries, EntryIds.withUniqueIds(entries))
        assertSame(entries[0], EntryIds.withUniqueIds(entries)[0])
    }

    @Test
    fun theFirstEntryKeepsAnIdThatLaterEntriesRepeat() {
        val result = EntryIds.withUniqueIds(listOf(entry("a"), entry("a", "Gym"), entry("a", "Party")))

        assertEquals("a", result[0].id)
        assertEquals("9fd357443296a7d1", result[1].id)
        assertEquals("0f902cf93256f2de", result[2].id)
        assertEquals(listOf("Dentist", "Gym", "Party"), result.map { it.name })
    }

    @Test
    fun repeatedIdsAreReplacedTheSameWayEveryTime() {
        val entries = listOf(entry("a"), entry("a"), entry("a"))

        assertEquals(EntryIds.withUniqueIds(entries), EntryIds.withUniqueIds(entries))
    }

    @Test
    fun aReplacementIdThatMeetsAnotherEntrysIdStillEndsUpUnique() {
        val result = EntryIds.withUniqueIds(listOf(entry("a"), entry("a"), entry("9fd357443296a7d1", "Gym")))

        assertEquals(3, result.map { it.id }.toSet().size)
    }
}
