package com.journal.core.network.interceptor

import com.journal.core.common.config.TokenSession
import okhttp3.Interceptor
import okhttp3.Response

class BearerTokenInterceptor(
    private val tokenSession: TokenSession,
    private val enabled: Boolean = true
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenSession.accessToken.value
        val request = if (enabled && !token.isNullOrBlank()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}
