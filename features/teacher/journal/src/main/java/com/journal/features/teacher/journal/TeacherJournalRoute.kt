package com.journal.features.teacher.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.journal.core.model.teacher.CreateGradeRequest
import com.journal.core.model.teacher.JournalGridAssessmentForm
import com.journal.core.model.teacher.JournalGridAttendance
import com.journal.core.model.teacher.JournalGridGrade
import com.journal.core.model.teacher.JournalGridLesson
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.JournalGridStudent
import com.journal.core.model.teacher.MarkAttendanceRequest
import com.journal.core.model.teacher.UpdateGradeRequest
import com.journal.core.network.api.JournalApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val BackgroundColor = Color(0xFFEDEEED)
private val PrimaryText = Color(0xFF223268)
private val CardBackground = Color.White
private val HeaderBackground = Color(0xFFD3D7E1)
private val CellBorder = Color(0xFFC9CED8)
private val AccentBlue = Color(0xFF223268)
private val PresentColor = Color(0xFF1F8A5B)
private val AbsentColor = Color(0xFFC44A4A)
private val ExcuseColor = Color(0xFFE19B2C)

private const val ATTENDANCE_COLUMN_WIDTH = 62
private const val GRADE_COLUMN_WIDTH = 74

@Composable
fun TeacherJournalRoute(
    groupId: String,
    disciplineId: String,
    periodId: String,
    lessonType: String,
    journalApi: JournalApi,
    onOpenStudentCard: (String) -> Unit
) {
    var journal by remember(groupId, disciplineId, periodId) { mutableStateOf<JournalGridResponse?>(null) }
    var isLoading by remember(groupId, disciplineId, periodId) { mutableStateOf(true) }
    var error by remember(groupId, disciplineId, periodId) { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(groupId, disciplineId, periodId, refreshKey) {
        isLoading = true
        error = null
        runCatching {
            journalApi.getGroupJournalGrid(
                groupId = groupId,
                disciplineId = disciplineId,
                academicPeriodId = periodId
            )
        }.onSuccess { response ->
            journal = response
            isLoading = false
        }.onFailure { throwable ->
            error = throwable.message ?: "Не удалось загрузить журнал"
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
            error != null -> Text(text = error.orEmpty(), color = PrimaryText, modifier = Modifier.align(Alignment.Center))
            journal != null -> JournalContent(
                journal = journal!!,
                selectedLessonType = lessonType,
                journalApi = journalApi,
                onOpenStudentCard = onOpenStudentCard,
                onRefresh = { refreshKey++ }
            )
        }
    }
}

@Composable
private fun JournalContent(
    journal: JournalGridResponse,
    selectedLessonType: String,
    journalApi: JournalApi,
    onOpenStudentCard: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var currentType by remember(selectedLessonType) { mutableStateOf(selectedLessonType.ifBlank { journal.lessons.firstOrNull()?.lessonType.orEmpty() }) }
    val availableTypes = journal.lessons.map { it.lessonType }.distinct().ifEmpty { listOf(currentType) }.filter { it.isNotBlank() }
    val filteredLessons = journal.lessons
        .filter { it.lessonType == currentType }
        .sortedBy { it.scheduledAt }
    val visibleAttendance = journal.attendance.filter { attendance -> filteredLessons.any { it.lessonId == attendance.lessonId } }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        JournalHeader(journal = journal, currentType = currentType)
        LessonTypeTabs(types = availableTypes, selectedType = currentType, onSelect = { currentType = it })
        JournalTable(
            students = journal.students,
            lessons = filteredLessons,
            attendance = visibleAttendance,
            assessmentForms = journal.assessmentForms.sortedBy { it.date },
            grades = journal.grades,
            canEditAttendance = journal.permissions.canEditAttendance,
            canEditGrades = journal.permissions.canEditGrades,
            journalApi = journalApi,
            onOpenStudentCard = onOpenStudentCard,
            onRefresh = onRefresh
        )
    }
}

@Composable
private fun JournalHeader(journal: JournalGridResponse, currentType: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(journal.discipline.name, color = PrimaryText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag(journal.group.name)
            Tag(journal.academicPeriod.name)
            Tag(lessonTypeName(currentType))
        }
    }
}

