package com.jiitkiosk.app.ui.calendar

import com.jiitkiosk.app.calendar.CalEvent
import com.jiitkiosk.app.ui.exams.Paper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import `in`.codelif.ktjiit.model.ExamSlot
import `in`.codelif.ktjiit.model.Semester

/**
 * Board 05's rows: what the calendar shows for each source, and which
 * registration today's exam fetch is asked about. Nothing is invented — an
 * undated paper drops, a blank note stays blank.
 */
class CalendarRowsTest {

    @Test
    fun `an exam paper becomes a dated row with its subject, event and time`() {
        val rows = dayRows(
            listOf(
                Paper(
                    "Test-2",
                    ExamSlot(date = "16/10/2026", from = "1:00 pm", subject = "Image Processing"),
                ),
            ),
            emptyList(),
            emptyList(),
        )

        assertEquals(1, rows.size)
        assertEquals(LocalDate.of(2026, 10, 16), rows[0].from)
        assertEquals("Image Processing · Test-2", rows[0].title)
        assertEquals("1:00 PM", rows[0].note)
        assertEquals(LocalTime.of(13, 0), rows[0].at)
    }

    @Test
    fun `undated papers drop and a blank subject falls back to the code`() {
        val rows = dayRows(
            listOf(
                Paper("Test-2", ExamSlot(date = "", from = "1:00 pm")),
                Paper("", ExamSlot(date = "19/10/2026", code = "ACS", subject = "")),
            ),
            emptyList(),
            emptyList(),
        )

        assertEquals(1, rows.size)
        assertEquals("ACS", rows[0].title)
        assertNull(rows[0].note)
    }

    @Test
    fun `a span gets its range note while single days and user rows stay blank`() {
        val rows = dayRows(
            emptyList(),
            listOf(
                CalEvent(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 16), "Muharram holiday", "holiday"),
                CalEvent(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1), "Orientation", "event"),
            ),
            listOf(CalEvent(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 3), "Buy books", "yours")),
        )

        assertEquals("15 AUG — 16 AUG", rows[0].note)
        assertNull(rows[1].note)
        assertNull(rows[2].note)
        assertEquals("yours", rows[2].kind)
    }

    @Test
    fun `the registration asked about is the one today falls in, else the nearest`() {
        val zone = ZoneId.systemDefault()
        fun sem(id: String, from: LocalDate, to: LocalDate) = Semester(
            id = id,
            code = id,
            from = from.atStartOfDay(zone).toInstant().toEpochMilli(),
            to = to.atStartOfDay(zone).toInstant().toEpochMilli(),
        )
        val odd = sem("odd", LocalDate.of(2026, 7, 23), LocalDate.of(2026, 11, 28))
        val even = sem("even", LocalDate.of(2027, 1, 4), LocalDate.of(2027, 5, 12))

        assertEquals("odd", portalSemester(listOf(odd, even), LocalDate.of(2026, 10, 17))?.id)
        // between two windows today sits nearest the next one
        assertEquals("even", portalSemester(listOf(odd, even), LocalDate.of(2026, 12, 20))?.id)
        // no dates anywhere: the first registration is the only honest answer
        assertEquals(
            "odd",
            portalSemester(listOf(Semester("odd", "odd"), Semester("even", "even")), LocalDate.of(2026, 10, 17))?.id,
        )
        assertNull(portalSemester(emptyList(), LocalDate.of(2026, 10, 17)))
    }
}
