package pl.stapik.calendar.data.cache

import pl.stapik.calendar.data.model.CalendarEntry

interface CalendarCacheStorage {
    suspend fun load(): CachedCalendar?
    suspend fun save(calendar: CachedCalendar)
}

data class CachedCalendar(
    val entries: List<CalendarEntry>,
    val updatedAt: String,
    val dirty: Boolean = false,
    val scope: String? = null,
    val modifiedAt: String? = null
)
