package com.jiitkiosk.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.ui.KioskViewModel
import com.jiitkiosk.app.ui.theme.Bad
import com.jiitkiosk.app.ui.theme.Bone
import com.jiitkiosk.app.ui.theme.Crimson
import com.jiitkiosk.app.ui.theme.Display
import com.jiitkiosk.app.ui.theme.IconAttendance
import com.jiitkiosk.app.ui.theme.IconBack
import com.jiitkiosk.app.ui.theme.IconExams
import com.jiitkiosk.app.ui.theme.IconGrades
import com.jiitkiosk.app.ui.theme.IconMe
import com.jiitkiosk.app.ui.theme.IconNext
import com.jiitkiosk.app.ui.theme.Ink
import com.jiitkiosk.app.ui.theme.Line
import com.jiitkiosk.app.ui.theme.LineStrong
import com.jiitkiosk.app.ui.theme.Mono
import com.jiitkiosk.app.ui.theme.Muted
import com.jiitkiosk.app.ui.theme.Sans
import com.jiitkiosk.app.ui.theme.Surface1
import com.jiitkiosk.app.ui.theme.cssGradient
import com.jiitkiosk.app.ui.theme.halftone
import `in`.codelif.ktjiit.http.PortalException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer
import `in`.codelif.ktjiit.api.Portal

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
) {
    Text(
        text = text,
        style = Mono.copy(fontWeight = if (medium) FontWeight.Medium else FontWeight.Normal),
        color = color,
        textAlign = align,
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

/** `.c` — a 2px-bordered block, square corners, nothing rounded anywhere */
@Composable
fun Card2(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(2.dp, Line)
            .padding(12.dp),
        content = content,
    )
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
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
            .clickable(onClick = onClick)
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
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onBack),
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
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
            Text(
                text = label.uppercase(),
                style = Mono.copy(fontWeight = FontWeight.Medium),
                color = Bone,
                modifier = Modifier
                    .clickable { onSelect(i) }
                    .background(if (on) Crimson else Line)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
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
    Attendance("Attendance", IconAttendance),
    Exams("Exams", IconExams),
    Grades("Grades", IconGrades),
    Me("Me", IconMe),
}

@Composable
fun BottomNav(current: Tab, modifier: Modifier = Modifier, onSelect: (Tab) -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { tab ->
            val on = tab == current
            Column(
                modifier = Modifier
                    .clickable { onSelect(tab) }
                    .widthIn(min = 72.dp)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier
                        .background(
                            if (on) com.jiitkiosk.app.ui.theme.Crimson else Color.Transparent,
                            androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        tint = if (on) Bone else Muted,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(tab.label, style = Sans.copy(fontSize = 12.sp), color = if (on) Bone else Muted)
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
        is Load.Loading -> Box(
            Modifier.fillMaxWidth().padding(vertical = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            MonoText("LOADING…")
        }

        is Load.Failed -> Card2 {
            MonoText("COULDN'T LOAD", color = Bad, medium = true)
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
    MonoText(text = head + tail, color = if (stale) Bad else Muted, medium = stale, modifier = modifier)
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
        D(value, 32.sp, maxLines = 1)
    }
}

/** the 44x26 switch from the design system: square thumb, crimson when on. It is
 *  only the picture of a switch: the row around it carries the tap and the switch
 *  semantics, so the target is never smaller than the design's 48dp row. */
@Composable
fun Toggle(checked: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(44.dp, 26.dp)
            .background(if (checked) Crimson else Line),
    ) {
        Box(
            Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(3.dp)
                .size(20.dp)
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
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            style = Sans.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            color = if (enabled) Ink else Muted,
        )
    }
}

/** the dashed empty/error panel: a big grey motif, a heading, one quiet line */
@Composable
fun EmptyState(
    heading: String,
    sentence: String,
    modifier: Modifier = Modifier,
    art: @Composable BoxScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .dashedBorder(LineStrong)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.padding(bottom = 14.dp), contentAlignment = Alignment.Center) { art() }
        D(heading, 28.sp, align = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        BodyText(sentence, color = Muted, modifier = Modifier.padding(horizontal = 8.dp))
    }
}

private fun Modifier.dashedBorder(color: Color, width: Dp = 2.dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawRect(
        color = color,
        topLeft = Offset(stroke / 2f, stroke / 2f),
        size = Size(size.width - stroke, size.height - stroke),
        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))),
    )
}
