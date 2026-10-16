package com.jiitkiosk.app.updates

import android.content.Context
import com.jiitkiosk.app.data.HttpError
import com.jiitkiosk.app.data.HttpJson
import java.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** what a check of the releases feed found */
sealed interface UpdateCheck {
    /** a release newer than the installed version */
    data class Newer(val tag: String, val url: String) : UpdateCheck
    data object UpToDate : UpdateCheck
    data class Problem(val message: String) : UpdateCheck
}

/**
 * The app's own GitHub releases are its update feed: one unauthenticated REST
 * call, the same public API the GitHub board uses. The last answer's validator
 * and release are kept, so a check against an unchanged feed costs a 304 and
 * nothing gets rewritten.
 */
object Updates {

    private const val ENDPOINT =
        "https://api.github.com/repos/anaymaurya04/jiit-kiosk-recon/releases/latest"

    fun check(context: Context, current: String): UpdateCheck {
        val prefs = context.getSharedPreferences("kiosk_updates", Context.MODE_PRIVATE)
        return try {
            val answer = HttpJson.getIfNoneMatch(ENDPOINT, prefs.getString("etag", null))
            if (answer.body == null) {
                // the feed did not move: judge the release seen last time
                judge(current, prefs.getString("tag", null).orEmpty(), prefs.getString("url", null).orEmpty())
            } else {
                val release = release(answer.body)
                if (release.tag.isBlank()) return UpdateCheck.Problem("The releases feed carried no tag.")
                prefs.edit()
                    .putString("etag", answer.etag)
                    .putString("tag", release.tag)
                    .putString("url", release.url)
                    .apply()
                judge(current, release.tag, release.url)
            }
        } catch (e: HttpError) {
            if (e.code == 404) UpdateCheck.Problem("No releases published yet.")
            else UpdateCheck.Problem("GitHub answered HTTP ${e.code}.")
        } catch (e: IOException) {
            UpdateCheck.Problem("No connection to GitHub.")
        } catch (e: Exception) {
            UpdateCheck.Problem(e.message?.takeIf { it.isNotBlank() } ?: "Something went wrong.")
        }
    }

    private fun judge(current: String, tag: String, url: String): UpdateCheck =
        if (tag.isNotBlank() && newer(current, tag)) UpdateCheck.Newer(tag, url) else UpdateCheck.UpToDate

    /** `v0.10.0` is newer than `0.9.0`; a name that is not dotted digits never is */
    fun newer(current: String, tag: String): Boolean {
        val a = numbers(current) ?: return false
        val b = numbers(tag) ?: return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return y > x
        }
        return false
    }

    private fun numbers(v: String): List<Int>? {
        val clean = v.trim().removePrefix("v").removePrefix("V").substringBefore('-')
        if (clean.isEmpty()) return null
        val out = ArrayList<Int>()
        for (part in clean.split('.')) {
            val digits = part.takeWhile { it.isDigit() }
            if (digits.isEmpty()) return null
            out += digits.toIntOrNull() ?: return null
        }
        return out
    }

    /** the feed's own words: a tag to compare and the page to open */
    internal data class Release(val tag: String, val url: String)

    internal fun release(json: String): Release {
        val body = Json.parseToJsonElement(json) as? JsonObject ?: return Release("", "")
        val tag = body["tag_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val url = body["html_url"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        return Release(tag, url)
    }
}
