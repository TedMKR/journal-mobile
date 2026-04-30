package com.journal.features.teacher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TeacherHomeRoute(
    onOpenLesson: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenVed: () -> Unit,
    viewModel: TeacherHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEEED))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Иванов Иван Иванович", style = MaterialTheme.typography.titleMedium, color = Color(0xFF223268))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Dashboard", modifier = Modifier.clickable(onClick = onOpenDashboard), color = Color(0xFF223268))
                Text("Ведомости", modifier = Modifier.clickable(onClick = onOpenVed), color = Color(0xFF223268))
            }
        }

        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            }

            uiState.error != null -> {
                Text(
                    text = uiState.error ?: "Ошибка",
                    color = Color(0xFF223268),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            uiState.lessons.isEmpty() -> {
                Text("Нет занятий", modifier = Modifier.padding(top = 16.dp), color = Color(0xFF223268))
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.lessons) { lesson ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenLesson() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(lesson.disciplineName, color = Color(0xFF223268), style = MaterialTheme.typography.titleSmall)
                                Text("${lesson.scheduledAt} • ${lesson.lessonType}", color = Color(0xFF223268))
                                Text(lesson.groupName, color = Color(0xFF223268))
                            }
                        }
                    }
                }
            }
        }
    }
}
