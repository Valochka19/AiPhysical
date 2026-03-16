package com.example.aiphysical.data.service

import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

/**
 * Provides OAuth2 access tokens for a Google service account using JWT Bearer flow.
 *
 * No external dependencies required — uses only Android's built-in crypto APIs.
 *
 * Usage:
 *   1. Put your service-account JSON file at:
 *      composeApp/src/androidMain/assets/service-account.json
 *   2. Create an instance once and keep it alive (token is cached until ~1 min before expiry).
 */
class ServiceAccountTokenProvider(private val saJsonString: String) {

    private data class CachedToken(val value: String, val expiresAtMs: Long)

    @Volatile
    private var cache: CachedToken? = null

    /** Returns a valid access token, refreshing automatically when needed. */
    @Throws(Exception::class)
    fun getToken(): String {
        val c = cache
        if (c != null && System.currentTimeMillis() < c.expiresAtMs - 60_000L) {
            return c.value
        }
        return fetchNewToken()
    }

    // ── JWT creation + token exchange ─────────────────────────────────────────

    private fun fetchNewToken(): String {
        val saObj       = JSONObject(saJsonString)
        val privateKeyPem = saObj.getString("private_key")
        val clientEmail   = saObj.getString("client_email")

        val nowSec = System.currentTimeMillis() / 1000L
        val expSec = nowSec + 3600L

        // --- build JWT ---
        val headerJson = """{"alg":"RS256","typ":"JWT"}"""
        val claimsJson = JSONObject()
            .put("iss",   clientEmail)
            .put("sub",   clientEmail)
            .put("scope", "https://www.googleapis.com/auth/cloud-platform")
            .put("aud",   "https://oauth2.googleapis.com/token")
            .put("iat",   nowSec)
            .put("exp",   expSec)
            .toString()

        val header = base64url(headerJson.toByteArray(Charsets.UTF_8))
        val claims = base64url(claimsJson.toByteArray(Charsets.UTF_8))
        val signingInput = "$header.$claims"

        val privateKey = parsePemPrivateKey(privateKeyPem)
        val sigBytes = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(signingInput.toByteArray(Charsets.UTF_8))
        }.sign()

        val jwt = "$signingInput.${base64url(sigBytes)}"

        // --- exchange JWT for access token ---
        val conn = (URL("https://oauth2.googleapis.com/token").openConnection()
                as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connectTimeout = 15_000
            readTimeout    = 15_000
            doOutput       = true
        }

        val grantType = "urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer"
        conn.outputStream.use { it.write("grant_type=$grantType&assertion=$jwt".toByteArray()) }

        val code = conn.responseCode
        val body = if (code == 200) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } else {
            val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText() ?: "unknown"
            throw Exception("Service account token exchange failed ($code): $err")
        }

        val tokenJson  = JSONObject(body)
        val accessToken = tokenJson.getString("access_token")
        val expiresIn   = tokenJson.optLong("expires_in", 3600L)

        cache = CachedToken(
            value       = accessToken,
            expiresAtMs = System.currentTimeMillis() + expiresIn * 1_000L
        )
        return accessToken
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parsePemPrivateKey(pem: String): PrivateKey {
        val stripped = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\n", "\n")
            .replace("\n", "")
            .trim()
        val keyBytes = Base64.decode(stripped, Base64.DEFAULT)
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(keyBytes))
    }

    private fun base64url(data: ByteArray): String =
        Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}

