package com.jiitkiosk.app.ui.classroom

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.common.api.ApiException
import com.jiitkiosk.app.classroom.ClassroomAuth
import com.jiitkiosk.app.classroom.findActivity
import com.jiitkiosk.app.data.HttpError
import com.jiitkiosk.app.ui.KioskViewModel
import java.io.IOException

/** what a screen needs to show and drive the Classroom connection */
@Stable
class ClassroomConnect(
    val connected: Boolean,
    val connecting: Boolean,
    val error: String?,
    /** bumps on every successful (re)connect, so callers can refetch */
    val version: Int,
    val connect: () -> Unit,
)

/**
 * The one place the Google permission dance lives, so the Class tab and the
 * Settings row behave identically: silent when Google already remembers the
 * account, one account-picker detour when it wants the account confirmed.
 */
@Composable
fun rememberClassroomConnect(vm: KioskViewModel): ClassroomConnect {
    val context = LocalContext.current
    var connected by remember { mutableStateOf(vm.classroom.token() != null) }
    var connecting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableIntStateOf(0) }
    var asked by remember { mutableStateOf<AuthorizationClient?>(null) }

    fun accept(token: String?) {
        connecting = false
        if (token.isNullOrBlank()) {
            error = "Google returned no token."
            return
        }
        vm.classroom.save(token, System.currentTimeMillis() + 55 * 60_000L)
        connected = true
        version++
    }

    val resolve = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val client = asked
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && client != null && data != null) {
            runCatching { client.getAuthorizationResultFromIntent(data) }
                .onSuccess { accept(it.accessToken) }
                .onFailure {
                    connecting = false
                    error = classroomFriendly(it)
                }
        } else {
            connecting = false
            error = "Google consent was not granted, so Classroom stays disconnected."
        }
    }

    fun connectNow() {
        val activity = context.findActivity()
        if (activity == null) {
            error = "No app window to ask in. Try again."
            return
        }
        val client = ClassroomAuth.client(activity)
        asked = client
        connecting = true
        error = null
        client.authorize(ClassroomAuth.request())
            .addOnSuccessListener { result ->
                val pending = result.pendingIntent
                if (result.hasResolution() && pending != null) {
                    // Google wants the account picked or the consent re-approved first
                    runCatching {
                        resolve.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                    }.onFailure {
                        connecting = false
                        error = classroomFriendly(it)
                    }
                } else {
                    accept(result.accessToken)
                }
            }
            .addOnFailureListener { e ->
                connecting = false
                error = classroomFriendly(e)
            }
    }

    return ClassroomConnect(connected, connecting, error, version) { connectNow() }
}

/** the failures Google and Classroom actually produce, in sentences */
fun classroomFriendly(e: Throwable): String = when {
    e is HttpError && e.code == 401 -> "Classroom access has expired. Connect again."
    e is HttpError && e.code == 403 ->
        "Google says this account has not granted all three read-only scopes. Connect again to re-approve."
    e is HttpError && e.code == 429 -> "Classroom is rate-limiting. Try again in a minute."
    e is HttpError -> "Classroom answered HTTP ${e.code}."
    e is ApiException ->
        e.message?.takeIf { it.isNotBlank() }?.let { "Google refused the connection: $it" }
            ?: "Google refused the connection (code ${e.statusCode})."
    e is IOException -> "No connection to Classroom."
    else -> e.message?.takeIf { it.isNotBlank() } ?: "Something went wrong."
}
