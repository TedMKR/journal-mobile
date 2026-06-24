package com.journal.features.admin.system

import androidx.compose.foundation.background
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.journal.core.model.teacher.AdminBackupsResponse
import com.journal.core.model.teacher.BackupArtifact
import com.journal.core.model.teacher.MobileAppDownloadResponse
import com.journal.core.model.teacher.UpdateMobileAppDownloadRequest
import com.journal.core.network.api.JournalApi
import com.journal.core.ui.AppAdminBadge
import com.journal.core.ui.AppErrorCard
import com.journal.core.ui.AppPrimaryButton
import com.journal.core.ui.AppSecondaryButton
import com.journal.core.ui.AppSectionCard
import com.journal.core.ui.AppTheme
import com.journal.core.ui.shareBytesFile
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Composable
fun AdminSystemRoute(journalApi: JournalApi) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var appSettings by remember { mutableStateOf<MobileAppDownloadResponse?>(null) }
    var backups by remember { mutableStateOf(AdminBackupsResponse()) }
    var visible by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            runCatching {
                val settings = journalApi.getMobileAppDownload()
                val backupList = journalApi.getAdminBackups()
                settings to backupList
            }.onSuccess { (settings, backupList) ->
                appSettings = settings
                visible = settings.isVisible
                url = settings.downloadUrl.orEmpty()
                backups = backupList
            }.onFailure {
                error = it.userFacingMessage("Не удалось загрузить системные настройки")
            }
            isLoading = false
        }
    }

    fun saveAppSettings() {
        scope.launch {
            isBusy = true
            error = null
            success = null
            runCatching {
                journalApi.updateAdminMobileAppDownload(
                    UpdateMobileAppDownloadRequest(
                        isVisible = visible,
                        downloadUrl = url.ifBlank { null }
                    )
                )
            }.onSuccess { settings ->
                appSettings = settings
                success = "Настройки скачивания приложения сохранены"
            }.onFailure {
                error = it.userFacingMessage("Не удалось сохранить настройки")
            }
            isBusy = false
        }
    }

    fun createBackup() {
        scope.launch {
            isBusy = true
            error = null
            success = null
            runCatching { journalApi.createAdminBackup() }
                .onSuccess {
                    success = "Создание бэкапа запущено"
                    load()
                }
                .onFailure { error = it.userFacingMessage("Не удалось создать бэкап") }
            isBusy = false
        }
    }

    fun downloadBackup(artifact: BackupArtifact) {
        scope.launch {
            error = null
            runCatching {
                val bytes = journalApi.downloadAdminBackup(artifact.id).bytes()
                shareBytesFile(
                    context = context,
                    bytes = bytes,
                    fileName = artifact.fileName ?: "${artifact.id}.dump",
                    mimeType = "application/octet-stream",
                    chooserTitle = "Бэкап"
                )
            }.onFailure { error = it.userFacingMessage("Не удалось скачать бэкап") }
        }
    }

    fun checkRestore(artifact: BackupArtifact) {
        scope.launch {
            isBusy = true
            error = null
            success = null
            runCatching { journalApi.checkAdminBackupRestore(artifact.id) }
                .onSuccess {
                    success = "Проверка восстановления завершена: ${it.status ?: "ok"}"
                    load()
                }
                .onFailure { error = it.userFacingMessage("Не удалось проверить восстановление") }
            isBusy = false
        }
    }

    LaunchedEffect(Unit) { load() }

    if (isLoading) {
        Box(Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colors.primary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            error?.let { AppErrorCard(it) }
            success?.let {
                Text(
                    text = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.successContainer, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    color = colors.success,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item {
            AppSectionCard {
                Text("Мобильное приложение", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = visible, onCheckedChange = { visible = it })
                    Text("Показывать ссылку на скачивание", color = colors.primary, fontWeight = FontWeight.SemiBold)
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL APK") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Обновлено: ${formatDateTime(appSettings?.updatedAt)}",
                    color = colors.mutedText,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                AppPrimaryButton(
                    text = "Сохранить",
                    onClick = ::saveAppSettings,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            BackupHeader(backups = backups, isBusy = isBusy, onCreate = ::createBackup)
        }

        if (backups.artifacts.isEmpty()) {
            item {
                Text(
                    text = "Бэкапы не найдены",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    color = colors.mutedText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            items(backups.artifacts) { artifact ->
                BackupRow(
                    artifact = artifact,
                    isBusy = isBusy,
                    onDownload = { downloadBackup(artifact) },
                    onCheck = { checkRestore(artifact) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackupHeader(
    backups: AdminBackupsResponse,
    isBusy: Boolean,
    onCreate: () -> Unit
) {
    val colors = AppTheme.colors
    val createEnabled = backups.capabilities
        .flatMap { it.actions }
        .firstOrNull { it.id == "create" }
        ?.enabled ?: true

    AppSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Бэкапы", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                backups.storageHint?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = colors.mutedText, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.width(10.dp))
            AppPrimaryButton(
                text = "Создать",
                onClick = onCreate,
                enabled = createEnabled && !isBusy
            )
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            backups.capabilities.forEach { capability ->
                AppAdminBadge(
                    text = "${capability.component ?: "backup"}: ${capability.status ?: "-"}",
                    color = if (capability.status == "enabled") colors.success else colors.mutedText,
                    background = if (capability.status == "enabled") colors.successContainer else colors.headerBackground
                )
            }
        }
    }
}

@Composable
private fun BackupRow(
    artifact: BackupArtifact,
    isBusy: Boolean,
    onDownload: () -> Unit,
    onCheck: () -> Unit
) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
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
                    text = artifact.fileName ?: artifact.id,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Создан: ${formatDateTime(artifact.createdAt)}", color = colors.mutedText, fontSize = 13.sp)
                Text("Размер: ${formatBytes(artifact.sizeBytes)}", color = colors.mutedText, fontSize = 13.sp)
                artifact.errorMessage?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = colors.danger, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.width(10.dp))
            AppAdminBadge(
                text = artifact.status ?: "-",
                color = if (artifact.status == "completed" || artifact.status == "ok") colors.success else colors.mutedText,
                background = if (artifact.status == "completed" || artifact.status == "ok") colors.successContainer else colors.headerBackground
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AppSecondaryButton("Скачать", onClick = onDownload, modifier = Modifier.weight(1f))
            AppSecondaryButton("Проверить", onClick = onCheck, modifier = Modifier.weight(1f), contentColor = if (isBusy) colors.mutedText else colors.primary)
        }
    }
}

private fun formatDateTime(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    return try {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    } catch (_: Exception) {
        value
    }
}

private fun formatBytes(value: Long?): String {
    if (value == null || value <= 0L) return "-"
    val mb = value / 1024.0 / 1024.0
    return if (mb >= 1) "%.1f MB".format(mb) else "${value / 1024} KB"
}
