package pl.stapik.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import pl.stapik.calendar.data.config.DataStoreApiConfigStorage
import pl.stapik.calendar.data.notifications.DataStoreNotificationPreferencesStorage
import pl.stapik.calendar.data.theme.DataStoreThemeStorage
import pl.stapik.calendar.ui.root.AppRoot
import pl.stapik.calendar.ui.theme.AppTheme
import pl.stapik.calendar.ui.theme.ThemePalettes
import pl.stapik.calendar.ui.theme.toMaterialColorScheme
import pl.stapik.calendar.ui.widget.WidgetRefreshScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val apiConfigStorage = remember { DataStoreApiConfigStorage(applicationContext) }
            val themeStorage = remember { DataStoreThemeStorage(applicationContext) }
            val notificationPreferencesStorage = remember { DataStoreNotificationPreferencesStorage(applicationContext) }

            val theme by themeStorage.theme.collectAsState(initial = AppTheme.CLASSIC)
            val themeColors = ThemePalettes.forTheme(theme)

            DisposableEffect(themeColors.isDark) {
                val barStyle = if (themeColors.isDark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose { }
            }

            MaterialTheme(colorScheme = themeColors.toMaterialColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = themeColors.windowBackground) {
                    AppRoot(apiConfigStorage = apiConfigStorage, themeStorage = themeStorage, notificationPreferencesStorage = notificationPreferencesStorage)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        WidgetRefreshScheduler.refreshNow(applicationContext)
    }
}
