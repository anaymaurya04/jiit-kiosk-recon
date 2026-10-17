package com.jiitkiosk.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class VerdictTest {

    @Test
    fun `the mockup samples sit in the default bands`() {
        assertEquals(Verdict.Safe, verdictOf(86.0, 75, 5))
        assertEquals(Verdict.Close, verdictOf(74.0, 75, 5))
        assertEquals(Verdict.Low, verdictOf(61.0, 75, 5))
    }

    @Test
    fun `the boundaries belong to the higher band`() {
        assertEquals(Verdict.Safe, verdictOf(75.0, 75, 5))
        assertEquals(Verdict.Close, verdictOf(74.99, 75, 5))
        assertEquals(Verdict.Close, verdictOf(70.0, 75, 5))
        assertEquals(Verdict.Low, verdictOf(69.99, 75, 5))
    }

    @Test
    fun `a zero band leaves no close space`() {
        assertEquals(Verdict.Safe, verdictOf(75.0, 75, 0))
        assertEquals(Verdict.Low, verdictOf(74.99, 75, 0))
    }
}
