package pl.stapik.calendar.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import pl.stapik.calendar.R
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.ui.theme.LocalEntryPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditDialog(
    initial: CalendarEntry?,
    initialDate: LocalDate,
    onSave: (CalendarEntry) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val entryPalette = LocalEntryPalette.current
    val uriHandler = LocalUriHandler.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var link by remember { mutableStateOf(initial?.link.orEmpty()) }
    var color by remember { mutableStateOf(initial?.color ?: "default") }
    var date by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.entry_dialog_add_title else R.string.entry_dialog_edit_title
                )
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.entry_name_label)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.entry_link_label)) }
                )
                if (link.isNotBlank()) {
                    TextButton(onClick = { runCatching { uriHandler.openUri(link.trim()) } }) {
                        Text(stringResource(R.string.entry_open_link))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.entry_date_label), fontWeight = FontWeight.Bold)
                Text(
                    text = dateFormatter.format(date),
                    modifier = Modifier
                        .clickable { showDatePicker = true }
                        .padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.entry_color_label), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    entryPalette.entries.forEach { (key, definition) ->
                        val selectedModifier = if (key == color) {
                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface)
                        } else {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.outline)
                        }
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(32.dp)
                                .background(definition.background)
                                .then(selectedModifier)
                                .clickable { color = key }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        CalendarEntry(
                            date = date.toString(),
                            name = name.trim(),
                            link = link.trim(),
                            color = color
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.button_save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.button_delete)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_cancel)) }
            }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.button_cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
