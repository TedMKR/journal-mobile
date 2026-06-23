package com.journal.core.data.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class NotificationSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _gradeNotificationsEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_GRADE_NOTIFICATIONS_ENABLED, true)
    )

    val gradeNotificationsEnabled: StateFlow<Boolean> =
        _gradeNotificationsEnabled.asStateFlow()

    fun areGradeNotificationsEnabled(): Boolean =
        _gradeNotificationsEnabled.value

    fun setGradeNotificationsEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_GRADE_NOTIFICATIONS_ENABLED, enabled)
            .apply()
        _gradeNotificationsEnabled.value = enabled
    }

    private companion object {
        const val PREFS_NAME = "journal_notification_settings"
        const val KEY_GRADE_NOTIFICATIONS_ENABLED = "grade_notifications_enabled"
    }
}
