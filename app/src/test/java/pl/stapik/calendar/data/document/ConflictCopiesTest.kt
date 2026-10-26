package pl.stapik.calendar.data.document

import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.stapik.calendar.data.model.CalendarDocument
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.model.CalendarPayload
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.sync.ConflictCopy
import pl.stapik.common.sync.ConflictSource

class ConflictCopiesTest {
    private val dentist = CalendarEntry("2026-01-02", "Dentist")
    private val createdAt = Instant.parse("2026-03-01T12:00:00Z")

    private fun copy(source: ConflictSource, content: String) =
        ConflictCopy(CalendarDocument.slot, source, createdAt, content)

    private fun envelopeWith(vararg entries: CalendarEntry): String {
        val payload = CalendarPayload(entries = entries.toList())
        return CalendarDocument.codec.encode(payload, DocumentEnvelope.empty(""), "2026-03-01T12:00:00Z").toJson()
    }

    @Test
    fun readsTheEntriesOfEachCopy() = runBlocking {
        val copies = ConflictCopies { listOf(copy(ConflictSource.LOCAL, envelopeWith(dentist))) }

        assertEquals(listOf(RecoverableCopy(createdAt, ConflictSource.LOCAL, listOf(dentist))), copies.all())
    }

    @Test
    fun keepsTheOrderOfTheStore() = runBlocking {
        val newer = copy(ConflictSource.CLOUD, envelopeWith(dentist)).copy(createdAt = createdAt.plusSeconds(60))
        val older = copy(ConflictSource.LOCAL, envelopeWith())

        val result = ConflictCopies { listOf(newer, older) }.all()

        assertEquals(listOf(ConflictSource.CLOUD, ConflictSource.LOCAL), result.map { it.source })
    }

    @Test
    fun skipsACopyThatIsNotAnEnvelope() = runBlocking {
        val copies = ConflictCopies {
            listOf(copy(ConflictSource.CLOUD, "not json"), copy(ConflictSource.LOCAL, envelopeWith(dentist)))
        }

        assertEquals(listOf(ConflictSource.LOCAL), copies.all().map { it.source })
    }

    @Test
    fun skipsACopyWhoseEntriesDoNotFitTheCalendar() = runBlocking {
        val broken = """{"lastUpdate":"2026-03-01T12:00:00Z","payload":{"entries":[{"date":"2026-01-02"}]}}"""

        assertEquals(emptyList<RecoverableCopy>(), ConflictCopies { listOf(copy(ConflictSource.LOCAL, broken)) }.all())
    }
}
