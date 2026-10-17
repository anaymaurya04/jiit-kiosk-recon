package com.jiitkiosk.app.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import com.jiitkiosk.app.ui.components.FreshnessLine
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.Motion
import com.jiitkiosk.app.ui.components.BackRow
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.Card2
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.KeyRow
import com.jiitkiosk.app.ui.components.Load
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.Stamp
import com.jiitkiosk.app.ui.components.StatBox
import com.jiitkiosk.app.ui.components.StateBox
import com.jiitkiosk.app.ui.components.friendly
import com.jiitkiosk.app.ui.components.rememberCached
import com.jiitkiosk.app.ui.components.softCall
import com.jiitkiosk.app.ui.components.scrollDepth
import com.jiitkiosk.app.ui.entrance
import com.jiitkiosk.app.ui.theme.Bad
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Good
import com.jiitkiosk.app.ui.theme.Ink
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.Ring
import com.jiitkiosk.app.ui.theme.verdictOf
import `in`.codelif.ktjiit.marks.MarksReport
import `in`.codelif.ktjiit.marks.Score
import `in`.codelif.ktjiit.model.ClassRecord
import `in`.codelif.ktjiit.model.RegisteredSubjects
import `in`.codelif.ktjiit.model.Semester
import `in`.codelif.ktjiit.model.SubjectAttendance
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** faculty, credits and the subject's marks, all of which arrive per semester */
@Serializable
data class SubjectFacts(
    val registered: RegisteredSubjects = RegisteredSubjects(),
    val marks: MarksReport? = null,
)

@Composable
private fun rememberFacts(vm: KioskViewModel, semester: Semester?, attempt: Int): Load<SubjectFacts> {
    val token = vm.session?.token.orEmpty()
    return rememberCached(
        vm,
        key = "facts/$token/${semester?.id.orEmpty()}",
        ser = SubjectFacts.serializer(),
        attempt = attempt,
        enabled = semester != null,
    ) { portal ->
        val term = requireNotNull(semester)
        SubjectFacts(
            registered = portal.registeredSubjects(term),
            marks = softCall<MarksReport?>(null) { portal.marks(term) },
        )
    }
}

@Composable
fun SubjectScreen(
    vm: KioskViewModel,
    subject: SubjectAttendance,
    sheet: Sheet,
    semester: Semester?,
    attempt: Int,
    onBack: () -> Unit,
) {
    val facts = rememberFacts(vm, semester, attempt)
    val tally = sheet.tallyOf(subject)
    val shown = subject.percent ?: tally.percent
    val verdict = verdictOf(shown, vm.attendTarget, vm.attendBand)
    val classes = sheet.daily[subject.subjectId]?.classes.orEmpty()
    val list = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxWidth(),
            state = list,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        ) {
        item { BackRow(onBack, Modifier.entrance(0)) }

        item {
            Column(Modifier.entrance(1)) {
                D(subject.name, 56.sp, maxLines = 3, modifier = Modifier.padding(top = 4.dp))
                MonoText(
                    listOf("SUBJECT DETAIL", subject.code).filter { it.isNotBlank() }.joinToString(" · "),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        item { FreshnessLine(facts, Modifier.padding(top = 12.dp, bottom = 4.dp).entrance(1)) }

        item {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp).entrance(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Ring(
                    percent = shown,
                    color = verdict.color,
                    size = 96.dp,
                    a11y = "${shown.roundToInt()} percent attendance, ${verdict.stamp}",
                    delayMs = Motion.STAGGER * 2,
                )
                Column {
                    Stamp(verdict.stamp, background = verdict.color)
                    val canMiss = tally.canMiss(vm.attendTarget)
                    BodyText(
                        if (canMiss > 0) "Can miss $canMiss more" else "Attend ${tally.mustAttend(vm.attendTarget)} more",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    MonoText("${tally.attended}/${tally.total} classes")
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp).entrance(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatBox("Lecture", percent(subject.lecturePercent), Modifier.weight(1f))
                StatBox("Tutorial", percent(subject.tutorialPercent), Modifier.weight(1f))
                StatBox("Practical", percent(subject.practicalPercent), Modifier.weight(1f))
            }
        }

        if (classes.isNotEmpty()) {
            item { Calendar(classes, Modifier.entrance(4)) }
        }

        item {
            val loaded = (facts as? Load.Ok)?.value
            val row = loaded?.registered?.rows?.firstOrNull { it.code == subject.code }
            val marks = loaded?.marks?.subjects?.firstOrNull { it.code == subject.code }
            val unresolved = if (facts is Load.Failed) "NOT AVAILABLE" else "PENDING"
            Card2(Modifier.padding(top = 4.dp).entrance(5)) {
                KeyRow("Faculty", row?.facultyName?.ifBlank { unresolved } ?: unresolved)
                KeyRow("Credits", row?.credits?.let { trimNumber(it) } ?: unresolved)
                KeyRow(
                    "Marks",
                    marksTotal(marks?.scores?.map { it.marks ?: it.weighted }.orEmpty()).ifBlank { "NOT YET" },
                )
            }
            when (facts) {
                is Load.Loading, is Load.Ok -> Unit
                is Load.Failed -> MonoText(
                    "FACULTY AND MARKS: ${friendly(facts.error)}",
                    modifier = Modifier.padding(top = 8.dp).entrance(6),
                )
            }
        }
        }

        // The full display title belongs to the artboard at rest. Once it has
        // scrolled away, this compact bar keeps the current subject identifiable;
        // the scroll is read in the draw phase so scrolling never recomposes.
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(Ink)
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp)
                .graphicsLayer { alpha = (list.scrollDepth() / 150f).coerceIn(0f, 1f) },
        ) {
            D(subject.name, 22.sp, maxLines = 1)
        }
    }
}

