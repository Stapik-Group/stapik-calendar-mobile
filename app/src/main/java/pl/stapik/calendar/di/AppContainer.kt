package pl.stapik.calendar.di

import android.content.Context
import pl.stapik.calendar.data.config.ApiConfigStorage
import pl.stapik.calendar.data.config.ConnectionApiConfigStorage

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val connectionStorage = ConnectionApiConfigStorage(appContext)

    /** The one connection storage of the process: the Activity, the widget and the reminders share it. */
    val configStorage: ApiConfigStorage = connectionStorage
}
