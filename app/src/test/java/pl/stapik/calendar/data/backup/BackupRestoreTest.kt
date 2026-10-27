package pl.stapik.calendar.data.backup

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import pl.stapik.calendar.data.FakeDocumentStore
import pl.stapik.calendar.data.envelopeOf
import pl.stapik.calendar.data.model.CalendarDocument
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.LocalDocument

class BackupRestoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val mirror by lazy { BackupMirror(tmp.root.toPath().resolve("mirror")) }
    private val stores = mutableMapOf<Int, FakeDocumentStore>()
    private val restore by lazy { BackupRestore(mirror) { year -> stores.getOrPut(year) { FakeDocumentStore() } } }

    private val dentist = CalendarEntry("dentist", "2026-01-02", "Dentist")
    private val gym = CalendarEntry("gym", "2025-12-31", "Gym")

    private fun entriesOf(year: Int) =
        CalendarDocument.codec.decode(stores.getValue(year).document!!.content).entries

    @Test
    fun bringsBackTheYearsThatTheDeviceLacksAsUnsyncedLocalDocuments() = runBlocking {
        mirror.write(2026, envelopeOf(dentist).toJson())
        mirror.write(2025, envelopeOf(gym).toJson())

        restore.run()

        assertEquals(listOf(dentist), entriesOf(2026))
        assertEquals(listOf(gym), entriesOf(2025))
        val document = stores.getValue(2026).document!!
        assertTrue(document.dirty)
        assertFalse(document.everSynchronized)
    }

    @Test
    fun leavesAYearThatTheDeviceAlreadyHas() = runBlocking {
        val present = LocalDocument(envelopeOf(gym))
        stores[2026] = FakeDocumentStore(present)
        mirror.write(2026, envelopeOf(dentist).toJson())

        restore.run()

        assertEquals(present, stores.getValue(2026).document)
        assertEquals(0, stores.getValue(2026).saves)
    }

    @Test
    fun leavesAYearWhoseStoredDocumentCannotBeRead() = runBlocking {
        stores[2026] = FakeDocumentStore(loadError = IllegalStateException("corrupted"))
        mirror.write(2026, envelopeOf(dentist).toJson())

        restore.run()

        assertEquals(0, stores.getValue(2026).saves)
    }

    @Test
    fun skipsACopyThatIsNotADocument() = runBlocking {
        mirror.write(2026, "not json")
        mirror.write(2025, """{"lastUpdate":"","payload":{"entries":"oops"}}""")

        restore.run()

        assertNull(stores[2026]?.document)
        assertNull(stores[2025]?.document)
    }

    @Test
    fun doesNothingWithoutCopies() = runBlocking {
        restore.run()

        assertEquals(emptyMap<Int, FakeDocumentStore>(), stores)
    }

    @Test
    fun keepsTheFieldsOfOtherAppsInARestoredDocument() = runBlocking {
        val text = """{"lastUpdate":"2026-01-01T00:00:00Z","extra":7,"payload":{"entries":[],"lastUpdate":"2026-01-01T00:00:00Z","payloadExtra":true}}"""
        mirror.write(2026, text)

        restore.run()

        assertEquals(DocumentEnvelope.parse(text).toJson(), stores.getValue(2026).document!!.content.toJson())
    }
}
