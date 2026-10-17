package com.jiitkiosk.app.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.code.CodePrefs
import com.jiitkiosk.app.ui.components.BackRow
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.Card2
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.DetailRow
import com.jiitkiosk.app.ui.components.EmptyState
import com.jiitkiosk.app.ui.components.FreshnessLine
import com.jiitkiosk.app.ui.components.KeyRow
import com.jiitkiosk.app.ui.components.Load
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.NavRow
import com.jiitkiosk.app.ui.components.PrimaryButton
import com.jiitkiosk.app.ui.components.SectionHead
import com.jiitkiosk.app.ui.classroom.rememberClassroomConnect
import com.jiitkiosk.app.ui.calendar.CalendarScreen
import com.jiitkiosk.app.ui.grades.GradesScreen
import com.jiitkiosk.app.ui.reminders.RemindersScreen
import com.jiitkiosk.app.reminders.ReminderPrefs
import com.jiitkiosk.app.ui.components.StateBox
import com.jiitkiosk.app.ui.components.Toggle
import com.jiitkiosk.app.ui.components.attempt
import com.jiitkiosk.app.ui.components.friendly
import com.jiitkiosk.app.ui.components.rememberCached
import com.jiitkiosk.app.ui.components.scrollDepth
import com.jiitkiosk.app.ui.entrance
import com.jiitkiosk.app.ui.theme.Bad
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.Ink
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.LineStrong
import com.jiitkiosk.app.ui.theme.Mono
import com.jiitkiosk.app.ui.theme.Muted
import com.jiitkiosk.app.ui.theme.Steel
import com.jiitkiosk.app.ui.theme.TrooperMark
import com.jiitkiosk.app.ui.theme.Violet
import com.jiitkiosk.app.ui.theme.halftone
import `in`.codelif.ktjiit.model.BankInfo
import `in`.codelif.ktjiit.model.FeeSummary
import `in`.codelif.ktjiit.model.FeedbackEvent
import `in`.codelif.ktjiit.model.FeedbackRow
import `in`.codelif.ktjiit.model.GeneralInfo
import `in`.codelif.ktjiit.model.HostelInfo
import `in`.codelif.ktjiit.model.PersonalInfo
import `in`.codelif.ktjiit.model.Rating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.jiitkiosk.app.ui.components.ArtTile
import com.jiitkiosk.app.ui.components.RowLine
import com.jiitkiosk.app.ui.components.SemesterTrack
import com.jiitkiosk.app.ui.components.StudentFileCard
import com.jiitkiosk.app.ui.components.ago
import com.jiitkiosk.app.ui.rememberSpin
import com.jiitkiosk.app.updates.UpdateCheck
import com.jiitkiosk.app.updates.Updates
import com.jiitkiosk.app.ui.theme.CometMark
import com.jiitkiosk.app.ui.theme.DossierMark
import com.jiitkiosk.app.ui.theme.GearMark
import com.jiitkiosk.app.ui.theme.HelmMark
import com.jiitkiosk.app.ui.theme.HourglassMark
import com.jiitkiosk.app.ui.theme.MarkCore
import com.jiitkiosk.app.ui.theme.MicMark
import com.jiitkiosk.app.ui.theme.RavenMark
import com.jiitkiosk.app.ui.theme.ShieldMark
import com.jiitkiosk.app.ui.theme.TomeMark
import com.jiitkiosk.app.ui.theme.TowerMark
import com.jiitkiosk.app.ui.theme.VinylMark
import kotlinx.coroutines.delay
import java.util.Locale

/** everything the Me tab shows, fetched together so one fetch feeds four screens */
@Serializable
data class MeData(
    val info: PersonalInfo? = null,
    val bank: BankInfo? = null,
    val hostel: HostelInfo? = null,
    val fees: FeeSummary? = null,
)

private val FEEDBACK_EVENTS = ListSerializer(FeedbackEvent.serializer())
private val JSON_ROWS = ListSerializer(JsonObject.serializer())

@Composable
private fun rememberMe(vm: KioskViewModel, attempt: Int): Load<MeData> {
    val token = vm.session?.token.orEmpty()
    return rememberCached(vm, "me/$token", MeData.serializer(), attempt) { portal ->
        coroutineScope {
            // each part is allowed to be missing, but all four missing means the
            // portal never answered: throwing keeps the cache and the header honest
            val info = async { attempt { portal.personalInfo() } }
            val bank = async { attempt { portal.bankInfo() } }
            val hostel = async { attempt { portal.hostel() } }
            val fees = async { attempt { portal.fees() } }
            val parts = listOf(info, bank, hostel, fees).map { it.await() }
            if (parts.all { it.isFailure }) {
                throw parts.firstNotNullOf { it.exceptionOrNull() }
            }
            MeData(
                info.await().getOrNull(),
                bank.await().getOrNull(),
                hostel.await().getOrNull(),
                fees.await().getOrNull(),
            )
        }
    }
}

private enum class MePage { Overview, Profile, Fees, Hostel, Grades, Calendar, Settings, Feedback, Reminders }

