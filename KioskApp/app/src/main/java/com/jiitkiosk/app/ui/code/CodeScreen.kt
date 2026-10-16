package com.jiitkiosk.app.ui.code

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.code.CodePrefs
import com.jiitkiosk.app.code.LEET_TTL_MS
import com.jiitkiosk.app.code.LeetRecent
import com.jiitkiosk.app.code.LeetStats
import com.jiitkiosk.app.code.LeetTopic
import com.jiitkiosk.app.code.LeetCodeClient
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.Card2
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.EmptyState
import com.jiitkiosk.app.ui.components.FreshnessLine
import com.jiitkiosk.app.ui.components.Load
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.ScreenTitle
import com.jiitkiosk.app.ui.components.SectionHead
import com.jiitkiosk.app.ui.components.Stamp
import com.jiitkiosk.app.ui.components.StateBox
import com.jiitkiosk.app.ui.components.ago
import com.jiitkiosk.app.ui.entrance
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.LineStrong
import com.jiitkiosk.app.ui.theme.LongswordMark
import com.jiitkiosk.app.ui.theme.Muted
import com.jiitkiosk.app.ui.theme.Night
import com.jiitkiosk.app.ui.theme.Surface2
import com.jiitkiosk.app.ui.theme.Violet
import java.util.Locale
import kotlinx.coroutines.CancellationException

// the two extra violet steps the board's heatmap adds between the theme's Night and Violet
private val HeatMid = Color(0xFF4A3A9A)
private val HeatMax = Color(0xFFB0A0F0)

/**
 * The Code tab: the LeetCode board for the handle set in Settings. The numbers
 * come from LeetCode's public API and are re-read at most every 15 minutes;
 * everything on screen says when it was last read.
 */
@Composable
fun CodeScreen(vm: KioskViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { CodePrefs(context) }
    val handle = remember { prefs.leetcode() }
    var attempt by remember { mutableIntStateOf(0) }

    val key = handle?.let { "leetcode/$it" }
    val ser = LeetStats.serializer()
    val seed = remember(key, attempt) { key?.let { vm.lastGood(it, ser) } }
    var state by remember(key, attempt) {
        mutableStateOf<Load<LeetStats>>(seed?.let { Load.Ok(it.value, it.at) } ?: Load.Loading)
    }
    LaunchedEffect(key, attempt) {
        if (key == null || handle == null) return@LaunchedEffect
        // inside the TTL nothing is asked of LeetCode and nothing is rewritten:
        // the board shows the cached numbers under their real sync time
        if (seed != null && attempt == 0 && System.currentTimeMillis() - seed.at < LEET_TTL_MS) {
            state = Load.Ok(seed.value, seed.at)
            return@LaunchedEffect
        }
        state = try {
            val s = vm.keep(key, ser) { LeetCodeClient.fetch(handle) }
            Load.Ok(s.value, s.at, s.stale)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Load.Failed(e)
        }
    }

    if (handle == null) {
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ScreenTitle("LeetCode", meta = "CODING PROFILE", modifier = Modifier.entrance(0)) }
            item {
                EmptyState(
                    "No Handle Yet",
                    "Add your LeetCode username in Me → Settings. The board fills itself from LeetCode's public API; nothing is written back.",
                    Modifier.entrance(1),
                ) { LongswordMark(size = 120.dp) }
            }
        }
        return
    }

    val stats = (state as? Load.Ok)?.value
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle("LeetCode", meta = "@$handle", modifier = Modifier.entrance(0)) }
        item { FreshnessLine(state, Modifier.entrance(1)) }
        if (stats == null) {
            item { StateBox(state, onRetry = { attempt++ }) }
            return@LazyColumn
        }
        item {
            Card2(Modifier.entrance(2)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SolvedRing(easy = stats.easy, medium = stats.medium, hard = stats.hard, total = stats.total, size = 124.dp)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MonoText("EASY", color = Bone, modifier = Modifier.width(72.dp))
                            MonoText("${stats.easy}", medium = true)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            MonoText("MEDIUM", color = Violet, modifier = Modifier.width(72.dp))
                            MonoText("${stats.medium}", medium = true)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            MonoText("HARD", color = Crimson, modifier = Modifier.width(72.dp))
                            MonoText("${stats.hard}", medium = true)
                        }
                        Box(
                            Modifier
                                .padding(top = 12.dp)
                                .border(2.dp, LineStrong)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            MonoText("STREAK ${stats.streak} ${if (stats.streak == 1) "DAY" else "DAYS"}", color = Bone)
                        }
                    }
                    LongswordMark(size = 84.dp, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
        item {
            Card2(Modifier.entrance(3)) {
                MonoText("SUBMISSIONS · LAST 22 WEEKS")
                Spacer(Modifier.height(12.dp))
                Heatmap(stats, Modifier.fillMaxWidth().aspectRatio(327f / 102f))
            }
        }
        if (stats.topics.isNotEmpty()) {
            item {
                Card2(Modifier.entrance(4)) {
                    MonoText("TOPICS SOLVED")
                    Spacer(Modifier.height(12.dp))
                    TopicsBars(stats.topics)
                }
            }
        }
        if (stats.recent.isNotEmpty()) {
            item { SectionHead("Recent", Modifier.padding(top = 10.dp, bottom = 4.dp).entrance(5)) }
            items(stats.recent, key = { it.slug + "/" + it.at }) { row ->
                RecentRow(row, Modifier.entrance(5))
            }
        }
        if (stats.total == 0) {
            item { MonoText("No accepted submissions show on LeetCode's public graph yet.", Modifier.entrance(5)) }
        }
    }
}

