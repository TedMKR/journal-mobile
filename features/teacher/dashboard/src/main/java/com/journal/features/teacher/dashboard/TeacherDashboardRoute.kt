package com.journal.features.teacher.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.TeacherLesson
import com.journal.core.network.api.JournalApi
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BackgroundColor = Color(0xFFEDEEED)
private val PrimaryText = Color(0xFF223268)
private val SecondaryText = Color(0xFF7E8E99)
private val CardBackground = Color.White
private val AccentBackground = Color(0xFFD3D7E1)
private val LessonBackground = Color(0xFFE4E6EC)

@Composable
fun TeacherDashboardRoute(
    journalApi: JournalApi,
    onBack: () -> Unit,
    onOpenJournal: (TeacherDashboardJournalTarget) -> Unit
) {
    var state by remember { mutableStateOf<TeacherDashboardUiState?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        runCatching {
            val lessons = journalApi.getLessons(limit = 200).lessons
            buildDashboardState(journalApi = journalApi, lessons = lessons)
        }.onSuccess { uiState ->
            state = uiState
            isLoading = false
        }.onFailure { throwable ->
            error = throwable.message ?: "Не удалось загрузить личный кабинет"
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            error != null -> Text(error.orEmpty(), color = PrimaryText, modifier = Modifier.align(Alignment.Center))
            state != null -> TeacherDashboardContent(
                state = state!!,
                onBack = onBack,
                onOpenJournal = onOpenJournal
            )
        }
    }
}

