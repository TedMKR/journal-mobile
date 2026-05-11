package com.journal.features.teacher.studentcard

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.JournalGridStudent
import com.journal.core.network.api.JournalApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BackgroundColor = Color(0xFFEDEEED)
private val PrimaryText = Color(0xFF223268)
private val MutedText = Color(0xFF7E8E99)
private val CardBackground = Color.White
private val LightBlue = Color(0xFFD3D7E1)
private val BarBackground = Color(0xFFCAD0E3)

@Composable
fun TeacherStudentCardRoute(
    groupId: String,
    disciplineId: String,
    periodId: String,
    studentId: String,
    journalApi: JournalApi,
    onBack: () -> Unit
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
            card != null -> StudentCardContent(card = card!!, onBack = onBack)
        }
    }
}

@Composable
private fun StudentCardContent(card: StudentCardUiState, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← Назад к журналу группы",
                color = MutedText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(vertical = 8.dp)
            )
            Text(card.disciplineName, color = PrimaryText, fontWeight = FontWeight.SemiBold)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(22.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    Text(card.student.fullName, color = PrimaryText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                .background(CardBackground, RoundedCornerShape(22.dp))
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
            .background(LightBlue, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Посещаемость по месяцам", color = PrimaryText, fontWeight = FontWeight.Bold)
        if (months.isEmpty()) {
            Text("Нет данных", color = MutedText)
        } else {
            months.forEach { month ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(month.month, color = PrimaryText, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    CircularPercent(month.percent)
                    Text("${month.percent}%", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CircularPercent(percent: Int) {
    Canvas(modifier = Modifier.height(34.dp).width(34.dp)) {
        val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        val arcSize = Size(size.minDimension, size.minDimension)
        drawArc(BarBackground, -90f, 360f, false, topLeft = Offset.Zero, size = arcSize, style = stroke)
        drawArc(PrimaryText, -90f, percent.coerceIn(0, 100) * 3.6f, false, topLeft = Offset.Zero, size = arcSize, style = stroke)
    }
}

@Composable
private fun ProgressCard(title: String, done: Int, total: Int) {
    val percent = if (total > 0) done.toFloat() / total.toFloat() else 0f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightBlue, RoundedCornerShape(18.dp))
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
    LocalDate.parse(date.take(10)).format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru")))
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
