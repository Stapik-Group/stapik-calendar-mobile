package pl.stapik.calendar.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import pl.stapik.calendar.R

@Composable
fun ConflictDialog(
    conflict: SyncConflict,
    onKeepLocal: () -> Unit,
    onUseServer: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.conflict_title)) },
        text = {
            Column {
                Text(
                    stringResource(
                        R.string.conflict_message,
                        conflict.localCount,
                        conflict.serverCount,
                        formatCachedTimestamp(conflict.serverUpdatedAt)
                    )
                )
                if (!conflict.canKeepLocal) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.conflict_read_only_hint))
                }
            }
        },
        confirmButton = {
            if (conflict.canKeepLocal) {
                TextButton(onClick = onKeepLocal) { Text(stringResource(R.string.conflict_keep_local)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onUseServer) { Text(stringResource(R.string.conflict_use_server)) }
        }
    )
}
