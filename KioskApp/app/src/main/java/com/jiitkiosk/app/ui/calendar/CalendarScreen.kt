package com.jiitkiosk.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.calendar.CalEvent
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
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.PrimaryButton
import com.jiitkiosk.app.ui.entrance
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
import com.jiitkiosk.app.ui.theme.Violet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val SHORT = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
private val DAY_MONTH = DateTimeFormatter.ofPattern("dd", Locale.ENGLISH)

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
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    MonoText(semester.from.format(SHORT).uppercase(Locale.ENGLISH))
                    MonoText("TODAY")
                    MonoText(semester.to.format(SHORT).uppercase(Locale.ENGLISH))
                }
            }
        }

        item {
            ChipRow(
                labels = months.map { it.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH) },
                selected = monthIndex,
                modifier = Modifier.padding(top = 6.dp).entrance(4),
            ) { monthIndex = it; selected = null }
        }

        item { MonthGrid(focusMonth, today, selected, official + user) { day -> selected = if (selected == day) null else day } }
        item { MonoText("OUTLINED = EXAM / EVENT · RED = TODAY", modifier = Modifier.entrance(5)) }

        val onDay = selected
        if (onDay != null) {
            item {
                Card2(Modifier.entrance(5)) {
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
                }
            }
        }

        val rows = (official + user).filter { it.inMonth(focusMonth) }.sortedWith(compareBy({ it.from }, { it.title }))
        if (rows.isEmpty()) {
            item {
                MonoText(
                    "NOTHING ON THE CALENDAR FOR THIS MONTH.",
                    modifier = Modifier.entrance(6).padding(top = 4.dp),
                )
            }
        } else {
            items(rows, key = { "${it.from}/${it.kind}/${it.title}" }) { event ->
                CalendarRow(
                    event = event,
                    pending = pendingRemove == "${event.from}/${event.title}",
                    onRemove = {
                        val key = "${event.from}/${event.title}"
                        if (pendingRemove == key) {
                            store.remove(event)
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
        height = 168.dp,
        angle = 118f,
        stops = arrayOf(0f to Violet, 0.75f to Ink),
        modifier = modifier,
        art = {
            Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(150.dp)) {
                Box(Modifier.align(Alignment.Center)) {
                    HourglassMark(size = 130.dp, color = Bone, accent = Crimson, sand = Violet)
                }
            }
        },
    ) {
        Column(Modifier.align(Alignment.TopStart).padding(14.dp)) {
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
                40.sp,
            )
        }
    }
}

/** one tile per teaching week; the current week is crimson */
@Composable
private fun WeekTrack(week: Int, total: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 1..total) {
            Box(
                Modifier
                    .weight(1f)
                    .height(12.dp)
                    .background(
                        when {
                            i == week -> Crimson
                            week == 0 -> LineStrong
                            i < week -> Bone
                            else -> Line
                        },
                    ),
            )
        }
    }
}

/** the month, Sunday first; today is filled, event days are outlined, dots mark their first day */
@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate?,
    events: List<CalEvent>,
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
                        Box(Modifier.weight(1f).height(52.dp))
                        continue
                    }
                    val date = month.atDay(day)
                    val starts = events.count { it.from == date }
                    val covered = events.any { it.on(date) }
                    val isToday = date == today
                    val isPicked = date == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .height(52.dp)
                            .background(if (isToday) Crimson else androidx.compose.ui.graphics.Color.Transparent)
                            .border(
                                2.dp,
                                when {
                                    isToday || isPicked -> Bone
                                    covered -> LineStrong
                                    else -> Line
                                },
                            )
                            .tap { onPick(date) }
                            .padding(5.dp),
                    ) {
                        D("$day", 18.sp, maxLines = 1)
                        Row(
                            Modifier.align(Alignment.BottomStart),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            repeat(starts.coerceAtMost(3)) {
                                Box(Modifier.size(5.dp).background(Bone))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** one calendar row: a boxed day, the title, and what kind of entry it is */
@Composable
private fun CalendarRow(event: CalEvent, pending: Boolean, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier.width(58.dp).border(2.dp, if (event.kind == CalendarStore.YOURS) Violet else Bone).padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            D(event.from.format(DAY_MONTH), 24.sp, maxLines = 1)
            MonoText(event.from.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            BodyText(event.title)
            // a single day's kind is the trailing tag; only a range needs its span spelled out
            val detail = when {
                event.kind == CalendarStore.YOURS -> "YOURS"
                event.single -> null
                else -> "${event.from.format(SHORT).uppercase(Locale.ENGLISH)} — ${event.to.format(SHORT).uppercase(Locale.ENGLISH)}"
            }
            if (detail != null) MonoText(detail, modifier = Modifier.padding(top = 2.dp))
        }
        when {
            event.kind != CalendarStore.YOURS -> MonoText(event.kind.uppercase(Locale.ENGLISH))
            pending -> Text(
                "TAP AGAIN",
                style = Mono.copy(fontWeight = FontWeight.Medium),
                color = Bad,
                modifier = Modifier.tap(onRemove),
            )
            else -> Text(
                "REMOVE",
                style = Mono.copy(fontWeight = FontWeight.Medium),
                color = Muted,
                modifier = Modifier.tap(onRemove),
            )
        }
    }
}
