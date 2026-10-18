package com.jiitkiosk.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.jiitkiosk.app.MainActivity
import com.jiitkiosk.app.R
import com.jiitkiosk.app.classroom.Work
import com.jiitkiosk.app.schedule.ClassEntry
import com.jiitkiosk.app.schedule.dayShort
import com.jiitkiosk.app.ui.theme.Verdict
import com.jiitkiosk.app.ui.theme.verdictOf
import java.time.LocalTime

/** the six providers and the layout each paints through */
enum class WidgetKind(val provider: Class<out AppWidgetProvider>, val layout: Int) {
    Next(NextClassWidget::class.java, R.layout.widget_next),
    Attendance(AttendanceWidget::class.java, R.layout.widget_attendance),
    Pending(PendingWidget::class.java, R.layout.widget_pending),
    Today(KioskWidget::class.java, R.layout.widget_today),
    Classes(ClassesWidget::class.java, R.layout.widget_classes),
    Dashboard(DashboardWidget::class.java, R.layout.widget_dashboard),
}

/** where a widget tap lands inside the app — MainActivity reads the extra */
private const val OPEN_SCHEDULE = "schedule"
private const val OPEN_ATTENDANCE = "attendance"
private const val OPEN_ASSIGNMENTS = "assignments"
private const val OPEN_RECONNECT = "reconnect"

/**
 * The one painter for all six widgets. Cache-only: the snapshot comes from the
 * files the app already wrote, and every field is written on every paint, so a
 * sample value from the picker layout can never leak onto the home screen.
 */
object WidgetRender {

    /** repaint everything on the home screen — app open, classroom sync, alarms */
    fun refresh(context: Context) {
        val snap = readSnapshot(context)
        WidgetKind.values().forEach { kind -> ids(context, kind).forEach { paint(context, kind, it, snap) } }
    }

    fun paint(context: Context, kind: WidgetKind, widgetIds: IntArray) {
        val snap = readSnapshot(context)
        widgetIds.forEach { paint(context, kind, it, snap) }
    }

    private fun paint(context: Context, kind: WidgetKind, id: Int, snap: WidgetSnapshot) {
        val settings = WidgetPrefs.get(context, id)
        AppWidgetManager.getInstance(context).updateAppWidget(id, build(context, kind, id, settings, snap))
    }

