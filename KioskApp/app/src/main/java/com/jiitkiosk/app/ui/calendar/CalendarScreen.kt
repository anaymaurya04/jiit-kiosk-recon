package com.jiitkiosk.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.calendar.CalEvent
import com.jiitkiosk.app.calendar.CalInsert
import com.jiitkiosk.app.calendar.CalSemester
import com.jiitkiosk.app.calendar.CalendarStore
import com.jiitkiosk.app.data.Cache
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.components.BackRow
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.Card2
import com.jiitkiosk.app.ui.components.ChipRow
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.EmptyState
import com.jiitkiosk.app.ui.components.HeroPanel
import com.jiitkiosk.app.ui.components.Load
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.PrimaryButton
import com.jiitkiosk.app.ui.components.rememberCached
import com.jiitkiosk.app.ui.entrance
import com.jiitkiosk.app.ui.exams.ExamSheet
import com.jiitkiosk.app.ui.exams.Paper
import com.jiitkiosk.app.ui.me.MeData
import com.jiitkiosk.app.ui.tap
import com.jiitkiosk.app.ui.theme.Bad
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.HourglassMark
import com.jiitkiosk.app.ui.theme.Ink
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.LineStrong
import com.jiitkiosk.app.ui.theme.Mono
import com.jiitkiosk.app.ui.theme.Muted
import com.jiitkiosk.app.ui.theme.Sans
import com.jiitkiosk.app.ui.theme.Surface1
import com.jiitkiosk.app.ui.theme.Violet
import `in`.codelif.ktjiit.model.Semester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val SHORT = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
private val DAY_MONTH = DateTimeFormatter.ofPattern("dd", Locale.ENGLISH)
private val CLOCK_AMPM = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
private val LIST_OF_SEMESTER = ListSerializer(Semester.serializer())

/** the kind an exam paper's row carries; it is not one of the asset's own kinds */
internal const val EXAM = "exam"

/**
 * One rendered calendar line: an official entry, the student's own note, or an
 * exam paper. [note] is the mono second line (a span, or a paper's time);
 * nothing is invented to fill it.
 */
internal data class DayRow(
    val from: LocalDate,
    val to: LocalDate,
    val title: String,
    val note: String?,
    val kind: String,
    /** an exam's start, when the portal gave one — the moment handed to the phone's calendar */
    val at: LocalTime? = null,
) {
    fun inMonth(month: YearMonth): Boolean =
        YearMonth.from(from) <= month && month <= YearMonth.from(to)
}

/** every source on one list; exams carry their subject · event and their time */
internal fun dayRows(exams: List<Paper>, official: List<CalEvent>, user: List<CalEvent>): List<DayRow> {
    val rows = ArrayList<DayRow>(official.size + user.size + exams.size)
    for (e in official) rows += DayRow(e.from, e.to, e.title, rangeNote(e), e.kind)
    for (e in user) rows += DayRow(e.from, e.to, e.title, null, e.kind)
    for (p in exams) {
        val day = p.slot.day ?: continue
        val who = p.slot.subjectName.ifBlank { p.slot.code }
        val title = listOfNotNull(who.takeIf { it.isNotBlank() }, p.event.trim().takeIf { it.isNotEmpty() })
            .joinToString(" · ")
        if (title.isBlank()) continue
        rows += DayRow(day, day, title, p.slot.start?.format(CLOCK_AMPM), EXAM, p.slot.start)
    }
    return rows
}

/** only a span needs its range spelled out; a single day says it with its own cell */
private fun rangeNote(e: CalEvent): String? =
    if (e.single) null
    else "${e.from.format(SHORT).uppercase(Locale.ENGLISH)} — ${e.to.format(SHORT).uppercase(Locale.ENGLISH)}"

/** the portal registration today falls in; the nearest one when today is between two */
internal fun portalSemester(sems: List<Semester>, today: LocalDate): Semester? {
    val zone = ZoneId.systemDefault()
    val zoned = sems.mapNotNull { s ->
        val from = s.from ?: return@mapNotNull null
        val to = s.to ?: return@mapNotNull null
        Triple(s, Instant.ofEpochMilli(from).atZone(zone).toLocalDate(), Instant.ofEpochMilli(to).atZone(zone).toLocalDate())
    }
    return zoned.firstOrNull { today >= it.second && today <= it.third }?.first
        ?: zoned.minByOrNull { kotlin.math.abs(ChronoUnit.DAYS.between(today, it.second)) }?.first
        ?: sems.firstOrNull()
}

