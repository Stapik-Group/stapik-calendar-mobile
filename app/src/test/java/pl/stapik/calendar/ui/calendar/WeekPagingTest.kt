package pl.stapik.calendar.ui.calendar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekPagingTest {
    private val wednesday = LocalDate.of(2026, 3, 4)
    private val paging = WeekPaging(wednesday)

    @Test
    fun theInitialPageIsTheWeekOfTheAnchorDay() {
        assertEquals(LocalDate.of(2026, 3, 2), paging.pageToWeekStart(WeekPaging.INITIAL_PAGE))
    }

    @Test
    fun pagesAreWeeksApart() {
        assertEquals(LocalDate.of(2026, 3, 9), paging.pageToWeekStart(WeekPaging.INITIAL_PAGE + 1))
        assertEquals(LocalDate.of(2026, 2, 23), paging.pageToWeekStart(WeekPaging.INITIAL_PAGE - 1))
    }

    @Test
    fun anyDayOfAWeekLeadsToThePageOfThatWeek() {
        assertEquals(WeekPaging.INITIAL_PAGE, paging.weekStartToPage(LocalDate.of(2026, 3, 8)))
        assertEquals(WeekPaging.INITIAL_PAGE + 1, paging.weekStartToPage(LocalDate.of(2026, 3, 9)))
    }

    @Test
    fun theAnchorCanBeAnyDayOfItsWeek() {
        assertEquals(paging.pageToWeekStart(10), WeekPaging(LocalDate.of(2026, 3, 8)).pageToWeekStart(10))
    }

    @Test
    fun aLaterAnchorMovesTheSameWeekToAnotherPage() {
        val nextWeek = WeekPaging(wednesday.plusWeeks(1))

        assertEquals(WeekPaging.INITIAL_PAGE - 1, nextWeek.weekStartToPage(wednesday))
    }
}
