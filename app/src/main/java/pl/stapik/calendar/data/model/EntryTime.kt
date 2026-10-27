package pl.stapik.calendar.data.model

object EntryTime {
    private const val MAX_HOUR = 23
    private const val MAX_MINUTE = 59
    private const val PART_LENGTH = 2
    private val compactLengths = 3..4

    fun normalize(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""
        val separator = trimmed.indexOfAny(charArrayOf(':', '.'))
        val (hourPart, minutePart) = when {
            separator >= 0 -> trimmed.substring(0, separator) to trimmed.substring(separator + 1)
            trimmed.all(::isAsciiDigit) && trimmed.length in compactLengths ->
                trimmed.dropLast(PART_LENGTH) to trimmed.takeLast(PART_LENGTH)
            else -> return null
        }
        if (!isNumber(hourPart) || !isNumber(minutePart)) return null
        if (hourPart.length > PART_LENGTH || minutePart.length != PART_LENGTH) return null
        val hour = hourPart.toInt()
        val minute = minutePart.toInt()
        if (hour > MAX_HOUR || minute > MAX_MINUTE) return null
        return "${hour.toString().padStart(PART_LENGTH, '0')}:${minute.toString().padStart(PART_LENGTH, '0')}"
    }

    private fun isNumber(text: String): Boolean = text.isNotEmpty() && text.all(::isAsciiDigit)

    private fun isAsciiDigit(c: Char): Boolean = c in '0'..'9'
}
