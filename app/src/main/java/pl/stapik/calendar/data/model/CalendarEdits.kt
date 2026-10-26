package pl.stapik.calendar.data.model

import java.time.Clock
import java.time.temporal.ChronoUnit

class CalendarEdits(private val clock: Clock = Clock.systemUTC()) {
    fun add(payload: CalendarPayload, entry: CalendarEntry): CalendarPayload =
        changed(payload, payload.entries + entry)

    fun replace(payload: CalendarPayload, old: CalendarEntry, new: CalendarEntry): CalendarPayload {
        val index = payload.entries.indexOf(old)
        if (index < 0) return payload
        val kept = new.copy(unknownFields = old.unknownFields)
        return changed(payload, payload.entries.toMutableList().also { it[index] = kept })
    }

    fun remove(payload: CalendarPayload, entry: CalendarEntry): CalendarPayload {
        val index = payload.entries.indexOf(entry)
        if (index < 0) return payload
        return changed(payload, payload.entries.toMutableList().also { it.removeAt(index) })
    }

    fun missing(payload: CalendarPayload, entries: List<CalendarEntry>): List<CalendarEntry> {
        val remaining = payload.entries.toMutableList()
        return entries.filterNot { remaining.remove(it) }
    }

    fun addMissing(payload: CalendarPayload, entries: List<CalendarEntry>): CalendarPayload =
        changed(payload, payload.entries + missing(payload, entries))

    private fun changed(payload: CalendarPayload, entries: List<CalendarEntry>): CalendarPayload =
        if (entries == payload.entries) payload else payload.copy(entries = entries, lastUpdate = now())

    private fun now(): String = clock.instant().truncatedTo(ChronoUnit.SECONDS).toString()
}
