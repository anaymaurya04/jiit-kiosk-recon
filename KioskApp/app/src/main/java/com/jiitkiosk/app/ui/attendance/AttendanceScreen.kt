package com.jiitkiosk.app.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.domain.AttendanceMath
import com.jiitkiosk.app.domain.Tally
import com.jiitkiosk.app.ui.BackLayer
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.Motion
import com.jiitkiosk.app.ui.PageFlip
import com.jiitkiosk.app.ui.artIn
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.ChipRow
import com.jiitkiosk.app.ui.components.CollapsingTitle
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.EmptyState
import com.jiitkiosk.app.ui.components.FreshnessLine
import com.jiitkiosk.app.ui.components.HeroPanel
import com.jiitkiosk.app.ui.components.Load
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.RefreshBox
import com.jiitkiosk.app.ui.components.Stamp
import com.jiitkiosk.app.ui.components.StateBox
import com.jiitkiosk.app.ui.components.rememberCached
import com.jiitkiosk.app.ui.components.scrollDepth
import com.jiitkiosk.app.ui.entrance
import com.jiitkiosk.app.ui.parallax
import com.jiitkiosk.app.ui.tap
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.CrimsonDeep
import com.jiitkiosk.app.ui.theme.HourglassMark
import com.jiitkiosk.app.ui.theme.Ink
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.LineStrong
import com.jiitkiosk.app.ui.theme.Ring
import com.jiitkiosk.app.ui.theme.SentinelMark
import com.jiitkiosk.app.ui.theme.ShieldMark
import com.jiitkiosk.app.ui.theme.Surface1
import com.jiitkiosk.app.ui.theme.TowerMark
import com.jiitkiosk.app.ui.theme.Verdict
import com.jiitkiosk.app.ui.theme.verdictOf
import com.jiitkiosk.app.widget.KioskWidget
import `in`.codelif.ktjiit.model.AttendanceDetail
import `in`.codelif.ktjiit.model.AttendanceMeta
import `in`.codelif.ktjiit.model.DailyAttendance
import `in`.codelif.ktjiit.model.Semester
import `in`.codelif.ktjiit.model.SubjectAttendance
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * One subject with its class-by-class list. The portal's attendance table only
 * carries percentages, so the counts and the calendar come from the per-subject
 * call; fetching them together keeps one cache entry and one freshness time.
 */
@Serializable
data class Sheet(val detail: AttendanceDetail, val daily: Map<String, DailyAttendance> = emptyMap()) {
    fun tallyOf(subject: SubjectAttendance): Tally = AttendanceMath.tally(daily[subject.subjectId]?.classes.orEmpty())
}

@Composable
fun rememberSheet(vm: KioskViewModel, semester: Semester?, attempt: Int): Load<Sheet> {
    val token = vm.session?.token.orEmpty()
    return rememberCached(
        vm,
        key = "att/$token/${semester?.id.orEmpty()}",
        ser = Sheet.serializer(),
        attempt = attempt,
        enabled = semester != null,
    ) { portal ->
        val term = requireNotNull(semester)
        val detail = portal.attendance(term, portal.attendanceMeta().header?.semesterNumber.orEmpty())
        val daily = coroutineScope {
            detail.subjects
                .map { s -> async { s.subjectId to portal.dailyAttendance(term, s) } }
                .map { it.await() }
                .toMap()
        }
        Sheet(detail, daily)
    }
}

@Composable
fun AttendanceScreen(vm: KioskViewModel) {
    // A refresh must survive the recomposition it triggers; otherwise its new
    // cache attempt is immediately lost and the request is not re-issued.
    var attempt by remember { mutableStateOf(0) }
    var semIndex by rememberSaveable { mutableStateOf(0) }
    // the open subject, by id: the sheet behind it is read live, so a refresh
    // updates the page the user is on instead of freezing what it looked like
    var openId by rememberSaveable { mutableStateOf<String?>(null) }

    val token = vm.session?.token.orEmpty()
    val meta = rememberCached(vm, "att-meta/$token", AttendanceMeta.serializer(), attempt) { it.attendanceMeta() }
    val semesters = (meta as? Load.Ok)?.value?.semesters.orEmpty()
    val semester = semesters.getOrNull(semIndex)
    val sheet = rememberSheet(vm, semester, attempt)

    val sheetOk = sheet as? Load.Ok
    val stamp = sheetOk ?: (meta as? Load.Ok)

    // the widget's attendance half reads this same cache: any load, cached or
    // fresh, means the home screen has something new to show
    val context = LocalContext.current
    LaunchedEffect(sheetOk?.at) { if (sheetOk != null) KioskWidget.refresh(context) }

    // the system back goes up a level before it leaves the app, and it does so
    // while following the gesture
    BackLayer(enabled = openId != null, onBack = { openId = null }) {
        PageFlip(target = openId, forward = openId != null) { id ->
            val subject = id?.let { sid ->
                sheetOk?.value?.detail?.subjects?.firstOrNull { it.subjectId == sid }
            }
            if (id != null && subject != null && sheetOk != null) {
                SubjectScreen(
                    vm = vm,
                    subject = subject,
                    sheet = sheetOk.value,
                    semester = semester,
                    attempt = attempt,
                    onBack = { openId = null },
                )
            } else {
                AttendanceList(
                    stamp = stamp,
                    meta = meta,
                    sheet = sheet,
                    semesters = semesters,
                    semIndex = semIndex,
                    target = vm.attendTarget,
                    band = vm.attendBand,
                    onSemester = { semIndex = it },
                    onRetry = { attempt++ },
                    onOpen = { openId = it },
                )
            }
        }
    }
}

