package pl.stapik.calendar.ui.calendar

import androidx.lifecycle.ViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.stapik.calendar.data.model.CalendarEntry

class CalendarViewModel : ViewModel() {
    private val mockEntries = listOf(
        CalendarEntry(date = "2026-06-30", name = "France - Sweden", link = "")
    )

    private val _uiState = MutableStateFlow<CalendarUiState>(
        CalendarUiState.Success(
            entriesByDay = mockEntries.groupBy { LocalDate.parse(it.date) }
        )
    )

    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
}