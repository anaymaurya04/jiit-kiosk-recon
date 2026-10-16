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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.code.CodePrefs
import com.jiitkiosk.app.code.GhLang
import com.jiitkiosk.app.code.GhRepo
import com.jiitkiosk.app.code.GitHubClient
import com.jiitkiosk.app.code.GitHubStats
import com.jiitkiosk.app.code.LEET_TTL_MS
import com.jiitkiosk.app.code.LeetRecent
import com.jiitkiosk.app.code.LeetStats
import com.jiitkiosk.app.code.LeetTopic
import com.jiitkiosk.app.code.LeetCodeClient
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.components.BodyText
import com.jiitkiosk.app.ui.components.Card2
import com.jiitkiosk.app.ui.components.ChipRow
import com.jiitkiosk.app.ui.components.D
import com.jiitkiosk.app.ui.components.EmptyState
import com.jiitkiosk.app.ui.components.FreshnessLine
import com.jiitkiosk.app.ui.components.HeroPanel
import com.jiitkiosk.app.ui.components.Load
import com.jiitkiosk.app.ui.components.MonoText
import com.jiitkiosk.app.ui.components.ScreenTitle
import com.jiitkiosk.app.ui.components.SectionHead
import com.jiitkiosk.app.ui.components.Stamp
import com.jiitkiosk.app.ui.components.StateBox
import com.jiitkiosk.app.ui.components.ago
import com.jiitkiosk.app.ui.entrance
import com.jiitkiosk.app.ui.theme.AccentDeep
import com.jiitkiosk.app.ui.theme.Bad
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.Ink
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.LineStrong
import com.jiitkiosk.app.ui.theme.LongswordMark
import com.jiitkiosk.app.ui.theme.Muted
import com.jiitkiosk.app.ui.theme.Night
import com.jiitkiosk.app.ui.theme.Surface2
import com.jiitkiosk.app.ui.theme.TowerMark
import com.jiitkiosk.app.ui.theme.Violet
import java.util.Locale
import kotlinx.coroutines.CancellationException

// LeetCode's heatmap adds two violet steps between the theme's Night and Violet
private val HeatMid = Color(0xFF4A3A9A)
private val HeatMax = Color(0xFFB0A0F0)
// GitHub's ramp runs the crimson family off the same empty tile
private val GhHeat1 = AccentDeep
private val GhHeat2 = Color(0xFF8A2438)
// the GitHub hero's violet, sampled off board 24
private val GhHeroViolet = Color(0xFF241A3E)

/**
 * The Code tab: the LeetCode and GitHub boards, one chip apart. Numbers come
 * from the two public APIs and both screens say when they were last read.
 */
@Composable
fun CodeScreen(vm: KioskViewModel) {
    val context = LocalContext.current
    val prefs = remember { CodePrefs(context) }
    var source by remember { mutableIntStateOf(0) }
    when (source) {
        0 -> LeetBoard(vm, prefs, source) { source = it }
        else -> GitHubBoard(vm, prefs, source) { source = it }
    }
}

@Composable
private fun CodeHeader(title: String, meta: String, source: Int, onSource: (Int) -> Unit) {
    Column(Modifier.entrance(0)) {
        ScreenTitle(title, meta = meta)
        ChipRow(listOf("LEETCODE", "GITHUB"), source, Modifier.padding(top = 12.dp)) { onSource(it) }
    }
}

private val PAGE_PADDING = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp)

// ---------------------------------------------------------------- LeetCode

/**
 * Board 23. LeetCode's public API is asked at most every 15 minutes; inside
 * that window nothing is fetched and nothing is rewritten, so the freshness
 * line keeps telling the truth about the data's age.
 */
