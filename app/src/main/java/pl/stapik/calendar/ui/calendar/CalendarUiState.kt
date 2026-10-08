package pl.stapik.calendar.ui.calendar

import pl.stapik.calendar.data.model.CalendarEntry
import java.time.LocalDate

sealed interface CalendarUiState {
    data object Loading : CalendarUiState
    data class Success(
        val entriesByDay: Map<LocalDate, List<CalendarEntry>>,
        val isRefreshing: Boolean = false,
        val isStale: Boolean = false,
        val updatedAt: String? = null,
        val canEdit: Boolean = false,
        val isLocalMode: Boolean = false,
        val hasPendingChanges: Boolean = false
    ) : CalendarUiState
    data class Error(val error: CalendarLoadError) : CalendarUiState
}
