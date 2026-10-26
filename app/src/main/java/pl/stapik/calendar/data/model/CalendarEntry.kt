package pl.stapik.calendar.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable(with = CalendarEntrySerializer::class)
data class CalendarEntry(
    val date: String,
    val name: String,
    val link: String = "",
    val color: String = "default",
    val unknownFields: JsonObject = JsonObject(emptyMap())
)
