package com.jiitkiosk.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.LocalReducedMotion
import com.jiitkiosk.app.ui.Motion
import com.jiitkiosk.app.ui.entrance
import com.jiitkiosk.app.ui.rememberSpin
import com.jiitkiosk.app.ui.shimmer
import com.jiitkiosk.app.ui.tap
import com.jiitkiosk.app.ui.theme.Bad
import com.jiitkiosk.app.ui.theme.BasilicaMark
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.CoffeeMark
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.Cursed
import com.jiitkiosk.app.ui.theme.DeadSunMark
import com.jiitkiosk.app.ui.theme.Display
import com.jiitkiosk.app.ui.theme.GlitchMark
import com.jiitkiosk.app.ui.theme.IconAttendance
import com.jiitkiosk.app.ui.theme.IconBack
import com.jiitkiosk.app.ui.theme.IconClass
import com.jiitkiosk.app.ui.theme.IconCode
import com.jiitkiosk.app.ui.theme.IconExams
import com.jiitkiosk.app.ui.theme.IconMe
import com.jiitkiosk.app.ui.theme.IconNext
import com.jiitkiosk.app.ui.theme.Ink
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.LineStrong
import com.jiitkiosk.app.ui.theme.MarkRing
import com.jiitkiosk.app.ui.theme.Mono
import com.jiitkiosk.app.ui.theme.Muted
import com.jiitkiosk.app.ui.theme.Sans
import com.jiitkiosk.app.ui.theme.Steel
import com.jiitkiosk.app.ui.theme.Surface1
import com.jiitkiosk.app.ui.theme.VinylMark
import com.jiitkiosk.app.ui.theme.cssGradient
import com.jiitkiosk.app.ui.theme.halftone
import `in`.codelif.ktjiit.http.PortalException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import `in`.codelif.ktjiit.api.Portal
import kotlin.math.abs
import kotlin.math.roundToInt

// ------------------------------------------------------------------- type

/** `.d` — condensed and uppercase; the size is per call site, as in the design */
@Composable
fun D(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Bone,
    maxLines: Int = 2,
    align: TextAlign? = null,
) {
    Text(
        text = text.uppercase(),
        style = Display.copy(fontSize = size),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
        modifier = modifier,
    )
}

/** `.m` — the 11px mono caption */
@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Muted,
    medium: Boolean = false,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        style = Mono.copy(fontWeight = if (medium) FontWeight.Medium else FontWeight.Normal),
        color = color,
        textAlign = align,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** the sans body, 15px unless told otherwise */
@Composable
fun BodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Bone,
    size: TextUnit = 15.sp,
    medium: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        style = Sans.copy(fontSize = size, fontWeight = if (medium) FontWeight.Medium else FontWeight.Normal),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

// -------------------------------------------------------------- surfaces

/** a hairline rule, drawn on the bottom edge of whatever it is applied to */
private fun Modifier.underline(): Modifier = drawBehind {
    val y = size.height - 0.5f.dp.toPx()
    drawLine(Line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
}

/** `.c` — a 2px-bordered block, square corners, nothing rounded anywhere.
 *  [art] is the ghost motif the v2 artboards put behind a card's top-right corner. */
@Composable
fun Card2(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    art: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(2.dp, Line)
            .then(if (onClick != null) Modifier.tap(onClick) else Modifier),
    ) {
        if (art != null) {
            Box(Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-12).dp).alpha(0.16f)) { art() }
        }
        Column(Modifier.padding(12.dp), content = content)
    }
}

/** `.st` — the solid stamp, bone by default, tinted for the three verdicts */
@Composable
fun Stamp(text: String, modifier: Modifier = Modifier, background: Color = Bone, color: Color = Ink) {
    Text(
        text = text.uppercase(),
        style = Mono.copy(fontWeight = FontWeight.Medium),
        color = color,
        modifier = modifier.background(background).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** `.r` — a 48px row, label at the left, anything you like at the right */
@Composable
fun RowLine(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    labelColor: Color = Bone,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.tap(onClick) else Modifier)
            .heightIn(min = 48.dp)
            .underline(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BodyText(label, color = labelColor, modifier = Modifier.weight(1f).padding(end = 12.dp))
        trailing()
    }
}

/** a label on the left and a mono value hard against the right edge */
@Composable
fun KeyRow(label: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.tap(onClick) else Modifier)
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp)
            .underline(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BodyText(label, color = Muted, modifier = Modifier.weight(1f))
        Text(
            text = value.ifBlank { "—" },
            style = Mono.copy(fontWeight = FontWeight.Medium),
            color = if (value.isBlank()) Muted else Bone,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f).padding(start = 12.dp),
        )
    }
}

