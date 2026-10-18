package com.jiitkiosk.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * The alarm half of W4. Same shape as the reminders: every re-lay cancels the
 * previous plan and lays the new one, the record lives beside the app's other
 * settings, and [sync] is idempotent — the worker, the app open, the tick itself
 * and boot all call it.
 */
object WidgetAlarms {

    const val ACTION = "com.jiitkiosk.app.WIDGET_TICK"
    private const val FILE = "kiosk"
    private const val SCHEDULED = "widget_alarms"

    fun sync(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val previous = scheduled(prefs)
        for (at in previous) alarmManager.cancel(bare(context, at))

        val entries = com.jiitkiosk.app.schedule.ScheduleStore.load(context.filesDir)?.entries.orEmpty()
        val plan = WidgetPlanner.plan(entries, java.time.LocalDateTime.now(), java.time.ZoneId.systemDefault())
        val record = org.json.JSONArray()
        for (at in plan) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent(context, at))
            record.put(at)
        }
        // commit, not apply: a lost record means orphan alarms nothing can cancel
        prefs.edit().putString(SCHEDULED, record.toString()).commit()
    }

    /** the same identity without payload, for cancelling */
    private fun bare(context: Context, at: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            at.hashCode(),
            Intent(context, WidgetTickReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun intent(context: Context, at: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            at.hashCode(),
            Intent(context, WidgetTickReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun scheduled(prefs: android.content.SharedPreferences): List<Long> {
        val raw = prefs.getString(SCHEDULED, null) ?: return emptyList()
        return runCatching {
            val array = org.json.JSONArray(raw)
            (0 until array.length()).map { array.getLong(it) }
        }.getOrDefault(emptyList())
    }

    /** any in-flight fetch is long dead when boot happens: the tile stops claiming UPDATING */
    fun forgetUpdating(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean("widget_updating", false).commit()
    }
}
