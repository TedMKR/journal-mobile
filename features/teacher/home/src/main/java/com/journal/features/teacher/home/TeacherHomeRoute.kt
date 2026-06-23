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
import androidx.compose.foundation.layout.size
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
import com.journal.core.ui.AppTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journal.core.model.teacher.TeacherLesson
import com.journal.core.ui.AppBackground
import com.journal.core.ui.AppHeaderBackground
import com.journal.core.ui.AppLessonBackground
import com.journal.core.ui.AppPrimary
import com.journal.core.ui.AppSecondaryText
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BackgroundColor: Color
    @Composable get() = AppTheme.colors.background
private val PrimaryText: Color
    @Composable get() = AppTheme.colors.primary
private val SecondaryText: Color
    @Composable get() = AppTheme.colors.secondaryText
private val CardBackground: Color
    @Composable get() = AppTheme.colors.surface
private val LessonBackground: Color
    @Composable get() = AppTheme.colors.lessonBackground
private val BadgeBackground: Color
    @Composable get() = AppTheme.colors.headerBackground

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

    val weekMonday = uiState.weekMonday
    val weekSunday = weekMonday.plusDays(6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WeekNavBar(
            weekMonday   = weekMonday,
            weekSunday   = weekSunday,
            isCurrentWeek = uiState.isCurrentWeek,
            onPrev       = { viewModel.navigateWeek(-1) },
            onNext       = { viewModel.navigateWeek(+1) }
        )

        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            uiState.error != null -> Text(
                text = uiState.error.orEmpty(),
                color = PrimaryText,
                modifier = Modifier.padding(top = 16.dp)
            )
            else -> WeekSchedule(
                lessons    = uiState.lessons,
                weekMonday = weekMonday,
                onOpenLesson = onOpenLesson
            )
        }
    }
}

// ── Week navigation bar ──────────────────────────────────────────────────────

@Composable
private fun WeekNavBar(
    weekMonday: LocalDate,
    weekSunday: LocalDate,
    isCurrentWeek: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale("ru"))
    val label = "${weekMonday.format(fmt)} – ${weekSunday.format(fmt)}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(BadgeBackground, RoundedCornerShape(12.dp))
                .clickable(onClick = onPrev),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "‹",
                color = PrimaryText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = PrimaryText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (isCurrentWeek) {
                Text(
                    text = "Текущая неделя",
                    color = SecondaryText,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .background(BadgeBackground, RoundedCornerShape(12.dp))
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "›",
                color = PrimaryText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Schedule content ─────────────────────────────────────────────────────────

@Composable
private fun WeekSchedule(
    lessons: List<TeacherLesson>,
    weekMonday: LocalDate,
    onOpenLesson: (TeacherLesson) -> Unit
) {
    if (lessons.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(16.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Нет занятий на этой неделе", color = SecondaryText)
        }
    } else {
        val groupedLessons = lessons.groupBy { lessonDayIndex(it.scheduledAt) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            (0..5).forEach { dayIndex ->
                val dayLessons = groupedLessons[dayIndex].orEmpty()
                    .sortedWith(compareBy({ it.lessonOrderNumber ?: Int.MAX_VALUE }, { it.scheduledAt }))
                if (dayLessons.isNotEmpty()) {
                    item {
                        DayScheduleCard(
                            dayName  = dayNames[dayIndex],
                            dayDate  = weekMonday.plusDays(dayIndex.toLong()),
                            lessons  = dayLessons,
                            onOpenLesson = onOpenLesson
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayScheduleCard(
    dayName: String,
    dayDate: LocalDate,
    lessons: List<TeacherLesson>,
    onOpenLesson: (TeacherLesson) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayName,
                color = SecondaryText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = dayDate.format(DateTimeFormatter.ofPattern("d MMM", Locale("ru"))),
                color = SecondaryText,
                style = MaterialTheme.typography.bodySmall
            )
        }

        lessons.forEach { lesson ->
            LessonCard(lesson = lesson, onOpenLesson = onOpenLesson)
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
            .background(LessonBackground, RoundedCornerShape(12.dp))
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
            Text(
                formatLessonTime(lesson.scheduledAt, lesson.endsAt),
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                lesson.disciplineName,
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
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

// ── Helpers ───────────────────────────────────────────────────────────────────

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
    "lecture"  -> "Лекция"
    "practice" -> "Практическое занятие"
    "lab"      -> "Лабораторная работа"
    "seminar"  -> "Семинар"
    else       -> type
}
