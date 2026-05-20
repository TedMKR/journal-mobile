package com.journal.core.common.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenSession {
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    private val _idToken = MutableStateFlow<String?>(null)
    val idToken: StateFlow<String?> = _idToken.asStateFlow()

    private val _refreshToken = MutableStateFlow<String?>(null)
    val refreshToken: StateFlow<String?> = _refreshToken.asStateFlow()

    fun setTokens(
        accessToken: String,
        idToken: String?,
        refreshToken: String?
    ) {
        _accessToken.value = accessToken
        _idToken.value = idToken
        _refreshToken.value = refreshToken
    }

    fun clear() {
        _accessToken.value = null
        _idToken.value = null
        _refreshToken.value = null
    }
}
