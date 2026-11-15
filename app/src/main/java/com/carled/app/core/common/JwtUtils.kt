package com.carled.app.core.common

import android.util.Base64
import com.google.gson.Gson

/**
 * The server's auth responses carry no user object at all — only
 * {accessToken, refreshToken, expiresIn, tokenType}. id/email/role are only
 * present as claims inside the JWT itself, so this decodes the payload
 * segment to recover them for the local [com.carled.app.domain.model.User].
 *
 * This does NOT verify the token's signature. That's fine here: the client
 * has no key to verify with, and the server already validated/issued the
 * token — we're only reading it back for display, not trusting it for
 * authorization (every real request still just sends the raw token and lets
 * the server re-validate it).
 */
data class JwtClaims(
    val sub: String? = null,
    val email: String? = null,
    val roles: List<String>? = null,
    val iat: Long? = null,
    val exp: Long? = null
) {
    /** "ROLE_CUSTOMER" -> "CUSTOMER", to match the plain role strings used
     * elsewhere (e.g. RegisterData.accountType). Empty string if no role claim. */
    fun primaryRole(): String =
        roles?.firstOrNull()?.removePrefix("ROLE_") ?: ""
}

object JwtUtils {
    private val gson = Gson()

    /** Returns null if [token] isn't a well-formed JWT or its payload isn't valid JSON. */
    fun decodeClaims(token: String): JwtClaims? {
        val parts = token.split(".")
        if (parts.size < 2) return null

        return try {
            val json = String(decodeBase64Url(parts[1]), Charsets.UTF_8)
            gson.fromJson(json, JwtClaims::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeBase64Url(segment: String): ByteArray {
        // JWT uses base64url with the padding stripped; Base64.decode needs
        // it restored (to a multiple of 4) before it'll accept the string.
        val normalized = segment.replace('-', '+').replace('_', '/')
        val padded = when (normalized.length % 4) {
            2 -> "$normalized=="
            3 -> "$normalized="
            else -> normalized
        }
        return Base64.decode(padded, Base64.DEFAULT)
    }
}