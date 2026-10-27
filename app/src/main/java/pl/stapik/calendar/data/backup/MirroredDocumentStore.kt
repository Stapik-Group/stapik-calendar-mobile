package pl.stapik.calendar.data.backup

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.LocalDocument
import pl.stapik.common.sync.ConflictCopy
import pl.stapik.common.sync.DocumentStore

class MirroredDocumentStore(
    private val delegate: DocumentStore,
    private val mirror: BackupMirror,
    private val year: Int
) : DocumentStore {
    override suspend fun load(): LocalDocument<DocumentEnvelope>? =
        delegate.load()?.also { document ->
            mirrorQuietly { if (!mirror.has(year)) mirror.write(year, document.content.toJson()) }
        }

    override suspend fun save(document: LocalDocument<DocumentEnvelope>) {
        delegate.save(document)
        mirrorQuietly { mirror.write(year, document.content.toJson()) }
    }

    override suspend fun saveConflictCopy(copy: ConflictCopy) = delegate.saveConflictCopy(copy)

    override suspend fun discard() {
        delegate.discard()
        mirrorQuietly { mirror.delete(year) }
    }

    private suspend fun mirrorQuietly(action: () -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                action()
            } catch (e: IOException) {
            }
        }
    }
}
