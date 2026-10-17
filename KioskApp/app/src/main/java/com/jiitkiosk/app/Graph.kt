package com.jiitkiosk.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.jiitkiosk.app.classroom.ClassroomStore
import com.jiitkiosk.app.data.Cache
import com.jiitkiosk.app.reminders.Notifications
import com.jiitkiosk.app.sync.ClassroomWorker
import `in`.codelif.ktjiit.api.Portal
import `in`.codelif.ktjiit.auth.Auth
import `in`.codelif.ktjiit.auth.Session
import `in`.codelif.ktjiit.http.Transport

class KioskApplication : Application() {

    lateinit var graph: Graph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = Graph(this)
        Notifications.ensureChannel(this)
        ClassroomWorker.enqueue(this)
    }
}

/**
 * One transport for the whole process: it carries the portal clock skew, which
 * is learned from responses and has to be shared by every call.
 */
class Graph(context: Context) {

    private val store = SessionStore(context)

    val transport = Transport()
    val auth = Auth(transport)
    val cache = Cache(context.filesDir)
    val classroom = ClassroomStore(context)

    private val prefs = context.getSharedPreferences("kiosk", Context.MODE_PRIVATE)

    /** pure-black mode: the only appearance setting, and it is remembered */
    var pureBlack: Boolean
        get() = prefs.getBoolean("pure_black", false)
        set(value) = prefs.edit().putBoolean("pure_black", value).apply()

    /**
     * The server session's health, remembered across process deaths: when true
     * the shell keeps serving cached data under the reconnect banner.
     */
    var sessionExpired: Boolean
        get() = prefs.getBoolean("session_expired", false)
        set(value) {
            prefs.edit().putBoolean("session_expired", value).apply()
        }

    /**
     * One silent refresh for whoever is awake: the resume path and the 15-minute
     * worker. Only an explicit rejection flips [sessionExpired]; network trouble
     * throws and changes nothing.
     */
    suspend fun refreshPortal(): Boolean {
        val s = session() ?: return false
        val ok = auth.refresh(s)
        sessionExpired = !ok
        Log.i(TAG, "portal refresh ok=$ok")
        return ok
    }

    fun session(): Session? = store.load()

    fun portal(session: Session) = Portal(session, transport)

    fun remember(session: Session) {
        store.save(session)
        sessionExpired = false
    }

    /**
     * Ends the session. Cached portal data stays unless [clearCache] — it is the
     * user's own copy on their own phone, and signing back in does not need it gone.
     * Clearing is the handed-the-phone-away path, so the Classroom authorization
     * goes with it.
     */
    fun forget(clearCache: Boolean = false) {
        store.clear()
        sessionExpired = false
        if (clearCache) {
            cache.clear()
            classroom.clear()
        }
    }

    private companion object {
        const val TAG = "kiosk-auth"
    }
}
