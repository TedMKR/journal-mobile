package com.journal.features.teacher.ved

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.journal.core.ui.AppBackground
import com.journal.core.ui.AppLessonBackground
import com.journal.core.ui.AppPrimary
import com.journal.core.ui.AppSecondaryText
import com.journal.core.ui.StyledDatePickerDialog
import com.journal.core.ui.appFieldColors
import com.journal.core.model.teacher.AcademicGroup
import com.journal.core.model.teacher.AcademicPeriod
import com.journal.core.model.teacher.CurrentAttestationOptions
import com.journal.core.model.teacher.CurrentAttestationOverrides
import com.journal.core.model.teacher.CurrentAttestationContext
import com.journal.core.model.teacher.CurrentAttestationPrefill
import com.journal.core.model.teacher.Discipline
import com.journal.core.model.teacher.TeacherLesson
import com.journal.core.model.teacher.RequestReportPayload
import com.journal.core.network.api.JournalApi
import java.io.File
import java.io.IOException
import java.util.UUID
import retrofit2.HttpException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BackgroundColor = AppBackground
private val PrimaryText = AppPrimary
private val SecondaryText = AppSecondaryText
private val CardBackground = Color.White
private val LightBlue = AppLessonBackground
private val Danger = Color(0xFFB42318)
private val Success = Color(0xFF027A48)

