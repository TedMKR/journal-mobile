package com.journal.features.methodist.journals

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import com.journal.core.common.config.PersonNameFormatter
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
import com.journal.core.ui.AppDropdown
import com.journal.core.ui.AppFieldPlaceholder
import com.journal.core.ui.AppHeaderBackground
import com.journal.core.ui.AppMutedText
import com.journal.core.ui.AppPrimary
import com.journal.core.ui.appFieldColors
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import com.journal.core.ui.AppFormField as WebFormField
import com.journal.core.ui.AppSelectCard as SelectCard
import com.journal.core.ui.AppMessageCards as MessageCards
import com.journal.core.ui.AppLoadingCard as LoadingCard
import com.journal.core.ui.AppStateCard as StateCard
import com.journal.core.ui.AppPrimaryButton as PrimaryButton
import com.journal.core.ui.AppTextActionButton as SecondaryButton
import com.journal.core.ui.AppFilterChip as FilterChip
import com.journal.core.ui.AppBadge as Badge

private val BackgroundColor = AppBackground
private val CardBackground = Color.White
private val PrimaryText = AppPrimary
private val SecondaryText = AppMutedText
private val LightBlue = AppHeaderBackground
private val AccentBlue = AppPrimary
private val DangerColor = AppDanger
private const val DashboardVisibleRows = 8

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
            textStyle = LocalTextStyle.current.copy(color = PrimaryText),
            colors = appFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        AppDropdown(
            label = "Период",
            options = listOf("" to "Все периоды") + periods.map { it.id to if (it.isActive) "${it.name} · активный" else it.name },
            selected = selectedPeriodId,
            onSelected = onPeriodChange
        )
        AppDropdown(
            label = "Дисциплина",
            options = listOf("" to "Все дисциплины") + disciplines.map { it.id to it.name },
            selected = selectedDisciplineId,
            onSelected = onDisciplineChange
        )
        AppDropdown(
            label = "Группа",
            options = listOf("" to "Все группы") + groups.map { it.id to it.name },
            selected = selectedGroupId,
            onSelected = onGroupChange
        )
        AppDropdown(
            label = "Преподаватель",
            options = listOf("" to "Все преподаватели") + teachers.map {
                it.id to PersonNameFormatter.formatFullName(it.fullName)
            },
            selected = selectedTeacherId,
            onSelected = onTeacherChange
        )
        AppDropdown(
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
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Список КТП", color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        MessageCards(error = error, success = success)
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            label = { Text("Поиск: название, дисциплина, описание") },
            textStyle = LocalTextStyle.current.copy(color = PrimaryText),
            colors = appFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        AppDropdown(
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
private fun JournalContextCard(context: JournalContext, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F8FB), RoundedCornerShape(16.dp))
            .border(1.dp, LightBlue, RoundedCornerShape(16.dp))
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
data class MethodistJournalTarget(
    val groupId: String,
    val disciplineId: String,
    val periodId: String,
    val teacherId: String,
    val lessonType: String
)

