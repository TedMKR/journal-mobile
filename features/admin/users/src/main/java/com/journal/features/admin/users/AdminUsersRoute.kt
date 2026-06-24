package com.journal.features.admin.users

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
import com.journal.core.common.config.PersonNameFormatter
import com.journal.core.common.config.PersonNameParts
import com.journal.core.common.config.userFacingMessage
import com.journal.core.model.teacher.AdminAccessBinding
import com.journal.core.model.teacher.AdminActionRequest
import com.journal.core.model.teacher.AdminJournalContext
import com.journal.core.model.teacher.AdminPeriod
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
import com.journal.core.ui.shareXlsxFile
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

// ─── Colors ───────────────────────────────────────────────────────────────────

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

// ─── Helpers ──────────────────────────────────────────────────────────────────


private const val USERS_PAGE_SIZE = 20
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

// ─── Shared UI components ─────────────────────────────────────────────────────

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

// ─── 1. Admin Dashboard ───────────────────────────────────────────────────────

@Composable
fun AdminUsersRoute(
    journalApi: JournalApi,
    viewModel: AdminUsersViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var actionError by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var filterRole by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }

    // Edit dialog state
    var editingUser by remember { mutableStateOf<AdminUser?>(null) }
    var editFullName by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editUsername by remember { mutableStateOf("") }
    var editFirstName by remember { mutableStateOf("") }
    var editLastName by remember { mutableStateOf("") }
    var editPatronymic by remember { mutableStateOf("") }
    var editSyncLocked by remember { mutableStateOf(false) }

    // Block/unblock dialog state
    var blockingUser by remember { mutableStateOf<AdminUser?>(null) }

    fun loadUsers() {
        actionError = null
        viewModel.loadUsers(
            page = page,
            pageSize = USERS_PAGE_SIZE,
            query = search,
            userType = filterRole,
            status = filterStatus
        )
    }

    LaunchedEffect(page, search, filterRole, filterStatus) { loadUsers() }

    val totalPages = maxOf(1, (uiState.total + USERS_PAGE_SIZE - 1) / USERS_PAGE_SIZE)
    val currentError = actionError ?: uiState.error

    fun exportUsers() {
        shareXlsxFile(
            context = context,
            rows = buildUsersRows(uiState.users),
            fileName = "admin-users.xlsx",
            sheetName = "Пользователи",
            chooserTitle = "Экспорт пользователей"
        )
    }
    // Block/unblock confirmation dialog
    blockingUser?.let { user ->
        val isBlocking = user.status != "blocked"
        ReasonDialog(
            title = if (isBlocking) "Заблокировать пользователя?" else "Разблокировать пользователя?",
            onConfirm = { reason ->
                scope.launch {
                    runCatching {
                        if (isBlocking) {
                            journalApi.blockAdminUser(user.id, AdminActionRequest(reason))
                        } else {
                            journalApi.unblockAdminUser(user.id, AdminActionRequest(reason))
                        }
                    }.onFailure { actionError = it.userFacingMessage("Не удалось изменить пользователя") }
                    blockingUser = null
                    loadUsers()
                }
            },
            onDismiss = { blockingUser = null }
        )
    }

    // Edit user dialog
    editingUser?.let {
        Dialog(onDismissRequest = { editingUser = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Редактировать пользователя",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "×",
                        modifier = Modifier
                            .clickable { editingUser = null }
                            .padding(4.dp),
                        color = SecondaryText,
                        fontSize = 22.sp
                    )
                }
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EditField("ФИО", editFullName) { editFullName = it }
                    EditField("Email", editEmail) { editEmail = it }
                    EditField("Логин", editUsername) { editUsername = it }
                    EditField("Имя", editFirstName) { editFirstName = it }
                    EditField("Фамилия", editLastName) { editLastName = it }
                    EditField("Отчество", editPatronymic) { editPatronymic = it }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = editSyncLocked,
                            onCheckedChange = { editSyncLocked = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Не перезаписывать профиль из IAM",
                            fontSize = 13.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    TextButton(onClick = { editingUser = null }) {
                        Text("Отмена", color = SecondaryText, fontWeight = FontWeight.SemiBold)
                    }
                    PrimaryButton(
                        text = "Сохранить",
                        onClick = {
                            scope.launch {
                                editingUser?.let { user ->
                                    runCatching {
                                        journalApi.updateAdminUser(
                                            user.id,
                                            AdminUpdateUserRequest(
                                                fullName = editFullName.trim().ifBlank { null },
                                                firstName = editFirstName.trim().ifBlank { null },
                                                lastName = editLastName.trim().ifBlank { null },
                                                patronymic = editPatronymic.trim().ifBlank { null },
                                                email = editEmail.trim().ifBlank { null },
                                                username = editUsername.trim().ifBlank { null },
                                                profileSyncLocked = editSyncLocked
                                            )
                                        )
                                    }.onFailure { actionError = it.userFacingMessage("Не удалось сохранить пользователя") }
                                    editingUser = null
                                    loadUsers()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // Filter bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = {
                    search = it
                    page = 1
                },
                label = { Text("Поиск по ФИО, email или логину") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = InputTextColor,
                    unfocusedTextColor = InputTextColor,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = LightBlue,
                    focusedLabelColor = PrimaryBlue
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminDropdown(
                    label = "Все роли",
                    selected = filterRole,
                    options = listOf(
                        "Все роли" to "",
                        "Преподаватели" to "teacher",
                        "Студенты" to "student"
                    ),
                    onSelected = { filterRole = it; page = 1 },
                    modifier = Modifier.weight(1f)
                )
                AdminDropdown(
                    label = "Все статусы",
                    selected = filterStatus,
                    options = listOf(
                        "Все статусы" to "",
                        "Активные" to "active",
                        "Заблокированные" to "blocked"
                    ),
                    onSelected = { filterStatus = it; page = 1 },
                    modifier = Modifier.weight(1f)
                )
            }
            SecondaryButton(
                text = "Экспорт XLSX",
                onClick = ::exportUsers,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Content
        if (uiState.isOffline) {
            OfflineNotice("Показаны сохраненные пользователи. Редактирование и блокировка доступны после восстановления сети.")
        }
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }

            currentError != null -> Column(modifier = Modifier.padding(16.dp)) {
                ErrorCard(currentError)
            }

            uiState.users.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Пользователи не найдены", color = SecondaryText, fontWeight = FontWeight.SemiBold)
            }

            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.users) { user ->
                    UserRow(
                        user = user,
                        onEdit = {
                            if (uiState.isOffline) {
                                actionError = "Для редактирования пользователя требуется подключение к сети"
                            } else {
                                editingUser = user
                                editFullName = user.displayName().takeIf { it != user.username && it != user.email } ?: (user.fullName ?: "")
                                editEmail = user.email ?: ""
                                editUsername = user.username ?: ""
                                editFirstName = user.firstName ?: ""
                                editLastName = user.lastName ?: ""
                                editPatronymic = user.patronymic ?: ""
                                editSyncLocked = user.profileSyncLocked ?: false
                            }
                        },
                        onToggleBlock = {
                            if (uiState.isOffline) {
                                actionError = "Для блокировки пользователя требуется подключение к сети"
                            } else {
                                blockingUser = user
                            }
                        }
                    )
                }

                // Pagination
                if (totalPages > 1) {
                    item {
                        AdminPaginationRow(
                            page = page - 1,
                            totalPages = totalPages,
                            onPrev = { if (page > 1) page-- },
                            onNext = { if (page < totalPages) page++ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineNotice(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(WarningLight)
            .padding(12.dp)
    ) {
        Text(text, color = Color(0xFF7A4F00), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun UserRow(
    user: AdminUser,
    onEdit: () -> Unit,
    onToggleBlock: () -> Unit
) {
    val isBlocked = user.status == "blocked"
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
                    text = user.displayName(),
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = user.email ?: user.username ?: "—",
                    color = SecondaryText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!user.groupName.isNullOrBlank()) {
                    Text(
                        text = "Группа: ${user.groupName}",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AdminBadge(
                    text = if (user.userType == "teacher") "Преподаватель" else "Студент",
                    color = PrimaryBlue,
                    background = AccentBadge
                )
                AdminBadge(
                    text = if (isBlocked) "Заблокирован" else "Активен",
                    color = if (isBlocked) DangerColor else GreenColor,
                    background = if (isBlocked) DangerLight else GreenLight
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecondaryButton(
                text = "Редактировать",
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            )
            if (isBlocked) {
                PrimaryButton("Разблокировать", onToggleBlock, modifier = Modifier.weight(1f))
            } else {
                DangerButton("Заблокировать", onToggleBlock, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EditField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = InputTextColor,
            unfocusedTextColor = InputTextColor,
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = LightBlue,
            focusedLabelColor = PrimaryBlue
        )
    )
}

private fun AdminUser.displayName(): String =
    PersonNameFormatter.format(
        PersonNameParts(
            fullName = fullName,
            firstName = firstName,
            lastName = lastName,
            middleName = patronymic,
            username = username ?: email
        )
    ).ifBlank { email ?: "Без имени" }

private fun buildUsersRows(users: List<AdminUser>): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    rows += listOf("ID", "ФИО", "Роль", "Email", "Логин", "Группа", "Статус", "Создан")
    users.forEach { user ->
        rows += listOf(
            user.id,
            user.displayName(),
            user.userType.orEmpty(),
            user.email.orEmpty(),
            user.username.orEmpty(),
            user.groupName.orEmpty(),
            user.status.orEmpty(),
            user.createdAt.orEmpty()
        )
    }
    return rows
}
private const val AUDIT_PAGE_SIZE = 20

