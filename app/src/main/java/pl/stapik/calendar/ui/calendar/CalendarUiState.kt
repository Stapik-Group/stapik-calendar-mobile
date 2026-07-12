package pl.stapik.calendar.ui.calendar

import pl.stapik.calendar.data.model.CalendarEntry
import java.time.LocalDate
import java.time.YearMonth

sealed interface CalendarUiState {
    data object Loading : CalendarUiState
    data class Success(val entriesByDay: Map<LocalDate, List<CalendarEntry>>) : CalendarUiState
    data class Error(val message: String) : CalendarUiState
}