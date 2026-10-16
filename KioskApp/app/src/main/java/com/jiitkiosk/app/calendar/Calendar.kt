package com.jiitkiosk.app.calendar

import android.content.Context
import com.jiitkiosk.app.data.Cache
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** one row of the official calendar: a single day or a range, straight from the PDF */
data class CalEvent(val from: LocalDate, val to: LocalDate, val title: String, val kind: String) {
    val single: Boolean get() = from == to
    fun on(day: LocalDate): Boolean = day >= from && day <= to
    fun inMonth(month: YearMonth): Boolean =
        YearMonth.from(from) <= month && YearMonth.from(to) >= month
}

/** a teaching window with the months its tab strip shows */
data class CalSemester(val name: String, val from: LocalDate, val to: LocalDate, val months: List<String>)

/** the student's own note on a day */
@Serializable
data class UserEvent(val date: String, val title: String)

/**
 * The calendar's data: the PDF distilled into the shipped asset, plus the
 * events the student adds, which live in their own cache record. No network,
 * no invented dates — an asset that cannot be read yields nothing and the
 * screen says so.
 */
class CalendarStore(private val context: Context) {

    private val cache = Cache(context.filesDir)

    fun official(): List<CalEvent> = CalendarSource.events(context)

    fun semesters(): List<CalSemester> = CalendarSource.semesters(context)

    fun user(): List<CalEvent> = record().mapNotNull { e ->
        runCatching { LocalDate.parse(e.date) }.getOrNull()?.let { CalEvent(it, it, e.title, YOURS) }
    }

    fun add(title: String, date: LocalDate) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        write(record() + UserEvent(date.toString(), clean))
    }

    /** removes the first row matching both the day and the title */
    fun remove(event: CalEvent) {
        val rows = record().toMutableList()
        val i = rows.indexOfFirst { it.date == event.from.toString() && it.title == event.title }
        if (i >= 0) {
            rows.removeAt(i)
            write(rows)
        }
    }

    private fun write(rows: List<UserEvent>) = cache.write(KEY, LIST, rows)

    private fun record(): List<UserEvent> = cache.read(KEY, LIST)?.value.orEmpty()

    companion object {
        const val YOURS = "yours"
        private const val KEY = "calendar.user"
        private val LIST = ListSerializer(UserEvent.serializer())

        /** the semester today falls in; the nearest one when today is between two */
        fun semesterFor(semesters: List<CalSemester>, today: LocalDate): CalSemester? =
            semesters.firstOrNull { sem -> sem.months.any { YearMonth.parse(it) == YearMonth.from(today) } }
                ?: semesters.minByOrNull { kotlin.math.abs(ChronoUnit.DAYS.between(today, it.from)) }
    }
}

/** the asset, parsed once per process */
private object CalendarSource {

    private var loaded: Pair<List<CalEvent>, List<CalSemester>>? = null

    fun events(context: Context): List<CalEvent> = load(context).first

    fun semesters(context: Context): List<CalSemester> = load(context).second

    private fun load(context: Context): Pair<List<CalEvent>, List<CalSemester>> {
        loaded?.let { return it }
        val text = runCatching {
            context.assets.open("calendar/2026-27.json").bufferedReader().use { it.readText() }
        }.getOrNull()
        val parsed = runCatching { parse(text.orEmpty()) }.getOrDefault(emptyList<CalEvent>() to emptyList())
        loaded = parsed
        return parsed
    }

    private fun parse(text: String): Pair<List<CalEvent>, List<CalSemester>> {
        if (text.isBlank()) return emptyList<CalEvent>() to emptyList()
        val root = JSONObject(text)
        val semesters = root.optJSONArray("semesters").objects { o ->
            val from = day(o.optString("from")) ?: return@objects null
            val to = day(o.optString("to")) ?: return@objects null
            CalSemester(o.optString("name"), from, to, o.optJSONArray("months").strings())
        }
        val events = root.optJSONArray("events").objects { o ->
            // a single day may be written as "date"; a range as "from"/"to"
            val from = day(o.optString("date").ifBlank { o.optString("from") }) ?: return@objects null
            val to = day(o.optString("date").ifBlank { o.optString("to") }) ?: from
            CalEvent(from, to, o.optString("title"), o.optString("kind"))
        }
        return events to semesters
    }

    private fun day(iso: String): LocalDate? = runCatching { LocalDate.parse(iso) }.getOrNull()

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }

    private fun <T> JSONArray?.objects(make: (JSONObject) -> T?): List<T> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { make(getJSONObject(it)) }
}
