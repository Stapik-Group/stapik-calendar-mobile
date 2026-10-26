package pl.stapik.calendar.util

import pl.stapik.calendar.BuildConfig

object AppInfo {
    const val AUTHOR = "Sebastian Smoliński"
    const val GITHUB_URL = "https://github.com/Stapik-Group/stapik-calendar-mobile"
    const val RELEASES_URL = "$GITHUB_URL/releases/latest"
    val VERSION_NAME: String = BuildConfig.VERSION_NAME
}
