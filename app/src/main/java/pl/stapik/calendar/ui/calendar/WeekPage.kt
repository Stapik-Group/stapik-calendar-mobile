package pl.stapik.calendar.ui.calendar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import pl.stapik.calendar.data.model.CalendarEntry
import java.time.LocalDate

@Composable
fun WeekPage(
    weekStart: LocalDate,
    entriesByDay: Map<LocalDate, List<CalendarEntry>>,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val days = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(days) { date ->
            DayRow(date = date, entries = entriesByDay[date].orEmpty(), isToday = date == today)
        }
    }
}
