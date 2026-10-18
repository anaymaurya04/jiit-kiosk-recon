package com.jiitkiosk.app.widget

import com.jiitkiosk.app.schedule.ClassEntry
import com.jiitkiosk.app.schedule.dayNumber
import com.jiitkiosk.app.schedule.minutesOf
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Which instants a repaint can change what the paint shows. updatePeriodMillis is
 * 0 on every provider: the board's freshness line is only honest when the tile
 * repaints exactly when its content moves — a class starting or ending today, and
 * the midnights that make a new day today (rows go Done, the next class comes
 * forward). Tomorrow's own times are laid by tomorrow's midnight tick, which is
 * why the plan is re-laid on the way out of every tick: the scheduler perpetuates
 * itself and a missed alarm costs one day, not forever.
 */
object WidgetPlanner {

    const val MIDNIGHTS = 7

    fun plan(entries: List<ClassEntry>, now: LocalDateTime, zone: ZoneId, midnights: Int = MIDNIGHTS): List<Long> {
        val date = now.toLocalDate()
        val day = dayNumber(date)
        val out = ArrayList<Long>()

        if (day != null) {
            for (e in entries) {
                if (e.day != day) continue
                val start = minutesOf(e.start) ?: continue
                val end = minutesOf(e.end) ?: start // an unparsable end is a zero-length class, as the rows treat it
                out += instant(date, start / 60, start % 60, zone)
                out += instant(date, end / 60, end % 60, zone)
            }
        }
        for (i in 1..midnights) out += instant(date.plusDays(i.toLong()), 0, 0, zone)
        return out.distinct()
            .filter { it > now.atZone(zone).toInstant().toEpochMilli() }
            .sorted()
    }

    /** DST-correct by construction: wall clock in the zone */
    private fun instant(date: LocalDate, h: Int, m: Int, zone: ZoneId): Long =
        date.atTime(h, m).atZone(zone).toInstant().toEpochMilli()
}
