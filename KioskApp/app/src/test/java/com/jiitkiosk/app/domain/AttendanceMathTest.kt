package com.jiitkiosk.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The skip/attend numbers are the reason anyone opens this app, so they get
 * hand-checked expectations rather than whatever the code happens to print.
 */
class AttendanceMathTest {

    @Test
    fun `exactly on target leaves no room to skip`() {
        val t = Tally(attended = 15, total = 20) // 75%
        assertEquals(0, t.canMiss(75))
        assertEquals(0, t.mustAttend(75))
    }

    @Test
    fun `skipping four from ninety percent lands exactly on seventy five`() {
        val t = Tally(attended = 18, total = 20) // 90%
        assertEquals(4, t.canMiss(75)) // 18 / 24 == 75%
        assertEquals(0, t.mustAttend(75))
    }

    @Test
    fun `half attendance needs twenty in a row to reach seventy five`() {
        val t = Tally(attended = 10, total = 20) // 50%
        assertEquals(20, t.mustAttend(75)) // 30 / 40 == 75%
        assertEquals(0, t.canMiss(75))
    }

    @Test
    fun `nothing counted yet is not a division by zero`() {
        val t = Tally.ZERO
        assertEquals(0.0, t.percent, 0.0)
        assertEquals(0, t.canMiss(75))
        assertEquals(0, t.mustAttend(75))
    }

    @Test
    fun `a perfect record still cannot skip once the target is one hundred`() {
        assertEquals(0, Tally(attended = 10, total = 10).mustAttend(100))
        assertEquals(Int.MAX_VALUE, Tally(attended = 9, total = 10).mustAttend(100))
    }

    @Test
    fun `extras count toward the top and never toward the bottom`() {
        val classes = listOf(
            record(present = true, extra = false),
            record(present = false, extra = false),
            record(present = true, extra = true),
        )
        val t = AttendanceMath.tally(classes)
        assertEquals(2, t.attended) // both presents
        assertEquals(2, t.total) // only the regulars
        assertEquals(100.0, t.percent, 0.001)
    }

    private fun record(present: Boolean, extra: Boolean) = `in`.codelif.ktjiit.model.ClassRecord(
        datetime = "01/01/2026 (10:00:AM - 10:50 AM)",
        present = if (present) "Present" else "Absent",
        classType = if (extra) "Extra" else "Regular",
    )
}
