package com.jiitkiosk.app.widget

import com.jiitkiosk.app.schedule.ClassEntry
import com.jiitkiosk.app.schedule.dayNumber
import com.jiitkiosk.app.schedule.minutesOf
import java.time.LocalDate
import java.time.LocalTime

/**
 * What the home-screen widgets show for "next class", computed from the saved
 * schedule (weekly day numbers) and the local clock only — no network, no session.
 * Everything here is pure so the widget renderer and its tests share one truth.
 */
sealed interface NextUp {
    /** nothing saved yet — the board's "NO SCHEDULE / TAP TO CREATE" state */
    data object NoSchedule : NextUp

    /** a class is running right now (start ≤ now < end) */
    data class Live(val entry: ClassEntry, val startMin: Int, val endMin: Int) : NextUp

    /** the next class that has not started; [today] false means another day (label it with dayShort) */
    data class Later(val entry: ClassEntry, val startMin: Int, val today: Boolean) : NextUp
}

private fun validStart(entry: ClassEntry): Int? = minutesOf(entry.start)

/**
 * The widget's headline. A running class wins; otherwise the earliest start still
 * ahead, scanning from today forward over the Mon–Sat week. Entries whose start
 * does not parse are skipped (the builder always writes valid times; this is armour).
 * An entry with an unparsable end is treated as zero-length.
 */
fun nextUp(entries: List<ClassEntry>, date: LocalDate, now: LocalTime): NextUp {
    if (entries.isEmpty()) return NextUp.NoSchedule
    val nowM = now.hour * 60 + now.minute
    val parsed = entries.mapNotNull { e -> validStart(e)?.let { s -> Triple(e, s, minutesOf(e.end) ?: s) } }
    val d0 = dayNumber(date)

    // running now — the newest start wins when two overlap
    if (d0 != null) {
        parsed.filter { (e, s, en) -> e.day == d0 && s <= nowM && en > nowM }
            .maxByOrNull { it.second }
            ?.let { (e, s, en) -> return NextUp.Live(e, s, en) }
    }

    // 0..6: the week repeats, so even after the last class of the week the next one is one scan away
    val from = d0 ?: 1
    for (offset in 0..6) {
        val d = (from - 1 + offset) % 6 + 1
        val today = offset == 0 && d0 != null
        val pick = parsed.filter { (e, s, _) -> e.day == d && (!today || s > nowM) }
            .minWithOrNull(compareBy({ it.second }, { it.third }))
        if (pick != null) return NextUp.Later(pick.first, pick.second, today)
    }
    return NextUp.NoSchedule // only reachable when every start is malformed
}

enum class ClassRowPhase { Done, Live, Upcoming }

data class ClassRow(val entry: ClassEntry, val startMin: Int, val phase: ClassRowPhase)

/** today's classes in start order with their phase — the Classes and Dashboard rows */
fun todayRows(entries: List<ClassEntry>, day: Int, now: LocalTime): List<ClassRow> {
    val nowM = now.hour * 60 + now.minute
    return entries.filter { it.day == day }
        .mapNotNull { e -> validStart(e)?.let { s -> e to s } }
        .sortedWith(compareBy({ it.second }, { it.first.end }))
        .map { (e, s) ->
            val en = minutesOf(e.end) ?: s
            ClassRow(
                e,
                s,
                when {
                    s > nowM -> ClassRowPhase.Upcoming
                    en > nowM -> ClassRowPhase.Live
                    else -> ClassRowPhase.Done
                },
            )
        }
}

/** "2:00 PM" — the widgets' big clock values */
fun clockLabel(min: Int): String {
    val h = min / 60
    val m = min % 60
    val h12 = when (h % 12) {
        0 -> 12
        else -> h % 12
    }
    return "$h12:${m.toString().padStart(2, '0')} ${if (h < 12) "AM" else "PM"}"
}
