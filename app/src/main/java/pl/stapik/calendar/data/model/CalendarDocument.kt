package pl.stapik.calendar.data.model

import pl.stapik.common.model.DocumentCodec
import pl.stapik.common.model.SlotKey

object CalendarDocument {
    val slot = SlotKey("calendar.json")

    val codec = DocumentCodec(CalendarPayload.serializer()) { payload -> payload.entries.isEmpty() }
}
