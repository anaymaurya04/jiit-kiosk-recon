package com.jiitkiosk.app.ui.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.Motion
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.ChipRow
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.DetailRow
import com.jiitkiosk.app.ui.components.EmptyState
import com.jiitkiosk.app.ui.components.FreshnessLine
import com.jiitkiosk.app.ui.components.HeroPanel
import com.jiitkiosk.app.ui.components.Load
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.ScreenTitle
import com.jiitkiosk.app.ui.components.SectionHead
import com.jiitkiosk.app.ui.components.Stamp
import com.jiitkiosk.app.ui.components.StateBox
import com.jiitkiosk.app.ui.components.rememberCached
import com.jiitkiosk.app.ui.components.scrollDepth
import com.jiitkiosk.app.ui.entrance
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Ink
import com.jiitkiosk.app.ui.theme.LineStrong
import com.jiitkiosk.app.ui.theme.Night
import com.jiitkiosk.app.ui.theme.PlanetMark
import com.jiitkiosk.app.ui.theme.Violet
import `in`.codelif.ktjiit.model.ExamSlot
import `in`.codelif.ktjiit.model.Semester
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** one paper, with the event it belongs to for context */
@Serializable
data class Paper(val event: String = "", val slot: ExamSlot = ExamSlot()) {
    val at: LocalDateTime? get() = slot.day?.atTime(slot.start ?: LocalTime.MIN)
}

private val PAPER_ORDER = compareBy<Paper>({ it.slot.day?.toEpochDay() }, { it.slot.start?.toSecondOfDay() })
private val STAMP_DATE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val STAMP_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

private val LIST_OF_SEMESTER = ListSerializer(Semester.serializer())

@Serializable
data class ExamSheet(val papers: List<Paper> = emptyList())

