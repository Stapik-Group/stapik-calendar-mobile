package pl.stapik.calendar.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object CalendarEntrySerializer : KSerializer<CalendarEntry> {
    private const val DATE = "date"
    private const val NAME = "name"
    private const val LINK = "link"
    private const val COLOR = "color"
    private const val DEFAULT_LINK = ""
    private const val DEFAULT_COLOR = "default"

    private val knownFields = setOf(DATE, NAME, LINK, COLOR)

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CalendarEntry")

    override fun deserialize(decoder: Decoder): CalendarEntry {
        val json = decoder as? JsonDecoder ?: throw SerializationException("A calendar entry can only be read from JSON")
        val fields = json.decodeJsonElement() as? JsonObject ?: throw SerializationException("A calendar entry is not an object")
        return CalendarEntry(
            date = fields.requiredText(DATE),
            name = fields.requiredText(NAME),
            link = fields.optionalText(LINK, DEFAULT_LINK),
            color = fields.optionalText(COLOR, DEFAULT_COLOR),
            unknownFields = JsonObject(fields.filterKeys { it !in knownFields })
        )
    }

    override fun serialize(encoder: Encoder, value: CalendarEntry) {
        val json = encoder as? JsonEncoder ?: throw SerializationException("A calendar entry can only be written as JSON")
        val known = mapOf(
            DATE to JsonPrimitive(value.date),
            NAME to JsonPrimitive(value.name),
            LINK to JsonPrimitive(value.link),
            COLOR to JsonPrimitive(value.color)
        )
        json.encodeJsonElement(JsonObject(known + value.unknownFields.filterKeys { it !in knownFields }))
    }

    private fun JsonObject.requiredText(key: String): String =
        text(key) ?: throw SerializationException("A calendar entry has no '$key'")

    private fun JsonObject.optionalText(key: String, default: String): String =
        if (this[key] == null || this[key] == JsonNull) default else requiredText(key)

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
}