@Composable
fun TeacherVedRoute(journalApi: JournalApi) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var periods by remember { mutableStateOf<List<AcademicPeriod>>(emptyList()) }
    var teacherLessons by remember { mutableStateOf<List<TeacherLesson>>(emptyList()) }
    var disciplines by remember { mutableStateOf<List<Discipline>>(emptyList()) }
    var groups by remember { mutableStateOf<List<AcademicGroup>>(emptyList()) }
    var selectedPeriodId by remember { mutableStateOf("") }
    var selectedDisciplineId by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf("") }
    var prefill by remember { mutableStateOf<CurrentAttestationPrefill?>(null) }
    var selectedFormat by remember { mutableStateOf("docx") }
    var returnToDeanBy by remember { mutableStateOf("") }
    var progressAsOf by remember { mutableStateOf(LocalDate.now().toString()) }
    var overrides by remember { mutableStateOf(CurrentAttestationOverrides()) }
    var options by remember { mutableStateOf(CurrentAttestationOptions()) }
    var readyStatements by remember { mutableStateOf<List<ReadyStatement>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingPrefill by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    // Idempotency key for async document creation. Preserved across network-error retries;
    // cleared after a successful job enqueue, on 409 IDEMPOTENCY_CONFLICT, and whenever
    // the user changes prefill context (new group / discipline / period).
    var pendingIdempotencyKey by remember { mutableStateOf<String?>(null) }

    fun syncCatalogs() {
        val disciplineLessons = selectedGroupId.takeIf { it.isNotBlank() }?.let { groupId ->
            teacherLessons.filter { it.groupId == groupId }
        } ?: teacherLessons
        val groupLessons = selectedDisciplineId.takeIf { it.isNotBlank() }?.let { disciplineId ->
            teacherLessons.filter { it.disciplineId == disciplineId }
        } ?: teacherLessons

        disciplines = disciplineLessons
            .filter { it.disciplineId != null }
            .map { Discipline(id = it.disciplineId!!, name = it.disciplineName) }
            .distinctBy { it.id }
        groups = groupLessons
            .filter { it.groupId != null }
            .map { AcademicGroup(id = it.groupId!!, name = it.groupName) }
            .distinctBy { it.id }

        if (selectedDisciplineId.isNotBlank() && disciplines.none { it.id == selectedDisciplineId }) selectedDisciplineId = ""
        if (selectedGroupId.isNotBlank() && groups.none { it.id == selectedGroupId }) selectedGroupId = ""
    }

    fun resetPrefill() {
        prefill = null
        message = null
        error = null
        pendingIdempotencyKey = null
    }

    LaunchedEffect(Unit) {
        runCatching {
            val loadedPeriods = journalApi.getAcademicPeriods(includeClosed = true).data
            periods = loadedPeriods
            selectedPeriodId = loadedPeriods.firstOrNull { it.isActive }?.id ?: loadedPeriods.firstOrNull()?.id.orEmpty()
        }.onFailure { throwable ->
            error = throwable.message ?: "Не удалось загрузить периоды"
        }
    }

    LaunchedEffect(selectedPeriodId) {
        if (selectedPeriodId.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        resetPrefill()
        runCatching {
            loadLessonsForPeriod(journalApi, selectedPeriodId, periods)
        }.onSuccess { lessons ->
            teacherLessons = lessons
            syncCatalogs()
        }.onFailure { throwable ->
            teacherLessons = emptyList()
            disciplines = emptyList()
            groups = emptyList()
            error = throwable.message?.takeIf { it.isNotBlank() }
                ?: "Не удалось загрузить группы и предметы преподавателя"
        }
        isLoading = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TemplateCard()
        }
        item {
            StatementFormCard(
                periods = periods,
                disciplines = disciplines,
                groups = groups,
                selectedPeriodId = selectedPeriodId,
                selectedDisciplineId = selectedDisciplineId,
                selectedGroupId = selectedGroupId,
                prefill = prefill,
                selectedFormat = selectedFormat,
                returnToDeanBy = returnToDeanBy,
                progressAsOf = progressAsOf,
                overrides = overrides,
                options = options,
                isLoading = isLoading,
                isLoadingPrefill = isLoadingPrefill,
                isGenerating = isGenerating,
                message = message,
                error = error,
                onPeriodChange = { value ->
                    selectedPeriodId = value
                    selectedDisciplineId = ""
                    selectedGroupId = ""
                    resetPrefill()
                },
                onDisciplineChange = { value ->
                    selectedDisciplineId = value
                    resetPrefill()
                    syncCatalogs()
                },
                onGroupChange = { value ->
                    selectedGroupId = value
                    resetPrefill()
                    syncCatalogs()
                },
                onFormatChange = { selectedFormat = it },
                onReturnToDeanByChange = { returnToDeanBy = it },
                onProgressAsOfChange = { progressAsOf = it },
                onOverridesChange = { overrides = it },
                onOptionsChange = { options = it },
                onLoadPrefill = {
                    scope.launch {
                        isLoadingPrefill = true
                        error = null
                        message = null
                        runCatching {
                            journalApi.getCurrentAttestationPrefill(
                                groupId = selectedGroupId,
                                disciplineId = selectedDisciplineId,
                                academicPeriodId = selectedPeriodId
                            )
                        }.onSuccess { loaded ->
                            prefill = loaded
                            selectedFormat = loaded.defaults.format
                            overrides = loaded.defaults.overrides
                            options = loaded.defaults.options
                            message = "Данные ведомости заполнены"
                        }.onFailure { throwable ->
                            error = throwable.message ?: "Не удалось загрузить данные для ведомости"
                        }
                        isLoadingPrefill = false
                    }
                },
                onGenerate = {
                    scope.launch {
                        val data = prefill ?: return@launch
                        // Reuse pending key on network-error retry; generate fresh key for new attempt.
                        val idempotencyKey = pendingIdempotencyKey ?: UUID.randomUUID().toString()
                        pendingIdempotencyKey = idempotencyKey
                        isGenerating = true
                        error = null
                        message = null
                        runCatching {
                            val accepted = requestStatementReport(
                                journalApi = journalApi,
                                idempotencyKey = idempotencyKey,
                                context = data.context,
                                format = selectedFormat,
                                returnToDeanBy = returnToDeanBy,
                                progressAsOf = progressAsOf,
                                overrides = overrides,
                                options = options
                            )
                            val jobId = accepted.jobId ?: accepted.id ?: throw IllegalStateException("Сервер не вернул идентификатор задачи")
                            ReadyStatement(
                                jobId = jobId,
                                title = "Ведомость текущего контроля успеваемости",
                                contextLabel = "${data.context.disciplineName} · ${data.context.groupName}",
                                format = selectedFormat,
                                status = accepted.status ?: "pending"
                            )
                        }.onSuccess { statement ->
                            // Job successfully enqueued — key no longer needed.
                            pendingIdempotencyKey = null
                            readyStatements = listOf(statement) + readyStatements.filter { it.jobId != statement.jobId }
                            message = "Ведомость отправлена на формирование"
                            waitForStatement(journalApi, statement) { updated ->
                                readyStatements = readyStatements.map { if (it.jobId == statement.jobId) updated else it }
                                if (updated.status == "done") message = "Ведомость готова к скачиванию"
                                if (updated.status == "failed" || updated.status == "permanently_failed") error = updated.error ?: "Формирование ведомости завершилось ошибкой"
                            }
                        }.onFailure { throwable ->
                            if (throwable.isIdempotencyConflict()) {
                                // Key was already used with different data — reset so next click starts fresh.
                                pendingIdempotencyKey = null
                                error = "Ключ идемпотентности уже использован с другими данными. Пожалуйста, попробуйте ещё раз."
                            } else if (throwable is IOException) {
                                // Network error — preserve pendingIdempotencyKey so the next click retries safely.
                                error = "Ошибка сети. Нажмите «Сформировать» ещё раз — запрос будет повторён безопасно."
                            } else {
                                // Non-retryable server error — reset key.
                                pendingIdempotencyKey = null
                                error = throwable.httpErrorMessage().ifBlank { "Не удалось сформировать ведомость" }
                            }
                        }
                        isGenerating = false
                    }
                }
            )
        }
        item {
            ReadyStatementsCard(
                statements = readyStatements,
                onRefresh = {
                    scope.launch {
                        readyStatements.forEach { statement ->
                            runCatching { journalApi.getReportStatus(statement.jobId) }.onSuccess { task ->
                                readyStatements = readyStatements.map {
                                    if (it.jobId == statement.jobId) {
                                        it.copy(status = task.status, fileName = task.result?.fileName, error = task.errorMessage)
                                    } else {
                                        it
                                    }
                                }
                            }
                        }
                    }
                },
                onDownload = { statement ->
                    scope.launch {
                        error = null
                        message = null
                        runCatching {
                            val body = journalApi.downloadReportFile(statement.jobId)
                            shareReportFile(
                                context = context,
                                bytes = body.bytes(),
                                fileName = statement.fileName ?: "statement-${statement.jobId}.${statement.format}",
                                format = statement.format
                            )
                        }.onSuccess {
                            message = "Файл ведомости открыт"
                        }.onFailure { throwable ->
                            error = throwable.message ?: "Не удалось скачать ведомость"
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun TemplateCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Шаблоны", color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryText, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text("Ведомость текущего контроля успеваемости", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatementFormCard(
    periods: List<AcademicPeriod>,
    disciplines: List<Discipline>,
    groups: List<AcademicGroup>,
    selectedPeriodId: String,
    selectedDisciplineId: String,
    selectedGroupId: String,
    prefill: CurrentAttestationPrefill?,
    selectedFormat: String,
    returnToDeanBy: String,
    progressAsOf: String,
    overrides: CurrentAttestationOverrides,
    options: CurrentAttestationOptions,
    isLoading: Boolean,
    isLoadingPrefill: Boolean,
    isGenerating: Boolean,
    message: String?,
    error: String?,
    onPeriodChange: (String) -> Unit,
    onDisciplineChange: (String) -> Unit,
    onGroupChange: (String) -> Unit,
    onFormatChange: (String) -> Unit,
    onReturnToDeanByChange: (String) -> Unit,
    onProgressAsOfChange: (String) -> Unit,
    onOverridesChange: (CurrentAttestationOverrides) -> Unit,
    onOptionsChange: (CurrentAttestationOptions) -> Unit,
    onLoadPrefill: () -> Unit,
    onGenerate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Сформировать ведомость", color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(prefill?.let { "${it.students.size} студентов" } ?: "Данные не загружены", color = SecondaryText)
            }
            message?.let { Text(it, color = Success) }
            error?.let { Text(it, color = Danger) }
            if (isLoading) {
                CircularProgressIndicator(color = PrimaryText)
            }
            SelectField(
                label = "Предмет",
                value = selectedDisciplineId,
                options = disciplines.map { it.id to it.name },
                placeholder = "Выберите предмет",
                onValueChange = onDisciplineChange
            )
            SelectField(
                label = "Группа",
                value = selectedGroupId,
                options = groups.map { it.id to it.name },
                placeholder = "Выберите группу",
                onValueChange = onGroupChange
            )
            SelectField(
                label = "Период",
                value = selectedPeriodId,
                options = periods.map { it.id to it.name + if (it.isActive) " · активный" else "" },
                placeholder = "Выберите период",
                onValueChange = onPeriodChange
            )
            Button(
                onClick = onLoadPrefill,
                enabled = selectedDisciplineId.isNotBlank() && selectedGroupId.isNotBlank() && selectedPeriodId.isNotBlank() && !isLoadingPrefill,
                colors = ButtonDefaults.buttonColors(containerColor = LightBlue, contentColor = PrimaryText),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    if (isLoadingPrefill) "Загружаю..." else "Заполнить данные",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
            prefill?.let { data ->
                StatementDetails(
                    prefill = data,
                    selectedFormat = selectedFormat,
                    returnToDeanBy = returnToDeanBy,
                    progressAsOf = progressAsOf,
                    overrides = overrides,
                    options = options,
                    onFormatChange = onFormatChange,
                    onReturnToDeanByChange = onReturnToDeanByChange,
                    onProgressAsOfChange = onProgressAsOfChange,
                    onOverridesChange = onOverridesChange,
                    onOptionsChange = onOptionsChange
                )
                Button(
                    onClick = onGenerate,
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryText, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        if (isGenerating) "Формирую..." else "Сформировать",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

@Composable
private fun StatementDetails(
    prefill: CurrentAttestationPrefill,
    selectedFormat: String,
    returnToDeanBy: String,
    progressAsOf: String,
    overrides: CurrentAttestationOverrides,
    options: CurrentAttestationOptions,
    onFormatChange: (String) -> Unit,
    onReturnToDeanByChange: (String) -> Unit,
    onProgressAsOfChange: (String) -> Unit,
    onOverridesChange: (CurrentAttestationOverrides) -> Unit,
    onOptionsChange: (CurrentAttestationOptions) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SelectField(
            label = "Формат",
            value = selectedFormat,
            options = (prefill.availableFormats.ifEmpty { listOf("docx", "pdf") }).map { it to formatLabel(it) },
            placeholder = "Выберите формат",
            onValueChange = onFormatChange
        )
        DateField("Вернуть в деканат до", returnToDeanBy, onReturnToDeanByChange)
        DateField("Успеваемость на дату", progressAsOf, onProgressAsOfChange)
        InputField("Семестр", overrides.semesterLabel.orEmpty(), { onOverridesChange(overrides.copy(semesterLabel = it)) })
        InputField("Факультет", overrides.facultyName.orEmpty(), { onOverridesChange(overrides.copy(facultyName = it)) })
        InputField("Кафедра", overrides.departmentName.orEmpty(), { onOverridesChange(overrides.copy(departmentName = it)) })
        InputField("Лектор", overrides.lectureTeacherName.orEmpty(), { onOverridesChange(overrides.copy(lectureTeacherName = it)) })
        InputField("Практика", overrides.practiceTeacherName.orEmpty(), { onOverridesChange(overrides.copy(practiceTeacherName = it)) })
        OptionCheckbox("Пропуски лекций", options.includeLectureAbsences) { onOptionsChange(options.copy(includeLectureAbsences = it)) }
        OptionCheckbox("Пропуски практик", options.includePracticeAbsences) { onOptionsChange(options.copy(includePracticeAbsences = it)) }
        OptionCheckbox("Коллоквиумы", options.includeColloquiums) { onOptionsChange(options.copy(includeColloquiums = it)) }
        OptionCheckbox("Лабораторные", options.includeLabs) { onOptionsChange(options.copy(includeLabs = it)) }
        OptionCheckbox("Контрольные работы", options.includeControlWorks) { onOptionsChange(options.copy(includeControlWorks = it)) }
        OptionCheckbox("Итоговая оценка", options.includeFinalGrade) { onOptionsChange(options.copy(includeFinalGrade = it)) }
        StudentsPreview(prefill)
    }
}

@Composable
private fun StudentsPreview(prefill: CurrentAttestationPrefill) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FB), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Состав ведомости", color = PrimaryText, fontWeight = FontWeight.Bold)
        Text("${prefill.context.disciplineName} · ${prefill.context.groupName}", color = SecondaryText)
        prefill.students.take(8).forEach { student ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(student.number.toString(), color = PrimaryText, modifier = Modifier.width(28.dp))
                Text(student.fullName, color = PrimaryText, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(student.studentCode ?: "—", color = SecondaryText)
            }
        }
    }
}

@Composable
private fun ReadyStatementsCard(
    statements: List<ReadyStatement>,
    onRefresh: () -> Unit,
    onDownload: (ReadyStatement) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Готовые ведомости", color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onRefresh, enabled = statements.isNotEmpty()) { Text("Обновить", color = PrimaryText) }
            }
            if (statements.isEmpty()) {
                Text("Готовые ведомости появятся здесь после формирования.", color = SecondaryText)
            } else {
                statements.forEach { statement ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightBlue, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(statement.title, color = PrimaryText, fontWeight = FontWeight.Bold)
                        Text("${statement.contextLabel} · ${formatLabel(statement.format)} · ${statusLabel(statement.status)}", color = SecondaryText)
                        Button(
                            onClick = { onDownload(statement) },
                            enabled = statement.status == "done",
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryText, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Скачать", maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == value }?.second.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(placeholder) },
                textStyle = LocalTextStyle.current.copy(color = Color.Black),
                colors = appFieldColors(unfocusedLabelColor = AppSecondaryText),
                trailingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.arrow_bottom),
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = 13.dp, height = 9.dp)
                            .rotate(if (expanded) 180f else 0f)
                    )
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .exposedDropdownSize(matchTextFieldWidth = true)
                    .background(CardBackground)
            ) {
                options.forEach { (optionValue, optionLabel) ->
                    DropdownMenuItem(
                        text = { Text(optionLabel, color = PrimaryText) },
                        onClick = {
                            onValueChange(optionValue)
                            expanded = false
                        },
                        modifier = Modifier.background(CardBackground)
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = PrimaryText, fontWeight = FontWeight.SemiBold)
}



@Composable
private fun InputField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            colors = appFieldColors(unfocusedLabelColor = AppSecondaryText),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, value: String, onValueChange: (String) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    val initialSelectedDateMillis = remember(value) { value.toUtcStartOfDayMillis() }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedDateMillis)

    if (showDatePicker) {
        StyledDatePickerDialog(
            state = datePickerState,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                datePickerState.selectedDateMillis?.let { selectedMillis ->
                    onValueChange(selectedMillis.toIsoLocalDate())
                }
                showDatePicker = false
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = value.toDisplayDate(),
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("ДД-ММ-ГГГГ") },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.Black),
                colors = appFieldColors(unfocusedLabelColor = AppSecondaryText),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
            Button(
                onClick = { showDatePicker = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE5E7EB),
                    contentColor = Color(0xFF374151)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("Выбрать", maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
            }
        }
    }
}

@Composable
private fun OptionCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightBlue, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, color = PrimaryText)
    }
}

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