@Composable
fun MeScreen(vm: KioskViewModel, openReminders: Boolean, onRemindersOpened: () -> Unit) {
    var page by remember { mutableStateOf(MePage.Overview) }
    // one counter for the whole tab: every Me screen reads the same cached record
    var attempt by remember { mutableStateOf(0) }
    val retry: () -> Unit = { attempt++ }

    // the Assignments pane's Reminders button lands here
    LaunchedEffect(openReminders) {
        if (openReminders) {
            page = MePage.Reminders
            onRemindersOpened()
        }
    }

    // the system back button goes up a level before it leaves the app
    BackHandler(enabled = page != MePage.Overview) { page = MePage.Overview }

    when (page) {
        MePage.Overview -> MeOverview(vm, attempt, retry) { page = it }
        MePage.Profile -> ProfileScreen(vm, attempt, retry) { page = MePage.Overview }
        MePage.Fees -> FeesScreen(vm, attempt, retry) { page = MePage.Overview }
        MePage.Hostel -> HostelScreen(vm, attempt, retry) { page = MePage.Overview }
        MePage.Grades -> GradesScreen(vm, onExit = { page = MePage.Overview })
        MePage.Calendar -> CalendarScreen(vm) { page = MePage.Overview }
        MePage.Settings -> SettingsScreen(
            vm,
            onBack = { page = MePage.Overview },
            onReminders = { page = MePage.Reminders },
        )
        MePage.Feedback -> FeedbackScreen(vm) { page = MePage.Overview }
        MePage.Reminders -> RemindersScreen(vm) { page = MePage.Settings }
    }
}

@Composable
private fun MeOverview(vm: KioskViewModel, attempt: Int, onRetry: () -> Unit, open: (MePage) -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    val state = rememberMe(vm, attempt)
    val data = (state as? Load.Ok)?.value
    val general = data?.info?.general
    val list = rememberLazyListState()
    val program = rememberProgram(vm, attempt)

    val currentSem = general?.semester?.takeIf { it > 0 }
    val totalSem = program

    // spec: the account number re-masks itself half a minute after a reveal
    LaunchedEffect(revealed) {
        if (revealed) {
            delay(30_000)
            revealed = false
        }
    }

    val feeDue = data?.fees?.heads.orEmpty().sumOf { it.due }
    val feeSub = if (data?.fees == null) "LEDGER" else if (feeDue > 0) "DUE: ${money(feeDue)}" else "ALL CLEAR"
    val hostelSub = data?.hostel?.let {
        listOf(it.hostel, it.room).filter { s -> s.isNotBlank() }.joinToString(" · ")
    }?.ifBlank { "NO ROOM" } ?: "HOSTEL"
    val profileSub = listOf(general?.program, general?.batch)
        .filter { !it.isNullOrBlank() }
        .joinToString(" · ")
        .ifBlank { "STUDENT FILE" }

    Box(Modifier.fillMaxWidth()) {
        LazyColumn(
            Modifier.fillMaxWidth(),
            state = list,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        ) {
            item {
                StudentFileCard(
                    name = general?.name?.takeIf { it.isNotBlank() } ?: vm.session?.username.orEmpty(),
                    enrolment = general?.enrollmentNo ?: vm.session?.username.orEmpty(),
                    modifier = Modifier.entrance(0),
                    height = 190.dp,
                ) {
                    Box(Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = 10.dp)) {
                        TrooperMark(band = Violet, card = Bone, clip = Crimson, size = 150.dp)
                    }
                }
            }

            item {
                Column(Modifier.padding(top = 14.dp).entrance(1)) {
                    if (currentSem != null) {
                        MonoText(
                            if (totalSem != null) "SEMESTER $currentSem OF $totalSem" else "SEMESTER $currentSem",
                            color = Muted,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    FreshnessLine(state)
                    if (currentSem != null && totalSem != null) {
                        Spacer(Modifier.height(6.dp))
                        SemesterTrack(current = currentSem, total = totalSem)
                    }
                }
            }

            item {
                D("YOUR FILES", 22.sp, color = Muted, modifier = Modifier.padding(top = 22.dp, bottom = 12.dp).entrance(2))
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().entrance(3),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Row 1: Profile & Fees
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ArtTile(
                            name = "Profile",
                            sub = profileSub,
                            background = Color(0xFF14202E),
                            rotate = -1f,
                            modifier = Modifier.weight(1f).height(130.dp),
                            onClick = { open(MePage.Profile) },
                        ) {
                            Box(Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-8).dp)) {
                                DossierMark(size = 100.dp)
                            }
                        }
                        ArtTile(
                            name = "Fees",
                            sub = feeSub,
                            background = Color(0xFF5A1522),
                            rotate = 1f,
                            modifier = Modifier.weight(1f).height(130.dp),
                            onClick = { open(MePage.Fees) },
                        ) {
                            Box(Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-8).dp)) {
                                CometMark(size = 100.dp)
                            }
                        }
                    }

                    // Row 2: Hostel & Bank
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ArtTile(
                            name = "Hostel",
                            sub = hostelSub,
                            background = Color(0xFF1C1C22),
                            rotate = 1f,
                            modifier = Modifier.weight(1f).height(130.dp),
                            onClick = { open(MePage.Hostel) },
                        ) {
                            Box(Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-8).dp)) {
                                TowerMark(size = 100.dp)
                            }
                        }
                        ArtTile(
                            name = "Bank",
                            sub = if (revealed) (data?.bank?.account?.takeIf { it.isNotBlank() } ?: "REVEALED") else "•••• TAP TO REVEAL",
                            background = Color(0xFF2B2060),
                            rotate = -1f,
                            modifier = Modifier.weight(1f).height(130.dp),
                            onClick = { revealed = !revealed },
                        ) {
                            Box(Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-8).dp)) {
                                ShieldMark(size = 100.dp)
                            }
                        }
                    }

                    // Row 3: Feedback & Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ArtTile(
                            name = "Feedback",
                            sub = "RATINGS",
                            background = Color(0xFF1C1C22),
                            rotate = -1f,
                            modifier = Modifier.weight(1f).height(130.dp),
                            onClick = { open(MePage.Feedback) },
                        ) {
                            Box(Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-8).dp)) {
                                MicMark(size = 100.dp)
                            }
                        }
                        ArtTile(
                            name = "Settings",
                            sub = "About · privacy",
                            background = Color(0xFF2A0A12),
                            rotate = 1f,
                            modifier = Modifier.weight(1f).height(130.dp),
                            onClick = { open(MePage.Settings) },
                        ) {
                            Box(Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-8).dp)) {
                                GearMark(size = 100.dp)
                            }
                        }
                    }

                    // Row 4: Grades and Calendar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ArtTile(
                            name = "Grades",
                            sub = "RESULT CARDS",
                            background = Color(0xFF14202E),
                            rotate = -1f,
                            modifier = Modifier.weight(1f).height(130.dp),
                            onClick = { open(MePage.Grades) },
                        ) {
                            Box(Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-8).dp)) {
                                TomeMark(size = 100.dp)
                            }
                        }
                        ArtTile(
                            name = "Calendar",
                            sub = "SEMESTER DATES",
                            background = Color(0xFF2B2060),
                            rotate = 1f,
                            modifier = Modifier.weight(1f).height(130.dp),
                            onClick = { open(MePage.Calendar) },
                        ) {
                            Box(Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-8).dp)) {
                                HourglassMark(size = 100.dp)
                            }
                        }
                    }
                }
            }

            if (revealed) {
                item {
                    val bank = data?.bank
                    Card2(Modifier.padding(top = 14.dp)) {
                        KeyRow("Bank", bank?.bank.orEmpty().ifBlank { "—" })
                        KeyRow("Account", bank?.account.orEmpty().ifBlank { "—" })
                        KeyRow("IFSC", bank?.ifsc.orEmpty().ifBlank { "—" })
                        KeyRow("Holder", bank?.holder.orEmpty().ifBlank { "—" })
                        if (bank?.frozen?.isNotBlank() == true) KeyRow("Frozen", bank.frozen)
                    }
                }
            }

            if (state is Load.Failed) {
                item { Spacer(Modifier.height(14.dp)); StateBox(state, onRetry) }
            }
        }

        // compact name once the avatar header has scrolled away; the scroll is
        // read in the draw phase so scrolling never recomposes the screen
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(Ink)
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp)
                .graphicsLayer { alpha = (list.scrollDepth() / 100f).coerceIn(0f, 1f) },
        ) {
            D(
                general?.name?.takeIf { it.isNotBlank() } ?: vm.session?.username.orEmpty(),
                22.sp,
                maxLines = 1,
            )
        }
    }
}

