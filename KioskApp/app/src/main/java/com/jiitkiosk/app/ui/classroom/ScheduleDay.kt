package com.jiitkiosk.app.ui.classroom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.schedule.ClassEntry
import com.jiitkiosk.app.schedule.Schedule
import com.jiitkiosk.app.schedule.dayNumber
import com.jiitkiosk.app.schedule.dayShort
import com.jiitkiosk.app.schedule.entriesOn
import com.jiitkiosk.app.schedule.minutes
import com.jiitkiosk.app.schedule.minutesOf
import com.jiitkiosk.app.schedule.minutesOn
import com.jiitkiosk.app.schedule.minutesWeek
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.Card2
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.Stamp
import com.jiitkiosk.app.ui.tap
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.Muted
import com.jiitkiosk.app.ui.theme.Surface1
import com.jiitkiosk.app.ui.theme.Verdict
import com.jiitkiosk.app.ui.theme.verdictOf
import `in`.codelif.ktjiit.model.SubjectAttendance
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val NOW_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

/** the timeline's scale: 1.1dp per minute, ≈ the boards' 64px hour */
private const val PX_PER_MIN = 1.1f

/**
 * The schedule's landing view once one is saved (board 12): the next class and
 * today's hours, the week strip, the day's timeline with attendance markers,
 * and the week's hours. Everything is the phone's own data plus the attendance
 * sheet's percentages — a marker simply stays absent when there is no number.
 */
@Composable
fun ScheduleDayPane(vm: KioskViewModel, schedule: Schedule, onEdit: () -> Unit) {
    val subjects = rememberScheduleSubjects(vm)
    val today = dayNumber(LocalDate.now())
    var day by rememberSaveable { mutableIntStateOf(today ?: 1) }
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        // the NOW line and the countdown tick on their own
        while (true) {
            delay(30_000)
            now = LocalTime.now()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            val upcoming = nextClass(schedule, today, now)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    MonoText("NEXT CLASS IN")
                    D(upcoming?.let { hmText(it.second) } ?: "—", 44.sp, maxLines = 1)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    MonoText("TODAY")
                    D(hmText(schedule.minutesOn(today ?: 0)), 44.sp, maxLines = 1)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..6).forEach { d ->
                    val on = d == day
                    Box(
                        Modifier
                            .weight(1f)
                            .border(2.dp, if (on) Bone else Line)
                            .background(if (on) Crimson else Color.Transparent)
                            .tap { day = d }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        MonoText(dayShort(d), color = Bone)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.End) {
                MonoText(
                    "EDIT SCHEDULE",
                    color = Bone,
                    modifier = Modifier.tap(onEdit).padding(vertical = 10.dp),
                )
            }
        }
        item { TimelineCard(schedule, day, today, now, subjects, vm.attendTarget, vm.attendBand) }
        item { HoursCard(schedule, today) }
        item {
            MonoText(
                "MARKER = YOUR ATTENDANCE IN THAT SUBJECT (SAFE / CLOSE / LOW), SAME AS THE ATTENDANCE TAB",
                color = Muted,
            )
        }
    }
}

