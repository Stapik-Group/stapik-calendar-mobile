package pl.stapik.calendar.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import kotlinx.coroutines.launch
import pl.stapik.calendar.R
import pl.stapik.calendar.data.config.ConnectionProblem
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.util.AppInfo
import pl.stapik.common.presentation.SyncAction
import pl.stapik.common.presentation.SyncBadge
import pl.stapik.common.ui.sync.FirstConnectionDialog
import pl.stapik.common.ui.sync.LoadErrorMessage
import pl.stapik.common.ui.sync.SyncStatusBanner
import pl.stapik.common.ui.theme.StapikTheme

@Composable
fun WeekPagerScreen(
    viewModelFactory: ViewModelProvider.Factory,
    connectionProblem: ConnectionProblem?,
    onNavigateToConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CalendarViewModel = viewModel(factory = viewModelFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sync by viewModel.sync.collectAsStateWithLifecycle()
    val firstConnectionPrompt by viewModel.firstConnectionPrompt.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = WeekPaging.INITIAL_PAGE) { WeekPaging.PAGE_COUNT }
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val today = remember { LocalDate.now() }
    val scheme = MaterialTheme.colorScheme
    var editor by remember { mutableStateOf<EntryEditorTarget?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.flushPendingSync() }

    firstConnectionPrompt?.let {
        FirstConnectionDialog(
            canKeepLocal = it.canKeepLocal,
            onChoose = viewModel::chooseSource,
            onCancel = viewModel::postponeChoice
        )
    }

    editor?.let { target ->
        EntryEditDialog(
            initial = target.entry,
            initialDate = target.date,
            onSave = { saved ->
                if (target.entry == null) viewModel.addEntry(saved) else viewModel.updateEntry(target.entry, saved)
                editor = null
            },
            onDelete = target.entry?.let { existing ->
                {
                    viewModel.deleteEntry(existing)
                    editor = null
                }
            },
            onDismiss = { editor = null }
        )
    }

    Column(modifier = modifier.fillMaxSize().background(scheme.background)) {
        val currentWeekStart = WeekPaging.pageToWeekStart(pagerState.currentPage)

        WeekNavBar(
            weekStart = currentWeekStart,
            onPreviousWeek = {
                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            },
            onNextWeek = {
                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }
        )

        PullToRefreshBox(
            isRefreshing = sync.badge == SyncBadge.SYNCING,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f)
        ) {
            when (val current = state) {
                is CalendarUiState.Loading -> CenteredMessage(stringResource(R.string.loading))

                is CalendarUiState.Error -> LoadErrorMessage(
                    kind = current.kind,
                    onDiscardLocal = viewModel::discardLocalAndResync
                )

                is CalendarUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (sync.badge == SyncBadge.LOCAL_ONLY) {
                            LocalModeBanner(connectionProblem = connectionProblem, onClick = onNavigateToConnect)
                        } else {
                            SyncStatusBanner(
                                state = sync,
                                onAction = { action ->
                                    when (action) {
                                        SyncAction.RETRY -> viewModel.refresh()
                                        SyncAction.CHOOSE_SOURCE -> viewModel.askForSource()
                                        SyncAction.DISMISS -> viewModel.dismissNotice()
                                        SyncAction.RECONNECT, SyncAction.ENTER_PASSPHRASE -> onNavigateToConnect()
                                        SyncAction.UPDATE_APP -> uriHandler.openUri(AppInfo.RELEASES_URL)
                                    }
                                }
                            )
                        }
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            WeekPage(
                                weekStart = WeekPaging.pageToWeekStart(page),
                                entriesByDay = current.entriesByDay,
                                today = today,
                                canEdit = current.canEdit,
                                onAddEntry = { date -> editor = EntryEditorTarget(entry = null, date = date) },
                                onEditEntry = { entry ->
                                    editor = EntryEditorTarget(entry = entry, date = runCatching { LocalDate.parse(entry.date) }.getOrDefault(today))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalModeBanner(connectionProblem: ConnectionProblem?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(StapikTheme.colors.button)
            .let { if (connectionProblem != ConnectionProblem.NEWER_VERSION) it.clickable(onClick = onClick) else it }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(connectionProblem?.messageRes() ?: R.string.local_mode_banner),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

private fun ConnectionProblem.messageRes(): Int = when (this) {
    ConnectionProblem.NEWER_VERSION -> R.string.connection_problem_newer_version
    ConnectionProblem.UNREADABLE -> R.string.connection_problem_unreadable
    ConnectionProblem.RESET -> R.string.connection_problem_reset
}

@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onBackground)
    }
}

private data class EntryEditorTarget(val entry: CalendarEntry?, val date: LocalDate)
