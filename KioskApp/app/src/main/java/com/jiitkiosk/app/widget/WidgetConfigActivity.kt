package com.jiitkiosk.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jiitkiosk.app.ui.tap
import com.jiitkiosk.app.ui.components.BackRow
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.Ink
import com.jiitkiosk.app.ui.theme.KioskTheme
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.Mono
import com.jiitkiosk.app.ui.theme.Muted

/**
 * Board 33: the widget's settings over a live draft of the widget itself. One
 * activity for all six providers — which widget it edits is the widget id the
 * launcher passed, and it must be one of this app's own registered widgets (a
 * caller whose id belongs to nobody's device never edits anything here). Back
 * cancels; Done returns Ok, and the launcher keeps the widget as configured.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var kind: WidgetKind? = null
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var settings by mutableStateOf(WidgetSettings())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        // back and any crash land here: the launcher must treat that as no widget to configure
        setResult(RESULT_CANCELED)
        widgetId = intent?.getIntExtra(EXTRA_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        kind = WidgetKind.values().firstOrNull {
            AppWidgetManager.getInstance(this).getAppWidgetIds(ComponentName(this, it.provider)).contains(widgetId)
        }
        if (kind == null) {
            finish()
            return
        }
        settings = WidgetPrefs.get(this, widgetId)
        setContent {
            KioskTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ConfigScreen(
                        kind = kind!!,
                        widgetId = widgetId,
                        settings = settings,
                        onChange = { changed -> apply(changed) },
                        onDone = {
                            setResult(RESULT_OK, Intent().putExtra(EXTRA_ID, widgetId))
                            finish()
                        },
                    )
                }
            }
        }
    }

    /** a flip repaints the home screen immediately, then the preview below follows */
    private fun apply(s: WidgetSettings) {
        WidgetPrefs.put(this, widgetId, s)
        WidgetRender.paint(this, kind!!, intArrayOf(widgetId))
        settings = s
    }

    companion object {
        const val EXTRA_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
    }
}

@Composable
private fun ConfigScreen(
    kind: WidgetKind,
    widgetId: Int,
    settings: WidgetSettings,
    onChange: (WidgetSettings) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
    ) {
        BackRow(onBack = onDone)
        D("Widget settings", 52.sp, modifier = Modifier.padding(top = 6.dp))
        MonoText("PREVIEW UPDATES AS YOU CHANGE", modifier = Modifier.padding(top = 8.dp, bottom = 14.dp))
        PreviewBox(widgetId, kind, settings)
        val sections = configSections(kind)
        for (section in sections) when (section) {
            ConfigSection.NEXT -> ToggleRow(
                "Next class", "FROM YOUR SCHEDULE", settings.showNext,
            ) { onChange(settings.copy(showNext = it)) }

            ConfigSection.ATTENDANCE -> ToggleRow(
                "Attendance", "OVERALL AND LOWEST SUBJECT", settings.showAttendance,
            ) { onChange(settings.copy(showAttendance = it)) }

            ConfigSection.PENDING -> ToggleRow(
                "Pending assignments", "FROM GOOGLE CLASSROOM", settings.showPending,
            ) { onChange(settings.copy(showPending = it)) }

            ConfigSection.CLASSES -> {
                D("Classes shown", 22.sp, color = Muted, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..3).forEach { n ->
                        Chip(
                            n.toString(),
                            on = settings.classes == n,
                            onTap = { onChange(settings.copy(classes = n)) },
                        )
                    }
                }
            }

            ConfigSection.APPEARANCE -> {
                D("Appearance and privacy", 22.sp, color = Muted, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    Chip("Match app", on = !settings.transparent, onTap = { onChange(settings.copy(transparent = false)) })
                    Chip("Transparent", on = settings.transparent, onTap = { onChange(settings.copy(transparent = true)) })
                }
                ToggleRow(
                    "Hide subject names", "SHOW ONLY TIMES AND NUMBERS", settings.hideNames,
                ) { onChange(settings.copy(hideNames = it)) }
            }
        }
        MonoText(
            "UPDATES: THE APP REFRESHES IN THE BACKGROUND AND ON EVERY CLASS START. "
                + "ANDROID LIMITS HOW OFTEN A WIDGET CAN UPDATE, SO NUMBERS CAN LAG A LITTLE; "
                + "THE FOOTER ALWAYS SHOWS THE SYNC TIME.",
            modifier = Modifier.padding(top = 14.dp, bottom = 14.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .background(Bone)
                .tap(onDone),
            contentAlignment = Alignment.Center,
        ) { MonoText("Done", color = Ink, medium = true) }
    }
}

/** board 33's preview: the widget's own painter at the boards' card size */
@Composable
private fun PreviewBox(widgetId: Int, kind: WidgetKind, settings: WidgetSettings) {
    val context = LocalContext.current
    val den = context.resources.displayMetrics.density
    // the boards' card geometry: 2x2 = 160dp, wide = 328dp, dashboard height 400dp
    val wide = kind == WidgetKind.Today || kind == WidgetKind.Classes || kind == WidgetKind.Dashboard
    val widthPx = ((if (wide) 328f else 160f) * den).toInt()
        .coerceAtMost(context.resources.displayMetrics.widthPixels - (40f * den).toInt())
    val heightPx = ((if (kind == WidgetKind.Dashboard) 400f else 160f) * den).toInt()
    AndroidView(
        modifier = Modifier.padding(bottom = 14.dp),
        factory = { ctx -> FrameLayout(ctx) },
        update = { parent ->
            parent.removeAllViews()
            val views = WidgetRender.build(context, kind, widgetId, settings, readSnapshot(context))
            val view = views.apply(context, parent)
            parent.addView(view, FrameLayout.LayoutParams(widthPx, heightPx))
        },
    )
}

/** one board-33 row: 15px label, mono sub, the 48x28 pill on the right */
@Composable
private fun ToggleRow(title: String, sub: String, on: Boolean, set: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .rowRule()
            .tap { set(!on) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.padding(vertical = 10.dp)) {
            BodyText(title)
            MonoText(sub)
        }
        Spacer(Modifier.weight(1f))
        Pill(on)
    }
}

@Composable
private fun Pill(on: Boolean) {
    Box(
        Modifier
            .width(48.dp)
            .height(28.dp)
            .background(if (on) Crimson else Line)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .background(Bone)
                .align(if (on) Alignment.CenterEnd else Alignment.CenterStart),
        )
    }
}

/** board 33's square chip: solid crimson when taken, line grey when not */
@Composable
private fun Chip(text: String, on: Boolean, onTap: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .background(if (on) Crimson else Line)
            .tap(onTap)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = Mono.copy(fontSize = 12.sp),
            color = Bone,
        )
    }
}

/** the 1px rule under every board-33 row */
private fun Modifier.rowRule(): Modifier = drawBehind {
    drawLine(Line, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx())
}
