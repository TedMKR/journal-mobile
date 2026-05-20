package com.journal.app

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.common.config.AppConfig
import com.journal.core.common.config.StoredTokens
import com.journal.core.common.config.TokenSession
import com.journal.core.common.config.TokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@HiltViewModel
class AppViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val tokenSession: TokenSession,
    private val appConfig: AppConfig
) : ViewModel() {

    sealed interface SessionState {
        /** Идёт проверка токенов */
        data object Checking : SessionState
        /** Токены есть — нужно подтверждение PIN/биометрии */
        data object RequireBiometric : SessionState
        /** Биометрия пройдена, сессия восстановлена */
        data class Authenticated(val role: String) : SessionState
        /** Токенов нет — нужен полный логин через Keycloak */
        data object Unauthenticated : SessionState
    }

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Checking)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    /** Токены, ожидающие подтверждения биометрией */
    private var pendingTokens: StoredTokens? = null

    init {
        viewModelScope.launch { checkSession() }
    }

    private suspend fun checkSession() = withContext(Dispatchers.IO) {
        val stored = tokenStore.load()
        if (stored == null) {
            Log.d(TAG, "No stored tokens — Keycloak login required")
            _sessionState.value = SessionState.Unauthenticated
            return@withContext
        }

        // Токен ещё действителен?
        val validTokens = if (stored.expiresAtMs > System.currentTimeMillis() + 30_000L) {
            Log.d(TAG, "Access token valid, requesting biometric confirmation")
            stored
        } else {
            // Пробуем тихое обновление через refresh_token
            val refreshToken = stored.refreshToken
            if (refreshToken.isNullOrBlank()) {
                Log.d(TAG, "Token expired, no refresh token — Keycloak login")
                tokenStore.clear()
                _sessionState.value = SessionState.Unauthenticated
                return@withContext
            }

            Log.d(TAG, "Token expired, attempting silent refresh...")
            val refreshed = tryRefresh(refreshToken, stored.role)
            if (refreshed == null) {
                Log.w(TAG, "Silent refresh failed — Keycloak login")
                tokenStore.clear()
                _sessionState.value = SessionState.Unauthenticated
                return@withContext
            }

            // Сохраняем обновлённые токены
            tokenStore.save(
                accessToken = refreshed.accessToken,
                idToken = refreshed.idToken,
                refreshToken = refreshed.refreshToken,
                expiresAtMs = refreshed.expiresAtMs,
                role = refreshed.role
            )
            Log.d(TAG, "Token refreshed, requesting biometric confirmation")
            refreshed
        }

        // Токены готовы — ждём подтверждения биометрией
        pendingTokens = validTokens
        _sessionState.value = SessionState.RequireBiometric
    }

    /** Вызвать после успешного прохождения биометрии */
    fun onBiometricSuccess() {
        val tokens = pendingTokens ?: run {
            _sessionState.value = SessionState.Unauthenticated
            return
        }
        tokenSession.setTokens(tokens.accessToken, tokens.idToken, tokens.refreshToken)
        _sessionState.value = SessionState.Authenticated(tokens.role)
        pendingTokens = null
    }

    /** Вызвать если пользователь отменил биометрию или она недоступна */
    fun onBiometricFailed() {
        pendingTokens = null
        _sessionState.value = SessionState.Unauthenticated
    }

    /** Очистить сессию при явном logout */
    fun clearSession() {
        tokenStore.clear()
        tokenSession.clear()
        pendingTokens = null
    }

    private fun tryRefresh(refreshToken: String, currentRole: String): StoredTokens? {
        return try {
            val tokenUrl = URL(
                "${appConfig.keycloakBaseUrl}/realms/${appConfig.keycloakRealm}" +
                    "/protocol/openid-connect/token"
            )
            val connection = (tokenUrl.openConnection() as HttpURLConnection).also { conn ->
                if (conn is HttpsURLConnection) {
                    conn.sslSocketFactory = trustAllSslContext.socketFactory
                    conn.hostnameVerifier = HostnameVerifier { _, _ -> true }
                }
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
            }

            val body = "grant_type=refresh_token" +
                "&client_id=${appConfig.keycloakClientId}" +
                "&refresh_token=$refreshToken"
            connection.outputStream.use { it.write(body.toByteArray()) }

            if (connection.responseCode != 200) {
                Log.w(TAG, "Refresh HTTP ${connection.responseCode}")
                return null
            }

            val json = JSONObject(connection.inputStream.bufferedReader().readText())
            val newAccess = json.optString("access_token").takeIf { it.isNotBlank() } ?: return null
            val newId = json.optString("id_token").takeIf { it.isNotBlank() }
            val newRefresh = json.optString("refresh_token").takeIf { it.isNotBlank() }
            val expiresIn = json.optLong("expires_in", 300L)
            val role = extractRole(newAccess, currentRole)

            StoredTokens(
                accessToken = newAccess,
                idToken = newId,
                refreshToken = newRefresh,
                expiresAtMs = System.currentTimeMillis() + expiresIn * 1000L,
                role = role
            )
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh exception", e)
            null
        }
    }

    private fun extractRole(accessToken: String, fallback: String): String {
        val payload = accessToken.split(".").getOrNull(1) ?: return fallback
        val decoded = runCatching {
            val bytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            String(bytes, Charsets.UTF_8)
        }.getOrNull() ?: return fallback

        val roles = runCatching {
            JSONObject(decoded)
                .optJSONObject("resource_access")
                ?.optJSONObject("journal-backend")
                ?.optJSONArray("roles")
        }.getOrNull()

        val supported = setOf("teacher", "student", "methodologist", "dean", "admin")
        for (i in 0 until (roles?.length() ?: 0)) {
            val role = roles?.optString(i).orEmpty()
            if (role in supported) return role
        }
        return fallback
    }

    companion object {
        private const val TAG = "AppViewModel"

        private val trustAllManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        private val trustAllSslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustAllManager), SecureRandom())
        }
    }
}
