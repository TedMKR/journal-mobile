package com.journal.features.student.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.journal.core.model.teacher.StudentLesson
import com.journal.core.model.teacher.StudentProfile
import com.journal.core.model.teacher.StudentSubjectSummary
import com.journal.core.network.api.JournalApi
import java.time.OffsetDateTime
import java.util.Locale

private val Background = Color(0xFFEDEEED)
private val CardBackground = Color.White
private val LessonBackground = Color(0xFFE4E6EC)
private val PrimaryText = Color(0xFF223268)
private val MutedText = Color(0xFF7E8E99)
private val BadgeBackground = Color(0xFFD3D7E1)
private val Accent = Color(0xFF3B82F6)
private val Danger = Color(0xFFDC2626)
private val Success = Color(0xFF16A34A)

private val dayNames = listOf(
    "Понедельник",
    "Вторник",
    "Среда",
    "Четверг",
    "Пятница",
    "Суббота"
)

@Composable
fun StudentScheduleRoute(
    journalApi: JournalApi,
    onOpenSubject: () -> Unit = {}
) {
    var lessons by remember { mutableStateOf<List<StudentLesson>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        runCatching { journalApi.getStudentLessons(limit = 200).lessons }
            .onSuccess { lessons = it }
            .onFailure { error = it.message ?: "Не удалось загрузить расписание" }
        isLoading = false
    }

    StudentScaffold(title = "Расписание занятий") {
        when {
            isLoading -> CenterState { CircularProgressIndicator(color = PrimaryText) }
            error != null -> CenterState { Text(error.orEmpty(), color = Danger) }
            else -> ScheduleContent(lessons = lessons, onOpenSubject = onOpenSubject)
        }
    }
}

@Composable
fun StudentDashboardRoute(
    journalApi: JournalApi,
    onBack: () -> Unit
) {
    var profile by remember { mutableStateOf<StudentProfile?>(null) }
    var subjects by remember { mutableStateOf<List<StudentSubjectSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        runCatching {
            val loadedProfile = journalApi.getStudentProfile()
            val loadedSubjects = journalApi.getStudentSubjects().subjects
            loadedProfile to loadedSubjects
        }.onSuccess { (loadedProfile, loadedSubjects) ->
            profile = loadedProfile
            subjects = loadedSubjects
        }.onFailure {
            error = it.message ?: "Не удалось загрузить личный кабинет"
        }
        isLoading = false
    }

    StudentScaffold(title = "Личный кабинет") {
        Text(
            text = "← Назад",
            modifier = Modifier.clickable(onClick = onBack),
            color = MutedText,
            fontWeight = FontWeight.SemiBold
        )
        when {
            isLoading -> CenterState { CircularProgressIndicator(color = PrimaryText) }
            error != null -> CenterState { Text(error.orEmpty(), color = Danger) }
            else -> StudentDashboardContent(profile = profile, subjects = subjects)
        }
    }
}

@Composable
private fun StudentScaffold(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = PrimaryText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun ScheduleContent(
    lessons: List<StudentLesson>,
    onOpenSubject: () -> Unit
) {
    val groupedLessons = lessons.groupBy { lessonDayIndex(it.scheduledAt) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        dayNames.forEachIndexed { dayIndex, dayName ->
            item {
                StudentDayScheduleCard(
                    dayName = dayName,
                    lessons = groupedLessons[dayIndex].orEmpty().sortedBy { it.scheduledAt },
                    onOpenSubject = onOpenSubject
                )
            }
        }
    }
}

@Composable
private fun StudentDayScheduleCard(
    dayName: String,
    lessons: List<StudentLesson>,
    onOpenSubject: () -> Unit
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
            color = MutedText,
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
            lessons.forEachIndexed { index, lesson ->
                StudentLessonCard(
                    lesson = lesson,
                    orderNumber = index + 1,
                    onClick = onOpenSubject
                )
            }
        }
    }
}

