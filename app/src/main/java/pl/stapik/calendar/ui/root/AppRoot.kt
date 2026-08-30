package pl.stapik.calendar.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import pl.stapik.calendar.R
import pl.stapik.calendar.data.config.ApiConfigStorage
import pl.stapik.calendar.data.config.ApiSchemaGuard
import pl.stapik.calendar.ui.about.AboutScreen
import pl.stapik.calendar.ui.calendar.WeekPagerScreen
import pl.stapik.calendar.ui.connect.ConnectScreen
import pl.stapik.calendar.ui.navigation.AppScreen
import pl.stapik.calendar.ui.theme.RetroColors

@Composable
fun AppRoot(apiConfigStorage: ApiConfigStorage, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val apiSchemaGuard = remember { ApiSchemaGuard(context.applicationContext, apiConfigStorage) }
    var schemaChecked by remember { mutableStateOf(false) }

    // Must run before any screen reads the stored config, otherwise a stale
    // pre-migration config could be used against the new API and fail with a
    // confusing raw network error instead of a clean redirect to Connect.
    LaunchedEffect(Unit) {
        apiSchemaGuard.ensureCurrentSchema()
        schemaChecked = true
    }

    if (!schemaChecked) {
        Box(
            modifier = modifier.fillMaxSize().background(RetroColors.WindowBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = RetroColors.TextDark)
        }
        return
    }

    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Calendar) }
    var menuExpanded by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        containerColor = RetroColors.WindowBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (screen == AppScreen.Calendar) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    FloatingActionButton(
                        onClick = { menuExpanded = true },
                        containerColor = RetroColors.CellBackground,
                        contentColor = RetroColors.TextDark
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_connect)) },
                            onClick = { menuExpanded = false; screen = AppScreen.Connect }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_about)) },
                            onClick = { menuExpanded = false; screen = AppScreen.About }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (screen) {
                AppScreen.Calendar -> WeekPagerScreen(
                    apiConfigStorage = apiConfigStorage,
                    onNavigateToConnect = { screen = AppScreen.Connect }
                )
                AppScreen.Connect -> ConnectScreen(storage = apiConfigStorage, onBack = { screen = AppScreen.Calendar })
                AppScreen.About -> AboutScreen(onBack = { screen = AppScreen.Calendar })
            }
        }
    }
}