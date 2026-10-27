package pl.stapik.calendar.ui.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

object DayBoundary {
    private const val MARGIN_MILLIS = 500L

    fun millisUntilNextDay(now: ZonedDateTime): Long {
        val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        return Duration.between(now, nextDay).toMillis() + MARGIN_MILLIS
    }
}

@Composable
fun rememberToday(clock: Clock = Clock.systemDefaultZone()): State<LocalDate> {
    val today = remember { mutableStateOf(LocalDate.now(clock)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { today.value = LocalDate.now(clock) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(DayBoundary.millisUntilNextDay(ZonedDateTime.now(clock)))
            today.value = LocalDate.now(clock)
        }
    }
    return today
}
