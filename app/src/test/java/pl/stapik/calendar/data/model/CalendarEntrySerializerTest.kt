package pl.stapik.calendar.data.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CalendarEntrySerializerTest {
    private val json = Json

    private fun decode(text: String): CalendarEntry = json.decodeFromString(CalendarEntrySerializer, text)

    @Test
    fun readsTheKnownFields() {
        val entry = decode("""{"date":"2026-01-02","name":"Dentist","link":"https://x.test","color":"red"}""")

        assertEquals(CalendarEntry("2026-01-02", "Dentist", "https://x.test", "red"), entry)
    }

    @Test
    fun missingLinkAndColorFallBackToTheirDefaults() {
        assertEquals(CalendarEntry("2026-01-02", "Dentist", "", "default"), decode("""{"date":"2026-01-02","name":"Dentist"}"""))
    }

    @Test
    fun nullLinkAndColorFallBackToTheirDefaults() {
        val entry = decode("""{"date":"2026-01-02","name":"Dentist","link":null,"color":null}""")

        assertEquals(CalendarEntry("2026-01-02", "Dentist", "", "default"), entry)
    }

    @Test
    fun readsTheLocationAndTheTime() {
        val entry = decode("""{"date":"2026-01-02","name":"Dentist","location":"Clinic","time":"09:30"}""")

        assertEquals("Clinic", entry.location)
        assertEquals("09:30", entry.time)
        assertEquals(JsonObject(emptyMap()), entry.unknownFields)
    }

    @Test
    fun missingOrNullLocationAndTimeAreEmpty() {
        assertEquals(CalendarEntry("2026-01-02", "Dentist"), decode("""{"date":"2026-01-02","name":"Dentist"}"""))
        assertEquals(
            CalendarEntry("2026-01-02", "Dentist"),
            decode("""{"date":"2026-01-02","name":"Dentist","location":null,"time":null}""")
        )
    }

    @Test
    fun alwaysWritesTheLocationAndTheTime() {
        val written = json.encodeToJsonElement(CalendarEntrySerializer, CalendarEntry("2026-01-02", "Dentist")).jsonObject

        assertEquals("", written.getValue("location").jsonPrimitive.content)
        assertEquals("", written.getValue("time").jsonPrimitive.content)
    }

    @Test
    fun keepsTheFieldsOfOtherApps() {
        val entry = decode("""{"date":"2026-01-02","name":"Dentist","id":7,"reminder":true}""")

        assertEquals(JsonObject(mapOf("id" to JsonPrimitive(7), "reminder" to JsonPrimitive(true))), entry.unknownFields)
    }

    @Test
    fun writesTheFieldsOfOtherApps() {
        val entry = decode("""{"date":"2026-01-02","name":"Dentist","location":"Clinic","time":"09:30","id":7}""")

        val written = json.encodeToJsonElement(CalendarEntrySerializer, entry).jsonObject

        assertEquals("Clinic", written.getValue("location").jsonPrimitive.content)
        assertEquals("09:30", written.getValue("time").jsonPrimitive.content)
        assertEquals("7", written.getValue("id").jsonPrimitive.content)
        assertEquals("Dentist", written.getValue("name").jsonPrimitive.content)
    }

    @Test
    fun anEntryCannotHideItsKnownFieldsInTheOtherOnes() {
        val hidden = JsonObject(mapOf("name" to JsonPrimitive("Other"), "time" to JsonPrimitive("23:59")))
        val entry = CalendarEntry("2026-01-02", "Dentist", time = "09:30", unknownFields = hidden)

        val written = json.encodeToJsonElement(CalendarEntrySerializer, entry).jsonObject

        assertEquals("Dentist", written.getValue("name").jsonPrimitive.content)
        assertEquals("09:30", written.getValue("time").jsonPrimitive.content)
    }

    @Test
    fun anEntryWithoutANameIsRejected() {
        assertThrows(SerializationException::class.java) { decode("""{"date":"2026-01-02"}""") }
    }

    @Test
    fun anEntryThatIsNotAnObjectIsRejected() {
        assertThrows(SerializationException::class.java) { decode("5") }
    }

    @Test
    fun aDateThatIsNotTextIsRejected() {
        assertThrows(SerializationException::class.java) { decode("""{"date":20260102,"name":"Dentist"}""") }
    }
}
