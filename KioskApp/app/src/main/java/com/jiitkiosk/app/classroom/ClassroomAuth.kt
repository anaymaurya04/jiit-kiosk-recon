package com.jiitkiosk.app.classroom

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope

/**
 * Google Identity, the authorization flavor: Classroom access is an OAuth
 * access token for three read-only scopes, not an ID token, so there is no
 * sign-in screen unless Google decides the user must pick or re-approve an
 * account. The same call silently re-authorizes later, which is what makes an
 * expired hourly token a non-event.
 *
 * When Google wants a human, authorize() *succeeds* with a result that carries
 * a PendingIntent; the caller launches it and swaps the returned Intent back
 * through [AuthorizationClient.getAuthorizationResultFromIntent].
 */
object ClassroomAuth {

    /**
     * One read-only scope per list the Class tab reads, nothing else, ever.
     * Google answers 403 for a list whose own scope is missing — and that 403
     * fails the whole sync, not just the rows that list feeds — so materials
     * and topics each need theirs alongside courses, coursework and submissions.
     */
    val SCOPES = listOf(
        "https://www.googleapis.com/auth/classroom.courses.readonly",
        "https://www.googleapis.com/auth/classroom.coursework.me.readonly",
        "https://www.googleapis.com/auth/classroom.student-submissions.me.readonly",
        "https://www.googleapis.com/auth/classroom.courseworkmaterials.readonly",
        "https://www.googleapis.com/auth/classroom.topics.readonly",
    )

    fun client(activity: Activity): AuthorizationClient = Identity.getAuthorizationClient(activity)

    fun request(): AuthorizationRequest =
        AuthorizationRequest.builder().setRequestedScopes(SCOPES.map { Scope(it) }).build()
}

/** the hosting activity, for the prompts that need one */
fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
