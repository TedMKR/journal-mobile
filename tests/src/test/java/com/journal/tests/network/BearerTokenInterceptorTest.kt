package com.journal.tests.network

import com.journal.core.common.config.TokenSession
import com.journal.core.network.interceptor.BearerTokenInterceptor
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BearerTokenInterceptorTest {

    @Test
    fun `request is unchanged when access token is missing`() {
        val captured = executeCapturedRequest(TokenSession())

        assertNull(captured.header("Authorization"))
    }

    @Test
    fun `request receives bearer authorization when access token exists`() {
        val tokenSession = TokenSession().apply {
            setTokens(accessToken = "access-token", idToken = null, refreshToken = null)
        }

        val captured = executeCapturedRequest(tokenSession)

        assertEquals("Bearer access-token", captured.header("Authorization"))
    }

    private fun executeCapturedRequest(tokenSession: TokenSession): Request {
        val terminal = CapturingTerminalInterceptor()
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(tokenSession))
            .addInterceptor(terminal)
            .build()

        client.newCall(
            Request.Builder()
                .url("https://journal.test/api")
                .build()
        ).execute().close()

        return terminal.request
    }

    private class CapturingTerminalInterceptor : Interceptor {
        lateinit var request: Request

        override fun intercept(chain: Interceptor.Chain): Response {
            request = chain.request()
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }
    }
}