@Composable
private fun TeacherDashboardContent(
    state: TeacherDashboardUiState,
    onBack: () -> Unit,
    onOpenJournal: (TeacherDashboardJournalTarget) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Header(onBack = onBack)
        ProfileSummary(state)
        TodayScheduleCard(state.todayLessons)
        AnalyticsCard(state = state, onOpenJournal = onOpenJournal)
        GroupPerformanceCard(state.groupPerformance)
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "← Назад",
            color = SecondaryText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun ProfileSummary(state: TeacherDashboardUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryText, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = state.teacherName ?: "Профиль преподавателя недоступен",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("Всего\nстудентов", state.totalStudents.toString(), Modifier.weight(1f))
            StatTile("Всего\nпредметов", state.totalDisciplines.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("Часов\nв расписании", state.hoursInSchedule, Modifier.weight(1f))
            StatTile("Средняя\nуспеваемость", state.avgGrade, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(PrimaryText, RoundedCornerShape(4.dp))
        )
        Text(title, color = PrimaryText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(value, color = PrimaryText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TodayScheduleCard(lessons: List<TeacherLesson>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Расписание на сегодня", color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (lessons.isEmpty()) {
            Text("На сегодня занятий нет", color = SecondaryText)
        } else {
            lessons.forEach { lesson ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LessonBackground, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(lesson.disciplineName, color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = listOf(formatLessonTime(lesson), lessonTypeName(lesson.lessonType), lesson.groupName).joinToString(" · "),
                        color = SecondaryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsCard(
    state: TeacherDashboardUiState,
    onOpenJournal: (TeacherDashboardJournalTarget) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Аналитика", color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(state.analyticsTitle, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
        AttendanceLineChart(state.attendanceByMonth)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ActionTile("Студентов\nв группе", state.selectedStudentsCount.toString(), Modifier.weight(1f)) {
                state.defaultJournalTarget?.let(onOpenJournal)
            }
            ActionTile("Открыть\nжурнал", "→", Modifier.weight(1f)) {
                state.defaultJournalTarget?.let(onOpenJournal)
            }
        }
    }
}

@Composable
private fun ActionTile(title: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .background(AccentBackground, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = PrimaryText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(value, color = PrimaryText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GroupPerformanceCard(items: List<GroupPerformance>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Успеваемость по группам", color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (items.isEmpty()) {
            Text("Нет данных", color = SecondaryText)
        } else {
            items.take(8).forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.groupName, color = PrimaryText, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .background(AccentBackground, RoundedCornerShape(6.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((item.avgGrade / 5f).coerceIn(0f, 1f))
                                .height(8.dp)
                                .background(PrimaryText, RoundedCornerShape(6.dp))
                        )
                    }
                    Text(formatGrade(item.avgGrade), color = PrimaryText, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(32.dp))
                }
            }
        }
    }
}

@Composable
private fun AttendanceLineChart(months: List<AttendanceMonth>) {
    if (months.isEmpty()) {
        Text("Нет данных по посещаемости", color = SecondaryText)
        return
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        val chartTop = 18.dp.toPx()
        val chartBottom = size.height - 30.dp.toPx()
        val leftPadding = 28.dp.toPx()
        val rightPadding = 28.dp.toPx()
        val availableWidth = size.width - leftPadding - rightPadding
        val points = months.mapIndexed { index, month ->
            val x = if (months.size == 1) size.width / 2f else leftPadding + availableWidth * index / (months.size - 1)
            val y = chartBottom - (chartBottom - chartTop) * (month.percent.coerceIn(0, 100) / 100f)
            Offset(x, y)
        }

        points.zipWithNext().forEach { (start, end) ->
            drawLine(PrimaryText, start, end, strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)
        }

        points.forEachIndexed { index, point ->
            val labelWidth = 38.dp.toPx()
            val labelHeight = 18.dp.toPx()
            drawRoundRect(
                color = PrimaryText,
                topLeft = Offset(point.x - labelWidth / 2f, point.y - labelHeight / 2f),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx())
            )
            drawContext.canvas.nativeCanvas.apply {
                val labelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 11.dp.toPx()
                    isAntiAlias = true
                }
                drawText("${months[index].percent}%", point.x, point.y + 4.dp.toPx(), labelPaint)

                val monthPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(34, 50, 104)
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 11.dp.toPx()
                    isAntiAlias = true
                }
                drawText(months[index].month, point.x, size.height - 4.dp.toPx(), monthPaint)
            }
        }
    }
}

private suspend fun buildDashboardState(
    journalApi: JournalApi,
    lessons: List<TeacherLesson>
): TeacherDashboardUiState {
    val uniqueDisciplines = lessons.mapNotNull { lesson -> lesson.disciplineId?.let { it to lesson.disciplineName } }.distinctBy { it.first }
    val defaultLesson = lessons.firstOrNull { it.groupId != null && it.disciplineId != null && it.periodId != null }
    val defaultJournal = defaultLesson?.let { lesson ->
        runCatching {
            journalApi.getGroupJournalGrid(
                groupId = lesson.groupId.orEmpty(),
                disciplineId = lesson.disciplineId.orEmpty(),
                academicPeriodId = lesson.periodId.orEmpty()
            )
        }.getOrNull()
    }

    return TeacherDashboardUiState(
        teacherName = defaultJournal?.teacher?.fullName,
        totalStudents = defaultJournal?.students?.size ?: 0,
        totalDisciplines = uniqueDisciplines.size,
        hoursInSchedule = formatHours(lessons.sumOf { lessonDurationMinutes(it) }),
        avgGrade = averageGrade(defaultJournal),
        todayLessons = todayLessons(lessons),
        analyticsTitle = defaultJournal?.let { "${it.discipline.name} · ${it.group.name}" } ?: "Нет выбранного журнала",
        selectedStudentsCount = defaultJournal?.students?.size ?: 0,
        attendanceByMonth = attendanceByMonth(defaultJournal),
        groupPerformance = groupPerformance(lessons = lessons, defaultJournal = defaultJournal),
        defaultJournalTarget = defaultLesson?.let {
            TeacherDashboardJournalTarget(
                groupId = it.groupId.orEmpty(),
                disciplineId = it.disciplineId.orEmpty(),
                periodId = it.periodId.orEmpty(),
                lessonType = it.lessonType
            )
        }
    )
}

private fun todayLessons(lessons: List<TeacherLesson>): List<TeacherLesson> {
    val today = LocalDate.now()
    return lessons.filter { lesson ->
        runCatching { OffsetDateTime.parse(lesson.scheduledAt).toLocalDate() == today }.getOrDefault(false)
    }.sortedBy { it.scheduledAt }
}

private fun attendanceByMonth(journal: JournalGridResponse?): List<AttendanceMonth> {
    if (journal == null) return emptyList()
    return journal.lessons
        .groupBy { lesson -> monthLabel(lesson.date) }
        .map { (month, lessons) ->
            val totalPossible = lessons.size * journal.students.size
            val present = lessons.sumOf { lesson ->
                journal.attendance.count { it.lessonId == lesson.lessonId && it.status == "present" }
            }
            AttendanceMonth(
                month = month,
                percent = if (totalPossible > 0) present * 100 / totalPossible else 0
            )
        }
}

private fun groupPerformance(lessons: List<TeacherLesson>, defaultJournal: JournalGridResponse?): List<GroupPerformance> {
    val groupNames = lessons
        .mapNotNull { lesson -> lesson.groupId?.let { it to lesson.groupName } }
        .distinctBy { it.first }
        .map { it.second }
    val defaultGroup = defaultJournal?.group?.name
    val defaultAvg = defaultJournal?.grades.orEmpty()
        .mapNotNull { it.value.toFloatOrNull() }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.toFloat()
    return groupNames.map { groupName ->
        GroupPerformance(
            groupName = groupName,
            avgGrade = if (groupName == defaultGroup && defaultAvg != null) defaultAvg else 0f
        )
    }.sortedByDescending { it.avgGrade }
}

private fun averageGrade(journal: JournalGridResponse?): String {
    val grades = journal?.grades.orEmpty().mapNotNull { it.value.toFloatOrNull() }
    return if (grades.isEmpty()) "—" else String.format(Locale.US, "%.1f", grades.average())
}

private fun lessonDurationMinutes(lesson: TeacherLesson): Int = runCatching {
    val start = OffsetDateTime.parse(lesson.scheduledAt)
    val end = lesson.endsAt?.let { OffsetDateTime.parse(it) } ?: return 0
    java.time.Duration.between(start, end).toMinutes().toInt().coerceAtLeast(0)
}.getOrDefault(0)

private fun formatHours(minutes: Int): String {
    if (minutes <= 0) return "—"
    val hours = minutes / 60f
    return String.format(Locale.US, "%.1f", hours)
}

private fun formatLessonTime(lesson: TeacherLesson): String = runCatching {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val start = OffsetDateTime.parse(lesson.scheduledAt).format(formatter)
    val end = lesson.endsAt?.let { OffsetDateTime.parse(it).format(formatter) }
    if (end == null) start else "$start - $end"
}.getOrElse { lesson.scheduledAt }

private fun lessonTypeName(type: String): String = when (type) {
    "lecture" -> "Лекция"
    "practice" -> "Практическое занятие"
    "lab" -> "Лабораторная работа"
    "seminar" -> "Семинар"
    else -> type
}

private fun monthLabel(date: String): String = runCatching {
    LocalDate.parse(date.take(10)).format(DateTimeFormatter.ofPattern("LLLL", Locale("ru")))
}.getOrElse { date }

private fun formatGrade(value: Float): String = String.format(Locale.US, "%.1f", value)

private data class TeacherDashboardUiState(
    val teacherName: String?,
    val totalStudents: Int,
    val totalDisciplines: Int,
    val hoursInSchedule: String,
    val avgGrade: String,
    val todayLessons: List<TeacherLesson>,
    val analyticsTitle: String,
    val selectedStudentsCount: Int,
    val attendanceByMonth: List<AttendanceMonth>,
    val groupPerformance: List<GroupPerformance>,
    val defaultJournalTarget: TeacherDashboardJournalTarget?
)

data class TeacherDashboardJournalTarget(
    val groupId: String,
    val disciplineId: String,
    val periodId: String,
    val lessonType: String
)

private data class AttendanceMonth(
    val month: String,
    val percent: Int
)

private data class GroupPerformance(
    val groupName: String,
    val avgGrade: Float
)
