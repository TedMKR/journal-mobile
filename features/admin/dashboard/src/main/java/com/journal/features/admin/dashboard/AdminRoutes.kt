package com.journal.features.admin.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.journal.core.model.teacher.AdminAccessBinding
import com.journal.core.model.teacher.AdminActionRequest
import com.journal.core.model.teacher.AdminJournalContext
import com.journal.core.model.teacher.AdminPeriod
import com.journal.core.model.teacher.AdminUpdateUserRequest
import com.journal.core.model.teacher.AdminUser
import com.journal.core.model.teacher.AuditEvent
import com.journal.core.network.api.JournalApi
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

// ─── Colors (shared with app palette) ────────────────────────────────────────

private val BackgroundColor = Color(0xFFEDEEED)
private val CardBackground = Color.White
private val PrimaryBlue = Color(0xFF223268)
private val SecondaryText = Color(0xFF6D7885)
private val LightBlue = Color(0xFFD3D7E1)
private val DangerColor = Color(0xFFC44A4A)
private val DangerLight = Color(0xFFFFE4E6)
private val GreenColor = Color(0xFF16A34A)
private val GreenLight = Color(0xFFDCFCE7)
private val AccentBadge = Color(0xFFEFF6FF)

// ─── Helpers ──────────────────────────────────────────────────────────────────

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
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(18.dp)
    ) {
        content()
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BackgroundColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = PrimaryBlue)
        Text(label, fontSize = 14.sp, color = SecondaryText)
    }
}

@Composable
private fun AdminBadge(text: String, color: Color, background: Color) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CardBackground,
            contentColor = PrimaryBlue
        )
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DangerColor)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AdminDropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>, // label to value
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayLabel = options.firstOrNull { it.second == selected }?.first ?: label
    Box {
        Text(
            text = displayLabel,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(BackgroundColor)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            color = PrimaryBlue,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (optLabel, optValue) ->
                DropdownMenuItem(
                    text = { Text(optLabel) },
                    onClick = {
                        onSelected(optValue)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ReasonDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = PrimaryBlue) },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Причина") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = LightBlue,
                    focusedLabelColor = PrimaryBlue
                )
            )
        },
        confirmButton = {
            PrimaryButton(
                text = "Подтвердить",
                onClick = { if (reason.isNotBlank()) onConfirm(reason.trim()) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = SecondaryText)
            }
        }
    )
}

@Composable
private fun ErrorCard(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DangerLight)
            .padding(14.dp),
        color = DangerColor,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun LoadingCard(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BackgroundColor)
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.width(20.dp).height(20.dp))
        Text(text, color = SecondaryText, fontWeight = FontWeight.SemiBold)
    }
}

// ─── 1. Admin Dashboard ───────────────────────────────────────────────────────

