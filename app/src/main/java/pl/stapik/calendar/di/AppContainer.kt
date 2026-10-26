package pl.stapik.calendar.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.time.Clock
import pl.stapik.calendar.data.cache.CalendarCacheMigration
import pl.stapik.calendar.data.cache.DataStoreCalendarCacheStorage
import pl.stapik.calendar.data.cloud.CalendarAccess
import pl.stapik.calendar.data.cloud.CloudGatewayProvider
import pl.stapik.calendar.data.config.ApiConfigStorage
import pl.stapik.calendar.data.config.ConnectionApiConfigStorage
import pl.stapik.calendar.data.document.ConflictCopies
import pl.stapik.calendar.data.document.MigratingDocumentStore
import pl.stapik.calendar.data.model.CalendarDocument
import pl.stapik.calendar.data.model.CalendarPayload
import pl.stapik.calendar.data.sync.CalendarSyncWorker
import pl.stapik.calendar.ui.calendar.CalendarViewModelFactory
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.cloud.HttpCloudGateway
import pl.stapik.common.cloud.OkHttpTransport
import pl.stapik.common.crypto.KeystoreCiphers
import pl.stapik.common.document.FileDocumentStore
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

    private val fileStore: FileDocumentStore by lazy {
        FileDocumentStore(
            directory = File(appContext.filesDir, DOCUMENTS_DIRECTORY).toPath(),
            slot = CalendarDocument.slot,
            cipher = KeystoreCiphers.create(DOCUMENT_KEY_ALIAS, CalendarDocument.slot.value.toByteArray())
        )
    }

    val conflictCopies: ConflictCopies by lazy { ConflictCopies(fileStore::listConflictCopies) }

    val calendarEngine: SyncEngine<CalendarPayload> by lazy {
        SyncEngine(
            slot = CalendarDocument.slot,
            codec = CalendarDocument.codec,
            store = MigratingDocumentStore(delegate = fileStore, migration = CalendarCacheMigration(legacyCache)),
            cloudProvider = { gatewayProvider.current() },
            clock = Clock.systemUTC()
        )
    }

    val calendarViewModelFactory: ViewModelProvider.Factory by lazy {
        CalendarViewModelFactory(
            engine = calendarEngine,
            access = calendarAccess,
            retry = WorkManagerSyncRetry(appContext, CalendarSyncWorker::class.java),
            connected = { configStorage.load() != null }
        )
    }

    /** Builds the controller of the connect form, prefilled with the saved connection. */
    fun newConnectController(initial: CloudConfig?): ConnectController =
        ConnectController(
            store = connectionStorage.store,
            gatewayFactory = { config -> HttpCloudGateway(config, transport) },
            documents = listOf(calendarEngine),
            initial = initial
        )

    private companion object {
        const val DOCUMENTS_DIRECTORY = "stapik/documents"
        const val DOCUMENT_KEY_ALIAS = "stapikcalendar_document_key"
    }
}
