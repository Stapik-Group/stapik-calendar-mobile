package pl.stapik.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CalendarSyncEnvelope(val lastUpdate: String, val payload: List<CalendarEntry>)