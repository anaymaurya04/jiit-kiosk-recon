package com.jiitkiosk.app.widget

import com.jiitkiosk.app.classroom.Work
import kotlin.math.roundToInt

/** the portal's per-subject percentages, averaged like the attendance hero does */
fun percentOf(percents: List<Double>): Int? =
    if (percents.isEmpty()) null else percents.average().roundToInt()

/** the next unsubmitted deadline after [now] — the classroom hero's own pick */
fun nextOf(work: List<Work>, now: Long): Work? =
    work.filter { it.submitted == false && it.dueAt != null && it.dueAt > now }
        .minByOrNull { it.dueAt!! }

/** "2D 4H" — the shape the classroom hero counts down with */
fun countdownText(ms: Long): String {
    val h = ms / 3_600_000
    val m = (ms % 3_600_000) / 60_000
    return when {
        h >= 48 -> "${h / 24}D ${h % 24}H"
        h >= 1 -> "${h}H ${m}M"
        else -> "${m}M"
    }
}
