package pl.stapik.calendar.data.document

import java.time.Clock
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import pl.stapik.calendar.data.model.CalendarDocument
import pl.stapik.calendar.data.model.CalendarEdits
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.model.CalendarPayload
import pl.stapik.calendar.data.model.CalendarYears
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.LocalDocument
import pl.stapik.common.sync.DocumentStore

data class LegacySplit(val years: Set<Int>, val wasSynchronized: Boolean)

class LegacyCalendarSplit(
    private val legacy: DocumentStore,
    private val storeOf: (Int) -> DocumentStore,
    private val clock: Clock = Clock.systemUTC(),
    private val edits: CalendarEdits = CalendarEdits(clock)
) {
    suspend fun run(): LegacySplit? {
        val document = legacy.load() ?: return null
        val entries = CalendarDocument.codec.decode(document.content).entries
        val byYear = CalendarYears.split(entries, currentYear())
        byYear.forEach { (year, yearEntries) ->
            val store = storeOf(year)
            store.save(partition(store.load(), yearEntries, document))
        }
        legacy.discard()
        return LegacySplit(byYear.keys, document.everSynchronized)
    }

    private fun partition(
        existing: LocalDocument<DocumentEnvelope>?,
        entries: List<CalendarEntry>,
        legacy: LocalDocument<DocumentEnvelope>
    ): LocalDocument<DocumentEnvelope> {
        val stamp = clock.instant().truncatedTo(ChronoUnit.SECONDS).toString()
        val current = existing?.let { CalendarDocument.codec.decode(it.content) } ?: CalendarPayload()
        val merged = edits.addMissing(current, entries)
        val previous = existing?.content ?: DocumentEnvelope.empty(stamp)
        val envelope = CalendarDocument.codec.encode(merged, previous, merged.lastUpdate.ifEmpty { stamp })
        return (existing ?: LocalDocument(envelope)).copy(
            content = envelope,
            dirty = true,
            modifiedAt = legacy.modifiedAt,
            scope = legacy.scope
        )
    }

    private fun currentYear(): Int = clock.instant().atZone(ZoneOffset.UTC).year
}