/** the portal's own programme length (stymax); null until it answers */
@Composable
private fun rememberProgram(vm: KioskViewModel, attempt: Int): Int? {
    val token = vm.session?.token.orEmpty()
    val state = rememberCached(
        vm, "program/$token",
        `in`.codelif.ktjiit.model.StudentProgram.serializer(), attempt,
    ) { it.program() }
    return (state as? Load.Ok)?.value?.maxSemesters?.takeIf { it > 0 }
}

@Composable
private fun ProfileScreen(vm: KioskViewModel, attempt: Int, onRetry: () -> Unit, onBack: () -> Unit) {
    val state = rememberMe(vm, attempt)
    val general = (state as? Load.Ok)?.value?.info?.general
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val currentSem = general?.semester?.takeIf { it > 0 }
    val totalSem = rememberProgram(vm, attempt)

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
    ) {
        item { BackRow(onBack, Modifier.entrance(0)) }
        item { D("Profile", 56.sp, modifier = Modifier.padding(top = 4.dp).entrance(1)) }
        item { FreshnessLine(state, Modifier.padding(top = 8.dp, bottom = 12.dp).entrance(1)) }

        if (state is Load.Loading) item { StateBox(state) }
        if (state is Load.Failed) item { StateBox(state, onRetry) }
        if (general == null && state is Load.Ok) {
            item { MonoText("The portal has no personal record on file.") }
        }

        if (general != null) {
            item {
                StudentFileCard(
                    name = general.name.ifBlank { vm.session?.username.orEmpty() },
                    enrolment = general.enrollmentNo.ifBlank { vm.session?.username.orEmpty() },
                    modifier = Modifier.entrance(2),
                    height = 170.dp,
                ) {
                    Box(Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = 10.dp)) {
                        TrooperMark(band = Violet, card = Bone, clip = Crimson, size = 150.dp)
                    }
                }
            }

            item {
                D("PROGRAM", 22.sp, color = Muted, modifier = Modifier.padding(top = 22.dp, bottom = 12.dp).entrance(3))
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().entrance(4),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 2x2 grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Card2(
                            modifier = Modifier.weight(1f),
                            art = {
                                Box(Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-10).dp)) {
                                    TowerMark(size = 70.dp, color = LineStrong)
                                }
                            },
                        ) {
                            MonoText("BATCH", color = Muted)
                            D(general.batch.ifBlank { "—" }, 40.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                        }
                        Card2(
                            modifier = Modifier.weight(1f),
                            art = {
                                Box(Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-10).dp)) {
                                    HelmMark(arch = LineStrong, accent = LineStrong, detail = Line, size = 70.dp)
                                }
                            },
                        ) {
                            MonoText("SECTION", color = Muted)
                            D(general.section.ifBlank { "—" }, 40.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Card2(
                            modifier = Modifier.weight(1f),
                            art = {
                                Box(Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-10).dp)) {
                                    HourglassMark(size = 70.dp, color = LineStrong)
                                }
                            },
                        ) {
                            MonoText("SEMESTER", color = Muted)
                            Row(verticalAlignment = Alignment.Bottom) {
                                if (currentSem != null) {
                                    D("$currentSem", 40.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                                    if (totalSem != null) {
                                        MonoText(" OF $totalSem", color = Muted, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                                    }
                                } else {
                                    MonoText("PENDING", color = Bone, modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }
                        Card2(
                            modifier = Modifier.weight(1f),
                            art = {
                                Box(Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-10).dp)) {
                                    TowerMark(size = 70.dp, color = LineStrong)
                                }
                            },
                        ) {
                            MonoText("ACADEMIC YEAR", color = Muted)
                            D(general.academicYear.ifBlank { "—" }, 40.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                        }
                    }
                }
            }

            if (currentSem != null && totalSem != null) {
                item {
                    Column(Modifier.padding(top = 14.dp).entrance(5)) {
                        MonoText("PROGRESS", color = Muted)
                        Spacer(Modifier.height(6.dp))
                        SemesterTrack(current = currentSem, total = totalSem)
                    }
                }
            }

            item {
                Column(Modifier.padding(top = 14.dp).entrance(6)) {
                    KeyRow("Programme", listOf(general.program, general.branch).filter { it.isNotBlank() }.joinToString(" · "))
                    KeyRow("Institute", general.institute.ifBlank { "—" })
                }
            }

            item {
                D("CONTACT", 22.sp, color = Muted, modifier = Modifier.padding(top = 22.dp, bottom = 12.dp).entrance(7))
            }

            item {
                val email = general.collegeEmail.ifBlank { general.personalEmail }.ifBlank { "—" }
                Card2(
                    modifier = Modifier.entrance(8),
                    art = {
                        Box(Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-10).dp)) {
                            RavenMark(size = 80.dp, color = LineStrong)
                        }
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            MonoText("COLLEGE EMAIL", color = Muted)
                            MonoText(email, color = Bone, modifier = Modifier.padding(top = 4.dp))
                        }
                        if (email != "—") {
                            Box(
                                modifier = Modifier
                                    .background(Bone)
                                    .clickable {
                                        clipboard.setText(AnnotatedString(email))
                                        copied = true
                                        scope.launch {
                                            delay(1500)
                                            copied = false
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (copied) "COPIED" else "COPY",
                                    style = Mono.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
                                    color = Ink,
                                )
                            }
                        }
                    }

                    if (general.personalEmail.isNotBlank() && general.personalEmail != email) {
                        Spacer(Modifier.height(10.dp))
                        KeyRow("Personal email", general.personalEmail)
                    }
                    if (general.phone.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        KeyRow("Phone", general.phone)
                    }
                    if (general.parentPhone.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        KeyRow("Parent phone", general.parentPhone)
                    }
                }
            }

            item {
                D("PERSONAL", 22.sp, color = Muted, modifier = Modifier.padding(top = 22.dp, bottom = 12.dp))
            }

            item {
                Card2 {
                    KeyRow("Date of birth", general.dateOfBirth)
                    KeyRow("Blood group", general.bloodGroup)
                    KeyRow("Gender", general.gender)
                    KeyRow("Nationality", general.nationality)
                    KeyRow("Category", general.category)
                    KeyRow("Father", general.fatherName)
                    KeyRow("Mother", general.motherName)
                }
            }

            val permanent = address(
                general.permanentAddress1, general.permanentAddress2, general.permanentAddress3,
                general.permanentCity, general.permanentDistrict, general.permanentState, general.permanentPin,
            )
            val current = address(
                general.currentAddress1, general.currentAddress3,
                general.currentCity, general.currentDistrict, general.currentState, general.currentPin,
            )

            if (permanent.isNotBlank() || current.isNotBlank()) {
                item {
                    D("ADDRESS", 22.sp, color = Muted, modifier = Modifier.padding(top = 22.dp, bottom = 12.dp))
                }
                item {
                    Card2 {
                        if (permanent.isNotBlank()) KeyRow("Permanent", permanent)
                        if (current.isNotBlank()) KeyRow("Current", current)
                    }
                }
            }

            val qualifications = (state as? Load.Ok)?.value?.info?.qualifications.orEmpty()
            if (qualifications.isNotEmpty()) {
                item {
                    D("QUALIFICATIONS", 22.sp, color = Muted, modifier = Modifier.padding(top = 22.dp, bottom = 12.dp))
                }
                items(qualifications, key = { it.code + it.year }) { q ->
                    DetailRow(
                        title = q.board.ifBlank { q.code },
                        detail = listOf(q.code, q.year).filter { it.isNotBlank() }.joinToString(" · "),
                        trailing = q.percent.takeIf { it > 0 }?.let { "${trimDouble(it)}%" }
                            ?: q.grade.takeIf { it.isNotBlank() }
                            ?: "—",
                    )
                }
            }
        }
    }
}

