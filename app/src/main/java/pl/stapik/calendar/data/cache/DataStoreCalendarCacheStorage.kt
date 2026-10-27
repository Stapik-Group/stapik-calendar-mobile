package pl.stapik.calendar.data.cache

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.calendarCacheDataStore by preferencesDataStore(name = "calendar_cache")

class DataStoreCalendarCacheStorage(private val context: Context) : CalendarCacheStorage {
    override suspend fun load(): CachedCalendar? {
        val raw = context.calendarCacheDataStore.data.first()[KEY_PAYLOAD] ?: return null
        return when (val reading = LegacyCacheCodec.read(raw)) {
            is LegacyCacheReading.Intact -> reading.calendar
            is LegacyCacheReading.Salvaged -> reading.calendar.also { keepUnreadable(raw) }
            LegacyCacheReading.Unreadable -> null.also { keepUnreadable(raw) }
        }
    }

    override suspend fun save(calendar: CachedCalendar) {
        context.calendarCacheDataStore.edit { it[KEY_PAYLOAD] = LegacyCacheCodec.encode(calendar) }
    }

    override suspend fun clear() {
        context.calendarCacheDataStore.edit { it.remove(KEY_PAYLOAD) }
    }

    private suspend fun keepUnreadable(raw: String) {
        context.calendarCacheDataStore.edit { if (it[KEY_UNREADABLE] == null) it[KEY_UNREADABLE] = raw }
    }

    private companion object {
        val KEY_PAYLOAD = stringPreferencesKey("cached_calendar")
        val KEY_UNREADABLE = stringPreferencesKey("cached_calendar_unreadable")
    }
}