@Composable
private fun AttendanceList(
    stamp: Load.Ok<*>?,
    meta: Load<AttendanceMeta>,
    sheet: Load<Sheet>,
    semesters: List<Semester>,
    semIndex: Int,
    target: Int,
    band: Int,
    onSemester: (Int) -> Unit,
    onRetry: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val list = rememberLazyListState()
    val metaSlot: (@Composable () -> Unit)? = if (stamp != null) {
        { FreshnessLine(at = stamp.at, stale = stamp.stale, refreshing = stamp.refreshing) }
    } else {
        null
    }
    Column(Modifier.fillMaxSize()) {
        CollapsingTitle(
            title = "Attendance",
            offset = { list.scrollDepth() },
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp).entrance(0),
            meta = metaSlot,
        )

        RefreshBox(
            syncing = sheet is Load.Ok && sheet.refreshing,
            onRefresh = { onRetry() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                state = list,
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (semesters.size > 1) {
                    item(key = "chips") {
                        ChipRow(
                            semesters.map { it.description.ifBlank { it.code } },
                            semIndex,
                            modifier = Modifier.entrance(1),
                        ) { onSemester(it) }
                    }
                }

                when {
                    meta is Load.Loading -> item(key = "meta-load") { StateBox(meta) }

                    meta is Load.Failed -> item(key = "meta-fail") { StateBox(meta, onRetry) }

                    meta is Load.Ok && semesters.isEmpty() -> item(key = "no-sem") {
                        EmptyState("No Registrations", "The portal has no attendance registration on your record yet.") {
                            TowerMark(color = LineStrong, accent = LineStrong, cut = LineStrong, size = 120.dp)
                        }
                    }

                    sheet is Load.Failed -> item(key = "sheet-fail") { StateBox(sheet, onRetry) }

                    sheet is Load.Ok && sheet.value.detail.subjects.isEmpty() -> item(key = "no-subjects") {
                        EmptyState("Nothing Marked", "The semester is registered but no class has been marked yet.") {
                            TowerMark(color = LineStrong, accent = LineStrong, cut = LineStrong, size = 120.dp)
                        }
                    }

                    sheet is Load.Ok -> {
                        val subjects = sheet.value.detail.subjects
                        val percent = subjects.mapNotNull { it.percent }
                        if (percent.isNotEmpty()) {
                            item(key = "overall") {
                                Overall(
                                    average = percent.average(),
                                    index = 2,
                                    offset = { list.scrollDepth() },
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                        }
                        itemsIndexed(subjects, key = { _, s -> s.subjectId }) { index, subject ->
                            SubjectCard(subject, sheet.value.tallyOf(subject), index, target, band) { onOpen(subject.subjectId) }
                        }
                    }

                    else -> item(key = "sheet-load") { StateBox(sheet) }
                }
            }
        }
    }
}

/** the crimson hero with the term's average across the subjects on the record */
@Composable
private fun Overall(average: Double, index: Int, offset: () -> Float, modifier: Modifier = Modifier) {
    HeroPanel(
        height = 120.dp,
        angle = 160f,
        stops = arrayOf(0f to Crimson, 0.7f to CrimsonDeep),
        modifier = modifier.entrance(index),
        art = {
            TowerMark(
                size = 150.dp,
                accent = Bone,
                cut = Bone,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 30.dp)
                    .parallax(offset)
                    .artIn(),
            )
        },
    ) {
        Column(Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 14.dp)) {
            MonoText("OVERALL", color = Bone)
            D("${average.roundToInt()}%", 64.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SubjectCard(
    subject: SubjectAttendance,
    tally: Tally,
    index: Int,
    target: Int,
    band: Int,
    onClick: () -> Unit,
) {
    val shown = subject.percent ?: tally.percent
    // the line Settings sets, shared with the Class schedule's marks
    val verdict = verdictOf(shown, target, band)
    // one stagger step for the card and a later one for its ring, so the number
    // arrives after the card it belongs to is on screen
    val step = (index + 3).coerceAtMost(Motion.MAX_STAGGER)
    Box(
        Modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(2.dp, Line)
            .tap(onClick)
            .entrance(step),
    ) {
        // the board gives each card the faint motif of its own verdict
        Box(Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp).alpha(0.18f)) { Ghost(verdict) }

        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Ring(
                percent = shown,
                color = verdict.color,
                size = 64.dp,
                a11y = "${shown.roundToInt()} percent attendance, ${verdict.stamp}",
                delayMs = step * Motion.STAGGER + 160,
            )
            Column {
                BodyText(subject.name, size = 16.sp, maxLines = 2)
                MonoText("${tally.attended}/${tally.total} classes")
                Row(
                    Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Stamp(verdict.stamp, background = verdict.color)
                    val canMiss = tally.canMiss(target)
                    MonoText(
                        if (canMiss > 0) "Can miss $canMiss" else "Attend ${tally.mustAttend(target)} more",
                        color = Bone,
                    )
                }
            }
        }
    }
}

/** one faint motif per verdict, as the board's three rows show */
@Composable
private fun Ghost(verdict: Verdict) {
    when (verdict) {
        Verdict.Safe -> ShieldMark(size = 90.dp, cut = Surface1)
        Verdict.Close -> HourglassMark(size = 90.dp)
        Verdict.Low -> SentinelMark(size = 90.dp, cut = Surface1)
    }
}
