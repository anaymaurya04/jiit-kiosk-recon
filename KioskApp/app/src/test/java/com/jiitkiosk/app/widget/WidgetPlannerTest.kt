package com.jiitkiosk.app.widget

import com.jiitkiosk.app.schedule.ClassEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * W4: updatePeriodMillis is 0 and the boards want honest freshness, so the plan
 * names the exact instants a repaint can change what the paint shows — every
 * remaining class start and end of today, the next seven midnights, nothing else
 * (tomorrow's class times are today's midnight problem, and one midnight re-lay).
 */
class WidgetPlannerTest {

    private val zone = ZoneId.of("Pacific/Honolulu")

    private fun e(day: Int, start: String, end: String, id: String = "$day-$start-$end") =
        ClassEntry(id = id, day = day, start = start, end = end, subject = "SE")

    private fun iso(date: String, time: String) =
        LocalDateTime.parse("${date}T$time").atZone(zone).toInstant().toEpochMilli()

    @Test
    fun a_running_class_repaints_at_its_end_and_nothing_its_start_behind_now() {
        // Sat 17 Oct, HST: the live class opened at 07:45 — its start is gone by 08:00
        val now = LocalDateTime.parse("2026-10-17T08:00")
        val plan = WidgetPlanner.plan(listOf(e(6, "07:45", "09:15"), e(1, "10:20", "11:10")), now, zone)
        assertEquals(listOf(iso("2026-10-17", "09:15"), iso("2026-10-18", "00:00")), plan.take(2))
        assertFalse(plan.contains(iso("2026-10-17", "07:45")))
    }

    @Test
    fun an_upcoming_class_ticks_at_both_ends_and_midnight_closes_the_day() {
        val now = LocalDateTime.parse("2026-10-17T08:00")
        val plan = WidgetPlanner.plan(listOf(e(6, "10:30", "11:20")), now, zone)
        assertEquals(
            listOf(iso("2026-10-17", "10:30"), iso("2026-10-17", "11:20"), iso("2026-10-18", "00:00")),
            plan.take(3),
        )
    }

    @Test
    fun with_no_schedule_the_plan_is_only_the_seven_midnights() {
        val now = LocalDateTime.parse("2026-10-17T08:00")
        val plan = WidgetPlanner.plan(emptyList(), now, zone)
        assertEquals(7, plan.size)
        assertEquals(iso("2026-10-18", "00:00"), plan.first())
        assertEquals(iso("2026-10-24", "00:00"), plan.last())
    }

    @Test
    fun tomorrow_s_times_belong_to_tomorrow_s_plan_not_today_s() {
        val now = LocalDateTime.parse("2026-10-17T08:00")
        val plan = WidgetPlanner.plan(listOf(e(1, "10:00", "10:50")), now, zone)
        assertFalse("Monday 10:00 must not be scheduled from Saturday", plan.contains(iso("2026-10-19", "10:00")))
        assertTrue(plan.contains(iso("2026-10-18", "00:00")))
    }

    @Test
    fun two_classes_sharing_a_minute_tick_once_there() {
        val now = LocalDateTime.parse("2026-10-17T08:00")
        val plan = WidgetPlanner.plan(
            listOf(e(6, "10:00", "11:00"), e(6, "10:00", "12:00")),
            now,
            zone,
        )
        assertEquals(1, plan.count { it == iso("2026-10-17", "10:00") })
        assertTrue(plan.contains(iso("2026-10-17", "11:00")))
        assertTrue(plan.contains(iso("2026-10-17", "12:00")))
    }

    @Test
    fun the_plan_is_sorted_and_strictly_ahead() {
        val now = LocalDateTime.parse("2026-10-17T08:00")
        val plan = WidgetPlanner.plan(
            listOf(e(6, "09:30", "10:20"), e(6, "07:45", "09:15"), e(6, "10:30", "11:20")),
            now,
            zone,
        )
        assertEquals(plan.sorted(), plan)
        assertTrue(plan.all { it > now.atZone(zone).toInstant().toEpochMilli() })
    }
}
