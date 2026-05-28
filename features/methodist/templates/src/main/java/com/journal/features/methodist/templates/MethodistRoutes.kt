package com.journal.features.methodist.templates

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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
import com.journal.core.model.teacher.TeacherProfile
import com.journal.core.model.teacher.TopicPayload
import com.journal.core.model.teacher.UpdateLessonTemplateRequest
import com.journal.core.network.api.JournalApi
import com.journal.core.ui.AppBackground
import com.journal.core.ui.AppDanger
import com.journal.core.ui.AppFieldBorder
import com.journal.core.ui.AppFieldPlaceholder
import com.journal.core.ui.AppHeaderBackground
import com.journal.core.ui.AppMutedText
import com.journal.core.ui.AppPrimary
import com.journal.core.ui.appFieldColors
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val BackgroundColor = AppBackground
private val CardBackground = Color.White
private val PrimaryText = AppPrimary
private val SecondaryText = AppMutedText
private val LightBlue = AppHeaderBackground
private val AccentBlue = AppPrimary
private val DangerColor = AppDanger
private const val DashboardVisibleRows = 8

@Composable
fun MethodistDashboardRoute(
    journalApi: JournalApi,
    /** First name extracted from JWT — shown in the profile header. */
    jwtFirstName: String? = null
) {
    var isLoading by remember { mutableStateOf(true) }
    var errors by remember { mutableStateOf<List<DashboardLoadError>>(emptyList()) }
    var periods by remember { mutableStateOf<List<AcademicPeriod>>(emptyList()) }
    var disciplines by remember { mutableStateOf<List<Discipline>>(emptyList()) }
    var teachers by remember { mutableStateOf<List<TeacherProfile>>(emptyList()) }
    var groups by remember { mutableStateOf<List<AcademicGroup>>(emptyList()) }
    var journals by remember { mutableStateOf<List<JournalContext>>(emptyList()) }
    var search by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoading = true
        val loadErrors = mutableListOf<DashboardLoadError>()

        suspend fun <T> loadSection(title: String, block: suspend () -> T): T? {
            return runCatching { block() }
                .onFailure { throwable ->
                    loadErrors += DashboardLoadError(
                        title = title,
                        message = throwable.message ?: "Не удалось получить данные"
                    )
                }
                .getOrNull()
        }

        periods = loadSection("Периоды") { journalApi.getAcademicPeriods(includeClosed = true).data }.orEmpty()
        disciplines = loadSection("Дисциплины") { journalApi.getDisciplines(limit = 200).data }.orEmpty()
        teachers = loadSection("Преподаватели") { journalApi.getTeachers(limit = 200).data }.orEmpty()
        groups = loadSection("Группы") { journalApi.getGroups(limit = 200).data }.orEmpty()

        val activePeriodId = periods.firstOrNull { it.isActive }?.id ?: periods.firstOrNull()?.id
        journals = loadSection("Журналы") {
            journalApi.getJournals(periodId = activePeriodId, limit = 200, offset = 0).data
        }.orEmpty()

        errors = loadErrors
        isLoading = false
    }

    val activePeriodName = periods.firstOrNull { it.isActive }?.name ?: periods.firstOrNull()?.name ?: "Период не найден"
    val query = search.trim().lowercase()
    val filteredDisciplines = remember(disciplines, query) {
        disciplines.filter { discipline ->
            query.isBlank() || listOf(discipline.name, discipline.code.orEmpty()).any { it.lowercase().contains(query) }
        }
    }
    val filteredTeachers = remember(teachers, query) {
        teachers.filter { teacher ->
            query.isBlank() || listOf(teacher.fullName, teacher.email.orEmpty()).any { it.lowercase().contains(query) }
        }
    }
    val filteredGroups = remember(groups, query) {
        groups.filter { group ->
            query.isBlank() || listOf(group.name, group.faculty.orEmpty(), group.year?.toString().orEmpty())
                .any { it.lowercase().contains(query) }
        }
    }

    MethodologistScaffold(title = "Личный кабинет", useContentCard = false) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (isLoading) {
                LoadingCard("Загружаю информацию...")
            } else {
                DashboardSummaryGrid(
                    disciplinesCount = disciplines.size,
                    teachersCount = teachers.size,
                    groupsCount = groups.size,
                    journalsCount = journals.size,
                    jwtFirstName = jwtFirstName
                )
                PeriodStrip(activePeriodName)
                if (errors.isNotEmpty()) {
                    DashboardAlerts(errors)
                }
                DirectoryPanel(
                    search = search,
                    onSearchChange = { search = it },
                    disciplines = filteredDisciplines,
                    teachers = filteredTeachers,
                    groups = filteredGroups
                )
            }
        }
    }
}

