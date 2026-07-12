package pl.stapik.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CalendarEntry (
    val date: String, // ISO 8601 yyyy-MM-dd
    val name: String,
    val link: String = ""
)