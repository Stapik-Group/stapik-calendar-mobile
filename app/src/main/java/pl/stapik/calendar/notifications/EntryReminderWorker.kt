package pl.stapik.calendar.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import pl.stapik.calendar.R
import pl.stapik.calendar.data.cache.DataStoreCalendarCacheStorage
import pl.stapik.calendar.data.config.DataStoreApiConfigStorage
import pl.stapik.calendar.data.notifications.DataStoreNotificationPreferencesStorage
import pl.stapik.calendar.data.repository.CalendarFetchOutcome
import pl.stapik.calendar.data.repository.CalendarRepository

class EntryReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val preferencesStorage = DataStoreNotificationPreferencesStorage(applicationContext)
        if (!preferencesStorage.enabled.first()) return Result.success()

        val repository = CalendarRepository(
            apiConfigStorage = DataStoreApiConfigStorage(applicationContext),
            cacheStorage = DataStoreCalendarCacheStorage(applicationContext)
        )

        val entries = when (val outcome = repository.fetchEntries()) {
            is CalendarFetchOutcome.Fresh -> outcome.result.entries
            is CalendarFetchOutcome.Cached -> outcome.cached.entries
            is CalendarFetchOutcome.Failure -> return Result.retry()
        }

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        entries.forEach { entry ->
            val entryDate = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: return@forEach
            when (entryDate) {
                today -> notify(entry.name, isToday = true)
                tomorrow -> notify(entry.name, isToday = false)
                else -> Unit
            }
        }

        return Result.success()
    }

    private fun notify(entryName: String, isToday: Boolean) {
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val title = applicationContext.getString(
            if (isToday) R.string.notification_title_today else R.string.notification_title_tomorrow
        )
        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.ENTRY_REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(entryName)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(entryName.hashCode(), notification)
    }
}