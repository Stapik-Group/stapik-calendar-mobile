package pl.stapik.calendar.ui.calendar

sealed interface CalendarLoadError {
    data object NoNetwork : CalendarLoadError
    data object Unauthorized : CalendarLoadError
    data object NotFound : CalendarLoadError
    data class Unknown(val message: String) : CalendarLoadError
}