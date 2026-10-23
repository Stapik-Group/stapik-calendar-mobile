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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stapikgroup.stapikcalendar.ui.calendar.WeekNavBar
import java.time.LocalDate
import kotlinx.coroutines.launch
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.repository.CalendarRepository
import pl.stapik.calendar.R
import pl.stapik.common.ui.components.themedSurface
import pl.stapik.common.ui.theme.StapikTheme

@Composable
fun WeekPagerScreen(
    repository: CalendarRepository,
    onNavigateToConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CalendarViewModel = viewModel(
        factory = remember(repository) { CalendarViewModelFactory(repository) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = WeekPaging.INITIAL_PAGE) { WeekPaging.PAGE_COUNT }
    val coroutineScope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val scheme = MaterialTheme.colorScheme
    val colors = StapikTheme.colors
    val conflict by viewModel.conflict.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<EntryEditorTarget?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    conflict?.let {
        ConflictDialog(conflict = it, onKeepLocal = viewModel::keepLocal, onUseServer = viewModel::useServer)
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

        val isRefreshing = (state as? CalendarUiState.Success)?.isRefreshing ?: false

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f)
        ) {
            when (val current = state) {
                is CalendarUiState.Loading -> CenteredMessage(stringResource(R.string.loading))

                is CalendarUiState.Error -> {
                    CenteredMessageWithAction(
                        message = current.error.toDisplayMessage(),
                        actionLabel = stringResource(R.string.retry),
                        onAction = viewModel::refresh
                    )
                }

                is CalendarUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val bannerText = when {
                            current.isLocalMode -> stringResource(R.string.local_mode_banner)
                            current.isStale && current.hasPendingChanges -> stringResource(R.string.pending_changes_banner)
                            current.isStale -> stringResource(R.string.cached_data_banner, formatCachedTimestamp(current.updatedAt))
                            else -> null
                        }
                        if (bannerText != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.button)
                                    .let { if (current.isLocalMode) it.clickable(onClick = onNavigateToConnect) else it }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(bannerText, color = scheme.onBackground)
                            }
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

internal fun formatCachedTimestamp(updatedAt: String?): String {
    if (updatedAt == null) return ""
    return runCatching {
        val instant = java.time.Instant.parse(updatedAt)
        java.time.format.DateTimeFormatter
            .ofLocalizedDateTime(java.time.format.FormatStyle.SHORT)
            .withZone(java.time.ZoneId.systemDefault())
            .format(instant)
    }.getOrDefault(updatedAt)
}

@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun CenteredMessageWithAction(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val colors = StapikTheme.colors
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = scheme.onBackground)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .themedSurface(colors, colors.button, raised = true)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(actionLabel, color = scheme.onBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class EntryEditorTarget(val entry: CalendarEntry?, val date: LocalDate)
