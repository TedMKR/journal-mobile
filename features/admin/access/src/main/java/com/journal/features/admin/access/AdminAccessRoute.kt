package com.journal.features.admin.access

import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.journal.core.ui.AppTheme
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.journal.core.common.config.userFacingMessage
import com.journal.core.model.teacher.AcademicGroup
import com.journal.core.model.teacher.AdminAccessBinding
import com.journal.core.model.teacher.AdminActionRequest
import com.journal.core.model.teacher.AdminCreateAccessBindingRequest
import com.journal.core.model.teacher.AdminCreateTeachingAssignmentRequest
import com.journal.core.model.teacher.AdminJournalContext
import com.journal.core.model.teacher.AdminPeriod
import com.journal.core.model.teacher.AdminTeachingAssignment
import com.journal.core.model.teacher.AdminUpdateUserRequest
import com.journal.core.model.teacher.AdminUser
import com.journal.core.model.teacher.AuditEvent
import com.journal.core.model.teacher.Discipline
import com.journal.core.model.teacher.ProblemStudentEntry
import com.journal.core.model.teacher.ProblemStudentsMeta
import com.journal.core.model.teacher.TeacherProfile
import com.journal.core.network.api.JournalApi
import com.journal.core.ui.AppBackground
import com.journal.core.ui.AppBarBackground
import com.journal.core.ui.AppDanger
import com.journal.core.ui.AppDangerLight
import com.journal.core.ui.AppFieldBorder
import com.journal.core.ui.AppHeaderBackground
import com.journal.core.ui.AppInputText
import com.journal.core.ui.AppMutedText
import com.journal.core.ui.AppPrimary
import com.journal.core.ui.AppSuccess
import com.journal.core.ui.AppSuccessLight
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import com.journal.core.ui.AppSectionCard as SectionCard
import com.journal.core.ui.AppStatCard as StatCard
import com.journal.core.ui.AppAdminBadge as AdminBadge
import com.journal.core.ui.AppPrimaryButton as PrimaryButton
import com.journal.core.ui.AppSecondaryButton as SecondaryButton
import com.journal.core.ui.AppDangerButton as DangerButton
import com.journal.core.ui.AppMenuDropdown as AdminDropdown
import com.journal.core.ui.AppErrorCard as ErrorCard
import com.journal.core.ui.AppLoadingCard as LoadingCard
import com.journal.core.ui.AppPaginationRow as AdminPaginationRow

// в”Ђв”Ђв”Ђ Colors в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

private val BackgroundColor: Color
    @Composable get() = AppTheme.colors.background
private val CardBackground: Color
    @Composable get() = AppTheme.colors.surface
private val PrimaryBlue: Color
    @Composable get() = AppTheme.colors.primary
private val SecondaryText: Color
    @Composable get() = AppTheme.colors.mutedText
private val LightBlue: Color
    @Composable get() = AppTheme.colors.headerBackground
private val FieldBorder: Color
    @Composable get() = AppTheme.colors.fieldBorder
private val InputTextColor: Color
    @Composable get() = AppTheme.colors.inputText
private val DangerColor: Color
    @Composable get() = AppTheme.colors.danger
private val DangerLight = AppDangerLight
private val GreenColor: Color
    @Composable get() = AppTheme.colors.success
private val GreenLight = AppSuccessLight
private val AccentBadge: Color
    @Composable get() = AppTheme.colors.headerBackground
private val BarBackground: Color
    @Composable get() = AppTheme.colors.barBackground
private val WarningLight = Color(0xFFFEF3C7)

// в”Ђв”Ђв”Ђ Helpers в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

private fun formatDateTime(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    return try {
        val dt = OffsetDateTime.parse(value)
        dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    } catch (_: Exception) {
        value
    }
}

private fun adminActionLabel(action: String?): String = when (action) {
    "JOURNAL_LOCKED" -> "Журнал заморожен"
    "JOURNAL_UNLOCKED" -> "Журнал разморожен"
    "JOURNAL_ARCHIVED" -> "Журнал в архиве"
    "JOURNAL_RESTORED" -> "Журнал восстановлен"
    "PERIOD_CLOSED" -> "Период закрыт"
    "PERIOD_REOPENED" -> "Период открыт"
    "ACCESS_BINDING_CREATED" -> "Доступ выдан"
    "ACCESS_BINDING_REVOKED" -> "Доступ отозван"
    "AUDIT_EXPORTED" -> "Экспорт аудита"
    "USER_BLOCKED" -> "Пользователь заблокирован"
    "USER_UNBLOCKED" -> "Пользователь разблокирован"
    else -> action ?: "—"
}

// в”Ђв”Ђв”Ђ Shared UI components в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

