package com.jiitkiosk.app

import android.content.Context
import `in`.codelif.ktjiit.auth.Session
import `in`.codelif.ktjiit.http.Transport

/**
 * The portal session survives app restarts.
 *
 * Plain app-private SharedPreferences: the file is sandboxed to this uid and
 * already encrypted at rest on modern Android, and the only thing in it is the
 * portal's own bearer token. Nothing is uploaded anywhere.
 */
class SessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("kiosk", Context.MODE_PRIVATE)

    fun load(): Session? = prefs.getString(SESSION, null)?.let {
        runCatching { Transport.json.decodeFromString(Session.serializer(), it) }.getOrNull()
    }

    fun save(session: Session) {
        prefs.edit().putString(SESSION, Transport.json.encodeToString(Session.serializer(), session)).apply()
    }

    fun clear() {
        // only the session goes: the appearance setting is not the session's business
        prefs.edit().remove(SESSION).apply()
    }

    private companion object {
        const val SESSION = "session"
    }
}
