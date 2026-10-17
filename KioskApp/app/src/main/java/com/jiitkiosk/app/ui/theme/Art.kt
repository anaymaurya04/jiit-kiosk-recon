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
            drawRect(Steel, Offset(49f, 18f), Size(2f, 40f))
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


// ---------------------------------------------------------------- board 27: the rest of the library

/** pennant: events, announcements */
@Composable
fun PennantMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, cut: Color = MarkRing) {
    val p0 = remember { pathOf("M25 12 H84 L68 30 L84 48 H25Z") }
    val p1 = remember { pathOf("M34 26 L52 30 L34 34Z") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawRect(color, Offset(20f, 6f), Size(5f, 88f))
            drawPath(p0, accent)
            drawPath(p1, color)
            drawRect(cut, Offset(14f, 90f), Size(17f, 5f))
        }
    }
}


/** sentinel visor: system alerts, low attendance, admin */
@Composable
fun SentinelMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, cut: Color = AccentDeep) {
    val p0 = remember { pathOf("M26 14 H74 L88 36 V70 L70 94 H30 L12 70 V36Z") }
    val p1 = remember { pathOf("M24 40 H76 L68 58 H32Z") }
    val p2 = remember { pathOf("M50 14 V40") }
    val p3 = remember { pathOf("M34 74 H66 M38 82 H62") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(p0, color)
            drawPath(p1, accent)
            drawPath(p2, cut, style = Stroke(4f))
            drawPath(p3, cut, style = Stroke(3f))
        }
    }
}


/** rocket: updates, app version, launch */
@Composable
fun RocketMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, aux: Color = Violet, cut: Color = Night) {
    val p0 = remember { pathOf("M50 4 Q70 26 66 60 H34 Q30 26 50 4Z") }
    val p1 = remember { pathOf("M34 46 L16 74 L34 68Z M66 46 L84 74 L66 68Z") }
    val p2 = remember { pathOf("M40 64 L50 94 L60 64Z") }
    val p3 = remember { pathOf("M45 64 L50 80 L55 64Z") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(p0, color)
            drawCircle(aux, 9f, Offset(50f, 34f))
            drawCircle(cut, 9f, Offset(50f, 34f), style = Stroke(3f))
            drawPath(p1, accent)
            drawPath(p2, accent)
            drawPath(p3, color)
        }
    }
}


/** crescent moon: offline, cached data, night */
@Composable
fun MoonMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, aux: Color = Violet, cut: Color = MarkRing) {
    val p0 = remember { pathOf("M62 8 A42 42 0 1 0 92 62 A34 34 0 1 1 62 8Z") }
    val p1 = remember { pathOf("M80 14 V26 M74 20 H86") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawCircle(aux.copy(alpha = 0.25f), 44f, Offset(52f, 50f))
            drawPath(p0, color)
            drawCircle(cut, 5f, Offset(34f, 48f))
            drawCircle(cut, 3.5f, Offset(46f, 72f))
            drawCircle(cut, 2.5f, Offset(28f, 68f))
            drawPath(p1, color, style = Stroke(2f))
            drawCircle(color, 1.5f, Offset(86f, 42f))
        }
    }
}


/** flying V: events, cultural, clubs */
@Composable
fun GuitarMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, cut: Color = MarkRing, shade: Color = Surface2) {
    val p0 = remember { pathOf("M50 44 L20 94 L38 96 L50 74 L62 96 L80 94Z") }
    val p1 = remember { pathOf("M50 10 V48") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawRect(cut, Offset(46f, 10f), Size(8f, 42f))
            drawRect(accent, Offset(43f, 3f), Size(14f, 10f))
            drawPath(p0, color)
            drawRect(shade, Offset(40f, 60f), Size(20f, 4f))
            drawRect(accent, Offset(43f, 68f), Size(14f, 4f))
            drawPath(p1, shade, style = Stroke(1f))
        }
    }
}


/** cassette: documents, records, history */
@Composable
fun CassetteMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, cut: Color = Surface2, shade: Color = MarkRing) {
    val p0 = remember { pathOf("M20 78 L26 70 H74 L80 78Z") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawRect(color, Offset(8f, 22f), Size(84f, 56f))
            drawRect(accent, Offset(16f, 28f), Size(68f, 20f))
            drawRect(cut, Offset(38f, 54f), Size(24f, 14f))
            drawCircle(cut, 8f, Offset(30f, 61f))
            drawCircle(cut, 8f, Offset(70f, 61f))
            drawCircle(color, 3f, Offset(30f, 61f))
            drawCircle(color, 3f, Offset(70f, 61f))
            drawPath(p0, shade)
        }
    }
}


