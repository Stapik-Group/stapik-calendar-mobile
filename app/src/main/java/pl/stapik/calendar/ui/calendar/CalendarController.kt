package pl.stapik.calendar.ui.calendar

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.stapik.calendar.data.document.CalendarBook
import pl.stapik.calendar.data.model.CalendarPayload
import pl.stapik.common.document.DocumentCorruptedException
import pl.stapik.common.model.DocumentDecodeException
import pl.stapik.common.presentation.DocumentUiState
import pl.stapik.common.presentation.LoadErrorKind
import pl.stapik.common.presentation.SyncBadge
import pl.stapik.common.presentation.SyncUiMapper
import pl.stapik.common.presentation.SyncUiState
import pl.stapik.common.presentation.isQuiet
import pl.stapik.common.sync.FirstConnectionChoice
import pl.stapik.common.sync.SyncOutcome
import pl.stapik.common.sync.SyncRetry
import pl.stapik.common.sync.SyncRetryPolicy
import pl.stapik.common.sync.SyncStatus

class CalendarController(
    private val book: CalendarBook,
    private val scope: CoroutineScope,
    private val retry: SyncRetry,
    private val connected: suspend () -> Boolean,
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS,
    private val waiter: suspend (Long) -> Unit = { delay(it) }
) {
    private val mutableDocument = MutableStateFlow<DocumentUiState<CalendarPayload>>(DocumentUiState.Loading)
    private val mutableSync = MutableStateFlow(SyncUiMapper.map(SyncStatus.IDLE, null, false, false))
    private var lastOutcome: SyncOutcome? = null
    private var resolvedNotice: SyncOutcome? = null
    private var debounce: Job? = null
    private var debounceWaiting = false

    val document: StateFlow<DocumentUiState<CalendarPayload>> = mutableDocument.asStateFlow()
    val sync: StateFlow<SyncUiState> = mutableSync.asStateFlow()

    init {
        scope.launch { book.isSyncing.collect { refreshIndicator() } }
    }

    suspend fun load() {
        mutableDocument.value = DocumentUiState.Loading
        mutableDocument.value = readDocument()
        refreshIndicator()
    }

    suspend fun editDebounced(year: Int, change: (CalendarPayload) -> CalendarPayload): SyncOutcome? {
        val refused = book.edit(year, change)
        if (refused != null) {
            finish(refused)
            return refused
        }
        cancelWaiting()
        debounceWaiting = true
        debounce = scope.launch {
            waiter(debounceMillis)
            debounceWaiting = false
            sync()
        }
        reloadDocument()
        refreshIndicator()
        return null
    }

    suspend fun flushPendingSync(): SyncOutcome? {
        if (!debounceWaiting) return null
        cancelWaiting()
        return sync()
    }

    suspend fun sync(choice: FirstConnectionChoice? = null): SyncOutcome {
        mutableSync.value = SyncUiState(SyncBadge.SYNCING, "sync.syncing")
        val outcome = book.sync(choice)
        finish(outcome)
        return outcome
    }

    suspend fun discardLocalAndResync(): SyncOutcome {
        book.discardUnreadable()
        lastOutcome = null
        resolvedNotice = null
        load()
        return sync()
    }

    suspend fun dismissResolvedNotice() {
        resolvedNotice = null
        refreshIndicator()
    }

    private fun cancelWaiting() {
        if (debounceWaiting) debounce?.cancel()
        debounceWaiting = false
    }

    private suspend fun finish(outcome: SyncOutcome) {
        if (SyncRetryPolicy.isWorthRetrying(outcome)) retry.schedule() else retry.cancel()
        if (outcome == SyncOutcome.ServerWon || outcome == SyncOutcome.LocalWon) {
            resolvedNotice = outcome
            lastOutcome = SyncOutcome.Synchronized
        } else {
            lastOutcome = outcome
        }
        if (outcome !is SyncOutcome.NewerSchema) reloadDocument()
        refreshIndicator()
    }

    private suspend fun reloadDocument() {
        mutableDocument.value = readDocument()
    }

    private suspend fun readDocument(): DocumentUiState<CalendarPayload> =
        try {
            DocumentUiState.Success(book.load())
        } catch (e: CancellationException) {
            throw e
        } catch (e: DocumentCorruptedException) {
            DocumentUiState.Error(if (e.undecryptable) LoadErrorKind.UNDECRYPTABLE else LoadErrorKind.CORRUPTED)
        } catch (e: DocumentDecodeException) {
            DocumentUiState.Error(LoadErrorKind.MODEL_MISMATCH)
        } catch (e: Exception) {
            DocumentUiState.Error(LoadErrorKind.UNKNOWN)
        }

    private suspend fun refreshIndicator() {
        val pending = try {
            book.hasPendingChanges()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
        val status = if (book.isSyncing.value) SyncStatus.SYNCING else SyncStatus.IDLE
        val current = SyncUiMapper.map(status, lastOutcome, pending, connected())
        val notice = resolvedNotice
        mutableSync.value =
            if (current.isQuiet && notice != null) SyncUiMapper.map(status, notice, pending, connected()) else current
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MILLIS = 2_000L
    }
}
