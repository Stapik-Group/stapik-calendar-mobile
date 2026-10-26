package pl.stapik.calendar.ui.root

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch
import pl.stapik.calendar.R
import pl.stapik.calendar.data.config.ApiConfigStorage
import pl.stapik.calendar.data.notifications.NotificationPreferencesStorage
import pl.stapik.calendar.data.theme.ThemeStorage
import pl.stapik.calendar.notifications.NotificationScheduler
import pl.stapik.calendar.ui.calendar.WeekPagerScreen
import pl.stapik.calendar.ui.navigation.AppScreen
import pl.stapik.calendar.ui.notifications.NotificationsScreen
import pl.stapik.calendar.util.AppInfo
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.presentation.ConnectController
import pl.stapik.common.theme.ThemeId
import pl.stapik.common.ui.components.StapikButton
import pl.stapik.common.ui.shell.AppDefinition
import pl.stapik.common.ui.shell.SettingsFlow
import pl.stapik.common.ui.theme.StapikTheme

private val appDefinition = AppDefinition(
    appId = "calendar",
    name = "Stapik Calendar",
    versionName = AppInfo.VERSION_NAME,
    author = AppInfo.AUTHOR,
    websiteUrl = AppInfo.GITHUB_URL,
)

/**
 * @param newConnectController builds the controller of the connect form, prefilled with the saved
 *   connection; called each time the settings are opened.
 */
@Composable
fun AppRoot(
    apiConfigStorage: ApiConfigStorage,
    themeStorage: ThemeStorage,
    notificationPreferencesStorage: NotificationPreferencesStorage,
    calendarViewModelFactory: ViewModelProvider.Factory,
    newConnectController: (CloudConfig?) -> ConnectController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentTheme by themeStorage.theme.collectAsState(initial = ThemeId.CLASSIC)
    val notificationsEnabled by notificationPreferencesStorage.enabled.collectAsState(initial = false)
    val connectionProblem by apiConfigStorage.problem.collectAsState()
    var savedConfig by remember { mutableStateOf<CloudConfig?>(null) }
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Calendar) }

    LaunchedEffect(Unit) { savedConfig = apiConfigStorage.load() }

    LaunchedEffect(notificationsEnabled) {
        if (notificationsEnabled) {
            NotificationScheduler.ensureScheduled(context.applicationContext)
        } else {
            NotificationScheduler.cancel(context.applicationContext)
        }
    }

    StapikTheme(currentTheme) {
        BackHandler(enabled = screen == AppScreen.Notifications) {
            screen = AppScreen.Settings
        }
        Scaffold(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                if (screen == AppScreen.Calendar) {
                    Box(modifier = Modifier.navigationBarsPadding()) {
                        FloatingActionButton(
                            onClick = { screen = AppScreen.Settings },
                            containerColor = StapikTheme.colors.button,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (screen) {
                    AppScreen.Calendar -> WeekPagerScreen(
                        viewModelFactory = calendarViewModelFactory,
                        connectionProblem = connectionProblem,
                        onNavigateToConnect = { screen = AppScreen.Settings }
                    )

                    AppScreen.Settings -> {
                        val connectController = remember {
                            newConnectController(savedConfig)
                        }
                        SettingsFlow(
                            app = appDefinition,
                            themeId = currentTheme,
                            onThemeChange = { theme -> scope.launch { themeStorage.setTheme(theme) } },
                            connectController = connectController,
                            connected = savedConfig != null,
                            onConnected = { scope.launch { savedConfig = apiConfigStorage.load() } },
                            onDisconnected = { savedConfig = null },
                            onClose = { screen = AppScreen.Calendar },
                            modifier = Modifier.fillMaxSize(),
                            extra = {
                                StapikButton(
                                    text = stringResource(R.string.menu_notifications),
                                    onClick = { screen = AppScreen.Notifications },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    }

                    AppScreen.Notifications -> NotificationsScreen(
                        storage = notificationPreferencesStorage,
                        onBack = { screen = AppScreen.Settings }
                    )
                }
            }
        }
    }
}
