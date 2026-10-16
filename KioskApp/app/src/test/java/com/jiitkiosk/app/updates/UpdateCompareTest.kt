package com.jiitkiosk.app.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCompareTest {

    @Test
    fun patchBumpIsNewer() {
        assertTrue(Updates.newer("0.2.0", "0.2.1"))
    }

    @Test
    fun vPrefixIsIgnored() {
        assertTrue(Updates.newer("0.2.0", "v0.3.0"))
    }

    @Test
    fun sameVersionIsNotNewer() {
        assertFalse(Updates.newer("0.2.0", "v0.2.0"))
    }

    @Test
    fun doubleDigitPatchComparesNumerically() {
        assertTrue(Updates.newer("0.2.9", "0.2.10"))
    }

    @Test
    fun minorBumpBeatsPatch() {
        assertTrue(Updates.newer("0.2.9", "0.3.0"))
    }

    @Test
    fun olderReleaseIsNotNewer() {
        assertFalse(Updates.newer("0.3.0", "0.2.9"))
    }

    @Test
    fun shorterTagPadsWithZero() {
        assertFalse(Updates.newer("0.2.0", "0.2"))
    }

    @Test
    fun prereleaseSuffixIsDropped() {
        assertFalse(Updates.newer("0.2.0", "0.2.0-rc1"))
    }

    @Test
    fun unparseableTagNeverClaimsAnUpdate() {
        assertFalse(Updates.newer("0.2.0", "nightly"))
    }

    @Test
    fun liveReleaseShapeParses() {
        val release = Updates.release(
            """{"tag_name":"v0.1","name":"jiit-kiosk","draft":false,"html_url":"https://github.com/anaymaurya04/jiit-kiosk-recon/releases/tag/v0.1"}""",
        )
        assertEquals("v0.1", release.tag)
        assertEquals("https://github.com/anaymaurya04/jiit-kiosk-recon/releases/tag/v0.1", release.url)
    }

    @Test
    fun aFeedWithoutATagIsBlank() {
        assertEquals("", Updates.release("""{"message":"Not Found"}""").tag)
    }
}
