package com.jiitkiosk.app.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

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
        runCatching { file(key).writeText(json.encodeToString(ser, value)) }
    }

    /** used by sign-out: another student's results must not survive it */
    fun clear() {
        runCatching { root.deleteRecursively() }
    }

    // the key is assembled from portal ids, so keep the filename boring
    private fun file(key: String) = File(root, key.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".json")
}
