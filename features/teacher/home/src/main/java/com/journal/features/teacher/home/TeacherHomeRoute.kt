package com.journal.features.teacher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journal.core.model.teacher.TeacherLesson
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val BgColor = Color(0xFFEDEEED)
private val PrimaryText = Color(0xFF223268)
private val CardBg = Color(0xFFE4E6EC)

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
            .background(BgColor)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Электронный\nЖурнал",
                color = PrimaryText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "☰",
                color = PrimaryText,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.clickable(onClick = onOpenDashboard)
            )
        }

        Text(
            text = "Иванов Иван Иванович",
            color = PrimaryText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )

        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            }

            uiState.error != null -> {
                Text(
                    text = uiState.error ?: "Ошибка",
                    color = PrimaryText,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            else -> {
                val grouped = uiState.lessons.groupBy { lessonDayLabel(it.scheduledAt) }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    grouped.forEach { (day, lessons) ->
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = day,
                                    color = Color(0xFF7E8E99),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (lessons.isEmpty()) {
                                    Text(
                                        text = "Нет занятий",
                                        color = PrimaryText,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp)
                                    )
                                } else {
                                    lessons.forEachIndexed { index, lesson ->
                                        LessonCard(
                                            lesson = lesson,
                                            index = index + 1,
                                            onOpenLesson = onOpenLesson,
                                            onOpenVed = onOpenVed
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (grouped.isEmpty()) {
                        item {
                            Text("Нет занятий", color = PrimaryText, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonCard(
    lesson: TeacherLesson,
    index: Int,
    onOpenLesson: () -> Unit,
    onOpenVed: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(8.dp))
            .clickable(onClick = onOpenLesson)
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFD3D7E1), RoundedCornerShape(7.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(index.toString(), color = PrimaryText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = formatLessonTime(lesson.scheduledAt, lesson.endsAt),
                    color = PrimaryText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = lesson.disciplineName,
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Box(
                modifier = Modifier
                    .background(Color(0xFFD3D7E1), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(lessonTypeRu(lesson.lessonType), color = PrimaryText, style = MaterialTheme.typography.bodySmall)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFD3D7E1), RoundedCornerShape(7.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(lesson.groupName, color = PrimaryText, style = MaterialTheme.typography.bodySmall)
                }

                Text(
                    text = "→",
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable(onClick = onOpenVed)
                )
            }
        }
    }
}

private fun lessonDayLabel(scheduledAt: String): String = runCatching {
    when (OffsetDateTime.parse(scheduledAt).dayOfWeek.value) {
        1 -> "Понедельник"
        2 -> "Вторник"
        3 -> "Среда"
        4 -> "Четверг"
        5 -> "Пятница"
        6 -> "Суббота"
        else -> "Воскресенье"
    }
}.getOrDefault("День")

private fun formatLessonTime(startsAt: String, endsAt: String?): String = runCatching {
    val start = OffsetDateTime.parse(startsAt)
    val end = endsAt?.let { OffsetDateTime.parse(it) } ?: start.plusMinutes(90)
    val fmt = DateTimeFormatter.ofPattern("H:mm")
    "${start.format(fmt)} - ${end.format(fmt)}"
}.getOrDefault(startsAt)

private fun lessonTypeRu(type: String): String = when (type.lowercase()) {
    "lecture" -> "Лекция"
    "practice" -> "Практическое занятие"
    "lab" -> "Лабораторная"
    "seminar" -> "Семинар"
    else -> type
}
