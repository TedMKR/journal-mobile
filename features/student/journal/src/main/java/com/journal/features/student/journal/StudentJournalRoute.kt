package com.journal.features.student.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.journal.core.ui.AppTheme
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journal.core.common.config.PersonNameFormatter
import com.journal.core.model.teacher.StudentJournalGrade
import com.journal.core.model.teacher.StudentJournalLesson
import com.journal.core.model.teacher.StudentSubjectCard
import com.journal.core.ui.AppSecondaryButton
import com.journal.core.ui.shareTextFile
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Background: Color
    @Composable get() = AppTheme.colors.background
private val CardBackground: Color
    @Composable get() = AppTheme.colors.surface
private val LessonBackground: Color
    @Composable get() = AppTheme.colors.lessonBackground
private val PrimaryText: Color
    @Composable get() = AppTheme.colors.primary
private val MutedText: Color
    @Composable get() = AppTheme.colors.secondaryText
private val BadgeBackground: Color
    @Composable get() = AppTheme.colors.headerBackground
private val LightBlue: Color
    @Composable get() = AppTheme.colors.headerBackground
private val BarBackground: Color
    @Composable get() = AppTheme.colors.barBackground
private val Accent: Color
    @Composable get() = AppTheme.colors.primary
private val Danger: Color
    @Composable get() = AppTheme.colors.danger
private val Success: Color
    @Composable get() = AppTheme.colors.success

// Journal table colours
private val JournalHeaderBg: Color
    @Composable get() = AppTheme.colors.headerBackground
private val JournalCellBorder: Color
    @Composable get() = AppTheme.colors.outline
private val JournalPresentColor: Color
    @Composable get() = AppTheme.colors.success
private val JournalAbsentColor: Color
    @Composable get() = AppTheme.colors.danger
private val JournalExcuseColor: Color
    @Composable get() = AppTheme.colors.warning

private const val STUDENT_NAME_COL   = 190
private const val STUDENT_ATTEND_COL = 82
private const val STUDENT_GRADE_COL  = 112

private val dayNames = listOf(
    "Понедельник",
    "Вторник",
    "Среда",
    "Четверг",
    "Пятница",
    "Суббота"
)

@Composable
fun StudentJournalRoute(
    disciplineId: String,
    periodId: String,
    groupId: String,
    viewModel: StudentJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(disciplineId, periodId, groupId) {
        viewModel.load(disciplineId, periodId, groupId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        if (uiState.isOffline) {
            StudentOfflineBanner()
        }
        when {
            uiState.isLoading && uiState.data == null -> CenterState { CircularProgressIndicator(color = PrimaryText) }
            uiState.error != null -> CenterState { Text(uiState.error.orEmpty(), color = Danger, textAlign = TextAlign.Center) }
            uiState.data != null -> uiState.data?.let { StudentJournalContent(it) }
        }
    }
}

@Composable
private fun StudentOfflineBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF59E0B), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Офлайн — данные из кеша",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StudentJournalContent(card: StudentSubjectCard) {
    val context = LocalContext.current
    val hScroll = rememberScrollState()
    val studentName = PersonNameFormatter.formatFullName(card.student?.fullName)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── 1. Info card ──────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        card.disciplineName,
                        color = PrimaryText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    card.teacherName?.takeIf { it.isNotBlank() }?.let {
                        JournalTag(PersonNameFormatter.formatFullName(it))
                    }
                    JournalTag(card.groupName)
                    AppSecondaryButton(
                        text = "Экспорт CSV",
                        onClick = {
                            shareTextFile(
                                context = context,
                                text = buildStudentJournalCsv(card, studentName),
                                fileName = "student-journal-${card.disciplineId.takeLast(8)}.csv",
                                chooserTitle = "Экспорт журнала"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ── 2. Journal table card ─────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground, RoundedCornerShape(16.dp))
            ) {
                Text(
                    "Посещаемость",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                HorizontalDivider(color = JournalCellBorder, thickness = 0.5.dp)

                // Scrollable table
                Box(modifier = Modifier.horizontalScroll(hScroll)) {
                    Column {
                        StudentJournalTableHeader(card.journalLessons, card.journalGrades)
                        StudentJournalDataRow(card.journalLessons, card.journalGrades, studentName)
                    }
                }
            }
        }

        // ── 3. Stats card ──────────────────────────────────────────────────────
        item {
            StudentStatsCard(card)
        }
    }
}

