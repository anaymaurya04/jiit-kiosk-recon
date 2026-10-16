package com.jiitkiosk.app.classroom

import com.jiitkiosk.app.data.HttpError
import com.jiitkiosk.app.data.HttpJson
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * The Classroom REST v1 reads behind the three read-only scopes: active
 * courses, their published coursework, and the student's own submission per
 * item. Nothing is ever written back.
 *
 * Request budget: one call for the courses, one per course for its coursework,
 * one per item for the submission state (capped). Items outside a recent
 * window are not carried at all — the tab talks about the near present.
 */
class ClassroomClient(private val token: String) {

    fun fetch(): ClassroomData {
        val courses = page("courses?courseStates=ACTIVE&studentId=me&pageSize=100", "courses")
            .map { Course(id = it.getString("id"), name = it.optString("name").ifBlank { "Untitled course" }) }

        val now = System.currentTimeMillis()
        val windowStart = now - 30L * DAY
        val windowEnd = now + 60L * DAY

        val work = ArrayList<Work>()
        var lookups = 0
        for (course in courses) {
            val items = page(
                "courses/${course.id}/courseWork?courseWorkStates=PUBLISHED&orderBy=dueDate+asc&pageSize=100",
                "courseWork",
            )
            for (raw in items) {
                val id = raw.optString("id")
                if (id.isBlank()) continue
                val dueAt = dueAt(raw)
                if (dueAt != null && (dueAt < windowStart || dueAt > windowEnd)) continue
                val (submitted, submittedAt) = if (lookups < SUBMISSION_CAP) {
                    lookups++
                    submission(course.id, id)
                } else {
                    // past the request budget: say "unknown" instead of "not submitted"
                    null to null
                }
                work += Work(
                    id = id,
                    courseId = course.id,
                    courseName = course.name,
                    title = raw.optString("title").ifBlank { "Untitled work" },
                    dueAt = dueAt,
                    dueHasTime = raw.optJSONObject("dueTime") != null,
                    submitted = submitted,
                    submittedAt = submittedAt,
                )
            }
        }
        // nearest deadline first, the undated last
        work.sortWith(compareBy({ it.dueAt == null }, { it.dueAt ?: Long.MAX_VALUE }))
        return ClassroomData(courses, work)
    }

    /** null pair when the state could not be read; only 401/403 may abort the fetch */
    private fun submission(courseId: String, workId: String): Pair<Boolean?, Long?> {
        val items = try {
            page("courses/$courseId/courseWork/$workId/studentSubmissions?userId=me", "studentSubmissions")
        } catch (e: HttpError) {
            if (e.code == 401 || e.code == 403) throw e
            return null to null
        }
        val first = items.firstOrNull() ?: return null to null
        val state = first.optString("state")
        val submitted = state == "TURNED_IN" || state == "RETURNED"
        // on TURNED_IN the update time is the turn-in; on RETURNED it is the grade,
        // so a "SUBMITTED <date>" from it would be a different date than the act
        val at = if (state == "TURNED_IN") time(first.optString("updateTime")) else null
        return submitted to at
    }

    private fun page(path: String, key: String): List<JSONObject> {
        val out = ArrayList<JSONObject>()
        var pageToken: String? = null
        do {
            val separator = if ('?' in path) '&' else '?'
            val url = BASE + path +
                (pageToken?.let { separator + "pageToken=" + URLEncoder.encode(it, "UTF-8") } ?: "")
            val body = JSONObject(HttpJson.get(url, bearer = token))
            val array: JSONArray = body.optJSONArray(key) ?: JSONArray()
            for (i in 0 until array.length()) out += array.getJSONObject(i)
            pageToken = body.optString("nextPageToken").ifBlank { null }
        } while (pageToken != null)
        return out
    }

    /** dueDate/dueTime arrive in UTC; a missing dueTime means "that day", not a time to invent */
    private fun dueAt(raw: JSONObject): Long? {
        val date = raw.optJSONObject("dueDate") ?: return null
        val day = runCatching {
            LocalDate.of(date.optInt("year"), date.optInt("month"), date.optInt("day"))
        }.getOrNull() ?: return null
        val clock = raw.optJSONObject("dueTime")
            ?.let { LocalTime.of(it.optInt("hours"), it.optInt("minutes")) }
            ?: LocalTime.of(23, 59)
        return day.atTime(clock).toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    private fun time(iso: String): Long? =
        iso.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

    private companion object {
        const val BASE = "https://classroom.googleapis.com/v1/"
        const val DAY = 24L * 3_600_000
        const val SUBMISSION_CAP = 60
    }
}
