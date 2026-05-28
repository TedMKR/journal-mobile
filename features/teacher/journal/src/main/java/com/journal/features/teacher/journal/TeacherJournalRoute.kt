package com.journal.features.teacher.journal

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.journal.core.ui.AppBackground
import com.journal.core.ui.AppDanger
import com.journal.core.ui.AppHeaderBackground
import com.journal.core.ui.AppPrimary
import com.journal.core.ui.AppSuccess
import com.journal.core.ui.AppWarning
import com.journal.core.ui.StyledDatePickerDialog
import com.journal.core.ui.appFieldColors
import com.journal.core.model.teacher.JournalGridAssessmentForm
import com.journal.core.model.teacher.JournalGridAttendance
import com.journal.core.model.teacher.JournalGridGrade
import com.journal.core.model.teacher.JournalGridLesson
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.JournalGridStudent
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val BackgroundColor = AppBackground
private val PrimaryText = AppPrimary
private val CardBackground = Color.White
private val HeaderBackground = AppHeaderBackground
private val CellBorder = Color(0xFFC9CED8)
private val AccentBlue = AppPrimary
private val PresentColor = AppSuccess
private val AbsentColor = AppDanger
private val ExcuseColor = AppWarning
private val DangerColor = AppDanger
private val DialogContainerColor = Color.White
private val DialogTextColor = Color.Black

private const val ATTENDANCE_COLUMN_WIDTH = 82
private const val GRADE_COLUMN_WIDTH = 112