@Composable
fun MethodistJournalsRoute(
    journalApi: JournalApi,
    onOpenJournal: (MethodistJournalTarget) -> Unit,
    onCreateJournal: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var journals by remember { mutableStateOf<List<JournalContext>>(emptyList()) }
    var periods by remember { mutableStateOf<List<AcademicPeriod>>(emptyList()) }
    var disciplines by remember { mutableStateOf<List<Discipline>>(emptyList()) }
    var groups by remember { mutableStateOf<List<AcademicGroup>>(emptyList()) }
    var teachers by remember { mutableStateOf<List<TeacherProfile>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var selectedPeriodId by remember { mutableStateOf("") }
    var selectedDisciplineId by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf("") }
    var selectedTeacherId by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("") }

    suspend fun loadJournals() {
        isLoading = true
        error = null
        runCatching {
            journalApi.getJournals(
                periodId = selectedPeriodId.ifBlank { null },
                disciplineId = selectedDisciplineId.ifBlank { null },
                groupId = selectedGroupId.ifBlank { null },
                teacherId = selectedTeacherId.ifBlank { null },
                lessonType = selectedType.ifBlank { null },
                query = search.trim().ifBlank { null },
                limit = 200,
                offset = 0
            ).data
        }.onSuccess { loadedJournals ->
            journals = loadedJournals
        }.onFailure { throwable ->
            journals = emptyList()
            error = throwable.message ?: "Не удалось загрузить журналы"
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        runCatching { periods = journalApi.getAcademicPeriods(includeClosed = true).data }
        runCatching { disciplines = journalApi.getDisciplines(limit = 200).data }
        runCatching { groups = journalApi.getGroups(limit = 200).data }
        runCatching { teachers = journalApi.getTeachers(limit = 200).data }
    }

    LaunchedEffect(selectedPeriodId, selectedDisciplineId, selectedGroupId, selectedTeacherId, selectedType, search) {
        loadJournals()
    }

    val contexts = remember(journals) { journals }

    MethodologistScaffold(
        title = "Журналы",
        subtitle = "Методист",
        actions = {
            PrimaryButton(text = "Создать журнал", onClick = onCreateJournal)
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            JournalFilters(
                search = search,
                onSearchChange = { search = it },
                periods = periods,
                selectedPeriodId = selectedPeriodId,
                onPeriodChange = { selectedPeriodId = it },
                disciplines = disciplines,
                selectedDisciplineId = selectedDisciplineId,
                onDisciplineChange = { selectedDisciplineId = it },
                groups = groups,
                selectedGroupId = selectedGroupId,
                onGroupChange = { selectedGroupId = it },
                teachers = teachers,
                selectedTeacherId = selectedTeacherId,
                onTeacherChange = { selectedTeacherId = it },
                selectedType = selectedType,
                onTypeChange = { selectedType = it }
            )
            when {
                isLoading -> LoadingCard("Загружаю журналы...")
                error != null -> StateCard(error.orEmpty(), isError = true)
                contexts.isEmpty() -> StateCard("Журналы не найдены")
                else -> contexts.forEach { context ->
                    JournalContextCard(context = context, onOpen = { onOpenJournal(context.toTarget()) })
                }
            }
        }
    }
}

