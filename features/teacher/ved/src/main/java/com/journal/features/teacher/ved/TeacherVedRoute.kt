package com.journal.features.teacher.ved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val BackgroundColor = Color(0xFFEDEEED)
private val PrimaryText = Color(0xFF223268)

@Composable
fun TeacherVedRoute() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Ведомости",
            style = MaterialTheme.typography.headlineSmall,
            color = PrimaryText,
            fontWeight = FontWeight.Bold
        )
        Text("Сценарии на базе /reports/* временно вне scope.", color = PrimaryText)
    }
}
