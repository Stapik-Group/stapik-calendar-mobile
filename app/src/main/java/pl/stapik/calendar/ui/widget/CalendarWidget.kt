package pl.stapik.calendar.ui.widget

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.CancellationException
import pl.stapik.calendar.MainActivity
import pl.stapik.calendar.R
import pl.stapik.calendar.appContainer
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.theme.DataStoreThemeStorage
import pl.stapik.calendar.ui.theme.entryColorFor
import pl.stapik.common.theme.ThemeId
import pl.stapik.common.theme.ThemePalettes
import pl.stapik.common.ui.theme.StapikColors
import pl.stapik.common.ui.theme.toMaterialColorScheme
import pl.stapik.common.ui.theme.toStapikColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val applicationContext = context.applicationContext

        val entries = readEntries(applicationContext)
        val theme = DataStoreThemeStorage(applicationContext).theme

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val entriesByDate = entries
            .mapNotNull { entry -> runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { it to entry } }
            .groupBy({ it.first }, { it.second })

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

private suspend fun readEntries(context: Context): List<CalendarEntry> =
    try {
        context.appContainer.calendarBook.load().entries
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    }

@Composable
private fun CalendarWidgetContent(
    today: LocalDate,
    tomorrow: LocalDate,
    todayLabel: String,
    tomorrowLabel: String,
    todayEntries: List<CalendarEntry>,
    tomorrowEntries: List<CalendarEntry>,
    theme: kotlinx.coroutines.flow.Flow<ThemeId>
) {
    val selectedTheme by theme.collectAsState(initial = ThemeId.CLASSIC)
    val palette = remember(selectedTheme) { ThemePalettes.of(selectedTheme) }
    val scheme = remember(palette) { palette.toMaterialColorScheme() }
    val colors = remember(palette) { palette.toStapikColors() }
    val appAction = actionStartActivity<MainActivity>()

    LazyColumn(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(
                ColorProvider(
                    day = scheme.background,
                    night = scheme.background
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
                scheme = scheme
            )
        }

        items(
            items = todayEntries,
            itemId = { entry -> entryId(entry) }
        ) { entry ->
            CalendarEntryRow(entry, scheme, colors, appAction)
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
                scheme = scheme
            )
        }

        items(
            items = tomorrowEntries,
            itemId = { entry -> entryId(entry) }
        ) { entry ->
            CalendarEntryRow(entry, scheme, colors, appAction)
        }
    }
}

@Composable
private fun DateHeader(
    label: String,
    date: LocalDate,
    action: androidx.glance.action.Action,
    scheme: ColorScheme
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(
                ColorProvider(
                    day = scheme.primary,
                    night = scheme.primary
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
                        day = scheme.onPrimary,
                        night = scheme.onPrimary
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            )

            Text(
                text = formatDate(date),
                style = TextStyle(
                    color = ColorProvider(
                        day = scheme.onPrimary,
                        night = scheme.onPrimary
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
    scheme: ColorScheme,
    colors: StapikColors,
    action: androidx.glance.action.Action
) {
    val entryColor = entryColorFor(colors, scheme, entry.color)
    val modifier = GlanceModifier
        .fillMaxWidth()
        .clickable(action)
        .background(ColorProvider(day = entryColor.background, night = entryColor.background))
        .padding(horizontal = 10.dp, vertical = 7.dp)

    Column(
        modifier = GlanceModifier.fillMaxWidth()
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.name,
                style = TextStyle(
                    color = ColorProvider(
                        day = entryColor.content,
                        night = entryColor.content
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
                        day = colors.borderDark,
                        night = colors.borderDark
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