private fun String.toDisplayDate(): String = runCatching {
    LocalDate.parse(take(10)).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
}.getOrElse { this }

private suspend fun requestStatementReport(
    journalApi: JournalApi,
    idempotencyKey: String,
    context: CurrentAttestationContext,
    format: String,
    returnToDeanBy: String,
    progressAsOf: String,
    overrides: CurrentAttestationOverrides,
    options: CurrentAttestationOptions
) = requestStatementPayloads(
    context = context,
    format = format,
    returnToDeanBy = returnToDeanBy,
    progressAsOf = progressAsOf,
    overrides = overrides,
    options = options
).let { payloads ->
    var lastError: Throwable? = null
    for (payload in payloads) {
        try {
            // Pass the same idempotency key for every payload variant: preflight 404s don't
            // consume the key on the server side, so reusing the key across variants is safe.
            return@let journalApi.requestCurrentAttestationReport(idempotencyKey, payload)
        } catch (throwable: Throwable) {
            lastError = throwable
            if (!throwable.isRecoverableReportRequestError()) throw throwable
        }
    }
    throw lastError ?: IllegalStateException("Не удалось сформировать ведомость")
}

private fun requestStatementPayloads(
    context: CurrentAttestationContext,
    format: String,
    returnToDeanBy: String,
    progressAsOf: String,
    overrides: CurrentAttestationOverrides,
    options: CurrentAttestationOptions
): List<RequestReportPayload> {
    val dateValue = returnToDeanBy.ifBlank { null }
    val base = RequestReportPayload(
        format = format,
        groupId = context.groupId,
        disciplineId = context.disciplineId,
        returnToDeanBy = dateValue,
        returnToDepartmentBy = dateValue,   // web sends both fields
        progressAsOf = progressAsOf.ifBlank { null },
        overrides = overrides.compact(),
        options = options
    )

    return buildList {
        add(base.copy(academicPeriodId = context.academicPeriodId))
        add(base.copy(periodId = context.academicPeriodId))
        add(base.copy(academicPeriodId = context.academicPeriodId, periodId = context.academicPeriodId))
        context.teacherId?.let { teacherId ->
            add(base.copy(teacherId = teacherId, academicPeriodId = context.academicPeriodId))
            add(base.copy(teacherId = teacherId, periodId = context.academicPeriodId))
            add(base.copy(teacherId = teacherId, academicPeriodId = context.academicPeriodId, periodId = context.academicPeriodId))
        }
    }
}

