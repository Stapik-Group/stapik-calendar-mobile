package pl.stapik.calendar.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import pl.stapik.calendar.data.cache.CachedCalendar
import pl.stapik.calendar.data.cache.CalendarCacheMigration
import pl.stapik.calendar.data.document.MigratingDocumentStore
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.LocalDocument

class MigratingDocumentStoreTest {
    private val cached = CachedCalendar(
        entries = listOf(CalendarEntry("2026-01-02", "Dentist")),
        updatedAt = "2026-01-01T10:00:00Z"
    )

    @Test
    fun importsTheLegacyCacheBeforeTheFirstLoad() = runBlocking {
        val delegate = FakeDocumentStore()
        val store = MigratingDocumentStore(delegate, CalendarCacheMigration(FakeCalendarCache(cached)))

        assertNotNull(store.load())
        assertEquals(1, delegate.saves)
    }

    @Test
    fun importsTheLegacyCacheBeforeTheFirstSave() = runBlocking {
        val delegate = FakeDocumentStore()
        val store = MigratingDocumentStore(delegate, CalendarCacheMigration(FakeCalendarCache(cached)))

        store.save(LocalDocument(DocumentEnvelope.empty("")))

        assertEquals(2, delegate.saves)
    }

    @Test
    fun neverImportsTwice() = runBlocking {
        val delegate = FakeDocumentStore()
        val store = MigratingDocumentStore(delegate, CalendarCacheMigration(FakeCalendarCache(cached)))

        store.load()
        store.discard()

        assertNull(store.load())
        assertEquals(1, delegate.saves)
    }
}
