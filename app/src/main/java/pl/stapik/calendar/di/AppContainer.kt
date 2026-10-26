package pl.stapik.calendar.di

import android.content.Context
import pl.stapik.calendar.data.cache.DataStoreCalendarCacheStorage
import pl.stapik.calendar.data.config.ApiConfigStorage
import pl.stapik.calendar.data.config.ConnectionApiConfigStorage
import pl.stapik.calendar.data.repository.CalendarRepository
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.cloud.HttpCloudGateway
import pl.stapik.common.cloud.OkHttpTransport
import pl.stapik.common.presentation.ConnectController

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val connectionStorage = ConnectionApiConfigStorage(appContext)
    private val transport = OkHttpTransport()

    /** The one connection storage of the process: the Activity, the widget and the reminders share it. */
    val configStorage: ApiConfigStorage = connectionStorage

    /** The one repository of the process: the screen, the widget and the reminders sync through it. */
    val calendarRepository = CalendarRepository(configStorage, DataStoreCalendarCacheStorage(appContext), transport)

    /** Builds the controller of the connect form, prefilled with the saved connection. */
    fun newConnectController(initial: CloudConfig?): ConnectController =
        ConnectController(
            store = connectionStorage.store,
            gatewayFactory = { config -> HttpCloudGateway(config, transport) },
            documents = emptyList(),
            initial = initial
        )
}