private fun buildStudentJournalCsv(card: StudentSubjectCard, studentName: String): String {
    val rows = mutableListOf<List<String>>()
    rows += listOf("Раздел", "Дата", "Название", "Тип", "Значение", "Комментарий")
    card.journalLessons.forEach { lesson ->
        rows += listOf(
            "Посещаемость",
            lesson.date,
            lesson.topic.orEmpty(),
            lesson.lessonType,
            lesson.attendanceStatus.orEmpty(),
            lesson.attendanceComment.orEmpty()
        )
    }
    card.journalGrades.forEach { grade ->
        rows += listOf(
            "Оценка",
            grade.date,
            grade.title,
            grade.type,
            grade.value.orEmpty(),
            grade.comment.orEmpty()
        )
    }
    val header = listOf(
        listOf("Студент", studentName),
        listOf("Дисциплина", card.disciplineName),
        listOf("Группа", card.groupName),
        emptyList()
    )
    return (header + rows).joinToString("\n") { row ->
        row.joinToString(";") { cell -> csvCell(cell) }
    }
}

private fun csvCell(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}

// ── Tag / StatSmallCard (TeacherStudentCard style) ─────────────────────────────

@Composable
private fun JournalTag(text: String) {
    Box(
        modifier = Modifier
            .background(LightBlue, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text, color = PrimaryText, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun JournalStatSmallCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(LightBlue, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(BarBackground, RoundedCornerShape(4.dp))
        )
        Text(title, color = PrimaryText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(value, color = PrimaryText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

// ── Stats card ──────────────────────────────────────────────────────────────────

@Composable
private fun StudentStatsCard(card: StudentSubjectCard) {
    val chartMonths = card.attendanceByMonth.map { m ->
        val label = runCatching {
            LocalDate.parse(m.month.take(10))
                .format(DateTimeFormatter.ofPattern("LLL", Locale("ru")))
                .replaceFirstChar { it.uppercase() }
        }.getOrElse { m.month.take(3) }
        JournalAttendanceMonth(label, m.attendancePct.toInt())
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Stats section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Статистика",
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    JournalStatSmallCard(
                        title = "Средний балл",
                        value = card.avgGrade?.let { "%.1f".format(it) } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    JournalStatSmallCard(
                        title = "Пропущено",
                        value = card.absencesTotal.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
                JournalAttendanceChartCard(chartMonths)
                JournalProgressCard(
                    title = "Сдано заданий",
                    done = card.assessmentsCompleted,
                    total = card.assessmentsTotal
                )
                JournalProgressCard(
                    title = "Посещено занятий",
                    done = card.lessonsAttended,
                    total = card.lessonsTotal
                )
            }
        }
    }
}

@Composable
private fun JournalAttendanceChartCard(months: List<JournalAttendanceMonth>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightBlue, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Посещаемость по месяцам", color = PrimaryText, fontWeight = FontWeight.Bold)
        if (months.isEmpty()) {
            Text("Нет данных", color = MutedText)
        } else {
            JournalAttendanceLineChart(months)
        }
    }
}

@Composable
private fun JournalAttendanceLineChart(months: List<JournalAttendanceMonth>) {
    val primaryText = PrimaryText
    val primaryTextArgb = primaryText.toArgb()
    val onPrimaryArgb = AppTheme.colors.onPrimary.toArgb()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        val chartTop = 18.dp.toPx()
        val chartBottom = size.height - 28.dp.toPx()
        val leftPadding = 28.dp.toPx()
        val rightPadding = 28.dp.toPx()
        val availableWidth = size.width - leftPadding - rightPadding

        val points = months.mapIndexed { index, month ->
            val x = if (months.size == 1) size.width / 2f
                    else leftPadding + availableWidth * index / (months.size - 1)
            val normalized = month.percent.coerceIn(0, 100) / 100f
            val y = chartBottom - (chartBottom - chartTop) * normalized
            Offset(x, y)
        }

        // Connecting lines
        points.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = primaryText,
                start = start,
                end = end,
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Labels on points
        points.forEachIndexed { index, point ->
            val percentText = "${months[index].percent}%"
            val labelW = 38.dp.toPx()
            val labelH = 18.dp.toPx()
            drawRoundRect(
                color = primaryText,
                topLeft = Offset(point.x - labelW / 2f, point.y - labelH / 2f),
                size = Size(labelW, labelH),
                cornerRadius = CornerRadius(9.dp.toPx())
            )
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = onPrimaryArgb
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 11.dp.toPx()
                    isAntiAlias = true
                }
                drawText(percentText, point.x, point.y + 4.dp.toPx(), paint)

                val monthPaint = android.graphics.Paint().apply {
                    color = primaryTextArgb
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 11.dp.toPx()
                    isAntiAlias = true
                }
                drawText(months[index].month, point.x, size.height - 4.dp.toPx(), monthPaint)
            }
        }

        // Baseline
        drawLine(
            color = primaryText.copy(alpha = 0.35f),
            start = Offset(leftPadding, chartBottom + 4.dp.toPx()),
            end = Offset(size.width - rightPadding, chartBottom + 4.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun JournalProgressCard(title: String, done: Int, total: Int) {
    val percent = if (total > 0) done.toFloat() / total.toFloat() else 0f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightBlue, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = PrimaryText, fontWeight = FontWeight.Bold)
            Text("$done/$total", color = PrimaryText, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(BarBackground, RoundedCornerShape(6.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent.coerceIn(0f, 1f))
                    .height(10.dp)
                    .background(PrimaryText, RoundedCornerShape(6.dp))
            )
        }
    }
}

private data class JournalAttendanceMonth(val month: String, val percent: Int)

@Composable
private fun StudentJournalTableHeader(
    lessons: List<StudentJournalLesson>,
    grades: List<StudentJournalGrade>
) {
    Row {
        StudentJournalCell(
            text = "Студент",
            width = STUDENT_NAME_COL,
            isHeader = true,
            textAlign = TextAlign.Start
        )
        lessons.forEach { lesson ->
            StudentJournalCell(
                text = "${studentFormatDate(lesson.date)}\n${studentLessonTypeName(lesson.lessonType)}",
                width = STUDENT_ATTEND_COL,
                isHeader = true
            )
        }
        grades.forEach { grade ->
            StudentJournalCell(
                text = "${grade.title}\n${studentFormTypeName(grade.type)}",
                width = STUDENT_GRADE_COL,
                isHeader = true
            )
        }
    }
}

@Composable
private fun StudentJournalDataRow(
    lessons: List<StudentJournalLesson>,
    grades: List<StudentJournalGrade>,
    studentName: String = ""
) {
    Row {
        StudentJournalCell(
            text = studentName.ifBlank { "Я" },
            width = STUDENT_NAME_COL,
            textAlign = TextAlign.Start
        )
        lessons.forEach { lesson ->
            StudentJournalCell(
                text = studentAttendanceSymbol(lesson.attendanceStatus),
                width = STUDENT_ATTEND_COL,
                color = studentAttendanceColor(lesson.attendanceStatus)
            )
        }
        grades.forEach { grade ->
            StudentJournalCell(
                text = grade.value.orEmpty().ifBlank { "—" },
                width = STUDENT_GRADE_COL,
                color = PrimaryText
            )
        }
    }
}

@Composable
private fun StudentJournalCell(
    text: String,
    width: Int,
    isHeader: Boolean = false,
    color: Color = PrimaryText,
    textAlign: TextAlign = TextAlign.Center
) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(if (isHeader) 64.dp else 46.dp)
            .background(if (isHeader) JournalHeaderBg else CardBackground)
            .border(0.5.dp, JournalCellBorder),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = textAlign,
            maxLines = if (isHeader) 3 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )
    }
}

