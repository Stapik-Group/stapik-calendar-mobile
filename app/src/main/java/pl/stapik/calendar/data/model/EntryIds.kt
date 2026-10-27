package pl.stapik.calendar.data.model

import java.security.MessageDigest
import kotlin.random.Random

object EntryIds {
    private const val LENGTH = 16
    private const val RADIX = 16
    private const val FIELD_SEPARATOR = "\u0000"
    private const val ATTEMPT_SEPARATOR = "#"
    private const val DIGEST_ALGORITHM = "SHA-256"

    fun generate(random: Random = Random.Default): String = format(random.nextLong())

    fun contentId(entry: CalendarEntry): String =
        derive(
            listOf(entry.date, entry.name, entry.link, entry.color, entry.location, entry.time)
                .joinToString(FIELD_SEPARATOR)
        )

    fun withUniqueIds(entries: List<CalendarEntry>): List<CalendarEntry> {
        val taken = HashSet<String>()
        return entries.map { entry ->
            val id = firstFree(entry.id, taken)
            taken += id
            if (id == entry.id) entry else entry.copy(id = id)
        }
    }

    private fun firstFree(id: String, taken: Set<String>): String {
        var candidate = id
        var attempt = 0
        while (candidate in taken) {
            attempt++
            candidate = derive("$id$ATTEMPT_SEPARATOR$attempt")
        }
        return candidate
    }

    private fun derive(seed: String): String {
        val digest = MessageDigest.getInstance(DIGEST_ALGORITHM).digest(seed.toByteArray(Charsets.UTF_8))
        val leading = digest.take(Long.SIZE_BYTES).fold(0L) { acc, byte ->
            (acc shl Byte.SIZE_BITS) or (byte.toLong() and 0xFFL)
        }
        return format(leading)
    }

    private fun format(value: Long): String = value.toULong().toString(RADIX).padStart(LENGTH, '0')
}
