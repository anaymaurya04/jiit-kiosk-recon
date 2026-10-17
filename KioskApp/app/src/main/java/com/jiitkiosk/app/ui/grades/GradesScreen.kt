package com.jiitkiosk.app.ui.grades

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.components.BackRow
import com.jiitkiosk.app.ui.components.Card2
import com.jiitkiosk.app.ui.components.ChipRow
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.DetailRow
import com.jiitkiosk.app.ui.components.EmptyState
import com.jiitkiosk.app.ui.components.FreshnessLine
import com.jiitkiosk.app.ui.components.Load
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.NavRow
import com.jiitkiosk.app.ui.components.ScreenTitle
import com.jiitkiosk.app.ui.components.SectionHead
import com.jiitkiosk.app.ui.components.StateBox
import com.jiitkiosk.app.ui.components.rememberCached
import com.jiitkiosk.app.ui.components.scrollDepth
import com.jiitkiosk.app.ui.entrance
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.Ink
import com.jiitkiosk.app.ui.theme.LineStrong
import com.jiitkiosk.app.ui.theme.MarkCore
import com.jiitkiosk.app.ui.theme.MarkRing
import com.jiitkiosk.app.ui.theme.Muted
import com.jiitkiosk.app.ui.theme.VinylMark
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import com.jiitkiosk.app.ui.components.Bar
import com.jiitkiosk.app.ui.components.BarChart
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.CreditPips
import com.jiitkiosk.app.ui.components.GradeBadge
import com.jiitkiosk.app.ui.components.Stamp
import com.jiitkiosk.app.ui.theme.CrimsonDeep
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.Night
import com.jiitkiosk.app.ui.theme.Surface1
import com.jiitkiosk.app.ui.theme.TomeMark
import com.jiitkiosk.app.ui.theme.Violet
import com.jiitkiosk.app.ui.theme.halftone
import `in`.codelif.ktjiit.marks.MarksReport
import `in`.codelif.ktjiit.marks.Score
import `in`.codelif.ktjiit.model.GradeEntry
import `in`.codelif.ktjiit.model.Semester
import `in`.codelif.ktjiit.model.SemesterResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.util.Locale

private val SEMS = ListSerializer(Semester.serializer())

@Serializable
data class GradeSheet(val results: List<SemesterResult> = emptyList())

@Serializable
data class SemesterCard(val semester: String = "", val entries: List<GradeEntry> = emptyList())

@Serializable
data class Cards(val semesters: List<SemesterCard> = emptyList())

@Composable
private fun rememberGrades(vm: KioskViewModel, attempt: Int): Load<GradeSheet> {
    val token = vm.session?.token.orEmpty()
    return rememberCached(vm, "grades/$token", GradeSheet.serializer(), attempt) { portal ->
        // the endpoint wants the current semester number, and the attendance meta
        // is the cheapest call that carries it
        val current = portal.attendanceMeta().header?.semesterNumber.orEmpty()
        GradeSheet(portal.semesterResults(current))
    }
}

/** a sub-screen of this tab, kept inside it so the bottom nav stays put */
private enum class GradesPage { Overview, GradeCards, Marks }

@Composable
fun GradesScreen(vm: KioskViewModel, onExit: (() -> Unit)? = null) {
    var page by rememberSaveable { mutableStateOf(GradesPage.Overview) }

    // the system back button goes up a level, then out of the tab that opened us
    BackHandler(enabled = page != GradesPage.Overview || onExit != null) {
        if (page != GradesPage.Overview) page = GradesPage.Overview else onExit?.invoke()
    }
    when (page) {
        GradesPage.Overview -> GradesOverview(vm, onExit) { page = it }
        GradesPage.GradeCards -> GradeCardsScreen(vm) { page = GradesPage.Overview }
        GradesPage.Marks -> MarksScreen(vm) { page = GradesPage.Overview }
    }
}

