package com.jiitkiosk.app.widget

import com.jiitkiosk.app.classroom.Work
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The shapes every widget renderer formats the same way. Pure, and the clock is
 * a parameter, so the unit tests pin the strings; renderers pass the wall clock.
 */

private val HEADING = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val STOP = setOf("of", "the", "and", "a", "an", "for", "to", "in", "at")

/** "Foundations of Data Analytics" → "FDA"; a one-word subject takes its first three letters */
fun subjectCode(name: String): String {
    val words = name.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotBlank() }
    val significant = words.filterNot { it.lowercase(Locale.ROOT) in STOP }.ifEmpty { words }
    val take = significant.take(3)
    return if (take.size == 1) take[0].take(3).uppercase(Locale.ROOT)
    else take.joinToString("") { it.first().uppercaseChar().toString() }
}

/** "FRI 16 OCT" — the boards' date heading */
fun dayHeading(date: LocalDate): String = date.format(HEADING).uppercase(Locale.ENGLISH)

/** the chip-sized countdown: "5H", "45M", "2D" */
fun inlineCountdown(ms: Long): String {
    val h = ms / 3_600_000
    return when {
        h >= 48 -> "${h / 24}D"
        h >= 1 -> "${h}H"
        else -> "${(ms / 60_000).coerceAtLeast(1)}M"
    }
}

/** unsubmitted pieces, and of those the ones already past their deadline */
fun pendingCounts(work: List<Work>, now: Long): Pair<Int, Int> {
    val open = work.filter { it.submitted == false }
    return open.size to open.count { it.dueAt != null && it.dueAt < now }
}

/** open work, most urgent first: overdue, then soonest deadline, then undated */
fun pendingRows(work: List<Work>, now: Long): List<Work> = work
    .filter { it.submitted == false }
    .sortedWith(compareBy({ !(it.dueAt != null && it.dueAt < now) }, { it.dueAt ?: Long.MAX_VALUE }))

/** hours of classes, floored; a part hour reads "<1H" */
fun hoursText(minutes: Int): String = when {
    minutes >= 60 -> "${minutes / 60}H"
    minutes > 0 -> "<1H"
    else -> "0H"
}

fun moreToday(more: Int, minutes: Int): String = "+$more MORE TODAY · ${hoursText(minutes)} OF CLASSES"

/** the same age shapes the app's freshness line uses ("4 MIN AGO") */
fun ageText(at: Long, now: Long): String {
    val min = ((now - at) / 60_000L).coerceAtLeast(0L)
    return when {
        min < 1L -> "JUST NOW"
        min < 60L -> "$min MIN AGO"
        min < 1440L -> "${min / 60L}H AGO"
        else -> "${min / 1440L}D AGO"
    }
}

/** aged past this, the footer switches to the warn colour (spec: "older than a few hours") */
const val STALE_MS = 6 * 60 * 60 * 1000L

/** the footer text and whether it wears the warn colour */
fun syncLine(at: Long?, expired: Boolean, now: Long): Pair<String, Boolean> = when {
    at == null -> "NOT SYNCED" to false
    expired -> "SHOWING DATA FROM ${ageText(at, now)}" to true
    else -> "SYNCED ${ageText(at, now)}" to (now - at >= STALE_MS)
}
