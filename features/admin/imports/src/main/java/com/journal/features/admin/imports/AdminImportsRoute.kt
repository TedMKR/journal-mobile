package com.journal.features.admin.imports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.journal.core.common.config.userFacingMessage
import com.journal.core.model.teacher.AdminImportBatch
import com.journal.core.network.api.JournalApi
import com.journal.core.ui.AppAdminBadge
import com.journal.core.ui.AppDangerButton
import com.journal.core.ui.AppErrorCard
import com.journal.core.ui.AppMenuDropdown
import com.journal.core.ui.AppPaginationRow
import com.journal.core.ui.AppPrimaryButton
import com.journal.core.ui.AppSecondaryButton
import com.journal.core.ui.AppTheme
import com.journal.core.ui.shareBytesFile
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private const val PAGE_SIZE = 20
private val Warning = Color(0xFFB45309)
private val WarningLight = Color(0xFFFEF3C7)

@Composable
fun AdminImportsRoute(journalApi: JournalApi) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember { mutableIntStateOf(1) }
    var status by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var imports by remember { mutableStateOf<List<AdminImportBatch>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            runCatching {
                journalApi.getAdminImports(
                    page = page,
                    pageSize = PAGE_SIZE,
                    status = status.ifBlank { null }
                )
            }.onSuccess { response ->
                imports = response.data
                total = response.meta?.total ?: response.data.size
            }.onFailure {
                error = it.userFacingMessage("Не удалось загрузить историю импорта")
            }
            isLoading = false
        }
    }

    fun downloadTemplate(id: Int) {
        scope.launch {
            runCatching {
                val bytes = journalApi.downloadAdminImportTemplate(id).bytes()
                shareBytesFile(
                    context = context,
                    bytes = bytes,
                    fileName = "import-template-$id.xlsx",
                    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    chooserTitle = "Шаблон импорта"
                )
            }.onFailure {
                error = it.userFacingMessage("Не удалось скачать шаблон")
            }
        }
    }

    fun applyBatch(id: String) {
        scope.launch {
            error = null
            success = null
            runCatching { journalApi.applyAdminImport(id) }
                .onSuccess {
                    success = "Импорт поставлен на применение"
                    load()
                }
                .onFailure { error = it.userFacingMessage("Не удалось применить импорт") }
        }
    }

    fun cancelBatch(id: String) {
        scope.launch {
            error = null
            success = null
            runCatching { journalApi.cancelAdminImport(id) }
                .onSuccess {
                    success = "Импорт отменён"
                    load()
                }
                .onFailure { error = it.userFacingMessage("Не удалось отменить импорт") }
        }
    }

    LaunchedEffect(page, status) { load() }

    val totalPages = maxOf(1, (total + PAGE_SIZE - 1) / PAGE_SIZE)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Статус:", color = colors.primary, fontWeight = FontWeight.SemiBold)
                AppMenuDropdown(
                    label = "Все",
                    selected = status,
                    options = listOf(
                        "Все" to "",
                        "Черновики" to "preview_ready",
                        "Конфликты" to "conflicts_detected",
                        "Применяются" to "applying",
                        "Завершены" to "completed",
                        "Ошибка" to "failed",
                        "Отменены" to "cancelled"
                    ),
                    onSelected = {
                        status = it
                        page = 1
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            TemplateButtons(onDownload = ::downloadTemplate)
        }

        error?.let { AppErrorCard(it, modifier = Modifier.padding(12.dp)) }
        success?.let {
            Text(
                text = it,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .background(colors.successContainer, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                color = colors.success,
                fontWeight = FontWeight.SemiBold
            )
        }

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            imports.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("История импорта пуста", color = colors.mutedText, fontWeight = FontWeight.SemiBold)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(imports) { batch ->
                    ImportBatchRow(
                        batch = batch,
                        onApply = { applyBatch(batch.id) },
                        onCancel = { cancelBatch(batch.id) }
                    )
                }
                if (totalPages > 1) {
                    item {
                        AppPaginationRow(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateButtons(onDownload: (Int) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppSecondaryButton("Пользователи", onClick = { onDownload(1) })
        AppSecondaryButton("Состав группы", onClick = { onDownload(2) })
        AppSecondaryButton("Темы КТП", onClick = { onDownload(3) })
        AppSecondaryButton("Пример КТП", onClick = { onDownload(4) })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImportBatchRow(
    batch: AdminImportBatch,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = AppTheme.colors
    val canApply = batch.status in setOf("preview_ready", "conflicts_detected", "resolved")
    val canCancel = batch.status !in setOf("completed", "cancelled", "failed", "partial")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = batch.sourceFileName ?: batch.importType ?: "Импорт",
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Старт: ${formatDateTime(batch.startedAt)}", color = colors.mutedText, fontSize = 13.sp)
                batch.groupName?.let { Text("Группа: $it", color = colors.mutedText, fontSize = 13.sp) }
                batch.periodName?.let { Text("Период: $it", color = colors.mutedText, fontSize = 13.sp) }
            }
            Spacer(Modifier.width(10.dp))
            StatusBadge(batch.status)
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            batch.summary?.let { summary ->
                MetricChip("Всего ${summary.total}")
                MetricChip("Создано ${summary.created}")
                MetricChip("Обновлено ${summary.updated}")
                MetricChip("Пропущено ${summary.skipped}")
                MetricChip("Ошибки ${summary.errors}")
            }
            if (batch.conflictsCount > 0) MetricChip("Конфликты ${batch.conflictsCount}")
            if (batch.warningsCount > 0) MetricChip("Предупреждения ${batch.warningsCount}")
        }

        if (canApply || canCancel) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (canApply) {
                    AppPrimaryButton("Применить", onClick = onApply, modifier = Modifier.weight(1f))
                }
                if (canCancel) {
                    AppDangerButton("Отменить", onClick = onCancel, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricChip(text: String) {
    val colors = AppTheme.colors
    Text(
        text = text,
        modifier = Modifier
            .background(colors.surfaceVariant, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = colors.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun StatusBadge(status: String?) {
    val colors = AppTheme.colors
    val text = when (status) {
        "pending" -> "Ожидает"
        "validating" -> "Проверка"
        "preview_ready" -> "Черновик"
        "conflicts_detected" -> "Конфликты"
        "resolved" -> "Готов"
        "applying" -> "Применяется"
        "completed" -> "Завершён"
        "failed" -> "Ошибка"
        "partial" -> "Частично"
        "cancelled" -> "Отменён"
        else -> status ?: "?"
    }
    val color = when (status) {
        "completed" -> colors.success
        "failed", "cancelled" -> colors.danger
        "applying", "validating", "conflicts_detected", "partial" -> Warning
        else -> colors.primary
    }
    val background = when (status) {
        "completed" -> colors.successContainer
        "failed", "cancelled" -> colors.dangerContainer
        "applying", "validating", "conflicts_detected", "partial" -> WarningLight
        else -> colors.headerBackground
    }
    AppAdminBadge(text = text, color = color, background = background)
}

private fun formatDateTime(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    return try {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    } catch (_: Exception) {
        value
    }
}
