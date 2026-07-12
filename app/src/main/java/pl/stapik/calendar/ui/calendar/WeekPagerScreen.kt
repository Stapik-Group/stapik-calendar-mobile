package pl.stapik.calendar.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stapikgroup.stapikcalendar.ui.calendar.WeekNavBar
import java.time.LocalDate
import kotlinx.coroutines.launch
import pl.stapik.calendar.ui.theme.RetroColors

@Composable
fun WeekPagerScreen(
    viewModel: CalendarViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = WeekPaging.INITIAL_PAGE) { WeekPaging.PAGE_COUNT }
    val coroutineScope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }

    Column(modifier = modifier.fillMaxSize().background(RetroColors.WindowBackground)) {
        when (val current = state) {
            is CalendarUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ładowanie...") }
            }
            is CalendarUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(current.message, color = RetroColors.TextDark)
                }
            }
            is CalendarUiState.Success -> {
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

                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    WeekPage(
                        weekStart = WeekPaging.pageToWeekStart(page),
                        entriesByDay = current.entriesByDay,
                        today = today
                    )
                }
            }
        }
    }
}