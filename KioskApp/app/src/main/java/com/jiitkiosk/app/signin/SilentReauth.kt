package com.jiitkiosk.app.signin

import android.util.Log
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jiitkiosk.app.ui.KioskViewModel
import `in`.codelif.ktjiit.auth.PortalConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The portal's refresh only covers its own window; when it says no, the Google
 * account already on this phone can still mint a fresh session without a tap.
 * This runs the sign-in page in its silent mode behind the shell — once per
 * expiry and once more on each resume after a failed try — and leaves the
 * reconnect banner alone if Google needs a human.
 */
@Composable
fun SilentReauth(vm: KioskViewModel, resumeTick: Int) {
    var config by remember { mutableStateOf<PortalConfig?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(vm.expired, resumeTick) {
        if (!vm.expired) {
            config = null
            return@LaunchedEffect
        }
        // one attempt at a time; a resume mid-attempt must not restart the page
        if (config != null || vm.session == null) return@LaunchedEffect
        val cfg = runCatching { vm.config() }.getOrNull()
        if (cfg?.googleEnabled == true) config = cfg else Log.i(TAG, "silent reauth: no google client")
    }

    val cfg = config ?: return
    LaunchedEffect(cfg) {
        delay(30_000)
        if (config === cfg) {
            Log.i(TAG, "silent reauth: timeout")
            config = null
        }
    }

    Box(Modifier.size(420.dp)) {
        AndroidView(
            factory = { ctx ->
                PortalWeb.create(
                    ctx,
                    onSignal = { signal ->
                        when (signal) {
                            is WebSignal.Credential -> {
                                config = null
                                scope.launch {
                                    runCatching { vm.exchange(signal.token, cfg) }
                                        .onSuccess {
                                            PortalWeb.persistCookies()
                                            vm.remember(it)
                                            Log.i(TAG, "silent reauth ok")
                                        }
                                        .onFailure {
                                            Log.i(TAG, "silent reauth exchange failed: ${it.javaClass.simpleName}")
                                        }
                                }
                            }

                            is WebSignal.NoAuto -> {
                                Log.i(TAG, "silent reauth: ${signal.why}")
                                config = null
                            }

                            is WebSignal.Failed -> {
                                Log.i(TAG, "silent reauth failed: ${signal.why}")
                                config = null
                            }

                            else -> {}
                        }
                    },
                    onPopup = { popup: WebView? -> popup?.destroy() },
                ).apply {
                    loadUrl(PortalWeb.signinUrl(cfg.googleClientId, silent = true, dark = false, widthPx = 400))
                }
            },
            onRelease = { it.destroy() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private const val TAG = "kiosk-auth"