private fun CurrentAttestationOverrides.compact(): CurrentAttestationOverrides = copy(
    semesterLabel = semesterLabel?.ifBlank { null },
    facultyName = facultyName?.ifBlank { null },
    departmentName = departmentName?.ifBlank { null },
    lectureTeacherName = lectureTeacherName?.ifBlank { null },
    practiceTeacherName = practiceTeacherName?.ifBlank { null }
)

/** Returns true when the server returned 409 IDEMPOTENCY_CONFLICT — the same key was already
 *  used with a different payload. The client must NOT auto-retry; instead show an error and
 *  generate a new key only when the user explicitly creates a new document. */
private fun Throwable.isIdempotencyConflict(): Boolean {
    if (this !is HttpException || code() != 409) return false
    val body = runCatching { response()?.errorBody()?.string().orEmpty() }.getOrElse { "" }
    return body.contains("IDEMPOTENCY_CONFLICT", ignoreCase = true)
}

/** Только 404 (или "not found" в теле) считается recoverable — попробуем следующий вариант payload.
 *  400 — ошибка валидации, не стоит повторять с теми же данными. */
private fun Throwable.isRecoverableReportRequestError(): Boolean {
    if (this !is HttpException) return false
    if (code() == 404) return true
    val body = runCatching { response()?.errorBody()?.string().orEmpty() }.getOrElse { "" }
    return body.contains("not found", ignoreCase = true) ||
           body.contains("teachers can only request reports for their own journal context", ignoreCase = true)
}

