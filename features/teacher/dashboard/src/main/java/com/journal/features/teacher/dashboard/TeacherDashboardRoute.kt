package com.journal.features.teacher.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import com.journal.core.model.teacher.GrantJournalAccessRequest
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.TeacherLesson
import com.journal.core.model.teacher.TeacherProfile
import com.journal.core.network.api.JournalApi
import kotlinx.coroutines.launch
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
        AnalyticsCard(
            state = state,
            journalApi = state.journalApi,
            onOpenJournal = onOpenJournal
        )
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
    journalApi: JournalApi,
    onOpenJournal: (TeacherDashboardJournalTarget) -> Unit
) {
    var selectedType by remember { mutableStateOf("practice") }
    var selectedDisciplineId by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf("") }
    var appliedTarget by remember { mutableStateOf<TeacherDashboardJournalTarget?>(null) }
    var showAccessDialog by remember { mutableStateOf(false) }

    val selectedTarget = appliedTarget ?: state.defaultJournalTarget
    val canApply = selectedDisciplineId.isNotBlank() && selectedGroupId.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Анализ", color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        AnalysisSelect(
            label = "Тип занятий",
            selectedText = lessonTypeName(selectedType),
            options = lessonTypeOptions,
            onSelect = { selectedType = it }
        )
        AnalysisSelect(
            label = "Предмет",
            selectedText = state.disciplines.firstOrNull { it.id == selectedDisciplineId }?.name ?: "Выберите предмет",
            options = state.disciplines.map { it.id to it.name },
            onSelect = { selectedDisciplineId = it }
        )
        AnalysisSelect(
            label = "Группа",
            selectedText = state.groups.firstOrNull { it.id == selectedGroupId }?.name ?: "Выберите группу",
            options = state.groups.map { it.id to it.name },
            onSelect = { selectedGroupId = it }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    val periodId = state.defaultJournalTarget?.periodId.orEmpty()
                    appliedTarget = TeacherDashboardJournalTarget(
                        groupId = selectedGroupId,
                        disciplineId = selectedDisciplineId,
                        periodId = periodId,
                        teacherId = null,
                        lessonType = selectedType
                    )
                },
                enabled = canApply,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBackground,
                    contentColor = Color.White,
                    disabledContainerColor = AccentBackground,
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Применить")
            }
            Button(
                onClick = { showAccessDialog = true },
                enabled = selectedTarget != null,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryText, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Открыть доступ")
            }
        }

        if (canApply && selectedTarget != null) {
            AttendanceLineChart(state.attendanceByMonth)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ActionTile("Студентов\nв группе", state.selectedStudentsCount.toString(), Modifier.weight(1f)) {
                    onOpenJournal(selectedTarget)
                }
                ActionTile("Открыть\nжурнал", "→", Modifier.weight(1f)) {
                    onOpenJournal(selectedTarget)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LessonBackground, RoundedCornerShape(14.dp))
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Выберите предмет и группу для просмотра аналитики", color = SecondaryText)
            }
        }
    }

    if (showAccessDialog) {
        AccessGrantDialog(
            journalApi = journalApi,
            target = selectedTarget,
            onDismiss = { showAccessDialog = false }
        )
    }
}

