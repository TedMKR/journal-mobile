package com.journal.features.student.home

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.journal.core.model.teacher.StudentJournalGrade
import com.journal.core.model.teacher.StudentJournalLesson
import com.journal.core.model.teacher.StudentLesson
import com.journal.core.model.teacher.StudentSubjectCard
import com.journal.core.model.teacher.StudentProfile
import com.journal.core.model.teacher.StudentSubjectSummary
import com.journal.core.network.api.JournalApi
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Background = Color(0xFFEDEEED)
private val CardBackground = Color.White
private val LessonBackground = Color(0xFFE4E6EC)
private val PrimaryText = Color(0xFF223268)
private val MutedText = Color(0xFF7E8E99)
private val BadgeBackground = Color(0xFFD3D7E1)
private val LightBlue = Color(0xFFD3D7E1)
private val BarBackground = Color(0xFFCAD0E3)
private val Accent = Color(0xFF3B82F6)
private val Danger = Color(0xFFDC2626)
private val Success = Color(0xFF16A34A)

// Journal table colours
private val JournalHeaderBg    = Color(0xFFD3D7E1)
private val JournalCellBorder  = Color(0xFFC9CED8)
private val JournalPresentColor = Color(0xFF1F8A5B)
private val JournalAbsentColor  = Color(0xFFC44A4A)
private val JournalExcuseColor  = Color(0xFFE19B2C)

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
fun StudentScheduleRoute(
    journalApi: JournalApi,
    onOpenLesson: (disciplineId: String, periodId: String, groupId: String) -> Unit = { _, _, _ -> }
) {
    var lessons by remember { mutableStateOf<List<StudentLesson>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        runCatching { journalApi.getStudentLessons(limit = 200).lessons }
            .onSuccess { lessons = it }
            .onFailure { error = it.message ?: "Не удалось загрузить расписание" }
        isLoading = false
    }

    StudentScaffold(title = "Расписание занятий") {
        when {
            isLoading -> CenterState { CircularProgressIndicator(color = PrimaryText) }
            error != null -> CenterState { Text(error.orEmpty(), color = Danger) }
            else -> ScheduleContent(lessons = lessons, onOpenLesson = onOpenLesson)
        }
    }
}

@Composable
fun StudentDashboardRoute(
    journalApi: JournalApi,
    /** First name extracted from JWT — shown in the profile header. */
    jwtFirstName: String? = null
) {
    var profile by remember { mutableStateOf<StudentProfile?>(null) }
    var subjects by remember { mutableStateOf<List<StudentSubjectSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        runCatching {
            val loadedProfile = journalApi.getStudentProfile()
            val loadedSubjects = journalApi.getStudentSubjects().subjects
            loadedProfile to loadedSubjects
        }.onSuccess { (loadedProfile, loadedSubjects) ->
            profile = loadedProfile
            subjects = loadedSubjects
        }.onFailure {
            error = it.message ?: "Не удалось загрузить личный кабинет"
        }
        isLoading = false
    }

    StudentScaffold(title = "Личный кабинет") {
        when {
            isLoading -> CenterState { CircularProgressIndicator(color = PrimaryText) }
            error != null -> CenterState { Text(error.orEmpty(), color = Danger) }
            else -> StudentDashboardContent(profile = profile, subjects = subjects, jwtFirstName = jwtFirstName)
        }
    }
}

