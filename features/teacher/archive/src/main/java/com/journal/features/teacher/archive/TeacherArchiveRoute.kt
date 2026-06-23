package com.journal.features.teacher.archive

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.journal.core.common.config.PersonNameFormatter
import com.journal.core.common.config.userFacingMessage
import com.journal.core.model.teacher.AcademicPeriod
import com.journal.core.model.teacher.ArchivedJournalEntry
import com.journal.core.network.api.JournalApi
import com.journal.core.ui.AppAdminBadge
import com.journal.core.ui.AppErrorCard
import com.journal.core.ui.AppMenuDropdown
import com.journal.core.ui.AppPaginationRow
import com.journal.core.ui.AppPrimaryButton
import com.journal.core.ui.AppTheme
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private const val PAGE_SIZE = 20

data class TeacherArchiveJournalTarget(
    val groupId: String,
    val disciplineId: String,
    val periodId: String,
    val teacherId: String?,
    val lessonType: String
)

@Composable
fun TeacherArchiveRoute(
    journalApi: JournalApi,
    onOpenJournal: (TeacherArchiveJournalTarget) -> Unit
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var page by remember { mutableIntStateOf(0) }
    var periods by remember { mutableStateOf<List<AcademicPeriod>>(emptyList()) }
    var selectedPeriodId by remember { mutableStateOf("") }
    var journals by remember { mutableStateOf<List<ArchivedJournalEntry>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            runCatching {
                val periodResponse = journalApi.getAcademicPeriods(includeClosed = true)
                val archiveResponse = journalApi.getArchivedJournals(
                    periodId = selectedPeriodId.ifBlank { null },
                    limit = PAGE_SIZE,
                    offset = page * PAGE_SIZE
                )
                periodResponse.data to archiveResponse
            }.onSuccess { (periodData, archiveResponse) ->
                periods = periodData.filter { it.isClosed }
                journals = archiveResponse.data
                total = archiveResponse.total ?: archiveResponse.meta?.total ?: archiveResponse.data.size
            }.onFailure {
                error = it.userFacingMessage("Не удалось загрузить архив журналов")
            }
            isLoading = false
        }
    }

    LaunchedEffect(page, selectedPeriodId) { load() }

    val periodOptions = listOf("Все периоды" to "") + periods.map { it.name to it.id }
    val totalPages = maxOf(1, (total + PAGE_SIZE - 1) / PAGE_SIZE)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Период:", color = colors.primary, fontWeight = FontWeight.SemiBold)
            AppMenuDropdown(
                label = "Все периоды",
                selected = selectedPeriodId,
                options = periodOptions,
                onSelected = {
                    selectedPeriodId = it
                    page = 0
                },
                modifier = Modifier.weight(1f)
            )
        }

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            error != null -> Column(Modifier.padding(16.dp)) { AppErrorCard(error.orEmpty()) }
            journals.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Архивные журналы не найдены", color = colors.mutedText, fontWeight = FontWeight.SemiBold)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(journals) { journal ->
                    ArchiveJournalRow(
                        journal = journal,
                        onOpen = {
                            val groupId = journal.groupId
                            val disciplineId = journal.disciplineId
                            val periodId = journal.periodId
                            if (!groupId.isNullOrBlank() && !disciplineId.isNullOrBlank() && !periodId.isNullOrBlank()) {
                                onOpenJournal(
                                    TeacherArchiveJournalTarget(
                                        groupId = groupId,
                                        disciplineId = disciplineId,
                                        periodId = periodId,
                                        teacherId = journal.teacherId,
                                        lessonType = journal.lessonType ?: "practice"
                                    )
                                )
                            }
                        }
                    )
                }
                if (totalPages > 1) {
                    item {
                        AppPaginationRow(
                            page = page,
                            totalPages = totalPages,
                            onPrev = { if (page > 0) page-- },
                            onNext = { if (page + 1 < totalPages) page++ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveJournalRow(
    journal: ArchivedJournalEntry,
    onOpen: () -> Unit
) {
    val colors = AppTheme.colors
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
                    text = journal.disciplineName ?: "Журнал",
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(journal.groupName ?: "-", color = colors.mutedText, fontSize = 13.sp)
                Text(
                    PersonNameFormatter.formatFullName(journal.teacherName.orEmpty()).ifBlank { journal.teacherName ?: "-" },
                    color = colors.mutedText,
                    fontSize = 13.sp
                )
                Text("Архив: ${formatDateTime(journal.archivedAt)}", color = colors.mutedText, fontSize = 13.sp)
            }
            Spacer(Modifier.width(10.dp))
            AppAdminBadge(
                text = journal.periodName ?: "Архив",
                color = colors.primary,
                background = colors.headerBackground
            )
        }
        AppPrimaryButton("Открыть журнал", onClick = onOpen, modifier = Modifier.fillMaxWidth())
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
