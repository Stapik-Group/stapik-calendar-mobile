package pl.stapik.calendar.data.model

fun List<CalendarEntry>.inDayOrder(): List<CalendarEntry> =
    sortedWith(compareBy<CalendarEntry>({ it.time.isEmpty() }, { it.time }))
