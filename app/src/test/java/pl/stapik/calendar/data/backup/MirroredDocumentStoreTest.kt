package pl.stapik.calendar.data.backup

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import pl.stapik.calendar.data.FakeDocumentStore
import pl.stapik.calendar.data.envelopeOf
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.common.model.LocalDocument

class MirroredDocumentStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val mirror by lazy { BackupMirror(tmp.root.toPath().resolve("mirror")) }
    private val delegate = FakeDocumentStore()
    private val store by lazy { MirroredDocumentStore(delegate, mirror, 2026) }

    private val document = LocalDocument(envelopeOf(CalendarEntry("dentist", "2026-01-02", "Dentist")))

    @Test
    fun savingKeepsAPlainCopyOfTheDocument() = runBlocking {
        store.save(document)

        assertEquals(document, delegate.document)
        assertEquals(document.content.toJson(), mirror.read(2026))
    }

    @Test
    fun savingAgainKeepsTheCopyCurrent() = runBlocking {
        store.save(document)
        val edited = LocalDocument(envelopeOf(CalendarEntry("gym", "2026-01-03", "Gym")))

        store.save(edited)

        assertEquals(edited.content.toJson(), mirror.read(2026))
    }

    @Test
    fun loadingADocumentThatHasNoCopyYetCreatesIt() = runBlocking {
        delegate.document = document

        assertEquals(document, store.load())

        assertEquals(document.content.toJson(), mirror.read(2026))
    }

    @Test
    fun loadingDoesNotOverwriteAnExistingCopy() = runBlocking {
        delegate.document = document
        mirror.write(2026, "kept")

        store.load()

        assertEquals("kept", mirror.read(2026))
    }

    @Test
    fun loadingNothingCreatesNoCopy() = runBlocking {
        assertNull(store.load())

        assertFalse(mirror.has(2026))
    }

    @Test
    fun discardingForgetsTheCopyToo() = runBlocking {
        store.save(document)

        store.discard()

        assertNull(delegate.document)
        assertFalse(mirror.has(2026))
    }
}