/** Извлекает читаемое сообщение из тела HTTP-ошибки или из throwable.message. */
internal fun Throwable.httpErrorMessage(): String {
    if (this is HttpException) {
        val body = runCatching { response()?.errorBody()?.string().orEmpty() }.getOrElse { "" }
        if (body.isNotBlank()) {
            // Пробуем вытащить поле error.message или message из JSON
            val candidate = body
                .substringAfter("\"message\":", "")
                .substringAfter("\"message\" :", "")
                .trimStart()
                .removePrefix("\"")
                .substringBefore("\"")
                .trim()
            if (candidate.isNotBlank()) return candidate
            // Если JSON не разобрался — вернём тело целиком (обрезаем до 200 символов)
            return body.take(200)
        }
    }
    return message?.takeIf { it.isNotBlank() } ?: "Неизвестная ошибка"
}

private suspend fun waitForStatement(
    journalApi: JournalApi,
    statement: ReadyStatement,
    onUpdate: (ReadyStatement) -> Unit
) {
    repeat(12) {
        delay(2500)
        runCatching { journalApi.getReportStatus(statement.jobId) }.onSuccess { task ->
            onUpdate(
                statement.copy(
                    status = task.status,
                    fileName = task.result?.fileName,
                    error = task.errorMessage
                )
            )
            if (task.status == "done" || task.status == "failed" || task.status == "permanently_failed") return
        }
    }
}

