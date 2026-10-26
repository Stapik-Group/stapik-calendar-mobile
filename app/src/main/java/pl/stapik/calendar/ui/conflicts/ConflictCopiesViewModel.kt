package pl.stapik.calendar.ui.conflicts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.stapik.calendar.data.document.ConflictCopies
import pl.stapik.calendar.data.document.RecoverableCopy
import pl.stapik.calendar.data.model.CalendarEdits
import pl.stapik.calendar.data.model.CalendarPayload
import pl.stapik.common.sync.SyncEngine

data class ConflictCopyRow(val copy: RecoverableCopy, val missingCount: Int)

fun conflictCopyRows(
    current: CalendarPayload,
    copies: List<RecoverableCopy>,
    edits: CalendarEdits = CalendarEdits()
): List<ConflictCopyRow> = copies.map { ConflictCopyRow(it, edits.missing(current, it.entries).size) }

class ConflictCopiesViewModel(
    private val engine: SyncEngine<CalendarPayload>,
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
            guarded { engine.edit { edits.addMissing(it, row.copy.entries) } }
            loaded.value = readRows()
        }
    }

    private suspend fun readRows(): List<ConflictCopyRow> =
        guarded { conflictCopyRows(engine.load(), copies.all(), edits) } ?: emptyList()

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
    private val engine: SyncEngine<CalendarPayload>,
    private val copies: ConflictCopies
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ConflictCopiesViewModel(engine, copies) as T
}
