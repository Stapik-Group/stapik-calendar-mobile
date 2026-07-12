package com.stapikgroup.stapikcalendar.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.stapik.calendar.ui.theme.RetroColors
import pl.stapik.calendar.ui.theme.retroBevel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WeekNavBar(
    weekStart: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val weekLabelFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }

    val weekEnd = weekStart.plusDays(6)
    val label = "${weekLabelFormatter.format(weekStart)} – ${weekLabelFormatter.format(weekEnd)} ${weekEnd.year}"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(RetroColors.WindowBackground)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavButton(label = "◀", onClick = onPreviousWeek)

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .background(RetroColors.HeaderBlue)
                .retroBevel(raised = false)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = label, color = RetroColors.TextOnBlue, fontWeight = FontWeight.Bold)
        }

        NavButton(label = "▶", onClick = onNextWeek)
    }
}

@Composable
private fun NavButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(RetroColors.CellBackground)
            .retroBevel(raised = true)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = RetroColors.TextDark)
    }
}