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

            repository.fetchEntries().fold(
                onSuccess = { entries ->
                    _uiState.value = CalendarUiState.Success(
                        entriesByDay = entries.groupBy { LocalDate.parse(it.date) }
                    )
                },
                onFailure = { error -> _uiState.value = mapError(error) }
            )
        }
    }

    private fun mapError(error: Throwable): CalendarUiState = when (error) {
        is MissingConfigException -> CalendarUiState.NotConfigured
        is UnknownHostException, is SocketTimeoutException ->
            CalendarUiState.Error(CalendarLoadError.NoNetwork)
        is HttpException -> when (error.code()) {
            401, 403 -> CalendarUiState.Error(CalendarLoadError.Unauthorized)
            else -> CalendarUiState.Error(CalendarLoadError.Unknown(error.message()))
        }
        else -> CalendarUiState.Error(CalendarLoadError.Unknown(error.message ?: "Unknown error"))
    }
}

class CalendarViewModelFactory(private val repository: CalendarRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(repository) as T
}