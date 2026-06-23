package com.journal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.journal.core.ui.DarkJournalAppColors
import com.journal.core.ui.LightJournalAppColors
import com.journal.core.ui.LocalJournalAppColors

private val LightColors = lightColorScheme(
    primary = LightJournalAppColors.primary,
    onPrimary = Color.White,
    background = LightJournalAppColors.background,
    surface = LightJournalAppColors.surface,
    surfaceVariant = LightJournalAppColors.surfaceVariant,
    onSurface = LightJournalAppColors.primary
)

private val DarkColors = darkColorScheme(
    primary = DarkJournalAppColors.primary,
    onPrimary = DarkJournalAppColors.onPrimary,
    background = DarkJournalAppColors.background,
    surface = DarkJournalAppColors.surface,
    surfaceVariant = DarkJournalAppColors.surfaceVariant,
    onSurface = DarkJournalAppColors.inputText
)

@Composable
fun JournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) DarkJournalAppColors else LightJournalAppColors
    CompositionLocalProvider(LocalJournalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content
        )
    }
}
