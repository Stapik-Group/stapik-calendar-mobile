package pl.stapik.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import pl.stapik.calendar.data.notifications.DataStoreNotificationPreferencesStorage
import pl.stapik.calendar.data.theme.DataStoreThemeStorage
import pl.stapik.calendar.ui.root.AppRoot
import pl.stapik.calendar.ui.widget.WidgetRefreshScheduler
import pl.stapik.common.theme.ThemeId
import pl.stapik.common.theme.ThemePalettes

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val container = appContainer
            val themeStorage = remember { DataStoreThemeStorage(applicationContext) }
            val notificationPreferencesStorage = remember { DataStoreNotificationPreferencesStorage(applicationContext) }

            val theme by themeStorage.theme.collectAsState(initial = ThemeId.CLASSIC)
            val isDark = ThemePalettes.of(theme).isDark

            DisposableEffect(isDark) {
                val barStyle = if (isDark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose { }
            }

            AppRoot(
                apiConfigStorage = container.configStorage,
                themeStorage = themeStorage,
                notificationPreferencesStorage = notificationPreferencesStorage,
                repository = container.calendarRepository,
                newConnectController = container::newConnectController
            )
        }
    }

    override fun onStop() {
        super.onStop()
        WidgetRefreshScheduler.refreshNow(applicationContext)
    }
}
