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

private val BackgroundColor = Color(0xFFEDEEED)
private val PrimaryText = Color(0xFF223268)
private val SecondaryText = Color(0xFF7E8E99)
private val CardBackground = Color.White
private val LessonBackground = Color(0xFFE4E6EC)
private val BadgeBackground = Color(0xFFD3D7E1)

private val dayNames = listOf(
    "Понедельник",
    "Вторник",
    "Среда",
    "Четверг",
    "Пятница",
    "Суббота"
)

@Composable
fun TeacherHomeRoute(
    onOpenLesson: (TeacherLesson) -> Unit,
    viewModel: TeacherHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            uiState.error != null -> Text(
                text = uiState.error.orEmpty(),
                color = PrimaryText,
                modifier = Modifier.padding(top = 16.dp)
            )
            else -> WeekSchedule(
                lessons = uiState.lessons,
                onOpenLesson = onOpenLesson
            )
        }
    }
}

@Composable
private fun WeekSchedule(
    lessons: List<TeacherLesson>,
    onOpenLesson: (TeacherLesson) -> Unit
) {
    val groupedLessons = lessons.groupBy { lessonDayIndex(it.scheduledAt) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        dayNames.forEachIndexed { dayIndex, dayName ->
            item {
                DayScheduleCard(
                    dayName = dayName,
                    lessons = groupedLessons[dayIndex].orEmpty()
                        .sortedWith(compareBy({ it.lessonOrderNumber ?: Int.MAX_VALUE }, { it.scheduledAt })),
                    onOpenLesson = onOpenLesson
                )
            }
        }
    }
}

@Composable
private fun DayScheduleCard(
    dayName: String,
    lessons: List<TeacherLesson>,
    onOpenLesson: (TeacherLesson) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = dayName,
            color = SecondaryText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (lessons.isEmpty()) {
            Text(
                text = "Нет занятий",
                color = PrimaryText,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            lessons.forEach { lesson ->
                LessonCard(
                    lesson = lesson,
                    onOpenLesson = onOpenLesson
                )
            }
        }
    }
}

@Composable
private fun LessonCard(
    lesson: TeacherLesson,
    onOpenLesson: (TeacherLesson) -> Unit
) {
    val orderNumber = lesson.lessonOrderNumber ?: 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LessonBackground, RoundedCornerShape(15.dp))
            .clickable { onOpenLesson(lesson) }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (orderNumber > 0) {
                Box(
                    modifier = Modifier
                        .background(BadgeBackground, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(orderNumber.toString(), color = PrimaryText, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(formatLessonTime(lesson.scheduledAt, lesson.endsAt), color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
            Text(lesson.disciplineName, color = PrimaryText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                LessonBadge(text = lessonTypeName(lesson.lessonType), background = BadgeBackground)
                LessonBadge(text = lesson.groupName, background = BadgeBackground)
            }
        }
        lesson.location?.takeIf { it.isNotBlank() }?.let { location ->
            LessonBadge(text = location, background = BadgeBackground)
        }
    }
}

@Composable
private fun LessonBadge(text: String, background: Color) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 3.dp)
    ) {
        Text(text, color = PrimaryText, style = MaterialTheme.typography.bodySmall)
    }
}

private fun lessonDayIndex(scheduledAt: String): Int = runCatching {
    OffsetDateTime.parse(scheduledAt).dayOfWeek.value - 1
}.getOrDefault(-1)

private fun formatLessonTime(scheduledAt: String, endsAt: String?): String = runCatching {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    val start = OffsetDateTime.parse(scheduledAt).format(fmt)
    val end = endsAt?.let { OffsetDateTime.parse(it).format(fmt) }
    if (end != null) "$start - $end" else start
}.getOrElse { "" }

private fun lessonTypeName(type: String): String = when (type) {
    "lecture" -> "Лекция"
    "practice" -> "Практическое занятие"
    "lab" -> "Лабораторная работа"
    "seminar" -> "Семинар"
    else -> type
}
