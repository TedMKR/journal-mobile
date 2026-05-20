package com.journal.app.di

import com.journal.app.BuildConfig
import com.journal.core.common.config.AppConfig
import com.journal.core.common.config.RoleSession
import com.journal.core.common.config.TokenSession
import com.journal.core.network.api.JournalApi
import com.journal.core.network.interceptor.BearerTokenInterceptor
import com.journal.core.network.interceptor.DebugRoleInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Singleton
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppConfig(): AppConfig = AppConfig(
        apiBaseUrl = BuildConfig.API_BASE_URL,
        openApiUrl = BuildConfig.OPENAPI_URL,
        keycloakBaseUrl = BuildConfig.KEYCLOAK_BASE_URL,
        keycloakRealm = BuildConfig.KEYCLOAK_REALM,
        keycloakClientId = BuildConfig.KEYCLOAK_CLIENT_ID,
        useDebugRole = BuildConfig.USE_DEBUG_ROLE,
        debugRole = BuildConfig.DEBUG_ROLE
    )

    @Provides
    @Singleton
    fun provideRoleSession(config: AppConfig): RoleSession = RoleSession(config.debugRole)

    @Provides
    @Singleton
    fun provideTokenSession(): TokenSession = TokenSession()

    @Provides
    @Singleton
    fun provideOkHttp(
        config: AppConfig,
        roleSession: RoleSession,
        tokenSession: TokenSession
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val trustAllManager = TrustAllManager()
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustAllManager), SecureRandom())
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllManager)
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .addInterceptor(DebugRoleInterceptor(config.useDebugRole) { roleSession.role.value })
            .addInterceptor(BearerTokenInterceptor(tokenSession, enabled = !config.useDebugRole))
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json, config: AppConfig): Retrofit {
        return Retrofit.Builder()
            .baseUrl(config.apiBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideJournalApi(retrofit: Retrofit): JournalApi = retrofit.create(JournalApi::class.java)
}

private class TrustAllManager : X509TrustManager {
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
