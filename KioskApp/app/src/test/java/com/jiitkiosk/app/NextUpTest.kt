package com.jiitkiosk.app

import com.jiitkiosk.app.schedule.ClassEntry
import com.jiitkiosk.app.widget.ClassRowPhase
import com.jiitkiosk.app.widget.NextUp
import com.jiitkiosk.app.widget.clockLabel
import com.jiitkiosk.app.widget.nextUp
import com.jiitkiosk.app.widget.todayRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** the widgets' next-class state machine (brief §10: boundaries, overlap, zones, midnight) */
class NextUpTest {

    private fun entry(day: Int, start: String, end: String, subject: String = "S") =
        ClassEntry(id = "$day-$start-$subject", day = day, start = start, end = end, subject = subject)

    // Mon 2026-10-19, Fri 2026-10-16, Sat 2026-10-17, Sun 2026-10-18
    private val monday = LocalDate.of(2026, 10, 19)
    private val saturday = LocalDate.of(2026, 10, 17)

    @Test
    fun `empty schedule is the no-schedule state`() {
        assertEquals(NextUp.NoSchedule, nextUp(emptyList(), monday, LocalTime.of(8, 0)))
    }

    @Test
    fun `a running class is live from its exact start to its end`() {
        val e = entry(1, "09:00", "10:00")
        assertEquals(NextUp.Live(e, 540, 600), nextUp(listOf(e), monday, LocalTime.of(9, 0)))
        assertEquals(NextUp.Live(e, 540, 600), nextUp(listOf(e), monday, LocalTime.of(9, 59)))
    }

    @Test
    fun `after the end the class is on next week`() {
        val e = entry(1, "09:00", "10:00")
        // same weekday, class over, nothing else in the week → next Monday, not today
        assertEquals(NextUp.Later(e, 540, today = false), nextUp(listOf(e), monday, LocalTime.of(10, 0)))
        assertEquals(NextUp.Later(e, 540, today = false), nextUp(listOf(e), monday, LocalTime.of(23, 59)))
    }

    @Test
    fun `the next class later today wins over other days`() {
        val mon = entry(1, "09:00", "10:00")
        val tue = entry(2, "08:00", "09:00")
        assertEquals(NextUp.Later(mon, 540, today = true), nextUp(listOf(tue, mon), monday, LocalTime.of(8, 0)))
    }

    @Test
    fun `after today's last class the next day with classes wins`() {
        val mon = entry(1, "09:00", "10:00")
        val fri = entry(5, "14:00", "15:00")
        assertEquals(NextUp.Later(fri, 840, today = false), nextUp(listOf(mon, fri), monday, LocalTime.of(11, 0)))
    }

    @Test
    fun `sunday only ever looks forward`() {
        val mon = entry(1, "09:00", "10:00")
        val sunday = LocalDate.of(2026, 10, 18)
        assertEquals(NextUp.Later(mon, 540, today = false), nextUp(listOf(mon), sunday, LocalTime.of(12, 0)))
    }

    @Test
    fun `after saturday's last class monday comes back around`() {
        val sat = entry(6, "10:00", "11:00")
        assertEquals(NextUp.Later(sat, 600, today = false), nextUp(listOf(sat), saturday, LocalTime.of(18, 0)))
    }

    @Test
    fun `overlapping classes take the newest start while live`() {
        val wide = entry(1, "09:00", "11:00")
        val narrow = entry(1, "10:00", "10:30")
        assertEquals(NextUp.Live(narrow, 600, 630), nextUp(listOf(wide, narrow), monday, LocalTime.of(10, 15)))
    }

    @Test
    fun `overlapping upcoming classes take the earliest start then end`() {
        val long = entry(1, "09:00", "10:00")
        val short = entry(1, "09:00", "09:30")
        assertEquals(NextUp.Later(short, 540, today = true), nextUp(listOf(long, short), monday, LocalTime.of(8, 0)))
    }

    @Test
    fun `a class spanning noon stays live`() {
        val noon = entry(1, "11:30", "13:00")
        assertEquals(NextUp.Live(noon, 690, 780), nextUp(listOf(noon), monday, LocalTime.of(12, 15)))
    }

