package com.jiitkiosk.app.code

import android.content.Context

/** The code-profile handles: plain prefs, none of this is a secret. */
class CodePrefs(context: Context) {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun leetcode(): String? = prefs.getString("leetcode", null)?.trim()?.takeIf { it.isNotEmpty() }

    /** blank clears the handle, which is also how the Code tab goes back to "not linked" */
    fun setLeetcode(handle: String?) {
        prefs.edit().apply {
            val clean = handle?.trim()
            if (clean.isNullOrEmpty()) remove("leetcode") else putString("leetcode", clean)
        }.apply()
    }

    companion object {
        const val FILE = "kiosk_code"
    }
}
