package pl.stapik.calendar.ui.theme

import androidx.compose.ui.graphics.Color

data class EntryColorDef(val background: Color, val textColor: Color)

object EntryPalettes {
    private val white = Color(0xFFFFFFFF)

    val Classic = build(
        default = ThemePalettes.Classic.accent,
        darkText = ThemePalettes.Classic.textDark,
        red = 0xFF800000, green = 0xFF008000, blue = 0xFF000080,
        yellow = 0xFF808000, purple = 0xFF800080, orange = 0xFFFF8000,
        teal = 0xFF008080, pink = 0xFFFF80C0, brown = 0xFF804000, gray = 0xFF808080,
        whiteTextKeys = setOf("red", "green", "blue", "purple", "teal", "brown", "gray")
    )

    val ClassicPink = build(
        default = ThemePalettes.ClassicPink.accent,
        darkText = ThemePalettes.ClassicPink.textDark,
        red = 0xFF800000, green = 0xFF008000, blue = 0xFF000080,
        yellow = 0xFF808000, purple = 0xFF800080, orange = 0xFFFF8000,
        teal = 0xFF008080, pink = 0xFFFF80C0, brown = 0xFF804000, gray = 0xFF808080,
        whiteTextKeys = setOf("red", "green", "blue", "purple", "teal", "brown", "gray")
    )

    val Modern = build(
        default = ThemePalettes.Modern.accent,
        darkText = ThemePalettes.Modern.textDark,
        red = 0xFFFF453A, green = 0xFF34C759, blue = 0xFF007AFF,
        yellow = 0xFFFFCC00, purple = 0xFFAF52DE, orange = 0xFFFF9500,
        teal = 0xFF30B0C7, pink = 0xFFFF2D55, brown = 0xFFA2845E, gray = 0xFF8E8E93,
        whiteTextKeys = setOf("red", "green", "blue", "purple", "orange", "teal", "pink", "brown", "gray")
    )

    fun forTheme(theme: AppTheme): Map<String, EntryColorDef> = when (theme) {
        AppTheme.CLASSIC -> Classic
        AppTheme.MODERN -> Modern
        AppTheme.CLASSIC_PINK -> ClassicPink
    }

    private fun build(
        default: Color, darkText: Color,
        red: Long, green: Long, blue: Long, yellow: Long, purple: Long, orange: Long,
        teal: Long, pink: Long, brown: Long, gray: Long,
        whiteTextKeys: Set<String>
    ): Map<String, EntryColorDef> {
        fun textFor(key: String) = if (key in whiteTextKeys) white else darkText
        return mapOf(
            "default" to EntryColorDef(default, white),
            "red" to EntryColorDef(Color(red), textFor("red")),
            "orange" to EntryColorDef(Color(orange), textFor("orange")),
            "yellow" to EntryColorDef(Color(yellow), textFor("yellow")),
            "green" to EntryColorDef(Color(green), textFor("green")),
            "teal" to EntryColorDef(Color(teal), textFor("teal")),
            "blue" to EntryColorDef(Color(blue), textFor("blue")),
            "purple" to EntryColorDef(Color(purple), textFor("purple")),
            "pink" to EntryColorDef(Color(pink), textFor("pink")),
            "brown" to EntryColorDef(Color(brown), textFor("brown")),
            "gray" to EntryColorDef(Color(gray), textFor("gray"))
        )
    }
}