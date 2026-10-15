package com.jiitkiosk.app.ui

import android.provider.Settings
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jiitkiosk.app.ui.theme.Crimson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The motion spec, in one place: durations, easings, springs and the stagger.
 * Everything animated in the app comes from here, so "feels the same" is
 * decided once rather than per screen.
 */
object Motion {
    const val INSTANT = 100
    const val FAST = 180
    const val BASE = 280
    const val SLOW = 450
    const val HERO = 700
    const val STAGGER = 40
    const val MAX_STAGGER = 7 // 8 staggered items; the rest arrive together

    /** things entering: fast at the start, settling gently */
    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** things leaving: slow at the start, quick at the end */
    val emphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** in-place value changes */
    val standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** press and selection feedback */
    val snappy: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 500f)

    /** the nav pill and sheets */
    val soft: SpringSpec<Float> = spring(dampingRatio = 0.75f, stiffness = 200f)

    fun enter(duration: Int = BASE) = tween<Float>(duration, easing = emphasizedDecelerate)
    fun exit(duration: Int = BASE) = tween<Float>(duration, easing = emphasizedAccelerate)
}

/**
 * True when the system says "remove animations" (animator duration scale 0).
 * Entrances become plain fades, counters jump, nothing loops.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

// ------------------------------------------------------------------ entrances

/**
 * Fade and a 16dp rise, delayed by [index] * 40ms. Plays once when the caller
 * enters composition — once per navigation entry, and not again on a refresh.
 * The animated values are read in the graphics layer, so a running entrance
 * costs a redraw, never a recomposition.
 */
@Composable
fun Modifier.entrance(index: Int = 0, rise: Dp = 16.dp): Modifier {
    val reduced = LocalReducedMotion.current
    val step = index.coerceAtMost(Motion.MAX_STAGGER)
    var played by rememberSaveable { mutableStateOf(false) }
    val alpha = remember { Animatable(if (played) 1f else 0f) }
    val offset = remember { Animatable(if (played) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (played) return@LaunchedEffect
        played = true
        if (reduced) {
            alpha.animateTo(1f, tween(Motion.INSTANT))
            offset.snapTo(0f)
        } else {
            coroutineScope {
                launch { alpha.animateTo(1f, tween(Motion.BASE, step * Motion.STAGGER, Motion.emphasizedDecelerate)) }
                launch { offset.animateTo(0f, tween(Motion.BASE + 80, step * Motion.STAGGER, Motion.emphasizedDecelerate)) }
            }
        }
    }
    val risePx = with(LocalDensity.current) { rise.toPx() }
    return graphicsLayer {
        this.alpha = alpha.value
        translationY = offset.value * risePx
    }
}

/**
 * Hero art entering: slides in 24dp from the right and settles from 1.04 to its
 * true size. Plays once when the screen is entered, like [entrance].
 */
@Composable
fun Modifier.artIn(delayMs: Int = 0): Modifier {
    val reduced = LocalReducedMotion.current
    var played by rememberSaveable { mutableStateOf(false) }
    val p = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (played) return@LaunchedEffect
        played = true
        if (reduced) {
            p.snapTo(1f)
        } else {
            p.animateTo(1f, tween(Motion.SLOW, delayMs, Motion.emphasizedDecelerate))
        }
    }
    val shift = with(LocalDensity.current) { 24.dp.toPx() }
    return graphicsLayer {
        val v = p.value
        translationX = (1f - v) * shift
        val s = 1f + 0.04f * (1f - v)
        scaleX = s
        scaleY = s
    }
}

/**
 * The design's tap: the element scales to 0.97 and flashes a sharp-cornered
 * crimson wash, no rounded ripple anywhere. Layout and touch bounds never change.
 */
@Composable
fun Modifier.tap(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press = remember { Animatable(0f) }
    LaunchedEffect(pressed) {
        if (pressed) press.animateTo(1f, tween(Motion.INSTANT)) else press.animateTo(0f, Motion.snappy)
    }
    return this
        .clickable(interactionSource = source, indication = null, onClick = onClick)
        .drawWithContent {
            drawContent()
            if (press.value > 0.002f) drawRect(Crimson.copy(alpha = 0.12f * press.value))
        }
        .graphicsLayer {
            val s = 1f - 0.03f * press.value
            scaleX = s
            scaleY = s
        }
}

/** a slow sine for idle floating; nothing runs under reduced motion */
@Composable
fun rememberOrbit(periodMs: Int = 6000): State<Float> {
    if (LocalReducedMotion.current) return remember { mutableFloatStateOf(0.35f) }
    val t = rememberInfiniteTransition(label = "orbit")
    return t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit",
    )
}

/**
 * Degrees for the records that spin: one turn per 20s when idle, one turn per
 * 1.2s while [active] (a sync is running). With [idle] off nothing runs until
 * something is actually syncing.
 */