@Composable
private fun StudentScaffold(
    @Suppress("UNUSED_PARAMETER") title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun ScheduleContent(
    lessons: List<StudentLesson>,
    onOpenLesson: (disciplineId: String, periodId: String, groupId: String) -> Unit
) {
    val groupedLessons = lessons.groupBy { lessonDayIndex(it.scheduledAt) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        dayNames.forEachIndexed { dayIndex, dayName ->
            item {
                StudentDayScheduleCard(
                    dayName = dayName,
                    lessons = groupedLessons[dayIndex].orEmpty()
                        .sortedWith(compareBy({ it.lessonOrderNumber ?: Int.MAX_VALUE }, { it.scheduledAt })),
                    onOpenLesson = onOpenLesson
                )
            }
        }
    }
}

@Composable
private fun StudentDayScheduleCard(
    dayName: String,
    lessons: List<StudentLesson>,
    onOpenLesson: (disciplineId: String, periodId: String, groupId: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = dayName,
            color = MutedText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (lessons.isEmpty()) {
            Text(
                text = "Нет занятий",
                color = PrimaryText,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            lessons.forEach { lesson ->
                StudentLessonCard(
                    lesson = lesson,
                    onClick = {
                        val pid = lesson.periodId
                        if (pid != null) onOpenLesson(lesson.disciplineId, pid, lesson.groupId)
                    }
                )
            }
        }
    }
}

@Composable
private fun StudentLessonCard(
    lesson: StudentLesson,
    onClick: () -> Unit
) {
    val orderNumber = lesson.lessonOrderNumber ?: 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LessonBackground, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (orderNumber > 0) {
                Box(
                    modifier = Modifier
                        .background(BadgeBackground, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(orderNumber.toString(), color = PrimaryText, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(formatLessonTime(lesson.scheduledAt, lesson.endsAt), color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = lesson.disciplineName,
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoChip(lessonTypeName(lesson.lessonType))
            lesson.teacherName?.takeIf { it.isNotBlank() }?.let { InfoChip(it) }
        }
        lesson.location?.takeIf { it.isNotBlank() }?.let { InfoChip(it) }
        lesson.myAttendanceStatus?.let { AttendanceChip(status = it) }
        lesson.topic?.takeIf { it.isNotBlank() }?.let {
            Text("Тема: $it", color = MutedText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StudentDashboardContent(
    profile: StudentProfile?,
    subjects: List<StudentSubjectSummary>,
    jwtFirstName: String? = null
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ProfileSummaryCard(profile = profile, subjects = subjects, jwtFirstName = jwtFirstName) }
        item { SubjectsCard(subjects) }
    }
}

@Composable
private fun ProfileSummaryCard(
    profile: StudentProfile?,
    subjects: List<StudentSubjectSummary>,
    jwtFirstName: String? = null
) {
    val avgGrade = subjects.mapNotNull { it.avgGrade }.takeIf { it.isNotEmpty() }?.average()
    val attendance = subjects.takeIf { it.isNotEmpty() }?.map { it.attendancePct }?.average() ?: 0.0

    // Display name priority: JWT first name → 2nd word of API full name → full name → fallback
    val displayName = jwtFirstName?.takeIf(String::isNotBlank)
        ?: profile?.fullName?.trim()?.split("\\s+".toRegex())?.getOrNull(1)?.takeIf(String::isNotBlank)
        ?: profile?.fullName
        ?: "Профиль студента"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryText, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = displayName,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            profile?.groupName?.takeIf { it.isNotBlank() }?.let { Text("Группа: $it", color = Color.White.copy(alpha = 0.84f)) }
            if (profile?.isHeadStudent == true) InfoChip("Староста")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("Всего\nпредметов", subjects.size.toString(), Modifier.weight(1f))
            StatTile("Средний\nбалл", avgGrade?.let { String.format(Locale.US, "%.1f", it) } ?: "—", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("Посещаемость", "${attendance.toInt()}%", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(PrimaryText, RoundedCornerShape(4.dp))
        )
        Text(title, color = PrimaryText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(value, color = PrimaryText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SubjectsCard(subjects: List<StudentSubjectSummary>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Мои предметы", color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (subjects.isEmpty()) {
            Text("Нет данных", color = MutedText)
        } else {
            subjects.forEach { subject -> SubjectRow(subject) }
        }
    }
}

@Composable
private fun SubjectRow(subject: StudentSubjectSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AttendanceRing(percent = subject.attendancePct.toInt())
            Column(modifier = Modifier.weight(1f)) {
                Text(subject.disciplineName, color = PrimaryText, fontWeight = FontWeight.Bold)
                subject.teacherName?.takeIf { it.isNotBlank() }?.let { Text(it, color = MutedText, style = MaterialTheme.typography.bodySmall) }
                Text(
                    "${subject.lessonsAttended}/${subject.lessonsTotal} занятий",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Средний балл",
                    color = MutedText,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = subject.avgGrade?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFE5E7EB)).padding(top = 1.dp))
    }
}

@Composable
private fun AttendanceRing(percent: Int) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(54.dp).padding(4.dp)) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = Color(0xFFE5E7EB),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke),
                size = Size(size.width, size.height)
            )
            drawArc(
                color = Accent,
                startAngle = -90f,
                sweepAngle = 360f * percent.coerceIn(0, 100) / 100f,
                useCenter = false,
                style = Stroke(width = stroke),
                size = Size(size.width, size.height)
            )
        }
        Text("$percent%", color = PrimaryText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoChip(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .background(BadgeBackground, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = PrimaryText,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun AttendanceChip(status: String) {
    val (text, color) = when (status) {
        "present" -> "Присутствовал" to Success
        "absent" -> "Отсутствовал" to Danger
        "valid_excuse" -> "Уважительная причина" to Accent
        else -> status to MutedText
    }
    Text(text, color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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

private fun lessonDayIndex(scheduledAt: String): Int = runCatching {
    OffsetDateTime.parse(scheduledAt).dayOfWeek.value - 1
}.getOrDefault(-1)

private fun formatLessonTime(scheduledAt: String, endsAt: String?): String = runCatching {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    val start = OffsetDateTime.parse(scheduledAt).format(fmt)
    val end = endsAt?.let { OffsetDateTime.parse(it).format(fmt) }
    if (end != null) "$start - $end" else start
}.getOrElse { "" }

private fun lessonTypeName(type: String): String = when (type) {
    "lecture" -> "Лекция"
    "practice" -> "Практика"
    "lab" -> "Лабораторная"
    "seminar" -> "Семинар"
    else -> type
}

// ─── Student Journal (read-only) ──────────────────────────────────────────────

@Composable
fun StudentJournalRoute(
    journalApi: JournalApi,
    disciplineId: String,
    periodId: String,
    groupId: String
) {
    var data by remember { mutableStateOf<StudentSubjectCard?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(disciplineId, periodId, groupId) {
        isLoading = true
        error = null
        runCatching { journalApi.getStudentSubjectCard(disciplineId, periodId, groupId) }
            .onSuccess { data = it }
            .onFailure { error = it.message ?: "Не удалось загрузить журнал" }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        when {
            isLoading -> CenterState { CircularProgressIndicator(color = PrimaryText) }
            error != null -> CenterState { Text(error.orEmpty(), color = Danger, textAlign = TextAlign.Center) }
            data != null -> StudentJournalContent(data!!)
        }
    }
}

@Composable
private fun StudentJournalContent(card: StudentSubjectCard) {
    val hScroll = rememberScrollState()
    val studentName = card.student?.fullName ?: ""

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
                    .background(CardBackground, RoundedCornerShape(22.dp))
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
                        JournalTag(it)
                    }
                    JournalTag(card.groupName)
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
                .background(PrimaryText, RoundedCornerShape(4.dp))
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
                .background(CardBackground, RoundedCornerShape(22.dp))
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
            .background(LightBlue, RoundedCornerShape(18.dp))
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
                color = PrimaryText,
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
                color = PrimaryText,
                topLeft = Offset(point.x - labelW / 2f, point.y - labelH / 2f),
                size = Size(labelW, labelH),
                cornerRadius = CornerRadius(9.dp.toPx())
            )
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 11.dp.toPx()
                    isAntiAlias = true
                }
                drawText(percentText, point.x, point.y + 4.dp.toPx(), paint)

                val monthPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(34, 50, 104)
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 11.dp.toPx()
                    isAntiAlias = true
                }
                drawText(months[index].month, point.x, size.height - 4.dp.toPx(), monthPaint)
            }
        }

        // Baseline
        drawLine(
            color = PrimaryText.copy(alpha = 0.35f),
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
            .background(LightBlue, RoundedCornerShape(18.dp))
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