@Composable
fun MethodistTemplatesRoute(
    journalApi: JournalApi
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var disciplines by remember { mutableStateOf<List<Discipline>>(emptyList()) }
    var templates by remember { mutableStateOf<List<LessonTemplate>>(emptyList()) }
    var selectedTemplate by remember { mutableStateOf<LessonTemplateDetail?>(null) }
    var search by remember { mutableStateOf("") }
    var selectedDisciplineId by remember { mutableStateOf("") }
    var includeArchived by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    fun loadTemplates() {
        scope.launch {
            isLoading = true
            error = null
            runCatching {
                templates = journalApi.getLessonTemplates(
                    disciplineId = selectedDisciplineId.ifBlank { null },
                    includeDeleted = includeArchived
                ).data
            }.onFailure { error = it.message ?: "Не удалось загрузить КТП" }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            disciplines = journalApi.getDisciplines().data
        }.onFailure { error = it.message ?: "Не удалось загрузить дисциплины" }
        loadTemplates()
    }

    val filteredTemplates = remember(templates, search) {
        val query = search.trim().lowercase()
        if (query.isBlank()) templates else templates.filter { template ->
            listOf(template.name, template.disciplineName, template.description.orEmpty())
                .any { it.lowercase().contains(query) }
        }
    }

    MethodologistScaffold(
        title = "КТП шаблоны",
        actions = {
            PrimaryButton(text = "Добавить КТП", onClick = { showCreateDialog = true })
        },
        useContentCard = false
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TemplateListBlock(
                error = error,
                success = success,
                search = search,
                onSearchChange = { search = it },
                disciplines = disciplines,
                selectedDisciplineId = selectedDisciplineId,
                onDisciplineSelected = {
                    selectedDisciplineId = it
                    loadTemplates()
                },
                includeArchived = includeArchived,
                onToggleArchived = {
                    includeArchived = !includeArchived
                    loadTemplates()
                },
                isLoading = isLoading,
                templates = filteredTemplates,
                selectedTemplateId = selectedTemplate?.id,
                onOpenTemplate = { template ->
                    scope.launch {
                        runCatching { journalApi.getLessonTemplate(template.id) }
                            .onSuccess { selectedTemplate = it }
                            .onFailure { error = it.message ?: "Не удалось открыть КТП" }
                    }
                }
            )
            selectedTemplate?.let { detail ->
                TemplateEditorCard(
                    detail = detail,
                    saving = saving,
                    onSave = { name: String, description: String?, topics: List<TopicDraft> ->
                        scope.launch {
                            saving = true
                            error = null
                            success = null
                            runCatching {
                                journalApi.updateLessonTemplate(detail.id, UpdateLessonTemplateRequest(name, description))
                                syncTopics(journalApi, detail, topics)
                                selectedTemplate = journalApi.getLessonTemplate(detail.id)
                                loadTemplates()
                            }.onSuccess { success = "Изменения сохранены" }
                                .onFailure { error = it.message ?: "Не удалось сохранить КТП" }
                            saving = false
                        }
                    },
                    onDelete = {
                        scope.launch {
                            runCatching { journalApi.deleteLessonTemplate(detail.id) }
                                .onSuccess {
                                    selectedTemplate = null
                                    success = "КТП удалён или перенесён в архив"
                                    loadTemplates()
                                }
                                .onFailure { error = it.message ?: "Не удалось удалить КТП" }
                        }
                    }
                )
            }
        }
    }

    if (showCreateDialog) {
        TemplateCreateDialog(
            disciplines = disciplines,
            saving = saving,
            onDismiss = { showCreateDialog = false },
            onCreate = { disciplineId, name, description, topics ->
                scope.launch {
                    saving = true
                    error = null
                    success = null
                    runCatching {
                        val created = journalApi.createLessonTemplateBulk(
                            CreateLessonTemplateBulkRequest(
                                disciplineId = disciplineId,
                                name = name,
                                description = description,
                                topics = topics
                            )
                        )
                        selectedTemplate = created
                        loadTemplates()
                    }.onSuccess {
                        success = "КТП создан"
                        showCreateDialog = false
                    }.onFailure { error = it.message ?: "Не удалось создать КТП" }
                    saving = false
                }
            }
        )
    }
}

