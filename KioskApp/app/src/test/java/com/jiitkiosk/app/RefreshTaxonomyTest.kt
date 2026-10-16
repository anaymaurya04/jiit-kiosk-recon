package com.jiitkiosk.app

import `in`.codelif.ktjiit.auth.Auth
import `in`.codelif.ktjiit.auth.Session
import `in`.codelif.ktjiit.http.PortalException
import `in`.codelif.ktjiit.http.Transport
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * The session may only end on an explicit rejection. Anything else (timeout,
 * 5xx, empty answer) must surface as an error so the caller keeps the session
 * and shows the cached copy with a sync-failed banner.
 */
class RefreshTaxonomyTest {

    private class FakeTransport(private val answer: suspend () -> JsonElement) : Transport() {
        override suspend fun post(path: String, token: String?, payload: JsonObject?, encrypt: Boolean, retry: Boolean): JsonElement = answer()
    }

    private val session = Session(token = "t", username = "u", instituteId = "1")

    @Test
    fun `rejection ends the session`() = runBlocking {
        assertFalse(Auth(FakeTransport { throw PortalException.SessionExpired() }).refresh(session))
        assertFalse(Auth(FakeTransport { throw PortalException.PortalError(listOf("no"), 400) }).refresh(session))
    }

    @Test
    fun `network trouble keeps the session`() = runBlocking {
        val thrown = runCatching {
            Auth(FakeTransport { throw PortalException.Network(IOException("airplane mode")) }).refresh(session)
        }.exceptionOrNull()
        assertTrue("network error must surface, not read as a rejection: $thrown", thrown is PortalException.Network)
    }

    @Test
    fun `a 5xx keeps the session`() = runBlocking {
        val thrown = runCatching {
            Auth(FakeTransport { throw PortalException.ServerUnavailable(503, "no body") }).refresh(session)
        }.exceptionOrNull()
        assertTrue(thrown is PortalException.ServerUnavailable)
    }

    @Test
    fun `only an explicit success extends`() = runBlocking {
        assertTrue(Auth(FakeTransport { buildJsonObject { put("msg", "Success") } }).refresh(session))
        assertFalse(Auth(FakeTransport { buildJsonObject { } }).refresh(session))
    }
}
