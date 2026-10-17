package com.jiitkiosk.app.ui.classroom

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.schedule.ClassEntry
import com.jiitkiosk.app.schedule.ClassType
import com.jiitkiosk.app.schedule.Schedule
import com.jiitkiosk.app.schedule.ScheduleStore
import com.jiitkiosk.app.schedule.dayShort
import com.jiitkiosk.app.schedule.entriesOn
import com.jiitkiosk.app.schedule.minutesOf
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.attendance.rememberSheet
import com.jiitkiosk.app.ui.components.BackRow
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.Card2
import com.jiitkiosk.app.ui.components.ChipRow
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.EmptyState
import com.jiitkiosk.app.ui.components.Load
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.PrimaryButton
import com.jiitkiosk.app.ui.components.dashedBorder
import com.jiitkiosk.app.ui.components.rememberCached
import com.jiitkiosk.app.ui.entrance
import com.jiitkiosk.app.ui.tap
import com.jiitkiosk.app.ui.theme.Bad
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.DossierMark
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.LineStrong
import com.jiitkiosk.app.ui.theme.Mono
import com.jiitkiosk.app.ui.theme.Muted
import `in`.codelif.ktjiit.model.AttendanceMeta
import `in`.codelif.ktjiit.model.SubjectAttendance
import java.time.LocalDate
import java.util.UUID

private val DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

/**
 * The Schedule pane of the Class tab: first run offers to build the week once,
 * after that it opens as the editor. Stored on this phone only.
 */
@Composable
fun SchedulePane(vm: KioskViewModel) {
    var schedule by remember { mutableStateOf(ScheduleStore.load(vm.dataDir)) }
    var building by remember { mutableStateOf(false) }
    // while nothing is saved, the only thing to look at is the builder itself
    if (schedule == null && !building) {
        FirstRun(onCreate = { building = true })
    } else {
        ScheduleBuilder(
            initial = schedule ?: Schedule(),
            canBack = schedule != null,
            hasPrev = ScheduleStore.hasPrev(vm.dataDir),
            onBack = { building = false },
            onSave = { draft ->
                schedule = ScheduleStore.save(vm.dataDir, draft)
                building = false
            },
            onRevert = { schedule = ScheduleStore.revert(vm.dataDir) },
            subjects = rememberSubjects(vm),
        )
    }
}

/** the attendance sheet's subjects, for the builder's picker; empty until that tab has data */
@Composable
private fun rememberSubjects(vm: KioskViewModel): List<SubjectAttendance> {
    val token = vm.session?.token.orEmpty()
    val meta = rememberCached(vm, "att-meta/$token", AttendanceMeta.serializer(), attempt = 0) { it.attendanceMeta() }
    val semester = (meta as? Load.Ok)?.value?.semesters?.firstOrNull()
    val sheet = rememberSheet(vm, semester, attempt = 0)
    return (sheet as? Load.Ok)?.value?.detail?.subjects.orEmpty()
}

@Composable
private fun FirstRun(onCreate: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            EmptyState(
                "Nothing carved in stone yet",
                "Your college doesn't publish class timings, so build your own once. It stays as your schedule until you change it.",
                Modifier.entrance(0).padding(top = 8.dp),
            ) { DossierMark(size = 110.dp) }
        }
        item { PrimaryButton("Create schedule", onClick = onCreate, modifier = Modifier.entrance(1)) }
        item {
            Box(Modifier.fillMaxWidth().entrance(2), contentAlignment = Alignment.Center) {
                MonoText("TAKES ABOUT A MINUTE · STORED ON THIS PHONE")
            }
        }
    }
}

