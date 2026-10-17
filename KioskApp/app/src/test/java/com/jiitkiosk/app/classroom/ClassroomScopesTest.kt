package com.jiitkiosk.app.classroom

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The write-time guard against the bug that silenced the Class tab: Google
 * answers 403 for a list whose own read-only scope was not requested, and that
 * 403 fails the whole fetch — so dropping one scope from the consent request
 * must fail here, in a test, not on a phone.
 */
class ClassroomScopesTest {

    @Test
    fun `every list the client reads has its scope`() {
        val scopes = ClassroomAuth.SCOPES
        // one per list in ClassroomClient.fetch(): courses, coursework,
        // submissions, materials, topics
        assertTrue(scopes.contains("https://www.googleapis.com/auth/classroom.courses.readonly"))
        assertTrue(scopes.contains("https://www.googleapis.com/auth/classroom.coursework.me.readonly"))
        assertTrue(scopes.contains("https://www.googleapis.com/auth/classroom.student-submissions.me.readonly"))
        assertTrue(scopes.contains("https://www.googleapis.com/auth/classroom.courseworkmaterials.readonly"))
        assertTrue(scopes.contains("https://www.googleapis.com/auth/classroom.topics.readonly"))
    }
}
