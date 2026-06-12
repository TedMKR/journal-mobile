package com.journal.core.common.config

import android.util.Base64
import org.json.JSONObject

object JwtUtils {

    fun decodePayload(token: String): JSONObject? = runCatching {
        val payload = token.split(".").getOrNull(1) ?: return null
        val bytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        JSONObject(String(bytes, Charsets.UTF_8))
    }.getOrNull()

    fun extractFullName(token: String): String? {
        val payload = decodePayload(token) ?: return null
        return jwtPersonName(payload).takeIf(String::isNotBlank)
    }

    fun extractFirstName(token: String): String? {
        val payload = decodePayload(token) ?: return null
        return PersonNameFormatter.firstNameFromFullName(jwtPersonName(payload))
    }

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

    private fun jwtPersonName(payload: JSONObject): String =
        PersonNameFormatter.format(
            PersonNameParts(
                fullName = payload.optString("name").takeIf(String::isNotBlank),
                firstName = payload.optString("given_name").takeIf(String::isNotBlank),
                lastName = payload.optString("family_name").takeIf(String::isNotBlank),
                middleName = payload.optString("middle_name").takeIf(String::isNotBlank)
                    ?: payload.optString("patronymic").takeIf(String::isNotBlank),
                username = payload.optString("preferred_username").takeIf(String::isNotBlank)
            )
        )
}
