package pl.stapik.calendar.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.stapik.common.model.DocumentEnvelope

class CalendarDocumentTest {
    private val codec = CalendarDocument.codec

    private val stored = """
        {
          "lastUpdate": "2026-01-01T10:00:00Z",
          "envelopeExtra": 7,
          "payload": {
            "lastUpdate": "2026-01-01T10:00:00Z",
            "entries": [{"date": "2026-01-02", "name": "Dentist", "link": "", "color": "red"}],
            "payloadExtra": {"flag": true}
          }
        }
    """.trimIndent()

    @Test
    fun decodesTheEntriesOfThePayload() {
        val payload = codec.decode(DocumentEnvelope.parse(stored))

        assertEquals(listOf(CalendarEntry("2026-01-02", "Dentist", "", "red")), payload.entries)
    }

    @Test
    fun emptyEnvelopeDecodesToEmptyCalendar() {
        val payload = codec.decode(DocumentEnvelope.empty(""))

        assertTrue(payload.entries.isEmpty())
        assertTrue(codec.isEmpty(payload))
    }

    @Test
    fun encodingKeepsUnknownKeysOfEnvelopeAndPayload() {
        val previous = DocumentEnvelope.parse(stored)
        val edited = codec.decode(previous).let { it.copy(entries = it.entries + CalendarEntry("2026-01-03", "Gym")) }

        val result = Json.parseToJsonElement(codec.encode(edited, previous, "2026-02-01T00:00:00Z").toJson()).jsonObject

        assertEquals(7, result.getValue("envelopeExtra").jsonPrimitive.int)
        assertEquals("2026-02-01T00:00:00Z", result.getValue("lastUpdate").jsonPrimitive.content)
        val payload = result.getValue("payload").jsonObject
        assertTrue("payloadExtra" in payload)
        assertEquals(2, payload.getValue("entries").jsonArray.size)
    }
}
