package com.jiitkiosk.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.ui.LocalReducedMotion
import com.jiitkiosk.app.ui.Motion
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------- background

/**
 * `radial-gradient(#00000055 1.2px, transparent 1.4px)` at a 7px step, the
 * halftone the mockups lay over every gradient. Rendered as one tiled rect
 * rather than a few thousand circles.
 */
fun Modifier.halftone(
    step: Dp = 7.dp,
    dot: Dp = 1.2.dp,
    color: Color = Color(0x55000000),
): Modifier = drawWithCache {
    val cell = step.toPx().roundToInt().coerceAtLeast(2)
    val tile = ImageBitmap(cell, cell)
    Canvas(tile).drawCircle(Offset(cell / 2f, cell / 2f), dot.toPx(), Paint().apply { this.color = color })
    val brush = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    onDrawWithContent {
        drawContent()
        drawRect(brush)
    }
}

/** a CSS `linear-gradient(<angle>deg, …)`, angle and all, corner to corner */
fun Modifier.cssGradient(angleDeg: Float, stops: Array<Pair<Float, Color>>): Modifier = drawWithCache {
    val a = Math.toRadians(angleDeg.toDouble())
    val dx = sin(a).toFloat()
    val dy = -cos(a).toFloat()
    val len = abs(size.width * dx) + abs(size.height * dy)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val brush = Brush.linearGradient(
        colorStops = stops,
        start = Offset(cx - dx * len / 2f, cy - dy * len / 2f),
        end = Offset(cx + dx * len / 2f, cy + dy * len / 2f),
    )
    onDrawBehind { drawRect(brush) }
}

// -------------------------------------------------------------- the line art

private const val ARCH_FINIAL = "M50 8 Q92 -2 96 22 Q74 14 56 24 Z"
private const val ARCH_BODY = "M24 92 V42 Q24 12 50 12 Q76 12 76 42 V92 Z"
private const val ORBIT_WAVE_1 = "M30 40 Q50 30 70 42"
private const val ORBIT_WAVE_2 = "M27 56 Q50 48 73 58"
private const val ID_BODY = "M20 92 V46 Q20 10 50 10 Q80 10 80 46 V92 Z"
private const val ID_BAND = "M27 38 H73 Q78 38 78 48 V56 Q78 64 70 64 H30 Q22 64 22 56 V48 Q22 38 27 38 Z"

private fun pathOf(d: String): Path = PathParser().parsePathString(d).toPath()

/** the great helm: the sign-in and attendance mockups' portico symbol */
@Composable
fun HelmMark(arch: Color, accent: Color, detail: Color, size: Dp, modifier: Modifier = Modifier) {
    val finial = remember { pathOf(ARCH_FINIAL) }
    val body = remember { pathOf(ARCH_BODY) }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(finial, accent)
            drawPath(body, arch)
            drawRect(detail.copy(alpha = 0.35f), Offset(47f, 12f), Size(6f, 80f))
            drawRect(detail, Offset(28f, 44f), Size(44f, 7f))
            listOf(62f to 66f, 66f to 74f, 34f to 66f, 38f to 74f).forEach { (x, y) ->
                drawCircle(detail, 2f, Offset(x, y))
            }
        }
    }
}

/** the ringed planet: exams, countdowns, schedules */
@Composable
fun PlanetMark(planet: Color, ring: Color, speck: Color, detail: Color, size: Dp, modifier: Modifier = Modifier) {
    val wave1 = remember { pathOf(ORBIT_WAVE_1) }
    val wave2 = remember { pathOf(ORBIT_WAVE_2) }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawCircle(speck, 1.5f, Offset(12f, 14f))
            drawCircle(speck, 1f, Offset(84f, 10f))
            drawCircle(speck, 1.5f, Offset(90f, 70f))
            drawCircle(planet, 24f, Offset(50f, 50f))
            drawPath(wave1, detail.copy(alpha = 0.4f), style = Stroke(3f))
            drawPath(wave2, detail.copy(alpha = 0.4f), style = Stroke(3f))
            rotate(-22f, Offset(50f, 50f)) {
                drawOval(ring, Offset(4f, 39f), Size(92f, 22f), style = Stroke(2.5f))
            }
        }
    }
}

/** the vinyl record: grades, CGPA, results */
@Composable
fun VinylMark(label: Color, groove: Color, disc: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawCircle(disc, 42f, Offset(50f, 50f))
            listOf(35f, 28f, 21f).forEach { drawCircle(groove, it, Offset(50f, 50f), style = Stroke(1f)) }
            drawCircle(label, 12f, Offset(50f, 50f))
            drawCircle(Ink, 2.5f, Offset(50f, 50f))
        }
    }
}

/** the trooper helmet: people, profile, faculty */
@Composable
fun TrooperMark(band: Color, card: Color, clip: Color, size: Dp, modifier: Modifier = Modifier) {
    val body = remember { pathOf(ID_BODY) }
    val strip = remember { pathOf(ID_BAND) }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(body, card)
            drawPath(strip, band)
            drawRect(clip, Offset(8f, 48f), Size(12f, 22f))
            drawRect(clip, Offset(80f, 48f), Size(12f, 22f))
            drawLine(Ink, Offset(38f, 76f), Offset(62f, 76f), 3f)
            drawLine(Ink, Offset(38f, 82f), Offset(62f, 82f), 3f)
        }
    }
}

/**
 * The percentage ring from the mockups: r=26 in a 64 box, 6 wide, starting at
 * twelve o'clock. The sweep and the number count up together, drawn in the same
 * pass, so a running entrance costs a redraw and never a recomposition.
 * [a11y] replaces the number for screen readers, e.g. "86 percent attendance, safe".
 */
@Composable
fun Ring(
    percent: Double,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    a11y: String? = null,
    delayMs: Int = 0,
) {
    val reduced = LocalReducedMotion.current
    val sweep = remember { Animatable(if (reduced) percent.toFloat() else 0f) }
    LaunchedEffect(percent) {
        if (reduced) {
            sweep.snapTo(percent.toFloat())
        } else {
            if (delayMs > 0) delay(delayMs.toLong())
            sweep.animateTo(
                targetValue = percent.coerceIn(0.0, 100.0).toFloat(),
                animationSpec = tween(Motion.HERO, easing = Motion.emphasizedDecelerate),
            )
        }
    }
    val measurer = rememberTextMeasurer()
    val m = if (a11y != null) modifier.clearAndSetSemantics { contentDescription = a11y } else modifier
    Box(m.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val u = this.size.minDimension / 64f
            val stroke = 6f * u
            val r = 26f * u
            val c = Offset(32f * u, 32f * u)
            drawCircle(Line, r, c, style = Stroke(stroke))
            val v = sweep.value
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = v * 3.6f,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(2f * r, 2f * r),
                style = Stroke(stroke),
            )
            val layout = measurer.measure(
                text = AnnotatedString(v.roundToInt().toString()),
                style = Mono.copy(
                    fontSize = (size.value * 14f / 64f).sp,
                    fontWeight = FontWeight.Medium,
                    color = Bone,
                ),
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    (this.size.width - layout.size.width) / 2f,
                    (this.size.height - layout.size.height) / 2f,
                ),
            )
        }
    }
}
