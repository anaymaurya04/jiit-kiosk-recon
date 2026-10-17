package com.jiitkiosk.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/**
 * Bitmap takes on the boards' two vector pieces, because RemoteViews cannot
 * draw arcs: the attendance ring (boards 28/29/30: r26 stroke 6 in a 64 box,
 * top-anchored, stopwatch butt caps) and the 6dp subject meter.
 */

/** [pct] of the ring in [color], track in Line — board 28's exact geometry */
fun ringBitmap(sizePx: Int, pct: Int, color: Int): Bitmap {
    val b = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val stroke = sizePx * 6f / 64f
    val pad = stroke / 2f
    val box = RectF(pad, pad, sizePx - pad, sizePx - pad)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.BUTT
    }
    p.color = C.LINE
    c.drawArc(box, 0f, 360f, false, p)
    val sweep = 360f * pct.coerceIn(0, 100) / 100f
    if (sweep > 0f) {
        p.color = color
        c.drawArc(box, -90f, sweep, false, p)
    }
    return b
}

/** board 30's strip meter: full track, then the coloured part to [pct] */
fun meterBitmap(heightPx: Int, pct: Int, color: Int): Bitmap {
    val w = 400
    val b = Bitmap.createBitmap(w, heightPx, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    c.drawRect(0f, 0f, w.toFloat(), heightPx.toFloat(), Paint().apply { this.color = C.LINE })
    c.drawRect(0f, 0f, w * pct.coerceIn(0, 100) / 100f, heightPx.toFloat(), Paint().apply { this.color = color })
    return b
}