/**
 * Board 22: the semester at a glance. Week count, days left and the week track
 * come from the teaching window the PDF prints; the grid and the rows come from
 * the distilled asset plus the student's own notes. Nothing is sampled.
 */
@Composable
fun CalendarScreen(vm: KioskViewModel, onExit: () -> Unit) {
    val context = LocalContext.current
    val store = remember { CalendarStore(context) }
    val today = remember { LocalDate.now() }
    val semesters = remember { store.semesters() }
    val official = remember { store.official() }
    val semester = remember { CalendarStore.semesterFor(semesters, today) }

    var user by remember { mutableStateOf(store.user()) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    var draft by remember { mutableStateOf("") }
    var pendingRemove by remember { mutableStateOf<String?>(null) }
    var semesterNo by remember { mutableStateOf<Int?>(null) }
    var noCal by remember { mutableStateOf(false) }

    // the number on the header is the portal's own semester, if Me has ever answered
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val me = Cache(context.filesDir)
                .read("me/${vm.session?.token.orEmpty()}", MeData.serializer())?.value
            semesterNo = me?.info?.general?.semester?.takeIf { it > 0 }
        }
    }

    if (semester == null) {
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        ) {
            item { BackRow(onExit, Modifier.entrance(0)) }
            item { D("Calendar", 56.sp, modifier = Modifier.padding(top = 4.dp).entrance(1)) }
            item {
                EmptyState("No Calendar Data", "The calendar that ships with the app could not be read.") {
                    HourglassMark(size = 110.dp, color = LineStrong, accent = LineStrong, sand = LineStrong)
                }
            }
        }
        return
    }

    // the exams the Exams tab already caches — the same keys, so one fetch feeds
    // both screens; a failure here just leaves the calendar without exam rows
    val token = vm.session?.token.orEmpty()
    val periods = rememberCached(vm, "exam-sems/$token", LIST_OF_SEMESTER) { it.examSemesters() }
    val term = portalSemester((periods as? Load.Ok)?.value.orEmpty(), today)
    val sheet = rememberCached(
        vm,
        key = "exams/$token/${term?.id.orEmpty()}",
        ser = ExamSheet.serializer(),
        enabled = term != null,
    ) { portal ->
        val picked = requireNotNull(term)
        val papers = coroutineScope {
            portal.examEvents(picked)
                .map { e -> async { portal.examSchedule(e).map { Paper(e.description, it) } } }
                .flatMap { it.await() }
        }
        // sorted here because the Exams screen reads this record back and its
        // "first future paper" only makes sense on an ordered list
        ExamSheet(papers.sortedWith(compareBy({ it.slot.day?.toEpochDay() }, { it.slot.start?.toSecondOfDay() })))
    }
    val exams = (sheet as? Load.Ok)?.value?.papers.orEmpty()

    val months = semester.months.mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }
    // the semester's tab strip opens on the current month, wherever in the semester today is
    var monthIndex by remember { mutableIntStateOf(months.indexOf(YearMonth.from(today)).coerceAtLeast(0)) }
    val focusMonth = months.getOrNull(monthIndex.coerceIn(0, months.size - 1)) ?: YearMonth.from(today)

    val totalWeeks = ((ChronoUnit.DAYS.between(semester.from, semester.to) + 1 + 6) / 7).toInt()
    val elapsed = ChronoUnit.DAYS.between(semester.from, today)
    val week = if (today < semester.from) 0 else (elapsed / 7).toInt() + 1
    val daysLeft = ChronoUnit.DAYS.between(today, semester.to)

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { BackRow(onExit, Modifier.entrance(0)) }
        item { D("Calendar", 56.sp, modifier = Modifier.padding(top = 4.dp).entrance(1)) }
        item {
            MonoText(
                listOfNotNull(
                    semesterNo?.let { "SEMESTER $it" },
                    semester.name,
                    "JIIT CALENDAR 2026-27",
                ).joinToString(" · "),
                modifier = Modifier.entrance(1),
            )
        }

        item { CalendarHero(semester, today, week, totalWeeks, daysLeft, Modifier.entrance(2)) }

        item {
            Column(Modifier.entrance(3)) {
                WeekTrack(week = week, total = totalWeeks)
                // board 05's one-line axis: start ——— TODAY ——— end
                MonoText(
                    listOfNotNull(
                        semester.from.format(SHORT).uppercase(Locale.ENGLISH),
                        "TODAY".takeIf { today >= semester.from && today <= semester.to },
                        semester.to.format(SHORT).uppercase(Locale.ENGLISH),
                    ).joinToString(" ————— "),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        item {
            ChipRow(
                labels = months.map { it.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH) },
                selected = monthIndex,
                modifier = Modifier.padding(top = 6.dp).entrance(4),
            ) { monthIndex = it; selected = null }
        }

        item {
            MonthGrid(focusMonth, today, selected, official + user, exams.mapNotNull { it.slot.day }.toSet()) { day ->
                selected = if (selected == day) null else day
            }
        }
        item { MonoText("OUTLINED = EXAM / EVENT · RED = TODAY", modifier = Modifier.entrance(5)) }

        val all = dayRows(exams, official, user)
        val onDay = selected
        if (onDay != null) {
            val dayEvents = all.filter { onDay >= it.from && onDay <= it.to }.sortedBy { it.title }
            item {
                Card2(Modifier.entrance(5)) {
                    if (dayEvents.isNotEmpty()) {
                        MonoText("ON THIS DAY")
                        Spacer(Modifier.height(6.dp))
                        dayEvents.forEach { row ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    BodyText(row.title, maxLines = 2)
                                    row.note?.let { MonoText(it) }
                                }
                                MonoText(
                                    "ADD TO CALENDAR",
                                    color = Crimson,
                                    modifier = Modifier.tap {
                                        val took = if (row.at != null) {
                                            CalInsert.moment(
                                                context,
                                                row.title,
                                                row.from.atTime(row.at).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                            )
                                        } else {
                                            CalInsert.span(context, row.title, row.from, row.to)
                                        }
                                        noCal = !took
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    MonoText("ADD YOUR OWN EVENT ON ${onDay.format(SHORT).uppercase(Locale.ENGLISH)}")
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        textStyle = Sans.copy(fontSize = 16.sp, color = Bone),
                        cursorBrush = SolidColor(Crimson),
                        modifier = Modifier.fillMaxWidth().background(Line).padding(horizontal = 12.dp, vertical = 12.dp),
                        decorationBox = { field ->
                            Box {
                                if (draft.isBlank()) MonoText("WHAT IS IT?")
                                field()
                            }
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton(
                        "Add to ${onDay.format(SHORT).uppercase(Locale.ENGLISH)}",
                        onClick = {
                            store.add(draft, onDay)
                            user = store.user()
                            draft = ""
                            selected = null
                        },
                        enabled = draft.isNotBlank(),
                    )
                    if (noCal) {
                        Spacer(Modifier.height(10.dp))
                        MonoText("NO CALENDAR APP TOOK THE INTENT", color = Bad)
                    }
                }
            }
        }

        val rows = all.filter { it.inMonth(focusMonth) }.sortedWith(compareBy({ it.from }, { it.title }))
        if (rows.isEmpty()) {
            item {
                MonoText(
                    "NOTHING ON THE CALENDAR FOR THIS MONTH.",
                    modifier = Modifier.entrance(6).padding(top = 4.dp),
                )
            }
        } else {
            items(rows, key = { "${it.from}/${it.kind}/${it.title}" }) { row ->
                CalendarRow(
                    row = row,
                    pending = pendingRemove == "${row.from}/${row.title}",
                    onRemove = {
                        // only the student's own rows can be removed
                        val key = "${row.from}/${row.title}"
                        if (pendingRemove == key) {
                            store.remove(CalEvent(row.from, row.to, row.title, row.kind))
                            user = store.user()
                            pendingRemove = null
                        } else {
                            pendingRemove = key
                        }
                    },
                )
            }
        }
    }
}

/** the hero: week of the teaching window and the days it still has */
@Composable
private fun CalendarHero(
    semester: CalSemester,
    today: LocalDate,
    week: Int,
    totalWeeks: Int,
    daysLeft: Long,
    modifier: Modifier = Modifier,
) {
    HeroPanel(
        height = 110.dp,
        angle = 160f,
        stops = arrayOf(0f to Violet, 0.85f to Ink),
        modifier = modifier,
        art = {
            // the hourglass runs off the panel's bottom-right corner, as on the board
            Box(Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 24.dp)) {
                HourglassMark(size = 150.dp, color = Bone, accent = Crimson, sand = Violet)
            }
        },
    ) {
        Column(Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 12.dp)) {
            MonoText(
                when {
                    today < semester.from -> "STARTS ${semester.from.format(SHORT).uppercase(Locale.ENGLISH)}"
                    week > totalWeeks -> "TEACHING OVER"
                    else -> "WEEK $week OF $totalWeeks"
                },
                color = Bone,
            )
            Spacer(Modifier.height(6.dp))
            D(
                when {
                    today < semester.from -> "${ChronoUnit.DAYS.between(today, semester.from)} DAYS TO START"
                    daysLeft > 0 -> "$daysLeft DAYS LEFT"
                    else -> "SEMESTER OVER"
                },
                44.sp,
            )
        }
    }
}

/** one tile per teaching week; the present week is the dark tile with a crimson outline */
@Composable
private fun WeekTrack(week: Int, total: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..total) {
            Box(
                Modifier
                    .weight(1f)
                    .height(14.dp)
                    .background(if (i < week) Bone else Line)
                    .then(if (i == week) Modifier.border(2.dp, Crimson) else Modifier),
            )
        }
    }
}

