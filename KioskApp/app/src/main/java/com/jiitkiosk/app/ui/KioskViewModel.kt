package com.jiitkiosk.app.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.jiitkiosk.app.KioskApplication
import com.jiitkiosk.app.data.HttpError
import com.jiitkiosk.app.data.Snapshot
import com.jiitkiosk.app.domain.SessionState
import `in`.codelif.ktjiit.api.Portal
import `in`.codelif.ktjiit.auth.PortalConfig
import `in`.codelif.ktjiit.auth.Session
import `in`.codelif.ktjiit.http.PortalException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement

class KioskViewModel(app: Application) : AndroidViewModel(app) {

    private val graph = (app as KioskApplication).graph

    var session by mutableStateOf(graph.session())
        private set

    /** why the portal ended the session; shown once on the sign-in screen */
    var signedOut by mutableStateOf<String?>(null)
        private set

    private val health = SessionState(graph.sessionExpired)

    /** true when the portal rejected the token and the refresh failed too */
    var expired by mutableStateOf(health.expired)
        private set

    /** the banner was tapped: the sign-in screen takes over, cached data stays */
    var reconnecting by mutableStateOf(false)
        private set

    fun remember(session: Session) {
        graph.remember(session)
        this.session = session
        signedOut = null
        reconnecting = false
        clearExpiry()
    }

    /** the user's own choice: no explanation needed */
    fun signOut(clearCache: Boolean = false) {
        graph.forget(clearCache)
        session = null
        signedOut = null
        reconnecting = false
        clearExpiry()
    }

    /**
     * The portal said no and the refresh failed with it: the local session stays,
     * every cached screen stays readable, and the shell shows the reconnect
     * banner. Logging out is still the only thing that clears anything.
     */
    private fun softExpire() {
        health.rejected()
        expired = true
        graph.sessionExpired = true
    }

    private fun clearExpiry() {
        health.revived()
        expired = false
        graph.sessionExpired = false
    }

    fun startReconnect() {
        signedOut = "Session expired, sign in again."
        reconnecting = true
    }

    fun cancelReconnect() {
        reconnecting = false
    }

    private var revivedAt = 0L

    /**
     * The proactive half of §4: called on resume (throttled — a resume storm
     * must not become a request storm) and by the 15-minute worker. Silent on
     * network trouble; an explicit rejection flips the banner on.
     */
    suspend fun revive() {
        if (session == null) return
        val now = System.currentTimeMillis()
        if (now - revivedAt < REVIVE_EVERY_MS) return
        revivedAt = now
        runCatching { graph.refreshPortal() }.onSuccess { ok ->
            if (ok) clearExpiry() else softExpire()
        }
    }

    /** pure-black mode, remembered between launches */
    var pureBlack by mutableStateOf(graph.pureBlack)
        private set

    fun applyPureBlack(value: Boolean) {
        graph.pureBlack = value
        pureBlack = value
    }

    /** the attendance line, remembered between launches; see [Graph.attendTarget] */
    var attendTarget by mutableStateOf(graph.attendTarget)
        private set
    var attendBand by mutableStateOf(graph.attendBand)
        private set

    fun applyAttendTarget(value: Int) {
        graph.attendTarget = value
        attendTarget = graph.attendTarget
    }

    fun applyAttendBand(value: Int) {
        graph.attendBand = value
        attendBand = graph.attendBand
    }

    // ---- sign in ----

    suspend fun config(): PortalConfig = graph.auth.config()

    suspend fun exchange(credential: String, config: PortalConfig): Session =
        graph.auth.exchangeGoogleToken(credential, config)

    fun sessionFrom(response: JsonElement): Session = graph.auth.sessionFrom(response)

    // ---- signed in ----

    /**
     * Runs a portal call, and when the token has aged out asks the portal to
     * extend it and tries once more. Only an explicit rejection that the refresh
     * could not fix soft-expires the session; everything else keeps it.
     */
    suspend fun <T> call(block: suspend (Portal) -> T): T {
        val current = session ?: throw PortalException.SessionExpired()
        return try {
            block(graph.portal(current))
        } catch (e: PortalException.SessionExpired) {
            if (!graph.auth.refresh(current)) {
                softExpire()
                throw e
            }
            // refresh extends the same token, so the session object is unchanged
            block(graph.portal(current))
        }
    }

    /**
     * The cached copy, read synchronously so a screen can paint its first frame
     * with the last known data instead of a spinner.
     */
    fun <T> lastGood(key: String, ser: KSerializer<T>): Snapshot<T>? = graph.cache.read(key, ser)

    /**
     * [call], but the answer is kept on disk. If the portal cannot be reached
     * the cached copy comes back marked stale rather than the screen going
     * blank, which is the whole point of this app on a bad portal day.
     */
    suspend fun <T> cached(key: String, ser: KSerializer<T>, block: suspend (Portal) -> T): Snapshot<T> {
        return try {
            val fresh = call(block)
            graph.cache.write(key, ser, fresh)
            Snapshot(fresh, System.currentTimeMillis(), false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: PortalException.SessionExpired) {
            // soft-expired keeps the local session: cached data keeps the screen alive
            graph.cache.read(key, ser) ?: throw e
        } catch (e: Throwable) {
            graph.cache.read(key, ser) ?: throw e
        }
    }

    // ---- Classroom (and later the Code tab's sources) ----

    val classroom get() = graph.classroom

    /** local, non-portal data (the class schedule) lives here and outlives sign-out */
    val dataDir get() = graph.dataDir

    /**
     * [cached] for calls that never touch the portal. Same disk behavior, with
     * one difference: an auth failure is rethrown instead of being papered over
     * by the cached copy, because a stale list behind an expired token reads as
     * "everything is fine" right up until it isn't.
     */
    suspend fun <T> keep(key: String, ser: KSerializer<T>, block: suspend () -> T): Snapshot<T> {
        return try {
            val fresh = block()
            graph.cache.write(key, ser, fresh)
            Snapshot(fresh, System.currentTimeMillis(), false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpError) {
            Log.w(TAG, "keep($key) failed with HTTP ${e.code}")
            if (e.code == 401 || e.code == 403) throw e
            graph.cache.read(key, ser) ?: throw e
        } catch (e: Throwable) {
            Log.w(TAG, "keep($key) failed, falling back to cache", e)
            graph.cache.read(key, ser) ?: throw e
        }
    }

    private companion object {
        const val TAG = "kiosk"
        const val REVIVE_EVERY_MS = 10 * 60_000L
    }
}