/** headphones: focus, feedback, help */
@Composable
fun HeadphonesMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, cut: Color = MarkRing) {
    val p0 = remember { pathOf("M20 60 V48 Q20 14 50 14 Q80 14 80 48 V60") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(p0, color, style = Stroke(7f))
            drawRect(accent, Offset(10f, 54f), Size(18f, 32f))
            drawRect(accent, Offset(72f, 54f), Size(18f, 32f))
            drawRect(cut, Offset(28f, 60f), Size(6f, 20f))
            drawRect(cut, Offset(66f, 60f), Size(6f, 20f))
        }
    }
}


/** amp stack: notifications */
@Composable
fun AmpMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, cut: Color = MarkCore, shade: Color = Surface2) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawRect(cut, Offset(14f, 8f), Size(72f, 84f))
            drawRect(color, Offset(14f, 8f), Size(72f, 84f), style = Stroke(3f))
            drawCircle(color, 22f, Offset(50f, 62f), style = Stroke(3f))
            drawCircle(accent, 11f, Offset(50f, 62f))
            drawCircle(shade, 3f, Offset(50f, 62f))
            drawCircle(color, 4f, Offset(30f, 24f))
            drawCircle(color, 4f, Offset(44f, 24f))
            drawRect(cut, Offset(58f, 21f), Size(20f, 6f))
        }
    }
}


/** skeleton key: login, access, bank reveal */
@Composable
fun KeyMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawCircle(color, 19f, Offset(26f, 50f), style = Stroke(8f))
            drawRect(color, Offset(44f, 46f), Size(48f, 8f))
            drawRect(color, Offset(76f, 54f), Size(8f, 14f))
            drawRect(color, Offset(62f, 54f), Size(8f, 9f))
            drawCircle(accent, 5f, Offset(26f, 50f))
        }
    }
}


/** lightning: urgent, fast action */
@Composable
fun BoltMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson) {
    val p0 = remember { pathOf("M60 4 L20 56 H44 L36 96 L80 40 H54Z") }
    val p1 = remember { pathOf("M60 4 L44 56 H54 L60 40Z") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(p0, accent)
            drawPath(p1, color.copy(alpha = 0.35f))
        }
    }
}


/** pigeon: notices, login, anything watched */
@Composable
fun PigeonMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, cut: Color = Cursed, shade: Color = MarkRing) {
    val p0 = remember { pathOf("M58 22 V10") }
    val p1 = remember { pathOf("M54 42 Q30 46 22 64 Q20 80 38 84 H64 Q84 80 82 60 Q80 46 68 42Z") }
    val p2 = remember { pathOf("M74 30 L90 35 L74 40Z") }
    val p3 = remember { pathOf("M34 62 Q50 54 68 66 Q58 78 38 74Z") }
    val p4 = remember { pathOf("M22 64 L6 78 L26 78Z") }
    val p5 = remember { pathOf("M46 84 V94 M58 84 V94 M42 94 H50 M54 94 H62") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(p0, color, style = Stroke(2.5f))
            drawCircle(accent, 3f, Offset(58f, 8f))
            drawPath(p1, color)
            drawCircle(color, 13f, Offset(62f, 32f))
            drawPath(p2, accent)
            drawCircle(cut, 7f, Offset(64f, 31f))
            drawCircle(accent, 7f, Offset(64f, 31f), style = Stroke(2f))
            drawCircle(accent, 3f, Offset(64f, 31f))
            drawPath(p3, shade)
            drawPath(p4, shade)
            drawPath(p5, accent, style = Stroke(3f))
        }
    }
}


/** blessed backup: saved, synced, cached safely */
@Composable
fun BackupMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, aux: Color = Violet, cut: Color = MarkRing, shade: Color = Cursed) {
    val p0 = remember { pathOf("M24 52 Q4 44 2 24 Q18 28 24 42Z M76 52 Q96 44 98 24 Q82 28 76 42Z") }
    val p1 = remember { pathOf("M22 30 H70 L80 40 V90 H22Z") }
    val p2 = remember { pathOf("M36 70 H64 M36 78 H56") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawOval(aux, Offset(30.0f, 8.0f), Size(40.0f, 12.0f))
            drawPath(p0, color)
            drawPath(p1, color)
            drawRect(cut, Offset(34f, 30f), Size(28f, 18f))
            drawRect(shade, Offset(52f, 33f), Size(6f, 12f))
            drawRect(accent, Offset(30f, 60f), Size(40f, 30f))
            drawPath(p2, shade, style = Stroke(3f))
        }
    }
}


