package pl.stapik.calendar.data

import pl.stapik.calendar.data.cache.CachedCalendar
import pl.stapik.calendar.data.cache.CalendarCacheStorage
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.LocalDocument
import pl.stapik.common.sync.ConflictCopy
import pl.stapik.common.sync.DocumentStore

internal class FakeCalendarCache(var calendar: CachedCalendar? = null) : CalendarCacheStorage {
    override suspend fun load(): CachedCalendar? = calendar

    override suspend fun save(calendar: CachedCalendar) {
        this.calendar = calendar
    }

    override suspend fun clear() {
        calendar = null
    }
}

internal class FakeDocumentStore(
    var document: LocalDocument<DocumentEnvelope>? = null,
    private val loadError: Exception? = null
) : DocumentStore {
    var saves = 0
        private set

    override suspend fun load(): LocalDocument<DocumentEnvelope>? {
        loadError?.let { throw it }
        return document
    }

    override suspend fun save(document: LocalDocument<DocumentEnvelope>) {
        this.document = document
        saves++
    }

    override suspend fun saveConflictCopy(copy: ConflictCopy) = Unit

    override suspend fun discard() {
        document = null
    }
}
