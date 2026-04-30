package com.journal.features.teacher.journal

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.journal.core.model.teacher.JournalGridAttendance
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.network.api.JournalApi
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val BgColor = Color(0xFFEDEEED)
private val PrimaryText = Color(0xFF223268)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TeacherJournalEntryPoint {
    fun journalApi(): JournalApi
}

@Composable
fun TeacherJournalRoute(
    groupId: String,
    disciplineId: String,
    periodId: String,
    onOpenStudentCard: () -> Unit
) {
    val context = LocalContext.current
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            TeacherJournalEntryPoint::class.java
        )
    }

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
            entryPoint.journalApi().getGroupJournalGrid(
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
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
            }

            error != null -> {
                Text(
                    text = error ?: "Ошибка",
                    color = PrimaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            journal != null -> {
                JournalContent(
                    journal = journal!!,
                    onOpenStudentCard = onOpenStudentCard
                )
            }
        }
    }
}

@Composable
private fun JournalContent(
    journal: JournalGridResponse,
    onOpenStudentCard: () -> Unit
) {
    Text(
        text = journal.discipline.name,
        color = PrimaryText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tag(journal.group.name)
        Tag(lessonTypeRu(journal.lessons.firstOrNull()?.lessonType ?: "practice"))
    }

    Button(onClick = { }) {
        Text("Экспорт")
    }

    val dates = journal.lessons
        .map { it.date }
        .distinct()
        .sorted()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Студент",
                color = Color.White,
                modifier = Modifier
                    .background(PrimaryText, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                fontWeight = FontWeight.SemiBold
            )
            dates.forEach { date ->
                Text(
                    text = dateToShort(date),
                    color = Color.White,
                    modifier = Modifier
                        .background(PrimaryText, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        journal.students.forEach { student ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = student.fullName,
                    color = PrimaryText,
                    modifier = Modifier
                        .clickable(onClick = onOpenStudentCard)
                        .padding(vertical = 2.dp)
                )

                dates.forEach { date ->
                    Text(
                        text = resolveCellValue(date, student.studentId, journal),
                        color = PrimaryText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .background(Color(0xFF223268), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text("Редактировать", color = Color.White)
    }
}

private fun resolveCellValue(
    date: String,
    studentId: String,
    journal: JournalGridResponse
): String {
    val lessonIdsForDate = journal.lessons
        .filter { it.date == date }
        .map { it.lessonId }
        .toSet()

    val grade = journal.grades.firstOrNull { grade ->
        grade.studentId == studentId &&
            journal.assessmentForms.any { form ->
                form.assessmentFormId == grade.assessmentFormId && form.date.startsWith(date)
            }
    }
    if (grade != null) return grade.value

    val attendance = journal.attendance.firstOrNull { attendance ->
        attendance.studentId == studentId && attendance.lessonId in lessonIdsForDate
    }
    if (attendance != null) return attendanceToCell(attendance)

    return "-"
}

private fun attendanceToCell(attendance: JournalGridAttendance): String = when (attendance.status) {
    "present" -> "П"
    "absent" -> "Н"
    "valid_excuse" -> "У"
    else -> "-"
}

private fun dateToShort(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd.MM"))
}.getOrDefault(value)

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
