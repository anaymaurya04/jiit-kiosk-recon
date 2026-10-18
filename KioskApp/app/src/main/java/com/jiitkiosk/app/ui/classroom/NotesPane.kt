package com.jiitkiosk.app.ui.classroom

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.classroom.ClassroomData
import com.jiitkiosk.app.classroom.Material
import com.jiitkiosk.app.classroom.MaterialKind
import com.jiitkiosk.app.classroom.NotesPrefs
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.Card2
import com.jiitkiosk.app.ui.components.ChipRow
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.EmptyState
import com.jiitkiosk.app.ui.components.HeroPanel
import com.jiitkiosk.app.ui.components.Load
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.StateBox
import com.jiitkiosk.app.ui.components.PrimaryButton
import com.jiitkiosk.app.ui.components.SkeletonScreen
import com.jiitkiosk.app.ui.components.Stamp
import com.jiitkiosk.app.ui.entrance
import com.jiitkiosk.app.ui.tap
import com.jiitkiosk.app.ui.theme.Bad
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.CassetteMark
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.DossierMark
import com.jiitkiosk.app.ui.theme.Ink
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.LineStrong
import com.jiitkiosk.app.ui.theme.Mono
import com.jiitkiosk.app.ui.theme.Muted
import com.jiitkiosk.app.ui.theme.NecromancerMark
import com.jiitkiosk.app.ui.theme.Surface2
import com.jiitkiosk.app.ui.theme.TomeMark
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val NOTE_DAY = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
private const val WEEK_MS = 7L * 24 * 3_600_000

/**
 * The Notes pane: materials from the connected courses, grouped course → topic,
 * badged by kind and tagged NEW until opened. Materials only — coursework lives
 * in the Assignments pane.
 */
@Composable
fun NotesPane(vm: KioskViewModel, state: Load<ClassroomData>, connect: ClassroomConnect, onRetry: () -> Unit) {
    val context = LocalContext.current
    val notes = remember(context) { NotesPrefs(context) }
    var seen by remember { mutableStateOf(notes.seen()) }
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val now = remember(state) { System.currentTimeMillis() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!connect.connected) {
            item {
                EmptyState(
                    "Not Connected",
                    "Course materials show up here once Google Classroom is connected. The app reads them; it never writes.",
                    Modifier.entrance(0),
                ) { TomeMark(size = 110.dp) }
            }
            item {
                PrimaryButton(
                    if (connect.connecting) "Connecting…" else "Connect Classroom",
                    onClick = connect.connect,
                    enabled = !connect.connecting,
                    modifier = Modifier.entrance(1),
                )
            }
            connect.error?.let { item { MonoText(it, color = Bad, modifier = Modifier.entrance(2)) } }
            return@LazyColumn
        }

        when (state) {
            is Load.Loading -> item { SkeletonScreen() }

            is Load.Failed -> item {
                // the one StateBox every screen uses; the copy stays Classroom's own
                StateBox(state, onRetry, message = classroomFriendly(state.error))
            }

            is Load.Ok -> {
                val all = state.value.materials
                val fresh = all.filter { it.createdAt > 0 && now - it.createdAt <= WEEK_MS }
                item { NotesHero(fresh.size, Modifier.entrance(0)) }
                item { SearchField(search, Modifier.entrance(1)) { search = it } }
                item { ChipRow(listOf("All", "PDF", "Slides", "Docs", "Links"), filter, Modifier.entrance(2)) { filter = it } }

                val query = search.trim()
                val visible = all.filter { m ->
                    filterMatch(m, filter) &&
                        (
                            query.isEmpty() ||
                                m.title.contains(query, ignoreCase = true) ||
                                m.courseName.contains(query, ignoreCase = true) ||
                                m.topic?.contains(query, ignoreCase = true) == true
                            )
                }
                if (visible.isEmpty()) {
                    item {
                        EmptyState(
                            if (all.isEmpty()) "No Materials" else "Nothing Matches",
                            if (all.isEmpty()) {
                                "Nothing has been posted in your active courses' materials yet."
                            } else {
                                "No material matches this search or filter."
                            },
                            Modifier.entrance(3),
                        ) {
                            // board 21 gives the missed search its own séance; the empty list keeps the archive
                            if (all.isEmpty()) {
                                DossierMark(size = 150.dp)
                            } else {
                                NecromancerMark(
                                    size = 170.dp,
                                    color = LineStrong,
                                    accent = LineStrong,
                                    aux = LineStrong,
                                )
                            }
                        }
                    }
                } else {
                    for ((course, list) in visible.groupBy { it.courseName }) {
                        item(key = "course/$course") { CourseHead(course, list.size, Modifier.entrance(3)) }
                        for ((topic, mats) in list.groupBy { it.topic }) {
                            if (topic != null) {
                                item(key = "topic/$course/$topic") {
                                    MonoText("[${topic.uppercase()}]", modifier = Modifier.entrance(3).padding(top = 4.dp))
                                }
                            }
                            items(mats, key = { "${it.courseId}/${it.id}" }) { material ->
                                NoteRow(
                                    material = material,
                                    isNew = material.createdAt > 0 && now - material.createdAt <= WEEK_MS &&
                                        material.id !in seen,
                                    onOpen = {
                                        seen = seen + material.id
                                        notes.mark(material.id)
                                    },
                                    modifier = Modifier.entrance(4),
                                )
                            }
                        }
                    }
                }
                item {
                    MonoText(
                        "MATERIALS ONLY · NO ASSIGNMENTS · TAP A FILE TO OPEN IT IN YOUR PHONE'S DEFAULT APP",
                        modifier = Modifier.entrance(5),
                    )
                }
            }
        }
    }
}

