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

// shortcut: unbounded per-process cache keyed by the path string; this file's
// paths are ~30 literals, revisit only if motifs ever take dynamic paths
private val pathCache = java.util.concurrent.ConcurrentHashMap<String, Path>()

private fun pathOf(d: String): Path = pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

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

/** Tower/Keep: campus, hostel, institute */
@Composable
fun TowerMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, cut: Color = Color.Transparent) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawRect(color, Offset(28f, 38f), Size(44f, 54f))
            listOf(28f, 45f, 62f).forEach { drawRect(color, Offset(it, 26f), Size(10f, 12f)) }
            drawRect(color, Offset(49f, 4f), Size(2f, 22f))
            drawPath(pathOf("M51 5 L74 11 L51 17 Z"), accent)
            drawPath(pathOf("M42 92 V72 Q50 62 58 72 V92 Z"), cut)
            drawRect(cut, Offset(48f, 46f), Size(4f, 10f))
        }
    }
}

/** Longsword: primary action, submit, beat a deadline */
@Composable
fun LongswordMark(
    size: Dp,
    modifier: Modifier = Modifier,
    color: Color = Bone,
    accent: Color = Crimson,
    grip: Color = MarkRing,
) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(pathOf("M50 4 L58 20 V62 H42 V20 Z"), color)
            drawRect(AccentDeep, Offset(49f, 18f), Size(2f, 40f))
            drawRect(accent, Offset(22f, 62f), Size(56f, 8f))
            drawRect(grip, Offset(45f, 70f), Size(10f, 18f))
            drawCircle(accent, 6f, Offset(50f, 92f))
        }
    }
}

/** Heater shield: privacy, bank, protected data */
@Composable
fun ShieldMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, cut: Color = Color.Transparent) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(pathOf("M18 12 H82 V50 Q82 78 50 94 Q18 78 18 50 Z"), color)
            drawPath(pathOf("M50 12 H82 V50 Q82 78 50 94 Z"), accent)
            drawRect(cut, Offset(48.5f, 12f), Size(3f, 82f))
            drawPath(pathOf("M50 30 L58 44 L50 58 L42 44 Z"), cut)
        }
    }
}

/** Comet: deadlines, fees due */
@Composable
fun CometMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, tail: Color = Violet) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(pathOf("M64 62 L6 12 L54 56 Z"), accent)
            drawPath(pathOf("M68 70 L10 44 L56 74 Z"), tail.copy(alpha = 0.7f))
            drawCircle(color, 16f, Offset(72f, 72f))
            drawCircle(LineStrong, 4f, Offset(67f, 67f))
            drawCircle(color, 1.5f, Offset(90f, 20f))
            drawCircle(color, 1.5f, Offset(24f, 86f))
            drawCircle(color, 1f, Offset(86f, 46f))
        }
    }
}

/** Vintage Mic: announcements, feedback */
@Composable
fun MicMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, cut: Color = Color.Transparent) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawCircle(color, 20f, Offset(50f, 30f))
            listOf(24f, 32f, 40f).forEach { y -> drawLine(cut, Offset(34f, y), Offset(66f, y), 2.5f) }
            drawRect(accent, Offset(38f, 48f), Size(24f, 8f))
            drawRect(LineStrong, Offset(44f, 56f), Size(12f, 28f))
            drawRect(color, Offset(34f, 84f), Size(32f, 7f))
        }
    }
}

/** Cog / Gear: settings */
@Composable
fun GearMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, cut: Color = Color.Transparent) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            listOf(0f, 45f, 90f, 135f).forEach { rot ->
                rotate(rot, Offset(50f, 50f)) {
                    drawRect(color, Offset(43f, 6f), Size(14f, 88f))
                }
            }
            drawCircle(color, 30f, Offset(50f, 50f))
            drawCircle(cut, 13f, Offset(50f, 50f))
            drawCircle(accent, 5f, Offset(50f, 50f))
        }
    }
}

/** Dossier: profile, records */
@Composable
fun DossierMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, cut: Color = Color.Transparent) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(pathOf("M10 24 H40 L48 32 H90 V88 H10 Z"), color)
            drawRect(color, Offset(10f, 36f), Size(80f, 52f))
            listOf(48f to 56f, 58f to 50f, 68f to 44f).forEach { (y, w) ->
                drawLine(cut, Offset(20f, y), Offset(w, y), 3f)
            }
            rotate(-12f, Offset(72f, 66f)) {
                drawCircle(accent, 14f, Offset(72f, 66f), style = Stroke(4f))
                drawLine(accent, Offset(63f, 66f), Offset(81f, 66f), 3f)
            }
        }
    }
}

