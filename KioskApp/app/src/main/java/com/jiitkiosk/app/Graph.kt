package com.jiitkiosk.app

import android.app.Application
import android.content.Context
import com.jiitkiosk.app.data.Cache
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

    private val prefs = context.getSharedPreferences("kiosk", Context.MODE_PRIVATE)

    /** pure-black mode: the only appearance setting, and it is remembered */
    var pureBlack: Boolean
        get() = prefs.getBoolean("pure_black", false)
        set(value) = prefs.edit().putBoolean("pure_black", value).apply()

    fun session(): Session? = store.load()

    fun portal(session: Session) = Portal(session, transport)

    fun remember(session: Session) = store.save(session)

    fun forget() {
        store.clear()
        cache.clear()
    }
}
