package com.journal.features.teacher.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.journal.core.model.teacher.JournalGridAttendance
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.network.api.JournalApi
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val BgColor = Color(0xFFEDEEED)
private val PrimaryText = Color(0xFF223268)
private val HeaderBg = Color(0xFF223268)
private val GridLine = Color(0xFF7E8E99).copy(alpha = 0.45f)
private val CellBg = Color.White

private const val NumberColWidth = 32
private const val StudentColWidth = 180
private const val LessonColWidth = 46

@Composable
fun TeacherJournalRoute(
    groupId: String,
    disciplineId: String,
    periodId: String,
    journalApi: JournalApi,
    onOpenStudentCard: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var journal by remember { mutableStateOf<JournalGridResponse?>(null) }

    LaunchedEffect(groupId, disciplineId, periodId) {
        isLoading = true
        error = null
        journal = null

        if (groupId.isBlank() || disciplineId.isBlank() || periodId.isBlank()) {
            isLoading = false
            error = "Недостаточно параметров для загрузки журнала"
            return@LaunchedEffect
        }

        runCatching {
            journalApi.getGroupJournalGrid(
                groupId = groupId,
                disciplineId = disciplineId,
                academicPeriodId = periodId
            )
        }.onSuccess {
            journal = it
            isLoading = false
        }.onFailure {
            error = it.message ?: "Не удалось загрузить журнал"
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Журнал", color = PrimaryText, style = MaterialTheme.typography.headlineSmall)

        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))

            error != null -> Text(
                text = error ?: "Ошибка",
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium
            )

            journal != null -> JournalContent(journal = journal!!, onOpenStudentCard = onOpenStudentCard)
        }
    }
}

@Composable
private fun JournalContent(
    journal: JournalGridResponse,
    onOpenStudentCard: () -> Unit
) {
    val lessons = journal.lessons.sortedBy { it.scheduledAt }

    Text(
        text = journal.discipline.name,
        color = PrimaryText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tag(journal.group.name)
        Tag(lessonTypeRu(lessons.firstOrNull()?.lessonType ?: "practice"))
    }

    Button(onClick = { }) {
        Text("Экспорт")
    }

    val hScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .background(CellBg, RoundedCornerShape(10.dp))
            .horizontalScroll(hScroll)
    ) {
        JournalHeader(lessons = lessons)

        journal.students.forEachIndexed { index, student ->
            Row(
                modifier = Modifier
                    .background(CellBg)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Cell(
                    text = (index + 1).toString().padStart(2, '0'),
                    widthDp = NumberColWidth,
                    isNumberColumn = true
                )
                Cell(
                    text = student.fullName,
                    widthDp = StudentColWidth,
                    clickable = true,
                    onClick = onOpenStudentCard,
                    alignCenter = false
                )

                lessons.forEach { lesson ->
                    Cell(
                        text = resolveCellValue(
                            lessonId = lesson.lessonId,
                            studentId = student.studentId,
                            journal = journal
                        ),
                        widthDp = LessonColWidth
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .background(HeaderBg, RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("Редактировать", color = Color.White)
    }
}

@Composable
private fun JournalHeader(lessons: List<com.journal.core.model.teacher.JournalGridLesson>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HeaderCell("№", NumberColWidth)
        HeaderCell("Студент", StudentColWidth)
        lessons.forEach { lesson ->
            HeaderCell(dateToShort(lesson.date), LessonColWidth)
        }
    }
}

@Composable
private fun HeaderCell(text: String, widthDp: Int) {
    Box(
        modifier = Modifier
            .width(widthDp.dp)
            .background(HeaderBg)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Cell(
    text: String,
    widthDp: Int,
    clickable: Boolean = false,
    onClick: () -> Unit = {},
    isNumberColumn: Boolean = false,
    alignCenter: Boolean = true
) {
    val contentModifier = Modifier
        .width(widthDp.dp)
        .background(if (isNumberColumn) HeaderBg else CellBg)
        .padding(horizontal = 6.dp, vertical = 8.dp)

    Box(
        modifier = if (clickable) contentModifier.clickable(onClick = onClick) else contentModifier,
        contentAlignment = if (alignCenter) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = if (isNumberColumn) Color.White else PrimaryText,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
    }

    Box(
        modifier = Modifier
            .width(1.dp)
            .background(GridLine)
    )
}

private fun resolveCellValue(
    lessonId: String,
    studentId: String,
    journal: JournalGridResponse
): String {
    val attendance = journal.attendance.firstOrNull { item ->
        item.studentId == studentId && item.lessonId == lessonId
    }

    if (attendance != null) return attendanceToCell(attendance)

    val lessonDate = journal.lessons.firstOrNull { it.lessonId == lessonId }?.date
    if (lessonDate != null) {
        val grade = journal.grades.firstOrNull { grade ->
            grade.studentId == studentId &&
                journal.assessmentForms.any { form ->
                    form.assessmentFormId == grade.assessmentFormId && form.date.startsWith(lessonDate)
                }
        }
        if (grade != null) return grade.value
    }

    return ""
}

private fun attendanceToCell(attendance: JournalGridAttendance): String = when (attendance.status) {
    "present" -> "П"
    "absent" -> "Н"
    "valid_excuse" -> "У"
    else -> ""
}

private fun dateToShort(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd"))
}.getOrElse {
    runCatching {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd"))
    }.getOrDefault(value)
}

private fun lessonTypeRu(type: String): String = when (type.lowercase()) {
    "lecture" -> "Лекция"
    "practice" -> "Практическое занятие"
    "lab" -> "Лабораторная"
    "seminar" -> "Семинар"
    else -> type
}

@Composable
private fun Tag(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFD3D7E1), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = PrimaryText, style = MaterialTheme.typography.bodySmall)
    }
}
