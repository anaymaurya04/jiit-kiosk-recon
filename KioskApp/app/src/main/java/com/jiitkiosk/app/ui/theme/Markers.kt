package com.jiitkiosk.app.ui.theme

import com.jiitkiosk.app.schedule.ClassEntry
import `in`.codelif.ktjiit.model.SubjectAttendance

/**
 * The subject's attendance verdict, when the sheet knows the subject and a
 * percentage. Lifted out of the day view (R2) so the widgets wear the exact
 * same marker the calendar does.
 */
fun markFor(entry: ClassEntry, subjects: List<SubjectAttendance>, target: Int, band: Int): Verdict? {
    val byId = entry.subjectId?.let { id -> subjects.firstOrNull { it.subjectId == id } }
    val match = byId ?: subjects.firstOrNull { it.name.equals(entry.subject, ignoreCase = true) }
    val percent = match?.combinedPercent ?: return null
    return verdictOf(percent, target, band)
}
