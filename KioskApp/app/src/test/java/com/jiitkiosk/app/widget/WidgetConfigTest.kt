package com.jiitkiosk.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The config screen shows no toggle its widget never reads — a switch that does
 * nothing is a lie. Board 33's own screen (Show rows, Classes shown, Appearance)
 * belongs to the Today tile; the other kinds show the sections they consume.
 */
class WidgetConfigTest {

    @Test
    fun today_shows_the_whole_board_33_screens_show_rows() {
        val s = configSections(WidgetKind.Today)
        assertTrue(ConfigSection.NEXT in s)
        assertTrue(ConfigSection.ATTENDANCE in s)
        assertTrue(ConfigSection.PENDING in s)
    }

    @Test
    fun the_wide_schedule_tiles_tune_the_classes_shown_chips() {
        assertEquals(listOf(ConfigSection.CLASSES, ConfigSection.APPEARANCE), configSections(WidgetKind.Classes))
        assertEquals(listOf(ConfigSection.CLASSES, ConfigSection.APPEARANCE), configSections(WidgetKind.Dashboard))
    }

    @Test
    fun the_three_2x2_tiles_get_appearance_only_so_no_toggle_is_a_lie() {
        for (kind in listOf(WidgetKind.Next, WidgetKind.Attendance, WidgetKind.Pending)) {
            assertEquals(listOf(ConfigSection.APPEARANCE), configSections(kind))
            assertFalse(ConfigSection.CLASSES in configSections(kind))
        }
    }
}
