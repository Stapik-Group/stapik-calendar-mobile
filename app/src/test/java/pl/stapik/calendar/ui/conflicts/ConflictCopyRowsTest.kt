package pl.stapik.calendar.ui.conflicts

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.stapik.calendar.data.document.RecoverableCopy
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.model.CalendarPayload
import pl.stapik.common.sync.ConflictSource

class ConflictCopyRowsTest {
    private val dentist = CalendarEntry("dentist", "2026-01-02", "Dentist")
    private val gym = CalendarEntry("gym", "2026-01-03", "Gym")
    private val party = CalendarEntry("party", "2026-01-04", "Party")
    private val createdAt = Instant.parse("2026-03-01T12:00:00Z")

    private fun copyOf(vararg entries: CalendarEntry) = RecoverableCopy(createdAt, ConflictSource.LOCAL, entries.toList())

    @Test
    fun eachRowCountsTheEntriesTheCalendarLacks() {
        val current = CalendarPayload(entries = listOf(dentist))

        val rows = conflictCopyRows(current, listOf(copyOf(dentist, gym, party), copyOf(dentist)))

        assertEquals(listOf(2, 0), rows.map { it.missingCount })
    }

    @Test
    fun noCopiesGiveNoRows() {
        assertEquals(emptyList<ConflictCopyRow>(), conflictCopyRows(CalendarPayload(), emptyList()))
    }
}
