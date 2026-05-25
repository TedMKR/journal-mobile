package com.journal.core.common.config

import android.util.Base64
import org.json.JSONObject

/**
 * Utility helpers for decoding JWT payloads.
 *
 * Only the *payload* segment is decoded — no signature verification is performed
 * (the server has already validated the token; we just need to read claims).
 */
object JwtUtils {

    /**
     * Decode the JWT payload and return the JSONObject, or null on any error.
     */
    fun decodePayload(token: String): JSONObject? = runCatching {
        val payload = token.split(".").getOrNull(1) ?: return null
        val bytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        JSONObject(String(bytes, Charsets.UTF_8))
    }.getOrNull()

    /**
     * Extract the display name from the JWT.
     *
     * Priority:
     * 1. `name` — Keycloak "Full Name" attribute (most reliable)
     * 2. `given_name` + `family_name` — concatenated if both present
     * 3. `preferred_username` — login / username as last resort
     */
    fun extractFullName(token: String): String? {
        val payload = decodePayload(token) ?: return null
        val name = payload.optString("name").takeIf(String::isNotBlank)
        if (name != null) return name

        val given = payload.optString("given_name").takeIf(String::isNotBlank)
        val family = payload.optString("family_name").takeIf(String::isNotBlank)
        if (given != null && family != null) return "$family $given"
        if (family != null) return family
        if (given != null) return given

        return payload.optString("preferred_username").takeIf(String::isNotBlank)
    }

    /**
     * Extract only the first name (имя) from the JWT.
     *
     * Priority:
     * 1. `given_name` Keycloak claim — the dedicated first-name field
     * 2. 2nd word of `name` — Russian full names are "Фамилия Имя Отчество",
     *    so index 1 is the first name
     * 3. Full `name` claim as last resort (better than nothing)
     */
    fun extractFirstName(token: String): String? {
        val payload = decodePayload(token) ?: return null

        // Keycloak dedicated first-name claim
        val given = payload.optString("given_name").takeIf(String::isNotBlank)
        if (given != null) return given

        // Derive from full name: "Фамилия Имя Отчество" → index 1 = first name
        val fullName = payload.optString("name").takeIf(String::isNotBlank)
        if (fullName != null) {
            val firstName = fullName.trim().split("\\s+".toRegex()).getOrNull(1)?.takeIf(String::isNotBlank)
            if (firstName != null) return firstName
            return fullName // only one word — use it as-is
        }

        return payload.optString("preferred_username").takeIf(String::isNotBlank)
    }

    /**
     * Extract roles from the Keycloak `resource_access.<clientId>.roles` claim.
     */
    fun extractRoles(token: String, clientId: String): List<String> {
        val payload = decodePayload(token) ?: return emptyList()
        return runCatching {
            payload
                .optJSONObject("resource_access")
                ?.optJSONObject(clientId)
                ?.optJSONArray("roles")
                ?.let { arr -> List(arr.length()) { arr.getString(it) } }
                ?: emptyList()
        }.getOrDefault(emptyList())
    }
}
