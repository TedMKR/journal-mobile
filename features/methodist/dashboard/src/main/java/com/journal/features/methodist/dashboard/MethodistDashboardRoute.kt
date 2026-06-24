package com.journal.features.methodist.dashboard

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.journal.core.ui.AppDropdown
import com.journal.core.ui.AppFieldPlaceholder
import com.journal.core.ui.AppMutedText
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

private val BackgroundColor: Color
    @Composable get() = AppTheme.colors.background
private val CardBackground: Color
    @Composable get() = AppTheme.colors.surface
private val LessonBackground: Color
    @Composable get() = AppTheme.colors.lessonBackground
private val PrimaryText: Color
    @Composable get() = AppTheme.colors.primary
private val SecondaryText: Color
    @Composable get() = AppTheme.colors.mutedText
private val LightBlue: Color
    @Composable get() = AppTheme.colors.headerBackground
private val SummaryTileBackground: Color
    @Composable get() = AppTheme.colors.headerBackground
private val SummaryTileBar: Color
    @Composable get() = AppTheme.colors.barBackground
private val AccentBlue: Color
    @Composable get() = AppTheme.colors.primary
private const val DashboardVisibleRows = 8

@Composable
fun MethodistDashboardRoute(
    /** Full name extracted from JWT and normalized for the profile header. */
    jwtName: String? = null,
    userId: String? = null,
    viewModel: MethodistDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboard = uiState.dashboard
    val periods = dashboard?.periods.orEmpty()
    val disciplines = dashboard?.disciplines.orEmpty()
    val teachers = dashboard?.teachers.orEmpty()
    val groups = dashboard?.groups.orEmpty()
    val journals = dashboard?.journals.orEmpty()
    var search by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        viewModel.load(userId)
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
            val teacherName = PersonNameFormatter.formatFullName(teacher.fullName)
            query.isBlank() || listOf(teacherName, teacher.email.orEmpty()).any { it.lowercase().contains(query) }
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
            if (uiState.isLoading && dashboard == null) {
                LoadingCard("Загружаю информацию...")
            } else if (uiState.error != null && dashboard == null) {
                StateCard(uiState.error.orEmpty(), isError = true)
            } else {
                if (uiState.isOffline) {
                    MethodistOfflineBanner()
                }
                DashboardSummaryGrid(
                    disciplinesCount = disciplines.size,
                    teachersCount = teachers.size,
                    groupsCount = groups.size,
                    journalsCount = journals.size,
                    jwtName = jwtName
                )
                PeriodStrip(activePeriodName)
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
private fun DashboardSummaryGrid(
    disciplinesCount: Int,
    teachersCount: Int,
    groupsCount: Int,
    journalsCount: Int,
    jwtName: String? = null
) {
    val displayName = PersonNameFormatter.formatFullName(jwtName).takeIf(String::isNotBlank) ?: "Методист"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            displayName,
            color = PrimaryText,
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
            .background(SummaryTileBackground, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(SummaryTileBar, RoundedCornerShape(999.dp))
        )
        Text(label, color = PrimaryText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(value, color = AccentBlue, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MethodistOfflineBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF59E0B), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Офлайн - данные из кеша",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
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
            .background(CardBackground, RoundedCornerShape(16.dp))
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
            textStyle = LocalTextStyle.current.copy(color = PrimaryText),
            colors = appFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        DirectoryBlock(
            title = "Дисциплины",
            count = disciplines.size,
            rows = disciplines.map { DirectoryRow(it.name, it.code ?: "Код не указан") },
            emptyText = "Нет дисциплин"
        )
        DirectoryBlock(
            title = "Преподаватели",
            count = teachers.size,
            rows = teachers.map {
                DirectoryRow(PersonNameFormatter.formatFullName(it.fullName), it.email ?: "Email не указан")
            },
            emptyText = "Нет преподавателей"
        )
        DirectoryBlock(
            title = "Группы",
            count = groups.size,
            rows = groups.map { DirectoryRow(it.name, groupSubtitle(it)) },
            emptyText = "Нет групп"
        )
    }
}

@Composable
private fun DirectoryBlock(
    title: String,
    count: Int,
    rows: List<DirectoryRow>,
    emptyText: String
) {
    var expanded by remember(title) { mutableStateOf(false) }
    val hiddenCount = (rows.size - DashboardVisibleRows).coerceAtLeast(0)
    val visibleRows = if (expanded) rows else rows.take(DashboardVisibleRows)

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
            visibleRows.forEach { row -> DirectoryDataRow(row) }
        }
        if (hiddenCount > 0) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    text = if (expanded) "Свернуть" else "Показать еще $hiddenCount",
                    color = AccentBlue,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
            .background(LessonBackground, RoundedCornerShape(16.dp))
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
            .background(CardBackground, RoundedCornerShape(12.dp))
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
private data class DirectoryRow(
    val title: String,
    val subtitle: String
)


data class MethodistJournalTarget(
    val groupId: String,
    val disciplineId: String,
    val periodId: String,
    val teacherId: String,
    val lessonType: String
)

