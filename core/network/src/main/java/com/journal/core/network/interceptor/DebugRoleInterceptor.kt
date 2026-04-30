package com.journal.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class DebugRoleInterceptor(
    private val useDebugRole: Boolean,
    private val roleProvider: () -> String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!useDebugRole) return chain.proceed(request)

        val updated = request.newBuilder()
            .header("X-Debug-Role", roleProvider())
            .build()
        return chain.proceed(updated)
    }
}
