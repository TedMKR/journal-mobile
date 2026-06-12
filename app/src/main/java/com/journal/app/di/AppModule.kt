package com.journal.app.di

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.journal.app.BuildConfig
import com.journal.core.common.config.AppConfig
import com.journal.core.common.config.TokenSession
import com.journal.core.common.config.TokenStore
import com.journal.core.network.api.JournalApi
import com.journal.core.network.interceptor.BearerTokenInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
        keycloakClientId = BuildConfig.KEYCLOAK_CLIENT_ID
    )

    @Provides
    @Singleton
    fun provideTokenSession(): TokenSession = TokenSession()

    @Provides
    @Singleton
    fun provideTokenStore(@ApplicationContext context: Context): TokenStore {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val prefs = EncryptedSharedPreferences.create(
            context,
            "journal_auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return TokenStore(prefs)
    }

    @Provides
    @Singleton
    fun provideOkHttp(
        tokenSession: TokenSession
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        return OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(tokenSession))
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