private fun shareReportFile(context: Context, bytes: ByteArray, fileName: String, format: String) {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(exportDir, fileName).apply { writeBytes(bytes) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = reportMimeType(format)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Скачать ведомость"))
}

private fun formatLabel(format: String): String = when (format) {
    "docx" -> "DOCX"
    "pdf" -> "PDF"
    "excel" -> "Excel"
    else -> format.uppercase()
}

private fun statusLabel(status: String): String = when (status) {
    "pending" -> "В очереди"
    "running" -> "Формируется"
    "done" -> "Готова"
    "failed", "permanently_failed" -> "Ошибка"
    else -> status
}

private fun reportMimeType(format: String): String = when (format) {
    "pdf" -> "application/pdf"
    "excel" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    else -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
}

private suspend fun loadLessonsForPeriod(
    journalApi: JournalApi,
    periodId: String,
    periods: List<AcademicPeriod>
): List<TeacherLesson> {
    val period = periods.find { it.id == periodId }

    // Use date-range strategy (mirrors the dashboard approach which is proven to return
    // disciplineId / groupId in each lesson object).
    if (period != null) {
        val dateFrom = period.startsAt.take(10)
        val dateTo = period.endsAt.take(10)
        val all = mutableListOf<TeacherLesson>()
        val pageSize = 200
        var offset = 0
        while (true) {
            val page = journalApi.getLessons(
                dateFrom = dateFrom,
                dateTo = dateTo,
                limit = pageSize,
                offset = offset
            )
            all.addAll(page.lessons)
            val total = page.total ?: break
            if (all.size >= total) break
            offset += pageSize
        }
        return all
    }

    // Fallback: period dates not available yet, try period_id param directly.
    return journalApi.getLessons(periodId = periodId, limit = 500).lessons
}

private data class ReadyStatement(
    val jobId: String,
    val title: String,
    val contextLabel: String,
    val format: String,
    val status: String,
    val fileName: String? = null,
    val error: String? = null
)