/** the tappable rows that open a sub-screen, `Grade cards ›` in the mockups */
@Composable
fun NavRow(label: String, modifier: Modifier = Modifier, detail: String? = null, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .tap(onClick)
            .heightIn(min = 48.dp)
            .underline(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BodyText(label, modifier = Modifier.weight(1f))
        if (!detail.isNullOrBlank()) {
            MonoText(detail, modifier = Modifier.padding(end = 8.dp))
        }
        Icon(IconNext, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun BackRow(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).tap(onBack),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(IconBack, contentDescription = "Back", tint = Bone)
    }
}

/** a row with a mono line under the title and a mono value hard right */
@Composable
fun DetailRow(
    title: String,
    detail: String = "",
    trailing: String = "",
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    titleColor: Color = Bone,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.tap(onClick) else Modifier)
            .padding(vertical = 10.dp)
            .underline(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BodyText(title, color = titleColor, maxLines = 2)
            if (detail.isNotBlank()) MonoText(detail, modifier = Modifier.padding(top = 2.dp))
        }
        if (trailing.isNotBlank()) {
            MonoText(trailing, color = Bone, medium = true, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

/** the 56px screen heading with its mono line underneath */
@Composable
fun ScreenTitle(title: String, meta: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        D(title, 56.sp)
        if (!meta.isNullOrBlank()) {
            MonoText(meta, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** the small condensed heading that introduces a group */
@Composable
fun SectionHead(text: String, modifier: Modifier = Modifier) {
    D(text, 22.sp, color = Muted, maxLines = 1, modifier = modifier)
}

/** a stamp row for picking a semester */
@Composable
fun ChipRow(labels: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            val bg by animateColorAsState(
                targetValue = if (on) Crimson else Line,
                animationSpec = tween(Motion.FAST),
                label = "chip",
            )
            Box(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .tap { onSelect(i) }
                    .background(bg)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label.uppercase(),
                    style = Mono.copy(fontWeight = FontWeight.Medium),
                    color = Bone,
                )
            }
        }
    }
}

/**
 * The Class tab's three-way switch: one bone frame, three equal panes, the
 * taken one filled crimson — board 09, verbatim.
 */
@Composable
fun SegmentedSwitch(
    labels: List<String>,
    selected: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    Row(modifier.fillMaxWidth().border(2.dp, Bone)) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(if (on) Crimson else Color.Transparent)
                    .tap { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                D(label, 20.sp)
            }
        }
    }
}

/**
 * The hero panels: a CSS gradient, the halftone over it, a line-art mark, then
 * content, with the 2px frame drawn last so nothing bleeds over it.
 */
@Composable
fun HeroPanel(
    height: Dp,
    angle: Float,
    stops: Array<Pair<Float, Color>>,
    modifier: Modifier = Modifier,
    border: Color = Bone,
    art: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    // the art is meant to run off the panel's edge, so the panel does the cutting
    Box(modifier.fillMaxWidth().height(height).clipToBounds()) {
        Box(Modifier.matchParentSize().cssGradient(angle, stops).halftone())
        art()
        content()
        Box(Modifier.matchParentSize().border(2.dp, border))
    }
}

// ------------------------------------------------------------------ nav

enum class Tab(val label: String, val icon: ImageVector) {
    Attendance("Attend", IconAttendance),
    Exams("Exams", IconExams),
    Class("Class", IconClass),
    Code("Code", IconCode),
    Me("Me", IconMe),
}

/**
 * The bottom bar. The crimson pill is one shared element: it slides between the
 * five tabs on a soft spring and stretches slightly while it travels, and the
 * icons and labels crossfade as it arrives.
 */
@Composable
fun BottomNav(current: Tab, modifier: Modifier = Modifier, onSelect: (Tab) -> Unit) {
    val tabs = Tab.entries
    val target = tabs.indexOf(current)
    val x = remember { Animatable(target.toFloat()) }
    LaunchedEffect(target) { x.animateTo(target.toFloat(), Motion.soft) }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val slot = maxWidth / tabs.size
        val slotPx = with(LocalDensity.current) { slot.toPx() }
        Box(
            Modifier
                .offset { IntOffset((x.value * slotPx).roundToInt(), 12.dp.roundToPx()) }
                .width(slot)
                .height(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(52.dp, 32.dp)
                    .graphicsLayer {
                        // stretched while the pill is still travelling, at rest it is exactly 52
                        val stretch = 1f + 0.18f * abs(x.value - target)
                        scaleX = stretch
                    }
                    .background(Crimson, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            tabs.forEach { tab ->
                val on = tab == current
                val tint by animateColorAsState(
                    targetValue = if (on) Bone else Muted,
                    animationSpec = tween(Motion.FAST),
                    label = "tint",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .tap { onSelect(tab) }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(Modifier.width(52.dp).height(32.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Text(tab.label, style = Sans.copy(fontSize = 11.sp), color = tint)
                }
            }
        }
    }
}

// --------------------------------------------------------------- loading

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Failed(val error: Throwable) : Load<Nothing>

    /**
     * [at] is when the value was really fetched, [stale] means the last refresh
     * failed and this is the cached copy, [refreshing] means a newer answer is
     * on its way.
     */
    data class Ok<T>(
        val value: T,
        val at: Long,
        val stale: Boolean = false,
        val refreshing: Boolean = false,
    ) : Load<T>
}

/** a plain call, nothing kept */
@Composable
fun <T> rememberLoad(vararg keys: Any?, block: suspend () -> T): Load<T> {
    var state by remember(*keys) { mutableStateOf<Load<T>>(Load.Loading) }
    LaunchedEffect(*keys) {
        state = try {
            Load.Ok(block(), System.currentTimeMillis())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Load.Failed(e)
        }
    }
    return state
}

/**
 * Loads through the on-disk cache. The seed is read from disk synchronously, so
 * a screen paints its last known data on the first frame instead of flashing a
 * spinner, and the portal is asked again behind it.
 */
@Composable
fun <T> rememberCached(
    vm: KioskViewModel,
    key: String,
    ser: KSerializer<T>,
    attempt: Int = 0,
    enabled: Boolean = true,
    block: suspend (Portal) -> T,
): Load<T> {
    val seed = remember(attempt, key, enabled) { if (enabled) vm.lastGood(key, ser) else null }
    var state by remember(attempt, key, enabled) {
        mutableStateOf<Load<T>>(
            seed?.let { Load.Ok(it.value, it.at, stale = false, refreshing = true) } ?: Load.Loading,
        )
    }
    LaunchedEffect(attempt, key, enabled) {
        if (!enabled) return@LaunchedEffect
        state = try {
            val s = vm.cached(key, ser, block)
            Load.Ok(s.value, s.at, s.stale)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Load.Failed(e)
        }
    }
    return state
}

/** [rememberCached] for the sources that are not the portal (Classroom today) */
@Composable
fun <T> rememberKept(
    vm: KioskViewModel,
    key: String,
    ser: KSerializer<T>,
    attempt: Int = 0,
    enabled: Boolean = true,
    block: suspend () -> T,
): Load<T> {
    val seed = remember(attempt, key, enabled) { if (enabled) vm.lastGood(key, ser) else null }
    var state by remember(attempt, key, enabled) {
        mutableStateOf<Load<T>>(
            seed?.let { Load.Ok(it.value, it.at, stale = false, refreshing = true) } ?: Load.Loading,
        )
    }
    LaunchedEffect(attempt, key, enabled) {
        if (!enabled) return@LaunchedEffect
        state = try {
            val s = vm.keep(key, ser, block)
            Load.Ok(s.value, s.at, s.stale)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Load.Failed(e)
        }
    }
    return state
}

/** for the calls that are allowed to come back empty: hostel, bank, fees */
suspend fun <T> softCall(fallback: T, block: suspend () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        fallback
    }

/**
 * Like [softCall] but keeps the reason. A screen that tolerates an empty hostel
 * still has to be able to tell "the portal has nothing" from "the portal never
 * answered", or it will cache the empty answer and its freshness line will lie.
 */
suspend fun <T> attempt(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

fun friendly(e: Throwable): String = when (e) {
    is PortalException.SessionExpired -> "Your portal session ended. Sign in again."
    is PortalException.Forbidden -> "The portal refused that request."
    is PortalException.ServerUnavailable -> "The portal is down right now (${e.code}). Try again shortly."
    is PortalException.PortalError -> e.errors.joinToString("; ").ifEmpty { "The portal returned an error." }
    is PortalException.EmptyResponse -> "The portal returned nothing for that."
    is PortalException.Untrusted -> "The portal's certificate could not be verified."
    is PortalException.Network -> "No connection to the portal."
    else -> e.message?.takeIf { it.isNotBlank() } ?: "Something went wrong."
}

@Composable
fun StateBox(state: Load<*>, onRetry: (() -> Unit)? = null) {
    when (state) {
        is Load.Loading -> SkeletonScreen()

        is Load.Failed -> Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clipToBounds()
                    .background(Cursed)
                    .halftone()
                    .border(2.dp, Bone),
                contentAlignment = Alignment.Center,
            ) {
                // one motif per failure kind, as the state boards pair them
                when (state.error) {
                    is PortalException.Network -> DeadSunMark(size = 190.dp)
                    is PortalException.Forbidden -> CoffeeMark(size = 220.dp)
                    else -> GlitchMark(size = 220.dp)
                }
            }
            MonoText("COULDN'T LOAD", color = Bad, medium = true, modifier = Modifier.padding(top = 14.dp))
            Spacer(Modifier.height(6.dp))
            D(
                when (state.error) {
                    is PortalException.Network -> "The sun is dead"
                    is PortalException.Forbidden -> "Not for you"
                    else -> "Glitch"
                },
                34.sp,
            )
            Spacer(Modifier.height(6.dp))
            BodyText(friendly(state.error), color = Muted)
            if (onRetry != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "RETRY",
                    style = Mono.copy(fontWeight = FontWeight.Medium),
                    color = Ink,
                    modifier = Modifier
                        .background(Bone)
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        is Load.Ok -> Unit
    }
}

/** the full-screen version, for when there is nothing to show behind it */
@Composable
fun FullState(state: Load<*>, onRetry: (() -> Unit)? = null) {
    if (state is Load.Loading) {
        // board 24: the syncing panel, its basilica, the mono line under it
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clipToBounds()
                    .background(Cursed)
                    .halftone()
                    .border(2.dp, Bone),
                contentAlignment = Alignment.Center,
            ) { BasilicaMark(size = 170.dp) }
            MonoText("SYNCING…", modifier = Modifier.padding(top = 14.dp))
        }
        return
    }
    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        StateBox(state, onRetry)
    }
}

// ------------------------------------------------------------- data age

/** "4 MIN AGO", for the freshness line every data screen carries */
fun ago(at: Long): String {
    val min = ((System.currentTimeMillis() - at) / 60_000L).coerceAtLeast(0L)
    return when {
        min < 1L -> "JUST NOW"
        min < 60L -> "$min MIN AGO"
        min < 1440L -> "${min / 60L}H AGO"
        else -> "${min / 1440L}D AGO"
    }
}

/**
 * The design system's freshness line. When the refresh failed but there is
 * cached data on screen this turns into the crimson failure banner instead of
 * throwing the data away.
 */
@Composable
fun FreshnessLine(
    at: Long,
    stale: Boolean,
    refreshing: Boolean = false,
    modifier: Modifier = Modifier,
    extra: String? = null,
) {
    val head = when {
        stale -> "SYNC FAILED · SHOWING DATA FROM ${ago(at)}"
        refreshing -> "SYNCING · LAST SYNC ${ago(at)}"
        else -> "SYNCED ${ago(at)}"
    }
    val tail = extra?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
    Crossfade(targetState = head + tail, animationSpec = tween(Motion.FAST), label = "freshness") { text ->
        MonoText(text = text, color = if (stale) Bad else Muted, medium = stale, modifier = modifier, maxLines = 1)
    }
}

/**
 * Pull to refresh: the record is dragged down by the finger and spins up while
 * the sync runs. Only a pull puts it on screen — a background sync never does —
 * and it goes back up when the sync that pull started has ended.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshBox(
    syncing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pull = rememberPullToRefreshState()
    var pulled by remember { mutableStateOf(false) }
    LaunchedEffect(pulled, syncing) {
        if (pulled && !syncing) pulled = false
    }
    val spin = rememberSpin(active = pulled, idle = false)
    PullToRefreshBox(
        isRefreshing = pulled,
        onRefresh = { pulled = true; onRefresh() },
        modifier = modifier,
        state = pull,
        indicator = {
            // distanceFraction is the finger's pull: 0 when hidden, 1 held at the
            // threshold while the pull's sync runs.
            val p = pull.distanceFraction
            Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.graphicsLayer {
                        alpha = p
                        translationY = (p - 1f) * 56.dp.toPx()
                        rotationZ = spin.value
                    },
                ) {
                    VinylMark(label = Crimson, groove = MarkRing, disc = Ink, size = 32.dp)
                }
            }
        },
    ) { content() }
}

/** the same line for the screens that only have the loaded state to hand */
@Composable
fun FreshnessLine(state: Load<*>, modifier: Modifier = Modifier) {
    if (state is Load.Ok) FreshnessLine(state.at, state.stale, state.refreshing, modifier)
}

// ------------------------------------------------------------ small parts

/** a card with a mono label over a display value */
@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(MaterialTheme.colorScheme.surfaceContainer).border(2.dp, Line).padding(12.dp),
    ) {
        MonoText(label.uppercase())
        Spacer(Modifier.height(6.dp))
        // Numeric values use the artboard's 32sp display type. A missing-data
        // label has to remain fully readable inside a third-width card, so it
        // intentionally switches to the compact metadata treatment instead of
        // being ellipsized into an ambiguous value.
        if (value.toDoubleOrNull() == null) MonoText(value, color = Bone, medium = true)
        else D(value, 32.sp, maxLines = 1)
    }
}

