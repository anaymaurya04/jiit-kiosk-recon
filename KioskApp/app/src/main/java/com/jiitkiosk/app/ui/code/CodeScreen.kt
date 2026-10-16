package com.jiitkiosk.app.ui.code

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.RowLine
import com.jiitkiosk.app.ui.components.ScreenTitle
import com.jiitkiosk.app.ui.components.SectionHead

/**
 * The Code tab: the LeetCode and GitHub profiles whose usernames live in
 * Settings. Both stay NOT SET until a username is saved there.
 */
@Composable
fun CodeScreen(vm: KioskViewModel) {
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { ScreenTitle("Code", meta = "LEETCODE & GITHUB") }
        item { SectionHead("Profiles", Modifier.padding(top = 10.dp, bottom = 4.dp)) }
        item { RowLine(label = "LeetCode", trailing = { MonoText("NOT SET") }) }
        item { RowLine(label = "GitHub", trailing = { MonoText("NOT SET") }) }
        item { MonoText("Add your usernames in Me → Settings.", Modifier.padding(top = 10.dp)) }
    }
}