    @Test
    fun `a malformed start is skipped`() {
        val bad = entry(1, "25:00", "26:00")
        val good = entry(1, "09:00", "10:00")
        assertEquals(NextUp.Later(good, 540, today = true), nextUp(listOf(bad, good), monday, LocalTime.of(8, 0)))
        assertEquals(NextUp.NoSchedule, nextUp(listOf(bad), monday, LocalTime.of(8, 0)))
    }

    @Test
    fun `midnight rollover starts a fresh day`() {
        val e = entry(1, "09:00", "10:00")
        assertEquals(NextUp.Later(e, 540, today = true), nextUp(listOf(e), monday, LocalTime.of(0, 1)))
    }

    @Test
    fun `the local zone decides which day it is`() {
        val fri = entry(5, "19:00", "20:00")
        val t = Instant.parse("2026-10-16T18:30:00Z") // Fri 18:30 UTC, Sat 00:00 IST
        val utc = ZonedDateTime.ofInstant(t, ZoneId.of("UTC"))
        val ist = ZonedDateTime.ofInstant(t, ZoneId.of("Asia/Kolkata"))
        assertEquals(NextUp.Later(fri, 1140, today = true), nextUp(listOf(fri), utc.toLocalDate(), utc.toLocalTime()))
        assertEquals(NextUp.Later(fri, 1140, today = false), nextUp(listOf(fri), ist.toLocalDate(), ist.toLocalTime()))
    }

    @Test
    fun `a dst spring-forward day keeps local class times`() {
        // 2026-03-08 America/New_York jumps 02:00 → 03:00; 01:30 is still Sunday, 09:15 Monday is class time
        val zone = ZoneId.of("America/New_York")
        val e = entry(1, "09:00", "10:00")
        val before = ZonedDateTime.of(2026, 3, 8, 1, 30, 0, 0, zone)
        val after = ZonedDateTime.of(2026, 3, 9, 9, 15, 0, 0, zone)
        assertEquals(NextUp.Later(e, 540, today = false), nextUp(listOf(e), before.toLocalDate(), before.toLocalTime()))
        assertEquals(NextUp.Live(e, 540, 600), nextUp(listOf(e), after.toLocalDate(), after.toLocalTime()))
    }

    @Test
    fun `today rows carry done live and upcoming in start order`() {
        val a = entry(1, "09:00", "10:00", "A")
        val b = entry(1, "11:00", "12:00", "B")
        val c = entry(1, "14:00", "15:00", "C")
        val rows = todayRows(listOf(c, a, b), 1, LocalTime.of(11, 30))
        assertEquals(listOf(ClassRowPhase.Done, ClassRowPhase.Live, ClassRowPhase.Upcoming), rows.map { it.phase })
        assertEquals(listOf(540, 660, 840), rows.map { it.startMin })
    }

    @Test
    fun `today row boundaries are start inclusive and end exclusive`() {
        val e = entry(1, "09:00", "10:00")
        assertEquals(ClassRowPhase.Upcoming, todayRows(listOf(e), 1, LocalTime.of(8, 59)).single().phase)
        assertEquals(ClassRowPhase.Live, todayRows(listOf(e), 1, LocalTime.of(9, 0)).single().phase)
        assertEquals(ClassRowPhase.Live, todayRows(listOf(e), 1, LocalTime.of(9, 59)).single().phase)
        assertEquals(ClassRowPhase.Done, todayRows(listOf(e), 1, LocalTime.of(10, 0)).single().phase)
        assertTrue(todayRows(listOf(e), 2, LocalTime.of(9, 0)).isEmpty())
    }

    @Test
    fun `clock labels print the board's shapes`() {
        assertEquals("2:00 PM", clockLabel(840))
        assertEquals("9:00 AM", clockLabel(540))
        assertEquals("12:00 AM", clockLabel(0))
        assertEquals("12:05 PM", clockLabel(725))
        assertEquals("11:59 PM", clockLabel(1439))
    }
}
