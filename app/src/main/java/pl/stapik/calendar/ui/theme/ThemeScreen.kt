package pl.stapik.calendar.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pl.stapik.calendar.R
import pl.stapik.calendar.data.theme.ThemeStorage
import pl.stapik.calendar.ui.common.RetroScreenHeader

@Composable
fun ThemeScreen(storage: ThemeStorage, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val currentTheme by storage.theme.collectAsState(initial = AppTheme.CLASSIC)
    val themeColors = LocalThemeColors.current
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().background(themeColors.windowBackground)) {
        RetroScreenHeader(title = stringResource(R.string.menu_theme), onBack = onBack)
        Column(modifier = Modifier.padding(16.dp)) {
            AppTheme.entries.forEach { theme ->
                val label = when (theme) {
                    AppTheme.CLASSIC -> stringResource(R.string.theme_classic)
                    AppTheme.MODERN -> stringResource(R.string.theme_modern)
                    AppTheme.CLASSIC_PINK -> stringResource(R.string.theme_classic_pink)
                }
                val isSelected = theme == currentTheme
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .themedSurface(
                            themeColors = themeColors,
                            backgroundColor = if (isSelected) themeColors.accent else themeColors.cellBackground,
                            raised = !isSelected
                        )
                        .clickable { scope.launch { storage.setTheme(theme) } }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) themeColors.textOnAccent else themeColors.textDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}