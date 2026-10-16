package com.jiitkiosk.app.classroom

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jiitkiosk.app.data.HttpError
import com.jiitkiosk.app.data.HttpJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeNoException
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * The real Classroom endpoint, with a token that isn't one: the point is that
 * the HTTP stack and the 401 the Reconnect screen keys off both work against
 * the live server. Runs online; skips itself when the device has no network.
 */
@RunWith(AndroidJUnit4::class)
class ClassroomHttpTest {

    @Test
    fun anInvalidTokenComesBackAs401() {
        val error = try {
            HttpJson.get(
                "https://classroom.googleapis.com/v1/courses?courseStates=ACTIVE&studentId=me",
                bearer = "definitely-not-a-token",
            )
            null
        } catch (e: HttpError) {
            e
        } catch (e: IOException) {
            assumeNoException("no network on this device", e)
            null
        }
        assertNotNull("Classroom should have rejected the token", error)
        assertEquals(401, error!!.code)
    }
}
