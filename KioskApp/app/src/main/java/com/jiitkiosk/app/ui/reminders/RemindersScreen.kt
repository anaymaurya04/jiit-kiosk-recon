package com.jiitkiosk.app.ui.reminders

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.classroom.ClassroomData
import com.jiitkiosk.app.classroom.Course
import com.jiitkiosk.app.data.Cache
import com.jiitkiosk.app.reminders.Notifications
import com.jiitkiosk.app.reminders.Reminder
import com.jiitkiosk.app.reminders.ReminderPlanner
import com.jiitkiosk.app.reminders.ReminderPrefs
import com.jiitkiosk.app.reminders.ReminderSync
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.Motion
import com.jiitkiosk.app.ui.components.BackRow
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.Card2
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.PrimaryButton
import com.jiitkiosk.app.ui.components.RowLine
import com.jiitkiosk.app.ui.components.SectionHead
import com.jiitkiosk.app.ui.components.Toggle
import com.jiitkiosk.app.ui.entrance
import com.jiitkiosk.app.ui.tap
import com.jiitkiosk.app.ui.theme.Bad
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.Cursed
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.Mono
import com.jiitkiosk.app.ui.theme.Muted
import com.jiitkiosk.app.ui.theme.RavenMark
import com.jiitkiosk.app.ui.theme.Surface1
import com.jiitkiosk.app.ui.theme.halftone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FIRE = DateTimeFormatter.ofPattern("EEE dd MMM HH:mm", Locale.ENGLISH)

/**
 * Board 21: the reminder settings as rows and chips, and under them the plan
 * those settings produce right now — real assignments, never a sample. Every
 * change re-lays the alarms immediately, so the screen never lies about what
 * will arrive.
 */