@Composable
private fun FeesScreen(vm: KioskViewModel, attempt: Int, onRetry: () -> Unit, onBack: () -> Unit) {
    val state = rememberMe(vm, attempt)
    val fees = (state as? Load.Ok)?.value?.fees
    val heads = fees?.heads.orEmpty()
    val currency = heads.firstOrNull()?.currency ?: "INR"
    val due = heads.sumOf { it.due }
    val paid = heads.sumOf { it.paid }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BackRow(onBack, Modifier.entrance(0)) }
        item { D("Fees", 56.sp, modifier = Modifier.padding(top = 4.dp).entrance(1)) }
        item { FreshnessLine(state, Modifier.padding(top = 8.dp).entrance(1)) }

        if (state is Load.Loading) item { StateBox(state) }
        if (state is Load.Failed) item { StateBox(state, onRetry) }

        if (state is Load.Ok && heads.isEmpty()) {
            item {
                EmptyState("Nothing Due", "The portal's fee ledger has no entry for you.") {
                    TrooperMark(band = LineStrong, card = LineStrong, clip = LineStrong, size = 110.dp)
                }
            }
        }

        if (heads.isNotEmpty()) {
            item {
                Card2(Modifier.entrance(2)) {
                    KeyRow("Outstanding", "$currency ${money(due)}")
                    KeyRow("Paid so far", "$currency ${money(paid)}")
                }
            }
            item { MonoText("This is a reading of the portal's ledger. Payments happen on the portal itself, in a browser.", modifier = Modifier.entrance(3)) }
            item { SectionHead("Semesters", Modifier.padding(top = 6.dp).entrance(4)) }
            items(heads, key = { "${it.semester}-${it.academicYear}-${it.type}" }) { head ->
                DetailRow(
                    title = "Semester ${head.semester}${if (head.type.isNotBlank()) " · ${head.type}" else ""}",
                    detail = listOf(head.academicYear, "fee ${money(head.fee)}", "paid ${money(head.paid)}")
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    trailing = if (head.due > 0) "$currency ${money(head.due)}" else "CLEAR",
                    titleColor = if (head.due > 0) Bad else Bone,
                )
            }
        }
    }
}