@Composable
fun TeacherJournalRoute(
    onOpenStudentCard: (String) -> Unit,
    viewModel: TeacherJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(10.dp)
    ) {
        when {
            uiState.isLoading && uiState.journal == null ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            uiState.error != null && uiState.journal == null ->
                Text(
                    text = uiState.error.orEmpty(),
                    color = PrimaryText,
                    modifier = Modifier.align(Alignment.Center)
                )

            uiState.journal != null -> {
                JournalContent(
                    journal = uiState.journal!!,
                    selectedLessonType = viewModel.lessonType,
                    onOpenStudentCard = onOpenStudentCard,
                    onRefresh = { viewModel.loadJournal() },
                    onMarkAttendance = { lessonId, studentId, status, comment ->
                        viewModel.markAttendance(lessonId, studentId, status, comment)
                    },
                    onCreateGrade = { studentId, formId, value, comment ->
                        viewModel.createGrade(studentId, formId, value, comment)
                    },
                    onUpdateGrade = { gradeId, value, comment ->
                        viewModel.updateGrade(gradeId, value, comment)
                    },
                    onCreateAssessmentForm = { title, type, date ->
                        viewModel.createAssessmentForm(title, type, date)
                    },
                    onUpdateAssessmentForm = { formId, title, type, date ->
                        viewModel.updateAssessmentForm(formId, title, type, date)
                    },
                    onDeleteAssessmentForm = { formId ->
                        viewModel.deleteAssessmentForm(formId)
                    },
                    onUpdateLessonTopic = { lessonId, topic ->
                        viewModel.updateLessonTopic(lessonId, topic)
                    }
                )
                // Offline banner
                if (uiState.isOffline) {
                    OfflineBanner(
                        pendingCount = uiState.pendingCount,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun JournalContent(
    journal: JournalGridResponse,
    selectedLessonType: String,
    onOpenStudentCard: (String) -> Unit,
    onRefresh: () -> Unit,
    onMarkAttendance: (lessonId: String, studentId: String, status: String, comment: String?) -> Unit,
    onCreateGrade: (studentId: String, formId: String, value: Int, comment: String?) -> Unit,
    onUpdateGrade: (gradeId: String, value: Int, comment: String?) -> Unit,
    onCreateAssessmentForm: (title: String, type: String, date: String) -> Unit,
    onUpdateAssessmentForm: (formId: String, title: String, type: String, date: String) -> Unit,
    onDeleteAssessmentForm: (formId: String) -> Unit,
    onUpdateLessonTopic: (lessonId: String, topic: String) -> Unit
) {
    val context = LocalContext.current
    var currentType by remember(selectedLessonType) { mutableStateOf(selectedLessonType.ifBlank { journal.lessons.firstOrNull()?.lessonType.orEmpty() }) }
    var assessmentDialog by remember { mutableStateOf<AssessmentEditState?>(null) }
    var deleteAssessmentDialog by remember { mutableStateOf<JournalGridAssessmentForm?>(null) }
    var attendanceDialog by remember { mutableStateOf<AttendanceEditState?>(null) }
    var gradeDialog by remember { mutableStateOf<GradeEditState?>(null) }
    var topicDialog by remember { mutableStateOf<TopicEditState?>(null) }
    val horizontalScroll = rememberScrollState()

    val availableTypes = journal.lessons.map { it.lessonType }.distinct().ifEmpty { listOf(currentType) }.filter { it.isNotBlank() }
    val filteredLessons = journal.lessons
        .filter { it.lessonType == currentType }
        .sortedBy { it.scheduledAt }
    val visibleAttendance = journal.attendance.filter { attendance -> filteredLessons.any { it.lessonId == attendance.lessonId } }
    val sortedAssessmentForms = journal.assessmentForms.sortedBy { it.date }
    val showGrades = currentType != "lecture"
    val visibleForms = if (showGrades) sortedAssessmentForms else emptyList()
    val canEditAttendance = journal.permissions.canEditAttendance && !journal.academicPeriod.isClosed
    val canEditGrades = journal.permissions.canEditGrades && !journal.academicPeriod.isClosed
    val canEditAssessments = showGrades && canEditGrades

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item { JournalHeader(journal = journal, currentType = currentType) }
        item { LessonTypeTabs(types = availableTypes, selectedType = currentType, onSelect = { currentType = it }) }
        item {
            JournalActions(
                canEditAssessments = canEditAssessments,
                onExport = {
                    shareJournalXlsx(
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
        }
        item { JournalTableTop() }
        stickyHeader {
            JournalStickyTableHeader(
                horizontalScroll = horizontalScroll,
                lessons = filteredLessons,
                assessmentForms = visibleForms,
                canEditGrades = canEditGrades,
                onLessonClick = { lesson -> topicDialog = TopicEditState(lesson) },
                onEditAssessment = { form ->
                    assessmentDialog = AssessmentEditState(
                        form = form,
                        initialTitle = form.title,
                        initialType = form.type,
                        initialDate = formatApiDate(form.date)
                    )
                }
            )
        }
        itemsIndexed(journal.students) { index, student ->
            val studentAttendance = visibleAttendance.filter { it.studentId == student.studentId }
            val studentGrades = journal.grades.filter { it.studentId == student.studentId }
            JournalTableStudentItem(
                horizontalScroll = horizontalScroll,
                index = index + 1,
                student = student,
                lessons = filteredLessons,
                attendance = studentAttendance,
                assessmentForms = visibleForms,
                grades = studentGrades,
                canEditAttendance = canEditAttendance,
                canEditGrades = canEditGrades,
                onOpenStudentCard = onOpenStudentCard,
                onAttendanceClick = { lesson, record ->
                    if (canEditAttendance) attendanceDialog = AttendanceEditState(student, lesson, record)
                },
                onGradeClick = { form, grade ->
                    if (canEditGrades) gradeDialog = GradeEditState(student, form, grade)
                }
            )
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground, RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                HorizontalScrollIndicator(
                    scrollValue = horizontalScroll.value,
                    maxValue = horizontalScroll.maxValue
                )
            }
        }
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
                if (state.form == null) {
                    onCreateAssessmentForm(title, type, date)
                } else {
                    onUpdateAssessmentForm(state.form.assessmentFormId, title, type, date)
                }
                assessmentDialog = null
                onRefresh()
            }
        )
    }

    deleteAssessmentDialog?.let { form ->
        ConfirmDeleteAssessmentDialog(
            form = form,
            onDismiss = { deleteAssessmentDialog = null },
            onConfirm = {
                onDeleteAssessmentForm(form.assessmentFormId)
                deleteAssessmentDialog = null
                onRefresh()
            }
        )
    }

    attendanceDialog?.let { state ->
        AttendanceDialog(
            state = state,
            onDismiss = { attendanceDialog = null },
            onSave = { status, comment ->
                onMarkAttendance(
                    state.lesson.lessonId,
                    state.student.studentId,
                    status,
                    comment.takeIf { it.isNotBlank() }
                )
                attendanceDialog = null
                onRefresh()
            }
        )
    }

    gradeDialog?.let { state ->
        GradeDialog(
            state = state,
            onDismiss = { gradeDialog = null },
            onSave = { value, comment ->
                if (state.grade == null) {
                    onCreateGrade(
                        state.student.studentId,
                        state.form.assessmentFormId,
                        value,
                        comment.takeIf { it.isNotBlank() }
                    )
                } else {
                    onUpdateGrade(
                        state.grade.gradeId,
                        value,
                        comment.takeIf { it.isNotBlank() }
                    )
                }
                gradeDialog = null
                onRefresh()
            }
        )
    }

    topicDialog?.let { state ->
        TopicDialog(
            state = state,
            onDismiss = { topicDialog = null },
            onSave = { topic ->
                onUpdateLessonTopic(state.lesson.lessonId, topic)
                topicDialog = null
                onRefresh()
            }
        )
    }
}

@Composable
private fun OfflineBanner(pendingCount: Int, modifier: Modifier = Modifier) {
    val text = if (pendingCount > 0) {
        "Офлайн — $pendingCount действий в очереди"
    } else {
        "Офлайн — данные из кэша"
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF59E0B), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
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
private fun JournalTableTop() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Text("Таблица журнала", color = PrimaryText, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun JournalStickyTableHeader(
    horizontalScroll: ScrollState,
    lessons: List<JournalGridLesson>,
    assessmentForms: List<JournalGridAssessmentForm>,
    canEditGrades: Boolean,
    onLessonClick: (JournalGridLesson) -> Unit,
    onEditAssessment: (JournalGridAssessmentForm) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground)
            .padding(horizontal = 8.dp)
    ) {
        Box(modifier = Modifier.horizontalScroll(horizontalScroll)) {
            JournalTableHeader(
                lessons = lessons,
                assessmentForms = assessmentForms,
                canEditGrades = canEditGrades,
                onLessonClick = onLessonClick,
                onEditAssessment = onEditAssessment
            )
        }
    }
}

@Composable
private fun JournalTableStudentItem(
    horizontalScroll: ScrollState,
    index: Int,
    student: JournalGridStudent,
    lessons: List<JournalGridLesson>,
    attendance: List<JournalGridAttendance>,
    assessmentForms: List<JournalGridAssessmentForm>,
    grades: List<JournalGridGrade>,
    canEditAttendance: Boolean,
    canEditGrades: Boolean,
    onOpenStudentCard: (String) -> Unit,
    onAttendanceClick: (JournalGridLesson, JournalGridAttendance?) -> Unit,
    onGradeClick: (JournalGridAssessmentForm, JournalGridGrade?) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground)
            .padding(horizontal = 8.dp)
    ) {
        Box(modifier = Modifier.horizontalScroll(horizontalScroll)) {
            JournalStudentRow(
                index = index,
                student = student,
                lessons = lessons,
                attendance = attendance,
                assessmentForms = assessmentForms,
                grades = grades,
                canEditAttendance = canEditAttendance,
                canEditGrades = canEditGrades,
                onOpenStudentCard = onOpenStudentCard,
                onAttendanceClick = onAttendanceClick,
                onGradeClick = onGradeClick
            )
        }
    }
}

@Composable
private fun JournalTableHeader(
    lessons: List<JournalGridLesson>,
    assessmentForms: List<JournalGridAssessmentForm>,
    canEditGrades: Boolean,
    onLessonClick: (JournalGridLesson) -> Unit,
    onEditAssessment: (JournalGridAssessmentForm) -> Unit
) {
    Row {
        TableCell("№", 42, isHeader = true)
        TableCell("Студент", 190, isHeader = true, textAlign = TextAlign.Start)
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
private fun JournalStudentRow(
    index: Int,
    student: JournalGridStudent,
    lessons: List<JournalGridLesson>,
    attendance: List<JournalGridAttendance>,
    assessmentForms: List<JournalGridAssessmentForm>,
    grades: List<JournalGridGrade>,
    canEditAttendance: Boolean,
    canEditGrades: Boolean,
    onOpenStudentCard: (String) -> Unit,
    onAttendanceClick: (JournalGridLesson, JournalGridAttendance?) -> Unit,
    onGradeClick: (JournalGridAssessmentForm, JournalGridGrade?) -> Unit
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
        containerColor = DialogContainerColor,
        titleContentColor = DialogTextColor,
        textContentColor = DialogTextColor,
        title = { Text("Посещаемость") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.student.fullName, color = DialogTextColor)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("present", "absent", "valid_excuse").forEach { status ->
                        StatusChip(
                            text = attendanceSymbol(status),
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status }
                        )
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий") },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    colors = appFieldColors()
                )
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
        containerColor = DialogContainerColor,
        titleContentColor = DialogTextColor,
        textContentColor = DialogTextColor,
        title = { Text(state.form.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.student.fullName, color = DialogTextColor)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (2..5).forEach { value ->
                        StatusChip(
                            text = value.toString(),
                            selected = selectedValue == value,
                            onClick = { selectedValue = value }
                        )
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий") },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    colors = appFieldColors()
                )
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
        StyledDatePickerDialog(
            state = datePickerState,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                datePickerState.selectedDateMillis?.let { selectedMillis ->
                    date = selectedMillis.toIsoLocalDate()
                }
                showDatePicker = false
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (state.form == null) "Добавить контроль" else "Редактировать контроль",
                color = PrimaryText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            WebFormField(label = "Название работы") {
                WebOutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Например: Лабораторная работа №1"
                )
            }

            WebFormField(label = "Тип контроля") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("exam", "quiz", "homework", "project").forEach { option ->
                        WebSelectOption(
                            text = formTypeName(option),
                            selected = type == option,
                            onClick = { type = option }
                        )
                    }
                }
            }

            WebFormField(label = "Дата проведения") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WebOutlinedTextField(
                        value = date,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = "YYYY-MM-DD",
                        modifier = Modifier.weight(1f)
                    )
                    WebSecondaryButton(text = "Выбрать", onClick = { showDatePicker = true })
                }
            }

            state.form?.let { form ->
                TextButton(onClick = { onDelete(form) }) {
                    Text("Удалить контроль", color = DangerColor, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                WebSecondaryButton(text = "Отмена", onClick = onDismiss)
                WebPrimaryButton(
                    text = "Сохранить",
                    enabled = title.isNotBlank() && date.isNotBlank(),
                    onClick = { onSave(title.trim(), type, date.trim()) }
                )
            }
        }
    }
}

