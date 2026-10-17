package com.jiitkiosk.app.widget

import com.jiitkiosk.app.ui.theme.Verdict

/**
 * The design tokens as ARGB ints — RemoteViews needs plain colours, not Compose
 * Color. Values are transcribed from design/boards/28..33 (same hexes as
 * ui/theme/Theme.kt), never invented here.
 */
object C {
    const val INK = 0xFF0B0B0D.toInt()
    const val BONE = 0xFFF0EEE9.toInt()
    const val MUTED = 0xFFA09DA6.toInt()
    const val SURFACE = 0xFF141418.toInt()
    const val LINE = 0xFF26262E.toInt()
    const val CRIMSON = 0xFFC23B4E.toInt()
    const val GOOD = 0xFF4CC38A.toInt()
    const val WARN = 0xFFE0A030.toInt()
    const val BAD = 0xFFE0566A.toInt()
    const val CLEAR = 0x00000000
}

fun verdictColor(v: Verdict): Int = when (v) {
    Verdict.Safe -> C.GOOD
    Verdict.Close -> C.WARN
    Verdict.Low -> C.BAD
}

fun verdictGlyph(v: Verdict): String = when (v) {
    Verdict.Safe -> "✓"
    Verdict.Close -> "!"
    Verdict.Low -> "✕"
}

/** the boards' marker words as chips: "✓ SAFE" / "! CLOSE" / "✕ LOW" */
fun verdictChip(v: Verdict): String = "${verdictGlyph(v)} ${v.stamp.uppercase()}"
