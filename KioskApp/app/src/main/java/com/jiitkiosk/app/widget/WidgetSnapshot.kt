package com.jiitkiosk.app.widget

import android.content.Context
import com.jiitkiosk.app.SessionStore
import com.jiitkiosk.app.classroom.ClassroomData
import com.jiitkiosk.app.classroom.Work
import com.jiitkiosk.app.data.Cache
import com.jiitkiosk.app.schedule.ClassEntry
import com.jiitkiosk.app.schedule.ScheduleStore
import com.jiitkiosk.app.schedule.dayNumber
import com.jiitkiosk.app.schedule.minutesOn
import com.jiitkiosk.app.ui.attendance.Sheet
import com.jiitkiosk.app.ui.theme.Verdict
import com.jiitkiosk.app.ui.theme.markFor
import com.jiitkiosk.app.ui.theme.verdictOf
import `in`.codelif.ktjiit.model.AttendanceMeta
import `in`.codelif.ktjiit.model.SubjectAttendance
import java.time.LocalDate
import java.time.LocalTime

/**
 * One read of every cache the widgets glance at. Cache-only by construction:
 * nothing here touches the network, the session, or the login flow, so a
 * widget can always paint immediately, even offline and even expired.
 */
data class WidgetSnapshot(
    val signedIn: Boolean,
    val expired: Boolean,
    val date: LocalDate,
    val next: NextUp,
    val rows: List<ClassRow>,
    val minutesToday: Int,
    val subjects: List<SubjectAttendance>,
    val percent: Int?,
    val overall: Verdict?,
    val lowest: SubjectAttendance?,
    val target: Int,
    val band: Int,
    val work: List<Work>,
    val sheetAt: Long?,
    val workAt: Long?,
    /** the app is mid-fetch and nothing is cached yet: board 31's updating tile */
    val updating: Boolean,
) {
    /** the calendar's own marker, same function */
    fun markOf(entry: ClassEntry): Verdict? = markFor(entry, subjects, target, band)

    /** the older of the two caches is the honest age of what is on screen */
    val syncedAt: Long? get() = listOfNotNull(sheetAt, workAt).minOrNull()
}

fun readSnapshot(context: Context): WidgetSnapshot {
    // the same keys Graph owns; read directly so a widget paint never builds the graph
    val prefs = context.getSharedPreferences("kiosk", Context.MODE_PRIVATE)
    val target = prefs.getInt("attend_target", 75).coerceIn(50, 100)
    val band = prefs.getInt("attend_band", 5).coerceIn(0, 25)
    val expired = prefs.getBoolean("session_expired", false)
    val updating = prefs.getBoolean("widget_updating", false)
    val session = SessionStore(context).load()

    val schedule = ScheduleStore.load(context.filesDir)
    val entries = schedule?.entries.orEmpty()
    val date = LocalDate.now()
    val now = LocalTime.now()
    val day = dayNumber(date)

    val cache = Cache(context.filesDir)
    val sheet = session?.token?.let { token ->
        cache.read("att-meta/$token", AttendanceMeta.serializer())?.value?.semesters
            ?.firstOrNull()
            ?.let { cache.read("att/$token/${it.id}", Sheet.serializer()) }
    }
    val workCache = cache.read(ClassroomData.CACHE_KEY, ClassroomData.serializer())

    val subjects = sheet?.value?.detail?.subjects.orEmpty()
    val percent = percentOf(subjects.mapNotNull { it.percent })
    return WidgetSnapshot(
        signedIn = session != null,
        expired = expired,
        date = date,
        next = nextUp(entries, date, now),
        rows = if (day != null) todayRows(entries, day, now) else emptyList(),
        minutesToday = if (day != null) schedule?.minutesOn(day) ?: 0 else 0,
        subjects = subjects,
        percent = percent,
        overall = percent?.let { verdictOf(it.toDouble(), target, band) },
        lowest = subjects.filter { it.percent != null }.minByOrNull { it.percent ?: 0.0 },
        target = target,
        band = band,
        work = workCache?.value?.work.orEmpty(),
        sheetAt = sheet?.at,
        workAt = workCache?.at,
        updating = updating,
    )
}
