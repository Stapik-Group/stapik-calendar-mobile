package pl.stapik.calendar.data.notifications

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationPrefsDataStore by preferencesDataStore(name = "notification_prefs")

interface NotificationPreferencesStorage {
    val enabled: Flow<Boolean>
    suspend fun setEnabled(enabled: Boolean)
}

class DataStoreNotificationPreferencesStorage(private val context: Context) : NotificationPreferencesStorage {
    override val enabled: Flow<Boolean> = context.notificationPrefsDataStore.data.map { prefs ->
        prefs[KEY_ENABLED] ?: false
    }

    override suspend fun setEnabled(enabled: Boolean) {
        context.notificationPrefsDataStore.edit { it[KEY_ENABLED] = enabled }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("entry_reminders_enabled")
    }
}