@Composable
private fun LeetBoard(vm: KioskViewModel, prefs: CodePrefs, source: Int, onSource: (Int) -> Unit) {
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
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PAGE_PADDING, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { CodeHeader("LeetCode", "CODING PROFILE", source, onSource) }
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
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PAGE_PADDING, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CodeHeader("LeetCode", "@$handle", source, onSource) }
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
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MonoText("EASY", color = Bone)
                            MonoText("${stats.easy}", medium = true)
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            MonoText("MEDIUM", color = Violet)
                            MonoText("${stats.medium}", medium = true)
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            MonoText("HARD", color = Crimson)
                            MonoText("${stats.hard}", medium = true)
                        }
                        StreakChip(stats.streak, Modifier.padding(top = 12.dp))
                    }
                    LongswordMark(size = 84.dp, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
        item {
            Card2(Modifier.entrance(3)) {
                MonoText("SUBMISSIONS · LAST 22 WEEKS")
                Spacer(Modifier.height(12.dp))
                Heatmap(stats.days, ::leetHeat, Modifier.fillMaxWidth().aspectRatio(327f / 102f))
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

// ------------------------------------------------------------------ GitHub

/** Board 24. REST with ETags, plus the GraphQL contribution graph when a token is saved. */
@Composable
private fun GitHubBoard(vm: KioskViewModel, prefs: CodePrefs, source: Int, onSource: (Int) -> Unit) {
    val handle = remember { prefs.github() }
    val token = remember { prefs.githubToken() }
    var attempt by remember { mutableIntStateOf(0) }

    val key = handle?.let { "github/$it" }
    val ser = GitHubStats.serializer()
    val seed = remember(key, attempt) { key?.let { vm.lastGood(it, ser) } }
    var state by remember(key, attempt) {
        mutableStateOf<Load<GitHubStats>>(seed?.let { Load.Ok(it.value, it.at) } ?: Load.Loading)
    }
    LaunchedEffect(key, attempt) {
        if (key == null || handle == null) return@LaunchedEffect
        state = try {
            val f = GitHubClient.fetch(handle, token, prefs.etags(handle), seed?.value)
            prefs.setEtags(handle, f.etags)
            if (f.changed || seed == null) {
                val s = vm.keep(key, ser) { f.stats }
                Load.Ok(s.value, s.at)
            } else {
                // every endpoint said 304: not one byte changed, so the data keeps its real age
                Load.Ok(seed.value, seed.at)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (seed != null) Load.Ok(seed.value, seed.at, stale = true) else Load.Failed(e)
        }
    }

    if (handle == null) {
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PAGE_PADDING, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { CodeHeader("GitHub", "CODING PROFILE", source, onSource) }
            item {
                EmptyState(
                    "No Handle Yet",
                    "Add your GitHub username in Me → Settings. Public data alone is enough; a token is optional and only lifts the rate limit.",
                    Modifier.entrance(1),
                ) { TowerMark(size = 120.dp, cut = Color.Transparent) }
            }
        }
        return
    }

    val stats = (state as? Load.Ok)?.value
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PAGE_PADDING, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CodeHeader("GitHub", "@$handle", source, onSource) }
        item { FreshnessLine(state, Modifier.entrance(1)) }
        if (stats == null) {
            item { StateBox(state, onRetry = { attempt++ }) }
            return@LazyColumn
        }
        item { GitHubHero(stats, Modifier.entrance(2)) }
        item {
            Card2(Modifier.entrance(3)) {
                MonoText(
                    if (stats.publicActivityOnly) "PUBLIC ACTIVITY ONLY · LAST 22 WEEKS"
                    else "CONTRIBUTIONS · LAST 22 WEEKS",
                )
                Spacer(Modifier.height(12.dp))
                Heatmap(stats.days, ::ghHeat, Modifier.fillMaxWidth().aspectRatio(327f / 102f))
                if (stats.publicActivityOnly) {
                    BodyText(
                        "Without a token this is the public events feed, about 90 days deep.",
                        color = Muted,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
        if (stats.languages.isNotEmpty()) {
            item {
                Card2(Modifier.entrance(4)) {
                    MonoText("LANGUAGES")
                    Spacer(Modifier.height(12.dp))
                    LanguagesBar(stats.languages, stats.otherLangRepos)
                    Spacer(Modifier.height(10.dp))
                    MonoText(
                        (
                            stats.languages.map { it.name.uppercase(Locale.ENGLISH) } +
                                listOfNotNull(if (stats.otherLangRepos > 0) "OTHER" else null)
                            ).joinToString(" · "),
                        color = Muted,
                    )
                }
            }
        }
        if (stats.top.isNotEmpty()) {
            item { SectionHead("Repositories", Modifier.padding(top = 10.dp, bottom = 4.dp).entrance(5)) }
            items(stats.top, key = { it.name }) { repo ->
                RepoCard(repo, Modifier.entrance(5))
            }
        }
    }
}

@Composable
private fun GitHubHero(stats: GitHubStats, modifier: Modifier = Modifier) {
    HeroPanel(
        height = 172.dp,
        angle = 135f,
        stops = arrayOf(0f to GhHeroViolet, 0.9f to Ink),
        modifier = modifier,
    ) {
        TowerMark(
            size = 168.dp,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
            color = Bone,
            accent = Crimson,
            cut = AccentDeep,
        )
        Column(Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                Column {
                    MonoText("REPOS", color = Muted)
                    D("${stats.repos}", 38.sp)
                }
                Column {
                    MonoText("STARS", color = Muted)
                    D("${stats.stars}", 38.sp)
                }
            }
            StreakChip(stats.streak, Modifier.padding(top = 12.dp))
        }
    }
}

/** the languages bar: a weighted strip in the palette's order, leftovers in grey */
@Composable
private fun LanguagesBar(langs: List<GhLang>, other: Int, modifier: Modifier = Modifier) {
    val colors = listOf(Bone, Violet, Crimson)
    Box(modifier.fillMaxWidth().height(10.dp).background(Line)) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            langs.forEachIndexed { i, lang ->
                Box(Modifier.weight(lang.repos.toFloat()).fillMaxHeight().background(colors[i % colors.size]))
            }
            if (other > 0) Box(Modifier.weight(other.toFloat()).fillMaxHeight().background(Muted))
        }
    }
}

@Composable
private fun RepoCard(repo: GhRepo, modifier: Modifier = Modifier) {
    Card2(modifier) {
        MonoText(repo.name, color = Bone, medium = true)
        repo.description?.takeIf { it.isNotBlank() }?.let {
            BodyText(it, color = Muted, modifier = Modifier.padding(top = 4.dp))
        }
        MonoText(
            listOfNotNull(repo.language?.uppercase(Locale.ENGLISH), "★ ${repo.stars}").joinToString(" · "),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

// ------------------------------------------------------------------ shared

@Composable
private fun StreakChip(streak: Int, modifier: Modifier = Modifier) {
    Box(modifier.border(2.dp, LineStrong).padding(horizontal = 10.dp, vertical = 5.dp)) {
        MonoText("STREAK $streak ${if (streak == 1) "DAY" else "DAYS"}", color = Bone)
    }
}

/** 22 weeks × 7 days, oldest week first, Sunday on top */
@Composable
private fun Heatmap(days: List<Int>, level: (Int) -> Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = this.size.width / 327f
        val cell = 12f * u
        val gap = 3f * u
        for (i in days.indices) {
            drawRect(
                color = level(days[i]),
                topLeft = Offset(i / 7 * (cell + gap), i % 7 * (cell + gap)),
                size = Size(cell, cell),
            )
        }
    }
}

private fun leetHeat(n: Int): Color = when {
    n <= 0 -> Surface2
    n < 5 -> Night
    n < 10 -> HeatMid
    n < 15 -> Violet
    else -> HeatMax
}

private fun ghHeat(n: Int): Color = when {
    n <= 0 -> Surface2
    n <= 2 -> GhHeat1
    n <= 5 -> GhHeat2
    n <= 9 -> Crimson
    else -> Bad
}
