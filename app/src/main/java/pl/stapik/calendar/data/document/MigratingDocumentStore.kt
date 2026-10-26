package pl.stapik.calendar.data.document

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.stapik.calendar.data.cache.CalendarCacheMigration
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.LocalDocument
import pl.stapik.common.sync.ConflictCopy
import pl.stapik.common.sync.DocumentStore

class MigratingDocumentStore(
    private val delegate: DocumentStore,
    private val migration: CalendarCacheMigration
) : DocumentStore {
    private val lock = Mutex()
    private var migrated = false

    override suspend fun load(): LocalDocument<DocumentEnvelope>? {
        migrateOnce()
        return delegate.load()
    }

    override suspend fun save(document: LocalDocument<DocumentEnvelope>) {
        migrateOnce()
        delegate.save(document)
    }

    override suspend fun saveConflictCopy(copy: ConflictCopy) = delegate.saveConflictCopy(copy)

    override suspend fun discard() = delegate.discard()

    private suspend fun migrateOnce() {
        lock.withLock {
            if (migrated) return
            migration.migrateInto(delegate)
            migrated = true
        }
    }
}
