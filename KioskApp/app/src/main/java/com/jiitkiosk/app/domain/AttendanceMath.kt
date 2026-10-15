package com.jiitkiosk.app.domain

import `in`.codelif.ktjiit.model.ClassRecord
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/** attended / total, with the two questions every student actually asks. */
data class Tally(val attended: Int, val total: Int) {

    val percent: Double get() = if (total == 0) 0.0 else attended * 100.0 / total

    /** classes you can skip and still sit at or above [target]% */
    fun canMiss(target: Int): Int {
        if (target <= 0 || total == 0) return 0
        return floor((100.0 * attended - target * total) / target).toInt().coerceAtLeast(0)
    }

    /** classes in a row you need to climb back to [target]% */
    fun mustAttend(target: Int): Int {
        if (target >= 100) return if (attended < total) Int.MAX_VALUE else 0
        return ceil((target * total - 100.0 * attended) / (100 - target)).toInt().coerceAtLeast(0)
    }

    companion object {
        val ZERO = Tally(0, 0)
    }
}

object AttendanceMath {

    /**
     * The portal's own rule: every present class counts toward the top, only
     * regular classes toward the bottom, so an extra can only ever help.
     */
    fun tally(classes: List<ClassRecord>) =
        Tally(classes.count { it.isPresent }, classes.count { !it.isExtra })

    /**
     * Counted percentage minus the portal's own number, or null when they agree.
     * The portal rounds to a decimal, so only a gap past 0.1 means the rule moved.
     */
    fun drift(server: Double?, classes: List<ClassRecord>): Double? {
        val t = tally(classes)
        if (server == null || t.total == 0) return null
        return (t.percent - server).takeIf { abs(it) > 0.1 }
    }

    /** running percentage after each class, oldest first */
    fun trend(classes: List<ClassRecord>): List<Pair<LocalDate, Double>> {
        val sorted = classes.filter { it.date != null }.sortedWith(compareBy({ it.date }, { it.start }))
        var present = 0
        var regular = 0
        return sorted.mapNotNull { c ->
            if (c.isPresent) present++
            if (!c.isExtra) regular++
            if (regular == 0) null else c.date!! to present * 100.0 / regular
        }
    }
}