@Composable
private fun ReasonDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "×",
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(4.dp),
                    color = SecondaryText,
                    fontSize = 22.sp
                )
            }
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Причина") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = InputTextColor,
                    unfocusedTextColor = InputTextColor,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = LightBlue,
                    focusedLabelColor = PrimaryBlue
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = SecondaryText, fontWeight = FontWeight.SemiBold)
                }
                PrimaryButton(
                    text = "Подтвердить",
                    onClick = { if (reason.isNotBlank()) onConfirm(reason.trim()) }
                )
            }
        }
    }
}

// в”Ђв”Ђв”Ђ 1. Admin Dashboard в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

private fun shortenId(id: String): String =
    if (id.length > 16) "…${id.takeLast(12)}" else id
@Composable
fun AdminAccessRoute(
    journalApi: JournalApi,
    viewModel: AdminAccessViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var actionError by remember { mutableStateOf<String?>(null) }
    var showRevoked by remember { mutableStateOf(false) }
    var revokingId by remember { mutableStateOf<String?>(null) }
    var teachers by remember { mutableStateOf<List<TeacherProfile>>(emptyList()) }
    var groups by remember { mutableStateOf<List<AcademicGroup>>(emptyList()) }
    var disciplines by remember { mutableStateOf<List<Discipline>>(emptyList()) }
    var periods by remember { mutableStateOf<List<AdminPeriod>>(emptyList()) }
    var assignments by remember { mutableStateOf<List<AdminTeachingAssignment>>(emptyList()) }
    var showAccessDialog by remember { mutableStateOf(false) }
    var showAssignmentDialog by remember { mutableStateOf(false) }
    var revokingAssignmentId by remember { mutableStateOf<String?>(null) }

    fun loadAccessData() {
        actionError = null
        viewModel.loadBindings(activeOnly = !showRevoked)
        scope.launch {
            runCatching {
                val teacherData = journalApi.getTeachers(limit = 500).data
                val groupData = journalApi.getGroups(limit = 500).data
                val disciplineData = journalApi.getDisciplines(limit = 500).data
                val periodData = journalApi.getAdminPeriods(includeClosed = true).data
                val assignmentData = journalApi.getAdminTeachingAssignments(activeOnly = !showRevoked).data
                CatalogBundle(teacherData, groupData, disciplineData, periodData, assignmentData)
            }.onSuccess { bundle ->
                teachers = bundle.teachers
                groups = bundle.groups
                disciplines = bundle.disciplines
                periods = bundle.periods
                assignments = bundle.assignments
            }.onFailure {
                actionError = it.userFacingMessage("Не удалось загрузить справочники доступов")
            }
        }
    }

    LaunchedEffect(showRevoked) { loadAccessData() }

    revokingId?.let { id ->
        Dialog(onDismissRequest = { revokingId = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Отозвать доступ?",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "?",
                        modifier = Modifier
                            .clickable { revokingId = null }
                            .padding(4.dp),
                        color = SecondaryText,
                        fontSize = 22.sp
                    )
                }
                Text("Это действие нельзя отменить.", color = SecondaryText, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    TextButton(onClick = { revokingId = null }) {
                        Text("Отмена", color = SecondaryText, fontWeight = FontWeight.SemiBold)
                    }
                    DangerButton(
                        text = "Отозвать",
                        onClick = {
                            scope.launch {
                                runCatching { journalApi.revokeAdminAccessBinding(id) }
                                    .onFailure { actionError = it.userFacingMessage("Не удалось отозвать доступ") }
                                revokingId = null
                                loadAccessData()
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAccessDialog) {
        AccessBindingDialog(
            teachers = teachers,
            groups = groups,
            disciplines = disciplines,
            periods = periods,
            onDismiss = { showAccessDialog = false },
            onSave = { granterId, granteeId, disciplineId, groupId, periodId, accessLevel ->
                scope.launch {
                    runCatching {
                        journalApi.createAdminAccessBinding(
                            AdminCreateAccessBindingRequest(
                                granterId = granterId,
                                granteeId = granteeId,
                                disciplineId = disciplineId,
                                groupId = groupId,
                                periodId = periodId,
                                accessLevel = accessLevel
                            )
                        )
                    }.onFailure { actionError = it.userFacingMessage("Не удалось выдать доступ") }
                    showAccessDialog = false
                    loadAccessData()
                }
            }
        )
    }

    if (showAssignmentDialog) {
        TeachingAssignmentDialog(
            teachers = teachers,
            groups = groups,
            disciplines = disciplines,
            periods = periods,
            onDismiss = { showAssignmentDialog = false },
            onSave = { teacherId, disciplineId, groupId, periodId ->
                scope.launch {
                    runCatching {
                        journalApi.createAdminTeachingAssignment(
                            AdminCreateTeachingAssignmentRequest(
                                teacherId = teacherId,
                                disciplineId = disciplineId,
                                groupId = groupId,
                                periodId = periodId
                            )
                        )
                    }.onFailure { actionError = it.userFacingMessage("Не удалось создать назначение") }
                    showAssignmentDialog = false
                    loadAccessData()
                }
            }
        )
    }

    revokingAssignmentId?.let { id ->
        Dialog(onDismissRequest = { revokingAssignmentId = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Отозвать назначение?", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 17.sp)
                Text("Преподаватель потеряет назначение на эту группу и дисциплину.", color = SecondaryText, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    TextButton(onClick = { revokingAssignmentId = null }) {
                        Text("Отмена", color = SecondaryText, fontWeight = FontWeight.SemiBold)
                    }
                    DangerButton(
                        text = "Отозвать",
                        onClick = {
                            scope.launch {
                                runCatching { journalApi.revokeAdminTeachingAssignment(id) }
                                    .onFailure { actionError = it.userFacingMessage("Не удалось отозвать назначение") }
                                revokingAssignmentId = null
                                loadAccessData()
                            }
                        }
                    )
                }
            }
        }
    }

    val currentError = actionError ?: uiState.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Показывать:", color = PrimaryBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            AdminDropdown(
                label = "Активные",
                selected = if (showRevoked) "all" else "active",
                options = listOf("Активные" to "active", "Все" to "all"),
                onSelected = { showRevoked = it == "all" },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground)
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryButton(
                text = "Выдать доступ",
                onClick = { showAccessDialog = true },
                enabled = teachers.isNotEmpty() && groups.isNotEmpty() && disciplines.isNotEmpty() && periods.isNotEmpty(),
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                text = "Назначить",
                onClick = { showAssignmentDialog = true },
                modifier = Modifier.weight(1f)
            )
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            currentError != null -> Column(Modifier.padding(16.dp)) { ErrorCard(currentError) }
            uiState.bindings.isEmpty() && assignments.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Доступы не найдены", color = SecondaryText, fontWeight = FontWeight.SemiBold)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (assignments.isNotEmpty()) {
                    item {
                        Text(
                            "Назначения преподавателей",
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    items(assignments) { assignment ->
                        TeachingAssignmentRow(
                            assignment = assignment,
                            onRevoke = { revokingAssignmentId = assignment.id }
                        )
                    }
                }
                if (uiState.bindings.isNotEmpty()) {
                    item {
                        Text(
                            "Делегированные доступы",
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
                items(uiState.bindings) { binding ->
                    AccessBindingRow(
                        binding = binding,
                        onRevoke = { revokingId = binding.id }
                    )
                }
            }
        }
    }
}

private data class CatalogBundle(
    val teachers: List<TeacherProfile>,
    val groups: List<AcademicGroup>,
    val disciplines: List<Discipline>,
    val periods: List<AdminPeriod>,
    val assignments: List<AdminTeachingAssignment>
)

@Composable
private fun AccessBindingDialog(
    teachers: List<TeacherProfile>,
    groups: List<AcademicGroup>,
    disciplines: List<Discipline>,
    periods: List<AdminPeriod>,
    onDismiss: () -> Unit,
    onSave: (granterId: String, granteeId: String, disciplineId: String, groupId: String, periodId: String, accessLevel: String) -> Unit
) {
    var granterId by remember(teachers) { mutableStateOf(teachers.firstOrNull()?.id.orEmpty()) }
    var granteeId by remember(teachers) { mutableStateOf(teachers.drop(1).firstOrNull()?.id ?: teachers.firstOrNull()?.id.orEmpty()) }
    var disciplineId by remember(disciplines) { mutableStateOf(disciplines.firstOrNull()?.id.orEmpty()) }
    var groupId by remember(groups) { mutableStateOf(groups.firstOrNull()?.id.orEmpty()) }
    var periodId by remember(periods) { mutableStateOf(periods.firstOrNull { it.isActive == true }?.id ?: periods.firstOrNull()?.id.orEmpty()) }
    var accessLevel by remember { mutableStateOf("read") }
    val canSave = granterId.isNotBlank() && granteeId.isNotBlank() && disciplineId.isNotBlank() && groupId.isNotBlank() && periodId.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Выдать доступ", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 17.sp)
            CatalogDropdown("Кто выдаёт", granterId, teachers.map { it.fullName to it.id }, { granterId = it })
            CatalogDropdown("Кому", granteeId, teachers.map { it.fullName to it.id }, { granteeId = it })
            CatalogDropdown("Дисциплина", disciplineId, disciplines.map { it.name to it.id }, { disciplineId = it })
            CatalogDropdown("Группа", groupId, groups.map { it.name to it.id }, { groupId = it })
            CatalogDropdown("Период", periodId, periods.map { (it.name ?: it.id) to it.id }, { periodId = it })
            CatalogDropdown("Уровень", accessLevel, listOf("Чтение" to "read", "Запись" to "write"), { accessLevel = it })
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = SecondaryText, fontWeight = FontWeight.SemiBold)
                }
                PrimaryButton(
                    text = "Сохранить",
                    enabled = canSave,
                    onClick = { onSave(granterId, granteeId, disciplineId, groupId, periodId, accessLevel) }
                )
            }
        }
    }
}

