package com.jiitkiosk.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.jiitkiosk.app.ui.attendance.AttendanceScreen
import com.jiitkiosk.app.ui.classroom.ClassroomScreen
import com.jiitkiosk.app.ui.code.CodeScreen
import com.jiitkiosk.app.ui.components.BottomNav
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.Tab
import com.jiitkiosk.app.ui.exams.ExamsScreen
import com.jiitkiosk.app.ui.me.MeScreen
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.CrimsonDeep
import com.jiitkiosk.app.ui.theme.Line

@Composable
fun AppShell(vm: KioskViewModel) {
    // the design opens on Attendance: there is no home tab in it
    var tab by rememberSaveable { mutableStateOf(Tab.Attendance) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .drawBehind {
                        drawLine(Line, Offset(0f, 0.5f), Offset(size.width, 0.5f), 1.dp.toPx())
                    }
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                BottomNav(tab) { tab = it }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (vm.expired) SessionBanner { vm.startReconnect() }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                // top-level tabs crossfade; the slide is for pages inside a tab. The
                // holder is why a tab comes back with its scroll and its open subject.
                val holder = rememberSaveableStateHolder()
                Crossfade(targetState = tab, animationSpec = tween(Motion.FAST), label = "tab") { shown ->
                    holder.SaveableStateProvider(shown) {
                        when (shown) {
                            Tab.Attendance -> AttendanceScreen(vm)
                            Tab.Exams -> ExamsScreen(vm)
                            Tab.Class -> ClassroomScreen(vm)
                            Tab.Code -> CodeScreen(vm)
                            Tab.Me -> MeScreen(vm)
                        }
                    }
                }
            }
        }
    }
}

/** the soft-expired session line: cached data stays live under it, one tap reconnects */
@Composable
private fun SessionBanner(onTap: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(CrimsonDeep)
            .drawBehind {
                drawLine(Crimson, Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx())
            }
            .tap(onTap)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        MonoText("SESSION EXPIRED · TAP TO RECONNECT", color = Crimson, medium = true)
    }
}
