package pl.stapik.calendar.ui.conflicts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.stapik.calendar.data.document.CalendarBook
import pl.stapik.calendar.data.document.ConflictCopies
import pl.stapik.calendar.data.document.RecoverableCopy
import pl.stapik.calendar.data.model.CalendarEdits
import pl.stapik.calendar.data.model.CalendarPayload
import pl.stapik.calendar.data.model.CalendarYears

data class ConflictCopyRow(val copy: RecoverableCopy, val missingCount: Int)

fun conflictCopyRows(
    current: CalendarPayload,
    copies: List<RecoverableCopy>,
    edits: CalendarEdits = CalendarEdits()
): List<ConflictCopyRow> = copies.map { ConflictCopyRow(it, edits.missing(current, it.entries).size) }

class ConflictCopiesViewModel(
    private val book: CalendarBook,
    private val copies: ConflictCopies,
    private val edits: CalendarEdits = CalendarEdits()
) : ViewModel() {
    private val loaded = MutableStateFlow<List<ConflictCopyRow>?>(null)

    val rows: StateFlow<List<ConflictCopyRow>?> = loaded.asStateFlow()

    fun reload() {
        viewModelScope.launch { loaded.value = readRows() }
    }

    fun addMissingEntries(row: ConflictCopyRow) {
        viewModelScope.launch {
            guarded { addToCalendar(row) }
            loaded.value = readRows()
        }
    }

    private suspend fun readRows(): List<ConflictCopyRow> =
        guarded { conflictCopyRows(book.load(), copies.all(), edits) } ?: emptyList()

    private suspend fun addToCalendar(row: ConflictCopyRow) {
        CalendarYears.split(row.copy.entries, LocalDate.now().year).forEach { (year, entries) ->
            book.edit(year) { edits.addMissing(it, entries) }
        }
        book.sync()
    }

    private suspend fun <R> guarded(block: suspend () -> R): R? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}

class ConflictCopiesViewModelFactory(
    private val book: CalendarBook,
    private val copies: ConflictCopies
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ConflictCopiesViewModel(book, copies) as T
}
