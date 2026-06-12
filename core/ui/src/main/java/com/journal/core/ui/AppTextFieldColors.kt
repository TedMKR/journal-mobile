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
    unfocusedLabelColor: Color = AppPrimary,
): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppPrimary,
    unfocusedTextColor = AppPrimary,
    disabledTextColor = AppPrimary,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White,
    focusedBorderColor = AppPrimary,
    unfocusedBorderColor = AppFieldBorder,
    disabledBorderColor = AppFieldBorder,
    focusedLabelColor = AppPrimary,
    unfocusedLabelColor = unfocusedLabelColor,
    disabledLabelColor = AppSecondaryText,
    focusedPlaceholderColor = AppFieldPlaceholder,
    unfocusedPlaceholderColor = AppFieldPlaceholder,
    disabledPlaceholderColor = AppFieldPlaceholder,
    cursorColor = AppPrimary
)
