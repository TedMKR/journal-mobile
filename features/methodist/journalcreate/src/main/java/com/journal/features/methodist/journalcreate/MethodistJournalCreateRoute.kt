package com.journal.features.methodist.journalcreate

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
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
import com.journal.core.ui.AppTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import com.journal.core.common.config.PersonNameFormatter
import com.journal.core.common.config.userFacingMessage
import com.journal.core.model.teacher.AcademicGroup
import com.journal.core.model.teacher.AcademicPeriod
import com.journal.core.model.teacher.AssignLessonTemplateRequest
import com.journal.core.model.teacher.CreateJournalRequest
import com.journal.core.model.teacher.CreateLessonTemplateBulkRequest
import com.journal.core.model.teacher.Discipline
import com.journal.core.model.teacher.LessonTemplate
import com.journal.core.model.teacher.LessonTemplateDetail
import com.journal.core.model.teacher.JournalContext
import com.journal.core.model.teacher.LessonTopic
import com.journal.core.model.teacher.ImportBatchPreview
import com.journal.core.model.teacher.StartStudentImportRequest
import com.journal.core.model.teacher.StudentImportRow
import com.journal.core.model.teacher.TeacherProfile
import com.journal.core.model.teacher.TemplateAssignment
import com.journal.core.model.teacher.TopicPayload
import com.journal.core.model.teacher.UpdateLessonTemplateRequest
import com.journal.core.network.api.JournalApi
import com.journal.core.ui.AppBackground
import com.journal.core.ui.AppDanger
import com.journal.core.ui.AppDropdown
import com.journal.core.ui.AppFieldPlaceholder
import com.journal.core.ui.AppHeaderBackground
import com.journal.core.ui.AppMutedText
import com.journal.core.ui.AppPrimary
import com.journal.core.ui.appFieldColors
import com.journal.core.ui.readSpreadsheetDocument
import com.journal.core.ui.shareBytesFile
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.journal.core.ui.AppFormField as WebFormField
import com.journal.core.ui.AppSelectCard as SelectCard
import com.journal.core.ui.AppMessageCards as MessageCards
import com.journal.core.ui.AppLoadingCard as LoadingCard
import com.journal.core.ui.AppStateCard as StateCard
import com.journal.core.ui.AppPrimaryButton as PrimaryButton
import com.journal.core.ui.AppTextActionButton as SecondaryButton
import com.journal.core.ui.AppFilterChip as FilterChip
import com.journal.core.ui.AppBadge as Badge

private val BackgroundColor: Color
    @Composable get() = AppTheme.colors.background
private val CardBackground: Color
    @Composable get() = AppTheme.colors.surface
private val PrimaryText: Color
    @Composable get() = AppTheme.colors.primary
private val SecondaryText: Color
    @Composable get() = AppTheme.colors.mutedText
private val LightBlue: Color
    @Composable get() = AppTheme.colors.headerBackground
private val AccentBlue: Color
    @Composable get() = AppTheme.colors.primary
private val DangerColor: Color
    @Composable get() = AppTheme.colors.danger
private const val DashboardVisibleRows = 8

