package com.journal.core.data.theme

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class AppearanceSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _darkThemeEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_DARK_THEME_ENABLED, false)
    )

    val darkThemeEnabled: StateFlow<Boolean> =
        _darkThemeEnabled.asStateFlow()

    fun setDarkThemeEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_DARK_THEME_ENABLED, enabled)
            .apply()
        _darkThemeEnabled.value = enabled
    }

    private companion object {
        const val PREFS_NAME = "journal_appearance_settings"
        const val KEY_DARK_THEME_ENABLED = "dark_theme_enabled"
    }
}