/** the 44x26 switch from the design system: square thumb, crimson when on. It is
 *  only the picture of a switch: the row around it carries the tap and the switch
 *  semantics, so the target is never smaller than the design's 48dp row. */
@Composable
fun Toggle(checked: Boolean, modifier: Modifier = Modifier) {
    val track by animateColorAsState(
        targetValue = if (checked) Crimson else Line,
        animationSpec = tween(Motion.FAST),
        label = "track",
    )
    val thumb by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = Motion.snappy,
        label = "thumb",
    )
    Box(modifier.size(44.dp, 26.dp).background(track)) {
        Box(
            Modifier
                .padding(3.dp)
                .size(20.dp)
                .graphicsLayer { translationX = 18.dp.toPx() * thumb }
                .background(Bone),
        )
    }
}

/** full width, 52dp, warm white, no radius */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(if (enabled) Bone else Line)
            .then(if (enabled) Modifier.tap(onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            style = Sans.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            color = if (enabled) Ink else Muted,
        )
    }
}

/** the state panel, as the boards draw it: a cursed slab with the motif, the words beneath */
@Composable
fun EmptyState(
    heading: String,
    sentence: String,
    modifier: Modifier = Modifier,
    art: @Composable BoxScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clipToBounds()
                .background(Cursed)
                .halftone()
                .border(2.dp, Bone),
            contentAlignment = Alignment.Center,
        ) { art() }
        D(heading, 34.sp, modifier = Modifier.padding(top = 14.dp))
        Spacer(Modifier.height(8.dp))
        BodyText(sentence, color = Muted)
    }
}

