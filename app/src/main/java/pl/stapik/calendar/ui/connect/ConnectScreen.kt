package pl.stapik.calendar.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.stapik.calendar.data.config.ApiConfigStorage
import pl.stapik.calendar.ui.common.RetroScreenHeader
import pl.stapik.calendar.ui.theme.RetroColors
import pl.stapik.calendar.R
import pl.stapik.calendar.ui.theme.retroBevel

@Composable
fun ConnectScreen(
    storage: ApiConfigStorage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectViewModel = viewModel(factory = remember { ConnectViewModelFactory(storage) })
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().background(RetroColors.WindowBackground)) {
        RetroScreenHeader(title = stringResource(R.string.menu_connect), onBack = onBack)

        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.connect_url_label), color = RetroColors.TextDark)
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = viewModel::onBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("https://twoj-serwer.pl") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(stringResource(R.string.connect_key_label), color = RetroColors.TextDark)
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = viewModel::onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .background(RetroColors.CellBackground)
                    .retroBevel(raised = true)
                    .clickable(onClick = viewModel::onSave)
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(stringResource(R.string.connect_save), color = RetroColors.TextDark, fontWeight = FontWeight.Bold)
            }

            if (state.isSaved) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.connect_saved_confirmation), color = RetroColors.TextDark)
            }
        }
    }
}
