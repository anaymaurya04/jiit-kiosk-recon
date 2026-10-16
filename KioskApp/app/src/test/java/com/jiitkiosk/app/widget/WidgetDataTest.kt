package com.jiitkiosk.app.widget

import com.jiitkiosk.app.classroom.Work
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetDataTest {

    private fun work(id: String, due: Long?, submitted: Boolean?) =
        Work(id = id, courseId = "c1", courseName = "DBMS", title = "Assignment", dueAt = due, submitted = submitted)

    @Test
    fun percentAveragesLikeTheHero() {
        assertEquals(79, percentOf(listOf(78.6, 79.4)))
    }

    @Test
    fun noPercentsMeansNoNumber() {
        assertNull(percentOf(emptyList()))
    }

    @Test
    fun nextIsTheEarliestUnsubmittedFutureDue() {
        val now = 1_000_000L
        val soon = work("soon", now + 3_600_000, false)
        val later = work("later", now + 86_400_000, false)
        assertEquals(soon.id, nextOf(listOf(later, soon), now)?.id)
    }

    @Test
    fun submittedUndatedAndPastWorkIsIgnored() {
        val now = 1_000_000L
        val works = listOf(
            work("done", now + 100, true),
            work("undated", null, false),
            work("past", now - 100, false),
        )
        assertNull(nextOf(works, now))
    }

    @Test
    fun countdownMatchesTheHeroesShape() {
        val hour = 3_600_000L
        assertEquals("2D 4H", countdownText(52 * hour))
        assertEquals("5H 30M", countdownText(5 * hour + 30 * 60_000))
        assertEquals("45M", countdownText(45 * 60_000))
    }
}
