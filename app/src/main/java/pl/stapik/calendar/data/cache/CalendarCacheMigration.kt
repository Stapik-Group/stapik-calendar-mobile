package pl.stapik.calendar.data.cache

import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CancellationException
import pl.stapik.calendar.data.model.CalendarDocument
import pl.stapik.calendar.data.model.CalendarPayload
import pl.stapik.common.model.CloudScope
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.LocalDocument
import pl.stapik.common.sync.DocumentStore

class CalendarCacheMigration(
    private val legacy: CalendarCacheStorage,
    private val clock: Clock = Clock.systemUTC()
) {
    suspend fun migrateInto(store: DocumentStore): Boolean {
        if (holdsDocument(store)) return false
        val cached = legacy.load() ?: return false
        store.save(toLocalDocument(cached))
        return true
    }

    private suspend fun holdsDocument(store: DocumentStore): Boolean =
        try {
            store.load() != null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            true
        }

    private fun toLocalDocument(cached: CachedCalendar): LocalDocument<DocumentEnvelope> {
        val modifiedAt = cached.modifiedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val stamp = (modifiedAt ?: clock.instant()).truncatedTo(ChronoUnit.SECONDS).toString()
        val payload = CalendarPayload(entries = cached.entries, lastUpdate = stamp)
        return LocalDocument(
            content = CalendarDocument.codec.encode(payload, DocumentEnvelope.empty(stamp), stamp),
            cloudBaseline = cached.updatedAt,
            dirty = cached.dirty,
            modifiedAt = modifiedAt,
            scope = cached.scope?.let(CloudScope::fromWire)
        )
    }
}
