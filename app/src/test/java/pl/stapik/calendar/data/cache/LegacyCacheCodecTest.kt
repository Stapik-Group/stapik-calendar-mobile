package pl.stapik.calendar.data.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.stapik.calendar.data.model.CalendarEntry

class LegacyCacheCodecTest {
    private val dentist = CalendarEntry("dentist", "2026-01-02", "Dentist")
    private val intact = CachedCalendar(
        entries = listOf(dentist),
        updatedAt = "2026-01-01T10:00:00Z",
        dirty = true,
        scope = "READ_WRITE",
        modifiedAt = "2026-01-02T08:30:15Z"
    )

    private val damaged = """
        {"entries":[
          {"id":"dentist","date":"2026-01-02","name":"Dentist"},
          {"id":"broken","date":"2026-01-03"}
        ],"updatedAt":"2026-01-01T10:00:00Z","dirty":true,"scope":"READ_WRITE","modifiedAt":"2026-01-02T08:30:15Z"}
    """.trimIndent()

    @Test
    fun readsWhatWasWritten() {
        assertEquals(LegacyCacheReading.Intact(intact), LegacyCacheCodec.read(LegacyCacheCodec.encode(intact)))
    }

    @Test
    fun keepsTheEntriesThatCanStillBeReadWhenOneIsDamaged() {
        val reading = LegacyCacheCodec.read(damaged) as LegacyCacheReading.Salvaged

        assertEquals(listOf(dentist), reading.calendar.entries)
        assertEquals("2026-01-01T10:00:00Z", reading.calendar.updatedAt)
        assertEquals("READ_WRITE", reading.calendar.scope)
        assertEquals("2026-01-02T08:30:15Z", reading.calendar.modifiedAt)
    }

    @Test
    fun doesNotSendALossyCalendarToTheCloudAsUnsyncedChanges() {
        val reading = LegacyCacheCodec.read(damaged) as LegacyCacheReading.Salvaged

        assertFalse(reading.calendar.dirty)
    }

    @Test
    fun keepsTheUnsyncedMarkWhenNothingWasLost() {
        val reading = LegacyCacheCodec.read(
            """{"entries":[{"date":"2026-01-02","name":"Dentist","extra":1}],"updatedAt":"x","dirty":true,"unknown":"field"}"""
        )

        assertTrue((reading as LegacyCacheReading.Intact).calendar.dirty)
    }

    @Test
    fun salvagesWhenOnlyTheMetadataIsDamaged() {
        val reading = LegacyCacheCodec.read(
            """{"entries":[{"id":"dentist","date":"2026-01-02","name":"Dentist"}],"updatedAt":5,"dirty":"yes"}"""
        ) as LegacyCacheReading.Salvaged

        assertEquals(listOf(dentist), reading.calendar.entries)
        assertEquals("", reading.calendar.updatedAt)
        assertFalse(reading.calendar.dirty)
    }

    @Test
    fun textThatIsNotJsonIsUnreadable() {
        assertEquals(LegacyCacheReading.Unreadable, LegacyCacheCodec.read("{\"entries\":[{"))
    }

    @Test
    fun jsonWithoutAListOfEntriesIsUnreadable() {
        assertEquals(LegacyCacheReading.Unreadable, LegacyCacheCodec.read("""{"updatedAt":"x"}"""))
        assertEquals(LegacyCacheReading.Unreadable, LegacyCacheCodec.read("[]"))
    }
}