@Composable
fun MethodistJournalCreateRoute(
    journalApi: JournalApi,
    onOpenJournal: (MethodistJournalTarget) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var creating by remember { mutableStateOf(false) }
    var previewing by remember { mutableStateOf(false) }
    var applyingImport by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var periods by remember { mutableStateOf<List<AcademicPeriod>>(emptyList()) }
    var disciplines by remember { mutableStateOf<List<Discipline>>(emptyList()) }
    var teachers by remember { mutableStateOf<List<TeacherProfile>>(emptyList()) }
    var groups by remember { mutableStateOf<List<AcademicGroup>>(emptyList()) }
    var templates by remember { mutableStateOf<List<LessonTemplate>>(emptyList()) }
    var periodId by remember { mutableStateOf("") }
    var disciplineId by remember { mutableStateOf("") }
    var teacherId by remember { mutableStateOf("") }
    var groupId by remember { mutableStateOf("") }
    var templateId by remember { mutableStateOf("") }
    var lessonType by remember { mutableStateOf("practice") }
    var studentRows by remember { mutableStateOf<List<StudentImportRow>>(emptyList()) }
    var sourceFileName by remember { mutableStateOf<String?>(null) }
    var importMode by remember { mutableStateOf("replace_active_roster") }
    var importPreview by remember { mutableStateOf<ImportBatchPreview?>(null) }
    var currentTemplateAssignment by remember { mutableStateOf<TemplateAssignment?>(null) }
    var templateAction by remember { mutableStateOf("replace") }
    var loadingTemplateAssignment by remember { mutableStateOf(false) }

    fun loadTemplates() {
        scope.launch {
            runCatching { journalApi.getLessonTemplates(disciplineId = disciplineId.ifBlank { null }).data }
                .onSuccess { templates = it }
                .onFailure { error = it.userFacingMessage("Не удалось загрузить КТП") }
        }
    }

    fun loadCurrentTemplateAssignment() {
        if (teacherId.isBlank() || disciplineId.isBlank() || groupId.isBlank() || periodId.isBlank()) {
            currentTemplateAssignment = null
            templateAction = "replace"
            return
        }
        scope.launch {
            loadingTemplateAssignment = true
            runCatching {
                journalApi.getCurrentLessonTemplateAssignment(
                    teacherId = teacherId,
                    disciplineId = disciplineId,
                    groupId = groupId,
                    periodId = periodId
                )
            }.onSuccess { assignment ->
                currentTemplateAssignment = assignment
                templateAction = "keep"
            }.onFailure { throwable ->
                if (throwable is HttpException && throwable.code() == 404) {
                    currentTemplateAssignment = null
                    templateAction = "replace"
                } else {
                    error = throwable.userFacingMessage("Не удалось проверить назначенный КТП")
                }
            }
            loadingTemplateAssignment = false
        }
    }

    fun downloadRosterTemplate(templateId: Int) {
        scope.launch {
            error = null
            runCatching {
                val bytes = journalApi.downloadAdminImportTemplate(templateId).bytes()
                shareBytesFile(
                    context = context,
                    bytes = bytes,
                    fileName = if (templateId == 1) "students-template.xlsx" else "students-example.xlsx",
                    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    chooserTitle = "Шаблон состава"
                )
            }.onFailure { error = it.userFacingMessage("Не удалось скачать шаблон состава") }
        }
    }

    fun createImportPreview() {
        if (groupId.isBlank() || periodId.isBlank() || studentRows.isEmpty()) return
        scope.launch {
            previewing = true
            error = null
            success = null
            runCatching {
                journalApi.startStudentImport(
                    StartStudentImportRequest(
                        groupId = groupId,
                        academicPeriodId = periodId,
                        importMode = importMode,
                        sourceFileName = sourceFileName,
                        students = studentRows
                    )
                )
            }.onSuccess { preview ->
                importPreview = preview
                if (preview.status == "preview_ready" || preview.status == "resolved") {
                    success = "Состав готов к применению"
                } else {
                    error = "Проверка состава: ${importStatusLabel(preview.status)}"
                }
            }.onFailure {
                error = it.userFacingMessage("Не удалось проверить состав")
            }
            previewing = false
        }
    }

    fun applyRosterImport() {
        val preview = importPreview ?: return
        scope.launch {
            applyingImport = true
            error = null
            success = null
            runCatching { journalApi.applyStudentImport(preview.batchId) }
                .onSuccess { result ->
                    success = "Состав применён: ${importStatusLabel(result.status)}"
                    studentRows = emptyList()
                    sourceFileName = null
                    importPreview = null
                }
                .onFailure { error = it.userFacingMessage("Не удалось применить состав") }
            applyingImport = false
        }
    }

    val rosterLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            error = null
            success = null
            runCatching {
                val document = readSpreadsheetDocument(context, uri)
                document.fileName to parseStudentImportRows(document.rows)
            }.onSuccess { (fileName, rows) ->
                sourceFileName = fileName
                studentRows = rows
                importPreview = null
                if (rows.isEmpty()) {
                    error = "В файле не найдены строки студентов"
                } else {
                    success = "Загружено строк: ${rows.size}"
                }
            }.onFailure {
                error = it.userFacingMessage("Не удалось разобрать файл состава")
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        runCatching {
            periods = journalApi.getAcademicPeriods(includeClosed = true).data
            disciplines = journalApi.getDisciplines().data
            teachers = journalApi.getTeachers().data
            groups = journalApi.getGroups().data
            periodId = periods.firstOrNull { it.isActive }?.id ?: periods.firstOrNull()?.id.orEmpty()
        }.onFailure { error = it.userFacingMessage("Не удалось загрузить справочники") }
        isLoading = false
        loadTemplates()
    }

    LaunchedEffect(teacherId, disciplineId, groupId, periodId) {
        loadCurrentTemplateAssignment()
    }

    val selectedTemplate = templates.firstOrNull { it.id == templateId }
    val hasAssignmentContext = periodId.isNotBlank() && disciplineId.isNotBlank() && groupId.isNotBlank() && teacherId.isNotBlank()
    val needsTemplateSelection = hasAssignmentContext &&
        !loadingTemplateAssignment &&
        (currentTemplateAssignment == null || templateAction == "replace")
    val canCreate = hasAssignmentContext && (!needsTemplateSelection || templateId.isNotBlank())

    MethodologistScaffold(title = "Создание журнала", useContentCard = false) {
        if (isLoading) {
            LoadingCard("Загружаю справочники...")
            return@MethodologistScaffold
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MessageCards(error = error, success = success)
            CreateJournalSectionCard(
                title = "Контекст журнала",
                subtitle = "Выберите период, дисциплину, тип занятия, преподавателя и группу."
            ) {
                SelectCard("Период", periods.map { it.id to if (it.isActive) "${it.name} · активный" else it.name }, periodId) { periodId = it }
                SelectCard("Дисциплина", disciplines.map { it.id to it.name }, disciplineId) {
                    disciplineId = it
                    templateId = ""
                    loadTemplates()
                }
                SelectCard("Тип занятия", listOf("lecture" to "Лекция", "practice" to "Практика"), lessonType) { lessonType = it }
                SelectCard(
                    "Преподаватель",
                    teachers.map { it.id to PersonNameFormatter.formatFullName(it.fullName) },
                    teacherId
                ) { teacherId = it }
                SelectCard("Группа", groups.map { it.id to it.name }, groupId) { groupId = it }
            }

            CreateJournalSectionCard(
                title = "КТП",
                subtitle = "Шаблон КТП будет назначен преподавателю перед открытием журнала."
            ) {
                when {
                    loadingTemplateAssignment -> StateCard("Проверяю назначенный КТП...")
                    currentTemplateAssignment != null -> {
                        StateCard("Назначен КТП: ${currentTemplateAssignment?.planName ?: currentTemplateAssignment?.planId.orEmpty()}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                text = "Оставить",
                                selected = templateAction == "keep",
                                onClick = { templateAction = "keep"; templateId = "" }
                            )
                            FilterChip(
                                text = "Заменить",
                                selected = templateAction == "replace",
                                onClick = { templateAction = "replace" }
                            )
                        }
                    }
                    else -> StateCard("Назначенный КТП не найден. Выберите шаблон.")
                }
                if (currentTemplateAssignment == null || templateAction == "replace") {
                    SelectCard(
                        label = "Шаблон КТП",
                        options = listOf("" to "Выберите шаблон") + templates.map { it.id to "${it.name} · ${it.totalLessons} занятий" },
                        selected = templateId,
                        onSelected = { templateId = it }
                    )
                    selectedTemplate?.let { template ->
                        TemplateSummaryCard(template)
                    }
                }
            }

            CreateJournalSectionCard(
                title = "Состав группы",
                subtitle = "Загрузите список студентов, проверьте изменения и примените состав группы."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(text = "Шаблон", onClick = { downloadRosterTemplate(1) })
                    SecondaryButton(text = "Пример", onClick = { downloadRosterTemplate(2) })
                }
                PrimaryButton(
                    text = "Загрузить состав",
                    onClick = {
                        rosterLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "text/csv",
                                "text/comma-separated-values",
                                "*/*"
                            )
                        )
                    },
                    enabled = groupId.isNotBlank() && periodId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        text = "Полный состав",
                        selected = importMode == "replace_active_roster",
                        onClick = { importMode = "replace_active_roster"; importPreview = null },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        text = "Добавить",
                        selected = importMode == "merge",
                        onClick = { importMode = "merge"; importPreview = null },
                        modifier = Modifier.weight(1f)
                    )
                }
                RosterSummaryGrid(
                    rowsCount = studentRows.size,
                    sourceFileName = sourceFileName,
                    preview = importPreview
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(
                        text = if (previewing) "Проверяю..." else "Проверить",
                        onClick = ::createImportPreview
                    )
                    PrimaryButton(
                        text = if (applyingImport) "Применяю..." else "Применить",
                        enabled = importPreview?.let { it.status == "preview_ready" || it.status == "resolved" } == true && !applyingImport,
                        onClick = ::applyRosterImport,
                        modifier = Modifier.weight(1f)
                    )
                }
                StateCard("Перед созданием журнала убедитесь, что состав группы актуален.")
            }

            CreateJournalSubmitCard(
                canCreate = canCreate,
                creating = creating,
                onCreate = {
                    scope.launch {
                        creating = true
                        error = null
                        success = null
                        runCatching {
                            if (currentTemplateAssignment != null && templateAction == "replace") {
                                journalApi.revokeLessonTemplateAssignment(currentTemplateAssignment?.id.orEmpty())
                                currentTemplateAssignment = null
                            }
                            if (templateId.isNotBlank()) {
                                journalApi.assignLessonTemplate(
                                    AssignLessonTemplateRequest(
                                        teacherId = teacherId,
                                        planId = templateId,
                                        disciplineId = disciplineId,
                                        groupId = groupId,
                                        periodId = periodId
                                    )
                                )
                            }
                            journalApi.createJournal(
                                CreateJournalRequest(
                                    teacherId = teacherId,
                                    disciplineId = disciplineId,
                                    groupId = groupId,
                                    periodId = periodId,
                                    lessonType = lessonType
                                )
                            )
                        }.onSuccess {
                            success = "Журнал создан"
                            onOpenJournal(MethodistJournalTarget(groupId, disciplineId, periodId, teacherId, lessonType))
                        }.onFailure { error = it.userFacingMessage("Не удалось создать журнал") }
                        creating = false
                    }
                }
            )
        }
    }
}
@Composable
private fun CreateJournalSectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
        }
        content()
    }
}
@Composable
private fun TemplateSummaryCard(template: LessonTemplate) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(template.name, color = PrimaryText, fontWeight = FontWeight.Bold)
        Text(template.disciplineName, color = SecondaryText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Badge("${template.totalLessons} занятий")
            Badge("${template.topicsCount} тем")
        }
    }
}