/** the day's classes on an hour grid, offset by their own start times */
@Composable
private fun TimelineCard(
    schedule: Schedule,
    day: Int,
    today: Int?,
    now: LocalTime,
    subjects: List<SubjectAttendance>,
    target: Int,
    band: Int,
) {
    val entries = schedule.entriesOn(day)
    var axisStart = 9 * 60
    var axisEnd = 17 * 60
    if (entries.isNotEmpty()) {
        axisStart = (entries.mapNotNull { minutesOf(it.start) }.minOrNull() ?: axisStart) / 60 * 60
        axisEnd = ((entries.mapNotNull { minutesOf(it.end) }.maxOrNull() ?: axisEnd) + 59) / 60 * 60
        if (axisEnd - axisStart < 240) axisEnd = axisStart + 240
    }

    Box(Modifier.fillMaxWidth().height(((axisEnd - axisStart) * PX_PER_MIN).dp)) {
        var t = axisStart
        while (t <= axisEnd) {
            val y = ((t - axisStart) * PX_PER_MIN).dp
            Box(Modifier.fillMaxWidth().offset(y = y).height(1.dp).background(Line))
            MonoText(hourLabel(t), modifier = Modifier.offset(y = y + 2.dp))
            t += 60
        }

        entries.forEach { entry ->
            val from = minutesOf(entry.start) ?: return@forEach
            val to = minutesOf(entry.end) ?: return@forEach
            val verdict = markFor(entry, subjects, target, band)
            Row(
                Modifier
                    .fillMaxWidth()
                    .offset(y = ((from - axisStart) * PX_PER_MIN).dp)
                    .padding(start = 44.dp)
                    .height(maxOf(((to - from) * PX_PER_MIN).dp - 4.dp, 28.dp))
                    .background(Surface1)
                    .border(2.dp, Line),
            ) {
                Box(Modifier.width(6.dp).fillMaxHeight().background(verdict?.color ?: Line))
                Column(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        BodyText(entry.subject, size = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                        verdict?.let { Stamp(it.stamp, background = it.color) }
                    }
                    MonoText(metaLine(entry))
                }
            }
        }

        if (day == today) {
            val nowM = now.hour * 60 + now.minute
            if (nowM in axisStart..axisEnd) {
                val y = ((nowM - axisStart) * PX_PER_MIN).dp
                Box(Modifier.fillMaxWidth().offset(y = y).height(2.dp).background(Crimson))
                Stamp(
                    "Now ${now.format(NOW_TIME)}",
                    background = Crimson,
                    color = Bone,
                    modifier = Modifier.align(Alignment.TopEnd).offset(y = y - 12.dp),
                )
            }
        }

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MonoText("NO CLASSES ON THIS DAY")
            }
        }
    }
}

/** hours per day, as the board's little bar chart */
@Composable
private fun HoursCard(schedule: Schedule, today: Int?) {
    Card2 {
        MonoText("HOURS THIS WEEK · ${hmText(schedule.minutesWeek())} TOTAL")
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().height(88.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            (1..6).forEach { d ->
                val mins = schedule.minutesOn(d)
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    MonoText(hmText(mins))
                    Spacer(Modifier.height(2.dp))
                    Box(
                        Modifier
                            .width(26.dp)
                            .height(((mins * 9) / 60).dp)
                            .background(if (d == today) Crimson else Bone),
                    )
                    Spacer(Modifier.height(4.dp))
                    MonoText(dayShort(d), color = Muted)
                }
            }
        }
    }
}

/** the subject's attendance verdict, when the sheet knows the subject and a percentage */
private fun markFor(entry: ClassEntry, subjects: List<SubjectAttendance>, target: Int, band: Int): Verdict? {
    val byId = entry.subjectId?.let { id -> subjects.firstOrNull { it.subjectId == id } }
    val match = byId ?: subjects.firstOrNull { it.name.equals(entry.subject, ignoreCase = true) }
    val percent = match?.combinedPercent ?: return null
    return verdictOf(percent, target, band)
}

/** "LAB · ROOM 201 · 2H" — the board's mono line under a class */
private fun metaLine(entry: ClassEntry): String {
    val parts = mutableListOf(entry.type.name.uppercase())
    entry.room?.let { parts += "ROOM $it" }
    entry.minutes()?.let { parts += if (it % 60 == 0) "${it / 60}H" else "${it / 60}H ${it % 60}M" }
    return parts.joinToString(" · ")
}

/** "3H" / "0H 20M" */
private fun hmText(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "${h}H" else "${h}H ${m}M"
}

/** "9AM" … "5PM" for the timeline gutter */
private fun hourLabel(minutes: Int): String {
    val h = minutes / 60
    val suffix = if (h < 12) "AM" else "PM"
    val h12 = when (h % 12) {
        0 -> 12
        else -> h % 12
    }
    return "$h12$suffix"
}

/** the next class that has not started yet, and the minutes until it (board: 0H 20M) */
private fun nextClass(schedule: Schedule, day: Int?, now: LocalTime): Pair<ClassEntry, Int>? {
    if (schedule.entries.isEmpty()) return null
    val nowM = now.hour * 60 + now.minute
    val from = day ?: 1
    for (offset in 0..5) {
        val d = (from - 1 + offset) % 6 + 1
        for (entry in schedule.entriesOn(d)) {
            val start = minutesOf(entry.start) ?: continue
            if (offset == 0 && day != null && start <= nowM) continue
            val ahead = if (day == null) (1440 - nowM) + offset * 1440 + start else offset * 1440 + start - nowM
            return entry to ahead
        }
    }
    return null
}
