package com.journal.app

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.common.config.RoleSession
import com.journal.core.common.config.StoredTokens
import com.journal.core.common.config.TokenSession
import com.journal.core.common.config.TokenStore
import com.journal.core.data.repository.AuthRepository
import com.journal.core.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val tokenSession: TokenSession,
    private val roleSession: RoleSession,
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    sealed interface SessionState {
        data object Checking : SessionState
        data object RequireBiometric : SessionState
        data class Authenticated(val role: String) : SessionState
        data object Unauthenticated : SessionState
    }

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Checking)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var pendingTokens: StoredTokens? = null

    init {
        viewModelScope.launch { checkSession() }
    }

    private suspend fun checkSession() = withContext(Dispatchers.IO) {
        val stored = tokenStore.load()

        if (stored == null) {
            Log.d(TAG, "No stored tokens - Keycloak login required")
            _sessionState.value = SessionState.Unauthenticated
            return@withContext
        }

        val validTokens = if (stored.expiresAtMs > System.currentTimeMillis() + TOKEN_EXPIRY_SKEW_MS) {
            Log.d(TAG, "Access token valid, requesting biometric confirmation")
            stored
        } else {
            val refreshToken = stored.refreshToken
            if (refreshToken.isNullOrBlank()) {
                Log.d(TAG, "Token expired without refresh token - Keycloak login required")
                clearStoredAuth()
                _sessionState.value = SessionState.Unauthenticated
                return@withContext
            }

            Log.d(TAG, "Token expired, attempting silent refresh")
            try {
                authRepository.refreshTokens(refreshToken, stored.role).also { refreshed ->
                    tokenStore.save(
                        accessToken = refreshed.accessToken,
                        idToken = refreshed.idToken,
                        refreshToken = refreshed.refreshToken,
                        expiresAtMs = refreshed.expiresAtMs,
                        role = refreshed.role
                    )
                    Log.d(TAG, "Token refreshed, requesting biometric confirmation")
                }
            } catch (error: Exception) {
                Log.w(TAG, "Silent refresh failed - Keycloak login required", error)
                clearStoredAuth()
                _sessionState.value = SessionState.Unauthenticated
                return@withContext
            }
        }

        pendingTokens = validTokens
        _sessionState.value = SessionState.RequireBiometric
    }

    fun onBiometricSuccess() {
        val tokens = pendingTokens
        if (tokens != null) {
            tokenSession.setTokens(tokens.accessToken, tokens.idToken, tokens.refreshToken)
            roleSession.setRole(tokens.role)
            _sessionState.value = SessionState.Authenticated(tokens.role)
            viewModelScope.launch(Dispatchers.IO) {
                val profile = authRepository.profileFromToken(tokens.accessToken)
                sessionRepository.saveSession(
                    role = tokens.role,
                    userId = profile.userId,
                    fullName = profile.fullName
                )
            }
            clearPendingAuth()
            return
        }

        _sessionState.value = SessionState.Unauthenticated
    }

    fun onBiometricFailed() {
        Log.d(TAG, "Biometric confirmation dismissed - keeping session locked")
        _sessionState.value = SessionState.RequireBiometric
    }

    fun clearSession() {
        clearStoredAuth()
        viewModelScope.launch(Dispatchers.IO) {
            sessionRepository.clearAll()
        }
    }

    private fun clearStoredAuth() {
        tokenStore.clear()
        tokenSession.clear()
        clearPendingAuth()
    }

    private fun clearPendingAuth() {
        pendingTokens = null
    }

    companion object {
        private const val TAG = "AppViewModel"
        private const val TOKEN_EXPIRY_SKEW_MS = 30_000L
    }
}