fun Modifier.dashedBorder(color: Color, width: Dp = 2.dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawRect(
        color = color,
        topLeft = Offset(stroke / 2f, stroke / 2f),
        size = Size(size.width - stroke, size.height - stroke),
        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))),
    )
}

// ----------------------------------------------------------- loading shapes

/** a slab in the shape of the content that is coming: surface, no border, one shimmer */
@Composable
fun SkeletonBlock(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .shimmer(),
    )
}

/** the loading state: a hero slab and stacked cards, as the real screens lay out */
@Composable
fun SkeletonScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonBlock(250.dp)
        SkeletonBlock(72.dp)
        SkeletonBlock(72.dp)
        SkeletonBlock(72.dp)
    }
}

// --------------------------------------------------------------- collapsing

/** the list's scroll depth in px; a scrolled-away first item counts as fully collapsed */
fun LazyListState.scrollDepth(): Float =
    if (firstVisibleItemIndex > 0) 10_000f else firstVisibleItemScrollOffset.toFloat()

/**
 * The 56sp display title pinned above a list: it shrinks to 22sp as the list
 * scrolls ([offset] is the scroll depth in px) and its slot — the freshness line —
 * fades and collapses away. Only this composable re-reads the scroll.
 */
@Composable
fun CollapsingTitle(
    title: String,
    offset: () -> Float,
    modifier: Modifier = Modifier,
    collapseAt: Float = 150f,
    meta: (@Composable () -> Unit)? = null,
) {
    val p = (offset() / collapseAt).coerceIn(0f, 1f)
    // the meta slot must fit the real line height at any font scale; fixed 16dp clipped it at 2x
    val scale = LocalDensity.current.fontScale
    Column(modifier.fillMaxWidth()) {
        D(title, (56f + (22f - 56f) * p).sp, maxLines = 1)
        if (meta != null) {
            Box(
                Modifier
                    .padding(top = 8.dp * (1f - p))
                    .height(16.dp * (1f - p) * scale)
                    .graphicsLayer { alpha = 1f - p }
                    .clipToBounds(),
            ) { meta() }
        }
    }
}

