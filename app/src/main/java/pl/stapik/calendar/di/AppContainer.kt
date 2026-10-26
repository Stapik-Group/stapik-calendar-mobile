package pl.stapik.calendar.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.nio.file.Path
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import pl.stapik.calendar.data.cache.CalendarCacheMigration
import pl.stapik.calendar.data.cache.DataStoreCalendarCacheStorage
import pl.stapik.calendar.data.cloud.CalendarAccess
import pl.stapik.calendar.data.cloud.CloudGatewayProvider
import pl.stapik.calendar.data.config.ApiConfigStorage
import pl.stapik.calendar.data.config.ConnectionApiConfigStorage
import pl.stapik.calendar.data.document.CalendarBook
import pl.stapik.calendar.data.document.CalendarYearFiles
import pl.stapik.calendar.data.document.ConflictCopies
import pl.stapik.calendar.data.document.LegacyCalendarSplit
import pl.stapik.calendar.data.document.LineSetFile
import pl.stapik.calendar.data.document.MigratingDocumentStore
import pl.stapik.calendar.data.model.CalendarDocument
import pl.stapik.calendar.data.sync.CalendarSyncWorker
import pl.stapik.calendar.ui.calendar.CalendarViewModelFactory
import pl.stapik.calendar.ui.conflicts.ConflictCopiesViewModelFactory
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.cloud.HttpCloudGateway
import pl.stapik.common.cloud.OkHttpTransport
import pl.stapik.common.crypto.KeystoreCiphers
import pl.stapik.common.document.FileDocumentStore
import pl.stapik.common.model.SlotKey
import pl.stapik.common.presentation.ConnectController
import pl.stapik.common.sync.SyncEngine
import pl.stapik.common.work.WorkManagerSyncRetry

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val connectionStorage = ConnectionApiConfigStorage(appContext)
    private val transport = OkHttpTransport()
    private val legacyCache = DataStoreCalendarCacheStorage(appContext)

    /** The one connection storage of the process: the Activity, the widget and the reminders share it. */
    val configStorage: ApiConfigStorage = connectionStorage

    private val gatewayProvider = CloudGatewayProvider(configStorage, transport)

    val calendarAccess = CalendarAccess { gatewayProvider.current() }

    private val documentsDirectory: Path = File(appContext.filesDir, DOCUMENTS_DIRECTORY).toPath()
    private val yearFiles = CalendarYearFiles(documentsDirectory)
    private val yearStores = ConcurrentHashMap<Int, FileDocumentStore>()

    private val wholeStore: FileDocumentStore by lazy { storeFor(CalendarDocument.slot) }

    private fun yearStore(year: Int): FileDocumentStore =
        yearStores.getOrPut(year) { storeFor(CalendarDocument.slotOf(year)) }

    private fun storeFor(slot: SlotKey): FileDocumentStore =
        FileDocumentStore(
            directory = documentsDirectory,
            slot = slot,
            cipher = KeystoreCiphers.create(DOCUMENT_KEY_ALIAS, slot.toString().toByteArray())
        )

    val conflictCopies: ConflictCopies by lazy {
        ConflictCopies {
            (listOf(wholeStore) + yearFiles.years().map(::yearStore))
                .flatMap { it.listConflictCopies() }
                .sortedByDescending { it.createdAt }
        }
    }

    val calendarBook: CalendarBook by lazy {
        val clock = Clock.systemUTC()
        CalendarBook(
            engineOf = { year ->
                SyncEngine(
                    slot = CalendarDocument.slotOf(year),
                    codec = CalendarDocument.codec,
                    store = yearStore(year),
                    cloudProvider = { gatewayProvider.current() },
                    clock = clock
                )
            },
            files = yearFiles,
            legacySplit = LegacyCalendarSplit(
                legacy = MigratingDocumentStore(delegate = wholeStore, migration = CalendarCacheMigration(legacyCache)),
                storeOf = ::yearStore,
                clock = clock
            ),
            gateway = { gatewayProvider.current() },
            pendingMerges = LineSetFile(documentsDirectory.resolve(PENDING_MERGES_FILE)),
            importedClouds = LineSetFile(documentsDirectory.resolve(IMPORTED_CLOUDS_FILE)),
            clock = clock
        )
    }

    val calendarViewModelFactory: ViewModelProvider.Factory by lazy {
        CalendarViewModelFactory(
            book = calendarBook,
            access = calendarAccess,
            retry = WorkManagerSyncRetry(appContext, CalendarSyncWorker::class.java),
            connected = { configStorage.load() != null }
        )
    }

    val conflictCopiesViewModelFactory: ViewModelProvider.Factory by lazy {
        ConflictCopiesViewModelFactory(calendarBook, conflictCopies)
    }

    /** Builds the controller of the connect form, prefilled with the saved connection. */
    fun newConnectController(initial: CloudConfig?): ConnectController =
        ConnectController(
            store = connectionStorage.store,
            gatewayFactory = { config -> HttpCloudGateway(config, transport) },
            documents = calendarBook.engines(),
            initial = initial
        )

    private companion object {
        const val DOCUMENTS_DIRECTORY = "stapik/documents"
        const val DOCUMENT_KEY_ALIAS = "stapikcalendar_document_key"
        const val PENDING_MERGES_FILE = "pending-merges"
        const val IMPORTED_CLOUDS_FILE = "imported-clouds"
    }
}