/** silicon basilica: campus, institute, the portal itself */
@Composable
fun BasilicaMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, aux: Color = Violet, cut: Color = Cursed) {
    val p0 = remember { pathOf("M30 92 V40 L50 12 L70 40 V92Z") }
    val p1 = remember { pathOf("M12 46 L21 22 L30 46Z M70 46 L79 22 L88 46Z") }
    val p2 = remember { pathOf("M44 40 V36 M50 40 V36 M56 40 V36 M44 58 V62 M50 58 V62 M56 58 V62 M41 45 H37 M41 51 H37 M59 45 H63 M59 51 H63") }
    val p3 = remember { pathOf("M42 92 V76 Q50 66 58 76 V92Z") }
    val p4 = remember { pathOf("M16 60 H26 M16 70 H26 M74 60 H84 M74 70 H84") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(p0, color)
            drawRect(color, Offset(12f, 46f), Size(18f, 46f))
            drawRect(color, Offset(70f, 46f), Size(18f, 46f))
            drawPath(p1, color)
            drawRect(cut, Offset(41f, 40f), Size(18f, 18f))
            drawRect(aux, Offset(46f, 45f), Size(8f, 8f))
            drawPath(p2, color, style = Stroke(2f))
            drawPath(p3, cut)
            drawPath(p4, cut, style = Stroke(2f))
        }
    }
}


/** glitch animal: errors, corrupted data, retry */
@Composable
fun GlitchMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, aux: Color = Violet, cut: Color = Cursed) {
    val p0 = remember { pathOf("M24 22 L28 4 L44 22Z M56 22 L72 4 L76 22Z") }
    val p1 = remember { pathOf("M10 64 H20 M80 50 H92") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(p0, color)
            drawRect(color, Offset(24f, 22f), Size(52f, 10f))
            drawRect(color, Offset(20f, 32f), Size(60f, 10f))
            drawRect(accent, Offset(30f, 42f), Size(56f, 8f))
            drawRect(color, Offset(14f, 50f), Size(56f, 8f))
            drawRect(aux, Offset(22f, 58f), Size(56f, 12f))
            drawRect(color, Offset(30f, 70f), Size(40f, 10f))
            drawRect(color, Offset(40f, 80f), Size(20f, 8f))
            drawRect(cut, Offset(32f, 34f), Size(9f, 5f))
            drawRect(cut, Offset(58f, 34f), Size(9f, 5f))
            drawRect(accent, Offset(46f, 48f), Size(8f, 5f))
            drawPath(p1, aux, style = Stroke(2f))
        }
    }
}


/** forbidden coffee: locked, restricted, permission denied */
@Composable
fun CoffeeMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, aux: Color = Violet, cut: Color = MarkCore) {
    val p0 = remember { pathOf("M36 30 Q28 20 38 12 Q46 6 38 0 M54 30 Q46 20 56 12") }
    val p1 = remember { pathOf("M20 40 H68 V72 Q68 90 44 90 Q20 90 20 72Z") }
    val p2 = remember { pathOf("M68 48 H78 Q90 48 90 60 Q90 72 76 72 H68") }
    val p3 = remember { pathOf("M20 60 H68") }
    val p4 = remember { pathOf("M41 58 V52 Q45 46 49 52 V58") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(p0, aux, style = Stroke(3f))
            drawPath(p1, color)
            drawPath(p2, color, style = Stroke(6f))
            drawOval(cut, Offset(20.0f, 35.0f), Size(48.0f, 10.0f))
            drawPath(p3, accent, style = Stroke(6f))
            drawRect(accent, Offset(38f, 58f), Size(14f, 14f))
            drawPath(p4, color, style = Stroke(3f))
        }
    }
}


/** bone archive: old records, previous semesters, archived */
@Composable
fun BoneArchiveMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, cut: Color = MarkCore) {
    val p0 = remember { pathOf("M20 36 H80 M20 64 H80") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawRect(cut, Offset(20f, 8f), Size(60f, 84f))
            drawRect(color, Offset(20f, 8f), Size(60f, 84f), style = Stroke(3f))
            drawPath(p0, color, style = Stroke(3f))
            drawRect(color, Offset(36f, 19f), Size(28f, 5f))
            drawCircle(color, 3.5f, Offset(36f, 18f))
            drawCircle(color, 3.5f, Offset(36f, 25f))
            drawCircle(color, 3.5f, Offset(64f, 18f))
            drawCircle(color, 3.5f, Offset(64f, 25f))
            drawRect(color, Offset(36f, 47f), Size(28f, 5f))
            drawCircle(color, 3.5f, Offset(36f, 46f))
            drawCircle(color, 3.5f, Offset(36f, 53f))
            drawCircle(color, 3.5f, Offset(64f, 46f))
            drawCircle(color, 3.5f, Offset(64f, 53f))
            drawRect(color, Offset(36f, 75f), Size(28f, 5f))
            drawCircle(color, 3.5f, Offset(36f, 74f))
            drawCircle(color, 3.5f, Offset(36f, 81f))
            drawCircle(color, 3.5f, Offset(64f, 74f))
            drawCircle(color, 3.5f, Offset(64f, 81f))
            drawRect(accent, Offset(40f, 29f), Size(20f, 4f))
        }
    }
}


