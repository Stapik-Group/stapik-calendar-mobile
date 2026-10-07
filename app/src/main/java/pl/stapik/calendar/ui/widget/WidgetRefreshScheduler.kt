package pl.stapik.calendar.ui.widget

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

object WidgetRefreshScheduler {
    private const val PERIODIC_WORK_NAME = "widget_daily_refresh"
    private const val IMMEDIATE_WORK_NAME = "widget_immediate_refresh"
    private val TARGET_TIME: LocalTime = LocalTime.of(0, 5)

    fun ensureScheduled(context: Context) {
        val now = LocalDateTime.now()
        val nextTarget = now.toLocalDate().atTime(TARGET_TIME)
            .let { if (it.isAfter(now)) it else it.plusDays(1) }

        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(Duration.ofDays(1))
            .setInitialDelay(Duration.between(now, nextTarget))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun refreshNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }
}
