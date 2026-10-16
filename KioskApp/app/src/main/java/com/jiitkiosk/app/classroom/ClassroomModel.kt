package com.jiitkiosk.app.classroom

import kotlinx.serialization.Serializable

/** an active course the student is enrolled in */
@Serializable
data class Course(val id: String, val name: String)

/**
 * One piece of published coursework. [submitted] is null when the submission
 * state could not be read — the screens say "unknown" rather than guessing,
 * and reminders never fire off a guess.
 */
@Serializable
data class Work(
    val id: String,
    val courseId: String,
    val courseName: String,
    val title: String,
    /** epoch millis, UTC; null = no deadline */
    val dueAt: Long? = null,
    /** false when Classroom only knows the day, so the UI never prints a fake time */
    val dueHasTime: Boolean = false,
    val submitted: Boolean? = null,
    val submittedAt: Long? = null,
)

@Serializable
data class ClassroomData(val courses: List<Course>, val work: List<Work>) {
    val unsubmitted: Int get() = work.count { it.submitted == false }
}
