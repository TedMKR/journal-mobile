package com.journal.features.admin.problemstudents

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.journal.core.common.config.PersonNameFormatter
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

// ─── 7. Admin Problem Students ────────────────────────────────────────────────

private const val PROBLEM_STUDENTS_PAGE_SIZE = 10

@Composable
fun AdminProblemStudentsRoute(journalApi: JournalApi) {
    val scope = rememberCoroutineScope()

    var students by remember { mutableStateOf<List<ProblemStudentEntry>>(emptyList()) }
    var meta by remember { mutableStateOf<ProblemStudentsMeta?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableIntStateOf(1) }
    var total by remember { mutableIntStateOf(0) }

    // Filter states — rememberSaveable сохраняет при повороте экрана
    var periodId by rememberSaveable { mutableStateOf("") }
    var groupId by rememberSaveable { mutableStateOf("") }
    var disciplineId by rememberSaveable { mutableStateOf("") }
    var minGrades by rememberSaveable { mutableStateOf("3") }
    var minFailingGrades by rememberSaveable { mutableStateOf("3") }
    var failingPercentThreshold by rememberSaveable { mutableStateOf("50") }

    // Filter options
    var periods by remember { mutableStateOf<List<AdminPeriod>>(emptyList()) }
    var groups by remember { mutableStateOf<List<com.journal.core.model.teacher.AcademicGroup>>(emptyList()) }
    var disciplines by remember { mutableStateOf<List<com.journal.core.model.teacher.Discipline>>(emptyList()) }

    fun loadStudents() {
        scope.launch {
            isLoading = true
            error = null
            runCatching {
                val resp = journalApi.getAdminProblemStudents(
                    periodId = periodId.ifBlank { null },
                    groupId = groupId.ifBlank { null },
                    disciplineId = disciplineId.ifBlank { null },
                    minGrades = minGrades.toIntOrNull(),
                    minFailingGrades = minFailingGrades.toIntOrNull(),
                    failingPercentThreshold = failingPercentThreshold.toIntOrNull(),
                    limit = PROBLEM_STUDENTS_PAGE_SIZE,
                    offset = (page - 1) * PROBLEM_STUDENTS_PAGE_SIZE
                )
                students = resp.data
                meta = resp.meta
                total = resp.meta.total
            }.onFailure { error = it.message ?: "Не удалось загрузить данные" }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            periods = journalApi.getAdminPeriods(includeClosed = true).data
            groups = journalApi.getGroups(limit = 200).data
            disciplines = journalApi.getDisciplines(limit = 200).data
        }
        loadStudents()
    }

    LaunchedEffect(page) { loadStudents() }

    val totalPages = maxOf(1, (total + PROBLEM_STUDENTS_PAGE_SIZE - 1) / PROBLEM_STUDENTS_PAGE_SIZE)

    val periodOptions = listOf("Все периоды" to "") +
        periods.map { (it.name ?: it.id) to it.id }
    val groupOptions = listOf("Все группы" to "") +
        groups.map { it.name to it.id }
    val disciplineOptions = listOf("Все дисциплины" to "") +
        disciplines.map { it.name to it.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // ── Фильтры ─────────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Фильтры", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 14.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminDropdown(
                        label = "Все периоды",
                        selected = periodId,
                        options = periodOptions,
                        onSelected = { periodId = it; page = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    AdminDropdown(
                        label = "Все группы",
                        selected = groupId,
                        options = groupOptions,
                        onSelected = { groupId = it; page = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                AdminDropdown(
                    label = "Все дисциплины",
                    selected = disciplineId,
                    options = disciplineOptions,
                    onSelected = { disciplineId = it; page = 1 },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProblemNumberField(
                        label = "Минимум оценок",
                        value = minGrades,
                        onChange = { minGrades = it },
                        modifier = Modifier.weight(1f)
                    )
                    ProblemNumberField(
                        label = "Двоек подряд",
                        value = minFailingGrades,
                        onChange = { minFailingGrades = it },
                        modifier = Modifier.weight(1f)
                    )
                    ProblemNumberField(
                        label = "Процент двоек",
                        value = failingPercentThreshold,
                        onChange = { failingPercentThreshold = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                PrimaryButton(
                    text = "Применить",
                    onClick = { page = 1; loadStudents() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ── Сводные карточки ────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatCard(
                    label = "Найдено",
                    value = total.toString(),
                    sub = "студентов",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatCard(
                    label = "Порог",
                    value = "${(meta?.failingPercentThreshold?.toInt() ?: failingPercentThreshold.toIntOrNull() ?: 50)}%",
                    sub = "доля двоек",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatCard(
                    label = "Серия",
                    value = (meta?.minFailingGrades ?: minFailingGrades.toIntOrNull() ?: 3).toString(),
                    sub = "подряд",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Ошибка ──────────────────────────────────────────────────────────
        error?.let { msg ->
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) { ErrorCard(msg) }
            }
        }

        // ── Загрузка ────────────────────────────────────────────────────────
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }
        }

        // ── Пусто ───────────────────────────────────────────────────────────
        if (!isLoading && students.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Проблемные студенты не найдены",
                        color = SecondaryText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── Список студентов ────────────────────────────────────────────────
        items(students) { student ->
            ProblemStudentCard(
                student = student,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
            )
        }

        // ── Пагинация ───────────────────────────────────────────────────────
        if (totalPages > 1) {
            item {
                AdminPaginationRow(
                    page = page,
                    totalPages = totalPages,
                    onPrev = { page-- },
                    onNext = { page++ }
                )
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .height(98.dp)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = SecondaryText,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = PrimaryBlue
        )
        Text(
            sub,
            fontSize = 11.sp,
            color = SecondaryText,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun ProblemNumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            color = SecondaryText,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
        OutlinedTextField(
            value = value,
            onValueChange = { new -> if (new.all { it.isDigit() }) onChange(new) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = InputTextColor,
                unfocusedTextColor = InputTextColor,
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = LightBlue,
                focusedLabelColor = PrimaryBlue
            )
        )
    }
}

@Composable
private fun ProblemStudentCard(student: ProblemStudentEntry, modifier: Modifier = Modifier) {
    val percent = (student.failingGradePercent / 100f).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground, RoundedCornerShape(16.dp))
            .border(1.dp, LightBlue, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Имя ─────────────────────────────────────────────────────────────
        Text(
            text = PersonNameFormatter.formatFullName(student.studentName).ifBlank { "—" },
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            modifier = Modifier.fillMaxWidth()
        )

        // ── Теги периода и группы ────────────────────────────────────────────
        if (!student.periodName.isNullOrBlank() || !student.groupName.isNullOrBlank()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!student.periodName.isNullOrBlank()) ProblemTag(student.periodName.orEmpty())
                if (!student.groupName.isNullOrBlank()) ProblemTag(student.groupName.orEmpty())
            }
        }

        // ── Дисциплина + счётчик двоек ──────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!student.disciplineName.isNullOrBlank()) {
                Text(
                    text = student.disciplineName.orEmpty(),
                    color = SecondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${student.failingGradeCount} из ${student.gradeCount} оценок — «2»",
                color = SecondaryText,
                fontSize = 13.sp
            )
        }

        // ── Максимальная серия ───────────────────────────────────────────────
        Text(
            text = "Максимальная серия: ${student.maxConsecutiveFailingGrades}",
            color = SecondaryText,
            fontSize = 13.sp
        )

        // ── Прогресс-бар ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(BarBackground, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent)
                    .height(8.dp)
                    .background(PrimaryBlue, RoundedCornerShape(4.dp))
            )
        }

        // ── Процент + стрелка ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${student.failingGradePercent.toInt()}% двоек",
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun ProblemTag(
    text: String,
    modifier: Modifier = Modifier,
    warning: Boolean = false
) {
    val bg = if (warning) WarningLight else LightBlue
    val textColor = if (warning) Color(0xFF92400E) else PrimaryBlue
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(10.dp))
            .border(1.dp, if (warning) Color(0xFFFCD34D) else LightBlue, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (warning) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
