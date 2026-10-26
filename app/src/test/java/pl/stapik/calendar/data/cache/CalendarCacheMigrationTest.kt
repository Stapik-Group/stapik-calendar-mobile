package pl.stapik.calendar.data.cache

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.stapik.calendar.data.FakeCalendarCache
import pl.stapik.calendar.data.FakeDocumentStore
import pl.stapik.calendar.data.model.CalendarDocument
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.common.model.CloudScope
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.LocalDocument

class CalendarCacheMigrationTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC)
    private val entry = CalendarEntry("2026-01-02", "Dentist", "", "red")

    private fun migration(cache: FakeCalendarCache) = CalendarCacheMigration(cache, clock)

    @Test
    fun importsUnsentChangesTogetherWithTheBaselineAndTheScope() = runBlocking {
        val cache = FakeCalendarCache(
            CachedCalendar(
                entries = listOf(entry),
                updatedAt = "2026-01-01T10:00:00Z",
                dirty = true,
                scope = "READ_WRITE",
                modifiedAt = "2026-01-02T08:30:15Z"
            )
        )
        val store = FakeDocumentStore()

        assertTrue(migration(cache).migrateInto(store))

        val document = store.document!!
        assertEquals("2026-01-01T10:00:00Z", document.cloudBaseline)
        assertTrue(document.dirty)
        assertEquals(CloudScope.READ_WRITE, document.scope)
        assertEquals(Instant.parse("2026-01-02T08:30:15Z"), document.modifiedAt)
        assertNull(document.cloudIdentity)
        assertEquals(listOf(entry), CalendarDocument.codec.decode(document.content).entries)
        assertEquals("2026-01-02T08:30:15Z", document.content.lastUpdate)
    }

    @Test
    fun importsACleanCacheAsSynchronised() = runBlocking {
        val cache = FakeCalendarCache(
            CachedCalendar(entries = listOf(entry), updatedAt = "2026-01-01T10:00:00Z", scope = "READ_ONLY")
        )
        val store = FakeDocumentStore()

        assertTrue(migration(cache).migrateInto(store))

        val document = store.document!!
        assertFalse(document.dirty)
        assertEquals(CloudScope.READ_ONLY, document.scope)
        assertEquals("2026-03-01T12:00:00Z", document.content.lastUpdate)
    }

    @Test
    fun keepsAnUnsyncedLocalCalendarUnsynced() = runBlocking {
        val cache = FakeCalendarCache(CachedCalendar(entries = listOf(entry), updatedAt = "", dirty = true))
        val store = FakeDocumentStore()

        migration(cache).migrateInto(store)

        val document = store.document!!
        assertFalse(document.everSynchronized)
        assertTrue(document.dirty)
        assertNull(document.scope)
    }

    @Test
    fun forgetsTheLegacyCacheOnceItIsImported() = runBlocking {
        val cache = FakeCalendarCache(CachedCalendar(entries = listOf(entry), updatedAt = "2026-01-01T10:00:00Z"))

        migration(cache).migrateInto(FakeDocumentStore())

        assertNull(cache.calendar)
    }

    @Test
    fun doesNothingWithoutALegacyCache() = runBlocking {
        val store = FakeDocumentStore()

        assertFalse(migration(FakeCalendarCache()).migrateInto(store))
        assertEquals(0, store.saves)
    }

    @Test
    fun doesNotOverwriteAnExistingDocument() = runBlocking {
        val existing = LocalDocument(DocumentEnvelope.empty("2026-02-01T00:00:00Z"))
        val store = FakeDocumentStore(existing)
        val cache = FakeCalendarCache(CachedCalendar(entries = listOf(entry), updatedAt = "2026-01-01T10:00:00Z"))

        assertFalse(migration(cache).migrateInto(store))
        assertEquals(existing, store.document)
        assertEquals(0, store.saves)
    }

    @Test
    fun doesNotOverwriteAStoreThatCannotBeRead() = runBlocking {
        val store = FakeDocumentStore(loadError = IllegalStateException("unreadable"))
        val cache = FakeCalendarCache(CachedCalendar(entries = listOf(entry), updatedAt = "2026-01-01T10:00:00Z"))

        assertFalse(migration(cache).migrateInto(store))
        assertEquals(0, store.saves)
        assertNotNull(cache.calendar)
    }
}
