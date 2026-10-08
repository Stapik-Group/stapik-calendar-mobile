package pl.stapik.calendar.data.repository

import pl.stapik.calendar.data.cache.CachedCalendar

sealed interface CalendarFetchOutcome {
    data class Fresh(val result: CalendarFetchResult) : CalendarFetchOutcome
    data class Cached(val cached: CachedCalendar, val cause: Throwable) : CalendarFetchOutcome
    data class LocalOnly(val cached: CachedCalendar) : CalendarFetchOutcome
    data class Conflict(val local: CachedCalendar, val server: CalendarFetchResult) : CalendarFetchOutcome
    data class Failure(val cause: Throwable) : CalendarFetchOutcome
}
