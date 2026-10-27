package pl.stapik.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CalendarPayload(
    @Serializable(with = CalendarEntriesSerializer::class) val entries: List<CalendarEntry> = emptyList(),
    val lastUpdate: String = ""
)