/** cursor necromancer: search, command palette, revive cache */
@Composable
fun NecromancerMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, aux: Color = Violet, cut: Color = MarkRing, shade: Color = Cursed) {
    val p0 = remember { pathOf("M50 10 Q70 12 72 38 L84 92 H12 L28 38 Q30 12 50 10Z") }
    val p1 = remember { pathOf("M80 10 H96 M88 10 V30 M80 30 H96") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(p0, cut)
            drawPath(p0, color, style = Stroke(3f))
            drawOval(shade, Offset(38.0f, 25.0f), Size(24.0f, 30.0f))
            drawRect(accent, Offset(40f, 38f), Size(6f, 5f))
            drawRect(accent, Offset(54f, 38f), Size(6f, 5f))
            drawRect(color, Offset(86f, 14f), Size(4f, 80f))
            drawPath(p1, accent, style = Stroke(3.5f))
            drawRect(aux, Offset(8f, 22f), Size(3f, 10f))
            drawRect(aux, Offset(68f, 6f), Size(3f, 10f))
        }
    }
}


/** untranslated tablet: unknown field, missing data, unsupported */
@Composable
fun TabletMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, cut: Color = Cursed, shade: Color = MarkRing) {
    val p0 = remember { pathOf("M22 92 V32 Q22 8 50 8 Q78 8 78 32 V92Z") }
    val p1 = remember { pathOf("M50 36 L56 26 L62 36Z") }
    val p2 = remember { pathOf("M48 66 A7 7 0 1 0 55 56") }
    val p3 = remember { pathOf("M36 74 H44 M40 70 V78 M58 70 L66 78 M66 70 L58 78") }
    val p4 = remember { pathOf("M44 50 L52 56") }
    val p5 = remember { pathOf("M30 86 H70") }
    val p6 = remember { pathOf("M58 8 L52 30 L60 40 L54 56") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(p0, color)
            drawCircle(cut, 4f, Offset(38f, 30f))
            drawPath(p1, cut)
            drawRect(cut, Offset(60f, 22f), Size(3f, 14f))
            drawRect(cut, Offset(34f, 46f), Size(9f, 9f))
            drawCircle(cut, 2.5f, Offset(62f, 50f))
            drawPath(p2, cut, style = Stroke(2.5f))
            drawPath(p3, cut)
            drawPath(p3, cut, style = Stroke(2.5f))
            drawPath(p4, accent, style = Stroke(3f))
            drawPath(p5, accent, style = Stroke(4f))
            drawPath(p6, shade, style = Stroke(1.5f))
        }
    }
}


/** tech debt bonsai: pending items, about, credits */
@Composable
fun BonsaiMark(size: Dp, modifier: Modifier = Modifier, color: Color = Bone, accent: Color = Crimson, aux: Color = Violet, cut: Color = MarkRing) {
    val p0 = remember { pathOf("M30 74 H70 L66 92 H34Z") }
    val p1 = remember { pathOf("M50 72 Q38 60 50 50 Q62 42 48 32") }
    val p2 = remember { pathOf("M52 52 Q68 50 76 40 M46 60 Q34 56 28 48") }
    val p3 = remember { pathOf("M62 44 V58 M30 50 V62 M80 42 V54") }
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) {
            drawPath(p0, accent)
            drawRect(cut, Offset(28f, 70f), Size(44f, 6f))
            drawPath(p1, color, style = Stroke(6f))
            drawPath(p2, color, style = Stroke(4f))
            drawCircle(aux, 14f, Offset(48f, 24f))
            drawCircle(aux, 10f, Offset(76f, 34f))
            drawCircle(aux, 10f, Offset(26f, 42f))
            drawPath(p3, color, style = Stroke(1.5f))
            drawRect(accent, Offset(58f, 58f), Size(9f, 11f))
            drawRect(color, Offset(26f, 62f), Size(9f, 11f))
            drawCircle(accent, 5f, Offset(80f, 58f))
        }
    }
}
