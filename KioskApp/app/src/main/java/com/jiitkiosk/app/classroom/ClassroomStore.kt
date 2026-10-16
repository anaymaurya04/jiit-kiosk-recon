package com.jiitkiosk.app.classroom

import android.content.Context
import android.util.Base64
import com.jiitkiosk.app.secure.Secrets
import org.json.JSONObject

/**
 * The Classroom OAuth access token, sealed exactly like the portal session:
 * Keystore AES-GCM before it touches the file, commit() so a process death
 * cannot lose it. The expiry rides along so a screen can re-authorize at the
 * right moment instead of one 401 late.
 */
class ClassroomStore(context: Context) {

    private val prefs = context.getSharedPreferences("kiosk", Context.MODE_PRIVATE)

    fun token(): String? = payload()?.optString("token")?.takeIf { it.isNotBlank() }

    fun expiresAt(): Long = payload()?.optLong("expiresAt") ?: 0L

    fun save(token: String, expiresAt: Long) {
        val json = JSONObject().put("token", token).put("expiresAt", expiresAt).toString().toByteArray()
        prefs.edit().putString(TOKEN, Base64.encodeToString(Secrets.seal(json), Base64.NO_WRAP)).commit()
    }

    fun clear() {
        prefs.edit().remove(TOKEN).commit()
    }

    private fun payload(): JSONObject? {
        val sealed = prefs.getString(TOKEN, null) ?: return null
        val json = runCatching { Secrets.open(Base64.decode(sealed, Base64.NO_WRAP)) }.getOrNull() ?: return null
        return runCatching { JSONObject(json.toString(Charsets.UTF_8)) }.getOrNull()
    }

    private companion object {
        const val TOKEN = "classroom_sealed"
    }
}
