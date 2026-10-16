package com.jiitkiosk.app.code

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The parse contract, pinned with a fixture instead of a live account: totals by
 * difficulty, a Sunday-aligned 22-week heatmap, a streak that survives an
 * unsolved today, the top five tags, newest-first accepted submissions — and a
 * moved schema or an unknown handle as typed failures, not crashes.
 */
class LeetCodeParseTest {

    // 2027-01-15T08:00:00Z — any instant works, it just has to be pinned
    private val nowSec = 1_800_000_000L
    private val today = nowSec / 86_400L

    private fun day(offset: Long, n: Int) = (today - offset) * 86_400L to n

    private fun calendar(vararg days: Pair<Long, Int>): String =
        days.joinToString(",") { (ts, n) -> "\\\"$ts\\\":$n" }

    private fun response(
        totals: String =
            """[{"difficulty":"All","count":154},{"difficulty":"Easy","count":84},{"difficulty":"Medium","count":61},{"difficulty":"Hard","count":9}]""",
        tags: String =
            """{"fundamental":[{"tagName":"Array","problemsSolved":52},{"tagName":"String","problemsSolved":30}],""" +
                """"intermediate":[{"tagName":"Dynamic Programming","problemsSolved":21},{"tagName":"Tree","problemsSolved":18}],""" +
                """"advanced":[{"tagName":"Graph","problemsSolved":14},{"tagName":"Segment Tree","problemsSolved":2}]}""",
        calendar: String = calendar(day(0, 3), day(1, 1), day(2, 5)),
        recent: String =
            """[{"title":"Two Sum","titleSlug":"two-sum","timestamp":"1800000000"},""" +
                """{"title":"Add Two Numbers","titleSlug":"add-two-numbers","timestamp":"1799999000"}]""",
        user: String =
            """{"username":"kolsar","submitStatsGlobal":{"acSubmissionNum":$totals},""" +
                """"tagProblemCounts":$tags,"userCalendar":{"submissionCalendar":"{$calendar}"}}""",
    ): String =
        """{"data":{"matchedUser":$user,"recentAcSubmissionList":$recent}}"""

    @Test
    fun `totals, ring numbers and a Sunday-aligned heatmap`() {
        val stats = LeetCodeClient.parse(response(), nowSec, "kolsar")

        assertEquals(154, stats.total)
        assertEquals(84, stats.easy)
        assertEquals(61, stats.medium)
        assertEquals(9, stats.hard)
        assertEquals(154, stats.days.size)
        // epoch day 0 was a Thursday, so Sundays land on day % 7 == 3
        assertEquals(3L, stats.startDay % 7)

        val todayIndex = (today - stats.startDay).toInt()
        assertEquals(3, stats.days[todayIndex])
        assertEquals(1, stats.days[todayIndex - 1])
        assertEquals(5, stats.days[todayIndex - 2])
        assertEquals(3, stats.streak)
    }

    @Test
    fun `streak keeps counting from yesterday when today is unsolved`() {
        val stats = LeetCodeClient.parse(
            response(calendar = calendar(day(1, 1), day(2, 1), day(3, 1))),
            nowSec,
            "kolsar",
        )
        assertEquals(3, stats.streak)
    }

    @Test
    fun `a gap ends the streak and future days are empty`() {
        val stats = LeetCodeClient.parse(
            response(calendar = calendar(day(1, 1), day(3, 1), day(4, 2))),
            nowSec,
            "kolsar",
        )
        assertEquals(1, stats.streak)
        val todayIndex = (today - stats.startDay).toInt()
        for (i in todayIndex + 1 until stats.days.size) {
            assertEquals(0, stats.days[i])
        }
    }

    @Test
    fun `topics are the top five by solved count`() {
        val stats = LeetCodeClient.parse(response(), nowSec, "kolsar")

        assertEquals(
            listOf("Array" to 52, "String" to 30, "Dynamic Programming" to 21, "Tree" to 18, "Graph" to 14),
            stats.topics.map { it.name to it.solved },
        )
    }

    @Test
    fun `recent accepted submissions are newest first`() {
        val stats = LeetCodeClient.parse(response(), nowSec, "kolsar")

        assertEquals(2, stats.recent.size)
        assertEquals("two-sum", stats.recent[0].slug)
        assertEquals(1_800_000_000_000L, stats.recent[0].at)
        assertEquals("add-two-numbers", stats.recent[1].slug)
    }

    @Test
    fun `an unknown handle is a typed failure`() {
        val failure = assertThrows(LeetNoUser::class.java) {
            LeetCodeClient.parse(response(user = "null"), nowSec, "nobody-here")
        }
        assertEquals("nobody-here", failure.handle)
    }

    @Test
    fun `a moved schema is a typed failure`() {
        assertThrows(LeetSchema::class.java) {
            LeetCodeClient.parse("""{"data":{"matchedUser":{"username":"kolsar"}}}""", nowSec, "kolsar")
        }
        assertThrows(LeetSchema::class.java) {
            LeetCodeClient.parse("""{"errors":[{"message":"rate limited"}]}""", nowSec, "kolsar")
        }
        assertThrows(LeetSchema::class.java) {
            LeetCodeClient.parse("<html>cloudflare</html>", nowSec, "kolsar")
        }
    }
}
