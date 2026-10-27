package pl.stapik.calendar.data.document

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import pl.stapik.calendar.data.FakeCloud
import pl.stapik.calendar.data.FakeDocumentStore
import pl.stapik.calendar.data.envelopeOf
import pl.stapik.calendar.data.model.CalendarDocument
import pl.stapik.calendar.data.model.CalendarEdits
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.model.CalendarYears
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.LocalDocument
import pl.stapik.common.sync.CloudError
import pl.stapik.common.sync.ConflictCopy
import pl.stapik.common.sync.DocumentStore
import pl.stapik.common.sync.FirstConnectionChoice
import pl.stapik.common.sync.SyncEngine
import pl.stapik.common.sync.SyncOutcome

class CalendarBookTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val clock = Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC)
    private val edits = CalendarEdits(clock)
    private val directory: Path get() = tmp.root.toPath()
    private val stores = mutableMapOf<Int, DocumentStore>()
    private val legacy = FakeDocumentStore()
    private var cloud: FakeCloud? = FakeCloud()
    private var restored: suspend () -> Unit = {}

    private val dentist = CalendarEntry("dentist", "2026-01-02", "Dentist")
    private val barber = CalendarEntry("barber", "2026-02-02", "Barber")
    private val gym = CalendarEntry("gym", "2025-12-31", "Gym")
    private val trip = CalendarEntry("trip", "2024-07-01", "Trip")

    private val book by lazy {
        CalendarBook(
            engineOf = { year ->
                SyncEngine(CalendarDocument.slotOf(year), CalendarDocument.codec, store(year), { cloud }, clock, CalendarDocument.schemaGate)
            },
            files = CalendarYearFiles(directory),
            legacySplit = LegacyCalendarSplit(legacy, ::store, clock),
            gateway = { cloud },
            pendingMerges = LineSetFile(directory.resolve("pending-merges")),
            importedClouds = LineSetFile(directory.resolve("imported-clouds")),
            restoreBackup = { restored() },
            clock = clock
        )
    }

    private fun store(year: Int): DocumentStore = stores.getOrPut(year) { FakeDocumentStore() }

    private suspend fun add(entry: CalendarEntry) {
        book.edit(checkNotNull(CalendarYears.yearOf(entry))) { edits.add(it, entry) }
    }

    private suspend fun entries() = book.load().entries.sortedBy { it.date }

    private fun remoteEntries(year: Int): List<CalendarEntry> =
        cloud!!.documents[CalendarDocument.slotOf(year)]
            ?.let { CalendarDocument.codec.decode(DocumentEnvelope.parse(it.content)).entries }
            .orEmpty()

    @Test
    fun aYearWrittenWithANewerSchemaIsNotEdited() = runBlocking {
        val newer = DocumentEnvelope.parse(
            """{"lastUpdate":"2026-01-01T00:00:00Z","payload":{"schemaVersion":2,"lastUpdate":"2026-01-01T00:00:00Z","entries":[{"id":"dentist","date":"2026-01-02","name":"Dentist"}]}}"""
        )
        val newerStore = store(2026) as FakeDocumentStore
        newerStore.document = LocalDocument(newer)

        val refused = book.edit(2026) { edits.add(it, barber) }

        assertTrue(refused is SyncOutcome.NewerSchema)
        assertEquals(newer.toJson(), newerStore.document!!.content.toJson())
        assertEquals(0, newerStore.saves)
    }

    @Test
    fun theBackupIsRestoredBeforeTheFirstLoad() = runBlocking {
        restored = { store(2026).save(LocalDocument(envelopeOf(dentist))) }

        assertEquals(listOf(dentist), entries())
    }

    @Test
    fun entriesOfEveryYearAreLoadedTogether() = runBlocking {
        cloud = null
        Files.createFile(directory.resolve("calendar.json@2025.doc"))
        add(dentist)
        add(gym)

        assertEquals(listOf(gym, dentist), entries())
    }

    @Test
    fun withoutACloudTheCalendarStaysLocal() = runBlocking {
        cloud = null
        add(dentist)

        assertEquals(SyncOutcome.LocalOnly, book.sync())
        assertTrue(book.hasPendingChanges())
    }

    @Test
    fun everyYearIsSentToItsOwnPartition() = runBlocking {
        add(dentist)
        add(gym)

        assertEquals(SyncOutcome.Synchronized, book.sync())

        assertEquals(listOf(gym), remoteEntries(2025))
        assertEquals(listOf(dentist), remoteEntries(2026))
        assertFalse(cloud!!.documents.containsKey(CalendarDocument.slot))
        assertFalse(book.hasPendingChanges())
    }

    @Test
    fun yearsKnownOnlyToTheCloudAreDownloaded() = runBlocking {
        cloud!!.put(CalendarDocument.slotOf(2024), trip)
        cloud!!.put(CalendarDocument.slotOf(2026), dentist)

        book.sync()

        assertEquals(listOf(trip, dentist), entries())
    }

    @Test
    fun theWholeDocumentOfAnOlderVersionIsImported() = runBlocking {
        cloud!!.put(CalendarDocument.slot, gym, dentist)

        book.sync()

        assertEquals(listOf(gym), remoteEntries(2025))
        assertEquals(listOf(dentist), remoteEntries(2026))
        assertEquals(listOf(gym, dentist), entries())
    }

    @Test
    fun theWholeDocumentIsAddedToTheYearsThatTheCloudAlreadyHas() = runBlocking {
        cloud!!.put(CalendarDocument.slot, dentist, barber)
        cloud!!.put(CalendarDocument.slotOf(2026), dentist)

        book.sync()

        assertEquals(listOf(dentist, barber), remoteEntries(2026))
    }

    @Test
    fun theWholeDocumentIsImportedOnlyOncePerCloud() = runBlocking {
        cloud!!.put(CalendarDocument.slot, dentist)
        book.sync()
        book.edit(2026) { edits.remove(it, dentist.id) }
        book.sync()

        book.sync()

        assertEquals(emptyList<CalendarEntry>(), entries())
        assertEquals(emptyList<CalendarEntry>(), remoteEntries(2026))
    }

    @Test
    fun anotherCloudIsImportedAgain() = runBlocking {
        cloud!!.put(CalendarDocument.slot, dentist)
        book.sync()
        cloud = FakeCloud(identity = "cloud-2").also { it.put(CalendarDocument.slot, gym) }

        book.sync()

        assertTrue(gym in entries())
    }

    @Test
    fun anUnreachableCloudDoesNotMarkTheImportAsDone() = runBlocking {
        cloud!!.put(CalendarDocument.slot, dentist)
        cloud!!.failure = CloudError.Network()
        assertTrue(book.sync() is SyncOutcome.Offline)

        cloud!!.failure = null
        book.sync()

        assertEquals(listOf(dentist), remoteEntries(2026))
    }

    @Test
    fun aCalendarSynchronisedBeforeIsMergedWithTheCloudWithoutAsking() = runBlocking {
        legacy.document = LocalDocument(envelopeOf(dentist), cloudBaseline = "2026-01-01T00:00:00Z", dirty = true)
        cloud!!.put(CalendarDocument.slotOf(2026), barber)

        val outcome = book.sync()

        assertEquals(SyncOutcome.Synchronized, outcome)
        assertEquals(listOf(barber, dentist), remoteEntries(2026))
        assertEquals(listOf(dentist, barber), entries())
    }

    @Test
    fun aCalendarThatWasNeverSynchronisedAsksOnTheFirstConnection() = runBlocking {
        legacy.document = LocalDocument(envelopeOf(dentist), dirty = true)
        cloud!!.put(CalendarDocument.slotOf(2026), barber)

        assertEquals(SyncOutcome.NeedsFirstConnectionChoice(canKeepLocal = true), book.sync())

        assertEquals(SyncOutcome.ServerWon, book.sync(FirstConnectionChoice.USE_CLOUD))
        assertEquals(listOf(barber), entries())
    }

    @Test
    fun theChoiceOfTheUserAppliesToEveryYear() = runBlocking {
        legacy.document = LocalDocument(envelopeOf(dentist, gym), dirty = true)
        cloud!!.put(CalendarDocument.slotOf(2026), barber)
        cloud!!.put(CalendarDocument.slotOf(2025), trip)

        book.sync(FirstConnectionChoice.SEND_LOCAL)

        assertEquals(listOf(dentist), remoteEntries(2026))
        assertEquals(listOf(gym), remoteEntries(2025))
    }

    @Test
    fun anEditThatChangesNothingLeavesTheCalendarClean() = runBlocking {
        add(dentist)
        book.sync()

        assertEquals(null, book.edit(2026) { edits.remove(it, barber.id) })

        assertFalse(book.hasPendingChanges())
    }

    @Test
    fun aDamagedYearCanBeDiscardedWithoutTouchingTheOthers() = runBlocking {
        val damaged = DamagedStore()
        stores[2025] = damaged
        Files.createFile(directory.resolve("calendar.json@2025.doc"))
        add(dentist)

        book.discardUnreadable()

        assertTrue(damaged.discarded)
        assertEquals(listOf(dentist), CalendarDocument.codec.decode(checkNotNull(store(2026).load()).content).entries)
    }

    @Test
    fun theEnginesOfTheKnownYearsAreHandedOutForDisconnecting() {
        Files.createFile(directory.resolve("calendar.json@2024.doc"))

        assertEquals(2, book.engines().size)
    }
}

private class DamagedStore : DocumentStore {
    var discarded = false
        private set

    override suspend fun load(): LocalDocument<DocumentEnvelope>? = throw IllegalStateException("damaged")

    override suspend fun save(document: LocalDocument<DocumentEnvelope>) = Unit

    override suspend fun saveConflictCopy(copy: ConflictCopy) = Unit

    override suspend fun discard() {
        discarded = true
    }
}
