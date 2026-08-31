package pl.stapik.calendar.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import pl.stapik.calendar.MainActivity
import pl.stapik.calendar.R
import pl.stapik.calendar.data.cache.DataStoreCalendarCacheStorage
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.theme.DataStoreThemeStorage
import pl.stapik.calendar.ui.theme.AppTheme
import pl.stapik.calendar.ui.theme.ThemeColors
import pl.stapik.calendar.ui.theme.ThemePalettes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val applicationContext = context.applicationContext

        val cache = DataStoreCalendarCacheStorage(applicationContext).load()
        val theme = DataStoreThemeStorage(applicationContext).theme

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val entriesByDate = cache?.entries.orEmpty()
            .groupBy { LocalDate.parse(it.date) }

        val todayLabel = context.getString(R.string.widget_today)
        val tomorrowLabel = context.getString(R.string.widget_tomorrow)

        provideContent {
            CalendarWidgetContent(
                today = today,
                tomorrow = tomorrow,
                todayLabel = todayLabel,
                tomorrowLabel = tomorrowLabel,
                todayEntries = entriesByDate[today].orEmpty(),
                tomorrowEntries = entriesByDate[tomorrow].orEmpty(),
                theme = theme
            )
        }
    }
}

@Composable
private fun CalendarWidgetContent(
    today: LocalDate,
    tomorrow: LocalDate,
    todayLabel: String,
    tomorrowLabel: String,
    todayEntries: List<CalendarEntry>,
    tomorrowEntries: List<CalendarEntry>,
    theme: kotlinx.coroutines.flow.Flow<AppTheme>
) {
    val selectedTheme by theme.collectAsState(initial = AppTheme.CLASSIC)
    val themeColors = ThemePalettes.forTheme(selectedTheme)
    val appAction = actionStartActivity<MainActivity>()

    LazyColumn(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(
                ColorProvider(
                    day = themeColors.windowBackground,
                    night = themeColors.windowBackground
                )
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        item {
            DateHeader(
                label = todayLabel,
                date = today,
                action = appAction,
                themeColors = themeColors
            )
        }

        items(
            items = todayEntries,
            itemId = { entry -> entryId(entry) }
        ) { entry ->
            CalendarEntryRow(entry, themeColors)
        }

        if (todayEntries.isNotEmpty()) {
            item {
                Spacer(GlanceModifier.height(10.dp))
            }
        }

        item {
            DateHeader(
                label = tomorrowLabel,
                date = tomorrow,
                action = appAction,
                themeColors = themeColors
            )
        }

        items(
            items = tomorrowEntries,
            itemId = { entry -> entryId(entry) }
        ) { entry ->
            CalendarEntryRow(entry, themeColors)
        }
    }
}

@Composable
private fun DateHeader(
    label: String,
    date: LocalDate,
    action: androidx.glance.action.Action,
    themeColors: ThemeColors
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(
                ColorProvider(
                    day = themeColors.accent,
                    night = themeColors.accent
                )
            )
            .clickable(action)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = TextStyle(
                    color = ColorProvider(
                        day = themeColors.textOnAccent,
                        night = themeColors.textOnAccent
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            )

            Text(
                text = formatDate(date),
                style = TextStyle(
                    color = ColorProvider(
                        day = themeColors.textOnAccent,
                        night = themeColors.textOnAccent
                    ),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun CalendarEntryRow(
    entry: CalendarEntry,
    themeColors: ThemeColors
) {
    val modifier = GlanceModifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 7.dp)

    Column(
        modifier = GlanceModifier.fillMaxWidth()
    ) {
        Row(
            modifier = if (entry.link.isNotBlank()) {
                modifier.clickable(
                    actionStartActivity<OpenLinkActivity>()
                )
            } else {
                modifier
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.name,
                style = TextStyle(
                    color = ColorProvider(
                        day = themeColors.textDark,
                        night = themeColors.textDark
                    ),
                    fontSize = 13.sp
                )
            )
        }

        Spacer(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    ColorProvider(
                        day = themeColors.borderDark,
                        night = themeColors.borderDark
                    )
                )
        )
    }
}

private fun formatDate(date: LocalDate): String =
    date.format(
        DateTimeFormatter.ofPattern(
            "d MMMM yyyy",
            Locale("pl", "PL")
        )
    )

private fun entryId(entry: CalendarEntry): Long =
    "${entry.date}|${entry.name}|${entry.link}|${entry.color}".hashCode().toLong()