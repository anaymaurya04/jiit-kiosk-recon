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
 * one per course for every submission state at once (`courseWorkId = "-"`).
 * Items outside a recent window are not carried at all — the tab talks about
 * the near present.
 */
class ClassroomClient(private val token: String) {

    fun fetch(): ClassroomData {
        val courses = page("courses?courseStates=ACTIVE&studentId=me&pageSize=100", "courses")
            .map { Course(id = it.getString("id"), name = it.optString("name").ifBlank { "Untitled course" }) }

        val now = System.currentTimeMillis()
        val windowStart = now - 30L * DAY
        val windowEnd = now + 60L * DAY

        val work = ArrayList<Work>()
        for (course in courses) {
            val items = page(
                "courses/${course.id}/courseWork?courseWorkStates=PUBLISHED&orderBy=dueDate+asc&pageSize=100",
                "courseWork",
            )
            val mine = submissions(course.id)
            for (raw in items) {
                val id = raw.optString("id")
                if (id.isBlank()) continue
                val dueAt = dueAt(raw)
                if (dueAt != null && (dueAt < windowStart || dueAt > windowEnd)) continue
                // absent from the map means the state could not be read: unknown, never "not submitted"
                val (submitted, submittedAt) = mine[id] ?: (null to null)
                work += Work(
                    id = id,
                    courseId = course.id,
                    courseName = course.name,
                    title = raw.optString("title").ifBlank { "Untitled work" },
                    dueAt = dueAt,
                    dueHasTime = raw.optJSONObject("dueTime") != null,
                    submitted = submitted,
                    submittedAt = submittedAt,
                    link = raw.optString("alternateLink").ifBlank { null },
                )
            }
        }
        // nearest deadline first, the undated last
        work.sortWith(compareBy({ it.dueAt == null }, { it.dueAt ?: Long.MAX_VALUE }))
        return ClassroomData(courses, work)
    }

    /** every submission of mine in one course, keyed by coursework id; empty on a non-auth failure */
    private fun submissions(courseId: String): Map<String, Pair<Boolean?, Long?>> {
        val items = try {
            page(
                "courses/$courseId/courseWork/-/studentSubmissions?userId=me&pageSize=100",
                "studentSubmissions",
            )
        } catch (e: HttpError) {
            if (e.code == 401 || e.code == 403) throw e
            return emptyMap()
        }
        val out = HashMap<String, Pair<Boolean?, Long?>>()
        for (s in items) {
            val workId = s.optString("courseWorkId")
            if (workId.isBlank() || workId in out) continue
            val state = s.optString("state")
            out[workId] = (state == "TURNED_IN" || state == "RETURNED") to
                // on TURNED_IN the update time is the turn-in; on RETURNED it is the grade,
                // so a "SUBMITTED <date>" from it would be a different date than the act
                if (state == "TURNED_IN") time(s.optString("updateTime")) else null
        }
        return out
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
    }
}