/** the board's donut: easy, medium and hard as three arcs around the accepted total */
@Composable
private fun SolvedRing(easy: Int, medium: Int, hard: Int, total: Int, size: Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val u = this.size.minDimension / 100f
            val stroke = 11f * u
            val r = 40f * u
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            drawCircle(Line, r, c, style = Stroke(stroke))
            if (total > 0) {
                var start = -90f
                listOf(easy to Bone, medium to Violet, hard to Crimson).forEach { (n, color) ->
                    val sweep = n * 360f / total
                    if (sweep > 0f) {
                        drawArc(
                            color = color,
                            startAngle = start,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(c.x - r, c.y - r),
                            size = Size(2f * r, 2f * r),
                            style = Stroke(stroke),
                        )
                    }
                    start += sweep
                }
            }
        }
        D("$total", (size.value * 36f / 132f).sp)
    }
}

/** 22 weeks × 7 days of accepted-submission counts, oldest week first, Sunday on top */
@Composable
private fun Heatmap(stats: LeetStats, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = this.size.width / 327f
        val cell = 12f * u
        val gap = 3f * u
        for (i in stats.days.indices) {
            val col = i / 7
            val row = i % 7
            drawRect(
                color = heat(stats.days[i]),
                topLeft = Offset(col * (cell + gap), row * (cell + gap)),
                size = Size(cell, cell),
            )
        }
    }
}

private fun heat(n: Int): Color = when {
    n <= 0 -> Surface2
    n < 5 -> Night
    n < 10 -> HeatMid
    n < 15 -> Violet
    else -> HeatMax
}

@Composable
private fun TopicsBars(topics: List<LeetTopic>) {
    val max = topics.maxOf { it.solved }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        topics.forEach { topic ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonoText(topic.name.uppercase(Locale.ENGLISH), modifier = Modifier.width(88.dp))
                Box(Modifier.weight(1f).height(10.dp).background(Line)) {
                    Box(
                        Modifier
                            .fillMaxWidth(topic.solved / max.toFloat())
                            .fillMaxHeight()
                            .background(Violet),
                    )
                }
                MonoText("${topic.solved}", modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}

/**
 * One accepted submission. The accepted-only endpoint carries no difficulty, so
 * the row says the age and the verdict and nothing it cannot know.
 */
@Composable
private fun RecentRow(row: LeetRecent, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            BodyText(row.title)
            MonoText(ago(row.at), color = Muted, modifier = Modifier.padding(top = 2.dp))
        }
        Stamp("ACCEPTED")
    }
}
