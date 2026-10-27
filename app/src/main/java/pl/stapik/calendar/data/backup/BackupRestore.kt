package pl.stapik.calendar.data.backup

import kotlinx.coroutines.CancellationException
import pl.stapik.calendar.data.model.CalendarDocument
import pl.stapik.common.model.DocumentDecodeException
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.EnvelopeFormatException
import pl.stapik.common.model.LocalDocument
import pl.stapik.common.sync.DocumentStore

class BackupRestore(
    private val mirror: BackupMirror,
    private val storeOf: (Int) -> DocumentStore
) {
    suspend fun run() {
        for (year in mirror.years()) {
            val store = storeOf(year)
            if (!hasNoDocument(store)) continue
            val envelope = readable(year) ?: continue
            store.save(LocalDocument(content = envelope, dirty = true))
        }
    }

    private suspend fun hasNoDocument(store: DocumentStore): Boolean =
        try {
            store.load() == null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }

    private fun readable(year: Int): DocumentEnvelope? =
        try {
            mirror.read(year)?.let(DocumentEnvelope::parse)?.also { CalendarDocument.codec.decode(it) }
        } catch (e: EnvelopeFormatException) {
            null
        } catch (e: DocumentDecodeException) {
            null
        }
}
