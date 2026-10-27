package pl.stapik.calendar.ui.calendar

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DayBoundaryTest {
    private val warsaw = ZoneId.of("Europe/Warsaw")

    @Test
    fun waitsUntilJustAfterTheNextMidnight() {
        val now = ZonedDateTime.of(2026, 3, 4, 23, 59, 0, 0, warsaw)

        assertEquals(60_500L, DayBoundary.millisUntilNextDay(now))
    }

    @Test
    fun countsAFullDayFromMidnight() {
        val now = ZonedDateTime.of(2026, 3, 4, 0, 0, 0, 0, warsaw)

        assertEquals(24 * 3_600_000L + 500L, DayBoundary.millisUntilNextDay(now))
    }

    @Test
    fun followsTheShorterDayWhenTheClocksGoForward() {
        val now = ZonedDateTime.of(2026, 3, 29, 0, 0, 0, 0, warsaw)

        assertEquals(23 * 3_600_000L + 500L, DayBoundary.millisUntilNextDay(now))
    }

    @Test
    fun followsTheLongerDayWhenTheClocksGoBack() {
        val now = ZonedDateTime.of(2026, 10, 25, 0, 0, 0, 0, warsaw)

        assertEquals(25 * 3_600_000L + 500L, DayBoundary.millisUntilNextDay(now))
    }
}
