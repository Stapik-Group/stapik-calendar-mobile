package pl.stapik.calendar.ui.theme

import androidx.compose.ui.graphics.Color

data class EntryColorDef(val background: Color, val textColor: Color)

object EntryPalettes {
    private val white = Color(0xFFFFFFFF)

    // Classic's own text-on-color rule wasn't given to us directly — assumed
    // identical to Classic Pink (white on red/green/blue/purple, dark on
    // yellow/orange) since Classic Pink reads like a reskin of Classic.
    val Classic = build(
        default = ThemePalettes.Classic.accent,
        darkText = ThemePalettes.Classic.textDark,
        red = 0xFF800000, green = 0xFF008000, blue = 0xFF000080,
        yellow = 0xFF808000, purple = 0xFF800080, orange = 0xFFFF8000,
        whiteTextKeys = setOf("red", "green", "blue", "purple")
    )

    val ClassicPink = build(
        default = ThemePalettes.ClassicPink.accent,
        darkText = ThemePalettes.ClassicPink.textDark,
        red = 0xFF800000, green = 0xFF008000, blue = 0xFF000080,
        yellow = 0xFF808000, purple = 0xFF800080, orange = 0xFFFF8000,
        whiteTextKeys = setOf("red", "green", "blue", "purple")
    )

    val Modern = build(
        default = ThemePalettes.Modern.accent,
        darkText = ThemePalettes.Modern.textDark,
        red = 0xFFFF453A, green = 0xFF34C759, blue = 0xFF007AFF,
        yellow = 0xFFFFCC00, purple = 0xFFAF52DE, orange = 0xFFFF9500,
        whiteTextKeys = setOf("red", "green", "blue", "purple", "orange")
    )

    fun forTheme(theme: AppTheme): Map<String, EntryColorDef> = when (theme) {
        AppTheme.CLASSIC -> Classic
        AppTheme.MODERN -> Modern
        AppTheme.CLASSIC_PINK -> ClassicPink
    }

    private fun build(
        default: Color, darkText: Color,
        red: Long, green: Long, blue: Long, yellow: Long, purple: Long, orange: Long,
        whiteTextKeys: Set<String>
    ): Map<String, EntryColorDef> {
        fun textFor(key: String) = if (key in whiteTextKeys) white else darkText
        return mapOf(
            "default" to EntryColorDef(default, white),
            "red" to EntryColorDef(Color(red), textFor("red")),
            "green" to EntryColorDef(Color(green), textFor("green")),
            "blue" to EntryColorDef(Color(blue), textFor("blue")),
            "yellow" to EntryColorDef(Color(yellow), textFor("yellow")),
            "purple" to EntryColorDef(Color(purple), textFor("purple")),
            "orange" to EntryColorDef(Color(orange), textFor("orange"))
        )
    }
}