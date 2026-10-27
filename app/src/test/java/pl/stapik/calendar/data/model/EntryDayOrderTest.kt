package pl.stapik.calendar.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EntryDayOrderTest {
    private val late = CalendarEntry("late", "2026-01-02", "Late", time = "18:00")
    private val early = CalendarEntry("early", "2026-01-02", "Early", time = "08:15")
    private val first = CalendarEntry("first", "2026-01-02", "First")
    private val second = CalendarEntry("second", "2026-01-02", "Second")

    @Test
    fun entriesWithATimeComeFirstInTheOrderOfTheClock() {
        assertEquals(listOf(early, late, first, second), listOf(first, late, second, early).inDayOrder())
    }

    @Test
    fun entriesWithoutATimeKeepTheirOrder() {
        assertEquals(listOf(second, first), listOf(second, first).inDayOrder())
    }

    @Test
    fun entriesWithTheSameTimeKeepTheirOrder() {
        val a = late.copy(name = "A")
        val b = late.copy(name = "B")

        assertEquals(listOf(b, a), listOf(b, a).inDayOrder())
    }
}
