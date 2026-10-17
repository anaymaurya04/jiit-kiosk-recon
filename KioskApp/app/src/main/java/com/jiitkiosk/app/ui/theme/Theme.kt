package com.jiitkiosk.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.jiitkiosk.app.R
import com.jiitkiosk.app.ui.LocalReducedMotion
import com.jiitkiosk.app.ui.rememberReducedMotion

// The palette is transcribed from the design mockups, not invented here.
val Ink = Color(0xFF0B0B0D) // page
val Bone = Color(0xFFF0EEE9) // primary text, and the solid stamp blocks
val Muted = Color(0xFFA09DA6) // secondary text
val Surface1 = Color(0xFF141418) // cards and the bottom bar
val Line = Color(0xFF26262E) // 2px card borders, 1px rules
val Crimson = Color(0xFFC23B4E)
val CrimsonDeep = Color(0xFF2A0A12) // dark end of the attendance gradient
val Night = Color(0xFF2B2060) // dark end of the exams gradient
val Violet = Color(0xFF7B5CD6)
val Good = Color(0xFF4CC38A)
val Warn = Color(0xFFE0A030)
val Bad = Color(0xFFE0566A)
val MarkRing = Color(0xFF3A3A44)
val MarkCore = Color(0xFF111111)
val Surface2 = Color(0xFF1C1C22) // secondary tiles
val LineStrong = Color(0xFF3A3A44) // dashed and empty borders, inactive chips
val AccentDeep = Color(0xFF5A1522)
val Steel = Color(0xFF14202E) // trooper tile background
val Cursed = Color(0xFF241338) // cursed-campus shadow (board 27)

/** pure-black mode maps the page onto true black, as the design system asks */
val PureBlack = Color(0xFF000000)

/**
 * Whether pure-black mode is on. Provided once at the top of the app, because
 * it changes the page colour on every screen at once.
 */
val LocalPureBlack = androidx.compose.runtime.staticCompositionLocalOf { false }

private val Barlow = FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

private val Plex = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
)

private val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

/**
 * `.d` from the design: condensed, uppercase, 0.9 leading. The design sets the
 * size per element (`class="d" style="font-size:56px"`), so callers do the same.
 */
val Display = TextStyle(
    fontFamily = Barlow,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.03.em,
    lineHeight = 0.95.em,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/** `.m` from the design: the 11px mono caption */
val Mono = TextStyle(
    fontFamily = PlexMono,
    fontSize = 11.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.02.em,
)

/** `.r` from the design: 15px sans */
val Sans = TextStyle(fontFamily = Plex, fontSize = 15.sp)

/** the three verdicts, and the banding the mockups use: 86 safe, 74 close, 61 low */
enum class Verdict(val stamp: String, val color: Color) {
    Safe("Safe", Good),
    Close("Close", Warn),
    Low("Low", Bad),
}

/**
 * The user's own line, from Settings: at or above [target] is Safe, within
 * [band] per cent under it Close, anything lower Low. The mockups' sample
 * numbers (86 up, 74 close, 61 low) are exactly the defaults, 75 with a
 * five-wide band.
 */
fun verdictOf(percent: Double, target: Int, band: Int): Verdict = when {
    percent >= target -> Verdict.Safe
    percent >= target - band -> Verdict.Close
    else -> Verdict.Low
}

/**
 * Pure-black mode swaps only the two surfaces the page is built from: the page
 * itself and the cards sitting on it. Everything else is the design's palette.
 */
private fun schemeFor(black: Boolean) = darkColorScheme(
    primary = Crimson,
    onPrimary = Bone,
    secondary = Violet,
    onSecondary = Bone,
    background = if (black) PureBlack else Ink,
    onBackground = Bone,
    surface = if (black) PureBlack else Ink,
    onSurface = Bone,
    surfaceContainer = if (black) Ink else Surface1,
    surfaceContainerHigh = if (black) Ink else Surface2,
    surfaceVariant = if (black) Surface1 else Surface2,
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = Line,
    error = Bad,
    onError = Ink,
)

private val type = Typography(
    bodyLarge = Sans.copy(fontSize = 16.sp),
    bodyMedium = Sans,
    bodySmall = Sans.copy(fontSize = 13.sp),
    labelLarge = Sans.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = Mono,
    labelSmall = Mono,
    titleLarge = Display.copy(fontSize = 28.sp),
    titleMedium = Sans.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun KioskTheme(content: @Composable () -> Unit) {
    val black = LocalPureBlack.current
    val reduced = rememberReducedMotion()
    CompositionLocalProvider(LocalReducedMotion provides reduced) {
        MaterialTheme(
            colorScheme = remember(black) { schemeFor(black) },
            typography = type,
            content = content,
        )
    }
}
