package com.jiitkiosk.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.jiitkiosk.app.ui.attendance.AttendanceScreen
import com.jiitkiosk.app.ui.components.BottomNav
import com.jiitkiosk.app.ui.components.Tab
import com.jiitkiosk.app.ui.exams.ExamsScreen
import com.jiitkiosk.app.ui.grades.GradesScreen
import com.jiitkiosk.app.ui.me.MeScreen
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
        Box(Modifier.fillMaxSize().padding(padding)) {
            // top-level tabs crossfade; the slide is for pages inside a tab
            Crossfade(targetState = tab, animationSpec = tween(Motion.FAST), label = "tab") { shown ->
                when (shown) {
                    Tab.Attendance -> AttendanceScreen(vm)
                    Tab.Exams -> ExamsScreen(vm)
                    Tab.Grades -> GradesScreen(vm)
                    Tab.Me -> MeScreen(vm)
                }
            }
        }
    }
}