@Composable
fun rememberSpin(
    active: Boolean,
    idle: Boolean = true,
    idleMs: Int = 20_000,
    activeMs: Int = 1_200,
): State<Float> {
    val reduced = LocalReducedMotion.current
    val angle = remember { Animatable(0f) }
    if (reduced) return angle.asState()
    LaunchedEffect(active, idle) {
        if (!active && !idle) return@LaunchedEffect
        val period = if (active) activeMs else idleMs
        while (true) {
            angle.animateTo(angle.value + 360f, tween(period, easing = LinearEasing))
        }
    }
    return angle.asState()
}

/** the diagonal light that sweeps a skeleton every 1.4s; static under reduced motion */
@Composable
fun Modifier.shimmer(): Modifier {
    if (LocalReducedMotion.current) return this
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer",
    )
    return drawWithContent {
        drawContent()
        val band = size.width * 0.4f
        val left = -band + (size.width + band) * x
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color.Transparent, Color(0x14F0EEE9), Color.Transparent),
                start = Offset(left, 0f),
                end = Offset(left + band, size.height),
            ),
        )
    }
}

/** art inside a scrolling list moves at [factor] x the scroll: parallax without recomposition */
@Composable
fun Modifier.parallax(offset: () -> Float, factor: Float = 0.5f): Modifier = this.graphicsLayer {
    translationY = offset() * factor
}

// ---------------------------------------------------------------- navigation

/**
 * Page swaps inside a tab. Forward: the new page fades in from 24dp right while
 * the old one fades out 8dp to the left. Back is the mirror. [target] is the
 * state being shown — null for a tab's own list — and [forward] says whether we
 * are going deeper, so a data refresh of the same target never replays a flip.
 */
@Composable
fun <T> PageFlip(
    target: T,
    forward: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val shift = with(LocalDensity.current) { 24.dp.roundToPx() }
    val edge = with(LocalDensity.current) { 8.dp.roundToPx() }
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        transitionSpec = {
            val enter = slideInHorizontally(tween(Motion.BASE, easing = Motion.emphasizedDecelerate)) {
                if (forward) shift else -shift
            } + fadeIn(tween(Motion.BASE))
            val exit = slideOutHorizontally(tween(Motion.BASE, easing = Motion.emphasizedAccelerate)) {
                if (forward) -edge else shift
            } + fadeOut(tween(Motion.BASE))
            enter togetherWith exit
        },
        label = "page",
    ) { t -> content(t) }
}

/**
 * Predictive back for a nested page. The screen follows the gesture with a
 * slight scale-down and a sideways shift; [onBack] fires when the gesture
 * completes, and the screen springs back if it is cancelled. On older Android
 * this degrades to a plain back handler.
 */
@Composable
fun BackLayer(enabled: Boolean, onBack: () -> Unit, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    PredictiveBackHandler(enabled) { events ->
        try {
            events.collect { progress.snapTo(it.progress) }
            progress.snapTo(0f)
            onBack()
        } catch (e: CancellationException) {
            progress.animateTo(0f, Motion.snappy)
            throw e
        }
    }
    Box(
        Modifier.graphicsLayer {
            translationX = size.width * 0.12f * progress.value
            val s = 1f - 0.05f * progress.value
            scaleX = s
            scaleY = s
        },
    ) { content() }}

// -------------------------------------------------------------- living values

/** a value that rolls in from the top/bottom when it changes: the countdown */
@Composable
fun RollingText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalReducedMotion.current
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            if (reduced) {
                fadeIn(tween(Motion.INSTANT)) togetherWith fadeOut(tween(Motion.INSTANT))
            } else {
                (slideInVertically(tween(Motion.FAST, easing = Motion.emphasizedDecelerate)) { it } +
                    fadeIn(tween(Motion.FAST))) togetherWith
                    (slideOutVertically(tween(Motion.FAST, easing = Motion.emphasizedAccelerate)) { -it } +
                        fadeOut(tween(Motion.FAST)))
            }
        },
        label = "roll",
    ) { value ->
        Text(text = value, style = style, color = color)
    }
}

/**
 * A number that counts to its target in sync with the entrance, formatted by
 * [format]. Under reduced motion it simply is the target.
 */
@Composable
fun rememberCountUp(target: Double, durationMs: Int = Motion.HERO, delayMs: Int = 0): State<Float> {
    val reduced = LocalReducedMotion.current
    val value = remember { Animatable(if (reduced) target.toFloat() else 0f) }
    LaunchedEffect(target, durationMs, delayMs) {
        if (reduced) {
            value.snapTo(target.toFloat())
        } else {
            if (delayMs > 0) delay(delayMs.toLong())
            value.animateTo(target.toFloat(), tween(durationMs, easing = Motion.emphasizedDecelerate))
        }
    }
    return value.asState()
}
