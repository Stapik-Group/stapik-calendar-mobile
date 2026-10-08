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
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.repository.CalendarFetchOutcome
import pl.stapik.calendar.data.repository.CalendarRepository
import pl.stapik.calendar.data.repository.SyncResolution
import retrofit2.HttpException

class CalendarViewModel(private val repository: CalendarRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _conflict = MutableStateFlow<SyncConflict?>(null)
    val conflict: StateFlow<SyncConflict?> = _conflict.asStateFlow()

    private var currentEntries: List<CalendarEntry> = emptyList()

    fun refresh() = load { repository.fetchEntries() }

    fun keepLocal() = load { repository.fetchEntries(SyncResolution.KEEP_LOCAL) }

    fun useServer() = load { repository.fetchEntries(SyncResolution.USE_SERVER) }

    fun addEntry(entry: CalendarEntry) = edit { it + entry }

    fun updateEntry(old: CalendarEntry, new: CalendarEntry) = edit { list ->
        val index = list.indexOf(old)
        if (index < 0) list else list.toMutableList().also { it[index] = new }
    }

    fun deleteEntry(entry: CalendarEntry) = edit { list ->
        val index = list.indexOf(entry)
        if (index < 0) list else list.toMutableList().also { it.removeAt(index) }
    }

    private fun load(fetch: suspend () -> CalendarFetchOutcome) {
        viewModelScope.launch {
            val currentSuccess = _uiState.value as? CalendarUiState.Success
            _uiState.value = currentSuccess?.copy(isRefreshing = true) ?: CalendarUiState.Loading
            apply(fetch())
        }
    }

    private fun edit(transform: (List<CalendarEntry>) -> List<CalendarEntry>) {
        val updated = transform(currentEntries)
        if (updated == currentEntries) return
        currentEntries = updated
        (_uiState.value as? CalendarUiState.Success)?.let {
            _uiState.value = it.copy(
                entriesByDay = updated.byDay(),
                isRefreshing = true,
                hasPendingChanges = true
            )
        }
        load { repository.saveEntries(updated) }
    }

    private fun apply(outcome: CalendarFetchOutcome) {
        when (outcome) {
            is CalendarFetchOutcome.Fresh -> {
                _conflict.value = null
                show(
                    entries = outcome.result.entries,
                    updatedAt = outcome.result.updatedAt,
                    canEdit = outcome.result.scope == SCOPE_READ_WRITE
                )
            }
            is CalendarFetchOutcome.Cached -> {
                _conflict.value = null
                show(
                    entries = outcome.cached.entries,
                    updatedAt = outcome.cached.updatedAt,
                    canEdit = outcome.cached.scope == SCOPE_READ_WRITE,
                    isStale = true,
                    hasPendingChanges = outcome.cached.dirty
                )
            }
            is CalendarFetchOutcome.LocalOnly -> {
                _conflict.value = null
                show(
                    entries = outcome.cached.entries,
                    updatedAt = null,
                    canEdit = true,
                    isLocalMode = true
                )
            }
            is CalendarFetchOutcome.Conflict -> {
                _conflict.value = SyncConflict(
                    localCount = outcome.local.entries.size,
                    serverCount = outcome.server.entries.size,
                    serverUpdatedAt = outcome.server.updatedAt,
                    canKeepLocal = outcome.server.scope == SCOPE_READ_WRITE
                )
                show(
                    entries = outcome.local.entries,
                    updatedAt = outcome.local.updatedAt.ifEmpty { null },
                    canEdit = false,
                    hasPendingChanges = true
                )
            }
            is CalendarFetchOutcome.Failure -> {
                _conflict.value = null
                _uiState.value = mapError(outcome.cause)
            }
        }
    }

    private fun show(
        entries: List<CalendarEntry>,
        updatedAt: String?,
        canEdit: Boolean,
        isStale: Boolean = false,
        isLocalMode: Boolean = false,
        hasPendingChanges: Boolean = false
    ) {
        currentEntries = entries
        _uiState.value = CalendarUiState.Success(
            entriesByDay = entries.byDay(),
            isStale = isStale,
            updatedAt = updatedAt,
            canEdit = canEdit,
            isLocalMode = isLocalMode,
            hasPendingChanges = hasPendingChanges
        )
    }

    private fun List<CalendarEntry>.byDay(): Map<LocalDate, List<CalendarEntry>> =
        mapNotNull { entry -> runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { it to entry } }
            .groupBy({ it.first }, { it.second })

    private fun mapError(error: Throwable): CalendarUiState = when (error) {
        is UnknownHostException, is SocketTimeoutException ->
            CalendarUiState.Error(CalendarLoadError.NoNetwork)
        is HttpException -> when (error.code()) {
            401, 403 -> CalendarUiState.Error(CalendarLoadError.Unauthorized)
            404 -> CalendarUiState.Error(CalendarLoadError.NotFound)
            else -> CalendarUiState.Error(CalendarLoadError.Unknown(error.message()))
        }
        else -> CalendarUiState.Error(CalendarLoadError.Unknown(error.message ?: "Unknown error"))
    }

    private companion object {
        const val SCOPE_READ_WRITE = "READ_WRITE"
    }
}

class CalendarViewModelFactory(private val repository: CalendarRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(repository) as T
}
