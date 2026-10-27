package pl.stapik.calendar.data.model

import java.time.Clock
import java.time.temporal.ChronoUnit

class CalendarEdits(private val clock: Clock = Clock.systemUTC()) {
    fun add(payload: CalendarPayload, entry: CalendarEntry): CalendarPayload =
        if (payload.entries.any { it.id == entry.id }) payload else changed(payload, payload.entries + entry)

    fun replace(payload: CalendarPayload, entry: CalendarEntry): CalendarPayload {
        val index = payload.entries.indexOfFirst { it.id == entry.id }
        if (index < 0) return payload
        val kept = entry.copy(unknownFields = payload.entries[index].unknownFields)
        return changed(payload, payload.entries.toMutableList().also { it[index] = kept })
    }

    fun remove(payload: CalendarPayload, id: String): CalendarPayload =
        changed(payload, payload.entries.filterNot { it.id == id })

    fun missing(payload: CalendarPayload, entries: List<CalendarEntry>): List<CalendarEntry> {
        val idsInCalendar = payload.entries.map { it.id }.toSet()
        val (sameId, otherId) = entries.partition { it.id in idsInCalendar }
        val matchedIds = sameId.map { it.id }.toSet()
        val unmatched = payload.entries
            .filterNot { it.id in matchedIds }
            .map { it.withoutId() }
            .toMutableList()
        return otherId.filterNot { unmatched.remove(it.withoutId()) }
    }

    fun addMissing(payload: CalendarPayload, entries: List<CalendarEntry>): CalendarPayload =
        changed(payload, payload.entries + missing(payload, entries))

    private fun CalendarEntry.withoutId(): CalendarEntry = copy(id = "")

    private fun changed(payload: CalendarPayload, entries: List<CalendarEntry>): CalendarPayload =
        if (entries == payload.entries) payload else payload.copy(entries = entries, lastUpdate = now())

    private fun now(): String = clock.instant().truncatedTo(ChronoUnit.SECONDS).toString()
}
