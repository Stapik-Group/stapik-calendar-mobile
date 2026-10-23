package pl.stapik.calendar.ui.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import pl.stapik.calendar.R
import pl.stapik.calendar.data.notifications.NotificationPreferencesStorage
import pl.stapik.calendar.notifications.NotificationChannels
import pl.stapik.common.ui.components.StapikScreen

@Composable
fun NotificationsScreen(
    storage: NotificationPreferencesStorage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enabled by storage.enabled.collectAsState(initial = false)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // If denied, the stored preference simply stays false - the Switch
        // reflects that automatically on next recomposition, no extra state needed.
        if (granted) scope.launch { storage.setEnabled(true) }
    }

    StapikScreen(
        title = stringResource(R.string.menu_notifications),
        onBack = onBack,
        backDescription = stringResource(pl.stapik.common.i18n.R.string.action_back),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.notifications_toggle_label),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
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
