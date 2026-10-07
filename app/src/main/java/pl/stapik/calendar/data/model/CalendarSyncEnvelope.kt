package pl.stapik.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CalendarSyncEnvelope(val lastUpdate: String, val payload: CalendarPayload)

@Serializable
data class CalendarPayload(val entries: List<CalendarEntry>, val lastUpdate: String = "")