@Composable
private fun GradesOverview(vm: KioskViewModel, onExit: (() -> Unit)?, open: (GradesPage) -> Unit) {
    var attempt by remember { mutableStateOf(0) }
    val state = rememberGrades(vm, attempt)
    val list = rememberLazyListState()

    Box(Modifier.fillMaxWidth()) {
        LazyColumn(
            Modifier.fillMaxWidth(),
            state = list,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onExit != null) item { BackRow(onExit, Modifier.entrance(0)) }
            item {
                Column(Modifier.padding(bottom = 4.dp).entrance(0)) {
                    ScreenTitle("Grades")
                    (state as? Load.Ok)?.let {
                        FreshnessLine(it.at, it.stale, it.refreshing, Modifier.padding(top = 8.dp))
                    }
                }
            }

            when (state) {
                is Load.Loading -> item { StateBox(state) }

                is Load.Failed -> item { StateBox(state) { attempt++ } }

                is Load.Ok -> {
                    val results = state.value.results.sortedBy { it.semester }
                    val latest = results.lastOrNull()
                    if (results.isEmpty()) {
                        item {
                            EmptyState("No Results", "No semester result has been published on the portal yet.") {
                                VinylMark(label = LineStrong, groove = LineStrong, disc = MarkCore, size = 110.dp)
                            }
                        }
                    } else {
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 4.dp).entrance(1),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                VinylMark(label = Crimson, groove = MarkRing, disc = MarkCore, size = 130.dp)
                                Column {
                                    MonoText("CGPA")
                                    D(twoDecimals(latest?.cgpa ?: 0.0), 72.sp, maxLines = 1)
                                    MonoText("SGPA ${twoDecimals(latest?.sgpa ?: 0.0)} LATEST")
                                }
                            }
                        }

                        item {
                            Card2(Modifier.entrance(2)) {
                                MonoText("SGPA BY SEMESTER")
                                Bars(results, Modifier.padding(top = 8.dp))
                            }
                        }

                        item { SectionHead("Documents", Modifier.padding(top = 6.dp).entrance(3)) }

                        item {
                            Column(Modifier.entrance(4)) {
                                NavRow("Grade cards") { open(GradesPage.GradeCards) }
                                NavRow("Marks by exam event") { open(GradesPage.Marks) }
                            }
                        }

                        item {
                            MonoText(
                                "Marks are read from the portal's own PDF. The portal decides what each event is worth.",
                                modifier = Modifier.entrance(5),
                            )
                        }
                    }
                }
            }
        }

        // compact title bar; the scroll is read in the draw phase so scrolling
        // never recomposes the screen
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(Ink)
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp)
                .graphicsLayer { alpha = (list.scrollDepth() / 120f).coerceIn(0f, 1f) },
        ) {
            D("Grades", 22.sp, maxLines = 1)
        }
    }
}

/**
 * One bar per semester, height in tenths of the CGPA scale, latest in crimson.
 * The mockup also lets you drag the leftmost bars to project a CGPA; that needs
 * a guess at future credits, so this shows what actually happened instead.
 */
@Composable
private fun Bars(results: List<SemesterResult>, modifier: Modifier = Modifier) {
    val scored = remember(results) { results.filter { it.sgpa > 0 } }
    if (scored.isEmpty()) {
        MonoText("Nothing published yet.", modifier = modifier)
        return
    }
    val latest = scored.last().semester
    Canvas(modifier.fillMaxWidth().height(110.dp)) {
        // the mockup's own proportions: 7-unit bars on an 11-unit pitch in a
        // 94 wide, 76 tall box, baseline at 70
        val ux = size.width / 94f
        val uy = size.height / 76f
        val pitch = 94f / (scored.size + 1)
        val barWidth = pitch * 0.64f
        val baseline = 70f * uy
        scored.forEachIndexed { i, result ->
            val barHeight = (result.sgpa / 10.0).toFloat() * baseline
            drawRect(
                color = if (result.semester == latest) Crimson else Bone,
                topLeft = Offset((pitch * (i + 1) - barWidth / 2f) * ux, baseline - barHeight),
                size = Size(barWidth * ux, barHeight),
            )
        }
    }
}

