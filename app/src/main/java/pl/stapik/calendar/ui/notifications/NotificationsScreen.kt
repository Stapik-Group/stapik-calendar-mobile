package pl.stapik.calendar.ui.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pl.stapik.calendar.R
import pl.stapik.calendar.data.notifications.NotificationPreferencesStorage
import pl.stapik.calendar.notifications.NotificationChannels
import pl.stapik.calendar.ui.common.RetroScreenHeader
import pl.stapik.calendar.ui.theme.LocalThemeColors

@Composable
fun NotificationsScreen(
    storage: NotificationPreferencesStorage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeColors = LocalThemeColors.current
    val scope = rememberCoroutineScope()
    val enabled by storage.enabled.collectAsState(initial = false)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // If denied, the stored preference simply stays false - the Switch
        // reflects that automatically on next recomposition, no extra state needed.
        if (granted) scope.launch { storage.setEnabled(true) }
    }

    Column(modifier = modifier.fillMaxSize().background(themeColors.windowBackground)) {
        RetroScreenHeader(title = stringResource(R.string.menu_notifications), onBack = onBack)
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.notifications_toggle_label), color = themeColors.textDark)
            Switch(
                checked = enabled,
                onCheckedChange = { turnOn ->
                    if (turnOn) {
                        NotificationChannels.ensureCreated(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            scope.launch { storage.setEnabled(true) }
                        }
                    } else {
                        scope.launch { storage.setEnabled(false) }
                    }
                }
            )
        }
    }
}