@Composable
fun ExamsScreen(vm: KioskViewModel) {
    var attempt by remember { mutableStateOf(0) }
    var semIndex by rememberSaveable { mutableStateOf(0) }
    val list = rememberLazyListState()

    val token = vm.session?.token.orEmpty()
    val periods = rememberCached(vm, "exam-sems/$token", LIST_OF_SEMESTER, attempt) { it.examSemesters() }
    val semesters = (periods as? Load.Ok)?.value.orEmpty()
    val semester = semesters.getOrNull(semIndex)

    val sheet = rememberCached(
        vm,
        key = "exams/$token/${semester?.id.orEmpty()}",
        ser = ExamSheet.serializer(),
        attempt = attempt,
        enabled = semester != null,
    ) { portal ->
        val term = requireNotNull(semester)
        val papers = coroutineScope {
            portal.examEvents(term)
                .map { e -> async { portal.examSchedule(e).map { Paper(e.description, it) } } }
                .flatMap { it.await() }
        }
        ExamSheet(papers.sortedWith(PAPER_ORDER))
    }

    val stamp = (sheet as? Load.Ok) ?: (periods as? Load.Ok)

    // collapsed title bar mirrors SubjectScreen pattern
    val titleCollapse = (list.scrollDepth() / 120f).coerceIn(0f, 1f)

    Box(Modifier.fillMaxWidth()) {
        LazyColumn(
            Modifier.fillMaxWidth(),
            state = list,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.padding(bottom = 4.dp).entrance(0)) {
                    ScreenTitle("Exams")
                    if (stamp != null) {
                        FreshnessLine(stamp.at, stamp.stale, stamp.refreshing, Modifier.padding(top = 8.dp))
                    }
                }
            }

            if (semesters.size > 1) {
                item { ChipRow(semesters.map { it.description.ifBlank { it.code } }, semIndex, Modifier.entrance(1)) { semIndex = it } }
            }

            when {
                periods is Load.Loading -> item { StateBox(periods) }

                periods is Load.Failed -> item { StateBox(periods) { attempt++ } }

                periods is Load.Ok && semesters.isEmpty() -> item {
                    EmptyState("No Exams", "The portal has no exam event published for you.") {
                        PlanetMark(planet = LineStrong, ring = LineStrong, speck = LineStrong, detail = LineStrong, size = 110.dp)
                    }
                }

                sheet is Load.Loading -> item { StateBox(sheet) }

                sheet is Load.Failed -> item { StateBox(sheet) { attempt++ } }

                sheet is Load.Ok -> {
                    val papers = sheet.value.papers
                    if (papers.isEmpty()) {
                        item {
                            EmptyState("No Dates Yet", "The exam event exists but no paper has a date on it.") {
                                PlanetMark(planet = LineStrong, ring = LineStrong, speck = LineStrong, detail = LineStrong, size = 110.dp)
                            }
                        }
                    } else {
                        val now = LocalDateTime.now()
                        val next = papers.firstOrNull { it.at?.isAfter(now) == true }
                        val current = next ?: papers.last()
                        val rest = papers.dropWhile { it !== current }.drop(1)

                        item { NextPaperHero(current, upcoming = next != null, Modifier.padding(bottom = 4.dp).entrance(2)) }

                        // 7-day strip from today
                        val today = LocalDate.now()
                        val weekDays = (0 until 7).map { offset ->
                            val d = today.plusDays(offset.toLong())
                            com.jiitkiosk.app.ui.components.DayCell(
                                weekday = d.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.ENGLISH).uppercase(),
                                date = d.dayOfMonth,
                                today = offset == 0,
                                hasExam = papers.any { it.slot.day == d }
                            )
                        }
                        item {
                            Column(Modifier.padding(top = 8.dp).entrance(3)) {
                                SectionHead("This week", Modifier.padding(bottom = 8.dp))
                                com.jiitkiosk.app.ui.components.WeekStrip(weekDays)
                            }
                        }

                        if (rest.isNotEmpty()) {
                            item { SectionHead("Then", Modifier.padding(top = 10.dp, bottom = 4.dp).entrance(4)) }
                            items(rest, key = { it.slot.code + "/" + it.slot.date + "/" + it.slot.start }) { paper ->
                                val date = paper.slot.day
                                val dayStr = date?.dayOfMonth?.toString() ?: "—"
                                val monthStr = date?.month?.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)?.uppercase() ?: ""
                                val dayOfWeek = date?.dayOfWeek?.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)?.uppercase() ?: ""
                                val timeStr = paper.slot.start?.format(STAMP_TIME) ?: ""
                                val detailStr = listOf(dayOfWeek, timeStr).filter { it.isNotBlank() }.joinToString(" · ")
                                val roomSeat = listOf(
                                    paper.slot.room.ifBlank { "ROOM TBA" },
                                    paper.slot.seat.takeIf { it.isNotBlank() }?.let { "SEAT $it" }
                                ).filterNotNull().joinToString(" · ")
                                com.jiitkiosk.app.ui.components.TimelineEvent(
                                    day = dayStr,
                                    month = monthStr,
                                    title = paper.slot.subjectName.ifBlank { paper.slot.code },
                                    detail = detailStr,
                                    tag = paper.event.takeIf { it.isNotBlank() }?.uppercase(),
                                    room = roomSeat,
                                    art = {
                                        com.jiitkiosk.app.ui.theme.SatelliteMark(
                                            size = 70.dp,
                                            color = Bone,
                                            accent = com.jiitkiosk.app.ui.theme.Crimson,
                                            panel = Violet,
                                            cut = com.jiitkiosk.app.ui.theme.Surface1
                                        )
                                    },
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                        }
                    }
                }
            }

            item { MonoText("Room and seat numbers land about a day before the paper.", modifier = Modifier.entrance(Motion.MAX_STAGGER)) }
        }

        // compact title bar once the header has scrolled away
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
                D("Exams", 22.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun NextPaperHero(paper: Paper, upcoming: Boolean, modifier: Modifier = Modifier) {
    val orbit = com.jiitkiosk.app.ui.rememberOrbit(6000)
    val floatPx = (kotlin.math.sin(orbit.value * 2 * Math.PI).toFloat() * 4f)
    val tiltDeg = (kotlin.math.sin(orbit.value * 2 * Math.PI).toFloat() * 2f)

    HeroPanel(
        height = 250.dp,
        angle = 160f,
        stops = arrayOf(0f to Night, 0.8f to Ink),
        border = Bone,
        modifier = modifier,
        art = {
            PlanetMark(
                planet = Violet,
                ring = Bone,
                speck = Bone,
                detail = Ink,
                size = 230.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = (-20).dp)
                    .graphicsLayer {
                        translationY = floatPx
                        rotationZ = tiltDeg
                    },
            )
        },
    ) {
        Column(Modifier.align(Alignment.TopStart).padding(14.dp)) {
            Stamp(if (upcoming) "NEXT PAPER" else "LAST PAPER")
            BodyText(
                paper.slot.subjectName,
                size = 18.sp,
                maxLines = 2,
                modifier = Modifier.padding(top = 10.dp),
            )
            val at = paper.at
            if (upcoming && at != null) {
                val left = Duration.between(LocalDateTime.now(), at).toMinutes().coerceAtLeast(0)
                val days = left / 1440
                val hours = (left % 1440) / 60
                val minutes = left % 60
                D(if (days > 0) "${days}d" else "${hours}h", 64.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                D(
                    if (days > 0) "${hours}h ${minutes}m" else "${minutes}m",
                    64.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            } else {
                D("DONE", 64.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
            }
            MonoText(paperLine(paper), color = Bone)
        }
    }
}

private fun paperLine(paper: Paper): String = listOf(
    paper.slot.day?.format(STAMP_DATE),
    paper.slot.start?.format(STAMP_TIME),
    paper.slot.room.ifBlank { "ROOM TBA" },
    paper.slot.seat.ifBlank { null }?.let { "SEAT $it" },
).filterNotNull().joinToString(" · ").uppercase()

private fun paperDetail(paper: Paper): String = listOf(
    paper.slot.day?.format(STAMP_DATE),
    paper.slot.start?.format(STAMP_TIME),
    paper.event.takeIf { it.isNotBlank() },
).filterNotNull().joinToString(" · ")
