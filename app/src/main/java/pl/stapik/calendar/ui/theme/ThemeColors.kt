package pl.stapik.calendar.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ThemeColors(
    val windowBackground: Color,
    val cellBackground: Color,
    val cellBackgroundSecondary: Color,
    val accent: Color,
    val todayBackground: Color,
    val todayText: Color,
    val textOnAccent: Color,
    val textDark: Color,
    val textMuted: Color,
    val borderLight: Color,
    val borderDark: Color,
    val usesBevel: Boolean,
    val cornerRadius: Dp,
    val isDark: Boolean = false
)

object ThemePalettes {
    val Classic = ThemeColors(
        windowBackground = Color(0xFFECECEC),
        cellBackground = Color(0xFFC0C0C0),
        cellBackgroundSecondary = Color(0xFFD4D4D4),
        accent = Color(0xFF000080),
        todayBackground = Color(0xFF000080),
        todayText = Color(0xFFFFFFFF),
        textOnAccent = Color(0xFFFFFFFF),
        textDark = Color(0xFF202020),
        textMuted = Color(0xFF808080),
        borderLight = Color(0xFFFFFFFF),
        borderDark = Color(0xFF808080),
        usesBevel = true,
        cornerRadius = 0.dp
    )

    val ClassicPink = ThemeColors(
        windowBackground = Color(0xFFF4B6DD),
        cellBackground = Color(0xFFF4B6DD),
        cellBackgroundSecondary = Color(0xFFF4B6DD),
        accent = Color(0xFF7B2FF7),
        todayBackground = Color(0xFF00E5FF),
        todayText = Color(0xFF1A1A2E),
        textOnAccent = Color(0xFFFFFFFF),
        textDark = Color(0xFF4A0E4E),
        textMuted = Color(0xFF4A0E4E),
        borderLight = Color(0xFFFFE0F3),
        borderDark = Color(0xFF8C3A72),
        usesBevel = true,
        cornerRadius = 0.dp
    )

    val Modern = ThemeColors(
        windowBackground = Color(0xFFF5F5F7),
        cellBackground = Color(0xFFFFFFFF),
        cellBackgroundSecondary = Color(0xFFF5F5F7),
        accent = Color(0xFF007AFF),
        todayBackground = Color(0xFFEAF2FF),
        todayText = Color(0xFF007AFF),
        textOnAccent = Color(0xFFFFFFFF),
        textDark = Color(0xFF1C1C1E),
        textMuted = Color(0xFF6E6E73),
        borderLight = Color(0xFFDCDCE0),
        borderDark = Color(0xFFE2E2E5),
        usesBevel = false,
        cornerRadius = 12.dp
    )

    val Dark = ThemeColors(
        windowBackground = Color(0xFF000000),
        cellBackground = Color(0xFF1C1C1E),
        cellBackgroundSecondary = Color(0xFF111113),
        accent = Color(0xFF0A84FF),
        todayBackground = Color(0xFF0A2A4D),
        todayText = Color(0xFF64B5FF),
        textOnAccent = Color(0xFFFFFFFF),
        textDark = Color(0xFFF2F2F7),
        textMuted = Color(0xFF8E8E93),
        borderLight = Color(0xFF38383A),
        borderDark = Color(0xFF2C2C2E),
        usesBevel = false,
        cornerRadius = 12.dp,
        isDark = true
    )

    fun forTheme(theme: AppTheme): ThemeColors = when (theme) {
        AppTheme.CLASSIC -> Classic
        AppTheme.MODERN -> Modern
        AppTheme.CLASSIC_PINK -> ClassicPink
        AppTheme.DARK -> Dark
    }
}

fun ThemeColors.toMaterialColorScheme(): ColorScheme = if (isDark) {
    darkColorScheme(
        primary = accent,
        onPrimary = textOnAccent,
        background = windowBackground,
        onBackground = textDark,
        surface = cellBackground,
        onSurface = textDark,
        surfaceVariant = cellBackgroundSecondary,
        onSurfaceVariant = textMuted,
        outline = borderLight,
        outlineVariant = borderDark
    )
} else {
    lightColorScheme()
}
