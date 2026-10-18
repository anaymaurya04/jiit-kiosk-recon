package com.jiitkiosk.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * An alarm fired (a class moved, the day turned), or the clock/date/timezone
 * moved under the plan. Every fire repaints the widgets from the caches and
 * re-lays the alarms — the scheduler perpetuates itself through exactly one
 * entry point, so the plan can never drift from the schedule it came from.
 */
class WidgetTickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        KioskWidget.refresh(app)
        WidgetAlarms.sync(app)
    }
}
