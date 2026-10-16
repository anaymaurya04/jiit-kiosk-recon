package com.jiitkiosk.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The planner's promises: one ping per chosen lead, quiet hours push rather
 * than silence, nothing is ever said about work that is submitted, unknown,
 * undated or muted, and a ping that would land after the deadline does not
 * happen at all.
 */
class ReminderPlannerTest {

    private val ist = ZoneId.of("Asia/Kolkata")

    /** the planner only ever sees instants after this */
    private val now: Long = LocalDateTime.of(2030, 1, 1, 0, 0).atZone(ist).toInstant().toEpochMilli()

    private fun values(
        enabled: Boolean = true,
        leads: Set<Int> = setOf(24, 6, 1),
        quiet: Boolean = true,
        off: Set<String> = emptySet(),
    ) = ReminderPrefs.Values(enabled, leads, quiet, 22 * 60, 7 * 60, off)

    private fun work(
        id: String = "w1",
        courseId: String = "c1",
        due: LocalDateTime? = null,
        submitted: Boolean? = false,
        zone: ZoneId = ist,
    ) = ReminderWork(
        id = id,
        courseId = courseId,
        courseName = "Software Engineering",
        title = "Assignment 1",
        dueAt = due?.atZone(zone)?.toInstant()?.toEpochMilli(),
        submitted = submitted,
        link = null,
    )

    private fun local(at: Long, zone: ZoneId = ist): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(at), zone)

    @Test
    fun `quiet off gives all three leads at their true instants`() {
        val due = LocalDateTime.of(2030, 3, 10, 12, 0)
        val plan = ReminderPlanner.plan(listOf(work(due = due)), values(quiet = false), now, ist)

        assertEquals(
            listOf(24 to due.minusHours(24), 6 to due.minusHours(6), 1 to due.minusHours(1)),
            plan.map { it.leadHours to local(it.fireAt) },
        )
    }

    @Test
    fun `a ping inside quiet hours moves to 07, and is dropped when that passes the deadline`() {
        // due 06:30: the 24h ping lands 06:30 the day before (quiet) and moves to 07:00
        // that morning; the 6h and 1h pings move to 07:00 of the due day, which is
        // already past 06:30, so neither of them may fire
        val due = LocalDateTime.of(2030, 3, 10, 6, 30)
        val plan = ReminderPlanner.plan(listOf(work(due = due)), values(), now, ist)

        assertEquals(1, plan.size)
        assertEquals(24, plan[0].leadHours)
        assertEquals(LocalDateTime.of(2030, 3, 9, 7, 0), local(plan[0].fireAt))
    }

    @Test
    fun `the quiet shift is wall-clock correct on a DST fall-back day`() {
        val ny = ZoneId.of("America/New_York")
        // due Mon 2 Nov 2026 05:00; its 24h ping lands Sun 1 Nov 05:00 local — the
        // day the clocks go back — and must shift to that day's 07:00 wall clock
        val due = LocalDateTime.of(2026, 11, 2, 5, 0)
        val plan = ReminderPlanner.plan(
            listOf(work(due = due, zone = ny)),
            values(),
            LocalDateTime.of(2026, 10, 1, 0, 0).atZone(ny).toInstant().toEpochMilli(),
            ny,
        )

        assertEquals(1, plan.size)
        assertEquals(LocalDateTime.of(2026, 11, 1, 7, 0), local(plan[0].fireAt, ny))
        // 07:00 EST is 12:00 UTC: a fixed-offset addition would have missed this
        assertEquals(Instant.parse("2026-11-01T12:00:00Z").toEpochMilli(), plan[0].fireAt)
    }

    @Test
    fun `a shifted ping colliding with the next lead keeps the smaller lead`() {
        // due 08:00: the 6h ping moves 02:00 -> 07:00 and lands exactly where the
        // 1h ping already was; only one notification may fire there, and it is the
        // one whose lead time is truer to the moment
        val due = LocalDateTime.of(2030, 3, 10, 8, 0)
        val plan = ReminderPlanner.plan(listOf(work(due = due)), values(), now, ist)

        val atSeven = plan.filter { local(it.fireAt) == LocalDateTime.of(2030, 3, 10, 7, 0) }
        assertEquals(1, atSeven.size)
        assertEquals(1, atSeven[0].leadHours)
        assertEquals(2, plan.size)
    }

    @Test
    fun `submitted, unknown, undated and muted work never reminds`() {
        val due = LocalDateTime.of(2030, 3, 10, 12, 0)
        val plan = ReminderPlanner.plan(
            listOf(
                work(id = "a", due = due, submitted = true),
                work(id = "b", due = due, submitted = null),
                work(id = "c", due = null),
                work(id = "d", courseId = "off", due = due),
            ),
            values(quiet = false, off = setOf("off")),
            now,
            ist,
        )
        assertTrue("nothing may be scheduled here: $plan", plan.isEmpty())
    }

    @Test
    fun `past deadlines and a switched-off master are silent`() {
        val past = LocalDateTime.of(2029, 12, 31, 12, 0)
        assertTrue(
            ReminderPlanner.plan(listOf(work(due = past)), values(quiet = false), now, ist).isEmpty(),
        )

        val due = LocalDateTime.of(2030, 3, 10, 12, 0)
        assertTrue(
            ReminderPlanner.plan(listOf(work(due = due)), values(enabled = false, quiet = false), now, ist).isEmpty(),
        )
    }
}
