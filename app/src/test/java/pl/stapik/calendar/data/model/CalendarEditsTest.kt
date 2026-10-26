package pl.stapik.calendar.data.model

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CalendarEditsTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-01T12:00:00.789Z"), ZoneOffset.UTC)
    private val edits = CalendarEdits(clock)
    private val stamp = "2026-03-01T12:00:00Z"

    private val dentist = CalendarEntry("2026-01-02", "Dentist")
    private val gym = CalendarEntry("2026-01-03", "Gym")
    private val payload = CalendarPayload(entries = listOf(dentist, gym), lastUpdate = "2026-01-01T00:00:00Z")

    @Test
    fun addAppendsTheEntryAndStampsThePayload() {
        val added = CalendarEntry("2026-01-04", "Party")

        val result = edits.add(payload, added)

        assertEquals(listOf(dentist, gym, added), result.entries)
        assertEquals(stamp, result.lastUpdate)
    }

    @Test
    fun replaceSwapsTheEntryInPlaceAndStampsThePayload() {
        val moved = dentist.copy(date = "2026-01-09")

        val result = edits.replace(payload, dentist, moved)

        assertEquals(listOf(moved, gym), result.entries)
        assertEquals(stamp, result.lastUpdate)
    }

    @Test
    fun replaceKeepsTheFieldsTheEntryCarriesFromOtherApps() {
        val withId = dentist.copy(unknownFields = JsonObject(mapOf("id" to JsonPrimitive(7))))
        val stored = payload.copy(entries = listOf(withId, gym))

        val result = edits.replace(stored, withId, dentist.copy(name = "Orthodontist"))

        assertEquals(withId.copy(name = "Orthodontist"), result.entries.first())
    }

    @Test
    fun replaceOfAnUnknownEntryChangesNothing() {
        assertSame(payload, edits.replace(payload, CalendarEntry("2026-05-05", "Ghost"), gym))
    }

    @Test
    fun replaceWithTheSameEntryChangesNothing() {
        assertSame(payload, edits.replace(payload, dentist, dentist.copy()))
    }

    @Test
    fun removeDropsOnlyTheFirstMatchingEntry() {
        val twice = payload.copy(entries = listOf(dentist, dentist, gym))

        val result = edits.remove(twice, dentist)

        assertEquals(listOf(dentist, gym), result.entries)
        assertEquals(stamp, result.lastUpdate)
    }

    @Test
    fun removeOfAnUnknownEntryChangesNothing() {
        assertSame(payload, edits.remove(payload, CalendarEntry("2026-05-05", "Ghost")))
    }

    @Test
    fun missingListsOnlyTheEntriesTheCalendarLacks() {
        val party = CalendarEntry("2026-01-04", "Party")

        assertEquals(listOf(party), edits.missing(payload, listOf(dentist, party)))
    }

    @Test
    fun missingCountsRepeatedEntries() {
        assertEquals(listOf(dentist), edits.missing(payload, listOf(dentist, dentist)))
    }

    @Test
    fun missingIgnoresTheIdsThatOtherAppsGive() {
        val identified = dentist.copy(unknownFields = JsonObject(mapOf("id" to JsonPrimitive(7))))

        assertEquals(emptyList<CalendarEntry>(), edits.missing(payload, listOf(identified)))
        assertEquals(emptyList<CalendarEntry>(), edits.missing(payload.copy(entries = listOf(identified)), listOf(dentist)))
    }

    @Test
    fun missingStillSeesOtherDifferences() {
        val timed = dentist.copy(time = "09:30")

        assertEquals(listOf(timed), edits.missing(payload, listOf(timed)))
    }

    @Test
    fun addMissingAppendsOnlyTheMissingEntriesAndStampsThePayload() {
        val party = CalendarEntry("2026-01-04", "Party")

        val result = edits.addMissing(payload, listOf(gym, party))

        assertEquals(listOf(dentist, gym, party), result.entries)
        assertEquals(stamp, result.lastUpdate)
    }

    @Test
    fun addMissingChangesNothingWhenThereIsNothingToAdd() {
        assertSame(payload, edits.addMissing(payload, listOf(gym, dentist)))
    }
}