@Composable
private fun Calendar(classes: List<ClassRecord>, modifier: Modifier = Modifier) {
    val days = remember(classes) { calendarDays(classes) }
    val byDate = remember(classes) { classes.filter { it.date != null }.groupBy { it.date!! } }
    val present = byDate.count { (_, c) -> c.all { it.isPresent } }
    val absent = byDate.size - present

    Column(modifier) {
        MonoText("CALENDAR · GREEN PRESENT / RED ABSENT / GREY NO CLASS")
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp)
                .clearAndSetSemantics {
                    contentDescription = "$present days present, $absent days with a missed class"
                },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            days.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { day ->
                        val cell = byDate[day]
                        val colour = when {
                            cell == null -> Line
                            cell.all { it.isPresent } -> Good
                            else -> Bad
                        }
                        Box(Modifier.weight(1f).height(30.dp).background(colour.copy(alpha = 0.85f)))
                    }
                    // a short final week still has to keep the columns lined up
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** every day the class could have been held, Monday first, ending on a Sunday */
private fun calendarDays(classes: List<ClassRecord>): List<LocalDate> {
    val dates = classes.mapNotNull { it.date }.distinct().sorted()
    if (dates.isEmpty()) return emptyList()
    val first = dates.first().with(DayOfWeek.MONDAY)
    val last = dates.last().with(DayOfWeek.SUNDAY)
    // a bogus record from years ago must not turn into a thousand cells
    val start = if (ChronoUnit.DAYS.between(first, last) > 175) last.minusDays(174) else first
    return generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }.toList()
}

private fun percent(value: Double?): String = value?.let { "${it.roundToInt()}%" } ?: "PENDING"

/** the portal hands back 3.0 where it means 3 */
private fun trimNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

/** obtained and maximum across every event that is not an absence or a dash */
private fun marksTotal(scores: List<Score?>): String {
    val values = scores.filterIsInstance<Score.Value>()
    if (values.isEmpty()) return ""
    val obtained = values.sumOf { it.obtained }
    val max = values.sumOf { it.max }
    if (max == 0.0) return ""
    return "${trimNumber(obtained)}/${trimNumber(max)}"
}