@Composable
fun AdminDashboardRoute(
    journalApi: JournalApi,
    onOpenUsers: () -> Unit,
    onOpenAudit: () -> Unit,
    onOpenJournals: () -> Unit,
    onOpenPeriods: () -> Unit,
    onOpenAccess: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var journalsCount by remember { mutableIntStateOf(0) }
    var usersCount by remember { mutableIntStateOf(0) }
    var periodsCount by remember { mutableIntStateOf(0) }
    var documentsCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        runCatching {
            val journals = journalApi.getAdminJournals(pageSize = 1)
            journalsCount = journals.meta?.total ?: journals.data.size

            val users = journalApi.getAdminUsers(pageSize = 1)
            usersCount = users.meta?.total ?: users.data.size

            val periods = journalApi.getAdminPeriods(includeClosed = true)
            periodsCount = periods.data.size

            val docs = journalApi.getAdminDocuments(pageSize = 1)
            documentsCount = docs.meta?.total ?: docs.data.size
        }.onFailure { error = it.message ?: "Не удалось загрузить данные" }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PrimaryBlue)
                .padding(20.dp)
        ) {
            Text(
                "Кабинет администратора",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.width(20.dp).height(20.dp))
                    Text("Загружаю данные...", color = Color.White.copy(alpha = 0.7f))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(12.dp)
                        ) {
                            Text(
                                journalsCount.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = PrimaryBlue
                            )
                            Text("Журналы", fontSize = 12.sp, color = SecondaryText)
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(12.dp)
                        ) {
                            Text(
                                usersCount.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = PrimaryBlue
                            )
                            Text("Пользователи", fontSize = 12.sp, color = SecondaryText)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(12.dp)
                        ) {
                            Text(
                                periodsCount.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = PrimaryBlue
                            )
                            Text("Периоды", fontSize = 12.sp, color = SecondaryText)
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(12.dp)
                        ) {
                            Text(
                                documentsCount.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = PrimaryBlue
                            )
                            Text("Документы", fontSize = 12.sp, color = SecondaryText)
                        }
                    }
                }
            }
        }

        error?.let { ErrorCard(it) }

        // Navigation cards
        Text("Разделы", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryBlue)

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Пользователи",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Локальные профили, блокировка и корректировка данных",
                        fontSize = 13.sp,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                PrimaryButton("Открыть", onOpenUsers)
            }
        }

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Аудит",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Журнал административных событий",
                        fontSize = 13.sp,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                PrimaryButton("Открыть", onOpenAudit)
            }
        }

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Журналы",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Заморозка, архивирование и восстановление журналов",
                        fontSize = 13.sp,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                PrimaryButton("Открыть", onOpenJournals)
            }
        }

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Учебные периоды",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Закрытие и повторное открытие периодов",
                        fontSize = 13.sp,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                PrimaryButton("Открыть", onOpenPeriods)
            }
        }

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Доступы",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Выдача и отзыв доступа преподавателей к журналам",
                        fontSize = 13.sp,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                PrimaryButton("Открыть", onOpenAccess)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─── 2. Admin Users ───────────────────────────────────────────────────────────

private const val USERS_PAGE_SIZE = 20

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
        AlertDialog(
            onDismissRequest = { editingUser = null },
            title = {
                Text("Редактировать пользователя", fontWeight = FontWeight.Bold, color = PrimaryBlue)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
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
            },
            confirmButton = {
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
            },
            dismissButton = {
                TextButton(onClick = { editingUser = null }) {
                    Text("Отмена", color = SecondaryText)
                }
            }
        )
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
                    onSelected = { filterRole = it; page = 1 }
                )
                AdminDropdown(
                    label = "Все статусы",
                    selected = filterStatus,
                    options = listOf(
                        "Все статусы" to "",
                        "Активные" to "active",
                        "Заблокированные" to "blocked"
                    ),
                    onSelected = { filterStatus = it; page = 1 }
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
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = LightBlue,
            focusedLabelColor = PrimaryBlue
        )
    )
}

// ─── 3. Admin Audit ───────────────────────────────────────────────────────────

private const val AUDIT_PAGE_SIZE = 20

