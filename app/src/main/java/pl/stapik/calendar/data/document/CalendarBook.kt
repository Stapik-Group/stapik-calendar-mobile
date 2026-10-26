package pl.stapik.calendar.data.document

import java.time.Clock
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pl.stapik.calendar.data.model.CalendarDocument
import pl.stapik.calendar.data.model.CalendarEdits
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.model.CalendarPayload
import pl.stapik.calendar.data.model.CalendarYears
import pl.stapik.common.model.DocumentDecodeException
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.EnvelopeFormatException
import pl.stapik.common.sync.CloudGateway
import pl.stapik.common.sync.FirstConnectionChoice
import pl.stapik.common.sync.RemoteDocument
import pl.stapik.common.sync.SyncEngine
import pl.stapik.common.sync.SyncOutcome

class CalendarBook(
    private val engineOf: (Int) -> SyncEngine<CalendarPayload>,
    private val files: CalendarYearFiles,
    private val legacySplit: LegacyCalendarSplit,
    private val gateway: suspend () -> CloudGateway?,
    private val pendingMerges: LineSetFile,
    private val importedClouds: LineSetFile,
    private val clock: Clock = Clock.systemUTC(),
    private val edits: CalendarEdits = CalendarEdits(clock)
) {
    private val byYear = ConcurrentHashMap<Int, SyncEngine<CalendarPayload>>()
    private val preparation = Mutex()
    private var prepared = false
    private val activeSyncs = AtomicInteger()
    private val mutableSyncing = MutableStateFlow(false)

    val isSyncing: StateFlow<Boolean> = mutableSyncing.asStateFlow()

    fun engines(): List<SyncEngine<CalendarPayload>> = (files.years() + byYear.keys + currentYear()).sorted().map(::engine)

    suspend fun load(): CalendarPayload {
        prepare()
        val payloads = mutableListOf<CalendarPayload>()
        for (year in localYears()) payloads += engine(year).load()
        return CalendarPayload(
            entries = payloads.flatMap { it.entries },
            lastUpdate = payloads.maxOfOrNull { it.lastUpdate }.orEmpty()
        )
    }

    suspend fun edit(year: Int, change: (CalendarPayload) -> CalendarPayload): SyncOutcome.NewerSchema? {
        prepare()
        val engine = engine(year)
        val current = engine.load()
        if (change(current) == current) return null
        return engine.editLocally(change)
    }

    suspend fun sync(choice: FirstConnectionChoice? = null): SyncOutcome = syncing {
        prepare()
        val cloud = gateway()
        val known = years(if (cloud != null) cloudYears(cloud) else emptySet())
        val outcomes = mutableListOf<SyncOutcome>()
        for (year in known) outcomes += syncYear(year, choice)
        val combined = SyncOutcomes.combine(outcomes)
        if (cloud != null && combined in settled) {
            SyncOutcomes.combine(outcomes + importLegacyCloud(cloud))
        } else {
            combined
        }
    }

    suspend fun hasPendingChanges(): Boolean {
        prepare()
        for (year in localYears()) {
            if (engine(year).hasPendingChanges()) return true
        }
        return false
    }

    suspend fun discardUnreadable() {
        prepare()
        for (year in localYears()) {
            val engine = engine(year)
            if (!readable(engine)) engine.discardLocalDocument()
        }
    }

    private suspend fun prepare() {
        preparation.withLock {
            if (prepared) return
            val split = io { legacySplit.run() }
            if (split != null && split.wasSynchronized) io { pendingMerges.add(split.years.map(Int::toString)) }
            prepared = true
        }
    }

    private fun engine(year: Int): SyncEngine<CalendarPayload> = byYear.getOrPut(year) { engineOf(year) }

    private suspend fun importedIdentities(): Set<String> = io { importedClouds.load() }

    private suspend fun pendingYears(): Set<String> = io { pendingMerges.load() }

    private suspend fun localYears(): List<Int> = years(emptySet())

    private suspend fun years(cloudYears: Set<Int>): List<Int> =
        (io { files.years() } + byYear.keys + cloudYears + currentYear()).sorted()

    private suspend fun cloudYears(cloud: CloudGateway): Set<Int> =
        try {
            cloud.partitions(CalendarDocument.slot).mapNotNull { CalendarYears.yearOfPartition(it.partition) }.toSet()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptySet()
        }

    private suspend fun syncYear(year: Int, choice: FirstConnectionChoice?): SyncOutcome {
        val engine = engine(year)
        val outcome = engine.sync(choice)
        val merging = year.toString() in pendingYears()
        return when {
            merging && choice == null && outcome is SyncOutcome.NeedsFirstConnectionChoice -> mergeWithCloud(engine, year)
            merging && outcome in settled -> outcome.also { io { pendingMerges.remove(year.toString()) } }
            else -> outcome
        }
    }

    private suspend fun mergeWithCloud(engine: SyncEngine<CalendarPayload>, year: Int): SyncOutcome {
        val local = engine.load().entries
        val adopted = engine.sync(FirstConnectionChoice.USE_CLOUD)
        if (adopted !in settled) return adopted
        val missing = edits.missing(engine.load(), local)
        val outcome = if (missing.isEmpty()) SyncOutcome.Synchronized else addAndSync(engine, missing)
        if (outcome in settled) io { pendingMerges.remove(year.toString()) }
        return outcome
    }

    private suspend fun addAndSync(engine: SyncEngine<CalendarPayload>, entries: List<CalendarEntry>): SyncOutcome {
        engine.editLocally { edits.addMissing(it, entries) }
        return engine.sync()
    }

    private suspend fun importLegacyCloud(cloud: CloudGateway): List<SyncOutcome> =
        try {
            val identity = cloud.info().identity
            if (identity in importedIdentities()) {
                emptyList()
            } else {
                val whole = cloud.read(CalendarDocument.slot)
                val outcomes = mutableListOf<SyncOutcome>()
                val imported = if (whole == null) emptyList() else entriesOf(whole)
                for ((year, entries) in CalendarYears.split(imported, currentYear())) {
                    val engine = engine(year)
                    val missing = edits.missing(engine.load(), entries)
                    if (missing.isNotEmpty()) outcomes += addAndSync(engine, missing)
                }
                io { importedClouds.add(listOf(identity)) }
                outcomes
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }

    private fun entriesOf(document: RemoteDocument): List<CalendarEntry> =
        try {
            CalendarDocument.codec.decode(DocumentEnvelope.parse(document.content)).entries
        } catch (e: EnvelopeFormatException) {
            emptyList()
        } catch (e: DocumentDecodeException) {
            emptyList()
        }

    private suspend fun readable(engine: SyncEngine<CalendarPayload>): Boolean =
        try {
            engine.load()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }

    private suspend fun <R> syncing(block: suspend () -> R): R {
        activeSyncs.incrementAndGet()
        mutableSyncing.value = true
        try {
            return block()
        } finally {
            if (activeSyncs.decrementAndGet() == 0) mutableSyncing.value = false
        }
    }

    private suspend fun <R> io(block: suspend () -> R): R = withContext(Dispatchers.IO) { block() }

    private fun currentYear(): Int = clock.instant().atZone(ZoneOffset.UTC).year

    private companion object {
        val settled = setOf(
            SyncOutcome.Synchronized,
            SyncOutcome.ServerWon,
            SyncOutcome.LocalWon,
            SyncOutcome.KeptLocalUnsynced
        )
    }
}
