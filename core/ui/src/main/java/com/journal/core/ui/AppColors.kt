package com.journal.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class JournalAppColors(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val inputText: Color,
    val fieldBorder: Color,
    val fieldPlaceholder: Color,
    val headerBackground: Color,
    val lessonBackground: Color,
    val barBackground: Color,
    val danger: Color,
    val dangerContainer: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val outline: Color,
    val overlay: Color
)

val LightJournalAppColors = JournalAppColors(
    primary = Color(0xFF223268),
    onPrimary = Color.White,
    background = Color(0xFFEDEEED),
    surface = Color.White,
    surfaceVariant = Color(0xFFF7F8FB),
    secondaryText = Color(0xFF7E8E99),
    mutedText = Color(0xFF6D7885),
    inputText = Color(0xFF223268),
    fieldBorder = Color(0xFFD1D5DB),
    fieldPlaceholder = Color(0xFF9CA3AF),
    headerBackground = Color(0xFFD3D7E1),
    lessonBackground = Color(0xFFE4E6EC),
    barBackground = Color(0xFFCAD0E3),
    danger = Color(0xFFC44A4A),
    dangerContainer = Color(0xFFFFE4E6),
    success = Color(0xFF1F8A5B),
    successContainer = Color(0xFFDCFCE7),
    warning = Color(0xFFE19B2C),
    warningContainer = Color(0xFFFEF3C7),
    outline = Color(0xFFD1D5DB),
    overlay = Color.Black.copy(alpha = 0.28f)
)

val DarkJournalAppColors = JournalAppColors(
    primary = Color(0xFFC5D0FF),
    onPrimary = Color(0xFF111A38),
    background = Color(0xFF101318),
    surface = Color(0xFF171B22),
    surfaceVariant = Color(0xFF202632),
    secondaryText = Color(0xFFB5BFCC),
    mutedText = Color(0xFF98A3B3),
    inputText = Color(0xFFE7ECF7),
    fieldBorder = Color(0xFF3A4354),
    fieldPlaceholder = Color(0xFF8390A3),
    headerBackground = Color(0xFF283044),
    lessonBackground = Color(0xFF222938),
    barBackground = Color(0xFF37425A),
    danger = Color(0xFFFFB4AB),
    dangerContainer = Color(0xFF4F171D),
    success = Color(0xFF7FE0AF),
    successContainer = Color(0xFF123B29),
    warning = Color(0xFFFFD27A),
    warningContainer = Color(0xFF4B3510),
    outline = Color(0xFF3A4354),
    overlay = Color.Black.copy(alpha = 0.48f)
)

val LocalJournalAppColors = staticCompositionLocalOf { LightJournalAppColors }

object AppTheme {
    val colors: JournalAppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalJournalAppColors.current
}

// Legacy light tokens. Prefer AppTheme.colors in new composables.
val AppPrimary = Color(0xFF223268)

val AppBackground = Color(0xFFEDEEED)

val AppSecondaryText = Color(0xFF7E8E99)
val AppMutedText = Color(0xFF6D7885)
val AppInputText = AppPrimary

val AppFieldBorder = Color(0xFFD1D5DB)
val AppFieldPlaceholder = Color(0xFF9CA3AF)

val AppHeaderBackground = Color(0xFFD3D7E1)
val AppLessonBackground = Color(0xFFE4E6EC)
val AppBarBackground = Color(0xFFCAD0E3)

val AppDanger = Color(0xFFC44A4A)
val AppDangerLight = Color(0xFFFFE4E6)
val AppSuccess = Color(0xFF1F8A5B)
val AppSuccessLight = Color(0xFFDCFCE7)
val AppWarning = Color(0xFFE19B2C)
