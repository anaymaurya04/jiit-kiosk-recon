package com.jiitkiosk.app.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** a non-2xx answer, body included so the screen can say why */
class HttpError(val code: Int, val body: String) : IOException("HTTP $code")

/** a conditional GET's answer: [body] is null when the server said 304, the cache is still current */
data class Tagged(val body: String?, val etag: String?)

/**
 * The HTTP surface this app needs: JSON in, JSON out, plain HttpURLConnection,
 * nothing vendored. Timeouts are real, so a dead socket cannot hang a job.
 */
object HttpJson {

    fun get(url: String, bearer: String? = null, headers: Map<String, String> = emptyMap()): String =
        request("GET", url, null, bearer, headers)

    fun post(url: String, body: String, bearer: String? = null, headers: Map<String, String> = emptyMap()): String =
        request("POST", url, body, bearer, headers)

    /**
     * A GET that carries [etag] as `If-None-Match`. An unchanged resource comes
     * back with a null body instead of an error, and the server's validator
     * (when it offers one) rides along for the next visit.
     */
    fun getIfNoneMatch(url: String, etag: String?, bearer: String? = null, headers: Map<String, String> = emptyMap()): Tagged {
        val conn = open("GET", url, null, bearer, headers)
        if (etag != null) conn.setRequestProperty("If-None-Match", etag)
        try {
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED) return Tagged(null, etag)
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw HttpError(code, text)
            return Tagged(text, conn.getHeaderField("ETag"))
        } finally {
            conn.disconnect()
        }
    }

    private fun request(
        method: String,
        url: String,
        body: String?,
        bearer: String?,
        headers: Map<String, String>,
    ): String {
        val conn = open(method, url, body, bearer, headers)
        try {
            body?.let { conn.outputStream.use { os -> os.write(it.toByteArray()) } }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw HttpError(code, text)
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun open(
        method: String,
        url: String,
        body: String?,
        bearer: String?,
        headers: Map<String, String>,
    ): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 15_000
        readTimeout = 20_000
        setRequestProperty("Accept", "application/json")
        headers.forEach { (k, v) -> setRequestProperty(k, v) }
        if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
        if (body != null) {
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
    }
}
