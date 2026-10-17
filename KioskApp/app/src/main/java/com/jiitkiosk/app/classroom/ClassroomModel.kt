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
    /** Classroom's own page for the item, when it offered one */
    val link: String? = null,
)

/** the file kind a material is badged with on the Notes board */
@Serializable
enum class MaterialKind { Pdf, Slides, Docs, Link }

/**
 * One published material post's first attachment. A post with nothing attached
 * has no row on the Notes board, so it is not carried at all.
 */
@Serializable
data class Material(
    val id: String,
    val courseId: String,
    val courseName: String,
    val title: String,
    /** the unit/topic name Google groups it under, when it does */
    val topic: String? = null,
    val kind: MaterialKind = MaterialKind.Link,
    /** where a tap opens it; null only when Classroom offered no link at all */
    val link: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class ClassroomData(
    val courses: List<Course>,
    val work: List<Work>,
    /** empty for data cached before materials existed */
    val materials: List<Material> = emptyList(),
) {
    val unsubmitted: Int get() = work.count { it.submitted == false }

    companion object {
        /** the one cache key the tab, the worker and the reminder sync all share */
        const val CACHE_KEY = "classroom.work"
    }
}
