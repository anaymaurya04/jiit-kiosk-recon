package com.jiitkiosk.app.code

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The GitHub parse contract on a fixture: repo rows become stars, a language
 * ranking and the newest four; events collapse into UTC days; the GraphQL
 * calendar keeps exact dates; HTTP failures and a moved schema are typed
 * states, not crashes.
 */
class GitHubParseTest {

    private val today = LocalDate.parse("2026-10-16").toEpochDay()

    private fun day(iso: String) = LocalDate.parse(iso).toEpochDay()

    private fun rows(): List<GhRepo> = GitHubClient.parseRepos(
        """
        [
          {"name":"b","description":null,"language":"Kotlin","stargazers_count":3},
          {"name":"a","description":"x","language":"Kotlin","stargazers_count":0},
          {"name":"c","description":"y","language":"Python","stargazers_count":1},
          {"name":"d","description":null,"language":"Python","stargazers_count":0},
          {"name":"e","description":null,"language":"TypeScript","stargazers_count":0},
          {"name":"f","description":null,"language":"Rust","stargazers_count":0},
          {"name":"g","description":null,"language":null,"stargazers_count":0}
        ]
        """.trimIndent(),
    )

    @Test
    fun `repos assemble into stars, a language ranking and the newest four`() {
        val parsed = rows()
        assertEquals(7, parsed.size)

        val stats = GitHubClient.assemble(
            handle = "me",
            repos = 7,
            rows = parsed,
            perDay = mapOf(today to 2, today - 1 to 1),
            today = today,
            publicActivityOnly = true,
        )

        assertEquals(4, stats.stars)
        // count first, name as the tie-break: Rust outruns TypeScript purely on the alphabet
        assertEquals(
            listOf("Kotlin" to 2, "Python" to 2, "Rust" to 1),
            stats.languages.map { it.name to it.repos },
        )
        assertEquals(1, stats.otherLangRepos)
        assertEquals(listOf("b", "a", "c", "d"), stats.top.map { it.name })
        assertEquals(2, stats.streak)
        assertEquals(154, stats.days.size)
        // epoch day 0 was a Thursday, so Sundays land on day % 7 == 3
        assertEquals(3L, stats.startDay % 7)
        assertTrue(stats.publicActivityOnly)
    }

    @Test
    fun `events collapse into UTC day counts`() {
        val days = GitHubClient.parseEvents(
            """
            [
              {"created_at":"2026-10-15T10:00:00Z"},
              {"created_at":"2026-10-15T23:59:00Z"},
              {"created_at":"2026-10-13T00:00:00Z"},
              {"created_at":"nonsense"}
            ]
            """.trimIndent(),
        )
        assertEquals(2, days.size)
        assertEquals(2, days[day("2026-10-15")])
        assertEquals(1, days[day("2026-10-13")])
    }

    @Test
    fun `the graph calendar keeps exact dates`() {
        val days = GitHubClient.parseGraphCalendar(
            """
            {"data":{"user":{"contributionsCollection":{"contributionCalendar":{"weeks":[
              {"contributionDays":[{"date":"2026-10-15","contributionCount":2},{"date":"2026-10-16","contributionCount":0}]}
            ]}}}}}
            """.trimIndent(),
        )
        assertEquals(2, days[day("2026-10-15")])
        assertEquals(0, days[day("2026-10-16")])
    }

    @Test
    fun `http failures map to typed states`() {
        val noUser = assertThrows(GhNoUser::class.java) {
            GitHubClient.mapHttpError(404, """{"message":"Not Found"}""", "nobody")
        }
        assertEquals("nobody", noUser.handle)
        assertThrows(GhToken::class.java) { GitHubClient.mapHttpError(401, "", "me") }
        val rate = assertThrows(GhSchema::class.java) {
            GitHubClient.mapHttpError(403, """{"message":"API rate limit exceeded for 1.2.3.4."}""", "me")
        }
        assertTrue(rate.message!!.contains("rate limit"))
    }

    @Test
    fun `schema drift is typed, not fatal`() {
        assertThrows(GhSchema::class.java) { GitHubClient.parseUser("{}") }
        assertThrows(GhSchema::class.java) { GitHubClient.parseRepos("{}") }
        assertThrows(GhSchema::class.java) { GitHubClient.parseEvents("not json") }
        assertThrows(GhSchema::class.java) {
            GitHubClient.parseGraphCalendar("""{"errors":[{"message":"Could not resolve to a User."}]}""")
        }
    }
}
