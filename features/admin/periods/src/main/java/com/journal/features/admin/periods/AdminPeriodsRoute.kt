package com.journal.features.admin.periods

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
import com.journal.core.model.teacher.AdminAccessBinding
import com.journal.core.model.teacher.AdminActionRequest
import com.journal.core.model.teacher.AdminCreatePeriodRequest
import com.journal.core.model.teacher.AdminJournalContext
import com.journal.core.model.teacher.AdminPeriod
import com.journal.core.model.teacher.AdminUpdatePeriodRequest
import com.journal.core.model.teacher.AdminUpdateUserRequest
import com.journal.core.model.teacher.AdminUser
import com.journal.core.model.teacher.AuditEvent
import com.journal.core.model.teacher.ProblemStudentEntry
import com.journal.core.model.teacher.ProblemStudentsMeta
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

private fun formatDate(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    return try {
        val dt = OffsetDateTime.parse(value)
        dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    } catch (_: Exception) {
        value
    }
}

@Composable
fun AdminPeriodsRoute(
    journalApi: JournalApi,
    viewModel: AdminPeriodsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var actionError by remember { mutableStateOf<String?>(null) }

    data class PendingToggle(val period: AdminPeriod, val closing: Boolean)
    var pendingToggle by remember { mutableStateOf<PendingToggle?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingPeriod by remember { mutableStateOf<AdminPeriod?>(null) }
    var deletingPeriod by remember { mutableStateOf<AdminPeriod?>(null) }

    fun loadPeriods() {
        actionError = null
        viewModel.loadPeriods()
    }

    LaunchedEffect(Unit) { loadPeriods() }

    pendingToggle?.let { pt ->
        ReasonDialog(
            title = if (pt.closing) "Закрыть период?" else "Открыть период?",
            onConfirm = { reason ->
                scope.launch {
                    runCatching {
                        if (pt.closing) {
                            journalApi.closeAdminPeriod(pt.period.id, AdminActionRequest(reason))
                        } else {
                            journalApi.reopenAdminPeriod(pt.period.id, AdminActionRequest(reason))
                        }
                    }.onFailure { actionError = it.userFacingMessage("Не удалось изменить период") }
                    pendingToggle = null
                    loadPeriods()
                }
            },
            onDismiss = { pendingToggle = null }
        )
    }

    if (showCreateDialog) {
        PeriodFormDialog(
            title = "Создать период",
            period = null,
            onDismiss = { showCreateDialog = false },
            onSave = { name, startsAt, endsAt, isActive, _ ->
                scope.launch {
                    runCatching {
                        journalApi.createAdminPeriod(
                            AdminCreatePeriodRequest(
                                name = name,
                                startsAt = startsAt,
                                endsAt = endsAt,
                                isActive = isActive
                            )
                        )
                    }.onFailure { actionError = it.userFacingMessage("Не удалось создать период") }
                    showCreateDialog = false
                    loadPeriods()
                }
            }
        )
    }

    editingPeriod?.let { period ->
        PeriodFormDialog(
            title = "Редактировать период",
            period = period,
            onDismiss = { editingPeriod = null },
            onSave = { name, startsAt, endsAt, isActive, reason ->
                scope.launch {
                    runCatching {
                        journalApi.updateAdminPeriod(
                            period.id,
                            AdminUpdatePeriodRequest(
                                name = name,
                                startsAt = startsAt,
                                endsAt = endsAt,
                                isActive = isActive,
                                reason = reason.ifBlank { null }
                            )
                        )
                    }.onFailure { actionError = it.userFacingMessage("Не удалось обновить период") }
                    editingPeriod = null
                    loadPeriods()
                }
            }
        )
    }

    deletingPeriod?.let { period ->
        ReasonDialog(
            title = "Удалить период?",
            onConfirm = { reason ->
                scope.launch {
                    runCatching { journalApi.deleteAdminPeriod(period.id, AdminActionRequest(reason)) }
                        .onFailure { actionError = it.userFacingMessage("Не удалось удалить период") }
                    deletingPeriod = null
                    loadPeriods()
                }
            },
            onDismiss = { deletingPeriod = null }
        )
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
            horizontalArrangement = Arrangement.End
        ) {
            PrimaryButton("Создать период", onClick = { showCreateDialog = true })
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            currentError != null -> Column(Modifier.padding(16.dp)) { ErrorCard(currentError) }
            uiState.periods.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Периоды не найдены", color = SecondaryText, fontWeight = FontWeight.SemiBold)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.periods) { period ->
                    PeriodAdminRow(
                        period = period,
                        onToggle = { pendingToggle = PendingToggle(period, !(period.isClosed ?: false)) },
                        onEdit = { editingPeriod = period },
                        onDelete = { deletingPeriod = period }
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodFormDialog(
    title: String,
    period: AdminPeriod?,
    onDismiss: () -> Unit,
    onSave: (name: String, startsAt: String, endsAt: String, isActive: Boolean, reason: String) -> Unit
) {
    var name by remember(period) { mutableStateOf(period?.name.orEmpty()) }
    var startsAt by remember(period) { mutableStateOf(period?.startsAt.orEmpty().take(10)) }
    var endsAt by remember(period) { mutableStateOf(period?.endsAt.orEmpty().take(10)) }
    var isActive by remember(period) { mutableStateOf(period?.isActive ?: false) }
    var reason by remember(period) { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 17.sp)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = startsAt,
                onValueChange = { startsAt = it },
                label = { Text("Дата начала, YYYY-MM-DD") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = endsAt,
                onValueChange = { endsAt = it },
                label = { Text("Дата окончания, YYYY-MM-DD") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                Text("Активный период", color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
            }
            if (period != null) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Причина изменения") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = SecondaryText, fontWeight = FontWeight.SemiBold)
                }
                PrimaryButton(
                    text = "Сохранить",
                    enabled = name.isNotBlank() && startsAt.isNotBlank() && endsAt.isNotBlank(),
                    onClick = { onSave(name.trim(), startsAt.trim(), endsAt.trim(), isActive, reason.trim()) }
                )
            }
        }
    }
}

@Composable
private fun PeriodAdminRow(
    period: AdminPeriod,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isClosed = period.isClosed ?: false
    val isActive = period.isActive ?: false

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = period.name ?: "—",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatDate(period.startsAt)} — ${formatDate(period.endsAt)}",
                    color = SecondaryText,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isActive) {
                    AdminBadge("Активный", PrimaryBlue, AccentBadge)
                }
                AdminBadge(
                    text = if (isClosed) "Закрыт" else "Открыт",
                    color = if (isClosed) DangerColor else GreenColor,
                    background = if (isClosed) DangerLight else GreenLight
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SecondaryButton("Изменить", onClick = onEdit, modifier = Modifier.weight(1f))
            if (isClosed) {
                PrimaryButton("Открыть", onClick = onToggle, modifier = Modifier.weight(1f))
            } else {
                DangerButton("Закрыть", onClick = onToggle, modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (!isActive) {
            DangerButton("Удалить", onClick = onDelete, modifier = Modifier.fillMaxWidth())
        }
    }
}

// в”Ђв”Ђв”Ђ 6. Admin Access Bindings в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

