package com.journal.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Стилизованный DatePickerDialog в цветах приложения (тёмно-синий/белый).
 * Используйте вместо стандартного DatePickerDialog + DatePicker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledDatePickerDialog(
    state: DatePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = AppTheme.colors
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Выбрать", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = colors.primary, fontWeight = FontWeight.SemiBold)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = colors.surface)
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(
                containerColor = colors.surface,
                titleContentColor = colors.primary,
                headlineContentColor = colors.primary,
                navigationContentColor = colors.primary,
                subheadContentColor = colors.primary,
                weekdayContentColor = colors.secondaryText,
                dayContentColor = colors.inputText,
                disabledDayContentColor = colors.fieldPlaceholder,
                todayContentColor = colors.primary,
                todayDateBorderColor = colors.primary,
                selectedDayContentColor = colors.onPrimary,
                selectedDayContainerColor = colors.primary,
                yearContentColor = colors.inputText,
                currentYearContentColor = colors.primary,
                selectedYearContentColor = colors.onPrimary,
                selectedYearContainerColor = colors.primary,
                dividerColor = colors.lessonBackground
            )
        )
    }
}
