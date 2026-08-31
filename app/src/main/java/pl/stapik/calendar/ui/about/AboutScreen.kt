package pl.stapik.calendar.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.stapik.calendar.BuildConfig
import pl.stapik.calendar.R
import pl.stapik.calendar.ui.common.RetroScreenHeader
import pl.stapik.calendar.ui.theme.LocalThemeColors
import pl.stapik.calendar.util.AppInfo

@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val themeColors = LocalThemeColors.current

    Column(modifier = modifier.fillMaxSize().background(themeColors.windowBackground)) {
        RetroScreenHeader(title = stringResource(R.string.menu_about), onBack = onBack)

        Column(modifier = Modifier.padding(16.dp)) {
            Text("Stapik Calendar", color = themeColors.textDark, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text("${stringResource(R.string.about_version_label)}: ${BuildConfig.VERSION_NAME}", color = themeColors.textDark)
            Text("${stringResource(R.string.about_author_label)}: ${AppInfo.AUTHOR}", color = themeColors.textDark)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.about_github_label),
                color = themeColors.accent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { uriHandler.openUri(AppInfo.GITHUB_URL) }
            )
        }
    }
}
