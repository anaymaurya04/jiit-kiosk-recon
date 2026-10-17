package com.jiitkiosk.app.widget

import com.jiitkiosk.app.classroom.Work
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** the widget formatters, pinned to the strings the boards print */
class WidgetFormatTest {

    @Test
    fun `subject codes take the significant initials`() {
        assertEquals("FDA", subjectCode("Foundations of Data Analytics"))
        assertEquals("SE", subjectCode("Software Engineering"))
        assertEquals("MAT", subjectCode("Math"))
    }

    @Test
    fun `inline countdown collapses to one unit`() {
        assertEquals("5H", inlineCountdown(5 * 3_600_000L + 10 * 60_000L))
        assertEquals("45M", inlineCountdown(45 * 60_000L))
        assertEquals("2D", inlineCountdown(52 * 3_600_000L))
    }

    @Test
    fun `pending rows put overdue first and keep undated last`() {
        val now = 1_000_000L
        val work = listOf(
            work("later", now + 3_600_000L, false),
            work("done", now - 5_000L, true),
            work("overdue", now - 1_000L, false),
            work("undated", null, false),
        )
        assertEquals(listOf("overdue", "later", "undated"), pendingRows(work, now).map { it.id })
        assertEquals(3 to 1, pendingCounts(work, now))
    }

    @Test
    fun `sync line follows expiry, staleness and the empty case`() {
        val now = 10_000_000_000L
        assertEquals("SYNCED 4 MIN AGO" to false, syncLine(now - 4 * 60_000L, false, now))
        assertEquals("SYNCED 7H AGO" to true, syncLine(now - 7 * 3_600_000L, false, now))
        assertEquals("SHOWING DATA FROM 3H AGO" to true, syncLine(now - 3 * 3_600_000L, true, now))
        assertEquals("NOT SYNCED" to false, syncLine(null, false, now))
    }

    @Test
    fun `hours floor, and the more-today line reads like the board`() {
        assertEquals("3H", hoursText(180))
        assertEquals("<1H", hoursText(40))
        assertEquals("+1 MORE TODAY · 3H OF CLASSES", moreToday(1, 180))
    }

    @Test
    fun `day heading reads like the board`() {
        assertEquals("FRI 16 OCT", dayHeading(LocalDate.of(2026, 10, 16)))
    }

    private fun work(id: String, due: Long?, submitted: Boolean) = Work(
        id = id,
        courseId = "course",
        courseName = "Course",
        title = id,
        dueAt = due,
        submitted = submitted,
    )
}
