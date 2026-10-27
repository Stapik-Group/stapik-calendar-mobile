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

    private fun withId(id: String = "e1", name: String = "Dentist") = CalendarEntry(id, "2026-01-02", name)

    @Test
    fun readsTheKnownFields() {
        val entry = decode("""{"id":"e1","date":"2026-01-02","name":"Dentist","link":"https://x.test","color":"red"}""")

        assertEquals(CalendarEntry("e1", "2026-01-02", "Dentist", "https://x.test", "red"), entry)
    }

    @Test
    fun missingLinkAndColorFallBackToTheirDefaults() {
        assertEquals(withId(), decode("""{"id":"e1","date":"2026-01-02","name":"Dentist"}"""))
    }

    @Test
    fun nullLinkAndColorFallBackToTheirDefaults() {
        val entry = decode("""{"id":"e1","date":"2026-01-02","name":"Dentist","link":null,"color":null}""")

        assertEquals(withId(), entry)
    }

    @Test
    fun readsTheLocationAndTheTime() {
        val entry = decode("""{"id":"e1","date":"2026-01-02","name":"Dentist","location":"Clinic","time":"09:30"}""")

        assertEquals("Clinic", entry.location)
        assertEquals("09:30", entry.time)
        assertEquals(JsonObject(emptyMap()), entry.unknownFields)
    }

    @Test
    fun missingOrNullLocationAndTimeAreEmpty() {
        assertEquals(withId(), decode("""{"id":"e1","date":"2026-01-02","name":"Dentist"}"""))
        assertEquals(
            withId(),
            decode("""{"id":"e1","date":"2026-01-02","name":"Dentist","location":null,"time":null}""")
        )
    }

    @Test
    fun alwaysWritesTheLocationAndTheTime() {
        val written = json.encodeToJsonElement(CalendarEntrySerializer, withId()).jsonObject

        assertEquals("", written.getValue("location").jsonPrimitive.content)
        assertEquals("", written.getValue("time").jsonPrimitive.content)
    }

    @Test
    fun keepsTheFieldsOfOtherApps() {
        val entry = decode("""{"id":"e1","date":"2026-01-02","name":"Dentist","reminder":true}""")

        assertEquals(JsonObject(mapOf("reminder" to JsonPrimitive(true))), entry.unknownFields)
    }

    @Test
    fun writesTheFieldsOfOtherApps() {
        val entry = decode("""{"id":"e1","date":"2026-01-02","name":"Dentist","location":"Clinic","time":"09:30","reminder":true}""")

        val written = json.encodeToJsonElement(CalendarEntrySerializer, entry).jsonObject

        assertEquals("Clinic", written.getValue("location").jsonPrimitive.content)
        assertEquals("09:30", written.getValue("time").jsonPrimitive.content)
        assertEquals("e1", written.getValue("id").jsonPrimitive.content)
        assertEquals("true", written.getValue("reminder").jsonPrimitive.content)
        assertEquals("Dentist", written.getValue("name").jsonPrimitive.content)
    }

    @Test
    fun anEntryCannotHideItsKnownFieldsInTheOtherOnes() {
        val hidden = JsonObject(
            mapOf("name" to JsonPrimitive("Other"), "time" to JsonPrimitive("23:59"), "id" to JsonPrimitive("hijacked"))
        )
        val entry = CalendarEntry("e1", "2026-01-02", "Dentist", time = "09:30", unknownFields = hidden)

        val written = json.encodeToJsonElement(CalendarEntrySerializer, entry).jsonObject

        assertEquals("Dentist", written.getValue("name").jsonPrimitive.content)
        assertEquals("09:30", written.getValue("time").jsonPrimitive.content)
        assertEquals("e1", written.getValue("id").jsonPrimitive.content)
    }

    @Test
    fun readsTheIdAndDoesNotKeepItAmongTheOtherFields() {
        val entry = decode("""{"id":"abc","date":"2026-01-02","name":"Dentist"}""")

        assertEquals("abc", entry.id)
        assertEquals(JsonObject(emptyMap()), entry.unknownFields)
    }

    @Test
    fun writesTheId() {
        val written = json.encodeToJsonElement(CalendarEntrySerializer, withId("abc")).jsonObject

        assertEquals("abc", written.getValue("id").jsonPrimitive.content)
    }

    @Test
    fun anEntryWithoutAnIdGetsOneDerivedFromItsContent() {
        val first = decode("""{"date":"2026-01-02","name":"Dentist"}""")
        val second = decode("""{"date":"2026-01-02","name":"Dentist"}""")

        assertEquals(EntryIds.contentId(first), first.id)
        assertEquals(first.id, second.id)
    }

    @Test
    fun aBlankOrNonTextIdCountsAsMissing() {
        val expected = decode("""{"date":"2026-01-02","name":"Dentist"}""").id

        assertEquals(expected, decode("""{"id":"","date":"2026-01-02","name":"Dentist"}""").id)
        assertEquals(expected, decode("""{"id":7,"date":"2026-01-02","name":"Dentist"}""").id)
        assertEquals(expected, decode("""{"id":null,"date":"2026-01-02","name":"Dentist"}""").id)
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