@Composable
fun MethodistJournalCreateRoute(
    journalApi: JournalApi,
    onOpenJournal: (MethodistJournalTarget) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var creating by remember { mutableStateOf(false) }
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

    fun loadTemplates() {
        scope.launch {
            runCatching { journalApi.getLessonTemplates(disciplineId = disciplineId.ifBlank { null }).data }
                .onSuccess { templates = it }
                .onFailure { error = it.message ?: "Не удалось загрузить КТП" }
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
        }.onFailure { error = it.message ?: "Не удалось загрузить справочники" }
        isLoading = false
        loadTemplates()
    }

    val selectedTemplate = templates.firstOrNull { it.id == templateId }
    val canCreate = periodId.isNotBlank() && disciplineId.isNotBlank() && groupId.isNotBlank() && teacherId.isNotBlank()

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
                SelectCard("Преподаватель", teachers.map { it.id to it.fullName }, teacherId) { teacherId = it }
                SelectCard("Группа", groups.map { it.id to it.name }, groupId) { groupId = it }
            }

            CreateJournalSectionCard(
                title = "КТП",
                subtitle = "Шаблон КТП будет назначен преподавателю перед открытием журнала."
            ) {
                SelectCard(
                    label = "Шаблон КТП",
                    options = listOf("" to "Выберите шаблон") + templates.map { it.id to "${it.name} · ${it.totalLessons} занятий" },
                    selected = templateId,
                    onSelected = { templateId = it }
                )
                selectedTemplate?.let { template ->
                    TemplateSummaryCard(template)
                } ?: StateCard("Можно создать журнал без шаблона КТП и назначить его позже.")
            }

            CreateJournalSectionCard(
                title = "Состав группы",
                subtitle = "Мобильная версия показывает состояние состава. Импорт файла выполняется в веб-версии."
            ) {
                RosterSummaryGrid()
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
                        }.onFailure { error = it.message ?: "Не удалось создать журнал" }
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
            .background(CardBackground, RoundedCornerShape(20.dp))
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
private fun RosterSummaryGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryTile("Строк в файле", "0", Modifier.weight(1f))
            SummaryTile("Preview", "не создан", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryTile("Конфликты", "0", Modifier.weight(1f))
            SummaryTile("Ошибки", "0", Modifier.weight(1f))
        }
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
            .background(CardBackground, RoundedCornerShape(20.dp))
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
                    .background(CardBackground, RoundedCornerShape(20.dp))
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
private fun JournalFilters(
    search: String,
    onSearchChange: (String) -> Unit,
    periods: List<AcademicPeriod>,
    selectedPeriodId: String,
    onPeriodChange: (String) -> Unit,
    disciplines: List<Discipline>,
    selectedDisciplineId: String,
    onDisciplineChange: (String) -> Unit,
    groups: List<AcademicGroup>,
    selectedGroupId: String,
    onGroupChange: (String) -> Unit,
    teachers: List<TeacherProfile>,
    selectedTeacherId: String,
    onTeacherChange: (String) -> Unit,
    selectedType: String,
    onTypeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            label = { Text("Поиск") },
            placeholder = { Text("Дисциплина, группа, преподаватель") },
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            colors = appFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        CompactOptionFilter(
            label = "Период",
            options = listOf("" to "Все периоды") + periods.map { it.id to if (it.isActive) "${it.name} · активный" else it.name },
            selected = selectedPeriodId,
            onSelected = onPeriodChange
        )
        CompactOptionFilter(
            label = "Дисциплина",
            options = listOf("" to "Все дисциплины") + disciplines.map { it.id to it.name },
            selected = selectedDisciplineId,
            onSelected = onDisciplineChange
        )
        CompactOptionFilter(
            label = "Группа",
            options = listOf("" to "Все группы") + groups.map { it.id to it.name },
            selected = selectedGroupId,
            onSelected = onGroupChange
        )
        CompactOptionFilter(
            label = "Преподаватель",
            options = listOf("" to "Все преподаватели") + teachers.map { it.id to it.fullName },
            selected = selectedTeacherId,
            onSelected = onTeacherChange
        )
        CompactOptionFilter(
            label = "Тип занятия",
            options = listOf(
                "" to "Все типы",
                "lecture" to "Лекция",
                "practice" to "Практика",
                "lab" to "Лабораторная",
                "seminar" to "Семинар"
            ),
            selected = selectedType,
            onSelected = onTypeChange
        )
    }
}

