package com.jiitkiosk.app.ui.classroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.calendar.CalInsert
import com.jiitkiosk.app.classroom.ClassroomClient
import com.jiitkiosk.app.classroom.ClassroomData
import com.jiitkiosk.app.classroom.Work
import com.jiitkiosk.app.data.HttpError
import com.jiitkiosk.app.reminders.ReminderPlanner
import com.jiitkiosk.app.reminders.ReminderPrefs
import com.jiitkiosk.app.reminders.ReminderSync
import com.jiitkiosk.app.reminders.asReminderWork
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.Card2
import com.jiitkiosk.app.ui.components.ChipRow
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.EmptyState
import com.jiitkiosk.app.ui.components.FreshnessLine
import com.jiitkiosk.app.ui.components.HeroPanel
import com.jiitkiosk.app.ui.components.Load
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.PrimaryButton
import com.jiitkiosk.app.ui.components.ScreenTitle
import com.jiitkiosk.app.ui.components.SegmentedSwitch
import com.jiitkiosk.app.ui.components.SkeletonScreen
import com.jiitkiosk.app.ui.components.Stamp
import com.jiitkiosk.app.ui.components.rememberKept
import com.jiitkiosk.app.ui.entrance
import com.jiitkiosk.app.ui.tap
import com.jiitkiosk.app.ui.theme.Bad
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.CrimsonDeep
import com.jiitkiosk.app.ui.theme.DossierMark
import com.jiitkiosk.app.ui.theme.Ink
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.Muted
import com.jiitkiosk.app.ui.theme.PlanetMark
import com.jiitkiosk.app.ui.theme.TomeMark
import com.jiitkiosk.app.ui.theme.Violet
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val DUE_DAY = DateTimeFormatter.ofPattern("EEE dd MMM", Locale.ENGLISH)
private val DUE_TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private const val SOON_MS = 72L * 3_600_000

/**
 * The Class tab: what Google Classroom says is coming, and nothing else. The
 * first frame before connecting is honest about being empty, and every failure
 * that needs a human (an expired authorization) leads with the one button that
 * fixes it.
 */
@Composable
fun ClassroomScreen(vm: KioskViewModel, onReminders: () -> Unit) {
    val connect = rememberClassroomConnect(vm)
    val context = LocalContext.current
    var attempt by remember { mutableIntStateOf(0) }
    var filter by remember { mutableIntStateOf(0) }
    var pane by rememberSaveable { mutableIntStateOf(0) }

    val state = rememberKept(
        vm,
        ClassroomData.CACHE_KEY,
        ClassroomData.serializer(),
        attempt = attempt + connect.version,
        enabled = connect.connected,
    ) {
        val token = vm.classroom.token() ?: throw IllegalStateException("Classroom is not connected.")
        // the client is plain blocking HTTP; it must never run on the UI thread
        withContext(Dispatchers.IO) { ClassroomClient(token).fetch() }
    }
    val now = remember(state) { System.currentTimeMillis() }

    // the access token lives about an hour: when it has run out, renew it
    // silently once instead of parking the user on a Reconnect button
    val failure = (state as? Load.Failed)?.error
    val authDead = failure is HttpError && (failure.code == 401 || failure.code == 403)
    var autoTried by remember { mutableStateOf(false) }
    LaunchedEffect(authDead) {
        if (authDead && !autoTried && !connect.connecting) {
            autoTried = true
            connect.connect()
        } else if (!authDead) {
            autoTried = false
        }
    }

    // a fresh fetch is also what stops the reminders for work that just got submitted
    val fresh = (state as? Load.Ok)?.value
    LaunchedEffect(fresh) {
        if (fresh != null) withContext(Dispatchers.IO) { ReminderSync.refresh(context, fresh) }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
            ScreenTitle("Classroom", modifier = Modifier.entrance(0))
            (state as? Load.Ok)?.let { ok ->
                FreshnessLine(
                    ok.at,
                    ok.stale,
                    refreshing = ok.refreshing,
                    extra = "GOOGLE CLASSROOM",
                    modifier = Modifier.entrance(1),
                )
            }
            SegmentedSwitch(
                listOf("Assignments", "Schedule", "Notes"),
                pane,
                Modifier.entrance(2).padding(top = 12.dp, bottom = 12.dp),
            ) { pane = it }
        }
        when (pane) {
            1 -> SchedulePane(vm)
            2 -> NotesPane(vm, state, connect, onRetry = { attempt++ })
            else -> AssignmentsPane(connect, state, now, filter, onReminders, onRetry = { attempt++ }, onFilter = { filter = it })
        }
    }
}

