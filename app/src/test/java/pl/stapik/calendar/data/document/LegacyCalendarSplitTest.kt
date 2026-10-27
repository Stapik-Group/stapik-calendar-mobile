package pl.stapik.calendar.data.document

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.stapik.calendar.data.FakeDocumentStore
import pl.stapik.calendar.data.envelopeOf
import pl.stapik.calendar.data.model.CalendarDocument
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.common.model.CloudScope
import pl.stapik.common.model.LocalDocument

class LegacyCalendarSplitTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC)
    private val dentist = CalendarEntry("dentist", "2026-01-02", "Dentist")
    private val gym = CalendarEntry("gym", "2025-12-31", "Gym")
    private val stores = mutableMapOf<Int, FakeDocumentStore>()

    private fun store(year: Int) = stores.getOrPut(year) { FakeDocumentStore() }

    private fun split(legacy: FakeDocumentStore) = LegacyCalendarSplit(legacy, ::store, clock)

    private fun entriesOf(year: Int) = CalendarDocument.codec.decode(checkNotNull(store(year).document).content).entries

    @Test
    fun nothingToSplitWithoutALegacyDocument() = runBlocking {
        assertNull(split(FakeDocumentStore()).run())
        assertTrue(stores.isEmpty())
    }

    @Test
    fun everyYearGetsItsOwnDocument() = runBlocking {
        val legacy = FakeDocumentStore(LocalDocument(envelopeOf(dentist, gym)))

        val result = checkNotNull(split(legacy).run())

        assertEquals(setOf(2025, 2026), result.years)
        assertEquals(listOf(gym), entriesOf(2025))
        assertEquals(listOf(dentist), entriesOf(2026))
    }

    @Test
    fun theLegacyDocumentIsRemovedAfterwards() = runBlocking {
        val legacy = FakeDocumentStore(LocalDocument(envelopeOf(dentist)))

        split(legacy).run()

        assertNull(legacy.document)
    }

    @Test
    fun theDocumentsOfTheYearsAreWaitingToBeSent() = runBlocking {
        val legacy = FakeDocumentStore(
            LocalDocument(
                envelopeOf(dentist),
                cloudBaseline = "2026-01-01T00:00:00Z",
                modifiedAt = Instant.parse("2026-02-01T08:00:00Z"),
                scope = CloudScope.READ_WRITE
            )
        )

        split(legacy).run()

        val document = checkNotNull(store(2026).document)
        assertTrue(document.dirty)
        assertEquals("", document.cloudBaseline)
        assertEquals(Instant.parse("2026-02-01T08:00:00Z"), document.modifiedAt)
        assertEquals(CloudScope.READ_WRITE, document.scope)
    }

    @Test
    fun itIsRememberedWhetherTheLegacyCalendarHadBeenSynchronised() = runBlocking {
        val synchronised = FakeDocumentStore(LocalDocument(envelopeOf(dentist), cloudBaseline = "2026-01-01T00:00:00Z"))
        val localOnly = FakeDocumentStore(LocalDocument(envelopeOf(dentist)))

        assertTrue(checkNotNull(split(synchronised).run()).wasSynchronized)
        assertFalse(checkNotNull(split(localOnly).run()).wasSynchronized)
    }

    @Test
    fun entriesWithoutADateGoToTheCurrentYear() = runBlocking {
        val broken = CalendarEntry("broken", "not a date", "Broken")

        split(FakeDocumentStore(LocalDocument(envelopeOf(broken)))).run()

        assertEquals(listOf(broken), entriesOf(2026))
    }

    @Test
    fun aDocumentThatAlreadyExistsIsCompletedNotReplaced() = runBlocking {
        val barber = CalendarEntry("barber", "2026-02-02", "Barber")
        store(2026).document = LocalDocument(envelopeOf(barber, dentist))

        split(FakeDocumentStore(LocalDocument(envelopeOf(dentist, gym)))).run()

        assertEquals(listOf(barber, dentist), entriesOf(2026))
        assertEquals(listOf(gym), entriesOf(2025))
    }

    @Test
    fun anEmptyLegacyDocumentIsJustRemoved() = runBlocking {
        val legacy = FakeDocumentStore(LocalDocument(envelopeOf()))

        val result = checkNotNull(split(legacy).run())

        assertEquals(emptySet<Int>(), result.years)
        assertNull(legacy.document)
    }
}
