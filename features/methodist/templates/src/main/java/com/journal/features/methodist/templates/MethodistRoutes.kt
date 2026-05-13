package com.journal.features.methodist.templates

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import com.journal.core.model.teacher.AcademicGroup
import com.journal.core.model.teacher.AcademicPeriod
import com.journal.core.model.teacher.AssignLessonTemplateRequest
import com.journal.core.model.teacher.CreateJournalRequest
import com.journal.core.model.teacher.CreateLessonTemplateBulkRequest
import com.journal.core.model.teacher.Discipline
import com.journal.core.model.teacher.LessonTemplate
import com.journal.core.model.teacher.LessonTemplateDetail
import com.journal.core.model.teacher.LessonTopic
import com.journal.core.model.teacher.TeacherLesson
import com.journal.core.model.teacher.TeacherProfile
import com.journal.core.model.teacher.TopicPayload
import com.journal.core.model.teacher.UpdateLessonTemplateRequest
import com.journal.core.network.api.JournalApi
import kotlinx.coroutines.launch

private val BackgroundColor = Color(0xFFEDEEED)
private val CardBackground = Color.White
private val PrimaryText = Color(0xFF223268)
private val SecondaryText = Color(0xFF6D7885)
private val LightBlue = Color(0xFFD3D7E1)
private val AccentBlue = Color(0xFF223268)
private val DangerColor = Color(0xFFC44A4A)