@Composable
private fun AssignmentsPane(
    connect: ClassroomConnect,
    state: Load<ClassroomData>,
    now: Long,
    filter: Int,
    onReminders: () -> Unit,
    onRetry: () -> Unit,
    onFilter: (Int) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!connect.connected) {
            item {
                EmptyState(
                    "Not Connected",
                    "Coursework and deadlines from Google Classroom show up here. The app reads them; it never writes.",
                    Modifier.entrance(1),
                ) { TomeMark(size = 110.dp) }
            }
            item {
                PrimaryButton(
                    if (connect.connecting) "Connecting…" else "Connect Classroom",
                    onClick = connect.connect,
                    enabled = !connect.connecting,
                    modifier = Modifier.entrance(2),
                )
            }
            item {
                MonoText(
                    "Three read-only permissions: courses, coursework, and your own submissions.",
                    modifier = Modifier.entrance(3),
                )
            }
            connect.error?.let { item { MonoText(it, color = Bad, modifier = Modifier.entrance(3)) } }
            return@LazyColumn
        }

        val failure = (state as? Load.Failed)?.error
        val authDead = failure is HttpError && (failure.code == 401 || failure.code == 403)

        when {
            authDead -> {
                item {
                    EmptyState(
                        "Reconnect Needed",
                        "Google has retired this Classroom authorization. Connecting again is one tap, and keeps your reminder settings.",
                        Modifier.entrance(1),
                    ) { TomeMark(size = 110.dp) }
                }
                item {
                    PrimaryButton(
                        if (connect.connecting) "Connecting…" else "Reconnect",
                        onClick = connect.connect,
                        enabled = !connect.connecting,
                        modifier = Modifier.entrance(2),
                    )
                }
                connect.error?.let { item { MonoText(it, color = Bad, modifier = Modifier.entrance(3)) } }
            }

            state is Load.Loading -> item { SkeletonScreen() }

            state is Load.Failed -> item {
                Card2(Modifier.entrance(1)) {
                    MonoText("COULDN'T LOAD", color = Bad, medium = true)
                    Spacer(Modifier.height(6.dp))
                    BodyText(classroomFriendly(state.error), color = Muted)
                    Spacer(Modifier.height(12.dp))
                    MonoText(
                        "RETRY",
                        color = Ink,
                        medium = true,
                        modifier = Modifier
                            .background(Bone)
                            .tap { onRetry() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            state is Load.Ok -> {
                val data = state.value
                if (data.work.isEmpty()) {
                    item {
                        EmptyState(
                            "No Coursework",
                            "Nothing is published in your active courses for the two months ahead.",
                            Modifier.entrance(2),
                        ) { DossierMark(size = 110.dp) }
                    }
                } else {
                    item { ClassroomHero(data, now, Modifier.entrance(2)) }
                    item {
                        ChipRow(listOf("All", "Missing", "Due soon", "Done"), filter, Modifier.entrance(3)) { onFilter(it) }
                    }
                    val visible = data.work.filter { filterMatch(it, filter, now) }
                    if (visible.isEmpty()) {
                        item { MonoText("Nothing in this filter.", modifier = Modifier.entrance(4)) }
                    }
                    items(visible, key = { "${it.courseId}/${it.id}" }) { work ->
                        WorkCard(work, now, Modifier.entrance(4))
                    }
                }
                item {
                    Row(
                        Modifier.entrance(5).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MonoText("Refreshes when this tab opens.", modifier = Modifier.weight(1f))
                        // the brief wants reminders reachable from here, not only Settings
                        MonoText(
                            "REMINDERS",
                            color = Crimson,
                            medium = true,
                            modifier = Modifier
                                .background(Line)
                                .tap { onReminders() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

/** the crimson hero: how much is unsubmitted, and the next deadline running down */
@Composable
private fun ClassroomHero(data: ClassroomData, now: Long, modifier: Modifier = Modifier) {
    val next = data.work
        .filter { it.submitted == false && it.dueAt != null && it.dueAt > now }
        .minByOrNull { it.dueAt!! }
    HeroPanel(
        height = 168.dp,
        angle = 160f,
        stops = arrayOf(0f to Crimson, 0.7f to CrimsonDeep),
        modifier = modifier,
        art = {
            // the violet block runs off the right edge, as on the board
            Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(150.dp).background(Violet)) {
                Box(Modifier.align(Alignment.TopEnd).offset(x = 30.dp, y = (-18).dp)) {
                    PlanetMark(planet = Violet, ring = Bone, speck = Violet, detail = Ink, size = 150.dp)
                }
            }
        },
    ) {
        Column(Modifier.align(Alignment.CenterStart).padding(start = 14.dp)) {
            Stamp("Unsubmitted")
            Spacer(Modifier.height(10.dp))
            D("${data.unsubmitted}", 64.sp, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            MonoText(
                next?.let { "NEXT DUE IN ${countdown(it.dueAt!! - now)}" } ?: "NOTHING DUE",
                color = Bone,
            )
        }
    }
}

@Composable
private fun WorkCard(work: Work, now: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var noCal by remember { mutableStateOf(false) }
    val missing = work.submitted == false && work.dueAt != null && work.dueAt < now
    val soon = work.submitted == false && work.dueAt != null && work.dueAt >= now && work.dueAt - now <= SOON_MS
    val reminders = remember { ReminderPrefs(context).values() }
    Card2(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoText(work.courseName.uppercase(), modifier = Modifier.weight(1f), medium = true)
            when {
                work.submitted == true -> Stamp("Submitted")
                missing -> Stamp("Missing", background = Crimson, color = Bone)
                soon -> Stamp("Due soon", background = Line, color = Muted)
                work.submitted == null -> Stamp("Unknown", background = Line, color = Muted)
            }
        }
        Spacer(Modifier.height(8.dp))
        BodyText(work.title, size = 18.sp, maxLines = 2)
        Spacer(Modifier.height(6.dp))
        MonoText(dueText(work))
        Spacer(Modifier.height(10.dp))
        // board 09: the fill is how much of the warning window has run out
        val window = work.dueAt
        val fill = when {
            missing -> 1f
            // soon already implies a deadline; this is the compiler's own proof
            soon -> 1f - (window - now).toFloat() / SOON_MS
            else -> 0f
        }
        Box(Modifier.fillMaxWidth().height(8.dp).background(Line)) {
            Box(
                Modifier
                    .fillMaxWidth(fill.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(if (missing) Crimson else Bone),
            )
        }
        Spacer(Modifier.height(6.dp))
        val line = reminderLine(work, reminders, now)
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoText(
                line ?: statusText(work, now),
                color = if (line == null && missing) Bad else Muted,
                modifier = Modifier.weight(1f),
            )
            val dueAt = work.dueAt
            if (dueAt != null && work.submitted != true) {
                MonoText(
                    "ADD TO CALENDAR",
                    color = Crimson,
                    modifier = Modifier.tap {
                        // a due day without a time becomes an all-day entry in the
                        // same zone the card prints it in
                        val took = if (work.dueHasTime) {
                            CalInsert.moment(context, work.title, dueAt, work.courseName)
                        } else {
                            val day = Instant.ofEpochMilli(dueAt).atZone(ZoneId.systemDefault()).toLocalDate()
                            CalInsert.span(context, work.title, day, day, work.courseName)
                        }
                        noCal = !took
                    },
                )
            }
        }
        if (noCal) {
            Spacer(Modifier.height(6.dp))
            MonoText("NO CALENDAR APP TOOK THE INTENT", color = Bad)
        }
    }
}

private fun filterMatch(work: Work, filter: Int, now: Long): Boolean = when (filter) {
    0 -> true
    1 -> work.submitted == false && work.dueAt != null && work.dueAt < now
    2 -> work.submitted == false && work.dueAt != null && work.dueAt >= now && work.dueAt - now <= SOON_MS
    else -> work.submitted == true
}

private fun dueText(work: Work): String {
    val at = work.dueAt ?: return "NO DEADLINE"
    val z = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())
    val day = z.format(DUE_DAY).uppercase(Locale.ENGLISH)
    return if (work.dueHasTime) "DUE $day · ${z.format(DUE_TIME)}" else "DUE $day"
}

private fun statusText(work: Work, now: Long): String = when {
    work.submitted == true -> {
        val at = work.submittedAt
        if (at == null) "SUBMITTED"
        else "SUBMITTED ${Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(DUE_DAY).uppercase(Locale.ENGLISH)}"
    }
    work.submitted == null -> "SUBMISSION STATE UNKNOWN · REFRESH TO CHECK"
    work.dueAt != null && work.dueAt < now -> "MISSING · NOT SUBMITTED"
    else -> "NOT SUBMITTED"
}

/** board 09's third line: the reminder story when there is one, else the raw status */
private fun reminderLine(work: Work, values: ReminderPrefs.Values, now: Long): String? {
    val (next, last) = ReminderPlanner.timeline(work.asReminderWork(), values, now, ZoneId.systemDefault())
    return when {
        next != null -> "NEXT REMINDER IN ${spanText(next - now, ahead = true)}"
        last != null -> "REMINDER SENT ${spanText(now - last, ahead = false)} AGO"
        else -> null
    }
}

/** [n]H per the board; sub-hour falls to minutes, and what it awaits is rounded up, never down */
private fun spanText(ms: Long, ahead: Boolean): String {
    val minutes = ms / 60_000
    if (minutes < 60) return "${minutes.coerceAtLeast(1)}M"
    val whole = minutes / 60
    return "${if (ahead && minutes % 60 != 0L) whole + 1 else whole}H"
}

private fun countdown(ms: Long): String {
    val h = ms / 3_600_000
    val m = (ms % 3_600_000) / 60_000
    return when {
        h >= 48 -> "${h / 24}D ${h % 24}H"
        h >= 1 -> "${h}H ${m}M"
        else -> "${m}M"
    }
}
