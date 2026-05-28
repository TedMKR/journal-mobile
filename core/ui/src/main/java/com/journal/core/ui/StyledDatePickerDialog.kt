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
import androidx.compose.ui.graphics.Color
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
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Выбрать", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = AppPrimary, fontWeight = FontWeight.SemiBold)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = Color.White)
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(
                containerColor = Color.White,
                titleContentColor = AppPrimary,
                headlineContentColor = AppPrimary,
                navigationContentColor = AppPrimary,
                subheadContentColor = AppPrimary,
                weekdayContentColor = AppSecondaryText,
                dayContentColor = AppPrimary,
                disabledDayContentColor = Color(0xFFADB5BD),
                todayContentColor = AppPrimary,
                todayDateBorderColor = AppPrimary,
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = AppPrimary,
                yearContentColor = AppPrimary,
                currentYearContentColor = AppPrimary,
                selectedYearContentColor = Color.White,
                selectedYearContainerColor = AppPrimary,
                dividerColor = AppLessonBackground
            )
        )
    }
}