@Composable
private fun TeachingAssignmentDialog(
    teachers: List<TeacherProfile>,
    groups: List<AcademicGroup>,
    disciplines: List<Discipline>,
    periods: List<AdminPeriod>,
    onDismiss: () -> Unit,
    onSave: (teacherId: String, disciplineId: String, groupId: String, periodId: String) -> Unit
) {
    var teacherId by remember(teachers) { mutableStateOf(teachers.firstOrNull()?.id.orEmpty()) }
    var disciplineId by remember(disciplines) { mutableStateOf(disciplines.firstOrNull()?.id.orEmpty()) }
    var groupId by remember(groups) { mutableStateOf(groups.firstOrNull()?.id.orEmpty()) }
    var periodId by remember(periods) { mutableStateOf(periods.firstOrNull { it.isActive == true }?.id ?: periods.firstOrNull()?.id.orEmpty()) }
    val canSave = teacherId.isNotBlank() && disciplineId.isNotBlank() && groupId.isNotBlank() && periodId.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Назначить преподавателя", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 17.sp)
            CatalogDropdown("Преподаватель", teacherId, teachers.map { it.fullName to it.id }, { teacherId = it })
            CatalogDropdown("Дисциплина", disciplineId, disciplines.map { it.name to it.id }, { disciplineId = it })
            CatalogDropdown("Группа", groupId, groups.map { it.name to it.id }, { groupId = it })
            CatalogDropdown("Период", periodId, periods.map { (it.name ?: it.id) to it.id }, { periodId = it })
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = SecondaryText, fontWeight = FontWeight.SemiBold)
                }
                PrimaryButton(
                    text = "Сохранить",
                    enabled = canSave,
                    onClick = { onSave(teacherId, disciplineId, groupId, periodId) }
                )
            }
        }
    }
}

