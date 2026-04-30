package com.journal.app.di

import com.journal.app.BuildConfig
import com.journal.core.common.config.AppConfig
import com.journal.core.network.api.JournalApi
import com.journal.core.common.config.RoleSession
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
import javax.inject.Singleton

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
    fun provideOkHttp(config: AppConfig, roleSession: RoleSession): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        return OkHttpClient.Builder()
            .addInterceptor(DebugRoleInterceptor(config.useDebugRole) { roleSession.role.value })
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
