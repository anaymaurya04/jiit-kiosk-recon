package com.jiitkiosk.app.secure

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecretsTest {

    @Test
    fun roundTripsThroughTheKeystore() {
        val plain = "portal token".toByteArray()
        assertArrayEquals(plain, Secrets.open(Secrets.seal(plain)))
    }

    @Test
    fun everySealGetsItsOwnIv() {
        val plain = "same plaintext".toByteArray()
        assertFalse(Secrets.seal(plain).contentEquals(Secrets.seal(plain)))
    }

    @Test
    fun aTamperedBlobDoesNotOpen() {
        val sealed = Secrets.seal("token".toByteArray())
        sealed[sealed.size - 1] = (sealed[sealed.size - 1].toInt() xor 1).toByte()
        assertNull(Secrets.open(sealed))
    }

    @Test
    fun garbageDoesNotOpen() {
        assertNull(Secrets.open(byteArrayOf(1, 2, 3)))
    }
}