@Composable
private fun WebFormField(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = PrimaryText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
private fun WebOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        singleLine = true,
        placeholder = { Text(placeholder, color = Color(0xFF9CA3AF)) },
        textStyle = LocalTextStyle.current.copy(color = Color.Black),
        colors = appFieldColors(),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun WebSelectOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Color(0xFFE8ECF8) else Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, if (selected) AccentBlue else Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = PrimaryText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun WebPrimaryButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentBlue,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFCBD5E1),
            disabledContentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun WebSecondaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE5E7EB),
            contentColor = Color(0xFF374151)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun ConfirmDeleteAssessmentDialog(
    form: JournalGridAssessmentForm,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogContainerColor,
        titleContentColor = DialogTextColor,
        textContentColor = DialogTextColor,
        title = { Text("Удаление контрольного мероприятия") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Удалить контроль: ${form.title}?", color = DialogTextColor)
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
        containerColor = DialogContainerColor,
        titleContentColor = DialogTextColor,
        textContentColor = DialogTextColor,
        title = { Text("Тема занятия") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(formatLessonDate(state.lesson), color = DialogTextColor)
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Тема / комментарий к занятию") },
                    minLines = 3,
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    colors = appFieldColors()
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

private fun shareJournalXlsx(
    context: android.content.Context,
    journal: JournalGridResponse,
    lessons: List<JournalGridLesson>,
    attendance: List<JournalGridAttendance>,
    assessmentForms: List<JournalGridAssessmentForm>,
    grades: List<JournalGridGrade>,
    showGrades: Boolean
) {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val exportFile = File(exportDir, "${journal.safeExportName()}.xlsx")
    exportFile.outputStream().use { output ->
        writeJournalXlsx(
            output = output,
            rows = buildJournalExportRows(
                journal = journal,
                lessons = lessons,
                attendance = attendance,
                assessmentForms = assessmentForms,
                grades = grades,
                showGrades = showGrades
            )
        )
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        exportFile
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = XLSX_MIME_TYPE
        putExtra(Intent.EXTRA_SUBJECT, "Журнал ${journal.group.name} ${journal.discipline.name}")
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Экспорт журнала"))
}

private fun buildJournalExportRows(
    journal: JournalGridResponse,
    lessons: List<JournalGridLesson>,
    attendance: List<JournalGridAttendance>,
    assessmentForms: List<JournalGridAssessmentForm>,
    grades: List<JournalGridGrade>,
    showGrades: Boolean
): List<List<String>> {
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

    return rows
}

private fun writeJournalXlsx(output: OutputStream, rows: List<List<String>>) {
    ZipOutputStream(output).use { zip ->
        zip.writeEntry("[Content_Types].xml", xlsxContentTypes())
        zip.writeEntry("_rels/.rels", xlsxRootRels())
        zip.writeEntry("xl/_rels/workbook.xml.rels", xlsxWorkbookRels())
        zip.writeEntry("xl/workbook.xml", xlsxWorkbook())
        zip.writeEntry("xl/styles.xml", xlsxStyles())
        zip.writeEntry("xl/worksheets/sheet1.xml", xlsxSheet(rows))
    }
}

private fun ZipOutputStream.writeEntry(name: String, content: String) {
    putNextEntry(ZipEntry(name))
    write(content.toByteArray(Charsets.UTF_8))
    closeEntry()
}

private fun xlsxSheet(rows: List<List<String>>): String = buildString {
    append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
    append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
    append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"5\" topLeftCell=\"A6\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
    append("<sheetFormatPr defaultRowHeight=\"18\"/>")
    append("<cols><col min=\"1\" max=\"1\" width=\"8\" customWidth=\"1\"/><col min=\"2\" max=\"2\" width=\"32\" customWidth=\"1\"/><col min=\"3\" max=\"80\" width=\"18\" customWidth=\"1\"/></cols>")
    append("<sheetData>")
    rows.forEachIndexed { rowIndex, row ->
        val excelRow = rowIndex + 1
        append("<row r=\"").append(excelRow).append("\">")
        row.forEachIndexed { columnIndex, value ->
            val cell = "${columnName(columnIndex + 1)}$excelRow"
            val style = when {
                rowIndex < 3 -> 2
                rowIndex == 4 -> 1
                else -> 0
            }
            append("<c r=\"").append(cell).append("\" t=\"inlineStr\" s=\"").append(style).append("\"><is><t>")
            append(value.xmlEscape())
            append("</t></is></c>")
        }
        append("</row>")
    }
    append("</sheetData>")
    append("<pageMargins left=\"0.7\" right=\"0.7\" top=\"0.75\" bottom=\"0.75\" header=\"0.3\" footer=\"0.3\"/>")
    append("</worksheet>")
}

private fun xlsxContentTypes(): String = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
        <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
        <Default Extension="xml" ContentType="application/xml"/>
        <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
        <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
        <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
    </Types>
""".trimIndent()

private fun xlsxRootRels(): String = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
    </Relationships>
""".trimIndent()

private fun xlsxWorkbookRels(): String = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
        <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
    </Relationships>
""".trimIndent()

private fun xlsxWorkbook(): String = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
        <sheets><sheet name="Журнал" sheetId="1" r:id="rId1"/></sheets>
    </workbook>
""".trimIndent()

private fun xlsxStyles(): String = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
        <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
        <fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FFD3D7E1"/><bgColor indexed="64"/></patternFill></fill></fills>
        <borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border><border><left style="thin"/><right style="thin"/><top style="thin"/><bottom style="thin"/><diagonal/></border></borders>
        <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
        <cellXfs count="3"><xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs>
        <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
    </styleSheet>
""".trimIndent()

private fun columnName(index: Int): String {
    var value = index
    val result = StringBuilder()
    while (value > 0) {
        value--
        result.insert(0, ('A'.code + value % 26).toChar())
        value /= 26
    }
    return result.toString()
}

private fun JournalGridResponse.safeExportName(): String = listOf("journal", group.name, discipline.name)
    .joinToString("_")
    .replace(Regex("[^A-Za-zА-Яа-я0-9_-]+"), "_")
    .trim('_')
    .ifBlank { "journal" }

private fun String.xmlEscape(): String = buildString {
    this@xmlEscape.forEach { char ->
        append(
            when (char) {
                '<' -> "&lt;"
                '>' -> "&gt;"
                '&' -> "&amp;"
                '"' -> "&quot;"
                '\'' -> "&apos;"
                else -> char.toString()
            }
        )
    }
}

private const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

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
