package com.jiitkiosk.app.classroom

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClassroomStoreTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs get() = ctx.getSharedPreferences("kiosk", Context.MODE_PRIVATE)

    @Before
    fun clean() = ClassroomStore(ctx).clear()

    @After
    fun tidy() = ClassroomStore(ctx).clear()

    @Test
    fun survivesANewStoreInstance() {
        ClassroomStore(ctx).save("ya29.token", 123456789L)
        val again = ClassroomStore(ctx)
        assertEquals("ya29.token", again.token())
        assertEquals(123456789L, again.expiresAt())
    }

    @Test
    fun theFileHoldsNoReadableToken() {
        ClassroomStore(ctx).save("CLASSROOM-SECRET", System.currentTimeMillis())
        val sealed = prefs.getString("classroom_sealed", null)
        assertNotNull(sealed)
        assertFalse(sealed!!.contains("CLASSROOM-SECRET"))
    }

    @Test
    fun clearedMeansDisconnected() {
        ClassroomStore(ctx).save("t", 1L)
        ClassroomStore(ctx).clear()
        assertNull(ClassroomStore(ctx).token())
    }

    @Test
    fun unreadableCiphertextMeansDisconnected() {
        prefs.edit().putString("classroom_sealed", "!!not base64!!").commit()
        assertNull(ClassroomStore(ctx).token())
        assertEquals(0L, ClassroomStore(ctx).expiresAt())
    }
}
