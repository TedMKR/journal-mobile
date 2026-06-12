package com.journal.app

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.common.config.StoredTokens
import com.journal.core.common.config.TokenSession
import com.journal.core.common.config.TokenStore
import com.journal.core.data.repository.AuthRepository
import com.journal.core.data.repository.SessionRepository
import com.journal.core.data.util.NetworkError
import com.journal.core.database.entity.SessionEntity
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
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    sealed interface SessionState {
        data object Checking : SessionState
        data object RequireBiometric : SessionState
        data class Authenticated(val role: String) : SessionState
        data class OfflineAuthenticated(val role: String) : SessionState
        data object Unauthenticated : SessionState
    }

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Checking)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var pendingTokens: StoredTokens? = null
    private var pendingOfflineSession: SessionEntity? = null
    private var pendingOfflineTokens: StoredTokens? = null

    init {
        viewModelScope.launch { checkSession() }
    }

    private suspend fun checkSession() = withContext(Dispatchers.IO) {
        val stored = tokenStore.load()
        val cachedSession = sessionRepository.getSession()

        if (stored == null) {
            if (cachedSession != null && cachedSession.isOfflineAllowed()) {
                Log.d(TAG, "No stored tokens, using cached offline session")
                requestOfflineUnlock(cachedSession, null)
            } else {
                Log.d(TAG, "No stored tokens and no cached session")
                _sessionState.value = SessionState.Unauthenticated
            }
            return@withContext
        }

        val validTokens = if (stored.expiresAtMs > System.currentTimeMillis() + TOKEN_EXPIRY_SKEW_MS) {
            Log.d(TAG, "Access token valid, requesting biometric confirmation")
            stored
        } else {
            val refreshToken = stored.refreshToken
            if (refreshToken.isNullOrBlank()) {
                if (cachedSession != null && cachedSession.isOfflineAllowed()) {
                    Log.d(TAG, "Token expired without refresh token, using cached offline session")
                    requestOfflineUnlock(cachedSession, stored)
                } else {
                    Log.d(TAG, "Token expired without refresh token and no cached session")
                    tokenStore.clear()
                    _sessionState.value = SessionState.Unauthenticated
                }
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
            } catch (error: NetworkError) {
                when (error) {
                    is NetworkError.NetworkUnavailable,
                    is NetworkError.ServerError -> {
                        if (cachedSession != null && cachedSession.isOfflineAllowed()) {
                            Log.w(TAG, "Refresh unavailable, using cached offline session")
                            requestOfflineUnlock(cachedSession, stored)
                        } else {
                            Log.w(TAG, "Refresh unavailable and no cached session")
                            _sessionState.value = SessionState.Unauthenticated
                        }
                    }
                    is NetworkError.AuthError -> {
                        Log.w(TAG, "Refresh rejected by auth server")
                        clearStoredAuth()
                        _sessionState.value = SessionState.Unauthenticated
                    }
                    is NetworkError.ValidationError,
                    is NetworkError.ConflictError,
                    is NetworkError.Unknown -> {
                        Log.w(TAG, "Refresh failed: ${error.message}")
                        _sessionState.value = SessionState.Unauthenticated
                    }
                }
                return@withContext
            }
        }

        pendingTokens = validTokens
        pendingOfflineSession = null
        pendingOfflineTokens = null
        _sessionState.value = SessionState.RequireBiometric
    }

    fun onBiometricSuccess() {
        val tokens = pendingTokens
        if (tokens != null) {
            tokenSession.setTokens(tokens.accessToken, tokens.idToken, tokens.refreshToken)
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

        val offlineSession = pendingOfflineSession
        if (offlineSession != null) {
            pendingOfflineTokens?.let {
                tokenSession.setTokens(it.accessToken, it.idToken, it.refreshToken)
            }
            _sessionState.value = SessionState.OfflineAuthenticated(offlineSession.role)
            clearPendingAuth()
            return
        }

        _sessionState.value = SessionState.Unauthenticated
    }

    fun onBiometricFailed() {
        clearPendingAuth()
        _sessionState.value = SessionState.Unauthenticated
    }

    fun clearSession() {
        clearStoredAuth()
        viewModelScope.launch(Dispatchers.IO) {
            sessionRepository.clearAll()
        }
    }

    private fun requestOfflineUnlock(session: SessionEntity, tokens: StoredTokens?) {
        pendingTokens = null
        pendingOfflineSession = session
        pendingOfflineTokens = tokens
        _sessionState.value = SessionState.RequireBiometric
    }

    private fun SessionEntity.isOfflineAllowed(): Boolean {
        val now = System.currentTimeMillis()
        return offlineAllowedUntil == 0L || offlineAllowedUntil > now
    }

    private fun clearStoredAuth() {
        tokenStore.clear()
        tokenSession.clear()
        clearPendingAuth()
    }

    private fun clearPendingAuth() {
        pendingTokens = null
        pendingOfflineSession = null
        pendingOfflineTokens = null
    }

    companion object {
        private const val TAG = "AppViewModel"
        private const val TOKEN_EXPIRY_SKEW_MS = 30_000L
    }
}
