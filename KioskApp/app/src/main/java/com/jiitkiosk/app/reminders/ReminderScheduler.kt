package com.jiitkiosk.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

/**
 * Alarms, not WorkManager: the 15-minute floor is right for refreshing the
 * data, but a deadline reminder that can be 15 minutes late is a lie. These use
 * setAndAllowWhileIdle, which the system may still defer in Doze — the drift
 * this app chooses over demanding the exact-alarm permission.
 *
 * [sync] cancels every alarm of the previous plan and lays the new one, so a
 * submitted, deleted or re-dated assignment drops out simply by being absent.
 */
object ReminderScheduler {

    fun sync(context: Context, reminders: List<Reminder>) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for ((id, lead) in previously(context)) alarms.cancel(bare(context, id, lead))
        for (r in reminders) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.fireAt, withPayload(context, r))
        }
        remember(context, reminders)
    }

    private fun withPayload(context: Context, r: Reminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(action(r.work.id, r.leadHours))
            .putExtra(ReminderReceiver.EXTRA_ID, r.work.id)
            .putExtra(ReminderReceiver.EXTRA_LEAD, r.leadHours)
            .putExtra(ReminderReceiver.EXTRA_TITLE, r.work.title)
            .putExtra(ReminderReceiver.EXTRA_COURSE, r.work.courseName)
            .putExtra(ReminderReceiver.EXTRA_LINK, r.work.link)
        return PendingIntent.getBroadcast(
            context,
            action(r.work.id, r.leadHours).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** the same identity without payload, for cancelling */
    private fun bare(context: Context, workId: String, lead: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            action(workId, lead).hashCode(),
            Intent(context, ReminderReceiver::class.java).setAction(action(workId, lead)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun action(workId: String, lead: Int) = "com.jiitkiosk.app.REMINDER/$workId/$lead"

    private fun remember(context: Context, reminders: List<Reminder>) {
        val array = JSONArray()
        for (r in reminders) array.put(JSONObject().put("id", r.work.id).put("lead", r.leadHours))
        // commit(), not apply(): a lost record means orphan alarms that fire with
        // nothing able to cancel them
        context.getSharedPreferences(ReminderPrefs.FILE, Context.MODE_PRIVATE)
            .edit().putString(SCHEDULED, array.toString()).commit()
    }

    private fun previously(context: Context): List<Pair<String, Int>> {
        val raw = context.getSharedPreferences(ReminderPrefs.FILE, Context.MODE_PRIVATE)
            .getString(SCHEDULED, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map {
                array.getJSONObject(it).let { o -> o.getString("id") to o.getInt("lead") }
            }
        }.getOrDefault(emptyList())
    }

    private const val SCHEDULED = "scheduled"
}