// ------------------------------------------------------------------ data viz

/** 4 → "4", 10.5 → "10.5": marks are written the way the portal writes them */
fun num(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

/**
 * The segmented score meter: one 12dp block per mark, 2dp gaps, half blocks
 * allowed, filled left to right at about 15ms a block. The score is always
 * written out as text beside it.
 */
@Composable
fun ScoreMeter(
    label: String,
    score: Double,
    outOf: Double,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalReducedMotion.current
    val blocks = 20
    val target = (score / outOf * blocks).coerceIn(0.0, blocks.toDouble())
    val fill = remember { Animatable(if (reduced) target.toFloat() else 0f) }
    LaunchedEffect(target) {
        if (reduced) {
            fill.snapTo(target.toFloat())
        } else {
            fill.animateTo(
                targetValue = target.toFloat(),
                animationSpec = tween(
                    (target * 15).toInt().coerceAtLeast(Motion.FAST),
                    easing = Motion.standard,
                ),
            )
        }
    }
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BodyText(label.uppercase(), size = 13.sp, modifier = Modifier.width(64.dp), maxLines = 1)
        Canvas(
            Modifier
                .weight(1f)
                .height(12.dp)
                .clearAndSetSemantics { contentDescription = "${num(score)} of ${num(outOf)}" },
        ) {
            val gap = 2.dp.toPx()
            val w = (size.width - gap * (blocks - 1)) / blocks
            repeat(blocks) { i ->
                val x = i * (w + gap)
                drawRect(Line, Offset(x, 0f), Size(w, size.height))
                val f = (fill.value - i).coerceIn(0f, 1f)
                if (f > 0f) drawRect(Bone, Offset(x, 0f), Size(w * f, size.height))
            }
        }
        MonoText("${num(score)}/${num(outOf)}", color = Bone, modifier = Modifier.width(56.dp), align = TextAlign.End)
    }
}

