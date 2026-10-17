package com.jiitkiosk.app.schedule

import java.time.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleTest {

    @Test
    fun `times parse only as HH mm`() {
        assertEquals(600, minutesOf("10:00"))
        assertEquals(1439, minutesOf("23:59"))
        assertNull(minutesOf("24:00"))
        assertNull(minutesOf("10:60"))
        assertNull(minutesOf("1000"))
        assertNull(minutesOf("ab:cd"))
        assertNull(minutesOf("10:"))
    }

    @Test
    fun `a class only has a length when it ends after it starts`() {
        assertEquals(60, ClassEntry("a", 3, "10:00", "11:00", "SE").minutes())
        assertNull(ClassEntry("a", 3, "11:00", "10:00", "SE").minutes())
        assertNull(ClassEntry("a", 3, "10:00", "10:00", "SE").minutes())
        assertNull(ClassEntry("a", 3, "nope", "11:00", "SE").minutes())
    }

    @Test
    fun `days bucket and hours add up`() {
        val s = Schedule(
            entries = listOf(
                ClassEntry("a", 3, "14:00", "15:00", "FDA"),
                ClassEntry("b", 3, "10:00", "11:00", "SE"),
                ClassEntry("c", 1, "09:00", "10:00", "IP"),
            ),
        )
        assertEquals(listOf("SE", "FDA"), s.entriesOn(3).map { it.subject })
        assertEquals(120, s.minutesOn(3))
        assertEquals(180, s.minutesWeek())
        assertEquals(0, s.minutesOn(6))
    }

    @Test
    fun `sunday has no number on the builder's week`() {
        assertNull(dayNumber(LocalDate.of(2026, 10, 18)))
        assertEquals(6, dayNumber(LocalDate.of(2026, 10, 17)))
    }

    @Test
    fun `the whole schedule survives a round trip through json`() {
        val s = Schedule(
            savedAt = 42,
            entries = listOf(ClassEntry("a", 2, "09:00", "09:50", "IP", "sid-7", "C-201", ClassType.Lab)),
        )
        val back = Json.decodeFromString(Schedule.serializer(), Json.encodeToString(Schedule.serializer(), s))
        assertEquals(s, back)
    }
}
