package com.jiitkiosk.app.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.domain.AttendanceMath
import com.jiitkiosk.app.domain.Tally
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.ChipRow
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.EmptyState
import com.jiitkiosk.app.ui.components.FreshnessLine
import com.jiitkiosk.app.ui.components.HeroPanel
import com.jiitkiosk.app.ui.components.Load
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.ScreenTitle
import com.jiitkiosk.app.ui.components.Stamp
import com.jiitkiosk.app.ui.components.StateBox
import com.jiitkiosk.app.ui.components.rememberCached
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.CrimsonDeep
import com.jiitkiosk.app.ui.theme.HelmMark
import com.jiitkiosk.app.ui.theme.Ink
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.LineStrong
import com.jiitkiosk.app.ui.theme.MarkRing
import com.jiitkiosk.app.ui.theme.PlanetMark
import com.jiitkiosk.app.ui.theme.Ring
import com.jiitkiosk.app.ui.theme.TrooperMark
import com.jiitkiosk.app.ui.theme.VinylMark
import com.jiitkiosk.app.ui.theme.Violet
import com.jiitkiosk.app.ui.theme.verdictOf
import `in`.codelif.ktjiit.model.AttendanceDetail
import `in`.codelif.ktjiit.model.AttendanceMeta
import `in`.codelif.ktjiit.model.DailyAttendance
import `in`.codelif.ktjiit.model.Semester
import `in`.codelif.ktjiit.model.SubjectAttendance
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** JIIT asks for 75 per cent, and every count on this screen is about that line */
const val TARGET = 75

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
    var attempt by remember { mutableStateOf(0) }
    var semIndex by rememberSaveable { mutableStateOf(0) }
    var open by remember { mutableStateOf<Pair<Sheet, SubjectAttendance>?>(null) }

    val token = vm.session?.token.orEmpty()
    val meta = rememberCached(vm, "att-meta/$token", AttendanceMeta.serializer(), attempt) { it.attendanceMeta() }
    val semesters = (meta as? Load.Ok)?.value?.semesters.orEmpty()
    val semester = semesters.getOrNull(semIndex)
    val sheet = rememberSheet(vm, semester, attempt)

    // the system back button goes up a level before it leaves the app
    BackHandler(enabled = open != null) { open = null }

    val openPair = open
    if (openPair != null) {
        SubjectScreen(
            vm = vm,
            subject = openPair.second,
            sheet = openPair.first,
            semester = semester,
            attempt = attempt,
            onBack = { open = null },
        )
        return
    }

    // the newest thing we have, whichever call produced it
    val stamp = (sheet as? Load.Ok) ?: (meta as? Load.Ok)

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                ScreenTitle("Attendance")
                if (stamp != null) {
                    FreshnessLine(
                        at = stamp.at,
                        stale = stamp.stale,
                        refreshing = stamp.refreshing,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        if (semesters.size > 1) {
            item { ChipRow(semesters.map { it.description.ifBlank { it.code } }, semIndex) { semIndex = it } }
        }

        when {
            meta is Load.Loading -> item { StateBox(meta) }

            meta is Load.Failed -> item { StateBox(meta) { attempt++ } }

            meta is Load.Ok && semesters.isEmpty() -> item {
                EmptyState("No Registrations", "The portal has no attendance registration on your record yet.") {
                    HelmMark(arch = LineStrong, accent = LineStrong, detail = LineStrong, size = 120.dp)
                }
            }

            sheet is Load.Loading -> item { StateBox(sheet) }

            sheet is Load.Failed -> item { StateBox(sheet) { attempt++ } }

            sheet is Load.Ok -> {
                val subjects = sheet.value.detail.subjects
                if (subjects.isEmpty()) {
                    item {
                        EmptyState("Nothing Marked", "The semester is registered but no class has been marked yet.") {
                            HelmMark(arch = LineStrong, accent = LineStrong, detail = LineStrong, size = 120.dp)
                        }
                    }
                } else {
                    item {
                        val percent = subjects.mapNotNull { it.percent }
                        if (percent.isNotEmpty()) {
                            Overall(percent.average(), Modifier.padding(bottom = 4.dp))
                        }
                    }
                    items(subjects, key = { it.subjectId }) { subject ->
                        SubjectCard(subject, sheet.value.tallyOf(subject), subjects.indexOf(subject)) {
                            open = sheet.value to subject
                        }
                    }
                }
            }
        }
    }
}

/** the crimson hero with the term's average across the subjects on the record */
@Composable
private fun Overall(average: Double, modifier: Modifier = Modifier) {
    HeroPanel(
        height = 120.dp,
        angle = 160f,
        stops = arrayOf(0f to Crimson, 0.7f to CrimsonDeep),
        modifier = modifier,
        art = {
            HelmMark(
                arch = Ink,
                accent = Crimson,
                detail = Crimson,
                size = 150.dp,
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = 30.dp),
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
private fun SubjectCard(subject: SubjectAttendance, tally: Tally, index: Int, onClick: () -> Unit) {
    val shown = subject.percent ?: tally.percent
    val verdict = verdictOf(shown)
    Box(
        Modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(2.dp, Line)
            .clickable(onClick = onClick),
    ) {
        // the mockups give each card a different faint motif behind the content
        Box(Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp).alpha(0.18f)) { Ghost(index) }

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
                    val canMiss = tally.canMiss(TARGET)
                    MonoText(
                        if (canMiss > 0) "Can miss $canMiss" else "Attend ${tally.mustAttend(TARGET)} more",
                        color = Bone,
                    )
                }
            }
        }
    }
}

/** one faint motif per subject, cycled so a list never repeats the same one twice in a row */
@Composable
private fun Ghost(index: Int) {
    when (index % 4) {
        0 -> HelmMark(arch = Bone, accent = Crimson, detail = Ink, size = 90.dp)
        1 -> PlanetMark(planet = Violet, ring = Bone, speck = Bone, detail = Ink, size = 90.dp)
        2 -> VinylMark(label = Crimson, groove = MarkRing, disc = Ink, size = 90.dp)
        else -> TrooperMark(band = Violet, card = Bone, clip = Crimson, size = 90.dp)
    }
}
