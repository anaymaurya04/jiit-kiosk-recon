package com.jiitkiosk.app.code

/** both code boards draw exactly 22 weeks of days */
internal const val WEEKS = 22
internal const val ONE_DAY_S = 86_400L

/** the Sunday-aligned start of the 22-week window that ends in [today]'s week */
internal fun windowStart(today: Long): Long = today - ((today + 4) % 7) - (WEEKS - 1) * 7L

/** [WEEKS] × 7 day counts from [startDay], missing days as zero */
internal fun daysOf(perDay: Map<Long, Int>, startDay: Long): List<Int> =
    (0 until WEEKS * 7).map { perDay[startDay + it] ?: 0 }

/** consecutive active days ending today or yesterday: today may still be quiet */
internal fun streak(perDay: Map<Long, Int>, today: Long): Int {
    var day = if ((perDay[today] ?: 0) > 0) today else today - 1
    var n = 0
    while ((perDay[day] ?: 0) > 0) {
        n++
        day--
    }
    return n
}

/** unix seconds to the UTC day number the heatmap keys are in */
internal fun dayOf(ts: Long): Long = ts / ONE_DAY_S
