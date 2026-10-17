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

    /** the phone's own copy: the class schedule and other local data live here, outside the portal cache */
    val dataDir = context.filesDir

    private val prefs = context.getSharedPreferences("kiosk", Context.MODE_PRIVATE)

    /** pure-black mode: the only appearance setting, and it is remembered */
    var pureBlack: Boolean
        get() = prefs.getBoolean("pure_black", false)
        set(value) = prefs.edit().putBoolean("pure_black", value).apply()

    /**
     * The attendance line, the user's own: the Attendance tab flags subjects by
     * it and the Class schedule marks its classes by it. The defaults are the
     * mockups' own samples — 75 with a 5-wide close band.
     */
    var attendTarget: Int
        get() = prefs.getInt("attend_target", 75).coerceIn(50, 100)
        set(value) = prefs.edit().putInt("attend_target", value.coerceIn(50, 100)).apply()

    var attendBand: Int
        get() = prefs.getInt("attend_band", 5).coerceIn(0, 25)
        set(value) = prefs.edit().putInt("attend_band", value.coerceIn(0, 25)).apply()

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
     *
     * A token that is still inside its own life does not need extending — and the
     * portal refuses to extend it anyway ({"msg":"Error"}), which used to read as
     * "session rejected" and light the banner on every app open while every data
     * call worked. A provably valid token is a healthy session; say so and skip.
     */
    suspend fun refreshPortal(): Boolean {
        val s = session() ?: return false
        if (s.validAt(transport.clock.now())) {
            sessionExpired = false
            Log.i(TAG, "portal refresh skipped, token valid until ${s.expiresAt}")
            return true
        }
        val ok = try {
            auth.refresh(s)
        } catch (e: Throwable) {
            Log.i(TAG, "refresh threw ${e.javaClass.simpleName}: ${e.message?.take(80)}")
            throw e
        }
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
