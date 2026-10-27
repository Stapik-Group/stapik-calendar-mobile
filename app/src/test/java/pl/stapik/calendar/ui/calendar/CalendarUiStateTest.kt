package pl.stapik.calendar.ui.calendar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.stapik.calendar.data.model.CalendarEntry
import pl.stapik.calendar.data.model.CalendarPayload
import pl.stapik.common.presentation.DocumentUiState
import pl.stapik.common.presentation.LoadErrorKind

class CalendarUiStateTest {
    private val dentist = CalendarEntry("dentist", "2026-01-02", "Dentist")
    private val gym = CalendarEntry("gym", "2026-01-02", "Gym")
    private val party = CalendarEntry("party", "2026-01-05", "Party")

    @Test
    fun aLoadingDocumentIsLoading() {
        assertEquals(CalendarUiState.Loading, DocumentUiState.Loading.toCalendarUiState(canEdit = true))
    }

    @Test
    fun aLoadErrorKeepsItsKind() {
        val state = DocumentUiState.Error(LoadErrorKind.CORRUPTED).toCalendarUiState(canEdit = true)

        assertEquals(CalendarUiState.Error(LoadErrorKind.CORRUPTED), state)
    }

    @Test
    fun entriesAreGroupedByTheirDayInStoredOrder() {
        val payload = CalendarPayload(entries = listOf(dentist, party, gym))

        val state = DocumentUiState.Success(payload).toCalendarUiState(canEdit = true) as CalendarUiState.Success

        assertEquals(listOf(dentist, gym), state.entriesByDay.getValue(LocalDate.parse("2026-01-02")))
        assertEquals(listOf(party), state.entriesByDay.getValue(LocalDate.parse("2026-01-05")))
    }

    @Test
    fun entriesOfADayAreOrderedByTheirTime() {
        val late = CalendarEntry("late", "2026-01-02", "Late", time = "18:00")
        val early = CalendarEntry("early", "2026-01-02", "Early", time = "08:15")
        val payload = CalendarPayload(entries = listOf(dentist, late, early))

        val state = DocumentUiState.Success(payload).toCalendarUiState(canEdit = true) as CalendarUiState.Success

        assertEquals(listOf(early, late, dentist), state.entriesByDay.getValue(LocalDate.parse("2026-01-02")))
    }

    @Test
    fun entriesWithAnUnreadableDateAreNotShown() {
        val payload = CalendarPayload(entries = listOf(dentist, CalendarEntry("broken", "not a date", "Broken")))

        val state = DocumentUiState.Success(payload).toCalendarUiState(canEdit = true) as CalendarUiState.Success

        assertEquals(mapOf(LocalDate.parse("2026-01-02") to listOf(dentist)), state.entriesByDay)
    }

    @Test
    fun editingFollowsThePermission() {
        val document = DocumentUiState.Success(CalendarPayload())

        assertTrue((document.toCalendarUiState(canEdit = true) as CalendarUiState.Success).canEdit)
        assertFalse((document.toCalendarUiState(canEdit = false) as CalendarUiState.Success).canEdit)
    }
}
