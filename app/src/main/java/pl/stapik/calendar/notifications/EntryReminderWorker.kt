package pl.stapik.calendar.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import pl.stapik.calendar.R
import pl.stapik.calendar.appContainer
import pl.stapik.calendar.data.notifications.DataStoreNotificationPreferencesStorage
import pl.stapik.calendar.data.notifications.DataStoreNotifiedEntriesStorage
import pl.stapik.calendar.ui.widget.CalendarWidget

class EntryReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val preferencesStorage = DataStoreNotificationPreferencesStorage(applicationContext)
        if (!preferencesStorage.enabled.first()) return Result.success()

        val book = applicationContext.appContainer.calendarBook
        runCatching { book.sync() }

        val entries = try {
            book.load().entries
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure()
        }

        CalendarWidget().updateAll(applicationContext)

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val notifiedStorage = DataStoreNotifiedEntriesStorage(applicationContext)
        val alreadyNotified = notifiedStorage.load()
        val notifiedNow = mutableSetOf<String>()

        entries.forEach { entry ->
            val entryDate = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: return@forEach
            val isToday = entryDate == today
            if (!isToday && entryDate != tomorrow) return@forEach

            val key = "${if (isToday) "today" else "tomorrow"}|${entry.date}|${entry.name}"
            if (key !in alreadyNotified) {
                notify(entry.name, isToday = isToday)
            }
            notifiedNow += key
        }

        notifiedStorage.save(notifiedNow)

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