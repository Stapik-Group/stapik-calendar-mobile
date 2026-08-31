package pl.stapik.calendar.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Classic/Classic Pink keep the raised Win98 bevel look; Modern gets a flat
// rounded card with a soft shadow instead, matching its desktop CSS
// (border-radius + box-shadow, no 3D border).
fun Modifier.themedSurface(
    themeColors: ThemeColors,
    backgroundColor: Color,
    raised: Boolean = true
): Modifier = if (themeColors.usesBevel) {
    this.background(backgroundColor).retroBevel(raised = raised)
} else {
    this
        .shadow(elevation = if (raised) 2.dp else 0.dp, shape = RoundedCornerShape(themeColors.cornerRadius))
        .clip(RoundedCornerShape(themeColors.cornerRadius))
        .background(backgroundColor)
}