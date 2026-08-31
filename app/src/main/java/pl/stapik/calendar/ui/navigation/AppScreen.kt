package pl.stapik.calendar.ui.navigation

sealed interface AppScreen {
    data object Calendar: AppScreen
    data object Connect: AppScreen
    data object About: AppScreen
    data object Theme: AppScreen
}