package com.jiitkiosk.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.jiitkiosk.app.signin.SignInScreen
import com.jiitkiosk.app.ui.AppShell
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.theme.KioskTheme
import com.jiitkiosk.app.ui.theme.LocalPureBlack
import com.jiitkiosk.app.widget.KioskWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: KioskViewModel by viewModels()

    /** where a widget tap asked to land; the shell consumes it once */
    private var open by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        open = intent.getStringExtra(EXTRA_OPEN)
    }

    /**
     * Opening the app is the user asking for what is on the home screen to be
     * current, and a chance to renew the portal token before it is ever missed.
     * The app may resume on any tab, so this cannot live in one of them.
     */
    override fun onResume() {
        super.onResume()
        KioskWidget.refresh(this)
        lifecycleScope.launch { vm.revive() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        open = intent.getStringExtra(EXTRA_OPEN)
        // the app is dark whatever the system is set to, so the bar icons stay light
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            CompositionLocalProvider(LocalPureBlack provides vm.pureBlack) {
                KioskTheme {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (vm.session == null || vm.reconnecting) {
                            // back returns to the shell: the local session and every cache is still there
                            BackHandler(enabled = vm.reconnecting) { vm.cancelReconnect() }
                            SignInScreen(vm)
                        } else {
                            AppShell(vm, open) { open = null }
                        }
                    }
                }
            }
        }
    }

    companion object {
        /** the extra a widget tap carries: schedule, attendance, assignments, reconnect */
        const val EXTRA_OPEN = "com.jiitkiosk.app.open"
    }
}