@Composable
private fun CompactOptionFilter(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val selectedText = options.firstOrNull { it.first == selected }?.second ?: options.firstOrNull()?.second.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = PrimaryText, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { fieldWidth = with(density) { it.width.toDp() } }
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, if (expanded) PrimaryText else AppFieldBorder, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    selectedText,
                    color = PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Image(
                    painter = painterResource(id = R.drawable.arrow_bottom),
                    contentDescription = null,
                    modifier = Modifier.size(width = 13.dp, height = 9.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(fieldWidth)
                    .background(Color.White)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.second, color = PrimaryText, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            onSelected(option.first)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateListBlock(
    error: String?,
    success: String?,
    search: String,
    onSearchChange: (String) -> Unit,
    disciplines: List<Discipline>,
    selectedDisciplineId: String,
    onDisciplineSelected: (String) -> Unit,
    includeArchived: Boolean,
    onToggleArchived: () -> Unit,
    isLoading: Boolean,
    templates: List<LessonTemplate>,
    selectedTemplateId: String?,
    onOpenTemplate: (LessonTemplate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Список КТП", color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        MessageCards(error = error, success = success)
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            label = { Text("Поиск: название, дисциплина, описание") },
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            colors = appFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        CompactOptionFilter(
            label = "Дисциплина",
            options = listOf("" to "Все дисциплины") + disciplines.map { it.id to it.name },
            selected = selectedDisciplineId,
            onSelected = onDisciplineSelected
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(text = "Архив", selected = includeArchived, onClick = onToggleArchived)
        }
        when {
            isLoading -> LoadingCard("Загрузка шаблонов...")
            templates.isEmpty() -> StateCard("КТП не найдены")
            else -> templates.forEach { template ->
                TemplateCard(
                    template = template,
                    selected = selectedTemplateId == template.id,
                    onClick = { onOpenTemplate(template) }
                )
            }
        }
    }
}

@Composable
private fun DashboardSummaryGrid(
    disciplinesCount: Int,
    teachersCount: Int,
    groupsCount: Int,
    journalsCount: Int,
    jwtFirstName: String? = null
) {
    // Priority: JWT first name → fallback title
    val displayName = jwtFirstName?.takeIf(String::isNotBlank) ?: "Методист"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryText, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            displayName,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardSummaryTile("Дисциплины", disciplinesCount.toString(), Modifier.weight(1f))
            DashboardSummaryTile("Преподаватели", teachersCount.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardSummaryTile("Группы", groupsCount.toString(), Modifier.weight(1f))
            DashboardSummaryTile("Журналы", journalsCount.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun DashboardSummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(CardBackground, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(PrimaryText, RoundedCornerShape(999.dp))
        )
        Text(label, color = PrimaryText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(value, color = AccentBlue, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PeriodStrip(activePeriodName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Текущий период", color = SecondaryText)
        Text(activePeriodName, color = AccentBlue, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DashboardAlerts(errors: List<DashboardLoadError>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFE4E6), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        errors.forEach { error ->
            Text("${error.title}: ${error.message}", color = DangerColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DirectoryPanel(
    search: String,
    onSearchChange: (String) -> Unit,
    disciplines: List<Discipline>,
    teachers: List<TeacherProfile>,
    groups: List<AcademicGroup>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Справочники", color = SecondaryText, fontWeight = FontWeight.SemiBold)
        Text(
            "Информация по учебному процессу",
            color = PrimaryText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            placeholder = { Text("Поиск по дисциплинам, преподавателям и группам", color = AppFieldPlaceholder) },
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            colors = appFieldColors(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        DirectoryBlock(
            title = "Дисциплины",
            count = disciplines.size,
            rows = disciplines.take(DashboardVisibleRows).map { DirectoryRow(it.name, it.code ?: "Код не указан") },
            hiddenCount = (disciplines.size - DashboardVisibleRows).coerceAtLeast(0),
            emptyText = "Нет дисциплин"
        )
        DirectoryBlock(
            title = "Преподаватели",
            count = teachers.size,
            rows = teachers.take(DashboardVisibleRows).map { DirectoryRow(it.fullName, it.email ?: "Email не указан") },
            hiddenCount = (teachers.size - DashboardVisibleRows).coerceAtLeast(0),
            emptyText = "Нет преподавателей"
        )
        DirectoryBlock(
            title = "Группы",
            count = groups.size,
            rows = groups.take(DashboardVisibleRows).map { DirectoryRow(it.name, groupSubtitle(it)) },
            hiddenCount = (groups.size - DashboardVisibleRows).coerceAtLeast(0),
            emptyText = "Нет групп"
        )
    }
}

@Composable
private fun DirectoryBlock(
    title: String,
    count: Int,
    rows: List<DirectoryRow>,
    hiddenCount: Int,
    emptyText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LightBlue, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                count.toString(),
                modifier = Modifier.background(LightBlue, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                color = AccentBlue,
                fontWeight = FontWeight.Bold
            )
        }
        if (rows.isEmpty()) {
            Text(emptyText, color = SecondaryText, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            rows.forEach { row -> DirectoryDataRow(row) }
        }
        if (hiddenCount > 0) {
            Text("Еще $hiddenCount", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DirectoryDataRow(row: DirectoryRow) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(row.title, color = AccentBlue, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(row.subtitle, color = SecondaryText, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun JournalContextCard(context: JournalContext, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F8FB), RoundedCornerShape(18.dp))
            .border(1.dp, LightBlue, RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(context.displayPeriodName(), color = SecondaryText)
                Text(
                    context.displayDisciplineName(),
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Badge(lessonTypeName(context.lessonType.orEmpty()))
        }
        JournalMetaTile("Группа", context.displayGroupName())
        JournalMetaTile("Преподаватель", context.displayTeacherName())
        JournalMetaTile("Занятий", "${context.lessonCount}, проведено ${context.heldCount}")
        JournalMetaTile("Создан", formatShortDate(context.createdAt))
        PrimaryButton(text = "Открыть", modifier = Modifier.fillMaxWidth(), onClick = onOpen)
    }
}

@Composable
private fun JournalMetaTile(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SecondaryText, modifier = Modifier.weight(1f))
        Text(
            value,
            color = PrimaryText,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f)
        )
    }
}

@Composable
private fun TemplateCard(template: LessonTemplate, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) LightBlue else Color.White, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(template.name, color = PrimaryText, fontWeight = FontWeight.Bold)
        Text("${template.disciplineName} · ${template.topicsCount} тем · ${template.totalLessons} занятий", color = SecondaryText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (template.isArchived) Badge("Архив")
            if (template.hasAssignments) Badge("Назначен")
        }
    }
}

@Composable
private fun TemplateEditorCard(
    detail: LessonTemplateDetail,
    saving: Boolean,
    onSave: (String, String?, List<TopicDraft>) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(detail.id) { mutableStateOf(detail.name) }
    var description by remember(detail.id) { mutableStateOf(detail.description.orEmpty()) }
    var topics by remember(detail.id) {
        mutableStateOf(detail.topics.sortedBy { it.orderIndex }.map { TopicDraft.from(it) })
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Редактирование шаблона", color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название") },
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            colors = appFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Описание") },
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            colors = appFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        TopicsEditor(topics = topics, onTopicsChange = { topics = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(
                text = if (saving) "Сохраняю..." else "Сохранить",
                enabled = !saving && name.isNotBlank() && topics.any { it.name.isNotBlank() },
                onClick = { onSave(name.trim(), description.trim().ifBlank { null }, topics.filter { it.name.isNotBlank() }) }
            )
            SecondaryButton(text = "Удалить", onClick = onDelete, color = DangerColor)
        }
    }
}

@Composable
private fun TemplateCreateDialog(
    disciplines: List<Discipline>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, String?, List<TopicPayload>) -> Unit
) {
    var disciplineId by remember { mutableStateOf(disciplines.firstOrNull()?.id.orEmpty()) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var topics by remember { mutableStateOf(listOf(TopicDraft.local(1))) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),  // ← Только скролл, без fillMaxHeight
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Создание", color = SecondaryText, style = MaterialTheme.typography.labelMedium)
                    Text("Новый КТП", color = PrimaryText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "×",
                    color = SecondaryText,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
            }

            WebFormField(label = "Дисциплина") {
                SelectCard("", disciplines.map { it.id to it.name }, disciplineId) { disciplineId = it }
            }
            WebFormField(label = "Название") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("КТП по дисциплине") },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
            colors = appFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            WebFormField(label = "Описание") {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Семестр, поток, комментарии") },
                    minLines = 3,
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
            colors = appFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            TopicsEditor(topics = topics, onTopicsChange = { topics = it })

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    disciplineId = disciplines.firstOrNull()?.id.orEmpty()
                    name = ""
                    description = ""
                    topics = listOf(TopicDraft.local(1))
                }) {
                    Text("Очистить", color = AccentBlue, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = SecondaryText, fontWeight = FontWeight.SemiBold)
                }
                PrimaryButton(
                    text = if (saving) "Создание..." else "Создать КТП",
                    enabled = !saving && disciplineId.isNotBlank() && name.isNotBlank() && topics.any { it.name.isNotBlank() },
                    onClick = {
                        onCreate(
                            disciplineId,
                            name.trim(),
                            description.trim().ifBlank { null },
                            topics.filter { it.name.isNotBlank() }.mapIndexed { index, topic ->
                                TopicPayload(topic.name.trim(), topic.description.trim().ifBlank { null }, topic.lessonCount, index + 1)
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun WebFormField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = PrimaryText, fontWeight = FontWeight.SemiBold)
        content()
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
                    .background(BackgroundColor, RoundedCornerShape(14.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Тема ${index + 1}", color = SecondaryText)
                OutlinedTextField(
                    value = topic.name,
                    onValueChange = { onTopicsChange(topics.replaceAt(index, topic.copy(name = it))) },
                    label = { Text("Название темы") },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
            colors = appFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = topic.lessonCount.toString(),
                        onValueChange = { value -> onTopicsChange(topics.replaceAt(index, topic.copy(lessonCount = value.toIntOrNull()?.coerceAtLeast(1) ?: 1))) },
                        label = { Text("Занятий") },
                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
            colors = appFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(text = "Удалить", onClick = { onTopicsChange(topics.filterIndexed { topicIndex, _ -> topicIndex != index }.reindexTopics()) }, color = DangerColor)
                }
                OutlinedTextField(
                    value = topic.description,
                    onValueChange = { onTopicsChange(topics.replaceAt(index, topic.copy(description = it))) },
                    label = { Text("Описание") },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
            colors = appFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SelectCard(label: String, options: List<Pair<String, String>>, selected: String, onSelected: (String) -> Unit) {
    CompactOptionFilter(
        label = label,
        options = options,
        selected = selected,
        onSelected = onSelected
    )
}

@Composable
private fun MessageCards(error: String?, success: String?) {
    error?.let { StateCard(text = it, isError = true) }
    success?.let { StateCard(text = it) }
}

@Composable
private fun LoadingCard(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(BackgroundColor, RoundedCornerShape(16.dp)).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.width(22.dp), color = AccentBlue)
        Text(text, color = PrimaryText)
    }
}

@Composable
private fun StateCard(text: String, isError: Boolean = false) {
    Text(
        text = text,
        color = if (isError) DangerColor else PrimaryText,
        modifier = Modifier.fillMaxWidth().background(BackgroundColor, RoundedCornerShape(16.dp)).padding(16.dp),
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White)
    ) { Text(text) }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit, color: Color = AccentBlue) {
    TextButton(onClick = onClick) { Text(text, color = color, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) AccentBlue else LightBlue, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) { Text(text, color = if (selected) Color.White else PrimaryText) }
}

@Composable
private fun Badge(text: String) {
    Text(
        text = text,
        color = PrimaryText,
        modifier = Modifier.background(LightBlue, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
        fontWeight = FontWeight.SemiBold
    )
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

private fun JournalContext.displayTeacherName(): String = teacherName ?: teacher?.fullName ?: "Не указан"

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
