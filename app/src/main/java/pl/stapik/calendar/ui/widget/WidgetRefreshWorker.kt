package pl.stapik.calendar.ui.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import pl.stapik.calendar.data.cache.DataStoreCalendarCacheStorage
import pl.stapik.calendar.data.config.DataStoreApiConfigStorage
import pl.stapik.calendar.data.repository.CalendarRepository

class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        runCatching {
            CalendarRepository(
                apiConfigStorage = DataStoreApiConfigStorage(applicationContext),
                cacheStorage = DataStoreCalendarCacheStorage(applicationContext)
            ).fetchEntries()
        }
        CalendarWidget().updateAll(applicationContext)

        if (inputData.getBoolean(WidgetRefreshScheduler.KEY_CHAIN, false)) {
            val hasWidgets = GlanceAppWidgetManager(applicationContext)
                .getGlanceIds(CalendarWidget::class.java)
                .isNotEmpty()
            if (hasWidgets) WidgetRefreshScheduler.scheduleNext(applicationContext)
        }
        return Result.success()
    }
}
