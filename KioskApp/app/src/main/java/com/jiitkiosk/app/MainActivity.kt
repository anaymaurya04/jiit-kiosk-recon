package com.jiitkiosk.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jiitkiosk.app.signin.SignInScreen
import com.jiitkiosk.app.ui.AppShell
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.theme.KioskTheme
import com.jiitkiosk.app.ui.theme.LocalPureBlack
import com.jiitkiosk.app.widget.KioskWidget

class MainActivity : ComponentActivity() {

    /**
     * Opening the app is the user asking for what is on the home screen to be
     * current. The app may resume on any tab, so this cannot live in one of them.
     */
    override fun onResume() {
        super.onResume()
        KioskWidget.refresh(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // the app is dark whatever the system is set to, so the bar icons stay light
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            val vm: KioskViewModel = viewModel()
            CompositionLocalProvider(LocalPureBlack provides vm.pureBlack) {
                KioskTheme {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (vm.session == null) SignInScreen(vm) else AppShell(vm)
                    }
                }
            }
        }
    }
}
