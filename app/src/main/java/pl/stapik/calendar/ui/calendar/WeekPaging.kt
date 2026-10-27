package pl.stapik.calendar.ui.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun LocalDate.toMonday(): LocalDate =
    minusDays((dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

class WeekPaging(anchorDay: LocalDate) {
    private val anchorWeekStart: LocalDate = anchorDay.toMonday()

    fun pageToWeekStart(page: Int): LocalDate =
        anchorWeekStart.plusWeeks((page - INITIAL_PAGE).toLong())

    fun weekStartToPage(weekStart: LocalDate): Int =
        INITIAL_PAGE + ChronoUnit.WEEKS.between(anchorWeekStart, weekStart.toMonday()).toInt()

    companion object {
        const val PAGE_COUNT = 100_000
        const val INITIAL_PAGE = PAGE_COUNT / 2
    }
}
