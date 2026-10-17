package com.jiitkiosk.app.data

import com.jiitkiosk.app.ui.me.MeData
import `in`.codelif.ktjiit.model.BankInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** The whole point of this app on a bad portal day, so it gets to be tested. */
class CacheTest {

    private fun fresh(): Cache = Cache(Files.createTempDirectory("kiosk-cache").toFile())

    @Test
    fun aWrittenAnswerComesBack() {
        val cache = fresh()
        val value = MeData(bank = BankInfo(bank = "SBI", account = "1234"))

        assertNull("nothing cached yet", cache.read("me/t", MeData.serializer()))

        cache.write("me/t", MeData.serializer(), value)

        val back = cache.read("me/t", MeData.serializer())
        assertEquals(value, back?.value)
        // anything read back off disk is by definition an earlier answer
        assertTrue("a file on disk is stale by construction", back!!.stale)
    }

    @Test
    fun anUnreadableEntryIsNotAnError() {
        val dir = Files.createTempDirectory("kiosk-cache").toFile()
        val cache = Cache(dir)
        cache.write("me/t", MeData.serializer(), MeData())

        // a half-written or older-schema file must not blow up the screen
        File(dir, "portal-cache").resolve("me_t.json").writeText("{ not json")

        assertNull(cache.read("me/t", MeData.serializer()))
    }

    @Test
    fun signingOutTakesTheDataWithIt() {
        val cache = fresh()
        cache.write("me/t", MeData.serializer(), MeData())
        cache.clear()
        assertNull(cache.read("me/t", MeData.serializer()))
    }

    @Test
    fun aTokenKeyLongerThanTheFileSystemAllowsStillRoundTrips() {
        // a real portal token is a ~600-char JWT; a filename cannot carry it
        val dir = Files.createTempDirectory("kiosk-cache").toFile()
        val key = "att-meta/eyJhbGciOiJIUzI1NiJ9." + "x".repeat(600)

        Cache(dir).write(key, MeData.serializer(), MeData(bank = BankInfo(bank = "SBI")))

        // a later instance must find the same file, and its name must fit the cap
        assertEquals("SBI", Cache(dir).read(key, MeData.serializer())?.value?.bank?.bank)
        val name = File(dir, "portal-cache").listFiles()!!.single().name
        assertTrue("name fits the 255-byte file system cap", name.length <= 255)
    }

    @Test
    fun keysThatFitTheLimitKeepTheFileNameOldBuildsWrote() {
        // updates must find caches already on disk: names under the cap stay as they are
        val dir = Files.createTempDirectory("kiosk-cache").toFile()
        val key = "att/eyJhbGciOiJIUzI1NiJ9." + "x".repeat(200)

        Cache(dir).write(key, MeData.serializer(), MeData())

        val expected = "att_eyJhbGciOiJIUzI1NiJ9." + "x".repeat(200) + ".json"
        assertTrue(File(dir, "portal-cache").resolve(expected).exists())
    }
}
