package pl.stapik.calendar.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntryTimeTest {
    @Test
    fun anEmptyTextMeansNoTime() {
        assertEquals("", EntryTime.normalize(""))
        assertEquals("", EntryTime.normalize("   "))
    }

    @Test
    fun acceptsTheSpellingsOfTheDesktopApp() {
        assertEquals("09:30", EntryTime.normalize("9:30"))
        assertEquals("09:30", EntryTime.normalize("09:30"))
        assertEquals("09:30", EntryTime.normalize("9.30"))
        assertEquals("09:30", EntryTime.normalize("930"))
        assertEquals("09:30", EntryTime.normalize("0930"))
    }

    @Test
    fun ignoresSurroundingSpaces() {
        assertEquals("07:05", EntryTime.normalize(" 7:05 "))
    }

    @Test
    fun acceptsTheWholeDay() {
        assertEquals("00:00", EntryTime.normalize("0:00"))
        assertEquals("23:59", EntryTime.normalize("23:59"))
    }

    @Test
    fun rejectsAnythingThatIsNotATimeOfDay() {
        listOf("24:00", "12:60", "12:5", "abc", "12", "12345", "1:2:3", "-1:30", ":30", "9:", "9 30", "١٢:٣٠")
            .forEach { assertNull(it, EntryTime.normalize(it)) }
    }
}