/** one bar of a marks chart; a null value means "not yet", drawn as a bare dash */
data class Bar(val label: String, val value: Double?)

/**
 * The marks bar chart: the dashed threshold line draws first, then the bars grow
 * from the baseline, each value label appearing as its bar lands. Bars under the
 * threshold are crimson and get one soft pulse after the last one lands. Values
 * and axis labels are always text, and the whole chart carries a text equivalent.
 */
@Composable
fun BarChart(
    bars: List<Bar>,
    modifier: Modifier = Modifier,
    height: Dp = 134.dp,
    max: Double = 20.0,
    threshold: Double = 10.0,
    thresholdLabel: String = "HALF",
) {
    val reduced = LocalReducedMotion.current
    val grow = remember(bars) { bars.map { Animatable(if (reduced) 1f else 0f) } }
    val line = remember { Animatable(if (reduced) 1f else 0f) }
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(bars) {
        if (reduced) {
            grow.forEach { it.snapTo(1f) }
            line.snapTo(1f)
        } else {
            line.animateTo(1f, tween(Motion.FAST, easing = Motion.standard))
            grow.forEachIndexed { i, bar ->
                launch {
                    delay((i * 60).toLong())
                    bar.animateTo(1f, tween(Motion.SLOW, easing = Motion.emphasizedDecelerate))
                }
            }
            delay((grow.size * 60 + Motion.SLOW).toLong())
            pulse.animateTo(1f, tween(200, easing = Motion.standard))
            pulse.animateTo(0f, tween(400, easing = Motion.standard))
        }
    }
    val measurer = rememberTextMeasurer()
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clearAndSetSemantics {
                contentDescription = bars.joinToString(", ") { bar ->
                    if (bar.value == null) "${bar.label} not yet" else "${bar.label} ${num(bar.value)}"
                }
            },
    ) {
        val labelH = 22.dp.toPx()
        val valueH = 18.dp.toPx()
        val baseline = size.height - labelH
        val scale = (baseline - valueH) / max.toFloat()
        val n = bars.size.coerceAtLeast(1)
        val pitch = size.width / n
        val barW = pitch * 0.6f

        val ty = baseline - threshold.toFloat() * scale
        val halfW = size.width / 2f * line.value
        drawLine(
            color = Muted,
            start = Offset(size.width / 2f - halfW, ty),
            end = Offset(size.width / 2f + halfW, ty),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
        )
        val halfLabel = measurer.measure(
            AnnotatedString(thresholdLabel),
            style = Mono.copy(fontSize = 9.sp, color = Muted),
        )
        drawText(
            textLayoutResult = halfLabel,
            alpha = line.value,
            topLeft = Offset(size.width - halfLabel.size.width, ty - halfLabel.size.height - 2.dp.toPx()),
        )

        bars.forEachIndexed { i, bar ->
            val cx = pitch * i + pitch / 2f
            val v = grow[i].value
            val value = bar.value
            val under = value != null && value < threshold
            if (value != null) {
                val h = (value * scale).toFloat() * v
                val bump = if (under) 1f + 0.05f * pulse.value else 1f
                drawRect(
                    color = if (under) Bad else Bone,
                    topLeft = Offset(cx - barW / 2f, baseline - h * bump),
                    size = Size(barW, h * bump),
                )
            }
            val label = measurer.measure(
                AnnotatedString(if (value == null) "–" else num(value)),
                style = Mono.copy(fontSize = 11.sp, color = Bone),
            )
            val top = if (value == null) baseline - label.size.height - 4.dp.toPx()
            else baseline - (value * scale).toFloat() - label.size.height - 4.dp.toPx()
            drawText(
                textLayoutResult = label,
                alpha = ((v - 0.85f) / 0.15f).coerceIn(0f, 1f),
                topLeft = Offset(cx - label.size.width / 2f, top),
            )
            val axis = measurer.measure(
                AnnotatedString(bar.label),
                style = Mono.copy(fontSize = 9.sp, color = Muted),
            )
            drawText(
                textLayoutResult = axis,
                topLeft = Offset(cx - axis.size.width / 2f, baseline + 6.dp.toPx()),
            )
        }
    }
}

