package pl.stapik.calendar.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.stapik.calendar.R
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.ui.theme.EntryColors
import pl.stapik.calendar.ui.theme.RetroColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun DayRow(
    date: LocalDate,
    entries: List<CalendarEntry>,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isToday) RetroColors.HeaderBlue else RetroColors.CellBackground)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val weekdayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                .replaceFirstChar { it.uppercase() }
            Text(
                text = weekdayName,
                color = if (isToday) RetroColors.TextOnBlue else RetroColors.TextDark,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = dayFormatter.format(date),
                color = if (isToday) RetroColors.TextOnBlue else RetroColors.TextDark
            )
        }

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RetroColors.CellBackgroundOtherMonth)
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Text(text = stringResource(R.string.no_entries), color = RetroColors.TextMuted)
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().background(RetroColors.CellBackgroundOtherMonth)) {
                entries.forEach { entry ->
                    val hasLink = entry.link.isNotBlank()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .background(EntryColors.forKey(entry.color))
                            .let { m -> if (hasLink) m.clickable { uriHandler.openUri(entry.link) } else m }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(text = entry.name, color = RetroColors.TextOnBlue, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}