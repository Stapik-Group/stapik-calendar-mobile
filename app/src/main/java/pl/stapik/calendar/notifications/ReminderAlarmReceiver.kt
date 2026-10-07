package pl.stapik.calendar.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        NotificationScheduler.ensureScheduled(appContext, LocalDateTime.now().plusMinutes(1))
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "entry_reminder_now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<EntryReminderWorker>().build()
        )
    }
}
