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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.ui.KioskViewModel
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
import `in`.codelif.ktjiit.model.HostelInfo
import `in`.codelif.ktjiit.model.PersonalInfo
import `in`.codelif.ktjiit.model.Rating
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
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

private enum class MePage { Overview, Profile, Fees, Hostel, Settings, Feedback }

@Composable
fun MeScreen(vm: KioskViewModel) {
    var page by remember { mutableStateOf(MePage.Overview) }
    // one counter for the whole tab: every Me screen reads the same cached record
    var attempt by remember { mutableStateOf(0) }
    val retry: () -> Unit = { attempt++ }

    // the system back button goes up a level before it leaves the app
    BackHandler(enabled = page != MePage.Overview) { page = MePage.Overview }

    when (page) {
        MePage.Overview -> MeOverview(vm, attempt, retry) { page = it }
        MePage.Profile -> ProfileScreen(vm, attempt, retry) { page = MePage.Overview }
        MePage.Fees -> FeesScreen(vm, attempt, retry) { page = MePage.Overview }
        MePage.Hostel -> HostelScreen(vm, attempt, retry) { page = MePage.Overview }
        MePage.Settings -> SettingsScreen(vm) { page = MePage.Overview }
        MePage.Feedback -> FeedbackScreen(vm) { page = MePage.Overview }
    }
}

