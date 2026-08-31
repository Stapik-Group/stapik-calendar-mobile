package pl.stapik.calendar.data.cache

import pl.stapik.calendar.data.model.CalendarEntry

interface CalendarCacheStorage {
    suspend fun load(): CachedCalendar?
    suspend fun save(entries: List<CalendarEntry>, updatedAt: String)
}

data class CachedCalendar(
    val entries: List<CalendarEntry>,
    val updatedAt: String
)