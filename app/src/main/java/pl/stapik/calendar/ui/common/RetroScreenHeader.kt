package pl.stapik.calendar.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.stapik.calendar.ui.theme.RetroColors
import pl.stapik.calendar.ui.theme.retroBevel

@Composable
fun RetroScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(RetroColors.WindowBackground)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(RetroColors.CellBackground)
                .retroBevel(raised = true)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text("◀", color = RetroColors.TextDark)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .background(RetroColors.HeaderBlue)
                .retroBevel(raised = false)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(title, color = RetroColors.TextOnBlue, fontWeight = FontWeight.Bold)
        }
    }
}