/** Satellite: sync, connectivity, exams timeline */
@Composable
fun SatelliteMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, panel: Color = Violet, cut: Color = Color.Transparent) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawRect(color, Offset(38f, 38f), Size(24f, 24f))
            drawRect(panel, Offset(4f, 42f), Size(30f, 16f))
            drawRect(panel, Offset(66f, 42f), Size(30f, 16f))
            listOf(14f, 24f, 76f, 86f).forEach { x -> drawLine(cut, Offset(x, 42f), Offset(x, 58f), 2f) }
            drawLine(color, Offset(50f, 38f), Offset(50f, 22f), 3f)
            drawCircle(accent, 5f, Offset(50f, 18f))
            drawPath(pathOf("M36 10 Q50 -2 64 10"), color, style = Stroke(2.5f))
            drawRect(LineStrong, Offset(44f, 62f), Size(12f, 10f))
        }
    }
}

/** Tome: syllabus, marks, subjects */
@Composable
fun TomeMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, spine: Color = Violet) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawRect(spine, Offset(18f, 8f), Size(64f, 82f))
            drawRect(accent, Offset(18f, 8f), Size(12f, 82f))
            drawRect(color, Offset(22f, 90f), Size(60f, 5f))
            drawRect(color, Offset(76f, 42f), Size(10f, 14f))
            drawPath(pathOf("M56 34 L68 49 L56 64 L44 49 Z"), color, style = Stroke(3f))
            drawCircle(color, 3f, Offset(56f, 49f))
        }
    }
}

/** Hourglass: countdown, pending */
@Composable
fun HourglassMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, sand: Color = Violet) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawRect(color, Offset(18f, 6f), Size(64f, 8f))
            drawRect(color, Offset(18f, 86f), Size(64f, 8f))
            drawPath(pathOf("M26 14 H74 L54 50 L74 86 H26 L46 50 Z"), color, style = Stroke(4f))
            drawPath(pathOf("M34 86 H66 L50 62 Z"), accent)
            drawPath(pathOf("M36 22 H64 L52 40 H48 Z"), sand)
        }
    }
}

/** Raven: notices, messages, contact */
@Composable
fun RavenMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, cut: Color = Color.Transparent) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(pathOf("M10 58 Q36 28 68 36 L92 28 L80 46 Q82 72 58 82 L62 94 H50 L44 84 Q22 82 10 58 Z"), color)
            drawPath(pathOf("M68 36 L94 30 L80 46 Z"), accent)
            drawCircle(cut, 3f, Offset(70f, 40f))
            drawPath(pathOf("M24 58 Q40 48 58 54"), cut, style = Stroke(2f))
        }
    }
}

/** Dead Sun: offline, sync failed */
@Composable
fun DeadSunMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            val ray = pathOf("M50 2 L56 22 H44 Z")
            (0 until 360 step 30).forEach { deg ->
                rotate(deg.toFloat(), Offset(50f, 50f)) {
                    drawPath(ray, accent)
                }
            }
            drawCircle(Ink, 28f, Offset(50f, 50f))
            drawCircle(color, 28f, Offset(50f, 50f), style = Stroke(3f))
            drawPath(pathOf("M50 24 L45 40 L55 50 L47 64 L52 76"), color, style = Stroke(2f))
        }
    }
}

/** Radar sweep: attendance, scan */
@Composable
fun RadarMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            listOf(42f, 28f, 14f).forEach { r ->
                drawCircle(LineStrong, r, Offset(50f, 50f), style = Stroke(2f))
            }
            drawLine(LineStrong, Offset(50f, 8f), Offset(50f, 92f), 2f)
            drawLine(LineStrong, Offset(8f, 50f), Offset(92f, 50f), 2f)
            drawPath(pathOf("M50 50 L50 8 A42 42 0 0 1 86 29 Z"), accent.copy(alpha = 0.85f))
            drawCircle(color, 3.5f, Offset(68f, 68f))
            drawCircle(color, 3f, Offset(34f, 36f))
        }
    }
}

