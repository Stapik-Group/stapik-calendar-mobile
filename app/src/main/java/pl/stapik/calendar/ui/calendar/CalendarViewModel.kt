package pl.stapik.calendar.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.stapik.calendar.data.repository.CalendarFetchOutcome
import pl.stapik.calendar.data.repository.CalendarRepository
import pl.stapik.calendar.data.repository.MissingConfigException
import retrofit2.HttpException

class CalendarViewModel(private val repository: CalendarRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val currentSuccess = _uiState.value as? CalendarUiState.Success
            _uiState.value = currentSuccess?.copy(isRefreshing = true) ?: CalendarUiState.Loading
            when (val outcome = repository.fetchEntries()) {
                is CalendarFetchOutcome.Fresh -> {
                    _uiState.value = CalendarUiState.Success(
                        entriesByDay = outcome.result.entries.groupBy { LocalDate.parse(it.date) },
                        isStale = false,
                        updatedAt = outcome.result.updatedAt
                    )
                }
                is CalendarFetchOutcome.Cached -> {
                    _uiState.value = CalendarUiState.Success(
                        entriesByDay = outcome.cached.entries.groupBy { LocalDate.parse(it.date) },
                        isStale = true,
                        updatedAt = outcome.cached.updatedAt
                    )
                }
                is CalendarFetchOutcome.Failure -> {
                    _uiState.value = mapError(outcome.cause)
                }
            }
        }
    }

    private fun mapError(error: Throwable): CalendarUiState = when (error) {
        is MissingConfigException -> CalendarUiState.NotConfigured
        is UnknownHostException, is SocketTimeoutException ->
            CalendarUiState.Error(CalendarLoadError.NoNetwork)
        is HttpException -> when (error.code()) {
            401, 403 -> CalendarUiState.Error(CalendarLoadError.Unauthorized)
            404 -> CalendarUiState.Error(CalendarLoadError.NotFound)
            else -> CalendarUiState.Error(CalendarLoadError.Unknown(error.message()))
        }
        else -> CalendarUiState.Error(CalendarLoadError.Unknown(error.message ?: "Unknown error"))
    }
}

class CalendarViewModelFactory(private val repository: CalendarRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(repository) as T
}