/** the quiet hero: how much arrived in the last seven days */
@Composable
private fun NotesHero(count: Int, modifier: Modifier = Modifier) {
    HeroPanel(
        height = 120.dp,
        angle = 160f,
        stops = arrayOf(0f to Surface2, 0.85f to Ink),
        modifier = modifier,
        art = {
            // the cassette runs off the bottom-right corner, as on the board
            Box(Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = 24.dp)) {
                CassetteMark(size = 150.dp)
            }
        },
    ) {
        Column(Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 14.dp)) {
            Stamp("New this week")
            Spacer(Modifier.height(10.dp))
            D("$count", 60.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SearchField(value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = Mono.copy(color = Bone, fontSize = 15.sp),
        cursorBrush = SolidColor(Crimson),
        modifier = modifier.fillMaxWidth().height(48.dp),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxSize().border(2.dp, Bone).padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) MonoText("Search notes…")
                inner()
            }
        },
    )
}

/** the course header: the name, the file count, and the crimson rule under it */
@Composable
private fun CourseHead(name: String, files: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            D(name, 22.sp, maxLines = 2, modifier = Modifier.weight(1f))
            MonoText("$files ${if (files == 1) "FILE" else "FILES"}")
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Crimson))
    }
}

/** one material: kind badge, title, the day it was posted, NEW until opened */
@Composable
private fun NoteRow(material: Material, isNew: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    Card2(
        modifier = modifier,
        onClick = {
            val link = material.link
            if (link == null) {
                failed = true
            } else {
                val took = runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                }.isSuccess
                failed = !took
                if (took) onOpen()
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stamp(badge(material.kind), background = Line, color = Bone)
            Spacer(Modifier.width(8.dp))
            BodyText(material.title, size = 16.sp, maxLines = 2, modifier = Modifier.weight(1f))
            if (isNew) {
                Spacer(Modifier.width(8.dp))
                Stamp("New", background = Crimson, color = Bone)
            }
        }
        val posted = postedText(material)
        if (posted != null) {
            Spacer(Modifier.height(6.dp))
            MonoText(posted)
        }
        if (failed) {
            Spacer(Modifier.height(6.dp))
            MonoText("NO APP TOOK THIS LINK", color = Bad)
        }
    }
}

private fun badge(kind: MaterialKind): String = when (kind) {
    MaterialKind.Pdf -> "PDF"
    MaterialKind.Slides -> "PPT"
    MaterialKind.Docs -> "DOC"
    MaterialKind.Link -> "URL"
}

/** the board's "[SIZE] · [DATE]" line; the API offers no size, so the date stands alone */
private fun postedText(m: Material): String? {
    val at = m.createdAt.takeIf { it > 0 } ?: return null
    return "ADDED " + Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(NOTE_DAY).uppercase(Locale.ENGLISH)
}

private fun filterMatch(m: Material, filter: Int): Boolean = when (filter) {
    1 -> m.kind == MaterialKind.Pdf
    2 -> m.kind == MaterialKind.Slides
    3 -> m.kind == MaterialKind.Docs
    4 -> m.kind == MaterialKind.Link
    else -> true
}
