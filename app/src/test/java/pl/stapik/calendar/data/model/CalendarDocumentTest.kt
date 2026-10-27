package pl.stapik.calendar.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
            "entries": [{"id": "dentist", "date": "2026-01-02", "name": "Dentist", "link": "", "color": "red"}],
            "payloadExtra": {"flag": true}
          }
        }
    """.trimIndent()

    @Test
    fun decodesTheEntriesOfThePayload() {
        val payload = codec.decode(DocumentEnvelope.parse(stored))

        assertEquals(listOf(CalendarEntry("dentist", "2026-01-02", "Dentist", "", "red")), payload.entries)
    }

    @Test
    fun encodingKeepsTheFieldsOfEntriesThatThePhoneDoesNotKnow() {
        val previous = DocumentEnvelope.parse(
            """
            {
              "lastUpdate": "2026-01-01T10:00:00Z",
              "payload": {
                "lastUpdate": "2026-01-01T10:00:00Z",
                "entries": [{"id": "dentist", "date": "2026-01-02", "name": "Dentist", "link": "", "color": "red", "location": "Clinic", "time": "09:30", "reminder": 7}]
              }
            }
            """.trimIndent()
        )
        val decoded = codec.decode(previous)

        val result = Json.parseToJsonElement(codec.encode(decoded, previous, "2026-02-01T00:00:00Z").toJson()).jsonObject
        val entry = result.getValue("payload").jsonObject.getValue("entries").jsonArray.single().jsonObject

        assertEquals("Clinic", entry.getValue("location").jsonPrimitive.content)
        assertEquals("09:30", entry.getValue("time").jsonPrimitive.content)
        assertEquals(7, entry.getValue("reminder").jsonPrimitive.int)
        assertEquals("dentist", entry.getValue("id").jsonPrimitive.content)
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
        val edited = codec.decode(previous).let { it.copy(entries = it.entries + CalendarEntry("gym", "2026-01-03", "Gym")) }

        val result = Json.parseToJsonElement(codec.encode(edited, previous, "2026-02-01T00:00:00Z").toJson()).jsonObject

        assertEquals(7, result.getValue("envelopeExtra").jsonPrimitive.int)
        assertEquals("2026-02-01T00:00:00Z", result.getValue("lastUpdate").jsonPrimitive.content)
        val payload = result.getValue("payload").jsonObject
        assertTrue("payloadExtra" in payload)
        assertEquals(2, payload.getValue("entries").jsonArray.size)
    }

    @Test
    fun encodingWritesTheCurrentSchemaVersionIntoThePayload() {
        val result = Json.parseToJsonElement(
            codec.encode(CalendarPayload(), DocumentEnvelope.empty(""), "2026-02-01T00:00:00Z").toJson()
        ).jsonObject

        assertEquals(CalendarDocument.SCHEMA_VERSION, result.getValue("payload").jsonObject.getValue("schemaVersion").jsonPrimitive.int)
    }

    @Test
    fun encodingKeepsTheSchemaVersionOfADocumentThatIsNewer() {
        val newer = DocumentEnvelope.parse("""{"lastUpdate":"","payload":{"schemaVersion":5,"entries":[]}}""")

        val result = Json.parseToJsonElement(codec.encode(codec.decode(newer), newer, "2026-02-01T00:00:00Z").toJson()).jsonObject

        assertEquals(5, result.getValue("payload").jsonObject.getValue("schemaVersion").jsonPrimitive.int)
    }

    @Test
    fun theGateKnowsADocumentWithoutAVersionAsNotNewer() {
        assertNull(CalendarDocument.schemaGate.newerVersionOf(DocumentEnvelope.parse(stored)))
    }

    @Test
    fun theGateSeesADocumentOfANewerVersion() {
        val newer = DocumentEnvelope.parse("""{"lastUpdate":"","payload":{"schemaVersion":2}}""")

        assertEquals(2, CalendarDocument.schemaGate.newerVersionOf(newer))
    }

    @Test
    fun theGateAcceptsTheCurrentVersion() {
        val current = DocumentEnvelope.parse("""{"lastUpdate":"","payload":{"schemaVersion":${CalendarDocument.SCHEMA_VERSION}}}""")

        assertNull(CalendarDocument.schemaGate.newerVersionOf(current))
    }

    @Test
    fun theGateReadsAVersionWrittenAsText() {
        val text = DocumentEnvelope.parse("""{"lastUpdate":"","payload":{"schemaVersion":"2"}}""")

        assertEquals(2, CalendarDocument.schemaGate.newerVersionOf(text))
    }

    @Test
    fun theGateIgnoresAVersionThatIsNotANumber() {
        val odd = DocumentEnvelope.parse("""{"lastUpdate":"","payload":{"schemaVersion":"abc"}}""")

        assertNull(CalendarDocument.schemaGate.newerVersionOf(odd))
    }
}
