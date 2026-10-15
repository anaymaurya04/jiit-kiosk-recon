package com.jiitkiosk.app.signin

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.artIn
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.Load
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.friendly
import com.jiitkiosk.app.ui.components.rememberLoad
import com.jiitkiosk.app.ui.entrance
import com.jiitkiosk.app.ui.tap
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.CrimsonDeep
import com.jiitkiosk.app.ui.theme.HelmMark
import com.jiitkiosk.app.ui.theme.Ink
import com.jiitkiosk.app.ui.theme.Mono
import com.jiitkiosk.app.ui.theme.cssGradient
import com.jiitkiosk.app.ui.theme.halftone
import `in`.codelif.ktjiit.http.Transport
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject

@Composable
fun SignInScreen(vm: KioskViewModel) {
    var attempt by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var onPortalPage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val config = rememberLoad(attempt) { vm.config() }

    Box(Modifier.fillMaxSize().clipToBounds()) {
        // the whole page is the crimson gradient, with the halftone over it
        Box(
            Modifier
                .matchParentSize()
                .cssGradient(170f, arrayOf(0f to Crimson, 0.6f to CrimsonDeep, 1f to Ink))
                .halftone(),
        )
        HelmMark(
            arch = Ink,
            accent = Crimson,
            detail = Crimson,
            size = 420.dp,
            modifier = Modifier.offset(x = (-20).dp, y = 20.dp).artIn(),
        )

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 40.dp),
        ) {
            D("J\nPortal", 88.sp, maxLines = 2, modifier = Modifier.entrance(0))
            BodyText(
                "Your JIIT portal, as an app. Unofficial.",
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp).entrance(1),
            )

            when (config) {
                is Load.Loading -> BoneBar(modifier = Modifier.entrance(2)) {
                    MonoText("REACHING THE PORTAL…", color = Ink, medium = true)
                }

                is Load.Failed -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonoText("PORTAL UNREACHABLE", color = Bone, medium = true)
                    BodyText(friendly(config.error))
                    BoneBar(modifier = Modifier.entrance(2), onClick = { error = null; attempt++ }, label = "TRY AGAIN")
                }

                is Load.Ok -> {
                    val cfg = config.value
                    if (cfg.googleEnabled) {
                        BoneBar(modifier = Modifier.entrance(2)) {
                            GoogleButton(
                                clientId = cfg.googleClientId,
                                onCredential = { credential ->
                                    error = null
                                    scope.launch {
                                        runCatching { vm.exchange(credential, cfg) }
                                            .onSuccess { PortalWeb.persistCookies(); vm.remember(it) }
                                            .onFailure { error = friendly(it) }
                                    }
                                },
                                onError = { error = it },
                            )
                        }
                    } else {
                        BoneBar(modifier = Modifier.entrance(2)) {
                            MonoText("NO GOOGLE SIGN-IN FROM THE PORTAL RIGHT NOW", color = Ink, medium = true)
                        }
                    }
                }
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                MonoText(it, color = Bone, medium = true, modifier = Modifier.entrance(3))
            }

            MonoText(
                "Session sealed on this phone. No analytics.",
                color = Bone,
                modifier = Modifier.padding(top = 12.dp).entrance(3),
            )

            // the mockup has no fallback link, but google blocking our page is a
            // real failure mode and the portal's own page always works
            Text(
                text = "HAVING TROUBLE? SIGN IN ON THE PORTAL PAGE",
                style = Mono.copy(fontWeight = FontWeight.Medium),
                color = Bone.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 16.dp).tap { onPortalPage = true },
            )
        }
    }

    if (onPortalPage) {
        PortalPageSignIn(vm = vm, onError = { error = it; onPortalPage = false }, onClose = { onPortalPage = false })
    }
}

/** the design's plain warm-white bar; the real google button sits inside it */
@Composable
private fun BoneBar(
    label: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Bone)
            .then(if (onClick != null) Modifier.tap(onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) {
            Text(label, style = Mono.copy(fontWeight = FontWeight.Medium), color = Ink)
        }
        content()
    }
}

/**
 * Google's own button, drawn by Google inside a small webview so it sits in our
 * layout like any other control. It has to be their rendering: that is the only
 * way their client id hands back a credential.
 */
@Composable
private fun GoogleButton(
    clientId: String,
    onCredential: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    var ready by remember { mutableStateOf(false) }
    var popup by remember { mutableStateOf<WebView?>(null) }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val widthPx = maxWidth.coerceAtMost(400.dp).value.toInt()
        if (!ready) {
            Text("SIGN IN WITH GOOGLE", style = Mono.copy(fontWeight = FontWeight.Medium), color = Ink)
        }
        AndroidView(
            factory = { ctx ->
                PortalWeb.create(
                    ctx,
                    onSignal = { signal ->
                        when (signal) {
                            is WebSignal.Ready -> ready = true
                            is WebSignal.Credential -> onCredential(signal.token)
                            is WebSignal.Failed -> onError(
                                if (signal.why == "webview_too_old") {
                                    "Update Android System WebView from the Play Store, then try again."
                                } else {
                                    "Google sign-in didn't load. Check your connection."
                                },
                            )
                            else -> {}
                        }
                    },
                    onPopup = { popup = it },
                ).apply {
                    loadUrl(PortalWeb.signinUrl(clientId, silent = false, dark = false, widthPx = widthPx))
                }
            },
            onRelease = { it.destroy() },
            modifier = Modifier.fillMaxSize().alpha(if (ready) 1f else 0f),
        )
    }
    PopupHost(popup) { popup?.destroy(); popup = null }
}

/** the fallback: the real portal page, whose own login response we read */
@Composable
private fun PortalPageSignIn(vm: KioskViewModel, onError: (String) -> Unit, onClose: () -> Unit) {
    var popup by remember { mutableStateOf<WebView?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close") }
                    BodyText("Sign in on the portal page")
                }
                AndroidView(
                    factory = { ctx ->
                        PortalWeb.create(
                            ctx,
                            onSignal = { signal ->
                                if (signal is WebSignal.LoginResponse) {
                                    runCatching {
                                        val root = Transport.json.parseToJsonElement(signal.body).jsonObject
                                        vm.sessionFrom(root["response"]!!)
                                    }
                                        .onSuccess { PortalWeb.persistCookies(); vm.remember(it) }
                                        .onFailure { onError("The portal signed you in, but the session couldn't be read.") }
                                }
                                if (signal is WebSignal.Failed) onError("The portal page didn't load.")
                            },
                            onPopup = { popup = it },
                        ).apply { loadUrl(PortalWeb.PORTAL_URL) }
                    },
                    onRelease = { it.destroy() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
    PopupHost(popup) { popup?.destroy(); popup = null }
}

/** full screen host for Google's sign-in popup */
@Composable
private fun PopupHost(popup: WebView?, onDismiss: () -> Unit) {
    if (popup == null) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Cancel") }
                    BodyText("Sign in with Google")
                }
                AndroidView({ popup }, Modifier.fillMaxSize())
            }
        }
    }
}
