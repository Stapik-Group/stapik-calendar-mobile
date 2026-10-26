package pl.stapik.calendar.ui.conflicts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import pl.stapik.calendar.R
import pl.stapik.common.sync.ConflictSource
import pl.stapik.common.ui.components.StapikButton
import pl.stapik.common.ui.components.StapikScreen
import pl.stapik.common.ui.components.themedSurface
import pl.stapik.common.ui.theme.StapikTheme

@Composable
fun ConflictCopiesScreen(
    viewModelFactory: ViewModelProvider.Factory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ConflictCopiesViewModel = viewModel(factory = viewModelFactory)
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val textColor = MaterialTheme.colorScheme.onBackground

    LaunchedEffect(Unit) { viewModel.reload() }

    StapikScreen(
        title = stringResource(R.string.menu_conflict_copies),
        onBack = onBack,
        backDescription = stringResource(pl.stapik.common.i18n.R.string.action_back),
        modifier = modifier
    ) {
        val current = rows
        when {
            current == null -> Text(stringResource(R.string.loading), color = textColor)
            current.isEmpty() -> Text(stringResource(R.string.conflict_copies_empty), color = textColor)
            else -> {
                Text(stringResource(R.string.conflict_copies_hint), color = textColor)
                current.forEach { row ->
                    ConflictCopyCard(row = row, onAddMissing = { viewModel.addMissingEntries(row) })
                }
            }
        }
    }
}

@Composable
private fun ConflictCopyCard(row: ConflictCopyRow, onAddMissing: () -> Unit) {
    val colors = StapikTheme.colors
    val textColor = MaterialTheme.colorScheme.onBackground
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .themedSurface(colors, colors.button, raised = false)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${stringResource(row.copy.source.labelRes())} · ${formatCopyTime(row.copy.createdAt)}",
            color = textColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.conflict_copy_summary, row.copy.entries.size, row.missingCount),
            color = textColor
        )
        StapikButton(
            text = stringResource(R.string.conflict_copy_add_missing),
            onClick = onAddMissing,
            enabled = row.missingCount > 0,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun ConflictSource.labelRes(): Int = when (this) {
    ConflictSource.LOCAL -> R.string.conflict_copy_source_local
    ConflictSource.CLOUD -> R.string.conflict_copy_source_cloud
}

private fun formatCopyTime(createdAt: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(createdAt)
