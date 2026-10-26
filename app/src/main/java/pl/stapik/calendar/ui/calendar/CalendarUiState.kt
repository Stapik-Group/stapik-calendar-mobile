package pl.stapik.calendar.ui.calendar

import java.time.LocalDate
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.model.CalendarPayload
import pl.stapik.calendar.data.model.inDayOrder
import pl.stapik.common.presentation.DocumentUiState
import pl.stapik.common.presentation.LoadErrorKind

sealed interface CalendarUiState {
    data object Loading : CalendarUiState
    data class Success(
        val entriesByDay: Map<LocalDate, List<CalendarEntry>>,
        val canEdit: Boolean
    ) : CalendarUiState
    data class Error(val kind: LoadErrorKind) : CalendarUiState
}

fun DocumentUiState<CalendarPayload>.toCalendarUiState(canEdit: Boolean): CalendarUiState = when (this) {
    DocumentUiState.Loading -> CalendarUiState.Loading
    is DocumentUiState.Error -> CalendarUiState.Error(kind)
    is DocumentUiState.Success -> CalendarUiState.Success(value.entries.byDay(), canEdit)
}

private fun List<CalendarEntry>.byDay(): Map<LocalDate, List<CalendarEntry>> =
    mapNotNull { entry -> runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { it to entry } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, entries) -> entries.inDayOrder() }
