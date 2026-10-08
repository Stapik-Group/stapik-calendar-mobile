package pl.stapik.calendar.ui.widget

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime

object WidgetRefreshScheduler {
    const val KEY_CHAIN = "chain"

    private const val CHAIN_WORK_NAME = "widget_refresh_chain"
    private const val LEGACY_PERIODIC_WORK_NAME = "widget_daily_refresh"
    private const val IMMEDIATE_WORK_NAME = "widget_immediate_refresh"
    private const val INTERVAL_HOURS = 3L
    private const val MINUTE_OFFSET = 5

    fun ensureScheduled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_PERIODIC_WORK_NAME)
        enqueueNext(context, ExistingWorkPolicy.KEEP)
    }

    fun scheduleNext(context: Context) {
        enqueueNext(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    fun refreshNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(CHAIN_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_PERIODIC_WORK_NAME)
    }

    private fun enqueueNext(context: Context, policy: ExistingWorkPolicy) {
        val now = LocalDateTime.now()
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setInitialDelay(Duration.between(now, nextSlot(now)))
            .setInputData(workDataOf(KEY_CHAIN to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(CHAIN_WORK_NAME, policy, request)
    }

    private fun nextSlot(now: LocalDateTime): LocalDateTime {
        val today = now.toLocalDate()
        return (0L until 24L step INTERVAL_HOURS)
            .map { today.atTime(it.toInt(), MINUTE_OFFSET) }
            .firstOrNull { it.isAfter(now.plusMinutes(1)) }
            ?: today.plusDays(1).atTime(0, MINUTE_OFFSET)
    }
}
