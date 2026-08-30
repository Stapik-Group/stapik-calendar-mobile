package pl.stapik.calendar.data.cache

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.stapik.calendar.data.model.CalendarEntry

private val Context.calendarCacheDataStore by preferencesDataStore(name = "calendar_cache")

@Serializable
private data class CachedCalendarPayload(val entries: List<CalendarEntry>, val updatedAt: String)

class DataStoreCalendarCacheStorage(private val context: Context) : CalendarCacheStorage {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load(): CachedCalendar? {
        val raw = context.calendarCacheDataStore.data.first()[KEY_PAYLOAD] ?: return null
        // A corrupted or pre-migration payload should not crash the app, just act as no cache.
        return runCatching { json.decodeFromString<CachedCalendarPayload>(raw) }
            .getOrNull()
            ?.let { CachedCalendar(entries = it.entries, updatedAt = it.updatedAt) }
    }

    override suspend fun save(entries: List<CalendarEntry>, updatedAt: String) {
        val payload = json.encodeToString(CachedCalendarPayload(entries = entries, updatedAt = updatedAt))
        context.calendarCacheDataStore.edit { it[KEY_PAYLOAD] = payload }
    }

    private companion object {
        val KEY_PAYLOAD = stringPreferencesKey("cached_calendar")
    }
}