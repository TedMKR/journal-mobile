package com.journal.features.teacher.journal

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.journal.core.model.teacher.CreateAssessmentFormRequest
import com.journal.core.model.teacher.CreateGradeRequest
import com.journal.core.model.teacher.JournalGridAssessmentForm
import com.journal.core.model.teacher.JournalGridAttendance
import com.journal.core.model.teacher.JournalGridGrade
import com.journal.core.model.teacher.JournalGridLesson
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.JournalGridStudent
import com.journal.core.model.teacher.MarkAttendanceRequest
import com.journal.core.model.teacher.UpdateAssessmentFormRequest
import com.journal.core.model.teacher.UpdateGradeRequest
import com.journal.core.model.teacher.UpdateLessonTopicDetailsRequest
import com.journal.core.network.api.JournalApi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
private val DangerColor = Color(0xFFC44A4A)

private const val ATTENDANCE_COLUMN_WIDTH = 82
private const val GRADE_COLUMN_WIDTH = 112

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentType by remember(selectedLessonType) { mutableStateOf(selectedLessonType.ifBlank { journal.lessons.firstOrNull()?.lessonType.orEmpty() }) }
    var assessmentDialog by remember { mutableStateOf<AssessmentEditState?>(null) }
    var deleteAssessmentDialog by remember { mutableStateOf<JournalGridAssessmentForm?>(null) }

    val availableTypes = journal.lessons.map { it.lessonType }.distinct().ifEmpty { listOf(currentType) }.filter { it.isNotBlank() }
    val filteredLessons = journal.lessons
        .filter { it.lessonType == currentType }
        .sortedBy { it.scheduledAt }
    val visibleAttendance = journal.attendance.filter { attendance -> filteredLessons.any { it.lessonId == attendance.lessonId } }
    val sortedAssessmentForms = journal.assessmentForms.sortedBy { it.date }
    val showGrades = currentType != "lecture"
    val canEditAssessments = showGrades && journal.permissions.canEditGrades && !journal.academicPeriod.isClosed

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        JournalHeader(journal = journal, currentType = currentType)
        LessonTypeTabs(types = availableTypes, selectedType = currentType, onSelect = { currentType = it })
        JournalActions(
            canEditAssessments = canEditAssessments,
            onExport = {
                shareJournalCsv(
                    context = context,
                    journal = journal,
                    lessons = filteredLessons,
                    attendance = visibleAttendance,
                    assessmentForms = sortedAssessmentForms,
                    grades = journal.grades,
                    showGrades = showGrades
                )
            },
            onAddAssessment = {
                assessmentDialog = AssessmentEditState(
                    form = null,
                    initialTitle = "",
                    initialType = "quiz",
                    initialDate = LocalDate.now().toString()
                )
            }
        )
        JournalTable(
            students = journal.students,
            lessons = filteredLessons,
            attendance = visibleAttendance,
            assessmentForms = sortedAssessmentForms,
            grades = journal.grades,
            canEditAttendance = journal.permissions.canEditAttendance && !journal.academicPeriod.isClosed,
            canEditGrades = journal.permissions.canEditGrades && !journal.academicPeriod.isClosed,
            showGrades = showGrades,
            journalApi = journalApi,
            onOpenStudentCard = onOpenStudentCard,
            onEditAssessment = { form ->
                assessmentDialog = AssessmentEditState(
                    form = form,
                    initialTitle = form.title,
                    initialType = form.type,
                    initialDate = formatApiDate(form.date)
                )
            },
            onRefresh = onRefresh
        )
    }

    assessmentDialog?.let { state ->
        AssessmentDialog(
            state = state,
            onDismiss = { assessmentDialog = null },
            onDelete = { form ->
                assessmentDialog = null
                deleteAssessmentDialog = form
            },
            onSave = { title, type, date ->
                scope.launch {
                    runCatching {
                        if (state.form == null) {
                            journalApi.createAssessmentForm(
                                CreateAssessmentFormRequest(
                                    title = title,
                                    formType = type,
                                    date = date,
                                    disciplineId = journal.discipline.id,
                                    groupId = journal.group.id,
                                    periodId = journal.academicPeriod.id
                                )
                            )
                        } else {
                            journalApi.updateAssessmentForm(
                                assessmentFormId = state.form.assessmentFormId,
                                request = UpdateAssessmentFormRequest(
                                    title = title,
                                    formType = type,
                                    date = date
                                )
                            )
                        }
                    }.onSuccess {
                        assessmentDialog = null
                        onRefresh()
                    }
                }
            }
        )
    }

    deleteAssessmentDialog?.let { form ->
        ConfirmDeleteAssessmentDialog(
            form = form,
            onDismiss = { deleteAssessmentDialog = null },
            onConfirm = {
                scope.launch {
                    runCatching {
                        journalApi.deleteAssessmentForm(assessmentFormId = form.assessmentFormId)
                    }.onSuccess {
                        deleteAssessmentDialog = null
                        onRefresh()
                    }
                }
            }
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
        Text(
            text = journal.discipline.name,
            color = PrimaryText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Tag(journal.group.name)
        Tag(journal.academicPeriod.name)
        Tag(lessonTypeName(currentType))
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
private fun JournalActions(
    canEditAssessments: Boolean,
    onExport: () -> Unit,
    onAddAssessment: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        JournalActionButton(text = "Экспорт", onClick = onExport)
        if (canEditAssessments) {
            JournalActionButton(text = "Добавить контроль", onClick = onAddAssessment)
        }
    }
}

@Composable
private fun JournalActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentBlue,
            contentColor = Color.White
        )
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
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
    showGrades: Boolean,
    journalApi: JournalApi,
    onOpenStudentCard: (String) -> Unit,
    onEditAssessment: (JournalGridAssessmentForm) -> Unit,
    onRefresh: () -> Unit
) {
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var attendanceDialog by remember { mutableStateOf<AttendanceEditState?>(null) }
    var gradeDialog by remember { mutableStateOf<GradeEditState?>(null) }
    var topicDialog by remember { mutableStateOf<TopicEditState?>(null) }
    val visibleForms = if (showGrades) assessmentForms else emptyList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(18.dp))
            .padding(8.dp)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .height(360.dp)
                    .verticalScroll(verticalScroll)
            ) {
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
                    DynamicHeader(
                        lessons = lessons,
                        assessmentForms = visibleForms,
                        canEditGrades = canEditGrades,
                        onLessonClick = { lesson -> topicDialog = TopicEditState(lesson) },
                        onEditAssessment = onEditAssessment
                    )
                    students.forEach { student ->
                        val studentAttendance = attendance.filter { it.studentId == student.studentId }
                        val studentGrades = grades.filter { it.studentId == student.studentId }
                        DynamicStudentRow(
                            lessons = lessons,
                            attendance = studentAttendance,
                            assessmentForms = visibleForms,
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
            VerticalScrollIndicator(
                scrollValue = verticalScroll.value,
                maxValue = verticalScroll.maxValue,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        HorizontalScrollIndicator(
            scrollValue = horizontalScroll.value,
            maxValue = horizontalScroll.maxValue
        )
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

    topicDialog?.let { state ->
        TopicDialog(
            state = state,
            onDismiss = { topicDialog = null },
            onSave = { topic ->
                scope.launch {
                    runCatching {
                        journalApi.updateLessonTopicDetails(
                            lessonId = state.lesson.lessonId,
                            request = UpdateLessonTopicDetailsRequest(topicCustomDetails = topic)
                        )
                    }.onSuccess {
                        topicDialog = null
                        onRefresh()
                    }
                }
            }
        )
    }
}

@Composable
private fun HorizontalScrollIndicator(scrollValue: Int, maxValue: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(HeaderBackground, RoundedCornerShape(8.dp))
    ) {
        val progress = if (maxValue > 0) scrollValue.toFloat() / maxValue.toFloat() else 0f
        Box(
            modifier = Modifier
                .fillMaxWidth(if (maxValue > 0) 0.28f else 1f)
                .height(8.dp)
                .offset(x = (220 * progress).dp)
                .background(AccentBlue, RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun VerticalScrollIndicator(scrollValue: Int, maxValue: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(8.dp)
            .height(350.dp)
            .background(HeaderBackground, RoundedCornerShape(8.dp))
    ) {
        val progress = if (maxValue > 0) scrollValue.toFloat() / maxValue.toFloat() else 0f
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(if (maxValue > 0) 70.dp else 350.dp)
                .offset(y = (280 * progress).dp)
                .background(AccentBlue, RoundedCornerShape(8.dp))
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
private fun DynamicHeader(
    lessons: List<JournalGridLesson>,
    assessmentForms: List<JournalGridAssessmentForm>,
    canEditGrades: Boolean,
    onLessonClick: (JournalGridLesson) -> Unit,
    onEditAssessment: (JournalGridAssessmentForm) -> Unit
) {
    Row {
        lessons.forEach { lesson ->
            TableCell(
                text = lessonHeaderText(lesson),
                width = ATTENDANCE_COLUMN_WIDTH,
                isHeader = true,
                clickable = true,
                onClick = { onLessonClick(lesson) }
            )
        }
        assessmentForms.forEach { form ->
            TableCell(
                text = assessmentHeaderText(form),
                width = GRADE_COLUMN_WIDTH,
                isHeader = true,
                clickable = canEditGrades,
                onClick = { onEditAssessment(form) }
            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TableCell(
    text: String,
    width: Int,
    isHeader: Boolean = false,
    color: Color = PrimaryText,
    textAlign: TextAlign = TextAlign.Center,
    clickable: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(if (isHeader) 70.dp else 44.dp)
            .background(if (isHeader) HeaderBackground else CardBackground)
            .border(0.5.dp, CellBorder)
            .then(
                if (clickable) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = textAlign,
            maxLines = if (isHeader) 4 else 2,
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
        confirmButton = { DialogPrimaryButton(text = "Сохранить", onClick = { onSave(selectedStatus, comment) }) },
        dismissButton = { DialogTextButton(text = "Отмена", onClick = onDismiss) }
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
                    (2..5).forEach { value ->
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
        confirmButton = { DialogPrimaryButton(text = "Сохранить", onClick = { onSave(selectedValue, comment) }) },
        dismissButton = { DialogTextButton(text = "Отмена", onClick = onDismiss) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssessmentDialog(
    state: AssessmentEditState,
    onDismiss: () -> Unit,
    onDelete: (JournalGridAssessmentForm) -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember(state) { mutableStateOf(state.initialTitle) }
    var type by remember(state) { mutableStateOf(state.initialType) }
    var date by remember(state) { mutableStateOf(state.initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    val initialSelectedDateMillis = remember(state) { date.toUtcStartOfDayMillis() }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedDateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                DialogPrimaryButton(
                    text = "Выбрать",
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            date = selectedMillis.toIsoLocalDate()
                        }
                        showDatePicker = false
                    }
                )
            },
            dismissButton = { DialogTextButton(text = "Отмена", onClick = { showDatePicker = false }) }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.form == null) "Добавить контроль" else "Редактировать контроль") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название работы") })
                Text("Тип контроля", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("exam", "quiz", "homework", "project").forEach { option ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (type == option) AccentBlue else HeaderBackground, RoundedCornerShape(10.dp))
                                .clickable { type = option }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(formTypeName(option), color = if (type == option) Color.White else PrimaryText)
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Дата") },
                        readOnly = true,
                        modifier = Modifier.weight(1f)
                    )
                    DialogPrimaryButton(text = "Выбрать", onClick = { showDatePicker = true })
                }
                state.form?.let { form ->
                    TextButton(onClick = { onDelete(form) }) {
                        Text("Удалить контроль", color = DangerColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            DialogPrimaryButton(
                text = "Сохранить",
                enabled = title.isNotBlank() && date.isNotBlank(),
                onClick = { onSave(title.trim(), type, date.trim()) }
            )
        },
        dismissButton = { DialogTextButton(text = "Отмена", onClick = onDismiss) }
    )
}

@Composable
private fun ConfirmDeleteAssessmentDialog(
    form: JournalGridAssessmentForm,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удаление контрольного мероприятия") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Удалить контроль: ${form.title}?")
                Text("Это действие скроет контрольное мероприятие и связанные с ним оценки из обычного журнала.", color = DangerColor)
            }
        },
        confirmButton = {
            DialogPrimaryButton(
                text = "Удалить",
                containerColor = DangerColor,
                onClick = onConfirm
            )
        },
        dismissButton = { DialogTextButton(text = "Отмена", onClick = onDismiss) }
    )
}

@Composable
private fun TopicDialog(
    state: TopicEditState,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var topic by remember(state) { mutableStateOf(state.lesson.topic.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Тема занятия") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(formatLessonDate(state.lesson), color = PrimaryText)
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Тема / комментарий к занятию") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            DialogPrimaryButton(
                text = "Сохранить",
                enabled = topic.isNotBlank(),
                onClick = { onSave(topic.trim()) }
            )
        },
        dismissButton = { DialogTextButton(text = "Отмена", onClick = onDismiss) }
    )
}

@Composable
private fun DialogPrimaryButton(
    text: String,
    enabled: Boolean = true,
    containerColor: Color = AccentBlue,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White,
            disabledContainerColor = HeaderBackground,
            disabledContentColor = PrimaryText.copy(alpha = 0.45f)
        )
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DialogTextButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text, color = AccentBlue, fontWeight = FontWeight.SemiBold)
    }
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

private data class AssessmentEditState(
    val form: JournalGridAssessmentForm?,
    val initialTitle: String,
    val initialType: String,
    val initialDate: String
)

private data class TopicEditState(
    val lesson: JournalGridLesson
)

private fun shareJournalCsv(
    context: android.content.Context,
    journal: JournalGridResponse,
    lessons: List<JournalGridLesson>,
    attendance: List<JournalGridAttendance>,
    assessmentForms: List<JournalGridAssessmentForm>,
    grades: List<JournalGridGrade>,
    showGrades: Boolean
) {
    val csv = buildJournalCsv(
        journal = journal,
        lessons = lessons,
        attendance = attendance,
        assessmentForms = assessmentForms,
        grades = grades,
        showGrades = showGrades
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "Журнал ${journal.group.name} ${journal.discipline.name}")
        putExtra(Intent.EXTRA_TEXT, csv)
    }
    context.startActivity(Intent.createChooser(intent, "Экспорт журнала"))
}

private fun buildJournalCsv(
    journal: JournalGridResponse,
    lessons: List<JournalGridLesson>,
    attendance: List<JournalGridAttendance>,
    assessmentForms: List<JournalGridAssessmentForm>,
    grades: List<JournalGridGrade>,
    showGrades: Boolean
): String {
    val rows = mutableListOf<List<String>>()
    rows += listOf("Группа", journal.group.name)
    rows += listOf("Дисциплина", journal.discipline.name)
    rows += listOf("Период", journal.academicPeriod.name)
    rows.add(emptyList())

    val header = mutableListOf("№ п/п", "Студент")
    lessons.forEach { lesson -> header += "${formatLessonDate(lesson)} ${lessonTypeName(lesson.lessonType)}" }
    if (showGrades) {
        assessmentForms.forEach { form -> header += "${form.title} (${formTypeName(form.type)})" }
    }
    rows += header

    journal.students.forEachIndexed { index, student ->
        val row = mutableListOf((index + 1).toString(), student.fullName)
        lessons.forEach { lesson ->
            val record = attendance.firstOrNull { it.studentId == student.studentId && it.lessonId == lesson.lessonId }
            row += attendanceExportText(record?.status)
        }
        if (showGrades) {
            assessmentForms.forEach { form ->
                val grade = grades.firstOrNull { it.studentId == student.studentId && it.assessmentFormId == form.assessmentFormId }
                row += grade?.value.orEmpty().ifBlank { "—" }
            }
        }
        rows += row
    }

    return rows.joinToString("\n") { row -> row.joinToString(";") { it.csvEscape() } }
}

private fun String.csvEscape(): String = "\"${replace("\"", "\"\"")}\""

private fun String.toUtcStartOfDayMillis(): Long? = runCatching {
    LocalDate.parse(take(10))
        .atStartOfDay()
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli()
}.getOrNull()

private fun Long.toIsoLocalDate(): String = Instant.ofEpochMilli(this)
    .atZone(ZoneOffset.UTC)
    .toLocalDate()
    .toString()

private fun formatLessonDate(lesson: JournalGridLesson): String = runCatching {
    LocalDate.parse(lesson.date.take(10)).format(DateTimeFormatter.ofPattern("dd.MM"))
}.getOrElse { lesson.date }

private fun formatApiDate(date: String): String = date.take(10)

private fun lessonHeaderText(lesson: JournalGridLesson): String {
    val topic = lesson.topic.orEmpty().trim()
    return listOf(formatLessonDate(lesson), lessonTypeName(lesson.lessonType), topic.takeIf { it.isNotBlank() })
        .filterNotNull()
        .joinToString("\n")
}

private fun assessmentHeaderText(form: JournalGridAssessmentForm): String = buildString {
    append(form.title)
    append("\n")
    append(formTypeName(form.type))
    append("\n")
    append(formatApiDate(form.date))
}

private fun attendanceSymbol(status: String?): String = when (status) {
    "present" -> "П"
    "absent" -> "Н"
    "valid_excuse" -> "У"
    null -> ""
    else -> status
}

private fun attendanceExportText(status: String?): String = when (status) {
    "present" -> "Присутствовал"
    "absent" -> "Отсутствовал"
    "valid_excuse" -> "Уважительная причина"
    null -> "—"
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
    "practice" -> "Практика"
    "lab" -> "Лабораторная"
    "seminar" -> "Семинар"
    else -> type
}

private fun formTypeName(type: String): String = when (type) {
    "exam" -> "Экзамен"
    "quiz" -> "Контрольная"
    "homework" -> "ДЗ"
    "project" -> "Проект"
    else -> type
}
