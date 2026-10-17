package com.jiitkiosk.app.domain

/**
 * The server session's health, separate from the local session's existence. The
 * user stays signed in here after the portal stops accepting the token — cached
 * screens keep reading — and only an explicit rejection expires it; network
 * trouble never does (callers simply do not report it here).
 */
class SessionState(expired: Boolean = false) {

    var expired: Boolean = expired
        private set

    /** the portal rejected the token and the refresh failed with it */
    fun rejected() {
        expired = true
    }

    /** a refresh or a fresh sign-in the portal accepted */
    fun revived() {
        expired = false
    }
}
