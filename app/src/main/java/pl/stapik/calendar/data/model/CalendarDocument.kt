package pl.stapik.calendar.data.model

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import pl.stapik.common.model.DocumentCodec
import pl.stapik.common.model.SchemaGate
import pl.stapik.common.model.SlotKey

object CalendarDocument {
    const val SCHEMA_VERSION = 1
    private const val SCHEMA_VERSION_KEY = "schemaVersion"

    val slot = SlotKey("calendar.json")

    val codec = DocumentCodec(CalendarPayload.serializer()) { payload -> payload.entries.isEmpty() }

    val schemaGate = SchemaGate(SCHEMA_VERSION) { envelope ->
        (envelope.payload[SCHEMA_VERSION_KEY] as? JsonPrimitive)?.intOrNull
    }

    fun slotOf(year: Int): SlotKey = slot.inPartition(CalendarYears.partitionOf(year))
}