@Composable
fun MethodistJournalsRoute(
    journalApi: JournalApi,
    onOpenJournal: (MethodistJournalTarget) -> Unit,
    onCreateJournal: () -> Unit,
    onOpenTemplates: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var lessons by remember { mutableStateOf<List<TeacherLesson>>(emptyList()) }
    var periods by remember { mutableStateOf<List<AcademicPeriod>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        runCatching {
            val loadedPeriods = journalApi.getAcademicPeriods(includeClosed = true).data
            val loadedLessons = journalApi.getLessons(limit = 200).lessons
            periods = loadedPeriods
            lessons = loadedLessons
        }.onFailure { error = it.message ?: "Не удалось загрузить журналы" }
        isLoading = false
    }

    val periodNames = remember(periods) { periods.associate { it.id to it.name } }
    val contexts = remember(lessons, periods, search, selectedType) {
        buildJournalContexts(lessons, periodNames)
            .filter { context ->
                val query = search.trim().lowercase()
                val matchesSearch = query.isBlank() || listOf(
                    context.disciplineName,
                    context.groupName,
                    context.teacherName,
                    context.periodName
                ).any { it.lowercase().contains(query) }
                matchesSearch && (selectedType.isBlank() || context.lessonType == selectedType)
            }
    }

    MethodologistScaffold(
        title = "Журналы",
        subtitle = "Методист",
        actions = {
            PrimaryButton(text = "Создать журнал", onClick = onCreateJournal)
            SecondaryButton(text = "КТП", onClick = onOpenTemplates)
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SearchAndTypeFilters(
                search = search,
                onSearchChange = { search = it },
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
    journalApi: JournalApi,
    onOpenJournals: () -> Unit
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
            SecondaryButton(text = "Журналы", onClick = onOpenJournals)
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MessageCards(error = error, success = success)
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Поиск: название, дисциплина, описание") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(text = "Все", selected = selectedDisciplineId.isBlank()) {
                    selectedDisciplineId = ""
                    loadTemplates()
                }
                disciplines.take(3).forEach { discipline ->
                    FilterChip(text = discipline.name, selected = selectedDisciplineId == discipline.id) {
                        selectedDisciplineId = discipline.id
                        loadTemplates()
                    }
                }
                FilterChip(text = "Архив", selected = includeArchived) {
                    includeArchived = !includeArchived
                    loadTemplates()
                }
            }
            when {
                isLoading -> LoadingCard("Загрузка шаблонов...")
                filteredTemplates.isEmpty() -> StateCard("КТП не найдены")
                else -> filteredTemplates.forEach { template ->
                    TemplateCard(
                        template = template,
                        selected = selectedTemplate?.id == template.id,
                        onClick = {
                            scope.launch {
                                runCatching { journalApi.getLessonTemplate(template.id) }
                                    .onSuccess { selectedTemplate = it }
                                    .onFailure { error = it.message ?: "Не удалось открыть КТП" }
                            }
                        }
                    )
                }
            }
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
    onOpenJournal: (MethodistJournalTarget) -> Unit,
    onBack: () -> Unit
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

    MethodologistScaffold(
        title = "Создание журнала",
        onBack = onBack
    ) {
        if (isLoading) {
            LoadingCard("Загружаю справочники...")
            return@MethodologistScaffold
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MessageCards(error = error, success = success)
            SelectCard("Период", periods.map { it.id to it.name }, periodId) { periodId = it }
            SelectCard("Дисциплина", disciplines.map { it.id to it.name }, disciplineId) {
                disciplineId = it
                templateId = ""
                loadTemplates()
            }
            SelectCard("Тип занятия", listOf("lecture" to "Лекция", "practice" to "Практика"), lessonType) { lessonType = it }
            SelectCard("Преподаватель", teachers.map { it.id to it.fullName }, teacherId) { teacherId = it }
            SelectCard("Группа", groups.map { it.id to it.name }, groupId) { groupId = it }
            SelectCard("Шаблон КТП", templates.map { it.id to "${it.name} · ${it.totalLessons} занятий" }, templateId) { templateId = it }
            PrimaryButton(
                text = if (creating) "Создаю..." else "Создать журнал",
                enabled = !creating && periodId.isNotBlank() && disciplineId.isNotBlank() && groupId.isNotBlank() && teacherId.isNotBlank(),
                onClick = {
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
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        onBack?.let { backAction ->
            Text(
                text = "← Назад",
                color = SecondaryText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable(onClick = backAction)
                    .padding(vertical = 8.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(20.dp))
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                subtitle?.let { Text(it, color = SecondaryText) }
                Text(title, color = PrimaryText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun SearchAndTypeFilters(
    search: String,
    onSearchChange: (String) -> Unit,
    selectedType: String,
    onTypeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            label = { Text("Поиск") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("Все", selectedType.isBlank()) { onTypeChange("") }
            FilterChip("Лекции", selectedType == "lecture") { onTypeChange("lecture") }
            FilterChip("Практики", selectedType == "practice") { onTypeChange("practice") }
        }
    }
}

@Composable
private fun JournalContextCard(context: JournalContext, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(context.periodName, color = SecondaryText)
                Text(context.disciplineName, color = PrimaryText, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Badge(lessonTypeName(context.lessonType))
        }
        InfoLine("Группа", context.groupName)
        InfoLine("Преподаватель", context.teacherName.ifBlank { "Не указан" })
        InfoLine("Занятий", "${context.lessonCount}, проведено ${context.heldCount}")
        SecondaryButton(text = "Открыть", onClick = onOpen)
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
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Редактирование", color = PrimaryText, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Описание") }, modifier = Modifier.fillMaxWidth())
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
                .padding(20.dp),
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                WebFormField(label = "Дисциплина") {
                    SelectCard("", disciplines.map { it.id to it.name }, disciplineId) { disciplineId = it }
                }
                WebFormField(label = "Название") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("КТП по дисциплине") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                WebFormField(label = "Описание") {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Семестр, поток, комментарии") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TopicsEditor(topics = topics, onTopicsChange = { topics = it })
            }

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
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = topic.lessonCount.toString(),
                        onValueChange = { value -> onTopicsChange(topics.replaceAt(index, topic.copy(lessonCount = value.toIntOrNull()?.coerceAtLeast(1) ?: 1))) },
                        label = { Text("Занятий") },
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(text = "Удалить", onClick = { onTopicsChange(topics.filterIndexed { topicIndex, _ -> topicIndex != index }.reindexTopics()) }, color = DangerColor)
                }
                OutlinedTextField(
                    value = topic.description,
                    onValueChange = { onTopicsChange(topics.replaceAt(index, topic.copy(description = it))) },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SelectCard(label: String, options: List<Pair<String, String>>, selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = PrimaryText, fontWeight = FontWeight.SemiBold)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (options.size > 4) 168.dp else ((options.size.coerceAtLeast(1) * 44).dp)),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(options) { option ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (selected == option.first) AccentBlue else LightBlue, RoundedCornerShape(12.dp))
                        .clickable { onSelected(option.first) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(option.second, color = if (selected == option.first) Color.White else PrimaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
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
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
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

@Composable
private fun InfoLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = SecondaryText)
        Text(value, color = PrimaryText, fontWeight = FontWeight.SemiBold)
    }
}

private fun buildJournalContexts(lessons: List<TeacherLesson>, periodNames: Map<String, String>): List<JournalContext> {
    val contexts = linkedMapOf<String, JournalContext>()
    lessons.forEach { lesson ->
        val groupId = lesson.groupId ?: return@forEach
        val disciplineId = lesson.disciplineId ?: return@forEach
        val periodId = lesson.periodId ?: return@forEach
        val key = listOf(groupId, disciplineId, periodId, lesson.lessonType, lesson.teacherId.orEmpty()).joinToString(":")
        val current = contexts[key]
        if (current == null) {
            contexts[key] = JournalContext(
                key = key,
                groupId = groupId,
                groupName = lesson.groupName,
                disciplineId = disciplineId,
                disciplineName = lesson.disciplineName,
                periodId = periodId,
                periodName = periodNames[periodId] ?: "Период не указан",
                teacherId = lesson.teacherId,
                teacherName = lesson.teacherName.orEmpty(),
                lessonType = lesson.lessonType,
                lessonCount = 1,
                heldCount = if (lesson.status == "held") 1 else 0,
                lastLesson = lesson.scheduledAt
            )
        } else {
            contexts[key] = current.copy(
                lessonCount = current.lessonCount + 1,
                heldCount = current.heldCount + if (lesson.status == "held") 1 else 0,
                lastLesson = maxOf(current.lastLesson, lesson.scheduledAt),
                teacherName = current.teacherName.ifBlank { lesson.teacherName.orEmpty() }
            )
        }
    }
    return contexts.values.sortedByDescending { it.lastLesson }
}

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

private data class JournalContext(
    val key: String,
    val groupId: String,
    val groupName: String,
    val disciplineId: String,
    val disciplineName: String,
    val periodId: String,
    val periodName: String,
    val teacherId: String?,
    val teacherName: String,
    val lessonType: String,
    val lessonCount: Int,
    val heldCount: Int,
    val lastLesson: String
) {
    fun toTarget(): MethodistJournalTarget = MethodistJournalTarget(groupId, disciplineId, periodId, teacherId.orEmpty(), lessonType)
}

data class MethodistJournalTarget(
    val groupId: String,
    val disciplineId: String,
    val periodId: String,
    val teacherId: String,
    val lessonType: String
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