/** a grade letter in the design's 44dp square: filled for B+ or better, outline below */
@Composable
fun GradeBadge(grade: String, modifier: Modifier = Modifier) {
    val filled = filledGrade(grade)
    Box(
        modifier
            .size(44.dp)
            .background(if (filled) Bone else Color.Transparent)
            .border(2.dp, Bone),
        contentAlignment = Alignment.Center,
    ) {
        D(grade, 24.sp, color = if (filled) Ink else Bone, maxLines = 1)
    }
}

/** the design's rule: B+ and above is a filled square, everything else outline */
fun filledGrade(grade: String): Boolean =
    grade.trim().uppercase() in setOf("B+", "A-", "A", "A+", "O", "S")

/** one 8dp square per credit, right-aligned on a course row */
@Composable
fun CreditPips(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.clearAndSetSemantics { contentDescription = "$count credits" },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(count.coerceIn(0, 12)) { Box(Modifier.size(8.dp).background(Bone)) }
    }
}

/** one day of the exams week strip */
data class DayCell(
    val weekday: String,
    val date: Int,
    val today: Boolean = false,
    val hasExam: Boolean = false,
)

/** the seven-day strip: today is crimson, exam days carry a white frame and a dot */
@Composable
fun WeekStrip(days: List<DayCell>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { day ->
            Column(
                Modifier
                    .weight(1f)
                    .background(if (day.today) Crimson else Color.Transparent)
                    .border(2.dp, if (day.today || day.hasExam) Bone else Line)
                    .padding(vertical = 8.dp)
                    .clearAndSetSemantics {
                        contentDescription = buildString {
                            append(day.weekday)
                            append(' ')
                            append(day.date)
                            if (day.today) append(", today")
                            if (day.hasExam) append(", exam")
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MonoText(day.weekday, color = Bone)
                D(day.date.toString(), 24.sp, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                Box(Modifier.padding(top = 6.dp).size(6.dp)) {
                    if (day.hasExam) Box(Modifier.fillMaxSize().background(Bone))
                }
            }
        }
    }
}

/** a paper in the "Then" list: a date block, then a card with its ghost motif */
@Composable
fun TimelineEvent(
    day: String,
    month: String,
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    tag: String? = null,
    room: String? = null,
    art: @Composable BoxScope.() -> Unit,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            Modifier.width(56.dp).border(2.dp, Bone).padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            D(day, 32.sp, maxLines = 1)
            MonoText(month, color = Bone)
        }
        Card2(art = art, modifier = Modifier.weight(1f)) {
            BodyText(title, size = 17.sp, maxLines = 3)
            MonoText(detail, color = Bone, modifier = Modifier.padding(top = 4.dp))
            if (tag != null || room != null) {
                Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (tag != null) Stamp(tag, background = Line, color = Bone)
                    if (room != null) {
                        MonoText(room, color = if (room.equals("room tba", true)) Muted else Bone)
                    }
                }
            }
        }
    }
}

