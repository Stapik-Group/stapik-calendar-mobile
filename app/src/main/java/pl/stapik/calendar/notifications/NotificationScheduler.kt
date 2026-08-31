package pl.stapik.calendar.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

object NotificationScheduler {
    private const val UNIQUE_WORK_NAME = "entry_reminder_check"
    private val TARGET_HOUR: LocalTime = LocalTime.of(8, 0)

    // Called from AppRoot whenever the stored preference is true - KEEP policy
    // makes this a cheap no-op if the periodic work is already scheduled, so
    // it is safe to call unconditionally on every app launch.
    fun ensureScheduled(context: Context) {
        val now = LocalDateTime.now()
        val nextTarget = now.toLocalDate().atTime(TARGET_HOUR)
            .let { if (it.isAfter(now)) it else it.plusDays(1) }
        val initialDelay = Duration.between(now, nextTarget)

        val request = PeriodicWorkRequestBuilder<EntryReminderWorker>(Duration.ofDays(1))
            .setInitialDelay(initialDelay)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}