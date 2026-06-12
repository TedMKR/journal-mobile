package com.journal.features.teacher.studentcard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.journal.core.common.config.PersonNameFormatter
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.JournalGridStudent
import com.journal.core.network.api.JournalApi
import com.journal.core.ui.AppBackground
import com.journal.core.ui.AppBarBackground
import com.journal.core.ui.AppHeaderBackground
import com.journal.core.ui.AppPrimary
import com.journal.core.ui.AppSecondaryText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BackgroundColor = AppBackground
private val PrimaryText = AppPrimary
private val MutedText = AppSecondaryText
private val CardBackground = Color.White
private val LightBlue = AppHeaderBackground
private val BarBackground = AppBarBackground

@Composable
fun TeacherStudentCardRoute(
    groupId: String,
    disciplineId: String,
    periodId: String,
    studentId: String,
    journalApi: JournalApi
) {
    var card by remember(groupId, disciplineId, periodId, studentId) { mutableStateOf<StudentCardUiState?>(null) }
    var isLoading by remember(groupId, disciplineId, periodId, studentId) { mutableStateOf(true) }
    var error by remember(groupId, disciplineId, periodId, studentId) { mutableStateOf<String?>(null) }

    LaunchedEffect(groupId, disciplineId, periodId, studentId) {
        isLoading = true
        error = null
        runCatching {
            val journal = journalApi.getGroupJournalGrid(
                groupId = groupId,
                disciplineId = disciplineId,
                academicPeriodId = periodId
            )
            buildStudentCard(journal, studentId)
        }.onSuccess { state ->
            card = state
            isLoading = false
        }.onFailure { throwable ->
            error = throwable.message ?: "Не удалось загрузить карточку студента"
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(10.dp)
    ) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            error != null -> Text(error.orEmpty(), color = PrimaryText, modifier = Modifier.align(Alignment.Center))
            card != null -> StudentCardContent(card = card!!)
        }
    }
}

@Composable
private fun StudentCardContent(card: StudentCardUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    Text(PersonNameFormatter.formatFullName(card.student.fullName), color = PrimaryText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Tag(card.groupName)
                    card.student.externalId?.takeIf { it.isNotBlank() }?.let { Tag(it) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatSmallCard("Средняя\nуспеваемость", card.avgGrade ?: "—")
                    StatSmallCard("Пропусков", card.absencesCount.toString())
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Статистика", color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AttendanceChartCard(card.attendanceByMonth)
                ProgressCard("Сдано заданий", card.completedAssessments, card.totalAssessments)
                ProgressCard("Посещено занятий", card.attendedLessons, card.totalLessons)
            }
        }
    }
}

@Composable
private fun StatSmallCard(title: String, value: String) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .background(CardBackground, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(PrimaryText, RoundedCornerShape(4.dp))
        )
        Text(title, color = PrimaryText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(value, color = PrimaryText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AttendanceChartCard(months: List<StudentAttendanceMonth>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightBlue, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Посещаемость по месяцам", color = PrimaryText, fontWeight = FontWeight.Bold)
        if (months.isEmpty()) {
            Text("Нет данных", color = MutedText)
        } else {
            AttendanceLineChart(months = months)
        }
    }
}

@Composable
private fun AttendanceLineChart(months: List<StudentAttendanceMonth>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        val chartTop = 18.dp.toPx()
        val chartBottom = size.height - 28.dp.toPx()
        val leftPadding = 28.dp.toPx()
        val rightPadding = 28.dp.toPx()
        val availableWidth = size.width - leftPadding - rightPadding
        val points = months.mapIndexed { index, month ->
            val x = if (months.size == 1) size.width / 2f else leftPadding + availableWidth * index / (months.size - 1)
            val normalized = month.percent.coerceIn(0, 100) / 100f
            val y = chartBottom - (chartBottom - chartTop) * normalized
            Offset(x, y)
        }

        points.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = PrimaryText,
                start = start,
                end = end,
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        points.forEachIndexed { index, point ->
            val percentText = "${months[index].percent}%"
            val labelWidth = 38.dp.toPx()
            val labelHeight = 18.dp.toPx()
            drawRoundRect(
                color = PrimaryText,
                topLeft = Offset(point.x - labelWidth / 2f, point.y - labelHeight / 2f),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx())
            )
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 11.dp.toPx()
                    isAntiAlias = true
                }
                drawText(percentText, point.x, point.y + 4.dp.toPx(), paint)

                val monthPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(34, 50, 104)
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 11.dp.toPx()
                    isAntiAlias = true
                }
                drawText(months[index].month, point.x, size.height - 4.dp.toPx(), monthPaint)
            }
        }

        drawLine(
            color = PrimaryText.copy(alpha = 0.35f),
            start = Offset(leftPadding, chartBottom + 4.dp.toPx()),
            end = Offset(size.width - rightPadding, chartBottom + 4.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun ProgressCard(title: String, done: Int, total: Int) {
    val percent = if (total > 0) done.toFloat() / total.toFloat() else 0f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightBlue, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = PrimaryText, fontWeight = FontWeight.Bold)
            Text("$done/$total", color = PrimaryText, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(BarBackground, RoundedCornerShape(6.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent.coerceIn(0f, 1f))
                    .height(10.dp)
                    .background(PrimaryText, RoundedCornerShape(6.dp))
            )
        }
    }
}

