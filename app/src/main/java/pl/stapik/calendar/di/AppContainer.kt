package pl.stapik.calendar.di

import android.content.Context
import java.io.File
import java.time.Clock
import pl.stapik.calendar.data.cache.CalendarCacheMigration
import pl.stapik.calendar.data.cache.DataStoreCalendarCacheStorage
import pl.stapik.calendar.data.cloud.CloudGatewayProvider
import pl.stapik.calendar.data.config.ApiConfigStorage
import pl.stapik.calendar.data.config.ConnectionApiConfigStorage
import pl.stapik.calendar.data.document.MigratingDocumentStore
import pl.stapik.calendar.data.model.CalendarDocument
import pl.stapik.calendar.data.model.CalendarPayload
import pl.stapik.calendar.data.repository.CalendarRepository
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.cloud.HttpCloudGateway
import pl.stapik.common.cloud.OkHttpTransport
import pl.stapik.common.crypto.KeystoreCiphers
import pl.stapik.common.document.FileDocumentStore
import pl.stapik.common.presentation.ConnectController
import pl.stapik.common.sync.SyncEngine

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val connectionStorage = ConnectionApiConfigStorage(appContext)
    private val transport = OkHttpTransport()
    private val legacyCache = DataStoreCalendarCacheStorage(appContext)

    /** The one connection storage of the process: the Activity, the widget and the reminders share it. */
    val configStorage: ApiConfigStorage = connectionStorage

    /** The one repository of the process: the screen, the widget and the reminders sync through it. */
    val calendarRepository = CalendarRepository(configStorage, legacyCache, transport)

    private val gatewayProvider = CloudGatewayProvider(configStorage, transport)

    val calendarEngine: SyncEngine<CalendarPayload> by lazy {
        SyncEngine(
            slot = CalendarDocument.slot,
            codec = CalendarDocument.codec,
            store = MigratingDocumentStore(
                delegate = FileDocumentStore(
                    directory = File(appContext.filesDir, DOCUMENTS_DIRECTORY).toPath(),
                    slot = CalendarDocument.slot,
                    cipher = KeystoreCiphers.create(DOCUMENT_KEY_ALIAS, CalendarDocument.slot.value.toByteArray())
                ),
                migration = CalendarCacheMigration(legacyCache)
            ),
            cloudProvider = { gatewayProvider.current() },
            clock = Clock.systemUTC()
        )
    }

    /** Builds the controller of the connect form, prefilled with the saved connection. */
    fun newConnectController(initial: CloudConfig?): ConnectController =
        ConnectController(
            store = connectionStorage.store,
            gatewayFactory = { config -> HttpCloudGateway(config, transport) },
            documents = emptyList(),
            initial = initial
        )

    private companion object {
        const val DOCUMENTS_DIRECTORY = "stapik/documents"
        const val DOCUMENT_KEY_ALIAS = "stapikcalendar_document_key"
    }
}
