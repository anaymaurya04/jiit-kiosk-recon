package com.jiitkiosk.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The soft-expired rule: only an explicit rejection expires; a revive clears. */
class SessionStateTest {

    @Test
    fun startsActive() {
        assertFalse(SessionState().expired)
    }

    @Test
    fun rejectionSoftExpires() {
        val s = SessionState()
        s.rejected()
        assertTrue(s.expired)
    }

    @Test
    fun reviveClears() {
        val s = SessionState(expired = true)
        s.revived()
        assertFalse(s.expired)
    }

    @Test
    fun aBootRemembersTheExpiry() {
        assertTrue(SessionState(expired = true).expired)
    }
}
