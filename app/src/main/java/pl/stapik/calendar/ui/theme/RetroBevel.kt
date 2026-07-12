package pl.stapik.calendar.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


fun Modifier.retroBevel(raised: Boolean = true, thickness: Dp = 2.dp): Modifier = this.drawBehind {
    val stroke = thickness.toPx()
    val topLeftColor = if (raised) RetroColors.BevelLight else RetroColors.BevelDark
    val bottomRightColor = if (raised) RetroColors.BevelDark else RetroColors.BevelLight

    drawLine(topLeftColor, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
    drawLine(topLeftColor, Offset(stroke / 2, 0f), Offset(stroke / 2, size.height), stroke)
    drawLine(bottomRightColor, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
    drawLine(bottomRightColor, Offset(size.width - stroke / 2, 0f), Offset(size.width - stroke / 2, size.height), stroke)
}
