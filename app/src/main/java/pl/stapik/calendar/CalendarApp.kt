package pl.stapik.calendar

import android.app.Application
import android.content.Context
import pl.stapik.calendar.di.AppContainer

class CalendarApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** The app's shared objects; works from workers and receivers too, which have no Activity. */
val Context.appContainer: AppContainer
    get() = (applicationContext as CalendarApp).container
