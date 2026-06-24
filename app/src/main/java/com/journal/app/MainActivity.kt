package com.journal.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.journal.app.navigation.JournalNavHost
import com.journal.app.ui.theme.JournalTheme
import com.journal.core.common.config.AppConfig
import com.journal.core.common.config.TokenSession
import com.journal.core.data.notification.NotificationSettingsRepository
import com.journal.core.data.notification.StudentGradeNotificationWorker
import com.journal.core.data.sync.SyncWorker
import com.journal.core.data.theme.AppearanceSettingsRepository
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

    @Inject
    lateinit var notificationSettingsRepository: NotificationSettingsRepository

    @Inject
    lateinit var appearanceSettingsRepository: AppearanceSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SyncWorker.enqueue(this)

        setContent {
            val darkThemeEnabled by appearanceSettingsRepository
                .darkThemeEnabled
                .collectAsState()

            JournalTheme(darkTheme = darkThemeEnabled) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val appViewModel: AppViewModel = hiltViewModel()
                    val sessionState by appViewModel.sessionState.collectAsState()
                    val gradeNotificationsEnabled by notificationSettingsRepository
                        .gradeNotificationsEnabled
                        .collectAsState()

                    LaunchedEffect(sessionState) {
                        if (sessionState is AppViewModel.SessionState.RequireBiometric) {
                            showBiometricPrompt(
                                onSuccess = { appViewModel.onBiometricSuccess() },
                                onFailed = {
                                    appViewModel.onBiometricFailed()
                                    finish()
                                }
                            )
                        }
                    }

                    LaunchedEffect(sessionState, gradeNotificationsEnabled) {
                        val isStudent = (sessionState as? AppViewModel.SessionState.Authenticated)
                            ?.role == "student"
                        if (isStudent && gradeNotificationsEnabled) {
                            requestPostNotificationsIfNeeded()
                            StudentGradeNotificationWorker.enqueuePeriodic(this@MainActivity)
                            StudentGradeNotificationWorker.enqueueOnce(this@MainActivity)
                        } else {
                            StudentGradeNotificationWorker.cancel(this@MainActivity)
                        }
                    }

                    when (val state = sessionState) {
                        is AppViewModel.SessionState.Checking,
                        is AppViewModel.SessionState.RequireBiometric -> {
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
                                gradeNotificationsEnabled = gradeNotificationsEnabled,
                                onGradeNotificationsEnabledChange = {
                                    notificationSettingsRepository.setGradeNotificationsEnabled(it)
                                },
                                darkThemeEnabled = darkThemeEnabled,
                                onDarkThemeEnabledChange = {
                                    appearanceSettingsRepository.setDarkThemeEnabled(it)
                                },
                                onClearSession = { appViewModel.clearSession() }
                            )
                        }

                        is AppViewModel.SessionState.Unauthenticated -> {
                            JournalNavHost(
                                journalApi = journalApi,
                                appConfig = appConfig,
                                tokenSession = tokenSession,
                                initialRole = null,
                                gradeNotificationsEnabled = gradeNotificationsEnabled,
                                onGradeNotificationsEnabledChange = {
                                    notificationSettingsRepository.setGradeNotificationsEnabled(it)
                                },
                                darkThemeEnabled = darkThemeEnabled,
                                onDarkThemeEnabledChange = {
                                    appearanceSettingsRepository.setDarkThemeEnabled(it)
                                },
                                onClearSession = { appViewModel.clearSession() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            POST_NOTIFICATIONS_REQUEST_CODE
        )
    }

    private fun showBiometricPrompt(
        onSuccess: () -> Unit,
        onFailed: () -> Unit
    ) {
        val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

        val canAuthenticate = BiometricManager.from(this)
            .canAuthenticate(authenticators)

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            onFailed()
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
                    onFailed()
                }

                override fun onAuthenticationFailed() = Unit
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }

    private companion object {
        const val POST_NOTIFICATIONS_REQUEST_CODE = 9101
    }
}
