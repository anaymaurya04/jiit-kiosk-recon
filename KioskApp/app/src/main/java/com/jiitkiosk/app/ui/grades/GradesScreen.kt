package com.jiitkiosk.app.ui.grades

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.LineStrong
import com.jiitkiosk.app.ui.theme.MarkCore
import com.jiitkiosk.app.ui.theme.MarkRing
import com.jiitkiosk.app.ui.theme.VinylMark
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
fun GradesScreen(vm: KioskViewModel) {
    var page by remember { mutableStateOf(GradesPage.Overview) }

    // the system back button goes up a level before it leaves the app
    BackHandler(enabled = page != GradesPage.Overview) { page = GradesPage.Overview }
    when (page) {
        GradesPage.Overview -> GradesOverview(vm) { page = it }
        GradesPage.GradeCards -> GradeCardsScreen(vm) { page = GradesPage.Overview }
        GradesPage.Marks -> MarksScreen(vm) { page = GradesPage.Overview }
    }
}

@Composable
private fun GradesOverview(vm: KioskViewModel, open: (GradesPage) -> Unit) {
    var attempt by remember { mutableStateOf(0) }
    val state = rememberGrades(vm, attempt)

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
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
                            Modifier.fillMaxWidth().padding(bottom = 4.dp),
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
                        Card2 {
                            MonoText("SGPA BY SEMESTER")
                            Bars(results, Modifier.padding(top = 8.dp))
                        }
                    }

                    item { SectionHead("Documents", Modifier.padding(top = 6.dp)) }

                    item {
                        Column {
                            NavRow("Grade cards") { open(GradesPage.GradeCards) }
                            NavRow("Marks by exam event") { open(GradesPage.Marks) }
                        }
                    }

                    item {
                        MonoText("Marks are read from the portal's own PDF. The portal decides what each event is worth.")
                    }
                }
            }
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
    ) {
        item { BackRow(onBack) }
        item { D("Grade Cards", 56.sp, modifier = Modifier.padding(top = 4.dp)) }
        item { FreshnessLine(state, Modifier.padding(top = 8.dp, bottom = 12.dp)) }

        when (state) {
            is Load.Loading -> item { StateBox(state) }

            is Load.Failed -> item { StateBox(state) { attempt++ } }

            is Load.Ok -> if (state.value.semesters.isEmpty()) {
                item { MonoText("No grade card has been published yet.") }
            } else {
                state.value.semesters.forEach { card ->
                    item { SectionHead(card.semester, Modifier.padding(top = 10.dp)) }
                    items(card.entries, key = { it.subjectId + it.semester }) { entry ->
                        DetailRow(
                            title = entry.name.ifBlank { entry.code },
                            detail = listOf(entry.code, "${trim(entry.credits)} credits")
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                            trailing = entry.grade.ifBlank { "—" },
                        )
                    }
                }
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
        item { BackRow(onBack) }
        item { D("Marks", 56.sp, modifier = Modifier.padding(top = 4.dp)) }
        item { FreshnessLine(report, Modifier.padding(top = 8.dp)) }

        if (list.size > 1) {
            item { ChipRow(list.map { it.description.ifBlank { it.code } }, index) { index = it } }
        }

        when {
            semesters is Load.Loading -> item { StateBox(semesters) }

            semesters is Load.Failed -> item { StateBox(semesters) { attempt++ } }

            list.isEmpty() -> item { MonoText("No marks have been published yet.") }

            report is Load.Loading -> item { StateBox(report) }

            report is Load.Failed -> item { StateBox(report) { attempt++ } }

            report is Load.Ok -> if (report.value.subjects.isEmpty()) {
                item { MonoText("That semester has no marks on the portal.") }
            } else {
                report.value.subjects.forEach { subject ->
                    item { SectionHead(subject.name.ifBlank { subject.code }, Modifier.padding(top = 10.dp)) }
                    // the keys of every row in a list have to be unique across the
                    // whole list, and "T1" shows up under each subject
                    items(subject.scores, key = { subject.code + "/" + it.event }) { score ->
                        DetailRow(title = score.event, trailing = scoreText(score.marks ?: score.weighted))
                    }
                }
                item {
                    MonoText(
                        report.value.generated.takeIf { it.isNotBlank() }
                            ?.let { "Generated $it." }
                            ?: "Read from the portal's marks PDF.",
                    )
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