@Composable
private fun LessonTypeTabs(types: List<String>, selectedType: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        types.forEach { type ->
            val selected = type == selectedType
            Box(
                modifier = Modifier
                    .background(if (selected) AccentBlue else CardBackground, RoundedCornerShape(14.dp))
                    .clickable { onSelect(type) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = lessonTypeName(type),
                    color = if (selected) Color.White else PrimaryText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun JournalTable(
    students: List<JournalGridStudent>,
    lessons: List<JournalGridLesson>,
    attendance: List<JournalGridAttendance>,
    assessmentForms: List<JournalGridAssessmentForm>,
    grades: List<JournalGridGrade>,
    canEditAttendance: Boolean,
    canEditGrades: Boolean,
    journalApi: JournalApi,
    onOpenStudentCard: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val horizontalScroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var attendanceDialog by remember { mutableStateOf<AttendanceEditState?>(null) }
    var gradeDialog by remember { mutableStateOf<GradeEditState?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(18.dp))
            .padding(8.dp)
    ) {
        Row {
            Column {
                FixedHeader()
                students.forEachIndexed { index, student ->
                    FixedStudentRow(
                        index = index + 1,
                        student = student,
                        onOpenStudentCard = onOpenStudentCard
                    )
                }
            }
            Column(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                DynamicHeader(lessons = lessons, assessmentForms = assessmentForms)
                students.forEach { student ->
                    val studentAttendance = attendance.filter { it.studentId == student.studentId }
                    val studentGrades = grades.filter { it.studentId == student.studentId }
                    DynamicStudentRow(
                        lessons = lessons,
                        attendance = studentAttendance,
                        assessmentForms = assessmentForms,
                        grades = studentGrades,
                        canEditAttendance = canEditAttendance,
                        canEditGrades = canEditGrades,
                        onAttendanceClick = { lesson, record ->
                            if (canEditAttendance) attendanceDialog = AttendanceEditState(student, lesson, record)
                        },
                        onGradeClick = { form, grade ->
                            if (canEditGrades) gradeDialog = GradeEditState(student, form, grade)
                        }
                    )
                }
            }
        }
    }

    attendanceDialog?.let { state ->
        AttendanceDialog(
            state = state,
            onDismiss = { attendanceDialog = null },
            onSave = { status, comment ->
                scope.launch {
                    runCatching {
                        journalApi.markAttendance(
                            lessonId = state.lesson.lessonId,
                            request = MarkAttendanceRequest(
                                studentId = state.student.studentId,
                                status = status,
                                comment = comment.takeIf { it.isNotBlank() }
                            )
                        )
                    }.onSuccess {
                        attendanceDialog = null
                        onRefresh()
                    }
                }
            }
        )
    }

    gradeDialog?.let { state ->
        GradeDialog(
            state = state,
            onDismiss = { gradeDialog = null },
            onSave = { value, comment ->
                scope.launch {
                    runCatching {
                        if (state.grade == null) {
                            journalApi.createGrade(
                                CreateGradeRequest(
                                    studentId = state.student.studentId,
                                    assessmentFormId = state.form.assessmentFormId,
                                    value = value,
                                    comment = comment.takeIf { it.isNotBlank() }
                                )
                            )
                        } else {
                            journalApi.updateGrade(
                                gradeId = state.grade.gradeId,
                                request = UpdateGradeRequest(
                                    value = value,
                                    comment = comment.takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    }.onSuccess {
                        gradeDialog = null
                        onRefresh()
                    }
                }
            }
        )
    }
}

@Composable
private fun FixedHeader() {
    Column {
        Row {
            TableCell("№", 42, isHeader = true)
            TableCell("Студент", 190, isHeader = true, textAlign = TextAlign.Start)
        }
    }
}

@Composable
private fun DynamicHeader(lessons: List<JournalGridLesson>, assessmentForms: List<JournalGridAssessmentForm>) {
    Row {
        lessons.forEach { lesson ->
            TableCell(formatLessonDate(lesson), ATTENDANCE_COLUMN_WIDTH, isHeader = true)
        }
        assessmentForms.forEach { form ->
            TableCell(form.title, GRADE_COLUMN_WIDTH, isHeader = true)
        }
    }
}

@Composable
private fun FixedStudentRow(
    index: Int,
    student: JournalGridStudent,
    onOpenStudentCard: (String) -> Unit
) {
    Row {
        TableCell(index.toString(), 42)
        TableCell(
            text = student.fullName,
            width = 190,
            textAlign = TextAlign.Start,
            clickable = true,
            onClick = { onOpenStudentCard(student.studentId) }
        )
    }
}

@Composable
private fun DynamicStudentRow(
    lessons: List<JournalGridLesson>,
    attendance: List<JournalGridAttendance>,
    assessmentForms: List<JournalGridAssessmentForm>,
    grades: List<JournalGridGrade>,
    canEditAttendance: Boolean,
    canEditGrades: Boolean,
    onAttendanceClick: (JournalGridLesson, JournalGridAttendance?) -> Unit,
    onGradeClick: (JournalGridAssessmentForm, JournalGridGrade?) -> Unit
) {
    Row {
        lessons.forEach { lesson ->
            val record = attendance.firstOrNull { it.lessonId == lesson.lessonId }
            TableCell(
                text = attendanceSymbol(record?.status),
                width = ATTENDANCE_COLUMN_WIDTH,
                color = attendanceColor(record?.status),
                clickable = canEditAttendance,
                onClick = { onAttendanceClick(lesson, record) }
            )
        }
        assessmentForms.forEach { form ->
            val grade = grades.firstOrNull { it.assessmentFormId == form.assessmentFormId }
            TableCell(
                text = grade?.value.orEmpty(),
                width = GRADE_COLUMN_WIDTH,
                color = PrimaryText,
                clickable = canEditGrades,
                onClick = { onGradeClick(form, grade) }
            )
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    width: Int,
    isHeader: Boolean = false,
    color: Color = PrimaryText,
    textAlign: TextAlign = TextAlign.Center,
    clickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(if (isHeader) 54.dp else 44.dp)
            .background(if (isHeader) HeaderBackground else CardBackground)
            .border(0.5.dp, CellBorder)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = textAlign,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Tag(text: String) {
    Box(
        modifier = Modifier
            .background(HeaderBackground, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text, color = PrimaryText, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AttendanceDialog(
    state: AttendanceEditState,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var selectedStatus by remember(state) { mutableStateOf(state.record?.status ?: "present") }
    var comment by remember(state) { mutableStateOf(state.record?.comment.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Посещаемость") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.student.fullName)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("present", "absent", "valid_excuse").forEach { status ->
                        StatusChip(
                            text = attendanceSymbol(status),
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status }
                        )
                    }
                }
                OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Комментарий") })
            }
        },
        confirmButton = { Button(onClick = { onSave(selectedStatus, comment) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun GradeDialog(
    state: GradeEditState,
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit
) {
    var selectedValue by remember(state) { mutableStateOf(state.grade?.value?.toIntOrNull() ?: 5) }
    var comment by remember(state) { mutableStateOf(state.grade?.comment.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.form.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.student.fullName)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { value ->
                        StatusChip(
                            text = value.toString(),
                            selected = selectedValue == value,
                            onClick = { selectedValue = value }
                        )
                    }
                }
                OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Комментарий") })
            }
        },
        confirmButton = { Button(onClick = { onSave(selectedValue, comment) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun StatusChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(if (selected) AccentBlue else HeaderBackground, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.White else PrimaryText, fontWeight = FontWeight.Bold)
    }
}

private data class AttendanceEditState(
    val student: JournalGridStudent,
    val lesson: JournalGridLesson,
    val record: JournalGridAttendance?
)

private data class GradeEditState(
    val student: JournalGridStudent,
    val form: JournalGridAssessmentForm,
    val grade: JournalGridGrade?
)

private fun formatLessonDate(lesson: JournalGridLesson): String = runCatching {
    LocalDate.parse(lesson.date).format(DateTimeFormatter.ofPattern("dd.MM"))
}.getOrElse { lesson.date }

private fun attendanceSymbol(status: String?): String = when (status) {
    "present" -> "П"
    "absent" -> "Н"
    "valid_excuse" -> "У"
    null -> ""
    else -> status
}

private fun attendanceColor(status: String?): Color = when (status) {
    "present" -> PresentColor
    "absent" -> AbsentColor
    "valid_excuse" -> ExcuseColor
    else -> PrimaryText
}

private fun lessonTypeName(type: String): String = when (type) {
    "lecture" -> "Лекция"
    "practice" -> "Практическое занятие"
    "lab" -> "Лабораторная работа"
    "seminar" -> "Семинар"
    else -> type
}
