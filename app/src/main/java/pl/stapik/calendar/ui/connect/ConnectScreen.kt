package pl.stapik.calendar.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.stapik.calendar.R
import pl.stapik.calendar.data.config.ApiConfigStorage
import pl.stapik.calendar.ui.common.RetroScreenHeader
import pl.stapik.calendar.ui.theme.LocalThemeColors
import pl.stapik.calendar.ui.theme.themedSurface

@Composable
fun ConnectScreen(
    storage: ApiConfigStorage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectViewModel = viewModel(factory = remember { ConnectViewModelFactory(storage) })
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val themeColors = LocalThemeColors.current
    Column(modifier = modifier.fillMaxSize().background(themeColors.windowBackground)) {
        RetroScreenHeader(title = stringResource(R.string.menu_connect), onBack = onBack)
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.connect_url_label), color = themeColors.textDark)
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = viewModel::onBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("https://twoj-serwer.pl") }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.connect_key_label), color = themeColors.textDark)
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
                    .themedSurface(themeColors = themeColors, backgroundColor = themeColors.cellBackground, raised = true)
                    .clickable(enabled = !state.isTesting, onClick = viewModel::onSave)
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                if (state.isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = themeColors.textDark)
                } else {
                    Text(stringResource(R.string.connect_save), color = themeColors.textDark, fontWeight = FontWeight.Bold)
                }
            }
            when (val result = state.testResult) {
                is ConnectTestResult.Success -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.connect_test_success, result.keyLabel ?: result.scope),
                        color = themeColors.textDark
                    )
                    if (result.scope != "READ_ONLY") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.connect_scope_warning),
                            color = Color(0xFFB00020)
                        )
                    }
                }
                is ConnectTestResult.Error -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(result.message, color = Color(0xFFB00020))
                }
                null -> Unit
            }
        }
    }
}