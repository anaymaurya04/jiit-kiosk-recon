package com.jiitkiosk.app.calendar

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.ZoneId

/**
 * Hands a prefilled event to whichever calendar app is installed: the insert
 * happens in that app, so this needs no calendar permission and writes nothing
 * itself. False means nothing took the intent — the caller says so.
 */
object CalInsert {

    /** an all-day event covering [from]..[to] */
    fun span(context: Context, title: String, from: LocalDate, to: LocalDate, description: String? = null): Boolean {
        val zone = ZoneId.systemDefault()
        val begin = from.atStartOfDay(zone).toInstant().toEpochMilli()
        // an all-day span ends exclusive: the midnight after the last day
        val end = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return fire(context, title, begin, end, allDay = true, description)
    }

    /** a timed event at [at], an hour long */
    fun moment(context: Context, title: String, at: Long, description: String? = null): Boolean =
        fire(context, title, at, at + 3_600_000L, allDay = false, description)

    private fun fire(context: Context, title: String, begin: Long, end: Long, allDay: Boolean, description: String?): Boolean {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, allDay)
        description?.takeIf { it.isNotBlank() }?.let { intent.putExtra(CalendarContract.Events.DESCRIPTION, it) }
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}
