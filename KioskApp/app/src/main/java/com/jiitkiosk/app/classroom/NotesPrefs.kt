package com.jiitkiosk.app.classroom

import android.content.Context

/**
 * Which materials have been opened on this phone — the NEW tag's memory. Kept
 * as ids in prefs, trimmed so it cannot grow forever.
 */
class NotesPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("notes", Context.MODE_PRIVATE)

    fun seen(): Set<String> = prefs.getStringSet("seen", emptySet()) ?: emptySet()

    fun mark(id: String) {
        val next = (seen() + id).toList().takeLast(MAX).toSet()
        prefs.edit().putStringSet("seen", next).apply()
    }

    private companion object {
        const val MAX = 500
    }
}
