package com.journal.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.journal.app.navigation.JournalNavHost
import com.journal.app.ui.theme.JournalTheme
import com.journal.core.common.config.AppConfig
import com.journal.core.common.config.TokenSession
import com.journal.core.network.api.JournalApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var journalApi: JournalApi

    @Inject
    lateinit var appConfig: AppConfig

    @Inject
    lateinit var tokenSession: TokenSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JournalTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    val appViewModel: AppViewModel = hiltViewModel()
                    val sessionState by appViewModel.sessionState.collectAsState()

                    // Показываем биометрический диалог когда токены найдены
                    LaunchedEffect(sessionState) {
                        if (sessionState is AppViewModel.SessionState.RequireBiometric) {
                            showBiometricPrompt(
                                onSuccess = { appViewModel.onBiometricSuccess() },
                                onFailed = { appViewModel.onBiometricFailed() }
                            )
                        }
                    }

                    when (val state = sessionState) {
                        is AppViewModel.SessionState.Checking,
                        is AppViewModel.SessionState.RequireBiometric -> {
                            // Пока идёт проверка или ожидание биометрии — спиннер
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is AppViewModel.SessionState.Authenticated -> {
                            JournalNavHost(
                                journalApi = journalApi,
                                appConfig = appConfig,
                                tokenSession = tokenSession,
                                initialRole = state.role,
                                onClearSession = { appViewModel.clearSession() }
                            )
                        }

                        is AppViewModel.SessionState.Unauthenticated -> {
                            JournalNavHost(
                                journalApi = journalApi,
                                appConfig = appConfig,
                                tokenSession = tokenSession,
                                initialRole = null,
                                onClearSession = { appViewModel.clearSession() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt(
        onSuccess: () -> Unit,
        onFailed: () -> Unit
    ) {
        val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

        // Проверяем доступность биометрии / PIN
        val canAuthenticate = BiometricManager.from(this)
            .canAuthenticate(authenticators)

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            // На устройстве не настроена блокировка экрана — входим без подтверждения
            onSuccess()
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Подтверждение входа")
            .setSubtitle("Используйте отпечаток пальца или PIN")
            .setAllowedAuthenticators(authenticators)
            .build()

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Пользователь отменил или слишком много попыток
                    onFailed()
                }

                override fun onAuthenticationFailed() {
                    // Неверный отпечаток — ждём ещё (BiometricPrompt сам показывает ошибку)
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}
