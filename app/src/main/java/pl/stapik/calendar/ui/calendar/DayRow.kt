package pl.stapik.calendar.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.stapik.calendar.R
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.ui.theme.LocalEntryPalette
import pl.stapik.calendar.ui.theme.LocalThemeColors
import pl.stapik.calendar.ui.theme.themedSurface
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun DayRow(
    date: LocalDate,
    entries: List<CalendarEntry>,
    isToday: Boolean,
    canEdit: Boolean,
    onAddEntry: (LocalDate) -> Unit,
    onEditEntry: (CalendarEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
    val uriHandler = LocalUriHandler.current
    val themeColors = LocalThemeColors.current
    val entryPalette = LocalEntryPalette.current

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isToday) themeColors.todayBackground else themeColors.cellBackground)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val weekdayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                .replaceFirstChar { it.uppercase() }
            Text(
                text = weekdayName,
                color = if (isToday) themeColors.todayText else themeColors.textDark,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = dayFormatter.format(date),
                color = if (isToday) themeColors.todayText else themeColors.textDark
            )
            if (canEdit) {
                Text(
                    text = "+",
                    color = if (isToday) themeColors.todayText else themeColors.textDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { onAddEntry(date) }
                        .padding(horizontal = 8.dp)
                )
            }
        }

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColors.cellBackgroundSecondary)
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Text(text = stringResource(R.string.no_entries), color = themeColors.textMuted)
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().background(themeColors.cellBackgroundSecondary)) {
                entries.forEach { entry ->
                    val hasLink = entry.link.isNotBlank()
                    val entryColor = entryPalette[entry.color] ?: entryPalette.getValue("default")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .themedSurface(themeColors = themeColors, backgroundColor = entryColor.background)
                            .let { m ->
                                when {
                                    canEdit -> m.clickable { onEditEntry(entry) }
                                    hasLink -> m.clickable { uriHandler.openUri(entry.link) }
                                    else -> m
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(text = entry.name, color = entryColor.textColor, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}