@Composable
private fun RosterSummaryGrid(
    rowsCount: Int,
    sourceFileName: String?,
    preview: ImportBatchPreview?
) {
    val summary = preview?.summary
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryTile("Строк в файле", rowsCount.toString(), Modifier.weight(1f))
            SummaryTile("Preview", preview?.status?.let(::importStatusLabel) ?: "не создан", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryTile("Конфликты", (summary?.conflicts ?: 0).toString(), Modifier.weight(1f))
            SummaryTile("Ошибки", (summary?.errors ?: 0).toString(), Modifier.weight(1f))
        }
        sourceFileName?.let { StateCard("Файл: $it") }
    }
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(LightBlue, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
        Text(value, color = PrimaryText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CreateJournalSubmitCard(
    canCreate: Boolean,
    creating: Boolean,
    onCreate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Создание", color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("После создания откроется журнал выбранной группы.", color = SecondaryText)
        PrimaryButton(
            text = if (creating) "Создаю..." else "Создать журнал",
            enabled = !creating && canCreate,
            modifier = Modifier.fillMaxWidth(),
            onClick = onCreate
        )
    }
}

private suspend fun syncTopics(
    journalApi: JournalApi,
    detail: LessonTemplateDetail,
    topics: List<TopicDraft>
) {
    val existingIds = detail.topics.mapNotNull { it.id }.toSet()
    val draftIds = topics.mapNotNull { it.id }.toSet()
    topics.forEachIndexed { index, topic ->
        val payload = TopicPayload(
            topicName = topic.name,
            topicDescription = topic.description.ifBlank { null },
            lessonCount = topic.lessonCount,
            orderIndex = index + 1
        )
        if (topic.id == null) {
            journalApi.addLessonTopic(detail.id, payload)
        } else {
            journalApi.updateLessonTopic(detail.id, topic.id, payload)
        }
    }
    existingIds.minus(draftIds).forEach { topicId -> journalApi.deleteLessonTopic(detail.id, topicId) }
}

@Composable
private fun MethodologistScaffold(
    @Suppress("UNUSED_PARAMETER") title: String,
    @Suppress("UNUSED_PARAMETER") subtitle: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    useContentCard: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (actions != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
        if (useContentCard) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}
@Composable
private fun TopicsEditor(topics: List<TopicDraft>, onTopicsChange: (List<TopicDraft>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Темы", color = PrimaryText, fontWeight = FontWeight.Bold)
            SecondaryButton(text = "Добавить тему", onClick = { onTopicsChange(topics + TopicDraft.local(topics.size + 1)) })
        }
        topics.forEachIndexed { index, topic ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundColor, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Тема ${index + 1}", color = SecondaryText)
                OutlinedTextField(
                    value = topic.name,
                    onValueChange = { onTopicsChange(topics.replaceAt(index, topic.copy(name = it))) },
                    label = { Text("Название темы") },
                    textStyle = LocalTextStyle.current.copy(color = PrimaryText),
            colors = appFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = topic.lessonCount.toString(),
                        onValueChange = { value -> onTopicsChange(topics.replaceAt(index, topic.copy(lessonCount = value.toIntOrNull()?.coerceAtLeast(1) ?: 1))) },
                        label = { Text("Занятий") },
                        textStyle = LocalTextStyle.current.copy(color = PrimaryText),
            colors = appFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(text = "Удалить", onClick = { onTopicsChange(topics.filterIndexed { topicIndex, _ -> topicIndex != index }.reindexTopics()) }, color = DangerColor)
                }
                OutlinedTextField(
                    value = topic.description,
                    onValueChange = { onTopicsChange(topics.replaceAt(index, topic.copy(description = it))) },
                    label = { Text("Описание") },
                    textStyle = LocalTextStyle.current.copy(color = PrimaryText),
            colors = appFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private val ShortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private fun formatShortDate(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    return runCatching { OffsetDateTime.parse(value).format(ShortDateFormatter) }
        .getOrElse { value.take(10) }
}

private fun groupSubtitle(group: AcademicGroup): String = listOfNotNull(
    group.faculty,
    group.year?.let { "$it курс" }
).joinToString(" · ").ifBlank { "Факультет и курс не указаны" }

private fun JournalContext.displayDisciplineName(): String = disciplineName ?: discipline?.name ?: "Дисциплина не указана"

private fun JournalContext.displayGroupName(): String = groupName ?: group?.name ?: "Не указана"

private fun JournalContext.displayPeriodName(): String = periodName ?: academicPeriod?.name ?: period?.name ?: "Период не указан"

private fun JournalContext.displayTeacherName(): String =
    PersonNameFormatter.formatFullName(teacherName ?: teacher?.fullName).ifBlank { "Не указан" }

private fun JournalContext.toTarget(): MethodistJournalTarget = MethodistJournalTarget(
    groupId = groupId ?: group?.id.orEmpty(),
    disciplineId = disciplineId ?: discipline?.id.orEmpty(),
    periodId = periodId ?: academicPeriodId ?: academicPeriod?.id ?: period?.id.orEmpty(),
    teacherId = teacherId ?: teacher?.id.orEmpty(),
    lessonType = lessonType.orEmpty()
)

private fun lessonTypeName(type: String): String = when (type) {
    "lecture" -> "Лекция"
    "practice" -> "Практика"
    "lab" -> "Лабораторная"
    "seminar" -> "Семинар"
    else -> type
}

private fun importStatusLabel(status: String): String = when (status) {
    "preview_ready", "resolved" -> "готов"
    "conflicts_detected" -> "конфликт"
    "completed" -> "завершён"
    "partial" -> "частично"
    "failed" -> "ошибка"
    "cancelled" -> "отменён"
    "pending" -> "ожидает"
    "validating" -> "проверка"
    "applying" -> "применение"
    else -> status
}

private fun parseStudentImportRows(rows: List<List<String>>): List<StudentImportRow> {
    val lastNameAliases = listOf("last_name", "фамилия")
    val firstNameAliases = listOf("first_name", "имя")
    val middleNameAliases = listOf("middle_name", "отчество")
    val emailAliases = listOf("email", "почта", "электронная почта")
    val codeAliases = listOf("student_code", "код", "номер зачетки", "зачетка", "номер зачётки", "зачётка")
    val subgroupAliases = listOf("subgroup_number", "подгруппа")
    val startDateAliases = listOf("start_date", "дата начала", "начало")
    val headerIndex = rows.indexOfFirst { row ->
        findColumn(row, lastNameAliases) != -1 && findColumn(row, firstNameAliases) != -1
    }
    if (headerIndex == -1) return emptyList()
    val header = rows[headerIndex]
    val lastNameIndex = findColumn(header, lastNameAliases)
    val firstNameIndex = findColumn(header, firstNameAliases)
    val middleNameIndex = findColumn(header, middleNameAliases)
    val emailIndex = findColumn(header, emailAliases)
    val codeIndex = findColumn(header, codeAliases)
    val subgroupIndex = findColumn(header, subgroupAliases)
    val startDateIndex = findColumn(header, startDateAliases)

    return rows.drop(headerIndex + 1)
        .mapIndexed { index, row ->
            StudentImportRow(
                rowNumber = headerIndex + index + 2,
                lastName = textCell(row, lastNameIndex),
                firstName = textCell(row, firstNameIndex),
                middleName = textCell(row, middleNameIndex).ifBlank { null },
                email = textCell(row, emailIndex).ifBlank { null },
                studentCode = textCell(row, codeIndex).ifBlank { null },
                subgroupNumber = textCell(row, subgroupIndex).toDoubleOrNull()?.toInt(),
                startDate = dateCell(row, startDateIndex)
            )
        }
        .filter { it.lastName.isNotBlank() && it.firstName.isNotBlank() }
}

private fun findColumn(row: List<String>, aliases: List<String>): Int {
    val normalizedAliases = aliases.map(::normalizeKey).toSet()
    return row.indexOfFirst { normalizeKey(it) in normalizedAliases }
}

private fun normalizeKey(value: String): String =
    value.trim()
        .lowercase()
        .replace('ё', 'е')
        .replace(Regex("[^a-zа-я0-9]+"), "")

private fun textCell(row: List<String>, index: Int): String =
    if (index in row.indices) row[index].trim() else ""

private fun dateCell(row: List<String>, index: Int): String? {
    val value = textCell(row, index)
    if (value.isBlank()) return null
    value.replace(',', '.').toDoubleOrNull()?.let { serial ->
        return runCatching { LocalDate.of(1899, 12, 30).plusDays(serial.toLong()).toString() }.getOrNull()
    }
    return runCatching { LocalDate.parse(value) }.getOrNull()?.toString()
        ?: runCatching {
            LocalDate.parse(value, DateTimeFormatter.ofPattern("dd.MM.yyyy")).toString()
        }.getOrNull()
        ?: value
}

private fun List<TopicDraft>.replaceAt(index: Int, item: TopicDraft): List<TopicDraft> =
    mapIndexed { currentIndex, current -> if (currentIndex == index) item else current }

private fun List<TopicDraft>.reindexTopics(): List<TopicDraft> = mapIndexed { index, topic -> topic.copy(orderIndex = index + 1) }

data class MethodistJournalTarget(
    val groupId: String,
    val disciplineId: String,
    val periodId: String,
    val teacherId: String,
    val lessonType: String
)

private data class DashboardLoadError(
    val title: String,
    val message: String
)

private data class DirectoryRow(
    val title: String,
    val subtitle: String
)

private data class TopicDraft(
    val id: String?,
    val name: String,
    val description: String,
    val lessonCount: Int,
    val orderIndex: Int
) {
    companion object {
        fun local(orderIndex: Int): TopicDraft = TopicDraft(null, "", "", 1, orderIndex)
        fun from(topic: LessonTopic): TopicDraft = TopicDraft(
            id = topic.id,
            name = topic.topicName,
            description = topic.topicDescription.orEmpty(),
            lessonCount = topic.lessonCount,
            orderIndex = topic.orderIndex
        )
    }
}
