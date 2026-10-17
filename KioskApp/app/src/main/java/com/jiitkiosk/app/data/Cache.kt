package com.jiitkiosk.app.data

import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

/**
 * A value that came off the portal, when it was fetched, and whether it is a
 * leftover because the portal could not be reached.
 */
data class Snapshot<T>(val value: T, val at: Long, val stale: Boolean)

/**
 * The last good answer for each call, kept as JSON under the app's own files
 * directory. The portal drops out most weeks; this is what keeps the screens
 * readable when it does.
 */
class Cache(dir: File) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val root = File(dir, "portal-cache").apply { mkdirs() }

    fun <T> read(key: String, ser: KSerializer<T>): Snapshot<T>? {
        val f = file(key)
        if (!f.exists()) return null
        return runCatching { Snapshot(json.decodeFromString(ser, f.readText()), f.lastModified(), true) }.getOrNull()
    }

    fun <T> write(key: String, ser: KSerializer<T>, value: T) {
        // a failed write must not be silent: this disk is what the widget reads,
        // and a swallowed error looked exactly like "never synced" for two releases
        runCatching { file(key).writeText(json.encodeToString(ser, value)) }
            .onFailure { Log.w("kiosk", "cache write failed: ${file(key).name}", it) }
    }

    /** used by sign-out: another student's results must not survive it */
    fun clear() {
        runCatching { root.deleteRecursively() }
    }

    // the key is assembled from portal ids, so keep the filename boring. A portal
    // token is a ~600-char JWT and the filesystem caps a name at 255 bytes: every
    // token-keyed write used to fail with ENAMETOOLONG, so the widget that reads
    // this cache found nothing. Long keys fold into a stable hash instead — read
    // and write agree on the same file, and short keys keep the names already there.
    private fun file(key: String): File {
        val name = key.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(root, if (name.length <= MAX_NAME) "$name.json" else "${sha256(key)}.json")
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        /** leaves room for ".json" under the 255-byte cap on ext4/f2fs */
        const val MAX_NAME = 240
    }
}
