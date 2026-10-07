package pl.stapik.calendar.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.stapik.calendar.data.notifications.DataStoreNotificationPreferencesStorage

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (DataStoreNotificationPreferencesStorage(appContext).enabled.first()) {
                    NotificationScheduler.ensureScheduled(appContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