@Composable
fun AdminAuditRoute(journalApi: JournalApi) {
    val scope = rememberCoroutineScope()

    var events by remember { mutableStateOf<List<AuditEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var filterAction by remember { mutableStateOf("") }
    var filterEntity by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }
    var total by remember { mutableIntStateOf(0) }

    fun loadAudit() {
        scope.launch {
            isLoading = true
            error = null
            runCatching {
                val resp = journalApi.getAdminAudit(
                    page = page,
                    pageSize = AUDIT_PAGE_SIZE,
                    action = filterAction.ifBlank { null },
                    entityType = filterEntity.ifBlank { null }
                )
                events = resp.data
                total = resp.meta?.total ?: resp.data.size
            }.onFailure { error = it.message ?: "Не удалось загрузить аудит" }
            isLoading = false
        }
    }

    LaunchedEffect(page, filterAction, filterEntity) { loadAudit() }

    val totalPages = maxOf(1, (total + AUDIT_PAGE_SIZE - 1) / AUDIT_PAGE_SIZE)

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
            Text("Фильтры", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AdminDropdown(
                        label = "Все события",
                        selected = filterAction,
                        options = listOf(
                            "Все события" to "",
                            "Журнал заморожен" to "JOURNAL_LOCKED",
                            "Журнал разморожен" to "JOURNAL_UNLOCKED",
                            "В архиве" to "JOURNAL_ARCHIVED",
                            "Восстановлен" to "JOURNAL_RESTORED",
                            "Период закрыт" to "PERIOD_CLOSED",
                            "Период открыт" to "PERIOD_REOPENED",
                            "Доступ выдан" to "ACCESS_BINDING_CREATED",
                            "Доступ отозван" to "ACCESS_BINDING_REVOKED",
                            "Заблокирован" to "USER_BLOCKED",
                            "Разблокирован" to "USER_UNBLOCKED"
                        ),
                        onSelected = { filterAction = it; page = 1 }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    AdminDropdown(
                        label = "Все сущности",
                        selected = filterEntity,
                        options = listOf(
                            "Все сущности" to "",
                            "Журнал" to "JournalContext",
                            "Пользователь" to "AdminUser",
                            "Период" to "AcademicPeriod",
                            "Документ" to "DocumentTask"
                        ),
                        onSelected = { filterEntity = it; page = 1 }
                    )
                }
            }
        }

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

            events.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("События не найдены", color = SecondaryText, fontWeight = FontWeight.SemiBold)
            }

            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(events) { event ->
                    AuditEventRow(event)
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
private fun AuditEventRow(event: AuditEvent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = adminActionLabel(event.action),
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (!event.entityType.isNullOrBlank()) {
                    Text(
                        text = "Сущность: ${event.entityType}",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }
                if (!event.entityId.isNullOrBlank()) {
                    Text(
                        text = event.entityId.orEmpty(),
                        color = SecondaryText,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDateTime(event.createdAt),
                    color = SecondaryText,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.actorId?.let { shortenId(it) } ?: "Система",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

private fun shortenId(id: String): String =
    if (id.length > 16) "…${id.takeLast(12)}" else id

// ─── 4. Admin Journals ────────────────────────────────────────────────────────

private const val JOURNALS_PAGE_SIZE = 20

private fun journalStatusLabel(status: String?): String = when (status) {
    "active" -> "Активен"
    "locked" -> "Заморожен"
    "archived" -> "Архив"
    else -> status ?: "—"
}

private fun journalStatusColor(status: String?): Color = when (status) {
    "active" -> GreenColor
    "locked" -> Color(0xFFB45309)
    "archived" -> SecondaryText
    else -> SecondaryText
}

private fun journalStatusBg(status: String?): Color = when (status) {
    "active" -> GreenLight
    "locked" -> Color(0xFFFEF3C7)
    "archived" -> Color(0xFFF3F4F6)
    else -> Color(0xFFF3F4F6)
}

private fun lessonTypeLabel(type: String?): String = when (type) {
    "lecture" -> "Лекция"
    "practice" -> "Практика"
    "lab" -> "Лабораторная"
    "seminar" -> "Семинар"
    "consultation" -> "Консультация"
    "exam" -> "Экзамен"
    else -> type ?: "—"
}

@Composable
fun AdminJournalsRoute(journalApi: JournalApi) {
    val scope = rememberCoroutineScope()

    var journals by remember { mutableStateOf<List<AdminJournalContext>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var filterStatus by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }
    var total by remember { mutableIntStateOf(0) }

    // Pending action dialog
    data class PendingAction(val journal: AdminJournalContext, val action: String)
    var pendingAction by remember { mutableStateOf<PendingAction?>(null) }

    fun loadJournals() {
        scope.launch {
            isLoading = true
            error = null
            runCatching {
                val resp = journalApi.getAdminJournals(
                    page = page,
                    pageSize = JOURNALS_PAGE_SIZE,
                    status = filterStatus.ifBlank { null }
                )
                journals = resp.data
                total = resp.meta?.total ?: resp.data.size
            }.onFailure { error = it.message ?: "Не удалось загрузить журналы" }
            isLoading = false
        }
    }

    LaunchedEffect(page, filterStatus) { loadJournals() }

    val totalPages = maxOf(1, (total + JOURNALS_PAGE_SIZE - 1) / JOURNALS_PAGE_SIZE)

    // Action confirmation dialog
    pendingAction?.let { pa ->
        val actionLabel = when (pa.action) {
            "lock" -> "Заморозит�� журнал"
            "unlock" -> "Разморозить журнал"
            "archive" -> "Архивировать журнал"
            "restore" -> "Восстановить журнал"
            else -> "Действие"
        }
        ReasonDialog(
            title = actionLabel,
            onConfirm = { reason ->
                scope.launch {
                    runCatching {
                        journalApi.adminJournalAction(pa.journal.id, pa.action, AdminActionRequest(reason))
                    }.onFailure { error = it.message }
                    pendingAction = null
                    loadJournals()
                }
            },
            onDismiss = { pendingAction = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // Filter bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Статус:", color = PrimaryBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            AdminDropdown(
                label = "Все",
                selected = filterStatus,
                options = listOf(
                    "Все" to "",
                    "Активные" to "active",
                    "Замороженные" to "locked",
                    "Архивные" to "archived"
                ),
                onSelected = { filterStatus = it; page = 1 }
            )
        }

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            error != null -> Column(Modifier.padding(16.dp)) { ErrorCard(error!!) }
            journals.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Журналы не найдены", color = SecondaryText, fontWeight = FontWeight.SemiBold)
            }
            else -> LazyColumn(modifier = Modifier.weight(1f)) {
                items(journals) { journal ->
                    JournalAdminRow(
                        journal = journal,
                        onAction = { action ->
                            pendingAction = PendingAction(journal, action)
                        }
                    )
                }
                if (totalPages > 1) {
                    item { AdminPaginationRow(page, totalPages, onPrev = { page-- }, onNext = { page++ }) }
                }
            }
        }
    }
}

@Composable
private fun JournalAdminRow(
    journal: AdminJournalContext,
    onAction: (String) -> Unit
) {
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
                    text = journal.disciplineName ?: "—",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        journal.groupName?.let { append(it) }
                        journal.teacherName?.let { append(" · $it") }
                    }.ifBlank { "—" },
                    color = SecondaryText,
                    fontSize = 13.sp
                )
                if (!journal.periodName.isNullOrBlank()) {
                    Text(journal.periodName.orEmpty(), color = SecondaryText, fontSize = 12.sp)
                }
                Text(
                    text = "${lessonTypeLabel(journal.lessonType)} · ${journal.lessonCount} зан.",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            AdminBadge(
                text = journalStatusLabel(journal.status),
                color = journalStatusColor(journal.status),
                background = journalStatusBg(journal.status)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (journal.status) {
                "active" -> {
                    SecondaryButton("Заморозить", onClick = { onAction("lock") })
                    DangerButton("В архив", onClick = { onAction("archive") })
                }
                "locked" -> {
                    PrimaryButton("Разморозить", onClick = { onAction("unlock") })
                }
                "archived" -> {
                    PrimaryButton("Восстановить", onClick = { onAction("restore") })
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(BackgroundColor))
}

// ─── 5. Admin Periods ─────────────────────────────────────────────────────────

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
fun AdminPeriodsRoute(journalApi: JournalApi) {
    val scope = rememberCoroutineScope()

    var periods by remember { mutableStateOf<List<AdminPeriod>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    data class PendingToggle(val period: AdminPeriod, val closing: Boolean)
    var pendingToggle by remember { mutableStateOf<PendingToggle?>(null) }

    fun loadPeriods() {
        scope.launch {
            isLoading = true
            error = null
            runCatching {
                periods = journalApi.getAdminPeriods(includeClosed = true).data
            }.onFailure { error = it.message ?: "Не удалось загрузить периоды" }
            isLoading = false
        }
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
                    }.onFailure { error = it.message }
                    pendingToggle = null
                    loadPeriods()
                }
            },
            onDismiss = { pendingToggle = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            error != null -> Column(Modifier.padding(16.dp)) { ErrorCard(error!!) }
            periods.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Периоды не найдены", color = SecondaryText, fontWeight = FontWeight.SemiBold)
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(periods) { period ->
                    PeriodAdminRow(
                        period = period,
                        onToggle = { pendingToggle = PendingToggle(period, !(period.isClosed ?: false)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodAdminRow(period: AdminPeriod, onToggle: () -> Unit) {
    val isClosed = period.isClosed ?: false
    val isActive = period.isActive ?: false

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp)
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
        if (isClosed) {
            PrimaryButton("Открыть", onClick = onToggle)
        } else {
            DangerButton("Закрыть", onClick = onToggle)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(BackgroundColor))
}

// ─── 6. Admin Access Bindings ─────────────────────────────────────────────────

@Composable
fun AdminAccessRoute(journalApi: JournalApi) {
    val scope = rememberCoroutineScope()

    var bindings by remember { mutableStateOf<List<AdminAccessBinding>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showRevoked by remember { mutableStateOf(false) }
    var revokingId by remember { mutableStateOf<String?>(null) }

    fun loadBindings() {
        scope.launch {
            isLoading = true
            error = null
            runCatching {
                bindings = journalApi.getAdminAccessBindings(activeOnly = !showRevoked).data
            }.onFailure { error = it.message ?: "Не удалось загрузить доступы" }
            isLoading = false
        }
    }

    LaunchedEffect(showRevoked) { loadBindings() }

    // Revoke confirmation
    revokingId?.let { id ->
        AlertDialog(
            onDismissRequest = { revokingId = null },
            title = { Text("Отозвать доступ?", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
            text = { Text("Это действие нельзя отменить.", color = SecondaryText) },
            confirmButton = {
                DangerButton("Отозвать", onClick = {
                    scope.launch {
                        runCatching {
                            journalApi.revokeAdminAccessBinding(id)
                        }.onFailure { error = it.message }
                        revokingId = null
                        loadBindings()
                    }
                })
            },
            dismissButton = {
                TextButton(onClick = { revokingId = null }) {
                    Text("Отмена", color = SecondaryText)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // Toggle active/all
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
                onSelected = { showRevoked = it == "all" }
            )
        }

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            error != null -> Column(Modifier.padding(16.dp)) { ErrorCard(error!!) }
            bindings.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Доступы не найдены", color = SecondaryText, fontWeight = FontWeight.SemiBold)
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(bindings) { binding ->
                    AccessBindingRow(
                        binding = binding,
                        onRevoke = { revokingId = binding.id }
                    )
                }
            }
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
    Box(Modifier.fillMaxWidth().height(1.dp).background(BackgroundColor))
}

// ─── Shared pagination row ────────────────────────────────────────────────────

@Composable
private fun AdminPaginationRow(page: Int, totalPages: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground)
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { if (page > 1) onPrev() }, enabled = page > 1) {
            Text("← Назад", color = if (page > 1) PrimaryBlue else SecondaryText)
        }
        Text(
            "$page / $totalPages",
            modifier = Modifier.padding(horizontal = 16.dp),
            color = PrimaryBlue,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = { if (page < totalPages) onNext() }, enabled = page < totalPages) {
            Text("Вперёд →", color = if (page < totalPages) PrimaryBlue else SecondaryText)
        }
    }
}
