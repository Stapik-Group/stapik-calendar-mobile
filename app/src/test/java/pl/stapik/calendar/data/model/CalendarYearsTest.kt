package pl.stapik.calendar.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CalendarYearsTest {
    private val dentist = CalendarEntry("2026-01-02", "Dentist")
    private val gym = CalendarEntry("2025-12-31", "Gym")
    private val broken = CalendarEntry("not a date", "Broken")

    @Test
    fun aPartitionIsTheFourDigitYear() {
        assertEquals("2026", CalendarYears.partitionOf(2026))
    }

    @Test
    fun aYearOutsideFourDigitsHasNoPartition() {
        assertThrows(IllegalArgumentException::class.java) { CalendarYears.partitionOf(999) }
        assertThrows(IllegalArgumentException::class.java) { CalendarYears.partitionOf(10000) }
    }

    @Test
    fun onlyFourDigitPartitionsAreYears() {
        assertEquals(2026, CalendarYears.yearOfPartition("2026"))
        listOf("202", "20266", "abcd", "0999", "20 6", "").forEach { assertNull(it, CalendarYears.yearOfPartition(it)) }
    }

    @Test
    fun anEntryBelongsToTheYearOfItsDate() {
        assertEquals(2026, CalendarYears.yearOf(dentist))
        assertEquals(2025, CalendarYears.yearOf(gym))
    }

    @Test
    fun anEntryWithoutAReadableDateHasNoYear() {
        assertNull(CalendarYears.yearOf(broken))
        assertNull(CalendarYears.yearOf(CalendarEntry("2026-02-30", "Impossible")))
    }

    @Test
    fun splittingGroupsEntriesByYearInOrder() {
        val split = CalendarYears.split(listOf(dentist, gym, dentist.copy(name = "Barber")), fallbackYear = 2000)

        assertEquals(listOf(2025, 2026), split.keys.toList())
        assertEquals(listOf("Dentist", "Barber"), split.getValue(2026).map { it.name })
    }

    @Test
    fun entriesWithoutAYearGoToTheFallbackYear() {
        val split = CalendarYears.split(listOf(broken, dentist), fallbackYear = 2030)

        assertEquals(listOf(broken), split.getValue(2030))
        assertEquals(listOf(dentist), split.getValue(2026))
    }

    @Test
    fun theSlotOfAYearIsAPartitionOfTheCalendar() {
        val slot = CalendarDocument.slotOf(2026)

        assertEquals(CalendarDocument.slot.value, slot.value)
        assertEquals("2026", slot.partition)
    }
}