@Composable
private fun Tag(text: String) {
    Box(
        modifier = Modifier
            .background(LightBlue, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text, color = PrimaryText, style = MaterialTheme.typography.bodySmall)
    }
}

private fun buildStudentCard(journal: JournalGridResponse, studentId: String): StudentCardUiState {
    val student = journal.students.first { it.studentId == studentId }
    val totalLessons = journal.lessons.size
    val attendedLessons = journal.lessons.count { lesson ->
        journal.attendance.any { it.studentId == studentId && it.lessonId == lesson.lessonId && it.status == "present" }
    }
    val absencesCount = totalLessons - attendedLessons
    val studentGrades = journal.grades.filter { it.studentId == studentId }
    val numericGrades = studentGrades.mapNotNull { it.value.toDoubleOrNull() }
    val avgGrade = numericGrades.takeIf { it.isNotEmpty() }?.average()?.let { String.format(Locale.US, "%.1f", it) }
    val completedAssessments = journal.assessmentForms.count { form ->
        studentGrades.any { it.assessmentFormId == form.assessmentFormId && it.value.isNotBlank() }
    }
    val attendanceByMonth = journal.lessons
        .groupBy { lesson -> monthLabel(lesson.date) }
        .map { (month, lessons) ->
            val attended = lessons.count { lesson ->
                journal.attendance.any { it.studentId == studentId && it.lessonId == lesson.lessonId && it.status == "present" }
            }
            StudentAttendanceMonth(
                month = month,
                percent = if (lessons.isNotEmpty()) (attended * 100 / lessons.size) else 0
            )
        }

    return StudentCardUiState(
        student = student,
        groupName = journal.group.name,
        disciplineName = journal.discipline.name,
        avgGrade = avgGrade,
        absencesCount = absencesCount,
        totalLessons = totalLessons,
        attendedLessons = attendedLessons,
        totalAssessments = journal.assessmentForms.size,
        completedAssessments = completedAssessments,
        attendanceByMonth = attendanceByMonth
    )
}

private fun monthLabel(date: String): String = runCatching {
    LocalDate.parse(date.take(10)).format(DateTimeFormatter.ofPattern("LLLL", Locale("ru")))
}.getOrElse { date }

private data class StudentCardUiState(
    val student: JournalGridStudent,
    val groupName: String,
    val disciplineName: String,
    val avgGrade: String?,
    val absencesCount: Int,
    val totalLessons: Int,
    val attendedLessons: Int,
    val totalAssessments: Int,
    val completedAssessments: Int,
    val attendanceByMonth: List<StudentAttendanceMonth>
)

data class StudentAttendanceMonth(
    val month: String,
    val percent: Int
)
