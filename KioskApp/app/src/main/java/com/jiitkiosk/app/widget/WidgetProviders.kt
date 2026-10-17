package com.jiitkiosk.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

/**
 * The six widget providers differ only in which kind they paint; everything
 * real lives in [WidgetRender]. Adding or resizing one repaints it, removing
 * one drops its settings.
 */
abstract class WidgetProvider : AppWidgetProvider() {

    abstract val kind: WidgetKind

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        WidgetRender.paint(context, kind, ids)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle?) {
        WidgetRender.paint(context, kind, intArrayOf(id))
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        WidgetPrefs.clear(context, ids)
    }
}

class NextClassWidget : WidgetProvider() {
    override val kind = WidgetKind.Next
}

class AttendanceWidget : WidgetProvider() {
    override val kind = WidgetKind.Attendance
}

class PendingWidget : WidgetProvider() {
    override val kind = WidgetKind.Pending
}

class ClassesWidget : WidgetProvider() {
    override val kind = WidgetKind.Classes
}

class DashboardWidget : WidgetProvider() {
    override val kind = WidgetKind.Dashboard
}
