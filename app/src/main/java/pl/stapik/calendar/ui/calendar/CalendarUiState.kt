package pl.stapik.calendar.ui.calendar

import pl.stapik.calendar.data.model.CalendarEntry
import java.time.LocalDate

sealed interface CalendarUiState {
    data object Loading : CalendarUiState
    data class Success(val entriesByDay: Map<LocalDate, List<CalendarEntry>>, val isRefreshing: Boolean = false) : CalendarUiState
    data class Error(val error: CalendarLoadError) : CalendarUiState
    data object NotConfigured : CalendarUiState
}