/** the month, Sunday first; today is filled, event days are outlined with a dot, weekends muted */
@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate?,
    events: List<CalEvent>,
    examDays: Set<LocalDate>,
    onPick: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value % 7
    val days = month.lengthOfMonth()
    val rows = (offset + days + 6) / 7
    val letters = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            letters.forEach { letter ->
                MonoText(letter, modifier = Modifier.weight(1f), align = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        for (row in 0 until rows) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (col in 0 until 7) {
                    val day = row * 7 + col - offset + 1
                    if (day < 1 || day > days) {
                        Box(Modifier.weight(1f).height(46.dp))
                        continue
                    }
                    val date = month.atDay(day)
                    val covered = date in examDays || events.any { it.on(date) }
                    val isToday = date == today
                    val isPicked = date == selected
                    val weekend = date.dayOfWeek.value >= 6
                    Box(
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                            .background(if (isToday) Crimson else Surface1)
                            .border(
                                2.dp,
                                when {
                                    isToday || covered || isPicked -> Bone
                                    else -> Line
                                },
                            )
                            .tap { onPick(date) }
                            .padding(horizontal = 5.dp, vertical = 3.dp),
                    ) {
                        Column {
                            MonoText(
                                "$day",
                                color = if (covered || isToday || !weekend) Bone else Muted,
                            )
                            if (covered) {
                                Spacer(Modifier.height(6.dp))
                                Box(Modifier.size(6.dp).background(Bone))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** one calendar row: a boxed day, the title and its mono note; only your own rows offer REMOVE */
@Composable
private fun CalendarRow(row: DayRow, pending: Boolean, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind { drawLine(Line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.width(52.dp).border(2.dp, Bone).padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            D(row.from.format(DAY_MONTH), 26.sp, maxLines = 1)
            MonoText(row.from.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            BodyText(row.title)
            row.note?.let { MonoText(it, modifier = Modifier.padding(top = 2.dp)) }
        }
        if (row.kind == CalendarStore.YOURS) {
            if (pending) {
                Text(
                    "TAP AGAIN",
                    style = Mono.copy(fontWeight = FontWeight.Medium),
                    color = Bad,
                    modifier = Modifier.tap(onRemove),
                )
            } else {
                Text(
                    "REMOVE",
                    style = Mono.copy(fontWeight = FontWeight.Medium),
                    color = Muted,
                    modifier = Modifier.tap(onRemove),
                )
            }
        }
    }
}