@Composable
private fun CatalogDropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    AdminDropdown(
        label = label,
        selected = selected,
        options = options.ifEmpty { listOf("Нет данных" to "") },
        onSelected = onSelected,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TeachingAssignmentRow(
    assignment: AdminTeachingAssignment,
    onRevoke: () -> Unit
) {
    val isRevoked = assignment.revokedAt != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = assignment.teacherName ?: shortenId(assignment.teacherId ?: "—"),
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontSize = 14.sp
                )
                Text("Дисциплина: ${assignment.disciplineName ?: shortenId(assignment.disciplineId ?: "—")}", color = SecondaryText, fontSize = 12.sp)
                Text("Группа: ${assignment.groupName ?: shortenId(assignment.groupId ?: "—")}", color = SecondaryText, fontSize = 12.sp)
                Text("Период: ${assignment.periodName ?: shortenId(assignment.periodId ?: "—")}", color = SecondaryText, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            AdminBadge(
                text = if (isRevoked) "Отозвано" else "Активно",
                color = if (isRevoked) DangerColor else GreenColor,
                background = if (isRevoked) DangerLight else GreenLight
            )
        }
        if (!isRevoked) {
            Spacer(modifier = Modifier.height(10.dp))
            DangerButton("Отозвать", onClick = onRevoke)
        }
    }
}

@Composable
private fun AccessBindingRow(
    binding: AdminAccessBinding,
    onRevoke: () -> Unit
) {
    val isRevoked = binding.revokedAt != null
    val accessLabel = if (binding.accessLevel == "write") "Запись" else "Чтение"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Получатель: ${shortenId(binding.granteeId ?: "—")}",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Выдал: ${shortenId(binding.granterId ?: "—")}",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
                Text(
                    text = "Дисциплина: ${shortenId(binding.disciplineId ?: "—")}",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
                Text(
                    text = "Группа: ${shortenId(binding.groupId ?: "—")}",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
                Text(
                    text = "Выдан: ${formatDateTime(binding.grantedAt)}",
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AdminBadge(accessLabel, PrimaryBlue, AccentBadge)
                AdminBadge(
                    text = if (isRevoked) "Отозван" else "Активен",
                    color = if (isRevoked) DangerColor else GreenColor,
                    background = if (isRevoked) DangerLight else GreenLight
                )
            }
        }
        if (!isRevoked) {
            Spacer(modifier = Modifier.height(10.dp))
            DangerButton("Отозвать", onClick = onRevoke)
        }
    }
}

// в”Ђв”Ђв”Ђ Shared pagination row в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

// в”Ђв”Ђв”Ђ 7. Admin Problem Students в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

private const val PROBLEM_STUDENTS_PAGE_SIZE = 10