@Composable
private fun MeOverview(vm: KioskViewModel, attempt: Int, onRetry: () -> Unit, open: (MePage) -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    val state = rememberMe(vm, attempt)
    val data = (state as? Load.Ok)?.value
    val general = data?.info?.general
    val list = rememberLazyListState()
    val titleCollapse = (list.scrollDepth() / 100f).coerceIn(0f, 1f)

    Box(Modifier.fillMaxWidth()) {
        LazyColumn(
            Modifier.fillMaxWidth(),
            state = list,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp).entrance(0),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarTile(96.dp)
                    Column(Modifier.weight(1f)) {
                        D(
                            general?.name?.takeIf { it.isNotBlank() } ?: vm.session?.username.orEmpty(),
                            40.sp,
                            maxLines = 2,
                        )
                        MonoText(
                            listOf(general?.enrollmentNo, general?.program, general?.branch)
                                .filter { !it.isNullOrBlank() }
                                .joinToString(" · ")
                                .ifBlank { "SIGNED IN" },
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            if (state is Load.Ok) {
                item {
                    FreshnessLine(
                        state.at,
                        state.stale,
                        state.refreshing,
                        Modifier.padding(bottom = 10.dp).entrance(1),
                    )
                }
            }

            item { NavRow("Profile", Modifier.entrance(2)) { open(MePage.Profile) } }
            item { NavRow("Fees", Modifier.entrance(3)) { open(MePage.Fees) } }
            item { NavRow("Hostel", Modifier.entrance(4)) { open(MePage.Hostel) } }

            item {
                NavRow(
                    label = "Bank details",
                    modifier = Modifier.entrance(5),
                    onClick = { revealed = !revealed },
                    detail = if (revealed) "HIDE" else "•••• •••• — TAP TO REVEAL",
                )
            }

            if (revealed) {
                item {
                    val bank = data?.bank
                    Card2(Modifier.padding(top = 10.dp)) {
                        KeyRow("Bank", bank?.bank.orEmpty())
                        KeyRow("Account", bank?.account.orEmpty())
                        KeyRow("IFSC", bank?.ifsc.orEmpty())
                        KeyRow("Holder", bank?.holder.orEmpty())
                        if (bank?.frozen?.isNotBlank() == true) KeyRow("Frozen", bank.frozen)
                    }
                }
            }

            item { NavRow("Feedback", Modifier.entrance(6)) { open(MePage.Feedback) } }
            item { NavRow("Settings & About", Modifier.entrance(7)) { open(MePage.Settings) } }

            if (state is Load.Failed) {
                item { StateBox(state, onRetry) }
            }
        }

        // compact name once the avatar header has scrolled away
        if (titleCollapse > 0f) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .background(Ink)
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp)
                    .graphicsLayer { alpha = titleCollapse },
            ) {
                D(
                    general?.name?.takeIf { it.isNotBlank() } ?: vm.session?.username.orEmpty(),
                    22.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

/** the trooper tile the design puts at the top of this tab */
@Composable
private fun AvatarTile(size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clipToBounds().background(Steel).border(2.dp, Bone)) {
        Box(Modifier.matchParentSize().halftone())
        TrooperMark(band = Violet, card = Bone, clip = Crimson, size = size)
    }
}

@Composable
private fun ProfileScreen(vm: KioskViewModel, attempt: Int, onRetry: () -> Unit, onBack: () -> Unit) {
    val state = rememberMe(vm, attempt)
    val general = (state as? Load.Ok)?.value?.info?.general

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
            item { SectionHead("Student", Modifier.padding(top = 10.dp, bottom = 4.dp).entrance(2)) }
            item {
                Card2(Modifier.entrance(3)) {
                    KeyRow("Name", general.name)
                    KeyRow("Enrolment", general.enrollmentNo)
                    KeyRow("Programme", listOf(general.program, general.branch).filter { it.isNotBlank() }.joinToString(" · "))
                    KeyRow("Batch", general.batch)
                    KeyRow("Section", general.section)
                    KeyRow("Semester", general.semester.takeIf { it > 0 }?.toString().orEmpty())
                    KeyRow("Academic year", general.academicYear)
                    KeyRow("Institute", general.institute)
                }
            }

            item { SectionHead("Contact", Modifier.padding(top = 16.dp, bottom = 4.dp).entrance(4)) }
            item {
                Card2(Modifier.entrance(5)) {
                    KeyRow("College email", general.collegeEmail)
                    KeyRow("Personal email", general.personalEmail)
                    KeyRow("Phone", general.phone)
                    KeyRow("Parent phone", general.parentPhone)
                }
            }

            item { SectionHead("Personal", Modifier.padding(top = 16.dp, bottom = 4.dp).entrance(6)) }
            item {
                Card2(Modifier.entrance(7)) {
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

            item { SectionHead("Address", Modifier.padding(top = 16.dp, bottom = 4.dp)) }
            item {
                Card2 {
                    KeyRow("Permanent", permanent)
                    KeyRow("Current", current)
                }
            }

            val qualifications = (state as? Load.Ok)?.value?.info?.qualifications.orEmpty()
            if (qualifications.isNotEmpty()) {
                item { SectionHead("Qualifications", Modifier.padding(top = 16.dp)) }
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

@Composable
private fun SettingsScreen(vm: KioskViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty()
    }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BackRow(onBack, Modifier.entrance(0)) }
        item { D("Settings", 56.sp, modifier = Modifier.entrance(1)) }
        item { MonoText("UNOFFICIAL · NOT AFFILIATED WITH JIIT", modifier = Modifier.entrance(1)) }

        item {
            Card2(Modifier.entrance(2)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        // the whole row is the switch, so the target is never 44dp of switch
                        .toggleable(value = vm.pureBlack, role = Role.Switch) { vm.applyPureBlack(it) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        BodyText("Pure black")
                        MonoText("Paints the page true black instead of near black.")
                    }
                    Toggle(vm.pureBlack)
                }
            }
        }

        item {
            Card2(Modifier.entrance(3)) {
                MonoText("LICENCES", color = Bone, medium = true)
                Spacer(Modifier.height(8.dp))
                BodyText("This app is GPL-3.0. It uses codelif's ktjiit portal client (GPL-3.0) and the OFL-licensed Barlow Condensed and IBM Plex families.", color = Muted)
            }
        }

        item {
            Card2(Modifier.entrance(4)) {
                MonoText("SESSION", color = Bone, medium = true)
                Spacer(Modifier.height(8.dp))
                BodyText("Your portal token lives on this phone only. Signing out deletes it along with everything cached by this app.", color = Muted)
            }
        }

        item {
            PrimaryButton("Sign out", onClick = { vm.signOut() }, modifier = Modifier.padding(top = 4.dp).entrance(5))
        }

        if (version.isNotBlank()) {
            item { MonoText("VERSION $version", modifier = Modifier.entrance(6)) }
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
        item { MonoText("1 UNSATISFIED · 3 GOOD · 5 EXCELLENT", modifier = Modifier.entrance(2)) }

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
