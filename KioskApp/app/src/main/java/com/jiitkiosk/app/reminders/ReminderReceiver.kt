package com.jiitkiosk.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** an alarm fired: post the reminder */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        Notifications.post(
            context = context,
            workId = id,
            lead = intent.getIntExtra(EXTRA_LEAD, 0),
            title = title,
            course = intent.getStringExtra(EXTRA_COURSE).orEmpty(),
            link = intent.getStringExtra(EXTRA_LINK),
        )
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_LEAD = "lead"
        const val EXTRA_TITLE = "title"
        const val EXTRA_COURSE = "course"
        const val EXTRA_LINK = "link"
    }
}
