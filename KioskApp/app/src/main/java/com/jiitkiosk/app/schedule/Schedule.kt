package com.jiitkiosk.app.schedule

import java.io.File
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** MON = 1 … SAT = 6. The college keeps Sunday, so the builder does too. */
@Serializable
data class ClassEntry(
    val id: String,
    val day: Int,
    /** "HH:mm", validated when the class is added */
    val start: String,
    val end: String,
    val subject: String,
    /** set when the subject was picked off the attendance sheet, so a marker can find its percentage */
    val subjectId: String? = null,
    val room: String? = null,
    val type: ClassType = ClassType.Lecture,
)

@Serializable
enum class ClassType { Lecture, Tutorial, Lab }

/** the one active version; [ScheduleStore] keeps the previous one beside it */
@Serializable
data class Schedule(
    val v: Int = 1,
    val savedAt: Long = 0L,
    val entries: List<ClassEntry> = emptyList(),
)

/**
 * The schedule lives in the app's own files directory, not the portal cache, so
 * signing out — even the clearing kind — never takes it. Every save rotates the
 * previous version into `schedule.prev.json`: a bad edit is one undo away.
 */
object ScheduleStore {
    private const val FILE = "schedule.json"
    private const val PREV = "schedule.prev.json"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(dir: File): Schedule? =
        runCatching { json.decodeFromString(Schedule.serializer(), File(dir, FILE).readText()) }.getOrNull()

    fun hasPrev(dir: File): Boolean = File(dir, PREV).exists()

    fun save(dir: File, schedule: Schedule): Schedule {
        val stamped = schedule.copy(savedAt = System.currentTimeMillis())
        load(dir)?.let { runCatching { File(dir, PREV).writeText(json.encodeToString(Schedule.serializer(), it)) } }
        runCatching { File(dir, FILE).writeText(json.encodeToString(Schedule.serializer(), stamped)) }
        return stamped
    }

    /** swaps current and previous, so the undo itself can be undone */
    fun revert(dir: File): Schedule? {
        val prev = runCatching { json.decodeFromString(Schedule.serializer(), File(dir, PREV).readText()) }
            .getOrNull() ?: return null
        load(dir)?.let { runCatching { File(dir, PREV).writeText(json.encodeToString(Schedule.serializer(), it)) } }
        runCatching { File(dir, FILE).writeText(json.encodeToString(Schedule.serializer(), prev)) }
        return prev
    }
}

/** minutes since midnight for an "HH:mm" string, null when it is not one */
fun minutesOf(hhmm: String): Int? {
    val parts = hhmm.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

/** the class's length in minutes, null when either end is unparsable or not positive */
fun ClassEntry.minutes(): Int? {
    val s = minutesOf(start) ?: return null
    val e = minutesOf(end) ?: return null
    return (e - s).takeIf { it > 0 }
}

fun Schedule.entriesOn(day: Int): List<ClassEntry> = entries.filter { it.day == day }.sortedBy { it.start }

fun Schedule.minutesOn(day: Int): Int = entriesOn(day).sumOf { it.minutes() ?: 0 }

fun Schedule.minutesWeek(): Int = entries.sumOf { it.minutes() ?: 0 }

/** 1..6 for a date, null on Sunday */
fun dayNumber(date: LocalDate): Int? = date.dayOfWeek.value.takeIf { it in 1..6 }

private val DAYS_SHORT = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")

/** "MON".."SAT" for the day number */
fun dayShort(day: Int): String = DAYS_SHORT.getOrElse(day - 1) { "?" }
