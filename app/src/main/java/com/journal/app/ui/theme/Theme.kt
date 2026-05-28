package com.journal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.journal.core.ui.AppBackground
import com.journal.core.ui.AppPrimary

private val LightColors = lightColorScheme(
    primary = AppPrimary,
    onPrimary = Color.White,
    background = AppBackground,
    surface = Color.White,
    onSurface = AppPrimary
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF91A4E6),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

@Composable
fun JournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
