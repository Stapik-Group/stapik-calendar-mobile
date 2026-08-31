package pl.stapik.calendar.ui.theme

import androidx.compose.ui.graphics.Color

object EntryColors {
    private val palette = mapOf(
        "default" to RetroColors.EntryBlue,
        "red" to Color(0xFF800000),
        "green" to Color(0xFF008000),
        "blue" to Color(0xFF000080),
        "yellow" to Color(0xFF808000),
        "purple" to Color(0xFF800080),
        "orange" to Color(0xFFFF8000)
    )

    fun forKey(key: String): Color = palette[key.lowercase()] ?: RetroColors.EntryBlue
}