    private fun ids(context: Context, kind: WidgetKind): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, kind.provider))

    /** also what the config screen previews, so "preview updates as you change" is literal */
    fun build(context: Context, kind: WidgetKind, id: Int, s: WidgetSettings, snap: WidgetSnapshot): RemoteViews {
        val nowMs = System.currentTimeMillis()
        val v = RemoteViews(context.packageName, kind.layout)
        v.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            if (s.transparent) android.R.color.transparent else R.drawable.widget_card,
        )
        v.setViewVisibility(R.id.widget_sample, View.GONE)
        // the whole card is the tap target; an expired session spends it on the way back in
        v.setOnClickPendingIntent(R.id.widget_root, intent(context, id * 10, if (snap.expired) OPEN_RECONNECT else null))
        when (kind) {
            WidgetKind.Next -> nextTile(context, v, id, s, snap, nowMs)
            WidgetKind.Attendance -> attendanceTile(context, v, s, snap)
            WidgetKind.Pending -> pendingTile(v, snap, nowMs)
            WidgetKind.Today -> todayWide(context, v, id, s, snap, nowMs)
            WidgetKind.Classes -> classesWide(context, v, id, s, snap, nowMs)
            WidgetKind.Dashboard -> dashboard(context, v, id, s, snap, nowMs)
        }
        return v
    }

    // ---------------------------------------------------------------- shared

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

    private fun chip(v: RemoteViews, id: Int, text: String, bg: Int, fg: Int) {
        v.setViewVisibility(id, View.VISIBLE)
        v.setTextViewText(id, text)
        v.setInt(id, "setBackgroundColor", bg)
        v.setTextColor(id, fg)
    }

    /** the boards' marker chip, or nothing when the sheet cannot mark the subject */
    private fun markChip(v: RemoteViews, id: Int, verdict: Verdict?) {
        if (verdict == null) v.setViewVisibility(id, View.GONE)
        else chip(v, id, verdictChip(verdict), verdictColor(verdict), C.INK)
    }

    private fun ghost(v: RemoteViews, id: Int, res: Int) {
        v.setViewVisibility(id, View.VISIBLE)
        v.setImageViewResource(id, res)
        v.setFloat(id, "setAlpha", Ghost.ALPHA)
    }

    /** the state card's art is the board's full-strength illustration, not a faint ghost */
    private fun stateArt(v: RemoteViews, showId: Int, hideId: Int, res: Int) {
        v.setViewVisibility(showId, View.VISIBLE)
        v.setImageViewResource(showId, res)
        v.setFloat(showId, "setAlpha", 1f)
        v.setViewVisibility(hideId, View.GONE)
    }

    private fun intent(context: Context, code: Int, open: String?): PendingIntent {
        val i = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (open != null) i.putExtra(MainActivity.EXTRA_OPEN, open)
        return PendingIntent.getActivity(
            context,
            code,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun subjectName(s: WidgetSettings, entry: ClassEntry): String =
        if (s.hideNames) entry.subjectId.orEmpty() else entry.subject

    private fun footer(v: RemoteViews, leftRes: Int, rightRes: Int, snap: WidgetSnapshot, nowMs: Long, dateToo: Boolean) {
        val (text, warn) = syncLine(snap.syncedAt, snap.expired, nowMs)
        if (dateToo) {
            // board 31, normal state: the date on the left, the age on the right
            v.setTextViewText(leftRes, dayHeading(snap.date))
            v.setTextColor(leftRes, C.MUTED)
            v.setViewVisibility(rightRes, View.VISIBLE)
            v.setTextViewText(rightRes, text)
            v.setTextColor(rightRes, if (warn) C.WARN else C.MUTED)
        } else {
            // every state that has no day to show keeps only the freshness text, left
            v.setTextViewText(leftRes, text)
            v.setTextColor(leftRes, if (warn) C.WARN else C.MUTED)
            v.setViewVisibility(rightRes, View.GONE)
        }
    }

    // ------------------------------------------------------------- 2x2 tiles

    private fun nextTile(context: Context, v: RemoteViews, id: Int, s: WidgetSettings, snap: WidgetSnapshot, nowMs: Long) {
        val now = LocalTime.now()
        val nowM = now.hour * 60 + now.minute
        when (val n = snap.next) {
            is NextUp.NoSchedule -> {
                ghost(v, R.id.next_ghost, R.drawable.widget_ghost_tablet)
                v.setTextViewText(R.id.next_time, "No schedule")
                v.setTextViewTextSize(R.id.next_time, TypedValue.COMPLEX_UNIT_SP, 20f)
                v.setViewVisibility(R.id.next_subject, View.GONE)
                v.setViewVisibility(R.id.next_count, View.GONE)
                chip(v, R.id.next_chip, "TAP TO CREATE", C.BONE, C.INK)
                v.setOnClickPendingIntent(R.id.widget_root, intent(context, id * 10, OPEN_SCHEDULE))
            }
            is NextUp.Live -> {
                ghost(v, R.id.next_ghost, R.drawable.widget_ghost_hourglass)
                v.setTextViewText(R.id.next_time, clockLabel(n.startMin))
                v.setTextViewTextSize(R.id.next_time, TypedValue.COMPLEX_UNIT_SP, 38f)
                val name = subjectName(s, n.entry)
                v.setTextViewText(R.id.next_subject, name)
                v.setViewVisibility(R.id.next_subject, if (name.isBlank()) View.GONE else View.VISIBLE)
                markChip(v, R.id.next_chip, snap.markOf(n.entry))
                v.setTextViewText(R.id.next_count, "NOW")
                v.setViewVisibility(R.id.next_count, View.VISIBLE)
            }
            is NextUp.Later -> {
                ghost(v, R.id.next_ghost, R.drawable.widget_ghost_hourglass)
                v.setTextViewText(
                    R.id.next_time,
                    if (n.today) clockLabel(n.startMin) else "${dayShort(n.entry.day)} ${clockLabel(n.startMin)}",
                )
                v.setTextViewTextSize(R.id.next_time, TypedValue.COMPLEX_UNIT_SP, if (n.today) 38f else 24f)
                val name = subjectName(s, n.entry)
                v.setTextViewText(R.id.next_subject, name)
                v.setViewVisibility(R.id.next_subject, if (name.isBlank()) View.GONE else View.VISIBLE)
                markChip(v, R.id.next_chip, snap.markOf(n.entry))
                // the countdown only makes sense for a class still ahead today
                if (n.today) {
                    v.setTextViewText(R.id.next_count, countdownText((n.startMin - nowM) * 60_000L))
                    v.setViewVisibility(R.id.next_count, View.VISIBLE)
                } else {
                    v.setViewVisibility(R.id.next_count, View.GONE)
                }
            }
        }
    }

    private fun attendanceTile(context: Context, v: RemoteViews, s: WidgetSettings, snap: WidgetSnapshot) {
        ghost(v, R.id.att_ghost, R.drawable.widget_ghost_shield)
        val pct = snap.percent
        if (pct == null) {
            v.setViewVisibility(R.id.att_ring, View.GONE)
            v.setViewVisibility(R.id.att_percent, View.GONE)
            v.setTextViewText(R.id.att_empty, if (snap.signedIn) "NOT SYNCED" else "SIGN IN TO SYNC")
            v.setViewVisibility(R.id.att_empty, View.VISIBLE)
            v.setViewVisibility(R.id.att_lowest_label, View.GONE)
            v.setViewVisibility(R.id.att_lowest, View.GONE)
            v.setViewVisibility(R.id.att_chip, View.GONE)
            return
        }
        v.setViewVisibility(R.id.att_empty, View.GONE)
        val size = dp(context, 58)
        v.setImageViewBitmap(R.id.att_ring, ringBitmap(size, pct, verdictColor(snap.overall ?: Verdict.Close)))
        v.setContentDescription(R.id.att_ring, "$pct percent")
        v.setViewVisibility(R.id.att_ring, View.VISIBLE)
        v.setTextViewText(R.id.att_percent, pct.toString())
        v.setViewVisibility(R.id.att_percent, View.VISIBLE)

        val low = snap.lowest
        if (low == null) {
            v.setViewVisibility(R.id.att_lowest_label, View.GONE)
            v.setViewVisibility(R.id.att_lowest, View.GONE)
            v.setViewVisibility(R.id.att_chip, View.GONE)
        } else {
            val shown = (low.percent ?: 0.0).toInt()
            val verdict = verdictOf(shown.toDouble(), snap.target, snap.band)
            v.setViewVisibility(R.id.att_lowest_label, View.VISIBLE)
            val named = if (s.hideNames) low.subjectId.orEmpty() else low.name
            v.setTextViewText(R.id.att_lowest, named)
            v.setViewVisibility(R.id.att_lowest, if (named.isBlank()) View.GONE else View.VISIBLE)
            chip(v, R.id.att_chip, "${verdictGlyph(verdict)} $shown% ${verdict.stamp.uppercase()}", verdictColor(verdict), C.INK)
        }
    }

    private fun pendingTile(v: RemoteViews, snap: WidgetSnapshot, nowMs: Long) {
        ghost(v, R.id.pend_ghost, R.drawable.widget_ghost_tome)
        val open = pendingRows(snap.work, nowMs)
        v.setTextViewText(R.id.pend_count, open.size.toString())
        if (open.isEmpty()) {
            v.setViewVisibility(R.id.pend_next_label, View.GONE)
            v.setTextViewText(R.id.pend_title, "Nothing due.")
            v.setTextColor(R.id.pend_title, C.MUTED)
            v.setViewVisibility(R.id.pend_title, View.VISIBLE)
            v.setViewVisibility(R.id.pend_missing, View.GONE)
            v.setViewVisibility(R.id.pend_due, View.GONE)
            return
        }
        val (_, missing) = pendingCounts(snap.work, nowMs)
        if (missing > 0) chip(v, R.id.pend_missing, "$missing MISSING", C.BAD, C.INK)
        else v.setViewVisibility(R.id.pend_missing, View.GONE)

        val pick = open.first()
        val due = pick.dueAt
        when {
            due == null -> {
                v.setTextViewText(R.id.pend_next_label, "NO DEADLINE")
                v.setViewVisibility(R.id.pend_next_label, View.VISIBLE)
                v.setViewVisibility(R.id.pend_title, View.GONE)
                v.setViewVisibility(R.id.pend_due, View.GONE)
            }
            due <= nowMs -> {
                v.setTextViewText(R.id.pend_next_label, "PAST DUE")
                v.setViewVisibility(R.id.pend_next_label, View.VISIBLE)
                v.setTextViewText(R.id.pend_title, pick.title)
                v.setTextColor(R.id.pend_title, C.BONE)
                v.setViewVisibility(R.id.pend_title, View.VISIBLE)
                v.setViewVisibility(R.id.pend_due, View.GONE)
            }
            else -> {
                v.setTextViewText(R.id.pend_next_label, "NEXT DUE")
                v.setViewVisibility(R.id.pend_next_label, View.VISIBLE)
                v.setTextViewText(R.id.pend_title, pick.title)
                v.setTextColor(R.id.pend_title, C.BONE)
                v.setViewVisibility(R.id.pend_title, View.VISIBLE)
                v.setTextViewText(R.id.pend_due, "IN ${inlineCountdown(due - nowMs)}")
                v.setViewVisibility(R.id.pend_due, View.VISIBLE)
            }
        }
    }

    // ------------------------------------------------------------------ wide

    private fun todayWide(context: Context, v: RemoteViews, id: Int, s: WidgetSettings, snap: WidgetSnapshot, nowMs: Long) {
        val remaining = snap.rows.count { it.phase != ClassRowPhase.Done }
        val open = pendingRows(snap.work, nowMs)
        val strip = snap.expired
        v.setTextViewText(R.id.today_strip, "SESSION EXPIRED · TAP TO RECONNECT")
        v.setViewVisibility(R.id.today_strip, if (strip) View.VISIBLE else View.GONE)
        v.setOnClickPendingIntent(R.id.today_strip, intent(context, id * 10 + 1, OPEN_RECONNECT))
        v.setFloat(R.id.today_cols, "setAlpha", if (strip) 0.6f else 1f)

        // first paint, nothing cached, a fetch running: board 31's updating tile
        if (snap.updating && snap.syncedAt == null) {
            v.setViewVisibility(R.id.today_cols, View.GONE)
            v.setViewVisibility(R.id.today_state, View.GONE)
            v.setViewVisibility(R.id.today_skeleton, View.VISIBLE)
            v.setTextViewText(R.id.today_footer_left, "UPDATING…")
            v.setTextColor(R.id.today_footer_left, C.MUTED)
            v.setViewVisibility(R.id.today_footer_right, View.GONE)
            return
        }
        v.setViewVisibility(R.id.today_skeleton, View.GONE)

        // no schedule, or the day is genuinely over: the tile becomes a state card
        val state = when {
            snap.next is NextUp.NoSchedule -> 1
            remaining == 0 && open.isEmpty() -> 2
            else -> 0
        }
        v.setViewVisibility(R.id.today_cols, if (state == 0) View.VISIBLE else View.GONE)
        v.setViewVisibility(R.id.today_state, if (state == 0) View.GONE else View.VISIBLE)
        if (state != 0) {
            stateBlock(v, snap, s, noSchedule = state == 1, compact = false, ids = TODAY_STATE, bodyPadPx = dp(context, 64))
            if (state == 1) v.setOnClickPendingIntent(R.id.widget_root, intent(context, id * 10, OPEN_SCHEDULE))
            footer(v, R.id.today_footer_left, R.id.today_footer_right, snap, nowMs, dateToo = false)
            return
        }

        // column 1 — the next class
        v.setViewVisibility(R.id.today_next, if (s.showNext) View.VISIBLE else View.GONE)
        if (s.showNext) {
            when (val n = snap.next) {
                is NextUp.Live -> {
                    v.setTextViewText(R.id.today_next_time, clockLabel(n.startMin))
                    v.setTextViewTextSize(R.id.today_next_time, TypedValue.COMPLEX_UNIT_SP, 34f)
                    v.setTextViewText(R.id.today_next_subject, subjectName(s, n.entry))
                    markChip(v, R.id.today_next_chip, snap.markOf(n.entry))
                }
                is NextUp.Later -> {
                    v.setTextViewText(
                        R.id.today_next_time,
                        if (n.today) clockLabel(n.startMin) else "${dayShort(n.entry.day)} ${clockLabel(n.startMin)}",
                    )
                    v.setTextViewTextSize(R.id.today_next_time, TypedValue.COMPLEX_UNIT_SP, if (n.today) 34f else 24f)
                    v.setTextViewText(R.id.today_next_subject, subjectName(s, n.entry))
                    markChip(v, R.id.today_next_chip, snap.markOf(n.entry))
                }
                is NextUp.NoSchedule -> Unit
            }
        }

        // column 2 — attendance
        v.setViewVisibility(R.id.today_att, if (s.showAttendance) View.VISIBLE else View.GONE)
        if (s.showAttendance) {
            val pct = snap.percent
            if (pct == null) {
                v.setViewVisibility(R.id.today_ring, View.GONE)
                v.setTextViewText(R.id.today_percent, if (snap.signedIn) "NOT SYNCED" else "SIGN IN")
                v.setTextViewTextSize(R.id.today_percent, TypedValue.COMPLEX_UNIT_SP, 10f)
                v.setTextColor(R.id.today_percent, C.MUTED)
            } else {
                val size = dp(context, 64)
                v.setImageViewBitmap(R.id.today_ring, ringBitmap(size, pct, verdictColor(snap.overall ?: Verdict.Close)))
                v.setContentDescription(R.id.today_ring, "$pct percent")
                v.setViewVisibility(R.id.today_ring, View.VISIBLE)
                v.setTextViewText(R.id.today_percent, pct.toString())
                v.setTextViewTextSize(R.id.today_percent, TypedValue.COMPLEX_UNIT_SP, 14f)
                v.setTextColor(R.id.today_percent, C.BONE)
            }
        }

        // column 3 — pending
        v.setViewVisibility(R.id.today_pend, if (s.showPending) View.VISIBLE else View.GONE)
        if (s.showPending) {
            v.setTextViewText(R.id.today_pend_count, open.size.toString())
            v.setTextViewTextSize(R.id.today_pend_count, TypedValue.COMPLEX_UNIT_SP, if (strip) 46f else 52f)
            val (_, missing) = pendingCounts(snap.work, nowMs)
            if (missing > 0) {
                v.setTextViewText(R.id.today_pend_missing, "$missing MISSING")
                v.setViewVisibility(R.id.today_pend_missing, View.VISIBLE)
            } else {
                v.setViewVisibility(R.id.today_pend_missing, View.GONE)
            }
        }

        footer(v, R.id.today_footer_left, R.id.today_footer_right, snap, nowMs, dateToo = !strip)
    }

    private fun classesWide(context: Context, v: RemoteViews, id: Int, s: WidgetSettings, snap: WidgetSnapshot, nowMs: Long) {
        v.setTextViewText(R.id.classes_header_left, "TODAY · ${dayHeading(snap.date)}")
        val (sync, warn) = syncLine(snap.syncedAt, snap.expired, nowMs)
        val at = snap.syncedAt
        v.setTextViewText(R.id.classes_sync, if (at == null) sync else ageText(at, nowMs))
        v.setTextColor(R.id.classes_sync, if (warn) C.WARN else C.MUTED)

        val remaining = snap.rows.filter { it.phase != ClassRowPhase.Done }
        val state = when {
            snap.next is NextUp.NoSchedule -> 1
            remaining.isEmpty() -> 2
            else -> 0
        }
        val shown = remaining.take(s.classes.coerceIn(1, 3))
        for (i in 0 until 3) {
            v.setViewVisibility(classesRowId(i), if (state == 0 && i < shown.size) View.VISIBLE else View.GONE)
        }
        v.setViewVisibility(R.id.classes_state, if (state == 0) View.GONE else View.VISIBLE)
        if (state != 0) {
            v.setViewVisibility(R.id.classes_more, View.GONE)
            stateBlock(v, snap, s, noSchedule = state == 1, compact = true, ids = CLASSES_STATE, bodyPadPx = dp(context, 64))
            if (state == 1) v.setOnClickPendingIntent(R.id.widget_root, intent(context, id * 10, OPEN_SCHEDULE))
            return
        }

        // the boards' "current/next" bar: the live row, or the next one when nothing is live
        val liveIdx = shown.indexOfFirst { it.phase == ClassRowPhase.Live }
        val barIdx = if (liveIdx >= 0) liveIdx else 0
        val barPad = dp(context, 10)
        shown.forEachIndexed { i, row ->
            val bar = i == barIdx
            v.setViewVisibility(classesBarId(i), if (bar) View.VISIBLE else View.GONE)
            v.setViewPadding(classesTimeId(i), if (bar) barPad else 0, 0, 0, 0)
            v.setTextViewText(classesTimeId(i), clockLabel(row.startMin))
            v.setTextViewText(classesSubjectId(i), subjectName(s, row.entry))
            markChip(v, classesChipId(i), snap.markOf(row.entry))
            v.setOnClickPendingIntent(classesRowId(i), intent(context, id * 10 + 2 + i, OPEN_SCHEDULE))
        }
        val more = remaining.size - shown.size
        if (more > 0) {
            v.setTextViewText(R.id.classes_more, moreToday(more, snap.minutesToday))
            v.setViewVisibility(R.id.classes_more, View.VISIBLE)
        } else {
            v.setViewVisibility(R.id.classes_more, View.GONE)
        }
    }

    private fun dashboard(context: Context, v: RemoteViews, id: Int, s: WidgetSettings, snap: WidgetSnapshot, nowMs: Long) {
        v.setTextViewText(R.id.dash_title, "Today")
        val (sync, warn) = syncLine(snap.syncedAt, snap.expired, nowMs)
        v.setTextViewText(R.id.dash_sync, "${dayHeading(snap.date)} · $sync")
        v.setTextColor(R.id.dash_sync, if (warn) C.WARN else C.MUTED)
        v.setTextViewText(R.id.dash_strip, "SESSION EXPIRED · TAP TO RECONNECT")
        v.setViewVisibility(R.id.dash_strip, if (snap.expired) View.VISIBLE else View.GONE)
        v.setOnClickPendingIntent(R.id.dash_strip, intent(context, id * 10 + 1, OPEN_RECONNECT))
        v.setFloat(R.id.dash_content, "setAlpha", if (snap.expired) 0.6f else 1f)

        val open = pendingRows(snap.work, nowMs)
        val remaining = snap.rows.count { it.phase != ClassRowPhase.Done }
        val state = when {
            snap.next is NextUp.NoSchedule -> 1
            remaining == 0 && open.isEmpty() -> 2
            else -> 0
        }
        val shownRows = snap.rows.take(s.classes.coerceIn(1, 3))
        if (state == 0) {
            val liveIdx = shownRows.indexOfFirst { it.phase == ClassRowPhase.Live }
            val barIdx = if (liveIdx >= 0) liveIdx else shownRows.indexOfFirst { it.phase == ClassRowPhase.Upcoming }
            val barPad = dp(context, 10)
            shownRows.forEachIndexed { i, row ->
                val bar = i == barIdx
                v.setViewVisibility(dashRowId(i), View.VISIBLE)
                v.setViewVisibility(dashBarId(i), if (bar) View.VISIBLE else View.GONE)
                v.setViewPadding(dashTimeId(i), if (bar) barPad else 0, 0, 0, 0)
                v.setFloat(dashRowId(i), "setAlpha", if (row.phase == ClassRowPhase.Done) 0.5f else 1f)
                v.setTextViewText(dashTimeId(i), clockLabel(row.startMin))
                v.setTextViewText(dashSubjectId(i), subjectName(s, row.entry))
                markChip(v, dashChipId(i), snap.markOf(row.entry))
                v.setOnClickPendingIntent(dashRowId(i), intent(context, id * 10 + 2 + i, OPEN_SCHEDULE))
            }
            for (i in shownRows.size until 3) v.setViewVisibility(dashRowId(i), View.GONE)
            v.setViewVisibility(R.id.dash_state, View.GONE)
        } else {
            for (i in 0 until 3) v.setViewVisibility(dashRowId(i), View.GONE)
            v.setViewVisibility(R.id.dash_state, View.VISIBLE)
            stateBlock(
                v, snap, s, noSchedule = state == 1, compact = true,
                ids = DASH_STATE, bodyPadPx = dp(context, 56),
            )
            if (state == 1) v.setOnClickPendingIntent(R.id.widget_root, intent(context, id * 10, OPEN_SCHEDULE))
        }
        val more = snap.rows.size - shownRows.size
        if (state == 0 && more > 0) {
            v.setTextViewText(R.id.dash_more, "+$more MORE TODAY")
            v.setViewVisibility(R.id.dash_more, View.VISIBLE)
        } else {
            v.setViewVisibility(R.id.dash_more, View.GONE)
        }

        // attendance: ring left, up to three metered subjects right
        val subjects = snap.subjects.filter { it.percent != null }.take(3)
        val pct = snap.percent
        if (pct == null) {
            v.setViewVisibility(R.id.dash_ring, View.GONE)
            v.setTextViewText(R.id.dash_percent, if (snap.signedIn) "NOT SYNCED" else "SIGN IN")
            v.setTextViewTextSize(R.id.dash_percent, TypedValue.COMPLEX_UNIT_SP, 10f)
            v.setTextColor(R.id.dash_percent, C.MUTED)
        } else {
            val size = dp(context, 52)
            v.setImageViewBitmap(R.id.dash_ring, ringBitmap(size, pct, verdictColor(snap.overall ?: Verdict.Close)))
            v.setContentDescription(R.id.dash_ring, "$pct percent")
            v.setViewVisibility(R.id.dash_ring, View.VISIBLE)
            v.setTextViewText(R.id.dash_percent, pct.toString())
            v.setTextViewTextSize(R.id.dash_percent, TypedValue.COMPLEX_UNIT_SP, 11f)
            v.setTextColor(R.id.dash_percent, C.BONE)
        }
        v.setOnClickPendingIntent(R.id.dash_att, intent(context, id * 10 + 5, OPEN_ATTENDANCE))
        for (i in 0 until 3) {
            val subject = subjects.getOrNull(i)
            if (subject == null) {
                v.setViewVisibility(dashStripRowId(i), View.GONE)
                continue
            }
            v.setViewVisibility(dashStripRowId(i), View.VISIBLE)
            val value = (subject.percent ?: 0.0).toInt()
            v.setTextViewText(dashCodeId(i), if (s.hideNames) "${i + 1}" else subjectCode(subject.name))
            v.setTextViewText(dashPctId(i), value.toString())
            v.setImageViewBitmap(
                dashMeterId(i),
                meterBitmap(dp(context, 6), value, verdictColor(verdictOf(value.toDouble(), snap.target, snap.band))),
            )
        }

        // pending: the label carries the count, then the two most urgent pieces
        v.setTextViewText(R.id.dash_pend_label, "PENDING · ${open.size}")
        if (open.isEmpty()) {
            v.setViewVisibility(R.id.dash_work1, View.VISIBLE)
            v.setTextViewText(R.id.dash_work1_title, "Nothing due.")
            v.setTextColor(R.id.dash_work1_title, C.MUTED)
            v.setViewVisibility(R.id.dash_work1_course, View.GONE)
            v.setViewVisibility(R.id.dash_work1_chip, View.GONE)
            v.setViewVisibility(R.id.dash_work2, View.GONE)
        } else {
            v.setViewVisibility(R.id.dash_work1, View.VISIBLE)
            workRow(v, 1, open.first(), s, nowMs)
            val second = open.getOrNull(1)
            if (second != null) {
                v.setViewVisibility(R.id.dash_work2, View.VISIBLE)
                workRow(v, 2, second, s, nowMs)
            } else {
                v.setViewVisibility(R.id.dash_work2, View.GONE)
            }
        }
        v.setOnClickPendingIntent(R.id.dash_work1, intent(context, id * 10 + 6, OPEN_ASSIGNMENTS))
        v.setOnClickPendingIntent(R.id.dash_work2, intent(context, id * 10 + 7, OPEN_ASSIGNMENTS))
    }

    private fun workRow(v: RemoteViews, slot: Int, work: Work, s: WidgetSettings, nowMs: Long) {
        val titleId = rowId(slot, 0, R.id.dash_work1_title, R.id.dash_work2_title)
        val courseId = rowId(slot, 0, R.id.dash_work1_course, R.id.dash_work2_course)
        val chipId = rowId(slot, 0, R.id.dash_work1_chip, R.id.dash_work2_chip)
        v.setTextViewText(titleId, work.title)
        v.setTextColor(titleId, C.BONE)
        val course = if (s.hideNames) work.courseId else work.courseName.uppercase()
        v.setTextViewText(courseId, course)
        v.setViewVisibility(courseId, if (course.isBlank()) View.GONE else View.VISIBLE)
        val due = work.dueAt
        when {
            due != null && due < nowMs -> chip(v, chipId, "MISSING", C.BAD, C.INK)
            due != null -> chip(v, chipId, "IN ${inlineCountdown(due - nowMs)}", C.LINE, C.BONE)
            else -> v.setViewVisibility(chipId, View.GONE)
        }
    }

    /** the state card the wide widgets share (board 31: no schedule / all clear) */
    private fun stateBlock(
        v: RemoteViews,
        snap: WidgetSnapshot,
        s: WidgetSettings,
        noSchedule: Boolean,
        compact: Boolean,
        ids: StateIds,
        bodyPadPx: Int,
    ) {
        if (noSchedule) {
            // the tablet sits above the title, so the body starts below it
            stateArt(v, ids.tablet, ids.ghost, R.drawable.widget_ghost_tablet)
            v.setViewPadding(ids.body, 0, bodyPadPx, 0, 0)
            v.setTextViewText(ids.title, "No schedule")
            v.setTextViewTextSize(ids.title, TypedValue.COMPLEX_UNIT_SP, if (compact) 22f else 26f)
            v.setViewVisibility(ids.sub, View.GONE)
            v.setViewVisibility(ids.next, View.GONE)
            chip(v, ids.chip, "TAP TO CREATE", C.BONE, C.INK)
        } else {
            // the moon keeps the card's top-right corner; the text runs top-left
            stateArt(v, ids.ghost, ids.tablet, R.drawable.widget_ghost_moon)
            v.setViewPadding(ids.body, 0, 0, 0, 0)
            v.setTextViewText(ids.title, "All clear")
            v.setTextViewTextSize(ids.title, TypedValue.COMPLEX_UNIT_SP, if (compact) 26f else 34f)
            v.setTextViewText(ids.sub, "No more classes today. Nothing due.")
            v.setViewVisibility(ids.sub, View.VISIBLE)
            val later = snap.next as? NextUp.Later
            if (later != null) {
                val name = subjectName(s, later.entry).uppercase()
                v.setTextViewText(ids.next, "NEXT: ${dayShort(later.entry.day)} ${clockLabel(later.startMin)} · $name")
                v.setViewVisibility(ids.next, View.VISIBLE)
            } else {
                v.setViewVisibility(ids.next, View.GONE)
            }
            v.setViewVisibility(ids.chip, View.GONE)
        }
    }

    /** the state card's ids, one row per widget whose board pins a card */
    private data class StateIds(
        val body: Int,
        val ghost: Int,
        val tablet: Int,
        val title: Int,
        val sub: Int,
        val next: Int,
        val chip: Int,
    )

    private val TODAY_STATE = StateIds(
        R.id.today_state_body, R.id.today_state_ghost, R.id.today_state_tablet,
        R.id.today_state_title, R.id.today_state_sub, R.id.today_state_next, R.id.today_state_chip,
    )

    private val CLASSES_STATE = StateIds(
        R.id.classes_state_body, R.id.classes_state_ghost, R.id.classes_state_tablet,
        R.id.classes_state_title, R.id.classes_state_sub, R.id.classes_state_next, R.id.classes_state_chip,
    )

    private val DASH_STATE = StateIds(
        R.id.dash_state_body, R.id.dash_state_ghost, R.id.dash_state_tablet,
        R.id.dash_state_title, R.id.dash_state_sub, R.id.dash_state_next, R.id.dash_state_chip,
    )

    // ------------------------------------------------------------- id tables

    private fun rowId(i: Int, fallback: Int, a: Int, b: Int): Int = when (i) {
        0 -> a
        1 -> b
        else -> fallback
    }

    private fun classesRowId(i: Int) = rowId(i, R.id.classes_row3, R.id.classes_row1, R.id.classes_row2)
    private fun classesBarId(i: Int) = rowId(i, R.id.classes_bar3, R.id.classes_bar1, R.id.classes_bar2)
    private fun classesTimeId(i: Int) = rowId(i, R.id.classes_time3, R.id.classes_time1, R.id.classes_time2)
    private fun classesSubjectId(i: Int) = rowId(i, R.id.classes_subject3, R.id.classes_subject1, R.id.classes_subject2)
    private fun classesChipId(i: Int) = rowId(i, R.id.classes_chip3, R.id.classes_chip1, R.id.classes_chip2)
    private fun dashRowId(i: Int) = rowId(i, R.id.dash_row3, R.id.dash_row1, R.id.dash_row2)
    private fun dashBarId(i: Int) = rowId(i, R.id.dash_bar3, R.id.dash_bar1, R.id.dash_bar2)
    private fun dashTimeId(i: Int) = rowId(i, R.id.dash_time3, R.id.dash_time1, R.id.dash_time2)
    private fun dashSubjectId(i: Int) = rowId(i, R.id.dash_subject3, R.id.dash_subject1, R.id.dash_subject2)
    private fun dashChipId(i: Int) = rowId(i, R.id.dash_chip3, R.id.dash_chip1, R.id.dash_chip2)
    private fun dashStripRowId(i: Int) = rowId(i, R.id.dash_strip3, R.id.dash_strip1, R.id.dash_strip2)
    private fun dashCodeId(i: Int) = rowId(i, R.id.dash_code3, R.id.dash_code1, R.id.dash_code2)
    private fun dashMeterId(i: Int) = rowId(i, R.id.dash_meter3, R.id.dash_meter1, R.id.dash_meter2)
    private fun dashPctId(i: Int) = rowId(i, R.id.dash_pct3, R.id.dash_pct1, R.id.dash_pct2)
}

/** the ghost art sits at the boards' faintness; one constant, tuned on the emulator pass */
private object Ghost {
    const val ALPHA = 0.10f
}
