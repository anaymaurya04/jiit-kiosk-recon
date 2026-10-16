package com.jiitkiosk.app

import android.content.Context
import android.util.Base64
import com.jiitkiosk.app.secure.Secrets
import `in`.codelif.ktjiit.auth.Session
import `in`.codelif.ktjiit.http.Transport

/**
 * The portal session survives app restarts.
 *
 * Sealed with the Android Keystore's AES-GCM key before it touches the file, so
 * the bearer token is unreadable even from a pulled copy of the app's data.
 * Writes use commit(), not apply(): a process killed moments after login must
 * still find the session, and that is exactly what apply() can lose.
 * Legacy plain-JSON entries are migrated on the first read.
 */
class SessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("kiosk", Context.MODE_PRIVATE)

    fun load(): Session? {
        prefs.getString(LEGACY, null)?.let { plain ->
            // pre-Keystore builds stored the session in the clear; seal it once and move on
            val session = runCatching { Transport.json.decodeFromString(Session.serializer(), plain) }.getOrNull()
            prefs.edit().remove(LEGACY)
                .apply { if (session != null) putString(SESSION, sealed(session)) }
                .commit()
            return session
        }
        val sealed = prefs.getString(SESSION, null) ?: return null
        val json = runCatching { Secrets.open(Base64.decode(sealed, Base64.NO_WRAP)) }.getOrNull()
            ?.toString(Charsets.UTF_8) ?: return null
        return runCatching { Transport.json.decodeFromString(Session.serializer(), json) }.getOrNull()
    }

    fun save(session: Session) {
        prefs.edit().putString(SESSION, sealed(session)).commit()
    }

    fun clear() {
        // only the session goes: the appearance setting is not the session's business
        prefs.edit().remove(SESSION).remove(LEGACY).commit()
    }

    private fun sealed(session: Session): String {
        val json = Transport.json.encodeToString(Session.serializer(), session).toByteArray()
        return Base64.encodeToString(Secrets.seal(json), Base64.NO_WRAP)
    }

    private companion object {
        const val SESSION = "session_sealed"
        const val LEGACY = "session"
    }
}
