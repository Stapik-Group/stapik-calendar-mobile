package pl.stapik.calendar.ui.theme

import pl.stapik.common.theme.ThemeId

/**
 * Temporary bridge: the calendar screens still draw with their own [AppTheme] palettes while the
 * theme is already chosen with the shared [ThemeId]. Removed when the screens move to the shared palettes.
 */
fun ThemeId.toAppTheme(): AppTheme = when (this) {
    ThemeId.CLASSIC -> AppTheme.CLASSIC
    ThemeId.CLASSIC_PINK -> AppTheme.CLASSIC_PINK
    ThemeId.MODERN, ThemeId.NEOCLASSIC -> AppTheme.MODERN
    ThemeId.DARK -> AppTheme.DARK
}
