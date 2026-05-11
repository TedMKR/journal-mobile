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
private val ActiveLessonBackground = Color(0xFFD7DDF2)
private val BadgeBackground = Color(0xFFD3D7E1)
private val ActiveBadgeBackground = Color(0xFFB8C3EA)

private val dayNames = listOf(
    "Понедельник",
    "Вторник",
    "Среда",
    "Четверг",
    "Пятница",
    "Суббота"
)

private val lessonStartTimes = listOf("09:00", "10:40", "12:50", "14:30", "16:10", "17:50", "19:30")

@Composable
fun TeacherHomeRoute(
    onOpenLesson: (TeacherLesson) -> Unit,
    onOpenDashboard: () -> Unit,
    viewModel: TeacherHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Header(onOpenDashboard = onOpenDashboard)

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
private fun Header(onOpenDashboard: () -> Unit) {
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
            text = "Личный кабинет",
            color = PrimaryText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(onClick = onOpenDashboard)
                .padding(vertical = 8.dp)
        )
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
                    lessons = groupedLessons[dayIndex].orEmpty().sortedWith(compareBy({ lessonOrderNumber(it) }, { it.scheduledAt })),
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
                    orderNumber = lessonOrderNumber(lesson),
                    isActive = isLessonCurrentlyActive(lesson),
                    onOpenLesson = onOpenLesson
                )
            }
        }
    }
}

@Composable
private fun LessonCard(
    lesson: TeacherLesson,
    orderNumber: Int,
    isActive: Boolean,
    onOpenLesson: (TeacherLesson) -> Unit
) {
    val lessonBackground = if (isActive) ActiveLessonBackground else LessonBackground
    val badgeBackground = if (isActive) ActiveBadgeBackground else BadgeBackground

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(lessonBackground, RoundedCornerShape(15.dp))
            .clickable { onOpenLesson(lesson) }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .background(badgeBackground, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(orderNumber.takeIf { it > 0 }?.toString().orEmpty(), color = PrimaryText, fontWeight = FontWeight.SemiBold)
            }
            Text(formatLessonTime(lesson), color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
            Text(lesson.disciplineName, color = PrimaryText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                LessonBadge(text = lessonTypeName(lesson.lessonType), background = badgeBackground)
                LessonBadge(text = lesson.groupName, background = badgeBackground)
                lesson.location?.takeIf { it.isNotBlank() }?.let { location ->
                    LessonBadge(text = location, background = badgeBackground)
                }
            }
            Text(text = "→", color = PrimaryText, style = MaterialTheme.typography.titleLarge)
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

private fun lessonOrderNumber(lesson: TeacherLesson): Int = runCatching {
    val time = OffsetDateTime.parse(lesson.scheduledAt).format(DateTimeFormatter.ofPattern("HH:mm"))
    lessonStartTimes.indexOf(time).takeIf { it >= 0 }?.plus(1) ?: 0
}.getOrDefault(0)

private fun formatLessonTime(lesson: TeacherLesson): String = runCatching {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val start = OffsetDateTime.parse(lesson.scheduledAt).format(formatter)
    val end = lesson.endsAt?.let { OffsetDateTime.parse(it).format(formatter) }
    if (end == null) start else "$start - $end"
}.getOrElse { lesson.scheduledAt }

private fun isLessonCurrentlyActive(lesson: TeacherLesson): Boolean = runCatching {
    val now = OffsetDateTime.now()
    val start = OffsetDateTime.parse(lesson.scheduledAt)
    val end = lesson.endsAt?.let { OffsetDateTime.parse(it) } ?: return false
    now >= start && now <= end
}.getOrDefault(false)

private fun lessonTypeName(type: String): String = when (type) {
    "lecture" -> "Лекция"
    "practice" -> "Практическое занятие"
    "lab" -> "Лабораторная работа"
    "seminar" -> "Семинар"
    else -> type
}
