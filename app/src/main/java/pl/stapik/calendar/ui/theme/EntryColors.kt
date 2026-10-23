package pl.stapik.calendar.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import pl.stapik.common.theme.CategoryColor
import pl.stapik.common.ui.theme.ColorOnColor
import pl.stapik.common.ui.theme.StapikColors
import pl.stapik.common.ui.theme.StapikTheme

/** The colour keys an entry can have, in the order of the picker; they are stored in the calendar document. */
val EntryColorKeys: List<String> =
    listOf("default", "red", "orange", "yellow", "green", "teal", "blue", "purple", "pink", "brown", "gray")

/**
 * Colours of an entry: the shared category colour of the theme for [key], or the accent of the theme
 * for `default` and for a key this version does not know.
 */
fun entryColorFor(colors: StapikColors, scheme: ColorScheme, key: String): ColorOnColor {
    val category = CategoryColor.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
    return category?.let { colors.categories[it] } ?: ColorOnColor(scheme.primary, scheme.onPrimary)
}

@Composable
fun entryColorFor(key: String): ColorOnColor =
    entryColorFor(StapikTheme.colors, MaterialTheme.colorScheme, key)
