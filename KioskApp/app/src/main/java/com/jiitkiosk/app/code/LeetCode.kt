package com.jiitkiosk.app.code

import com.jiitkiosk.app.data.HttpJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** LeetCode answered, but not in the shape this app knows: a sync failure, never a crash. */
class LeetSchema(message: String) : IllegalStateException(message)

/** There is no LeetCode account with that handle. */
class LeetNoUser(val handle: String) : IllegalStateException("No LeetCode profile for @$handle")

@Serializable
data class LeetTopic(val name: String, val solved: Int)

@Serializable
data class LeetRecent(val title: String, val slug: String, val at: Long)

@Serializable
data class LeetStats(
    val handle: String,
    val total: Int,
    val easy: Int,
    val medium: Int,
    val hard: Int,
    val streak: Int,
    /** epoch day (UTC) of the first heatmap cell, a Sunday; [days] is 22 whole weeks from there */
    val startDay: Long,
    val days: List<Int>,
    val topics: List<LeetTopic>,
    val recent: List<LeetRecent>,
)

/** the public API is asked again only after this long */
const val LEET_TTL_MS = 15 * 60_000L

private const val WEEKS = 22
private const val ONE_DAY_S = 86_400L

private val json = Json { ignoreUnknownKeys = true }

/**
 * LeetCode's public GraphQL, read-only and unauthenticated. One POST asks for
 * everything the board shows and the parser is pure, so the shape of the answer
 * is proven by a fixture test instead of by somebody else's live account.
 */
object LeetCodeClient {

    const val ENDPOINT = "https://leetcode.com/graphql"

    private val QUERY = """
        query(${'$'}u: String!) {
          matchedUser(username: ${'$'}u) {
            username
            submitStatsGlobal { acSubmissionNum { difficulty count } }
            tagProblemCounts {
              fundamental { tagName problemsSolved }
              intermediate { tagName problemsSolved }
              advanced { tagName problemsSolved }
            }
            userCalendar { submissionCalendar }
          }
          recentAcSubmissionList(username: ${'$'}u, limit: 12) { title titleSlug timestamp }
        }
    """.trimIndent()

    fun body(handle: String): String = buildJsonObject {
        put("query", JsonPrimitive(QUERY))
        put("variables", buildJsonObject { put("u", JsonPrimitive(handle)) })
    }.toString()

    /**
     * [nowSec] anchors the heatmap and the streak; tests pin it, the app does
     * not. Throws [LeetNoUser], [LeetSchema] or whatever the HTTP layer threw —
     * the screen owns the words for all of them.
     */
    fun parse(text: String, nowSec: Long, handle: String): LeetStats {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() as? JsonObject
            ?: throw LeetSchema("LeetCode did not answer with JSON.")
        (root["errors"] as? JsonArray)?.takeIf { it.isNotEmpty() }?.let { errors ->
            throw LeetSchema(
                errors.mapNotNull { (it as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull }
                    .joinToString("; ").ifEmpty { "LeetCode returned an error." },
            )
        }
        val data = root["data"] as? JsonObject ?: throw LeetSchema("LeetCode returned no data.")
        val user = data["matchedUser"] as? JsonObject ?: throw LeetNoUser(handle)

        val counts = user["submitStatsGlobal"]?.jsonObject?.get("acSubmissionNum")?.jsonArray
            ?.mapNotNull { el ->
                val row = el as? JsonObject ?: return@mapNotNull null
                val key = row["difficulty"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                key to (row["count"]?.jsonPrimitive?.intOrNull ?: 0)
            }
            ?.toMap()
            ?: throw LeetSchema("No submission totals in the answer.")
        val easy = counts["Easy"] ?: 0
        val medium = counts["Medium"] ?: 0
        val hard = counts["Hard"] ?: 0

        val calendar = user["userCalendar"]?.jsonObject?.get("submissionCalendar")?.jsonPrimitive?.contentOrNull
            ?: throw LeetSchema("No submission calendar in the answer.")
        val perDay: Map<Long, Int> = runCatching { json.parseToJsonElement(calendar) }.getOrNull()
            ?.let { it as? JsonObject }
            ?.mapNotNull { (k, v) ->
                val day = k.toLongOrNull()?.div(ONE_DAY_S) ?: return@mapNotNull null
                day to (v.jsonPrimitive.intOrNull ?: 0)
            }
            ?.toMap()
            ?: throw LeetSchema("The submission calendar was not readable.")

        val today = nowSec / ONE_DAY_S
        // top row is Sunday; 1970-01-01 was a Thursday, so +4 lines the epoch up with Sunday
        val thisSunday = today - ((today + 4) % 7)
        val startDay = thisSunday - (WEEKS - 1) * 7L
        val days = (0 until WEEKS * 7).map { i -> perDay[startDay + i] ?: 0 }

        val topics = listOf("fundamental", "intermediate", "advanced")
            .flatMap { group ->
                (user["tagProblemCounts"]?.jsonObject?.get(group) as? JsonArray).orEmpty().mapNotNull { el ->
                    val row = el as? JsonObject ?: return@mapNotNull null
                    val name = row["tagName"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    name to (row["problemsSolved"]?.jsonPrimitive?.intOrNull ?: 0)
                }
            }
            .filter { (_, solved) -> solved > 0 }
            .sortedByDescending { (_, solved) -> solved }
            .take(5)
            .map { (name, solved) -> LeetTopic(name, solved) }

        val recent = (data["recentAcSubmissionList"] as? JsonArray).orEmpty()
            .mapNotNull { el ->
                val row = el as? JsonObject ?: return@mapNotNull null
                val title = row["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val slug = row["titleSlug"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val at = row["timestamp"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@mapNotNull null
                LeetRecent(title, slug, at * 1000L)
            }
            .sortedByDescending { it.at }
            .take(10)

        return LeetStats(
            handle = handle,
            total = counts["All"] ?: (easy + medium + hard),
            easy = easy,
            medium = medium,
            hard = hard,
            streak = streak(perDay, today),
            startDay = startDay,
            days = days,
            topics = topics,
            recent = recent,
        )
    }

    /** consecutive accepted days ending today or yesterday: today may still be unsolved */
    private fun streak(perDay: Map<Long, Int>, today: Long): Int {
        var day = if ((perDay[today] ?: 0) > 0) today else today - 1
        var n = 0
        while ((perDay[day] ?: 0) > 0) {
            n++
            day--
        }
        return n
    }

    suspend fun fetch(handle: String): LeetStats = withContext(Dispatchers.IO) {
        // the GraphQL endpoint likes a browser-shaped POST; without these it can answer with a CSRF error page
        val text = HttpJson.post(
            ENDPOINT,
            body(handle),
            headers = mapOf("Origin" to "https://leetcode.com", "Referer" to "https://leetcode.com/$handle/"),
        )
        parse(text, System.currentTimeMillis() / 1000L, handle)
    }
}
