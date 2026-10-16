package com.jiitkiosk.app.code

import android.content.Context
import android.util.Base64
import com.jiitkiosk.app.secure.Secrets

/**
 * The code-profile handles and the GitHub token. The handles are plain prefs —
 * nobody signs in with them; the token never touches the file in the clear.
 */
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

    fun github(): String? = prefs.getString("github", null)?.trim()?.takeIf { it.isNotEmpty() }

    fun setGithub(handle: String?) {
        prefs.edit().apply {
            val clean = handle?.trim()
            if (clean.isNullOrEmpty()) remove("github") else putString("github", clean)
        }.apply()
    }

    /** sealed through the Android Keystore, exactly like the portal session; blank clears it */
    fun githubToken(): String? {
        val sealed = prefs.getString(TOKEN, null) ?: return null
        val plain = runCatching { Secrets.open(Base64.decode(sealed, Base64.NO_WRAP)) }.getOrNull() ?: return null
        return String(plain, Charsets.UTF_8).takeIf { it.isNotBlank() }
    }

    fun setGithubToken(token: String?) {
        val clean = token?.trim()
        prefs.edit().apply {
            if (clean.isNullOrEmpty()) remove(TOKEN)
            else putString(
                TOKEN,
                Base64.encodeToString(Secrets.seal(clean.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP),
            )
        }.commit()
    }

    /** validators per handle so a changed handle can never reuse another account's ETag */
    fun etags(handle: String): GhEtags = GhEtags(
        user = prefs.getString("etag:user:$handle", null),
        repos = prefs.getString("etag:repos:$handle", null),
        events = prefs.getString("etag:events:$handle", null),
    )

    fun setEtags(handle: String, etags: GhEtags) {
        prefs.edit().apply {
            putString("etag:user:$handle", etags.user)
            putString("etag:repos:$handle", etags.repos)
            putString("etag:events:$handle", etags.events)
        }.apply()
    }

    companion object {
        const val FILE = "kiosk_code"
        private const val TOKEN = "github_token_sealed"
    }
}