@Composable
fun RemindersScreen(vm: KioskViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { ReminderPrefs(context) }
    var values by remember { mutableStateOf(prefs.values()) }
    var plan by remember { mutableStateOf<List<Reminder>>(emptyList()) }
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var allowed by remember { mutableStateOf(Notifications.allowed(context)) }

    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = Notifications.allowed(context)
    }

    // one path for every change: recompute the plan, then hand it to the alarms
    LaunchedEffect(values, allowed) {
        withContext(Dispatchers.IO) {
            val data = Cache(context.filesDir)
                .read(ClassroomData.CACHE_KEY, ClassroomData.serializer())?.value
            courses = data?.courses.orEmpty()
            plan = ReminderSync.refresh(context, data)
        }
    }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { BackRow(onBack, Modifier.entrance(0)) }
        item { D("Reminders", 56.sp, modifier = Modifier.padding(top = 4.dp).entrance(1)) }
        item {
            MonoText(
                "AUTOMATIC · FOR UNSUBMITTED ASSIGNMENTS",
                modifier = Modifier.entrance(1).padding(bottom = 8.dp),
            )
        }

        item {
            Card2(
                Modifier.entrance(2),
                art = {
                    // the board's faint raven sits behind this card
                    RavenMark(size = 90.dp, cut = Surface1)
                },
            ) {
                MonoText("GOOGLE ACCOUNT CONNECTED")
                Spacer(Modifier.height(4.dp))
                BodyText(
                    if (vm.classroom.token() != null) {
                        "Read-only access to the account you approved on this phone."
                    } else {
                        "Not connected yet — connect Google Classroom on the Class tab."
                    },
                    color = Muted,
                )
            }
        }

        if (!allowed) {
            item {
                Card2(Modifier.padding(top = 10.dp).entrance(2)) {
                    MonoText("NOTIFICATIONS BLOCKED", color = Bad, medium = true)
                    Spacer(Modifier.height(6.dp))
                    BodyText(
                        "Deadline reminders arrive as notifications, and Android has not let this app post them.",
                        color = Muted,
                    )
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton(
                        "Allow notifications",
                        onClick = { ask.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    )
                }
            }
        }

        item {
            RowLine(
                label = "Remind me about unsubmitted work",
                modifier = Modifier
                    .toggleable(value = values.enabled, role = Role.Switch) {
                        prefs.setEnabled(it)
                        values = prefs.values()
                    }
                    .padding(top = 10.dp)
                    .entrance(3),
                trailing = { Toggle(values.enabled) },
            )
        }
        item {
            MonoText(
                "CHECKS EVERY 15 MIN IN THE BACKGROUND",
                modifier = Modifier.entrance(3).padding(bottom = 6.dp),
            )
        }

        item { SectionHead("Remind me before deadline", Modifier.padding(top = 18.dp, bottom = 10.dp).entrance(4)) }
        item {
            LeadChips(values.leads, Modifier.entrance(4)) { lead ->
                prefs.setLead(lead, lead !in values.leads)
                values = prefs.values()
            }
        }
        item {
            MonoText(
                "One ping per selected lead time, per deadline.",
                modifier = Modifier.entrance(4).padding(top = 10.dp),
            )
        }

        item {
            RowLine(
                label = "Quiet hours",
                modifier = Modifier
                    .toggleable(value = values.quietEnabled, role = Role.Switch) {
                        prefs.setQuiet(it)
                        values = prefs.values()
                    }
                    .padding(top = 14.dp)
                    .entrance(5),
                trailing = { Toggle(values.quietEnabled) },
            )
        }
        item {
            MonoText(
                "${hhmm(values.quietStartMin)} TO ${hhmm(values.quietEndMin)} · A PING INSIDE THE " +
                    "WINDOW MOVES TO ITS END AND IS DROPPED IF THAT WOULD PASS THE DEADLINE",
                modifier = Modifier.entrance(5).padding(top = 4.dp),
            )
        }

        item { SectionHead("Courses", Modifier.padding(top = 22.dp, bottom = 6.dp).entrance(6)) }
        if (courses.isEmpty()) {
            item {
                MonoText(
                    "No courses cached yet — open the Class tab and connect Google Classroom.",
                    modifier = Modifier.entrance(6),
                )
            }
        }
        items(courses, key = { it.id }) { course ->
            RowLine(
                label = course.name,
                modifier = Modifier.toggleable(
                    value = values.courseOn(course.id),
                    role = Role.Switch,
                ) {
                    prefs.setCourse(course.id, it)
                    values = prefs.values()
                },
                trailing = { Toggle(values.courseOn(course.id)) },
            )
        }

        item {
            val next = plan.firstOrNull()
            Card2(Modifier.padding(top = 22.dp).entrance(7)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // the board's cursed tile with the raven in it
                    Box(Modifier.size(56.dp).background(Cursed).halftone()) {
                        Box(Modifier.align(Alignment.Center)) { RavenMark(size = 56.dp, cut = Cursed) }
                    }
                    Column(Modifier.weight(1f)) {
                        MonoText("PREVIEW · NOTIFICATION")
                        Spacer(Modifier.height(6.dp))
                        if (next != null) {
                            BodyText("${next.work.title} is due in ${next.leadHours}h")
                            Spacer(Modifier.height(4.dp))
                            MonoText("${next.work.courseName.uppercase(Locale.ENGLISH)} · NOT SUBMITTED")
                            Spacer(Modifier.height(10.dp))
                            MonoText("NEXT PING ${fireLabel(next.fireAt)} · ${plan.size} SCHEDULED")
                        } else {
                            MonoText("NOTHING SCHEDULED", color = Bone, medium = true)
                            Spacer(Modifier.height(4.dp))
                            MonoText(whyNone(values, allowed))
                        }
                    }
                }
            }
        }
        item {
            MonoText(
                "Android may delay a ping by minutes while the phone is dozing. This app asks for no " +
                    "exact-alarm permission, so a slight drift is the honest trade.",
                modifier = Modifier.entrance(7).padding(top = 10.dp),
            )
        }
    }
}

/** board 21's lead chips — unlike the filter chips, several stay on at once */
@Composable
private fun LeadChips(leads: Set<Int>, modifier: Modifier = Modifier, onToggle: (Int) -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReminderPlanner.LEADS.forEach { lead ->
            val on = lead in leads
            val bg by animateColorAsState(
                targetValue = if (on) Crimson else Line,
                animationSpec = tween(Motion.FAST),
                label = "lead",
            )
            Text(
                text = "${lead}H",
                style = Mono.copy(fontWeight = FontWeight.Medium),
                color = Bone,
                modifier = Modifier
                    .tap { onToggle(lead) }
                    .background(bg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

private fun hhmm(minutes: Int): String =
    String.format(Locale.ENGLISH, "%02d:%02d", minutes / 60, minutes % 60)

private fun fireLabel(at: Long): String =
    Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(FIRE)

private fun whyNone(values: ReminderPrefs.Values, allowed: Boolean): String = when {
    !allowed -> "Android is blocking notifications, so nothing would arrive."
    !values.enabled -> "Reminders are switched off."
    values.leads.isEmpty() -> "No lead time is selected."
    else -> "No unsubmitted assignment with a deadline is cached right now."
}
