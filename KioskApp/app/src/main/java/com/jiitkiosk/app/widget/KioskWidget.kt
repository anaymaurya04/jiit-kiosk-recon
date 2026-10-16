package com.jiitkiosk.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.jiitkiosk.app.MainActivity
import com.jiitkiosk.app.R
import com.jiitkiosk.app.SessionStore
import com.jiitkiosk.app.classroom.ClassroomData
import com.jiitkiosk.app.data.Cache
import com.jiitkiosk.app.ui.attendance.Sheet
import com.jiitkiosk.app.ui.components.ago
import `in`.codelif.ktjiit.model.AttendanceMeta

/**
 * The home-screen glance: the attendance hero's number and the classroom's next
 * deadline, read from the same cache files the screens read. No network, no
 * writes, no Glance — RemoteViews only. The launcher repaints it every 30
 * minutes, and every Classroom sync pushes one.
 */
class KioskWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = views(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    companion object {

        /** repaint whatever copy is on the home screen right now */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, KioskWidget::class.java))
            if (ids.isEmpty()) return
            val views = views(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun views(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_kiosk)
            val cache = Cache(context.filesDir)
            val now = System.currentTimeMillis()

            // the attendance cache is keyed by the session's token, exactly like the screen's
            val token = SessionStore(context).load()?.token
            val semesters = token?.let {
                cache.read("att-meta/$it", AttendanceMeta.serializer())?.value?.semesters
            }.orEmpty()
            val sheet = token?.let { t ->
                semesters.firstOrNull()?.let { cache.read("att/$t/${it.id}", Sheet.serializer()) }
            }
            val work = cache.read(ClassroomData.CACHE_KEY, ClassroomData.serializer())

            val percent = sheet?.value?.detail?.subjects?.mapNotNull { it.percent }?.let { percentOf(it) }
            val next = work?.value?.work?.let { nextOf(it, now) }

            views.setTextViewText(R.id.widget_percent, percent?.toString() ?: "—")
            views.setViewVisibility(R.id.widget_pct_sign, if (percent != null) View.VISIBLE else View.GONE)
            views.setProgressBar(R.id.widget_meter, 100, percent ?: 0, false)
            views.setTextViewText(
                R.id.widget_next,
                when {
                    next != null -> next.courseName
                    work != null -> "NOTHING DUE"
                    else -> "—"
                },
            )
            val pill = next?.dueAt?.let { "${countdownText(it - now)} remaining" }.orEmpty()
            views.setTextViewText(R.id.widget_pill, pill)
            views.setViewVisibility(R.id.widget_pill, if (pill.isEmpty()) View.GONE else View.VISIBLE)
            views.setTextViewText(R.id.widget_semester, semesters.firstOrNull()?.description?.uppercase().orEmpty())

            // the older of the two sources is the honest age of what is on screen;
            // a missing attendance sheet says so instead of pretending freshness
            val at = listOfNotNull(sheet?.at, work?.at).minOrNull()
            views.setTextViewText(
                R.id.widget_fresh,
                when {
                    sheet != null && at != null -> "UPDATED ${ago(at)}"
                    else -> "OPEN THE APP TO SYNC"
                },
            )

            val pending = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            return views
        }
    }
}
