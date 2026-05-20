package com.journal.core.common.config

import android.content.SharedPreferences

/**
 * Persistent storage for auth tokens. Use EncryptedSharedPreferences in production.
 * The SharedPreferences instance is provided via DI from the app module.
 */
class TokenStore(private val prefs: SharedPreferences) {

    fun save(
        accessToken: String,
        idToken: String?,
        refreshToken: String?,
        expiresAtMs: Long,
        role: String
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_ID_TOKEN, idToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT, expiresAtMs)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun load(): StoredTokens? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        return StoredTokens(
            accessToken = accessToken,
            idToken = prefs.getString(KEY_ID_TOKEN, null),
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null),
            expiresAtMs = prefs.getLong(KEY_EXPIRES_AT, 0L),
            role = prefs.getString(KEY_ROLE, "teacher") ?: "teacher"
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_ID_TOKEN = "id_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_ROLE = "role"
    }
}

data class StoredTokens(
    val accessToken: String,
    val idToken: String?,
    val refreshToken: String?,
    val expiresAtMs: Long,
    val role: String
)
