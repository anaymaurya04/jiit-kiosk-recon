package com.jiitkiosk.app.secure

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jiitkiosk.app.SessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs in a fresh process after phase one + `am force-stop`: the session must
 * be there, sealed, with no plaintext left behind.
 */
@RunWith(AndroidJUnit4::class)
class RestorePhaseTwoTest {

    @Test
    fun aFreshProcessFindsTheSealedSession() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("PROCESSTEST", SessionStore(ctx).load()?.token)
        val prefs = ctx.getSharedPreferences("kiosk", Context.MODE_PRIVATE)
        assertNull(prefs.getString("session", null))
        val sealed = prefs.getString("session_sealed", null)
        assertNotNull(sealed)
        assertFalse(sealed!!.contains("PROCESSTEST"))
    }
}
