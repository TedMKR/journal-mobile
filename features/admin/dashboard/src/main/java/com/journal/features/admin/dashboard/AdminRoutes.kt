package com.journal.features.admin.dashboard

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
import com.journal.core.ui.AppTheme
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance

import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journal.core.model.teacher.AdminAccessBinding
import com.journal.core.model.teacher.AdminActionRequest
import com.journal.core.model.teacher.AdminJournalContext
import com.journal.core.model.teacher.AdminPeriod
import com.journal.core.model.teacher.AdminUpdateUserRequest
import com.journal.core.model.teacher.AdminUser
import com.journal.core.model.teacher.AuditEvent
import com.journal.core.model.teacher.ProblemStudentEntry
import com.journal.core.model.teacher.ProblemStudentsMeta
import com.journal.core.ui.AppFieldBorder
import com.journal.core.ui.AppInputText
import com.journal.core.ui.AppMutedText
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
private val SummaryTileBackground: Color
    @Composable get() = AppTheme.colors.headerBackground
private val FieldBorder: Color
    @Composable get() = AppTheme.colors.fieldBorder
private val InputTextColor: Color
    @Composable get() = AppTheme.colors.inputText
private val DangerColor: Color
    @Composable get() = AppTheme.colors.danger
private val GreenColor: Color
    @Composable get() = AppTheme.colors.success
private val AccentBadge: Color
    @Composable get() = AppTheme.colors.headerBackground
private val BarBackground: Color
    @Composable get() = AppTheme.colors.barBackground
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

@Composable
private fun AdminOfflineBanner(modifier: Modifier = Modifier) {
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
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── 1. Admin Dashboard ───────────────────────────────────────────────────────

@Composable
fun AdminDashboardRoute(
    userId: String? = null,
    viewModel: AdminDashboardViewModel = hiltViewModel(),
    onOpenUsers: () -> Unit,
    onOpenAudit: () -> Unit,
    onOpenJournals: () -> Unit,
    onOpenPeriods: () -> Unit,
    onOpenAccess: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenImports: () -> Unit,
    onOpenSystem: () -> Unit,
    onOpenProblemStudents: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboard = uiState.dashboard
    val journalsCount = dashboard?.journalsCount ?: 0
    val usersCount = dashboard?.usersCount ?: 0
    val periodsCount = dashboard?.periodsCount ?: 0
    val documentsCount = dashboard?.documentsCount ?: 0
    val colors = AppTheme.colors
    val isDarkTheme = colors.background.luminance() < 0.5f
    val dashboardCardBackground = if (isDarkTheme) CardBackground else PrimaryBlue
    val dashboardTitleColor = if (isDarkTheme) PrimaryBlue else colors.onPrimary
    val dashboardLoadingTextColor = if (isDarkTheme) SecondaryText else colors.onPrimary.copy(alpha = 0.78f)
    val dashboardTileBackground = if (isDarkTheme) SummaryTileBackground else colors.onPrimary
    val dashboardTileValueColor = PrimaryBlue
    val dashboardTileLabelColor = if (isDarkTheme) SecondaryText else PrimaryBlue

    LaunchedEffect(userId) {
        viewModel.load(userId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(dashboardCardBackground)
                .padding(20.dp)
        ) {
            Text(
                "Кабинет администратора",
                color = dashboardTitleColor,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading && dashboard == null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = dashboardTitleColor, modifier = Modifier.width(20.dp).height(20.dp))
                    Text("Загружаю данные...", color = dashboardLoadingTextColor)
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
                                .background(dashboardTileBackground)
                                .padding(12.dp)
                        ) {
                            Text(
                                journalsCount.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = dashboardTileValueColor
                            )
                            Text("Журналы", fontSize = 12.sp, color = dashboardTileLabelColor)
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(dashboardTileBackground)
                                .padding(12.dp)
                        ) {
                            Text(
                                usersCount.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = dashboardTileValueColor
                            )
                            Text("Пользователи", fontSize = 12.sp, color = dashboardTileLabelColor)
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
                                .background(dashboardTileBackground)
                                .padding(12.dp)
                        ) {
                            Text(
                                periodsCount.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = dashboardTileValueColor
                            )
                            Text("Периоды", fontSize = 12.sp, color = dashboardTileLabelColor)
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(dashboardTileBackground)
                                .padding(12.dp)
                        ) {
                            Text(
                                documentsCount.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = dashboardTileValueColor
                            )
                            Text("Документы", fontSize = 12.sp, color = dashboardTileLabelColor)
                        }
                    }
                }
            }
        }

        if (uiState.isOffline) {
            AdminOfflineBanner()
        }
        uiState.error?.let { ErrorCard(it) }

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

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Документы",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "История формирования документов и скачивание готовых файлов",
                        fontSize = 13.sp,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                PrimaryButton("Открыть", onOpenDocuments)
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
                        "Импорт",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Шаблоны, история загрузок, применение и отмена импортов",
                        fontSize = 13.sp,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                PrimaryButton("Открыть", onOpenImports)
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
                        "Система",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Ссылка на Android-приложение и операции с резервными копиями",
                        fontSize = 13.sp,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                PrimaryButton("Открыть", onOpenSystem)
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
                        "Проблемные студенты",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Студенты с высокой долей двоек и серийными неудами",
                        fontSize = 13.sp,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                PrimaryButton("Открыть", onOpenProblemStudents)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─── 2. Admin Users ───────────────────────────────────────────────────────────

private const val USERS_PAGE_SIZE = 20

