package com.jiitkiosk.app.secure

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jiitkiosk.app.SessionStore
import `in`.codelif.ktjiit.auth.Session
import `in`.codelif.ktjiit.http.Transport
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionStoreTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs get() = ctx.getSharedPreferences("kiosk", Context.MODE_PRIVATE)

    @Before
    fun clean() = SessionStore(ctx).clear()

    @After
    fun tidy() = SessionStore(ctx).clear()

    @Test
    fun survivesANewStoreInstance() {
        val session = Session(token = "T1", username = "u", instituteId = "9")
        SessionStore(ctx).save(session)
        assertEquals(session, SessionStore(ctx).load())
    }

    @Test
    fun theFileHoldsNoReadableToken() {
        SessionStore(ctx).save(Session(token = "SECRET-TOKEN", username = "u", instituteId = "9"))
        assertNull(prefs.getString("session", null))
        val sealed = prefs.getString("session_sealed", null)
        assertNotNull(sealed)
        assertFalse(sealed!!.contains("SECRET-TOKEN"))
    }

    @Test
    fun aLegacyPlainSessionIsMigratedOnce() {
        val session = Session(token = "T2", username = "legacy", instituteId = "9")
        prefs.edit().putString("session", Transport.json.encodeToString(Session.serializer(), session)).commit()
        assertEquals(session, SessionStore(ctx).load())
        assertNull("plaintext must be gone", prefs.getString("session", null))
        assertNotNull(prefs.getString("session_sealed", null))
        assertEquals(session, SessionStore(ctx).load())
    }

    @Test
    fun unreadableCiphertextMeansSignedOut() {
        prefs.edit().putString("session_sealed", "!!not base64!!").commit()
        assertNull(SessionStore(ctx).load())
        prefs.edit().putString("session_sealed", "QUJD").commit()
        assertNull(SessionStore(ctx).load())
    }
}
