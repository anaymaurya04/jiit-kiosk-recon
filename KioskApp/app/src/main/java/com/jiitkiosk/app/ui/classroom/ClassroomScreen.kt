package com.jiitkiosk.app.ui.classroom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.components.EmptyState
import com.jiitkiosk.app.ui.components.ScreenTitle
import com.jiitkiosk.app.ui.theme.TomeMark

/**
 * The Class tab. Until a Classroom account is connected there is nothing to
 * show, and saying exactly that is the honest first frame.
 */
@Composable
fun ClassroomScreen(vm: KioskViewModel) {
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle("Class", meta = "COURSEWORK & DEADLINES") }
        item {
            EmptyState(
                "Not Connected",
                "Connect your Classroom account from Me → Settings to see coursework and deadlines here.",
            ) { TomeMark(size = 110.dp) }
        }
    }
}
