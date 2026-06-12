package com.journal.features.admin.users

import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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

// ─── Colors ───────────────────────────────────────────────────────────────────

private val BackgroundColor = AppBackground
private val CardBackground = Color.White
private val PrimaryBlue = AppPrimary
private val SecondaryText = AppMutedText
private val LightBlue = AppHeaderBackground
private val FieldBorder = AppFieldBorder
private val InputTextColor = AppInputText
private val DangerColor = AppDanger
private val DangerLight = AppDangerLight
private val GreenColor = Color(0xFF16A34A)
private val GreenLight = AppSuccessLight
private val AccentBadge = Color(0xFFEFF6FF)
private val BarBackground = AppBarBackground
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
                .background(Color.White, RoundedCornerShape(20.dp))
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
fun AdminUsersRoute(journalApi: JournalApi) {
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var filterRole by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }
    var total by remember { mutableIntStateOf(0) }

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
        scope.launch {
            isLoading = true
            error = null
            runCatching {
                val resp = journalApi.getAdminUsers(
                    page = page,
                    pageSize = USERS_PAGE_SIZE,
                    query = search.trim().ifBlank { null },
                    userType = filterRole.ifBlank { null },
                    status = filterStatus.ifBlank { null }
                )
                users = resp.data
                total = resp.meta?.total ?: resp.data.size
            }.onFailure { error = it.message ?: "Не удалось загрузить пользователей" }
            isLoading = false
        }
    }

    LaunchedEffect(page, search, filterRole, filterStatus) { loadUsers() }

    val totalPages = maxOf(1, (total + USERS_PAGE_SIZE - 1) / USERS_PAGE_SIZE)

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
                    }.onFailure { error = it.message }
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
                    .background(Color.White, RoundedCornerShape(20.dp))
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
                                    }.onFailure { error = it.message }
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
        }

        // Content
        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }

            error != null -> Column(modifier = Modifier.padding(16.dp)) {
                ErrorCard(error!!)
            }

            users.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Пользователи не найдены", color = SecondaryText, fontWeight = FontWeight.SemiBold)
            }

            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(users) { user ->
                    UserRow(
                        user = user,
                        onEdit = {
                            editingUser = user
                            editFullName = user.fullName ?: ""
                            editEmail = user.email ?: ""
                            editUsername = user.username ?: ""
                            editFirstName = user.firstName ?: ""
                            editLastName = user.lastName ?: ""
                            editPatronymic = user.patronymic ?: ""
                            editSyncLocked = user.profileSyncLocked ?: false
                        },
                        onToggleBlock = { blockingUser = user }
                    )
                }

                // Pagination
                if (totalPages > 1) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBackground)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { if (page > 1) page-- },
                                enabled = page > 1
                            ) {
                                Text("← Назад", color = if (page > 1) PrimaryBlue else SecondaryText)
                            }
                            Text(
                                "$page / $totalPages",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { if (page < totalPages) page++ },
                                enabled = page < totalPages
                            ) {
                                Text("Вперёд →", color = if (page < totalPages) PrimaryBlue else SecondaryText)
                            }
                        }
                    }
                }
            }
        }
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
            .background(CardBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName ?: user.username ?: user.email ?: "Без имени",
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(
                text = "Редактировать",
                onClick = onEdit
            )
            if (isBlocked) {
                PrimaryButton("Разблокировать", onToggleBlock)
            } else {
                DangerButton("Заблокировать", onToggleBlock)
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BackgroundColor)
    )
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

// ─── 3. Admin Audit ───────────────────────────────────────────────────────────

private const val AUDIT_PAGE_SIZE = 20

