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

private val AppPrimaryText = Color(0xFF223268)
private val AppCardBackground = Color.White
private val AppSecondaryText = Color(0xFF7E8E99)
private val AppLightBlue = Color(0xFFE4E6EC)

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
                    containerColor = AppPrimaryText,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Выбрать", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = AppPrimaryText, fontWeight = FontWeight.SemiBold)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = AppCardBackground)
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(
                containerColor = AppCardBackground,
                titleContentColor = AppPrimaryText,
                headlineContentColor = AppPrimaryText,
                navigationContentColor = AppPrimaryText,
                subheadContentColor = AppPrimaryText,
                weekdayContentColor = AppSecondaryText,
                dayContentColor = AppPrimaryText,
                disabledDayContentColor = Color(0xFFADB5BD),
                todayContentColor = AppPrimaryText,
                todayDateBorderColor = AppPrimaryText,
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = AppPrimaryText,
                yearContentColor = AppPrimaryText,
                currentYearContentColor = AppPrimaryText,
                selectedYearContentColor = Color.White,
                selectedYearContainerColor = AppPrimaryText,
                dividerColor = AppLightBlue
            )
        )
    }
}
