package com.journal.core.data.repository

import com.journal.core.common.config.AppConfig
import com.journal.core.common.config.JwtUtils
import com.journal.core.common.config.StoredTokens
import com.journal.core.data.util.NetworkError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class TokenProfile(
    val userId: String?,
    val fullName: String?
)

@Singleton
class AuthRepository @Inject constructor(
    private val appConfig: AppConfig
) {

    suspend fun refreshTokens(refreshToken: String, currentRole: String): StoredTokens =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val tokenUrl = URL(
                    "${appConfig.keycloakBaseUrl}/realms/${appConfig.keycloakRealm}" +
                        "/protocol/openid-connect/token"
                )
                connection = (tokenUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }

                val body = formBody(
                    "grant_type" to "refresh_token",
                    "client_id" to appConfig.keycloakClientId,
                    "refresh_token" to refreshToken
                )
                connection.outputStream.use { it.write(body.toByteArray()) }

                val responseCode = connection.responseCode
                val responseBody = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }

                if (responseCode !in 200..299) {
                    throw NetworkError.fromHttp(
                        code = responseCode,
                        body = responseBody,
                        authEndpoint = true
                    )
                }

                val payload = JSONObject(responseBody)
                val newAccess = payload.optString("access_token").takeIf { it.isNotBlank() }
                    ?: throw NetworkError.AuthError(responseCode, responseBody)
                val newId = payload.optString("id_token").takeIf { it.isNotBlank() }
                val newRefresh = payload.optString("refresh_token").takeIf { it.isNotBlank() }
                val expiresIn = payload.optLong("expires_in", 300L)
                val role = extractRole(newAccess, currentRole)

                StoredTokens(
                    accessToken = newAccess,
                    idToken = newId,
                    refreshToken = newRefresh,
                    expiresAtMs = System.currentTimeMillis() + expiresIn * 1000L,
                    role = role
                )
            } catch (e: NetworkError) {
                throw e
            } catch (e: IOException) {
                throw NetworkError.NetworkUnavailable(e)
            } catch (e: Exception) {
                throw NetworkError.Unknown(e)
            } finally {
                connection?.disconnect()
            }
        }

    fun profileFromToken(accessToken: String): TokenProfile {
        val payload = JwtUtils.decodePayload(accessToken)
        return TokenProfile(
            userId = payload?.optString("sub")?.takeIf { it.isNotBlank() },
            fullName = JwtUtils.extractFullName(accessToken)
        )
    }

    fun extractRole(accessToken: String, fallback: String = "teacher"): String {
        val supported = setOf("teacher", "student", "methodologist", "dean", "admin")
        return JwtUtils.extractRoles(accessToken, "journal-backend")
            .firstOrNull { it in supported }
            ?: fallback
    }

    private fun formBody(vararg pairs: Pair<String, String>): String =
        pairs.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }

    private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
}