@Composable
private fun GradeCardsScreen(vm: KioskViewModel, onBack: () -> Unit) {
    var attempt by remember { mutableStateOf(0) }
    val token = vm.session?.token.orEmpty()
    // the same cache the overview fills: it carries the sgpa a grade card itself does not
    val grades = rememberGrades(vm, attempt)
    val state = rememberCached(vm, "gradecards/$token", Cards.serializer(), attempt) { portal ->
        val program = portal.program()
        val cards = coroutineScope {
            portal.gradeCardSemesters()
                .map { semester ->
                    async {
                        val entries = runCatching { portal.gradeCard(semester, program).entries }
                            .getOrDefault(emptyList())
                        if (entries.isEmpty()) null
                        else SemesterCard(semester.description.ifBlank { semester.code }, entries)
                    }
                }
                .mapNotNull { it.await() }
        }
        Cards(cards)
    }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { BackRow(onBack, Modifier.entrance(0)) }
        item { D("Grade Cards", 56.sp, modifier = Modifier.padding(top = 4.dp).entrance(1)) }
        item { FreshnessLine(state, Modifier.padding(top = 8.dp, bottom = 4.dp).entrance(1)) }

        when (state) {
            is Load.Loading -> item { StateBox(state) }
            is Load.Failed -> item { StateBox(state) { attempt++ } }
            is Load.Ok -> {
                val semesters = state.value.semesters
                if (semesters.isEmpty()) {
                    item { MonoText("No grade card has been published yet.") }
                } else {
                    val latestCard = semesters.last()
                    val totalCourses = latestCard.entries.size
                    val totalCredits = latestCard.entries.sumOf { it.credits }.toInt()
                    val latestSgpa = (grades as? Load.Ok)?.value?.results
                        ?.sortedBy { it.semester }?.lastOrNull()?.sgpa?.takeIf { it > 0 }

                    // Top summary pair from GradeCards.dc.html: SGPA hero (crimson gradient + halftone + vinyl) + stats card (courses, credits)
                    item {
                        Row(Modifier.fillMaxWidth().entrance(2), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(130.dp)
                                    .clipToBounds()
                                    .border(2.dp, Line)
                                    .background(Brush.linearGradient(listOf(Crimson, CrimsonDeep)))
                                    .halftone()
                                    .padding(14.dp)
                            ) {
                                VinylMark(
                                    label = Crimson,
                                    groove = MarkRing,
                                    disc = MarkCore,
                                    size = 110.dp,
                                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = 24.dp, y = 24.dp)
                                )
                                Column {
                                    Stamp("SGPA", background = Bone, color = Ink)
                                    if (latestSgpa != null) {
                                        D(twoDecimals(latestSgpa), 52.sp, modifier = Modifier.padding(top = 6.dp))
                                    } else {
                                        MonoText("PENDING", color = Bone, modifier = Modifier.padding(top = 10.dp))
                                    }
                                }
                            }
                            Card2(Modifier.weight(1f)) {
                                MonoText("COURSES")
                                D(totalCourses.toString(), 36.sp)
                                Spacer(Modifier.height(4.dp))
                                MonoText("CREDITS")
                                D(totalCredits.toString(), 36.sp)
                            }
                        }
                    }

                    // Grade distribution chart
                    val gradesMap = latestCard.entries.groupBy { it.grade.ifBlank { "—" } }
                        .mapValues { it.value.size }
                    if (gradesMap.isNotEmpty()) {
                        item {
                            Card2(
                                Modifier.entrance(3),
                                art = { TomeMark(size = 90.dp, color = Bone, accent = Crimson, spine = Violet) }
                            ) {
                                MonoText("GRADES THIS SEMESTER · COURSES PER GRADE")
                                Spacer(Modifier.height(8.dp))
                                GradeDistributionBars(gradesMap)
                            }
                        }
                    }

                    semesters.forEach { card ->
                        item {
                            Column(Modifier.padding(top = 10.dp)) {
                                D(
                                    "${card.semester} · FILLED SQUARE = B+ OR BETTER · DOTS = CREDITS",
                                    22.sp,
                                    color = Bone,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                        items(card.entries, key = { it.subjectId + "/" + it.semester + "/" + it.code }) { entry ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                                    .drawBehind {
                                        drawLine(Line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                GradeBadge(entry.grade)
                                Column(Modifier.weight(1f)) {
                                    BodyText(entry.name.ifBlank { entry.code }, maxLines = 2)
                                    MonoText(entry.code, modifier = Modifier.padding(top = 2.dp))
                                }
                                CreditPips(entry.credits.toInt())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeDistributionBars(distribution: Map<String, Int>, modifier: Modifier = Modifier) {
    val items = remember(distribution) { distribution.entries.sortedBy { it.key } }
    val maxCount = remember(items) { items.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1 }
    Row(
        modifier.fillMaxWidth().height(90.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        items.forEach { (grade, count) ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                MonoText(count.toString(), color = Bone)
                androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                val barHeight = (count.toFloat() / maxCount * 56.dp.value).dp.coerceAtLeast(4.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .background(if (com.jiitkiosk.app.ui.components.filledGrade(grade)) Bone else LineStrong)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
                MonoText(grade, color = Muted)
            }
        }
    }
}

@Composable
private fun MarksScreen(vm: KioskViewModel, onBack: () -> Unit) {
    var attempt by remember { mutableStateOf(0) }
    var index by rememberSaveable { mutableStateOf(0) }
    val token = vm.session?.token.orEmpty()

    val semesters = rememberCached(vm, "marks-sems/$token", SEMS, attempt) { it.marksSemesters() }
    val list = (semesters as? Load.Ok)?.value.orEmpty()
    val semester = list.getOrNull(index)

    val report = rememberCached(
        vm,
        key = "marks/$token/${semester?.id.orEmpty()}",
        ser = MarksReport.serializer(),
        attempt = attempt,
        enabled = semester != null,
    ) { portal -> portal.marks(requireNotNull(semester)) }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BackRow(onBack, Modifier.entrance(0)) }
        item { D("Marks", 56.sp, modifier = Modifier.padding(top = 4.dp).entrance(1)) }
        item { FreshnessLine(report, Modifier.padding(top = 8.dp).entrance(1)) }

        if (list.size > 1) {
            item { ChipRow(list.map { it.description.ifBlank { it.code } }, index, Modifier.entrance(2)) { index = it } }
        }

        when {
            semesters is Load.Loading -> item { StateBox(semesters) }
            semesters is Load.Failed -> item { StateBox(semesters) { attempt++ } }
            list.isEmpty() -> item { MonoText("No marks have been published yet.") }
            report is Load.Loading -> item { StateBox(report) }
            report is Load.Failed -> item { StateBox(report) { attempt++ } }
            report is Load.Ok -> {
                val subjects = report.value.subjects
                if (subjects.isEmpty()) {
                    item { MonoText("That semester has no marks on the portal.") }
                } else {
                    // Test-1 bars for the hero chart; the scale comes from the
                    // portal's own maxima, never a hardcoded "out of 20"
                    val test1 = subjects.mapNotNull { subj ->
                        val score = subj.scores.firstOrNull { it.event.contains("TEST-1", ignoreCase = true) || it.event.contains("T1", ignoreCase = true) }
                        val v = (score?.marks ?: score?.weighted) as? Score.Value ?: return@mapNotNull null
                        Bar(subj.code.takeLast(3).ifBlank { subj.name.take(3) }, v.obtained) to v.max
                    }

                    if (test1.isNotEmpty()) {
                        val chartMax = test1.maxOf { it.second }.coerceAtLeast(1.0)
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clipToBounds()
                                    .border(2.dp, Line)
                                    .background(Brush.linearGradient(listOf(Night, Surface1)))
                                    .halftone()
                                    .padding(14.dp)
                                    .entrance(3)
                            ) {
                                Column {
                                    Stamp("TEST-1 · OUT OF ${trim(chartMax)}")
                                    Spacer(Modifier.height(10.dp))
                                    BarChart(test1.map { it.first }, max = chartMax, threshold = chartMax / 2, thresholdLabel = "HALF")
                                }
                            }
                        }
                    }

                    subjects.forEachIndexed { sIdx, subject ->
                        item {
                            Card2(
                                Modifier.padding(top = 4.dp).entrance(4 + (sIdx % 4)),
                                art = {
                                    // the board cycles the four card motifs in order
                                    when (sIdx % 4) {
                                        0 -> com.jiitkiosk.app.ui.theme.GearMark(size = 100.dp, cut = com.jiitkiosk.app.ui.theme.Surface1)
                                        1 -> com.jiitkiosk.app.ui.theme.RadarMark(size = 100.dp)
                                        2 -> com.jiitkiosk.app.ui.theme.TomeMark(size = 100.dp)
                                        else -> com.jiitkiosk.app.ui.theme.HourglassMark(size = 100.dp)
                                    }
                                }
                            ) {
                                D(subject.name.ifBlank { subject.code }, 24.sp, maxLines = 2)
                                androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
                                subject.scores.forEach { score ->
                                    val valScore = score.marks ?: score.weighted
                                    when (valScore) {
                                        is Score.Value -> {
                                            com.jiitkiosk.app.ui.components.ScoreMeter(
                                                label = score.event,
                                                score = valScore.obtained,
                                                outOf = valScore.max.coerceAtLeast(1.0),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                        is Score.Absent -> {
                                            Row(
                                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                com.jiitkiosk.app.ui.components.BodyText(score.event, size = 13.sp)
                                                com.jiitkiosk.app.ui.components.Stamp("ABSENT", background = com.jiitkiosk.app.ui.theme.Bad, color = Bone)
                                            }
                                        }
                                        null -> {
                                            Row(
                                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                com.jiitkiosk.app.ui.components.BodyText(score.event, size = 13.sp)
                                                com.jiitkiosk.app.ui.components.Stamp("NOT YET", background = com.jiitkiosk.app.ui.theme.Line, color = Muted)
                                            }
                                        }
                                        else -> {
                                            Row(
                                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                com.jiitkiosk.app.ui.components.BodyText(score.event, size = 13.sp)
                                                MonoText(scoreText(valScore))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        MonoText(
                            report.value.generated.takeIf { it.isNotBlank() }
                                ?.let { "Generated $it." }
                                ?: "Read from the portal's marks PDF.",
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun twoDecimals(value: Double): String = String.format(Locale.ENGLISH, "%.2f", value)

/** the portal hands back 3.0 where it means 3 */
private fun trim(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private fun scoreText(score: Score?): String = when (score) {
    null -> "—"
    is Score.Value -> "${trim(score.obtained)}/${trim(score.max)}"
    is Score.Absent -> "ABSENT"
    is Score.NotApplicable -> "—"
    is Score.Other -> score.raw
}