@Composable
private fun ScheduleBuilder(
    initial: Schedule,
    canBack: Boolean,
    hasPrev: Boolean,
    onBack: () -> Unit,
    onSave: (Schedule) -> Unit,
    onRevert: () -> Unit,
    subjects: List<SubjectAttendance>,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var day by rememberSaveable { mutableIntStateOf(LocalDate.now().dayOfWeek.value.takeIf { it in 1..6 } ?: 1) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var subject by remember { mutableStateOf<SubjectAttendance?>(null) }
    var subjectText by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf(false) }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var type by remember { mutableIntStateOf(0) }
    var copyTo by remember { mutableStateOf(emptySet<Int>()) }
    var error by remember { mutableStateOf<String?>(null) }

    fun resetForm() {
        editingId = null
        subject = null
        subjectText = ""
        picking = false
        manual = false
        start = ""
        end = ""
        room = ""
        type = 0
        copyTo = emptySet()
        error = null
    }

    fun apply() {
        val name = subjectText.trim()
        if (name.isEmpty()) {
            error = "PICK OR TYPE A SUBJECT"
            return
        }
        val from = minutesOf(start)
        if (from == null) {
            error = "START NEEDS HH:MM"
            return
        }
        val to = minutesOf(end)
        if (to == null) {
            error = "END NEEDS HH:MM"
            return
        }
        if (to <= from) {
            error = "END MUST BE AFTER START"
            return
        }
        val entry = ClassEntry(
            id = editingId ?: UUID.randomUUID().toString(),
            day = day,
            start = start.trim(),
            end = end.trim(),
            subject = name,
            subjectId = subject?.subjectId,
            room = room.trim().ifBlank { null },
            type = ClassType.entries[type],
        )
        val days = (listOf(day) + copyTo).distinct()
        draft = draft.copy(entries = draft.entries.filterNot { it.id == entry.id } + days.map { entry.copy(day = it) })
        resetForm()
    }

    fun startEdit(entry: ClassEntry) {
        editingId = entry.id
        subject = subjects.firstOrNull { it.subjectId == entry.subjectId }
        subjectText = entry.subject
        picking = false
        manual = false
        start = entry.start
        end = entry.end
        room = entry.room.orEmpty()
        type = ClassType.entries.indexOf(entry.type)
        copyTo = emptySet()
        error = null
    }

    fun remove(entry: ClassEntry) {
        draft = draft.copy(entries = draft.entries.filterNot { it.id == entry.id })
        if (editingId == entry.id) resetForm()
    }

    if (canBack) BackHandler { onBack() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (canBack) item { BackRow(onBack, Modifier.entrance(0)) }
        item { D("Schedule", 56.sp, modifier = Modifier.entrance(0)) }
        item { MonoText("BUILDER · SAVED SCHEDULE STAYS UNTIL YOU CHANGE IT", modifier = Modifier.entrance(1)) }
        if (hasPrev) {
            item {
                MonoText(
                    "UNDO LAST SAVE",
                    color = Crimson,
                    medium = true,
                    modifier = Modifier.entrance(1).tap(onRevert).padding(vertical = 6.dp),
                )
            }
        }
        item { ChipRow(DAYS, day - 1, Modifier.entrance(2).padding(top = 6.dp)) { day = it + 1 } }

        val onDay = draft.entriesOn(day)
        items(onDay, key = { it.id }) { entry ->
            EntryRow(
                entry = entry,
                onEdit = { startEdit(entry) },
                onRemove = { remove(entry) },
                modifier = Modifier.entrance(3),
            )
        }

        item {
            Column(
                Modifier
                    .dashedBorder(LineStrong)
                    .entrance(4)
                    .padding(12.dp),
            ) {
                MonoText("ADD A CLASS · ${dayShort(day)}")
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .border(2.dp, LineStrong)
                        .tap { picking = !picking }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Column {
                        MonoText("SUBJECT")
                        BodyText(
                            subjectText.ifBlank { "PICK OR TYPE" },
                            size = 15.sp,
                            color = if (subjectText.isBlank()) Muted else Bone,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                if (picking) {
                    Column(Modifier.padding(top = 6.dp)) {
                        subjects.forEach { s ->
                            MonoText(
                                s.name.uppercase(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tap {
                                        subject = s
                                        subjectText = s.name
                                        picking = false
                                        manual = false
                                    }
                                    .padding(vertical = 10.dp),
                            )
                        }
                        MonoText(
                            if (subjects.isEmpty()) "NO SUBJECTS ON THE ATTENDANCE SHEET YET" else "OR TYPE YOUR OWN",
                            modifier = Modifier
                                .fillMaxWidth()
                                .tap {
                                    picking = false
                                    manual = true
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
                if (manual || subjects.isEmpty()) {
                    BasicTextField(
                        value = subjectText,
                        onValueChange = {
                            subjectText = it
                            subject = null
                        },
                        singleLine = true,
                        textStyle = Mono.copy(color = Bone, fontSize = 15.sp),
                        cursorBrush = SolidColor(Crimson),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(44.dp),
                        decorationBox = { inner ->
                            Box(
                                Modifier.fillMaxSize().border(2.dp, LineStrong).padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (subjectText.isEmpty()) MonoText("SUBJECT NAME")
                                inner()
                            }
                        },
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SmallField("START", start, "HH:MM", { start = it }, Modifier.weight(1f))
                    SmallField("END", end, "HH:MM", { end = it }, Modifier.weight(1f))
                    SmallField("ROOM", room, "OPT", { room = it }, Modifier.weight(1f))
                }
                ChipRow(listOf("Lecture", "Tutorial", "Lab"), type, Modifier.padding(top = 12.dp)) { type = it }
                Spacer(Modifier.height(10.dp))
                MonoText("COPY TO THE WEEK'S OTHER DAYS")
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DAYS.forEachIndexed { i, label ->
                        if (i + 1 != day) {
                            val on = (i + 1) in copyTo
                            MonoText(
                                label.uppercase(),
                                color = Bone,
                                modifier = Modifier
                                    .tap { copyTo = if (on) copyTo - (i + 1) else copyTo + (i + 1) }
                                    .background(if (on) Crimson else Line)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                error?.let { MonoText(it, color = Bad, modifier = Modifier.padding(top = 8.dp)) }
                PrimaryButton(
                    if (editingId != null) "Update class" else "Add class",
                    onClick = { apply() },
                    modifier = Modifier.padding(top = 10.dp),
                )
                if (editingId != null) {
                    MonoText(
                        "CANCEL EDIT",
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .tap { resetForm() }
                            .padding(top = 10.dp),
                    )
                }
            }
        }

        item {
            PrimaryButton(
                "Save schedule",
                onClick = { onSave(draft) },
                modifier = Modifier.entrance(5).padding(top = 6.dp),
            )
        }
    }
}

/** one class already on the day being edited */
@Composable
private fun EntryRow(entry: ClassEntry, onEdit: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Card2(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                MonoText("${entry.start} – ${entry.end}")
                BodyText(entry.subject, size = 16.sp, maxLines = 2, modifier = Modifier.padding(vertical = 4.dp))
                MonoText(
                    listOfNotNull(entry.type.name.uppercase(), entry.room?.let { "ROOM $it" }).joinToString(" · "),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                MonoText("EDIT", color = Muted, modifier = Modifier.tap(onEdit).padding(vertical = 10.dp))
                MonoText("REMOVE", color = Muted, modifier = Modifier.tap(onRemove).padding(vertical = 10.dp))
            }
        }
    }
}

/** one bracketed field of the add form */
@Composable
private fun SmallField(
    label: String,
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.border(2.dp, LineStrong).padding(horizontal = 10.dp, vertical = 8.dp)) {
        MonoText(label)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = Mono.copy(color = Bone, fontSize = 15.sp),
            cursorBrush = SolidColor(Crimson),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(24.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) MonoText(placeholder)
                    inner()
                }
            },
        )
    }
}
