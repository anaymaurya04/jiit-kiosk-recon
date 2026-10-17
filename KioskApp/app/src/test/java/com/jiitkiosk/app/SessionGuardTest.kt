package com.jiitkiosk.app

import `in`.codelif.ktjiit.auth.Session
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Base64

/** the guard that keeps a healthy session out of the soft-expired banner */
class SessionGuardTest {

    private fun session(exp: Long?): Session {
        val payload = exp?.let { """{"exp":$it}""" } ?: "{}"
        val jwt = "h." + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.toByteArray()) + ".s"
        return Session(token = jwt, username = "u", instituteId = "1")
    }

    @Test
    fun `a token inside its life is healthy`() {
        val now = Instant.ofEpochSecond(1_000_000)
        assertTrue(session(1_000_001).validAt(now))
        assertFalse(session(999_999).validAt(now))
        assertFalse(session(null).validAt(now))
    }
}
