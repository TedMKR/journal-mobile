package com.journal.features.auth

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.common.config.AppConfig
import com.journal.core.common.config.RoleSession
import com.journal.core.common.config.TokenSession
import com.journal.core.common.config.TokenStore
import com.journal.core.data.repository.AuthRepository
import com.journal.core.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.connectivity.ConnectionBuilder
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import java.net.HttpURLConnection
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@HiltViewModel
class AuthViewModel @Inject constructor(
    application: Application,
    private val appConfig: AppConfig,
    private val roleSession: RoleSession,
    private val tokenSession: TokenSession,
    private val tokenStore: TokenStore,
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) : AndroidViewModel(application) {

    private val authService = AuthorizationService(
        application,
        AppAuthConfiguration.Builder()
            .setConnectionBuilder(KeycloakDevConnectionBuilder(appConfig.keycloakBaseUrl))
            .build()
    )
    private val redirectUri = Uri.parse("com.university.journal:/oauth2redirect")

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun buildAuthIntent(): Intent {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse("${appConfig.keycloakBaseUrl}/realms/${appConfig.keycloakRealm}/protocol/openid-connect/auth"),
            Uri.parse("${appConfig.keycloakBaseUrl}/realms/${appConfig.keycloakRealm}/protocol/openid-connect/token")
        )

        val request = AuthorizationRequest.Builder(
            serviceConfig,
            appConfig.keycloakClientId,
            ResponseTypeValues.CODE,
            redirectUri
        )
            .setScope("openid profile email")
            .build()

        return authService.getAuthorizationRequestIntent(request)
    }

    fun handleAuthResult(data: Intent?, onSuccess: (String) -> Unit) {
        val response = data?.let(AuthorizationResponse::fromIntent)
        val exception = data?.let(AuthorizationException::fromIntent)

        if (exception != null) {
            Log.e(TAG, "Keycloak authorization failed", exception)
            _state.value = AuthUiState.Error(exception.toDisplayMessage())
            return
        }

        if (response == null) {
            _state.value = AuthUiState.Error("Keycloak не вернул код авторизации")
            return
        }

        _state.value = AuthUiState.Loading
        authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenException ->
            if (tokenException != null) {
                Log.e(TAG, "Keycloak token exchange failed", tokenException)
                _state.value = AuthUiState.Error(tokenException.toDisplayMessage())
                return@performTokenRequest
            }

            val accessToken = tokenResponse?.accessToken
            if (accessToken.isNullOrBlank()) {
                _state.value = AuthUiState.Error("В ответе Keycloak нет access token")
                return@performTokenRequest
            }

            viewModelScope.launch(Dispatchers.Default) {
                val role = authRepository.extractRole(accessToken)
                val profile = authRepository.profileFromToken(accessToken)
                val expiresAtMs = tokenResponse.accessTokenExpirationTime
                    ?: (System.currentTimeMillis() + 300_000L)
                tokenSession.setTokens(
                    accessToken = accessToken,
                    idToken = tokenResponse.idToken,
                    refreshToken = tokenResponse.refreshToken
                )
                tokenStore.save(
                    accessToken = accessToken,
                    idToken = tokenResponse.idToken,
                    refreshToken = tokenResponse.refreshToken,
                    expiresAtMs = expiresAtMs,
                    role = role
                )
                sessionRepository.saveSession(
                    role = role,
                    userId = profile.userId,
                    fullName = profile.fullName
                )
                roleSession.setRole(role)
                _state.value = AuthUiState.Authenticated(role)
                launch(Dispatchers.Main) {
                    onSuccess(role)
                }
            }
        }
    }

    override fun onCleared() {
        authService.dispose()
        super.onCleared()
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}

private class KeycloakDevConnectionBuilder(keycloakBaseUrl: String) : ConnectionBuilder {
    private val keycloakHost = Uri.parse(keycloakBaseUrl).host.orEmpty()
    private val trustAllSocketFactory by lazy {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(TRUST_ALL_MANAGER), SecureRandom())
        sslContext.socketFactory
    }

    override fun openConnection(uri: Uri): HttpURLConnection {
        val connection = DefaultConnectionBuilder.INSTANCE.openConnection(uri)
        if (uri.host == keycloakHost && connection is HttpsURLConnection) {
            connection.sslSocketFactory = trustAllSocketFactory
            connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
        }
        return connection
    }

    private companion object {
        private val TRUST_ALL_MANAGER = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
    }
}

private fun AuthorizationException.toDisplayMessage(): String {
    val rootCause = cause?.localizedMessage.orEmpty()
    val details = listOfNotNull(
        errorDescription,
        rootCause.takeIf { it.isNotBlank() }
    ).joinToString(separator = ": ")

    return when {
        details.isNotBlank() -> details
        code == AuthorizationException.GeneralErrors.NETWORK_ERROR.code -> "Network error: проверьте доступ к Keycloak"
        else -> "Ошибка авторизации Keycloak"
    }
}

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(val role: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
