package pl.stapik.calendar.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object NotificationScheduler {
    private const val LEGACY_WORK_NAME = "entry_reminder_check"
    private const val ALARM_REQUEST_CODE = 1
    private val TARGET_TIMES: List<LocalTime> = listOf(8, 11, 14, 17, 20).map { LocalTime.of(it, 0) }

    fun ensureScheduled(context: Context, from: LocalDateTime = LocalDateTime.now()) {
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_WORK_NAME)

        val today = from.toLocalDate()
        val nextTarget = TARGET_TIMES
            .map { today.atTime(it) }
            .firstOrNull { it.isAfter(from) }
            ?: today.plusDays(1).atTime(TARGET_TIMES.first())
        val triggerAtMillis = nextTarget.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        val pendingIntent = alarmIntent(context)

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_WORK_NAME)
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context))
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        Intent(context, ReminderAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
