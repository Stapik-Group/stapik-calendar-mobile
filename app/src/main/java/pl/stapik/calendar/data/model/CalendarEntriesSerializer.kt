package pl.stapik.calendar.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object CalendarEntriesSerializer : KSerializer<List<CalendarEntry>> {
    private val entries = ListSerializer(CalendarEntrySerializer)

    override val descriptor: SerialDescriptor = entries.descriptor

    override fun deserialize(decoder: Decoder): List<CalendarEntry> =
        EntryIds.withUniqueIds(decoder.decodeSerializableValue(entries))

    override fun serialize(encoder: Encoder, value: List<CalendarEntry>) =
        encoder.encodeSerializableValue(entries, value)
}
