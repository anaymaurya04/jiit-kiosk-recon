package com.jiitkiosk.app.reminders

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** what the planner needs to know about one piece of work */
data class ReminderWork(
    val id: String,
    val courseId: String,
    val courseName: String,
    val title: String,
    val dueAt: Long?,
    val submitted: Boolean?,
    val link: String?,
)

/** an alarm that should exist */
data class Reminder(val work: ReminderWork, val leadHours: Int, val fireAt: Long)

/**
 * Pure: given the coursework and the settings, the instants to ping at.
 *
 * Undated work never reminds. Submitted work stops reminding — a submitted or
 * deleted item is simply absent from the next plan, which is also how its old
 * alarm dies. Work whose submission state is unknown never reminds either:
 * a notification must not be based on a guess. A ping the quiet window would
 * swallow moves to the window's end, unless that would land at or after the
 * deadline, in which case it is dropped — a reminder after the fact is noise.
 */
object ReminderPlanner {

    val LEADS = listOf(24, 6, 1)

    fun plan(
        work: List<ReminderWork>,
        values: ReminderPrefs.Values,
        now: Long,
        zone: ZoneId,
    ): List<Reminder> {
        if (!values.enabled || values.leads.isEmpty()) return emptyList()

        val out = ArrayList<Reminder>()
        for (w in work) {
            val due = w.dueAt ?: continue
            if (w.submitted != false) continue
            if (!values.courseOn(w.courseId)) continue
            for (lead in values.leads) {
                val raw = due - lead * 3_600_000L
                if (raw <= now) continue
                val fire = quietAdjusted(raw, values, zone)
                if (fire >= due) continue
                out += Reminder(w, lead, fire)
            }
        }
        // after a shift two leads can collide; the smaller lead is the truer story
        return out
            .sortedWith(compareBy({ it.fireAt }, { it.leadHours }, { it.work.id }))
            .distinctBy { it.work.id to it.fireAt }
    }

    /** the quiet window pushes a ping to its end; DST-correct via wall-clock arithmetic */
    private fun quietAdjusted(fireAt: Long, values: ReminderPrefs.Values, zone: ZoneId): Long {
        if (!values.quietEnabled) return fireAt
        val start = values.quietStartMin
        val end = values.quietEndMin
        if (start == end) return fireAt
        val z = Instant.ofEpochMilli(fireAt).atZone(zone)
        val minute = z.hour * 60 + z.minute
        val quiet = if (start < end) minute in start until end else minute >= start || minute < end
        if (!quiet) return fireAt
        var shifted = z.toLocalDate().atTime(LocalTime.of(end / 60, end % 60)).atZone(zone)
        if (shifted.toInstant().toEpochMilli() <= fireAt) shifted = shifted.plusDays(1)
        return shifted.toInstant().toEpochMilli()
    }
}
