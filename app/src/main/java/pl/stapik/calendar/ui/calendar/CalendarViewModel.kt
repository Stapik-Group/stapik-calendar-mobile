package pl.stapik.calendar.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.stapik.calendar.data.cloud.CalendarAccess
import pl.stapik.calendar.data.document.CalendarBook
import pl.stapik.calendar.data.model.CalendarEdits
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.model.CalendarPayload
import pl.stapik.calendar.data.model.CalendarYears
import pl.stapik.common.presentation.DocumentUiState
import pl.stapik.common.presentation.SyncBadge
import pl.stapik.common.presentation.SyncUiState
import pl.stapik.common.sync.FirstConnectionChoice
import pl.stapik.common.sync.SyncOutcome
import pl.stapik.common.sync.SyncRetry

data class FirstConnectionPrompt(val canKeepLocal: Boolean)

class CalendarViewModel(
    book: CalendarBook,
    private val access: CalendarAccess,
    connected: suspend () -> Boolean,
    retry: SyncRetry,
    private val edits: CalendarEdits = CalendarEdits()
) : ViewModel() {
    private val controller = CalendarController(book, viewModelScope, retry, connected)
    private val writable = MutableStateFlow(true)
    private val prompt = MutableStateFlow<FirstConnectionPrompt?>(null)
    private var canKeepLocal = true

    val sync: StateFlow<SyncUiState> = controller.sync

    val firstConnectionPrompt: StateFlow<FirstConnectionPrompt?> = prompt.asStateFlow()

    val uiState: StateFlow<CalendarUiState> =
        combine(controller.document, controller.sync, writable) { document, syncState, canWrite ->
            document.toCalendarUiState(canEdit = canWrite && syncState.badge != SyncBadge.READ_ONLY)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, CalendarUiState.Loading)

    fun refresh() = launchSync {
        if (controller.document.value !is DocumentUiState.Success) controller.load()
        if (controller.document.value is DocumentUiState.Success) controller.sync() else null
    }

    fun addEntry(entry: CalendarEntry) = edit(yearOf(entry)) { edits.add(it, entry) }

    fun updateEntry(old: CalendarEntry, new: CalendarEntry) {
        val oldYear = yearOf(old)
        val newYear = yearOf(new)
        if (oldYear == newYear) {
            edit(oldYear) { edits.replace(it, old, new) }
        } else {
            val moved = new.copy(unknownFields = old.unknownFields)
            launchSync {
                controller.editDebounced(oldYear) { edits.remove(it, old) }
                controller.editDebounced(newYear) { edits.add(it, moved) }
            }
        }
    }

    fun deleteEntry(entry: CalendarEntry) = edit(yearOf(entry)) { edits.remove(it, entry) }

    fun flushPendingSync() = launchSync { controller.flushPendingSync() }

    fun discardLocalAndResync() = launchSync { controller.discardLocalAndResync() }

    fun askForSource() {
        prompt.value = FirstConnectionPrompt(canKeepLocal)
    }

    fun chooseSource(choice: FirstConnectionChoice) {
        prompt.value = null
        launchSync { controller.sync(choice) }
    }

    fun postponeChoice() {
        prompt.value = null
    }

    fun dismissNotice() {
        viewModelScope.launch { controller.dismissResolvedNotice() }
    }

    private fun edit(year: Int, change: (CalendarPayload) -> CalendarPayload) {
        if (controller.document.value !is DocumentUiState.Success) return
        launchSync { controller.editDebounced(year, change) }
    }

    private fun yearOf(entry: CalendarEntry): Int = CalendarYears.yearOf(entry) ?: LocalDate.now().year

    private fun launchSync(block: suspend () -> SyncOutcome?) {
        viewModelScope.launch {
            val outcome = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                controller.load()
                null
            }
            if (outcome is SyncOutcome.NeedsFirstConnectionChoice) {
                canKeepLocal = outcome.canKeepLocal
                askForSource()
            }
            writable.value = access.canEdit()
        }
    }
}

class CalendarViewModelFactory(
    private val book: CalendarBook,
    private val access: CalendarAccess,
    private val retry: SyncRetry,
    private val connected: suspend () -> Boolean
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(book, access, connected, retry) as T
}