@Composable
private fun HostelScreen(vm: KioskViewModel, attempt: Int, onRetry: () -> Unit, onBack: () -> Unit) {
    val state = rememberMe(vm, attempt)
    val hostel = (state as? Load.Ok)?.value?.hostel

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
    ) {
        item { BackRow(onBack, Modifier.entrance(0)) }
        item { D("Hostel", 56.sp, modifier = Modifier.padding(top = 4.dp).entrance(1)) }
        item { FreshnessLine(state, Modifier.padding(top = 8.dp, bottom = 12.dp).entrance(1)) }

        if (state is Load.Loading) item { StateBox(state) }
        if (state is Load.Failed) item { StateBox(state, onRetry) }

        if (state is Load.Ok && hostel == null) {
            item {
                EmptyState("No Allocation", "The portal has no hostel room allotted to you.", Modifier.padding(top = 8.dp)) {
                    TrooperMark(band = LineStrong, card = LineStrong, clip = LineStrong, size = 110.dp)
                }
            }
        }

        if (hostel != null) {
            item {
                Card2(Modifier.entrance(2)) {
                    KeyRow("Hostel", hostel.hostel)
                    KeyRow("Room", hostel.room)
                    KeyRow("Floor", hostel.floor)
                    KeyRow("Bed", hostel.bed)
                    KeyRow("Room type", hostel.roomType)
                    KeyRow("Hostel type", hostel.hostelType)
                    KeyRow("Allotted from", hostel.from)
                    KeyRow("Allotted till", hostel.until)
                    hostel.leftOn?.takeIf { it.isNotBlank() }?.let { KeyRow("Left on", it) }
                }
            }
        }
    }
}

/** one row of the attendance line: the number with a − and a + */
@Composable
private fun StepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoText(label.uppercase(), modifier = Modifier.weight(1f))
        StepTap("−", onMinus)
        Box(Modifier.width(52.dp), contentAlignment = Alignment.Center) {
            MonoText(value, color = Bone, medium = true)
        }
        StepTap("+", onPlus)
    }
}

@Composable
private fun StepTap(sign: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .border(2.dp, Line)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MonoText(sign, color = Bone, medium = true)
    }
}

