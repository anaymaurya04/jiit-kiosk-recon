package com.jiitkiosk.app.code

import com.jiitkiosk.app.data.HttpError
import com.jiitkiosk.app.data.HttpJson
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** There is no GitHub account with that handle. */
class GhNoUser(val handle: String) : IllegalStateException("No GitHub profile for @$handle")

/** GitHub rejected the saved token. */
class GhToken : IllegalStateException("GitHub rejected the saved token — fix or clear it in Settings.")

/** GitHub answered, but not in the shape this app knows. */
class GhSchema(message: String) : IllegalStateException(message)

@Serializable
data class GhRepo(val name: String, val description: String?, val language: String?, val stars: Int)

@Serializable
data class GhLang(val name: String, val repos: Int)

@Serializable
data class GitHubStats(
    val handle: String,
    val repos: Int,
    val stars: Int,
    val streak: Int,
    /** epoch day (UTC) of the first heatmap cell, a Sunday; [days] is 22 whole weeks from there */
    val startDay: Long,
    val days: List<Int>,
    /** the three most common primary languages, by repo count */
    val languages: List<GhLang>,
    val otherLangRepos: Int,
    val top: List<GhRepo>,
    /** true when the numbers come from `/events/public` alone — the screen says so */
    val publicActivityOnly: Boolean,
)

/** the validators GitHub offered last visit, one per endpoint */
data class GhEtags(val user: String? = null, val repos: String? = null, val events: String? = null)

/**
 * GitHub's REST API, public data only unless the user saved a token. Every GET
 * carries the last ETag, so an unchanged account answers 304 and nothing is
 * re-written or re-downloaded. With a token the contribution graph comes from
 * GraphQL; without one the public events feed stands in, labeled as such.
 */
object GitHubClient {

    private const val API = "https://api.github.com"
    private val HEADERS = mapOf(
        "Accept" to "application/vnd.github+json",
        "X-GitHub-Api-Version" to "2022-11-28",
        "User-Agent" to "jiit-kiosk",
    )

    private val GRAPH_QUERY = """
        query(${'$'}u: String!) {
          user(login: ${'$'}u) {
            contributionsCollection { contributionCalendar { weeks { contributionDays { date contributionCount } } } }
          }
        }
    """.trimIndent()

    data class Fetch(val stats: GitHubStats, val changed: Boolean, val etags: GhEtags)

    /** 404 is a missing handle, 401 a dead token, 403 usually the rate limit; the message is GitHub's own */
    fun mapHttpError(code: Int, body: String, handle: String): Nothing = when (code) {
        404 -> throw GhNoUser(handle)
        401 -> throw GhToken()
        403 -> throw GhSchema(messageOf(body) ?: "GitHub refused the request (HTTP 403).")
        else -> throw HttpError(code, body)
    }

    private fun messageOf(body: String): String? = runCatching {
        (Json.parseToJsonElement(body) as JsonObject)["message"]?.jsonPrimitive?.contentOrNull
    }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun JsonElement?.asObj(): JsonObject? = this as? JsonObject
    private fun JsonElement?.asArr(): JsonArray? = this as? JsonArray

    /** [text] is `/users/{u}`: the profile's own repo count */
    fun parseUser(text: String): Int {
        val obj = runCatching { Json.parseToJsonElement(text) }.getOrNull().asObj()
            ?: throw GhSchema("GitHub did not answer with a user object.")
        return obj["public_repos"]?.jsonPrimitive?.intOrNull
            ?: throw GhSchema("The user answer carried no repo count.")
    }

