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
import java.util.Locale

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

        val materials = ArrayList<Material>()
        for (course in courses) {
            // topic names are a nicety for the Notes tab; never fail a sync over them
            val topics = runCatching {
                page("courses/${course.id}/topics?pageSize=100", "topic")
                    .associate { it.optString("topicId") to it.optString("name").ifBlank { "Untitled topic" } }
            }.getOrDefault(emptyMap())
            val posts = page(
                "courses/${course.id}/courseWorkMaterials?courseWorkMaterialStates=PUBLISHED" +
                    "&orderBy=creationTime+desc&pageSize=100",
                "courseWorkMaterial",
            )
            for (raw in posts) {
                val id = raw.optString("id")
                if (id.isBlank()) continue
                val attached = firstAttachment(raw) ?: continue
                materials += Material(
                    id = id,
                    courseId = course.id,
                    courseName = course.name,
                    title = raw.optString("title").ifBlank { attached.first },
                    topic = raw.optString("topicId").takeIf { it.isNotBlank() }?.let(topics::get),
                    kind = attached.second,
                    link = attached.third,
                    createdAt = time(raw.optString("creationTime")) ?: 0,
                    updatedAt = time(raw.optString("updateTime")) ?: 0,
                )
            }
        }
        return ClassroomData(courses, work, materials)
    }

    /** the first attachment of a material post as (name, kind, link); a bare post has no row on the board */
    private fun firstAttachment(raw: JSONObject): Triple<String, MaterialKind, String?>? {
        val list = raw.optJSONArray("materials") ?: return null
        for (i in 0 until list.length()) {
            val m = list.optJSONObject(i) ?: continue
            m.optJSONObject("driveFile")?.let { outer ->
                // the REST API nests the file once more: {"driveFile": {"driveFile": {...}}}
                val file = outer.optJSONObject("driveFile") ?: outer
                val name = file.optString("title").ifBlank { "File" }
                return Triple(name, kindOf(name), file.optString("alternateLink").ifBlank { null })
            }
            m.optJSONObject("youtubeVideo")?.let {
                return Triple(
                    it.optString("title").ifBlank { "Video" },
                    MaterialKind.Link,
                    it.optString("alternateLink").ifBlank { null },
                )
            }
            m.optJSONObject("link")?.let {
                return Triple(
                    it.optString("title").ifBlank { "Link" },
                    MaterialKind.Link,
                    it.optString("url").ifBlank { null },
                )
            }
            m.optJSONObject("form")?.let {
                return Triple(
                    it.optString("title").ifBlank { "Form" },
                    MaterialKind.Link,
                    it.optString("formUrl").ifBlank { null },
                )
            }
        }
        return null
    }

    /** the badge from the file's own name; anything we cannot name opens as a link anyway */
    private fun kindOf(name: String): MaterialKind =
        when (name.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
            "pdf" -> MaterialKind.Pdf
            "ppt", "pptx", "key" -> MaterialKind.Slides
            "doc", "docx", "odt", "rtf", "txt" -> MaterialKind.Docs
            else -> MaterialKind.Link
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
