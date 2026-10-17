package `in`.codelif.ktjiit.auth

import android.util.Log
import `in`.codelif.ktjiit.http.PortalException
import `in`.codelif.ktjiit.http.Transport
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale

@Serializable
public data class PortalConfig(
    @SerialName("loadGoogleClientId") val googleClientId: String = "",
    @SerialName("loadGoogleButtonShow") val googleButtonShow: String = "",
    /** full url of the google token exchange, lives on a different port and api */
    @SerialName("URLapiBase") val loginUrl: String = DEFAULT_LOGIN_URL,
) {
    public val googleEnabled: Boolean get() = googleButtonShow == "Y" && googleClientId.isNotEmpty()

    public companion object {
        public const val DEFAULT_LOGIN_URL: String =
            "https://webportal.jiit.ac.in:6013/CLXSSOAPI/token/generate-token-google-signin"
    }
}

public class Auth(private val transport: Transport) {

    public suspend fun config(): PortalConfig =
        Transport.json.decodeFromJsonElement(transport.get("/token/getlogoClientWise", null))

    /** trades a google id token (the gsi `credential`) for a portal session */
    public suspend fun exchangeGoogleToken(credential: String, config: PortalConfig? = null): Session {
        val url = (config ?: runCatching { config() }.getOrNull())?.loginUrl ?: PortalConfig.DEFAULT_LOGIN_URL
        val payload = buildJsonObject {
            put("googleToken", credential)
            put("modulename", "studentportal")
        }
        return sessionFrom(transport.post(url, null, payload, encrypt = false))
    }

    /** builds a session from the login `response` object, e.g. one sniffed out of the portal page */
    public fun sessionFrom(response: JsonElement): Session {
        val res = Transport.json.decodeFromJsonElement<LoginResponse>(response)
        if (res.token.isEmpty() || res.value.isEmpty()) throw PortalException.Malformed("login response without token")
        val now = transport.clock.now()
        return Session(
            token = res.token,
            username = res.username,
            instituteId = res.value,
            instituteName = res.label,
            clientId = res.clientid,
            memberId = res.memberid,
            userId = res.userid,
            enrollmentNo = res.enrollmentno,
            name = res.name,
            memberType = res.membertype,
            tokenDate = jsDateString(now),
            obtainedAt = now.toEpochMilli(),
        )
    }

    /**
     * what the portal does on a 401: ask to extend the session, then retry with the
     * same token. returns true if the server said Success.
     *
     * shortcut: three tokendate candidates until a live log pins the one the server
     * wants; first Success wins, and the log names it
     */
    public suspend fun refresh(session: Session): Boolean {
        val candidates = LinkedHashMap<String, String>()
        fun offer(label: String, value: String) {
            if (value.isNotEmpty() && !candidates.containsValue(value)) candidates[label] = value
        }
        offer("stored", session.tokenDate)
        jwtIssuedAt(session.token)?.let { offer("iat", jsDateString(it)) }
        offer("now", jsDateString(transport.clock.now()))

        for ((label, date) in candidates) {
            val msg = ask(session, date)
            if (msg == "Success") {
                Log.i(TAG, "refresh ok via '$label'")
                return true
            }
            Log.i(TAG, "refresh['$label'] -> ${msg ?: "no answer"}")
        }
        return false
    }

    private suspend fun ask(session: Session, tokendate: String): String? {
        val payload = buildJsonObject {
            put("username", session.username)
            put("tokendate", tokendate)
        }
        return try {
            val res = transport.post("/token/refreshTokenRequest", session.token, payload, encrypt = false)
            runCatching { res.jsonObject["msg"]?.jsonPrimitive?.contentOrNull }.getOrNull()
        } catch (e: PortalException.SessionExpired) {
            Log.i(TAG, "refresh ask: http 401")
            null
        } catch (e: PortalException.PortalError) {
            // the server's own verdict, never the payload
            Log.i(TAG, "refresh ask: http ${e.code}: ${e.errors.joinToString("; ").take(120)}")
            null
        }
    }

    /** the jwt's own issue moment, in server time */
    private fun jwtIssuedAt(token: String): Instant? = runCatching {
        val claims = Transport.json.parseToJsonElement(String(Base64.getUrlDecoder().decode(token.split('.')[1])))
        Instant.ofEpochSecond(claims.jsonObject["iat"]!!.jsonPrimitive.long)
    }.getOrNull()

    internal companion object {
        private const val TAG = "kiosk-auth"
        private val JS_DATE = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT+0530 (India Standard Time)'", Locale.US)

        fun jsDateString(at: Instant): String = JS_DATE.format(at.atOffset(`in`.codelif.ktjiit.crypto.IST))
    }
}
