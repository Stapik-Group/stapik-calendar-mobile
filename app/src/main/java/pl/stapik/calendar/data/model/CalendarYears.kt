package pl.stapik.calendar.data.model

import java.time.LocalDate

object CalendarYears {
    private const val PARTITION_LENGTH = 4
    private val supportedYears = 1000..9999

    fun partitionOf(year: Int): String {
        require(year in supportedYears) { "Unsupported year: $year" }
        return year.toString()
    }

    fun yearOfPartition(partition: String): Int? =
        partition
            .takeIf { it.length == PARTITION_LENGTH && it.all(Char::isDigit) }
            ?.toInt()
            ?.takeIf { it in supportedYears }

    fun yearOf(entry: CalendarEntry): Int? =
        runCatching { LocalDate.parse(entry.date).year }.getOrNull()?.takeIf { it in supportedYears }

    fun split(entries: List<CalendarEntry>, fallbackYear: Int): Map<Int, List<CalendarEntry>> =
        entries.groupBy { yearOf(it) ?: fallbackYear }.toSortedMap()
}
