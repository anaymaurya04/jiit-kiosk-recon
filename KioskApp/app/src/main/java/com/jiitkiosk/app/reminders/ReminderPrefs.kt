package com.jiitkiosk.app.reminders

import android.content.Context

/**
 * The reminder settings, plain prefs — none of this is a secret. Defaults
 * match board 21: on, all three leads, quiet 22:00–07:00, every course on.
 */
class ReminderPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    data class Values(
        val enabled: Boolean,
        val leads: Set<Int>,
        val quietEnabled: Boolean,
        val quietStartMin: Int,
        val quietEndMin: Int,
        val offCourses: Set<String>,
    ) {
        fun courseOn(courseId: String) = courseId !in offCourses
    }

    fun values(): Values = Values(
        enabled = prefs.getBoolean("enabled", true),
        leads = buildSet {
            if (prefs.getBoolean("lead_24", true)) add(24)
            if (prefs.getBoolean("lead_6", true)) add(6)
            if (prefs.getBoolean("lead_1", true)) add(1)
        },
        quietEnabled = prefs.getBoolean("quiet", true),
        quietStartMin = prefs.getInt("quiet_start", 22 * 60),
        quietEndMin = prefs.getInt("quiet_end", 7 * 60),
        offCourses = prefs.getStringSet("courses_off", emptySet()).orEmpty().toSet(),
    )

    fun setEnabled(on: Boolean) = prefs.edit().putBoolean("enabled", on).apply()

    fun setLead(hours: Int, on: Boolean) = prefs.edit().putBoolean("lead_$hours", on).apply()

    fun setQuiet(on: Boolean) = prefs.edit().putBoolean("quiet", on).apply()

    fun setCourse(courseId: String, on: Boolean) {
        val off = values().offCourses.toMutableSet()
        if (on) off.remove(courseId) else off.add(courseId)
        prefs.edit().putStringSet("courses_off", off).apply()
    }

    fun clear() = prefs.edit().clear().apply()

    companion object {
        const val FILE = "kiosk_reminders"
    }
}
