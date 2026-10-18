package com.jiitkiosk.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jiitkiosk.app.widget.WidgetAlarms

/**
 * A restart or an app update wipes the alarm table but keeps the settings and
 * the cache, so the plan is simply laid again from what is already on disk.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                runCatching { ReminderSync.refresh(context.applicationContext) }
                // the widget alarms died with the alarm table too, and no fetch is in flight
                runCatching { WidgetAlarms.forgetUpdating(context.applicationContext) }
                runCatching { WidgetAlarms.sync(context.applicationContext) }
            }
        }
    }
}