@Composable
private fun SettingsScreen(vm: KioskViewModel, onBack: () -> Unit, onReminders: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copiedReport by remember { mutableStateOf(false) }
    var showPolicy by remember { mutableStateOf(false) }
    var askingSignOut by remember { mutableStateOf(false) }

    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()?.ifBlank { "1.0.0" } ?: "1.0.0"
    }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { BackRow(onBack, Modifier.entrance(0)) }
        item { D("Settings", 56.sp, modifier = Modifier.entrance(1)) }
        item { MonoText("UNOFFICIAL · NOT AFFILIATED WITH JIIT", modifier = Modifier.entrance(1).padding(bottom = 8.dp)) }

        // Toggles
        item {
            RowLine(
                label = "Pure black mode",
                modifier = Modifier
                    .toggleable(value = vm.pureBlack, role = Role.Switch) { vm.applyPureBlack(it) }
                    .entrance(2),
                trailing = { Toggle(vm.pureBlack) },
            )
        }

        // The attendance line: the same numbers flag subjects on the Attendance
        // tab and will mark classes on the Class schedule.
        item {
            Column(Modifier.padding(top = 18.dp).entrance(2)) {
                SectionHead("Attendance line", Modifier.padding(bottom = 8.dp))
                StepperRow(
                    label = "Safe at or above",
                    value = "${vm.attendTarget}%",
                    onMinus = { vm.applyAttendTarget(vm.attendTarget - 1) },
                    onPlus = { vm.applyAttendTarget(vm.attendTarget + 1) },
                )
                StepperRow(
                    label = "Close within",
                    value = "${vm.attendBand}%",
                    onMinus = { vm.applyAttendBand(vm.attendBand - 1) },
                    onPlus = { vm.applyAttendBand(vm.attendBand + 1) },
                )
                val low = (vm.attendTarget - vm.attendBand).coerceAtLeast(0)
                MonoText(
                    "SAFE ≥ ${vm.attendTarget}% · CLOSE $low–${vm.attendTarget - 1}% · LOW BELOW $low%",
                    modifier = Modifier.padding(top = 8.dp),
                )
                MonoText(
                    "BOTH COUNTS ON THE ATTENDANCE TAB AND THE SCHEDULE MARKS READ THIS LINE.",
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item {
            // the releases feed is the update feed: tap checks it, and once a newer
            // release is known the tap opens its page instead
            var check by remember { mutableStateOf<UpdateCheck?>(null) }
            var checking by remember { mutableStateOf(false) }
            fun runCheck() {
                checking = true
                scope.launch {
                    check = withContext(Dispatchers.IO) { Updates.check(context, version) }
                    checking = false
                }
            }
            RowLine(
                label = "Check GitHub for updates",
                modifier = Modifier
                    .clickable {
                        val state = check
                        if (checking) return@clickable
                        if (state is UpdateCheck.Newer) {
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(state.url),
                                    ),
                                )
                            }
                        } else {
                            runCheck()
                        }
                    }
                    .entrance(3),
                trailing = {
                    val state = check
                    MonoText(
                        when {
                            checking -> "CHECKING…"
                            state is UpdateCheck.Newer -> "${state.tag.uppercase()} AVAILABLE"
                            state is UpdateCheck.UpToDate -> "UP TO DATE · $version"
                            state is UpdateCheck.Problem -> "COULDN'T CHECK"
                            else -> "GITHUB"
                        },
                    )
                },
            )
            (check as? UpdateCheck.Problem)?.let {
                MonoText(it.message, modifier = Modifier.padding(vertical = 6.dp))
            }
        }

        item {
            val classroom = rememberClassroomConnect(vm)
            Column(Modifier.padding(top = 14.dp).entrance(4)) {
                SectionHead("Accounts & integrations", Modifier.padding(bottom = 4.dp))
                RowLine(
                    label = "Classroom",
                    onClick = if (classroom.connected) null else classroom.connect,
                    trailing = {
                        MonoText(
                            when {
                                classroom.connected -> "CONNECTED"
                                classroom.connecting -> "CONNECTING…"
                                else -> "CONNECT"
                            },
                        )
                    },
                )
                classroom.error?.let {
                    BodyText(it, color = Bad, modifier = Modifier.padding(vertical = 6.dp))
                }
                RowLine(
                    label = "Reminders",
                    onClick = onReminders,
                    // recomposed fresh after the Reminders page returns, so this is never stale
                    trailing = { MonoText(if (remember { ReminderPrefs(context).values().enabled }) "ON" else "OFF") },
                )
                val codePrefs = remember { CodePrefs(context) }
                var leet by remember { mutableStateOf(codePrefs.leetcode()) }
                var editingLeet by remember { mutableStateOf(false) }
                var leetDraft by remember { mutableStateOf("") }
                var gh by remember { mutableStateOf(codePrefs.github()) }
                var ghTokenSaved by remember { mutableStateOf(codePrefs.githubToken() != null) }
                var editingGh by remember { mutableStateOf(false) }
                var ghDraft by remember { mutableStateOf("") }
                var ghTokenDraft by remember { mutableStateOf("") }
                RowLine(
                    label = "LeetCode",
                    onClick = {
                        leetDraft = leet.orEmpty()
                        editingLeet = true
                    },
                    trailing = { MonoText(leet?.let { "@$it" } ?: "SET HANDLE") },
                )
                if (editingLeet) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        BasicTextField(
                            value = leetDraft,
                            onValueChange = { leetDraft = it },
                            singleLine = true,
                            textStyle = Mono.copy(color = Bone, fontSize = 15.sp),
                            cursorBrush = SolidColor(Crimson),
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            decorationBox = { inner ->
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .border(2.dp, Line)
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    if (leetDraft.isEmpty()) MonoText("USERNAME, NO @")
                                    inner()
                                }
                            },
                        )
                        Row(
                            Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PrimaryButton(
                                "Save",
                                onClick = {
                                    codePrefs.setLeetcode(leetDraft)
                                    leet = codePrefs.leetcode()
                                    editingLeet = false
                                },
                                modifier = Modifier.weight(1f),
                            )
                            Box(
                                Modifier
                                    .clickable { editingLeet = false }
                                    .padding(horizontal = 8.dp, vertical = 14.dp),
                            ) {
                                MonoText("CANCEL")
                            }
                        }
                    }
                }
                RowLine(
                    label = "GitHub",
                    onClick = {
                        ghDraft = gh.orEmpty()
                        ghTokenDraft = ""
                        editingGh = true
                    },
                    trailing = { MonoText(gh?.let { "@$it" } ?: "SET HANDLE") },
                )
                if (editingGh) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        BasicTextField(
                            value = ghDraft,
                            onValueChange = { ghDraft = it },
                            singleLine = true,
                            textStyle = Mono.copy(color = Bone, fontSize = 15.sp),
                            cursorBrush = SolidColor(Crimson),
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            decorationBox = { inner ->
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .border(2.dp, Line)
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    if (ghDraft.isEmpty()) MonoText("USERNAME, NO @")
                                    inner()
                                }
                            },
                        )
                        BasicTextField(
                            value = ghTokenDraft,
                            onValueChange = { ghTokenDraft = it },
                            singleLine = true,
                            textStyle = Mono.copy(color = Bone, fontSize = 15.sp),
                            cursorBrush = SolidColor(Crimson),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(44.dp),
                            decorationBox = { inner ->
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .border(2.dp, Line)
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    if (ghTokenDraft.isEmpty()) {
                                        MonoText(if (ghTokenSaved) "TOKEN SAVED — TYPE TO REPLACE" else "TOKEN, OPTIONAL")
                                    }
                                    inner()
                                }
                            },
                        )
                        BodyText(
                            "The token stays sealed in this phone's Keystore and lifts GitHub's rate limit. " +
                                "Without it the board shows PUBLIC ACTIVITY ONLY.",
                            color = Muted,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Row(
                            Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PrimaryButton(
                                "Save",
                                onClick = {
                                    codePrefs.setGithub(ghDraft)
                                    if (ghTokenDraft.isNotBlank()) codePrefs.setGithubToken(ghTokenDraft)
                                    gh = codePrefs.github()
                                    ghTokenSaved = codePrefs.githubToken() != null
                                    editingGh = false
                                },
                                modifier = Modifier.weight(1f),
                            )
                            Box(
                                Modifier
                                    .clickable { editingGh = false }
                                    .padding(horizontal = 8.dp, vertical = 14.dp),
                            ) {
                                MonoText("CANCEL")
                            }
                        }
                        if (ghTokenSaved) {
                            Box(
                                Modifier
                                    .clickable {
                                        codePrefs.setGithubToken(null)
                                        ghTokenSaved = false
                                    }
                                    .padding(vertical = 10.dp),
                            ) {
                                MonoText("REMOVE SAVED TOKEN", color = Bad)
                            }
                        }
                    }
                }
            }
        }

        // Spinning vinyl JPortal block
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp)
                    .entrance(5),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val spin by rememberSpin(active = false, idle = true)
                Box(Modifier.graphicsLayer { rotationZ = spin }) {
                    VinylMark(label = Crimson, groove = LineStrong, disc = MarkCore, size = 64.dp)
                }
                Column {
                    BodyText("JPortal", size = 16.sp)
                    MonoText("VERSION $version", modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        // Links
        item {
            RowLine(
                label = "Copy debug report",
                modifier = Modifier
                    .clickable {
                        val report = "JPortal $version\nAndroid: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\nModel: ${android.os.Build.MODEL}"
                        clipboard.setText(AnnotatedString(report))
                        copiedReport = true
                        scope.launch {
                            delay(1500)
                            copiedReport = false
                        }
                    }
                    .entrance(6),
                trailing = {
                    MonoText(if (copiedReport) "COPIED" else "NO TOKENS, NAMES OR IDS")
                },
            )
        }

        item {
            Column(Modifier.entrance(7)) {
                NavRow(
                    label = "Privacy policy",
                    detail = if (showPolicy) "HIDE" else "SHOW",
                ) { showPolicy = !showPolicy }
                if (showPolicy) {
                    MonoText(
                        "This app talks only to webportal.jiit.ac.in. The session token is sealed " +
                            "with this phone's keystore and cached portal data sits in the app's own files. " +
                            "No analytics, no third-party servers. Signing out removes the session; " +
                            "the sign-out sheet can clear the cached data too.",
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        item {
            RowLine(
                label = "Licences",
                modifier = Modifier.entrance(8),
                trailing = { MonoText("GPL-3.0") },
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
            if (!askingSignOut) {
                PrimaryButton("Sign out", onClick = { askingSignOut = true }, modifier = Modifier.entrance(9))
            } else {
                Column(Modifier.entrance(9), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonoText("SIGN OUT?")
                    BodyText("Cached portal data stays on this phone unless you clear it as well. Clearing also unlinks Google Classroom.")
                    PrimaryButton("Sign out", onClick = { vm.signOut() })
                    RowLine(label = "Sign out and clear cached data", onClick = { vm.signOut(clearCache = true) })
                    RowLine(label = "Cancel", onClick = { askingSignOut = false })
                }
            }
        }
    }
}

@Composable
private fun FeedbackScreen(vm: KioskViewModel, onBack: () -> Unit) {
    var attempt by remember { mutableStateOf(0) }
    var open by remember { mutableStateOf<FeedbackEvent?>(null) }
    val token = vm.session?.token.orEmpty()

    BackHandler(enabled = open != null) { open = null; attempt++ }

    val event = open
    if (event != null) {
        FeedbackRows(vm, event, onBack = { open = null; attempt++ })
        return
    }

    val state = rememberCached(vm, "feedback/$token", FEEDBACK_EVENTS, attempt) { it.feedbackEvents() }
    val events = (state as? Load.Ok)?.value.orEmpty()

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BackRow(onBack, Modifier.entrance(0)) }
        item { D("Feedback", 56.sp, modifier = Modifier.padding(top = 4.dp).entrance(1)) }
        item { FreshnessLine(state, Modifier.padding(top = 8.dp).entrance(1)) }

        when {
            state is Load.Loading -> item { StateBox(state) }
            state is Load.Failed -> item { StateBox(state) { attempt++ } }
            events.isEmpty() -> item {
                EmptyState("Nothing to Rate", "The portal has no feedback form open for you right now.") {
                    TrooperMark(band = LineStrong, card = LineStrong, clip = LineStrong, size = 110.dp)
                }
            }
            else -> {
                item { MonoText("The portal opens these for a few days each semester. Rating all of them is what closes it.", modifier = Modifier.entrance(2)) }
                events.forEach { e ->
                    item { NavRow(e.description.ifBlank { e.code }, onClick = { open = e }) }
                }
            }
        }
    }
}

/** one open event: every teacher still unrated, each with the portal's five answers */
@Composable
private fun FeedbackRows(vm: KioskViewModel, event: FeedbackEvent, onBack: () -> Unit) {
    val token = vm.session?.token.orEmpty()
    val scope = rememberCoroutineScope()
    var attempt by remember { mutableStateOf(0) }
    var picked by remember { mutableStateOf<Map<String, Rating>>(emptyMap()) }
    var sending by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf<Set<String>>(emptySet()) }

    val state = rememberCached(
        vm,
        key = "fb/$token/${event.id}",
        ser = JSON_ROWS,
        attempt = attempt,
    ) { portal -> portal.feedbackGrid(event).map { it.raw } }

    val rows = state.let { if (it is Load.Ok) it.value.map(::FeedbackRow) else emptyList() }
        .filterNot { it.key in sent }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { BackRow(onBack, Modifier.entrance(0)) }
        item { D(event.description.ifBlank { "Feedback" }, 40.sp, maxLines = 3, modifier = Modifier.padding(top = 4.dp).entrance(1)) }
        item { FreshnessLine(state, Modifier.padding(top = 8.dp).entrance(1)) }
        // the mockup carried a sample tally; what the numbers on the buttons
        // actually mean is the honest thing to print here
        item {
            MonoText(
                Rating.entries.joinToString(" · ") { ratingLabel(it) },
                modifier = Modifier.entrance(2),
            )
        }

        when {
            state is Load.Loading -> item { StateBox(state) }
            state is Load.Failed -> item { StateBox(state) { attempt++ } }
            rows.isEmpty() -> item {
                EmptyState("All Rated", "Every teacher in this event has been rated. Thank you.") {
                    TrooperMark(band = LineStrong, card = LineStrong, clip = LineStrong, size = 110.dp)
                }
            }
            else -> items(rows, key = { it.key }) { row ->
                Column(Modifier.padding(top = 6.dp)) {
                    BodyText(row.subjectName.ifBlank { row.subjectCode }, maxLines = 2)
                    MonoText(
                        listOf(row.facultyName, row.component, row.subjectCode)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Rating.entries.forEachIndexed { i, rating ->
                            val on = picked[row.key] == rating
                            Text(
                                "${i + 1}",
                                style = Mono.copy(fontWeight = FontWeight.Medium),
                                color = Bone,
                                modifier = Modifier
                                    .clickable { picked = picked + (row.key to rating) }
                                    .background(if (on) Crimson else Line)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                    val chosen = picked[row.key]
                    if (chosen != null) {
                        val busy = sending == row.key
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .background(Bone)
                                .clickable(enabled = !busy) {
                                    sending = row.key
                                    error = null
                                    scope.launch {
                                        runCatching {
                                            vm.call { portal -> portal.submitFeedback(event, row, chosen) }
                                        }
                                            .onSuccess { sent = sent + row.key; picked = picked - row.key }
                                            .onFailure { error = friendly(it) }
                                        sending = null
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (busy) "SENDING…" else "SUBMIT ${ratingLabel(chosen)}",
                                style = Mono.copy(fontWeight = FontWeight.Medium),
                                color = Ink,
                            )
                        }
                    }
                }
            }
        }

        error?.let {
            item { Card2 { MonoText("NOT SENT", color = Bad, medium = true); Spacer(Modifier.height(6.dp)); BodyText(it, color = Muted) } }
        }
        if (sent.isNotEmpty() && rows.isNotEmpty()) {
            item { MonoText("${sent.size} RATED · NOTHING IS SENT UNTIL YOU PRESS SUBMIT") }
        }
    }
}

private fun ratingLabel(rating: Rating): String = when (rating) {
    Rating.UNSATISFIED -> "1 · UNSATISFIED"
    Rating.SATISFIED -> "2 · SATISFIED"
    Rating.GOOD -> "3 · GOOD"
    Rating.VERY_GOOD -> "4 · VERY GOOD"
    Rating.EXCELLENT -> "5 · EXCELLENT"
}

private fun address(vararg parts: String): String =
    parts.filter { it.isNotBlank() }.joinToString(", ")

private fun money(value: Double): String =
    if (value % 1.0 == 0.0) String.format(Locale.ENGLISH, "%,d", value.toLong())
    else String.format(Locale.ENGLISH, "%,.2f", value)

private fun trimDouble(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