@Composable
private fun StudentLessonCard(
    lesson: StudentLesson,
    orderNumber: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LessonBackground, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .background(BadgeBackground, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(orderNumber.toString(), color = PrimaryText, fontWeight = FontWeight.SemiBold)
            }
            Text(lessonSlotTime(orderNumber), color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = lesson.disciplineName,
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoChip(lessonTypeName(lesson.lessonType))
            lesson.teacherName?.takeIf { it.isNotBlank() }?.let { InfoChip(it) }
        }
        lesson.location?.takeIf { it.isNotBlank() }?.let { InfoChip(it) }
        lesson.myAttendanceStatus?.let { AttendanceChip(status = it) }
        lesson.topic?.takeIf { it.isNotBlank() }?.let {
            Text("Тема: $it", color = MutedText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StudentDashboardContent(
    profile: StudentProfile?,
    subjects: List<StudentSubjectSummary>
) {
    ProfileCard(profile)
    SummaryStats(subjects)
    SubjectsCard(subjects)
}

@Composable
private fun ProfileCard(profile: StudentProfile?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(profile?.fullName ?: "Профиль студента", color = PrimaryText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        profile?.studentCode?.takeIf { it.isNotBlank() }?.let { Text("Код: $it", color = MutedText) }
        profile?.groupName?.takeIf { it.isNotBlank() }?.let { Text("Группа: $it", color = MutedText) }
        if (profile?.isHeadStudent == true) InfoChip("Староста")
    }
}

@Composable
private fun SummaryStats(subjects: List<StudentSubjectSummary>) {
    val avgGrade = subjects.mapNotNull { it.avgGrade }.takeIf { it.isNotEmpty() }?.average()
    val attendance = subjects.takeIf { it.isNotEmpty() }?.map { it.attendancePct }?.average() ?: 0.0
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(title = "Предметы", value = subjects.size.toString(), modifier = Modifier.weight(1f))
        StatCard(title = "Средний балл", value = avgGrade?.let { String.format(Locale.US, "%.1f", it) } ?: "—", modifier = Modifier.weight(1f))
        StatCard(title = "Посещаемость", value = "${attendance.toInt()}%", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(CardBackground, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = PrimaryText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(title, color = MutedText, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SubjectsCard(subjects: List<StudentSubjectSummary>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Мои предметы", color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (subjects.isEmpty()) {
            Text("Нет данных", color = MutedText)
        } else {
            subjects.forEach { subject -> SubjectRow(subject) }
        }
    }
}

@Composable
private fun SubjectRow(subject: StudentSubjectSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AttendanceRing(percent = subject.attendancePct.toInt())
            Column(modifier = Modifier.weight(1f)) {
                Text(subject.disciplineName, color = PrimaryText, fontWeight = FontWeight.Bold)
                subject.teacherName?.takeIf { it.isNotBlank() }?.let { Text(it, color = MutedText, style = MaterialTheme.typography.bodySmall) }
                Text(
                    "${subject.lessonsAttended}/${subject.lessonsTotal} занятий",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(subject.avgGrade?.let { String.format(Locale.US, "%.1f", it) } ?: "—", color = PrimaryText, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFE5E7EB)).padding(top = 1.dp))
    }
}

@Composable
private fun AttendanceRing(percent: Int) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(54.dp).padding(4.dp)) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = Color(0xFFE5E7EB),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke),
                size = Size(size.width, size.height)
            )
            drawArc(
                color = Accent,
                startAngle = -90f,
                sweepAngle = 360f * percent.coerceIn(0, 100) / 100f,
                useCenter = false,
                style = Stroke(width = stroke),
                size = Size(size.width, size.height)
            )
        }
        Text("$percent%", color = PrimaryText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoChip(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .background(BadgeBackground, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = PrimaryText,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun AttendanceChip(status: String) {
    val (text, color) = when (status) {
        "present" -> "Присутствовал" to Success
        "absent" -> "Отсутствовал" to Danger
        "valid_excuse" -> "Уважительная причина" to Accent
        else -> status to MutedText
    }
    Text(text, color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun CenterState(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 44.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}

private fun lessonDayIndex(scheduledAt: String): Int = runCatching {
    OffsetDateTime.parse(scheduledAt).dayOfWeek.value - 1
}.getOrDefault(-1)

private fun lessonSlotTime(orderNumber: Int): String = when (orderNumber) {
    1 -> "09:00 - 10:30"
    2 -> "10:40 - 12:10"
    3 -> "12:50 - 14:20"
    4 -> "14:30 - 16:00"
    5 -> "16:10 - 17:40"
    6 -> "17:50 - 19:20"
    else -> "—"
}

private fun lessonTypeName(type: String): String = when (type) {
    "lecture" -> "Лекция"
    "practice" -> "Практика"
    "lab" -> "Лабораторная"
    "seminar" -> "Семинар"
    else -> type
}
