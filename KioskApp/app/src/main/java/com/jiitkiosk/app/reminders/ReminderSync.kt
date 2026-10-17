package com.jiitkiosk.app.reminders

import android.content.Context
import com.jiitkiosk.app.classroom.ClassroomData
import com.jiitkiosk.app.classroom.Work
import com.jiitkiosk.app.data.Cache
import java.time.ZoneId

/**
 * One path from "here is the coursework" to "here are the alarms". The Class
 * tab calls it after every fresh fetch (which is how a just-submitted
 * assignment's reminders stop), the worker calls it every 15 minutes, and the
 * boot receiver calls it after a restart wiped the alarm table.
 */
object ReminderSync {

    fun refresh(context: Context, data: ClassroomData? = null): List<Reminder> {
        val values = ReminderPrefs(context).values()
        val work = workFor(context, data, values.enabled)
        val plan = ReminderPlanner.plan(work, values, System.currentTimeMillis(), ZoneId.systemDefault())
        ReminderScheduler.sync(context, plan)
        return plan
    }

    /** the plan the UI shows, without touching the alarms */
    fun preview(context: Context, data: ClassroomData? = null): List<Reminder> {
        val values = ReminderPrefs(context).values()
        val work = workFor(context, data, values.enabled)
        return ReminderPlanner.plan(work, values, System.currentTimeMillis(), ZoneId.systemDefault())
    }

    private fun workFor(context: Context, data: ClassroomData?, enabled: Boolean): List<ReminderWork> {
        if (!enabled) return emptyList()
        val source = data ?: Cache(context.filesDir)
            .read(ClassroomData.CACHE_KEY, ClassroomData.serializer())?.value
        return source?.work?.map { it.asReminderWork() } ?: emptyList()
    }
}

/** the one mapping, so the alarms and the card's reminder line can never drift */
internal fun Work.asReminderWork() = ReminderWork(
    id = id,
    courseId = courseId,
    courseName = courseName,
    title = title,
    dueAt = dueAt,
    submitted = submitted,
    link = link,
)
