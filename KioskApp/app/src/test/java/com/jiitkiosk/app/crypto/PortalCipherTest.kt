package com.jiitkiosk.app.crypto

import `in`.codelif.ktjiit.crypto.PortalCipher
import `in`.codelif.ktjiit.crypto.dateSeq
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.Instant

/**
 * Pins the SPEC §9 vectors and the midnight rollover against the vendored
 * cipher that actually runs on device. The SPEC's body vector travels the same
 * decrypt path as the LocalName vector asserted here.
 */
class PortalCipherTest {

    // the capture day: 2026-10-15, 12:00 IST, weekday=4
    private val capture = Instant.parse("2026-10-15T06:30:00Z")

    @Test
    fun theKeyDerivesFromTheCaptureDate() {
        assertEquals("qa8y1124506ty1pn", "qa8y" + dateSeq(capture) + "ty1pn")
    }

    @Test
    fun theSpecLocalNameVectorDecrypts() {
        assertEquals(
            "VZnv1124506eD230",
            PortalCipher.decrypt("QKM0Nb0fYVJBmR+qDGy+PMOJ731xjUxEE/DXq+W1wyk=", capture),
        )
    }

    @Test
    fun aLocalNameIsAlwaysA32ByteSecret() {
        val name = PortalCipher.localName(capture)
        assertEquals(44, name.length) // 32 bytes -> base64, always
        val plain = PortalCipher.decrypt(name, capture)
        assertEquals(16, plain.length) // 4 noise + 7 date + 5 noise
        assertEquals("1124506", plain.substring(4, 11))
    }

    @Test
    fun theKeyRotatesAtIstMidnight() {
        val before = Instant.parse("2026-10-15T18:29:59Z") // 23:59:59 IST
        val after = Instant.parse("2026-10-15T18:30:01Z") // 00:00:01 IST next day
        assertEquals("1124506", dateSeq(before))
        assertEquals("1125606", dateSeq(after))
        val sealed = PortalCipher.encrypt("{\"a\":1}", before)
        val across = runCatching { PortalCipher.decrypt(sealed, after) }.getOrNull()
        assertNotEquals("{\"a\":1}", across)
    }

    @Test
    fun encryptionRoundTripsWithinTheDay() {
        assertEquals("{\"userid\":\"x\"}", PortalCipher.decrypt(PortalCipher.encrypt("{\"userid\":\"x\"}", capture), capture))
    }
}