    /** [text] is `/users/{u}/repos`, already ordered the way the screen shows it */
    fun parseRepos(text: String): List<GhRepo> {
        val arr = runCatching { Json.parseToJsonElement(text) }.getOrNull().asArr()
            ?: throw GhSchema("GitHub did not answer with a repo list.")
        return arr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val name = o["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            GhRepo(
                name = name,
                description = o["description"]?.jsonPrimitive?.contentOrNull,
                language = o["language"]?.jsonPrimitive?.contentOrNull,
                stars = o["stargazers_count"]?.jsonPrimitive?.intOrNull ?: 0,
            )
        }
    }

    /** [text] is `/users/{u}/events/public`: one count per UTC day with a public event */
    fun parseEvents(text: String): Map<Long, Int> {
        val arr = runCatching { Json.parseToJsonElement(text) }.getOrNull().asArr()
            ?: throw GhSchema("GitHub did not answer with an event list.")
        return arr.mapNotNull { el ->
            val created = (el as? JsonObject)?.get("created_at")?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null
            runCatching { Instant.parse(created).epochSecond }.getOrNull()?.let { dayOf(it) }
        }.groupingBy { it }.eachCount()
    }

    /** [text] is the GraphQL contribution calendar: exact dates, up to a year back */
    fun parseGraphCalendar(text: String): Map<Long, Int> {
        val root = runCatching { Json.parseToJsonElement(text) }.getOrNull().asObj()
            ?: throw GhSchema("GitHub did not answer with JSON.")
        (root["errors"] as? JsonArray)?.takeIf { it.isNotEmpty() }?.let { errors ->
            throw GhSchema(
                errors.mapNotNull { (it as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull }
                    .joinToString("; ").ifEmpty { "GitHub returned an error." },
            )
        }
        val weeks = root["data"].asObj()?.get("user").asObj()
            ?.get("contributionsCollection").asObj()
            ?.get("contributionCalendar").asObj()
            ?.get("weeks").asArr()
            ?: throw GhSchema("No contribution calendar in the answer.")
        return weeks.flatMap { (it as? JsonObject)?.get("contributionDays").asArr().orEmpty() }
            .mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val date = o["date"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val count = o["contributionCount"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                runCatching { LocalDate.parse(date).toEpochDay() }.getOrNull()?.let { it to count }
            }
            .toMap()
    }

    /** everything the screen shows, from the pieces both paths provide */
    fun assemble(
        handle: String,
        repos: Int,
        rows: List<GhRepo>,
        perDay: Map<Long, Int>,
        today: Long,
        publicActivityOnly: Boolean,
    ): GitHubStats {
        val ranked = rows.mapNotNull { it.language }
            .groupingBy { it }.eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        val startDay = windowStart(today)
        return GitHubStats(
            handle = handle,
            repos = repos,
            stars = rows.sumOf { it.stars },
            streak = streak(perDay, today),
            startDay = startDay,
            days = daysOf(perDay, startDay),
            languages = ranked.take(3).map { GhLang(it.key, it.value) },
            otherLangRepos = ranked.drop(3).sumOf { it.value },
            top = rows.take(4),
            publicActivityOnly = publicActivityOnly,
        )
    }

    suspend fun fetch(
        handle: String,
        token: String?,
        etags: GhEtags,
        cached: GitHubStats?,
    ): Fetch = withContext(Dispatchers.IO) {
        fun rest(url: String, etag: String?): com.jiitkiosk.app.data.Tagged = try {
            HttpJson.getIfNoneMatch(url, etag, token, HEADERS)
        } catch (e: HttpError) {
            mapHttpError(e.code, e.body, handle)
        }

        val userRes = rest("$API/users/$handle", etags.user)
        val reposRes = rest("$API/users/$handle/repos?per_page=100&sort=updated", etags.repos)

        // page one is the conditional one; the rest ride fresh only when it moved
        val rows: List<GhRepo>? = reposRes.body?.let { first ->
            var all = parseRepos(first)
            // shortcut: stars count the newest 300 repos, plenty for a personal account
            while (all.size % 100 == 0 && all.size in 100..200) {
                val page = all.size / 100 + 1
                val more = rest("$API/users/$handle/repos?per_page=100&sort=updated&page=$page", null).body
                    ?: throw GhSchema("A repo page went missing.")
                all = all + parseRepos(more)
            }
            all
        }

        var eventsEtag = etags.events
        val perDay: Map<Long, Int>? = if (token != null) {
            parseGraphCalendar(
                HttpJson.post(
                    "$API/graphql",
                    buildJsonObject {
                        put("query", JsonPrimitive(GRAPH_QUERY))
                        put("variables", buildJsonObject { put("u", JsonPrimitive(handle)) })
                    }.toString(),
                    bearer = token,
                    headers = HEADERS,
                ),
            )
        } else {
            val first = rest("$API/users/$handle/events/public?per_page=100", etags.events)
            eventsEtag = first.etag
            first.body?.let { body ->
                var days = parseEvents(body)
                // the public feed holds about 90 days, so three pages cover it
                var page = 2
                var last = arraySize(body)
                while (page <= 3 && last == 100) {
                    val next = rest("$API/users/$handle/events/public?per_page=100&page=$page", null).body
                        ?: break
                    days = plus(days, parseEvents(next))
                    last = arraySize(next)
                    page++
                }
                days
            }
        }

        val nothing = GhSchema("Nothing cached to fall back on.")
        val reposCount = userRes.body?.let { parseUser(it) } ?: cached?.repos ?: throw nothing
        val usable = perDay ?: cached?.let { dayMap(it) } ?: throw nothing
        val today = maxOf(dayOf(System.currentTimeMillis() / 1000L), usable.keys.maxOrNull() ?: 0L)

        val stats = if (rows != null) {
            assemble(handle, reposCount, rows, usable, today, token == null)
        } else {
            (cached ?: throw nothing).copy(
                handle = handle,
                repos = reposCount,
                streak = streak(usable, today),
                startDay = windowStart(today),
                days = daysOf(usable, windowStart(today)),
                publicActivityOnly = token == null,
            )
        }
        Fetch(
            stats = stats,
            changed = userRes.body != null || reposRes.body != null || perDay != null,
            etags = GhEtags(
                user = userRes.body?.let { userRes.etag } ?: etags.user,
                repos = reposRes.body?.let { reposRes.etag } ?: etags.repos,
                events = eventsEtag,
            ),
        )
    }

    private fun dayMap(stats: GitHubStats): Map<Long, Int> =
        stats.days.indices.associate { stats.startDay + it to stats.days[it] }

    private fun plus(a: Map<Long, Int>, b: Map<Long, Int>): Map<Long, Int> {
        val out = a.toMutableMap()
        b.forEach { (k, v) -> out[k] = (out[k] ?: 0) + v }
        return out
    }

    private fun arraySize(text: String): Int =
        runCatching { Json.parseToJsonElement(text).asArr()?.size ?: 0 }.getOrDefault(0)
}