/**
 * One segment per semester: completed white, current crimson, future grey. The
 * segments land one after another and the current one arrives with a small
 * overshoot. The strip carries the same sentence a screen reader would want.
 */
@Composable
fun SemesterTrack(current: Int, total: Int, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val land = remember(total) { List(total) { Animatable(if (reduced) 1f else 0f) } }
    LaunchedEffect(current, total) {
        land.forEachIndexed { i, segment ->
            val isCurrent = i == current - 1
            if (!reduced) delay((i * 120).toLong())
            if (isCurrent && !reduced) segment.animateTo(1f, Motion.snappy) else segment.snapTo(1f)
        }
    }
    Row(
        modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "Semester $current of $total" },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(total) { i ->
            val color = when {
                i < current - 1 -> Bone
                i == current - 1 -> Crimson
                else -> Line
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(14.dp)
                    .graphicsLayer {
                        val v = land[i].value
                        alpha = v
                        scaleY = 0.5f + 0.5f * v
                    }
                    .background(color),
            )
        }
    }
}

/** bars derived from the enrolment digits: decoration, never scannable */
@Composable
private fun Barcode(seed: String, modifier: Modifier = Modifier) {
    val digits = seed.filter { it.isDigit() }.map { it - '0' }.ifEmpty { listOf(1) }
    Canvas(modifier.fillMaxWidth().height(28.dp).clearAndSetSemantics { }) {
        val u = size.width / 88f
        var x = 0f
        var i = 0
        while (x < size.width) {
            val d = digits[i % digits.size]
            val w = (1 + d % 3) * u
            drawRect(Bone, Offset(x, 0f), Size(w, size.height))
            x += w + (1 + (d + 1) % 2) * u
            i++
        }
    }
}

/**
 * The student file card from the v2 Me and Profile artboards: steel gradient,
 * halftone, the trooper cropped off the right edge, the name at 46sp and the
 * enrolment in mono, with the decorative barcode along the bottom.
 */
@Composable
fun StudentFileCard(
    name: String,
    enrolment: String,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp,
    art: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth().height(height).clipToBounds()) {
        Box(Modifier.matchParentSize().cssGradient(160f, arrayOf(0f to Steel, 0.85f to Ink)).halftone())
        art()
        Column(Modifier.align(Alignment.TopStart).padding(14.dp).fillMaxWidth(0.62f)) {
            Stamp("Student file")
            D(name, 46.sp, modifier = Modifier.padding(top = 12.dp))
            MonoText(enrolment, color = Bone, modifier = Modifier.padding(top = 8.dp))
        }
        Barcode(enrolment, Modifier.align(Alignment.BottomCenter).padding(horizontal = 14.dp).padding(bottom = 12.dp))
        Box(Modifier.matchParentSize().border(2.dp, Bone))
    }
}

/** a hub tile: colour, halftone, a motif cropped top-right, the name bottom-left */
@Composable
fun ArtTile(
    name: String,
    sub: String,
    background: Color,
    rotate: Float = 0f,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    art: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .graphicsLayer { rotationZ = rotate }
            .clipToBounds()
            .background(background)
            .halftone()
            .then(if (onClick != null) Modifier.tap(onClick) else Modifier)
            .border(2.dp, Bone),
    ) {
        art()
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            D(name, 28.sp, maxLines = 1)
            MonoText(sub, color = Bone, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
