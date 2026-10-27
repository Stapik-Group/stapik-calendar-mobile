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

    private val dentist = CalendarEntry("dentist", "2026-01-02", "Dentist")
    private val gym = CalendarEntry("gym", "2026-01-03", "Gym")
    private val ghost = CalendarEntry("ghost", "2026-05-05", "Ghost")
    private val payload = CalendarPayload(entries = listOf(dentist, gym), lastUpdate = "2026-01-01T00:00:00Z")

    @Test
    fun addAppendsTheEntryAndStampsThePayload() {
        val added = CalendarEntry("party", "2026-01-04", "Party")

        val result = edits.add(payload, added)

        assertEquals(listOf(dentist, gym, added), result.entries)
        assertEquals(stamp, result.lastUpdate)
    }

    @Test
    fun addOfAnEntryThatIsAlreadyThereChangesNothing() {
        assertSame(payload, edits.add(payload, dentist.copy(name = "Again")))
    }

    @Test
    fun replaceSwapsTheEntryWithTheSameIdInPlaceAndStampsThePayload() {
        val moved = dentist.copy(date = "2026-01-09")

        val result = edits.replace(payload, moved)

        assertEquals(listOf(moved, gym), result.entries)
        assertEquals(stamp, result.lastUpdate)
    }

    @Test
    fun replaceStillFindsTheEntryAfterItsOtherFieldsChangedElsewhere() {
        val changedElsewhere = payload.copy(entries = listOf(dentist.copy(name = "Orthodontist"), gym))

        val result = edits.replace(changedElsewhere, dentist.copy(time = "09:30"))

        assertEquals(listOf(dentist.copy(time = "09:30"), gym), result.entries)
    }

    @Test
    fun replaceKeepsTheFieldsTheStoredEntryCarriesFromOtherApps() {
        val hidden = JsonObject(mapOf("reminder" to JsonPrimitive(true)))
        val stored = payload.copy(entries = listOf(dentist.copy(unknownFields = hidden), gym))

        val result = edits.replace(stored, dentist.copy(name = "Orthodontist"))

        assertEquals(dentist.copy(name = "Orthodontist", unknownFields = hidden), result.entries.first())
    }

    @Test
    fun replaceTouchesOnlyTheEntryWithThatIdEvenWhenAnotherLooksTheSame() {
        val twin = dentist.copy(id = "twin")
        val twins = payload.copy(entries = listOf(dentist, twin, gym))

        val result = edits.replace(twins, twin.copy(date = "2026-01-09"))

        assertEquals(listOf(dentist, twin.copy(date = "2026-01-09"), gym), result.entries)
    }

    @Test
    fun replaceOfAnUnknownEntryChangesNothing() {
        assertSame(payload, edits.replace(payload, ghost))
    }

    @Test
    fun replaceWithTheSameEntryChangesNothing() {
        assertSame(payload, edits.replace(payload, dentist.copy()))
    }

    @Test
    fun removeDropsTheEntryWithThatId() {
        val result = edits.remove(payload, "dentist")

        assertEquals(listOf(gym), result.entries)
        assertEquals(stamp, result.lastUpdate)
    }

    @Test
    fun removeKeepsAnEntryThatLooksTheSame() {
        val twin = dentist.copy(id = "twin")
        val twins = payload.copy(entries = listOf(dentist, twin, gym))

        assertEquals(listOf(dentist, gym), edits.remove(twins, "twin").entries)
    }

    @Test
    fun removeOfAnUnknownIdChangesNothing() {
        assertSame(payload, edits.remove(payload, "ghost"))
    }

    @Test
    fun missingListsOnlyTheEntriesTheCalendarLacks() {
        val party = CalendarEntry("party", "2026-01-04", "Party")

        assertEquals(listOf(party), edits.missing(payload, listOf(dentist, party)))
    }

    @Test
    fun missingRecognizesAnEntryByItsIdEvenWhenItsContentDiffers() {
        assertEquals(emptyList<CalendarEntry>(), edits.missing(payload, listOf(dentist.copy(name = "Orthodontist"))))
    }

    @Test
    fun missingRecognizesAnEntryWithAnotherIdByItsContent() {
        assertEquals(emptyList<CalendarEntry>(), edits.missing(payload, listOf(dentist.copy(id = "from-elsewhere"))))
    }

    @Test
    fun missingCountsRepeatedContentOfEntriesWithOtherIds() {
        val first = dentist.copy(id = "a")
        val second = dentist.copy(id = "b")

        assertEquals(listOf(second), edits.missing(payload, listOf(first, second)))
    }

    @Test
    fun missingStillSeesOtherDifferences() {
        val timed = dentist.copy(id = "from-elsewhere", time = "09:30")

        assertEquals(listOf(timed), edits.missing(payload, listOf(timed)))
    }

    @Test
    fun addMissingAppendsOnlyTheMissingEntriesAndStampsThePayload() {
        val party = CalendarEntry("party", "2026-01-04", "Party")

        val result = edits.addMissing(payload, listOf(gym, party))

        assertEquals(listOf(dentist, gym, party), result.entries)
        assertEquals(stamp, result.lastUpdate)
    }

    @Test
    fun addMissingChangesNothingWhenThereIsNothingToAdd() {
        assertSame(payload, edits.addMissing(payload, listOf(gym, dentist)))
    }
}
