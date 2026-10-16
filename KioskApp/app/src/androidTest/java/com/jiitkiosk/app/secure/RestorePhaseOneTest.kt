package com.jiitkiosk.app.secure

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jiitkiosk.app.SessionStore
import `in`.codelif.ktjiit.auth.Session
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Host script: run this, `adb shell am force-stop com.jiitkiosk.app`, then
 * RestorePhaseTwoTest. Both together are the process-death test.
 */
@RunWith(AndroidJUnit4::class)
class RestorePhaseOneTest {

    @Test
    fun seedASessionThroughTheRealStore() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        SessionStore(ctx).save(Session(token = "PROCESSTEST", username = "phase", instituteId = "9"))
        assertEquals("PROCESSTEST", SessionStore(ctx).load()?.token)
    }
}
