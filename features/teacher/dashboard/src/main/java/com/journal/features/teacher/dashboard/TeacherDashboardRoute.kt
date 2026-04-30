package com.journal.features.teacher.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TeacherDashboardRoute(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEEED))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Личный кабинет", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF223268))
        Text("Поддержанные API-блоки будут подключены на следующем шаге.", color = Color(0xFF223268))
        Button(onClick = onBack) {
            Text("Назад")
        }
    }
}
