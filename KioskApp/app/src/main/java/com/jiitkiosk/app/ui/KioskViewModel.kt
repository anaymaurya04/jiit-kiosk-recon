package com.jiitkiosk.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.jiitkiosk.app.KioskApplication
import com.jiitkiosk.app.data.HttpError
import com.jiitkiosk.app.data.Snapshot
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

    fun remember(session: Session) {
        graph.remember(session)
        this.session = session
        signedOut = null
    }

    /** the user's own choice: no explanation needed */
    fun signOut(clearCache: Boolean = false) {
        graph.forget(clearCache)
        session = null
        signedOut = null
    }

    /** the portal said no: the cache stays, but the login says honestly why it showed up */
    private fun sessionEnded() {
        graph.forget(clearCache = false)
        session = null
        signedOut = "Session expired, sign in again."
    }

    /** pure-black mode, remembered between launches */
    var pureBlack by mutableStateOf(graph.pureBlack)
        private set

    fun applyPureBlack(value: Boolean) {
        graph.pureBlack = value
        pureBlack = value
    }

    // ---- sign in ----

    suspend fun config(): PortalConfig = graph.auth.config()

    suspend fun exchange(credential: String, config: PortalConfig): Session =
        graph.auth.exchangeGoogleToken(credential, config)

    fun sessionFrom(response: JsonElement): Session = graph.auth.sessionFrom(response)

    // ---- signed in ----

    /**
     * Runs a portal call, and when the token has aged out asks the portal to
     * extend it and tries once more. Only if that fails does the session end.
     */
    suspend fun <T> call(block: suspend (Portal) -> T): T {
        val current = session ?: throw PortalException.SessionExpired()
        return try {
            block(graph.portal(current))
        } catch (e: PortalException.SessionExpired) {
            if (!graph.auth.refresh(current)) {
                sessionEnded()
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
            throw e
        } catch (e: Throwable) {
            graph.cache.read(key, ser) ?: throw e
        }
    }

    // ---- Classroom (and later the Code tab's sources) ----

    val classroom get() = graph.classroom

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
            if (e.code == 401 || e.code == 403) throw e
            graph.cache.read(key, ser) ?: throw e
        } catch (e: Throwable) {
            graph.cache.read(key, ser) ?: throw e
        }
    }
}
