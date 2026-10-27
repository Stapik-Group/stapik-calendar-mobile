package pl.stapik.calendar.data.document

import java.time.Instant
import pl.stapik.calendar.data.model.CalendarDocument
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.common.model.DocumentDecodeException
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.EnvelopeFormatException
import pl.stapik.common.sync.ConflictCopy
import pl.stapik.common.sync.ConflictSource

data class RecoverableCopy(
    val createdAt: Instant,
    val source: ConflictSource,
    val entries: List<CalendarEntry>
)

class ConflictCopies(private val listCopies: suspend () -> List<ConflictCopy>) {
    suspend fun all(): List<RecoverableCopy> = listCopies().mapNotNull(::readable)

    private fun readable(copy: ConflictCopy): RecoverableCopy? =
        try {
            val payload = CalendarDocument.codec.decode(DocumentEnvelope.parse(copy.content))
            RecoverableCopy(copy.createdAt, copy.source, payload.entries)
        } catch (e: EnvelopeFormatException) {
            null
        } catch (e: DocumentDecodeException) {
            null
        }
}