private fun studentAttendanceSymbol(status: String?): String = when (status) {
    "present"      -> "П"
    "absent"       -> "Н"
    "valid_excuse" -> "У"
    null           -> ""
    else           -> status
}

@Composable
private fun studentAttendanceColor(status: String?): Color = when (status) {
    "present"      -> JournalPresentColor
    "absent"       -> JournalAbsentColor
    "valid_excuse" -> JournalExcuseColor
    else           -> PrimaryText
}

private fun studentFormatDate(date: String): String = runCatching {
    LocalDate.parse(date.take(10)).format(DateTimeFormatter.ofPattern("dd.MM"))
}.getOrElse { date }

private fun studentLessonTypeName(type: String): String = when (type) {
    "lecture"      -> "Лекция"
    "practice"     -> "Практика"
    "lab"          -> "Лаб."
    "seminar"      -> "Семинар"
    "consultation" -> "Консульт."
    "exam"         -> "Экзамен"
    else           -> type
}

private fun studentFormTypeName(type: String): String = when (type) {
    "quiz"     -> "КР"
    "exam"     -> "Экзамен"
    "lab"      -> "Лаб"
    "practice" -> "Практика"
    "homework" -> "ДЗ"
    else       -> type
}
@Composable
private fun CenterState(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 44.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}
