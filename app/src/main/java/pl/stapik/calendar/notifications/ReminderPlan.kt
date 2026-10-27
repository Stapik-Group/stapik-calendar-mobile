package pl.stapik.calendar.notifications

import java.time.LocalDate
import pl.stapik.calendar.data.model.CalendarEntry

enum class ReminderDay(val keyPart: String) {
    TODAY("today"),
    TOMORROW("tomorrow")
}

data class DueReminder(val entry: CalendarEntry, val day: ReminderDay)

data class ReminderPlan(val toNotify: List<DueReminder>, val notifiedKeys: Set<String>)

object ReminderPlanner {
    fun plan(entries: List<CalendarEntry>, today: LocalDate, alreadyNotified: Set<String>): ReminderPlan {
        val toNotify = mutableListOf<DueReminder>()
        val notifiedKeys = mutableSetOf<String>()
        entries.forEach { entry ->
            val day = dayOf(entry, today) ?: return@forEach
            val key = keyOf(entry, day)
            if (key !in alreadyNotified && legacyKeyOf(entry, day) !in alreadyNotified) {
                toNotify += DueReminder(entry, day)
            }
            notifiedKeys += key
        }
        return ReminderPlan(toNotify, notifiedKeys)
    }

    fun keyOf(entry: CalendarEntry, day: ReminderDay): String = "${day.keyPart}|${entry.date}|${entry.id}"

    private fun legacyKeyOf(entry: CalendarEntry, day: ReminderDay): String = "${day.keyPart}|${entry.date}|${entry.name}"

    private fun dayOf(entry: CalendarEntry, today: LocalDate): ReminderDay? =
        when (runCatching { LocalDate.parse(entry.date) }.getOrNull()) {
            today -> ReminderDay.TODAY
            today.plusDays(1) -> ReminderDay.TOMORROW
            else -> null
        }
}