@Composable
private fun AnalysisSelect(
    label: String,
    selectedText: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = PrimaryText, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, PrimaryText, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedText, color = PrimaryText, modifier = Modifier.weight(1f))
                Image(
                    painter = painterResource(id = R.drawable.arrow_bottom),
                    contentDescription = null,
                    modifier = Modifier.size(width = 13.dp, height = 9.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                options.forEach { (value, title) ->
                    DropdownMenuItem(
                        text = { Text(title, color = PrimaryText) },
                        onClick = {
                            onSelect(value)
                            expanded = false
                        }
                    )
                }
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
private fun AccessGrantDialog(
    journalApi: JournalApi,
    target: TeacherDashboardJournalTarget?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var teachers by remember { mutableStateOf<List<TeacherProfile>>(emptyList()) }
    var selectedTeacher by remember { mutableStateOf<TeacherProfile?>(null) }
    var accessLevel by remember { mutableStateOf("read") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { journalApi.getTeachers(limit = 200).data }
            .onSuccess { loaded ->
                teachers = loaded.filter { !it.keycloakId.isNullOrBlank() }
                isLoading = false
            }
            .onFailure { throwable ->
                message = throwable.message ?: "Не удалось загрузить преподавателей"
                isLoading = false
            }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(18.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Открыть доступ", color = PrimaryText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Выберите преподавателя и уровень доступа к текущему журналу.", color = SecondaryText, style = MaterialTheme.typography.bodySmall)

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Преподаватель", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    teachers.take(6).forEach { teacher ->
                        SelectRow(
                            text = teacher.fullName,
                            selected = selectedTeacher?.id == teacher.id,
                            onClick = { selectedTeacher = teacher }
                        )
                    }
                }

                Text("Уровень доступа", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectRow(
                        text = "Чтение",
                        selected = accessLevel == "read",
                        modifier = Modifier.weight(1f),
                        onClick = { accessLevel = "read" }
                    )
                    SelectRow(
                        text = "Запись",
                        selected = accessLevel == "write",
                        modifier = Modifier.weight(1f),
                        onClick = { accessLevel = "write" }
                    )
                }
            }

            message?.let { Text(it, color = PrimaryText, style = MaterialTheme.typography.bodySmall) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Отмена",
                    color = SecondaryText,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(12.dp)
                )
                Button(
                    onClick = {
                        val granteeId = selectedTeacher?.keycloakId
                        if (target == null || granteeId.isNullOrBlank()) {
                            message = "Выберите преподавателя"
                            return@Button
                        }
                        scope.launch {
                            isSaving = true
                            runCatching {
                                journalApi.grantJournalAccess(
                                    GrantJournalAccessRequest(
                                        granteeId = granteeId,
                                        disciplineId = target.disciplineId,
                                        groupId = target.groupId,
                                        periodId = target.periodId,
                                        accessLevel = accessLevel
                                    )
                                )
                            }.onSuccess {
                                isSaving = false
                                onDismiss()
                            }.onFailure { throwable ->
                                message = throwable.message ?: "Не удалось открыть доступ"
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryText, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isSaving) "Сохранение..." else "Сохранить")
                }
            }
        }
    }
}

@Composable
private fun SelectRow(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = PrimaryText,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .background(if (selected) AccentBackground else BackgroundColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    )
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
    val uniqueDisciplines = lessons.mapNotNull { lesson -> lesson.disciplineId?.let { it to lesson.disciplineName } }.distinctBy { it.first }

    return TeacherDashboardUiState(
        teacherName = defaultJournal?.teacher?.fullName ?: defaultLesson?.teacherName,
        totalStudents = defaultJournal?.students?.size ?: 0,
        totalDisciplines = uniqueDisciplines.size,
        hoursInSchedule = formatHours(lessons.sumOf { lessonDurationMinutes(it) }),
        avgGrade = averageGrade(defaultJournal),
        todayLessons = todayLessons(lessons),
        analyticsTitle = defaultJournal?.let { "${it.discipline.name} · ${it.group.name}" } ?: "Нет выбранного журнала",
        selectedStudentsCount = defaultJournal?.students?.size ?: 0,
        attendanceByMonth = attendanceByMonth(defaultJournal),
        disciplines = lessons.mapNotNull { lesson -> lesson.disciplineId?.let { it to lesson.disciplineName } }
            .distinctBy { it.first }
            .map { SelectOption(it.first, it.second) },
        groups = lessons.mapNotNull { lesson -> lesson.groupId?.let { it to lesson.groupName } }
            .distinctBy { it.first }
            .map { SelectOption(it.first, it.second) },
        journalApi = journalApi,
        defaultJournalTarget = defaultLesson?.let {
            TeacherDashboardJournalTarget(
                groupId = it.groupId.orEmpty(),
                disciplineId = it.disciplineId.orEmpty(),
                periodId = it.periodId.orEmpty(),
                teacherId = it.teacherId,
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

private val lessonTypeOptions = listOf(
    "practice" to "Практические занятия",
    "lecture" to "Лекции",
    "lab" to "Лабораторные работы",
    "seminar" to "Семинары"
)

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
    val disciplines: List<SelectOption>,
    val groups: List<SelectOption>,
    val journalApi: JournalApi,
    val defaultJournalTarget: TeacherDashboardJournalTarget?
)

data class TeacherDashboardJournalTarget(
    val groupId: String,
    val disciplineId: String,
    val periodId: String,
    val teacherId: String?,
    val lessonType: String
)

private data class AttendanceMonth(
    val month: String,
    val percent: Int
)

private data class SelectOption(
    val id: String,
    val name: String
)

