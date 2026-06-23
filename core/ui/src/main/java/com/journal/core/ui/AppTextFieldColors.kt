package com.journal.core.ui

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Единый стиль полей ввода для всего приложения.
 *
 * @param unfocusedLabelColor Цвет лейбла в неактивном состоянии.
 *   По умолчанию [AppPrimary] (тёмно-синий).
 *   Передайте [AppSecondaryText] для приглушённого варианта (ведомости, фильтры).
 */
@Composable
fun appFieldColors(
    unfocusedLabelColor: Color = AppTheme.colors.primary,
): TextFieldColors {
    val colors = AppTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.inputText,
        unfocusedTextColor = colors.inputText,
        disabledTextColor = colors.inputText,
        focusedContainerColor = colors.surface,
        unfocusedContainerColor = colors.surface,
        disabledContainerColor = colors.surface,
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.fieldBorder,
        disabledBorderColor = colors.fieldBorder,
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = unfocusedLabelColor,
        disabledLabelColor = colors.secondaryText,
        focusedPlaceholderColor = colors.fieldPlaceholder,
        unfocusedPlaceholderColor = colors.fieldPlaceholder,
        disabledPlaceholderColor = colors.fieldPlaceholder,
        cursorColor = colors.primary
    )
}
