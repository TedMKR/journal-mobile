package com.journal.features.admin.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.journal.core.model.teacher.AdminDocumentTask
import com.journal.core.network.api.JournalApi
import com.journal.core.ui.AppAdminBadge
import com.journal.core.ui.AppBackground
import com.journal.core.ui.AppDanger
import com.journal.core.ui.AppDangerLight
import com.journal.core.ui.AppErrorCard
import com.journal.core.ui.AppHeaderBackground
import com.journal.core.ui.AppMenuDropdown
import com.journal.core.ui.AppMutedText
import com.journal.core.ui.AppPaginationRow
import com.journal.core.ui.AppPrimary
import com.journal.core.ui.AppSecondaryButton
import com.journal.core.ui.AppSuccess
import com.journal.core.ui.AppSuccessLight
import com.journal.core.ui.shareBytesFile
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private const val PAGE_SIZE = 20
private val Warning = Color(0xFFB45309)
private val WarningLight = Color(0xFFFEF3C7)

@Composable
fun AdminDocumentsRoute(journalApi: JournalApi) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember { mutableIntStateOf(1) }
    var status by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var documents by remember { mutableStateOf<List<AdminDocumentTask>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            runCatching {
                journalApi.getAdminDocuments(
                    page = page,
                    pageSize = PAGE_SIZE,
                    status = status.ifBlank { null }
                )
            }.onSuccess { response ->
                documents = response.data
                total = response.meta?.total ?: response.data.size
            }.onFailure {
                error = it.userFacingMessage("Не удалось загрузить документы")
            }
            isLoading = false
        }
    }

    fun download(task: AdminDocumentTask) {
        scope.launch {
            runCatching {
                val bytes = journalApi.downloadAdminDocument(task.id).bytes()
                val fallbackName = task.result?.fileName ?: "document-${task.id.takeLast(8)}"
                val mimeType = task.result?.contentType ?: "application/octet-stream"
                shareBytesFile(
                    context = context,
                    bytes = bytes,
                    fileName = fallbackName,
                    mimeType = mimeType,
                    chooserTitle = "Экспорт документа"
                )
            }.onFailure {
                error = it.userFacingMessage("Не удалось скачать документ")
            }
        }
    }

    LaunchedEffect(page, status) { load() }

    val totalPages = maxOf(1, (total + PAGE_SIZE - 1) / PAGE_SIZE)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Статус:", color = AppPrimary, fontWeight = FontWeight.SemiBold)
            AppMenuDropdown(
                label = "Все",
                selected = status,
                options = listOf(
                    "Все" to "",
                    "Ожидают" to "pending",
                    "В работе" to "running",
                    "Готовы" to "done",
                    "Ошибка" to "failed",
                    "Окончательная ошибка" to "permanently_failed"
                ),
                onSelected = {
                    status = it
                    page = 1
                },
                modifier = Modifier.weight(1f)
            )
        }

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppPrimary)
            }
            error != null -> Column(Modifier.padding(16.dp)) { AppErrorCard(error.orEmpty()) }
            documents.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Документы не найдены", color = AppMutedText, fontWeight = FontWeight.SemiBold)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(documents) { task ->
                    DocumentRow(task = task, onDownload = { download(task) })
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

@Composable
private fun DocumentRow(
    task: AdminDocumentTask,
    onDownload: () -> Unit
) {
    val isDone = task.status == "done"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, AppHeaderBackground, RoundedCornerShape(16.dp))
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
                    text = task.result?.fileName ?: task.jobType ?: "Документ",
                    color = AppPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text("Создан: ${formatDateTime(task.createdAt)}", color = AppMutedText, fontSize = 13.sp)
                task.errorMsg?.takeIf { it.isNotBlank() }?.let { error ->
                    Text(error, color = AppDanger, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.width(10.dp))
            StatusBadge(task.status)
        }
        if (isDone) {
            AppSecondaryButton(
                text = "Скачать",
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String?) {
    val text = when (status) {
        "pending" -> "Ожидает"
        "running" -> "В работе"
        "done" -> "Готов"
        "failed" -> "Ошибка"
        "permanently_failed" -> "Ошибка"
        else -> status ?: "?"
    }
    val color = when (status) {
        "done" -> AppSuccess
        "failed", "permanently_failed" -> AppDanger
        "running" -> Warning
        else -> AppMutedText
    }
    val background = when (status) {
        "done" -> AppSuccessLight
        "failed", "permanently_failed" -> AppDangerLight
        "running" -> WarningLight
        else -> AppHeaderBackground
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
