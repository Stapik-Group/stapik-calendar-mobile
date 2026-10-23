package pl.stapik.calendar.ui.navigation

sealed interface AppScreen {
    data object Calendar: AppScreen
    data object Settings: AppScreen
    data object Notifications: AppScreen
}
