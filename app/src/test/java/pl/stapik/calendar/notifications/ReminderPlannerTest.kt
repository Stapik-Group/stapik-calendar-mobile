package pl.stapik.calendar.notifications

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.stapik.calendar.data.model.CalendarEntry

class ReminderPlannerTest {
    private val today = LocalDate.of(2026, 3, 1)

    private val dentist = CalendarEntry("dentist", "2026-03-01", "Dentist")
    private val gym = CalendarEntry("gym", "2026-03-02", "Gym")
    private val later = CalendarEntry("later", "2026-03-05", "Later")
    private val broken = CalendarEntry("broken", "not a date", "Broken")

    @Test
    fun remindsAboutTodayAndTomorrowOnly() {
        val plan = ReminderPlanner.plan(listOf(dentist, gym, later, broken), today, emptySet())

        assertEquals(listOf(DueReminder(dentist, ReminderDay.TODAY), DueReminder(gym, ReminderDay.TOMORROW)), plan.toNotify)
        assertEquals(setOf("today|2026-03-01|dentist", "tomorrow|2026-03-02|gym"), plan.notifiedKeys)
    }

    @Test
    fun doesNotRepeatAReminderThatWasAlreadySent() {
        val plan = ReminderPlanner.plan(listOf(dentist, gym), today, setOf("today|2026-03-01|dentist"))

        assertEquals(listOf(DueReminder(gym, ReminderDay.TOMORROW)), plan.toNotify)
        assertEquals(setOf("today|2026-03-01|dentist", "tomorrow|2026-03-02|gym"), plan.notifiedKeys)
    }

    @Test
    fun remindsSeparatelyAboutEntriesWithTheSameNameOnTheSameDay() {
        val first = dentist.copy(id = "first")
        val second = dentist.copy(id = "second")

        val plan = ReminderPlanner.plan(listOf(first, second), today, setOf("today|2026-03-01|first"))

        assertEquals(listOf(DueReminder(second, ReminderDay.TODAY)), plan.toNotify)
        assertEquals(2, plan.notifiedKeys.size)
    }

    @Test
    fun acceptsTheNameBasedKeyOfAnEarlierVersionAndRewritesItWithTheId() {
        val plan = ReminderPlanner.plan(listOf(dentist), today, setOf("today|2026-03-01|Dentist"))

        assertEquals(emptyList<DueReminder>(), plan.toNotify)
        assertEquals(setOf("today|2026-03-01|dentist"), plan.notifiedKeys)
    }

    @Test
    fun forgetsKeysOfEntriesThatAreNoLongerDue() {
        val plan = ReminderPlanner.plan(listOf(later), today, setOf("today|2026-03-01|dentist"))

        assertEquals(emptySet<String>(), plan.notifiedKeys)
    }

    @Test
    fun remindsAgainWhenAnEntryMovesToAnotherDay() {
        val moved = dentist.copy(date = "2026-03-02")

        val plan = ReminderPlanner.plan(listOf(moved), today, setOf("today|2026-03-01|dentist"))

        assertEquals(listOf(DueReminder(moved, ReminderDay.TOMORROW)), plan.toNotify)
    }
}
