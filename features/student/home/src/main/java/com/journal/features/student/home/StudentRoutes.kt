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
import androidx.compose.runtime.mutableIntStateOf
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
import com.journal.core.common.config.PersonNameFormatter
import com.journal.core.model.teacher.StudentJournalGrade
import com.journal.core.model.teacher.StudentJournalLesson
import com.journal.core.model.teacher.StudentLesson
import com.journal.core.model.teacher.StudentSubjectCard
import com.journal.core.model.teacher.StudentProfile
import com.journal.core.model.teacher.StudentSubjectSummary
import com.journal.core.network.api.JournalApi
import com.journal.core.ui.AppBackground
import com.journal.core.ui.AppBarBackground
import com.journal.core.ui.AppDanger
import com.journal.core.ui.AppHeaderBackground
import com.journal.core.ui.AppLessonBackground
import com.journal.core.ui.AppPrimary
import com.journal.core.ui.AppSecondaryText
import com.journal.core.ui.AppSuccess
import com.journal.core.ui.AppWarning
import retrofit2.HttpException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.journal.core.ui.AppStatTile as StatTile
import com.journal.core.ui.AppBadge as InfoChip

private val Background = AppBackground
private val CardBackground = Color.White
private val LessonBackground = AppLessonBackground
private val PrimaryText = AppPrimary
private val MutedText = AppSecondaryText
private val BadgeBackground = AppHeaderBackground
private val LightBlue = AppHeaderBackground
private val BarBackground = AppBarBackground
private val Accent = AppPrimary
private val Danger = AppDanger
private val Success = AppSuccess

// Journal table colours
private val JournalHeaderBg    = AppHeaderBackground
private val JournalCellBorder  = Color(0xFFC9CED8)
private val JournalPresentColor = AppSuccess
private val JournalAbsentColor  = AppDanger
private val JournalExcuseColor  = AppWarning

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
    val today = remember { LocalDate.now() }
    var weekOffset by remember { mutableIntStateOf(0) }

    val weekMonday = remember(weekOffset) { today.with(DayOfWeek.MONDAY).plusWeeks(weekOffset.toLong()) }
    val weekSunday  = remember(weekMonday) { weekMonday.plusDays(6) }

    var lessons by remember { mutableStateOf<List<StudentLesson>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(weekMonday) {
        isLoading = true
        error = null
        runCatching {
            journalApi.getStudentLessons(
                dateFrom = weekMonday.format(DateTimeFormatter.ISO_LOCAL_DATE),
                dateTo   = weekSunday.format(DateTimeFormatter.ISO_LOCAL_DATE),
                limit    = 200
            ).lessons
        }
            .onSuccess { lessons = it }
            .onFailure { error = it.message ?: "Не удалось загрузить расписание" }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WeekNavBar(
            weekMonday  = weekMonday,
            weekSunday  = weekSunday,
            isToday     = weekOffset == 0,
            onPrev      = { weekOffset-- },
            onNext      = { weekOffset++ }
        )
        when {
            isLoading  -> CenterState { CircularProgressIndicator(color = PrimaryText) }
            error != null -> CenterState { Text(error.orEmpty(), color = Danger) }
            else -> ScheduleContent(
                lessons     = lessons,
                weekMonday  = weekMonday,
                onOpenLesson = onOpenLesson
            )
        }
    }
}

@Composable
fun StudentDashboardRoute(
    journalApi: JournalApi,
    /** Full name extracted from JWT; used when the profile payload has no name. */
    jwtName: String? = null
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
            else -> StudentDashboardContent(profile = profile, subjects = subjects, jwtName = jwtName)
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
private fun WeekNavBar(
    weekMonday: LocalDate,
    weekSunday: LocalDate,
    isToday: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale("ru"))
    val label = "${weekMonday.format(fmt)} – ${weekSunday.format(fmt)}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(LightBlue, RoundedCornerShape(12.dp))
                .clickable(onClick = onPrev),
            contentAlignment = Alignment.Center
        ) {
            Text("‹", color = PrimaryText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = PrimaryText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (isToday) {
                Text(
                    text = "Текущая неделя",
                    color = MutedText,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .background(LightBlue, RoundedCornerShape(12.dp))
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Text("›", color = PrimaryText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ScheduleContent(
    lessons: List<StudentLesson>,
    weekMonday: LocalDate,
    onOpenLesson: (disciplineId: String, periodId: String, groupId: String) -> Unit
) {
    if (lessons.isEmpty()) {
        CenterState { Text("Нет занятий на этой неделе", color = MutedText) }
    } else {
        val groupedByDay = lessons.groupBy { lessonDayIndex(it.scheduledAt) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            (0..5).forEach { dayIndex ->
                val dayLessons = groupedByDay[dayIndex].orEmpty()
                    .sortedWith(compareBy({ it.lessonOrderNumber ?: Int.MAX_VALUE }, { it.scheduledAt }))
                if (dayLessons.isNotEmpty()) {
                    item {
                        StudentDayScheduleCard(
                            dayName  = dayNames[dayIndex],
                            dayDate  = weekMonday.plusDays(dayIndex.toLong()),
                            lessons  = dayLessons,
                            onOpenLesson = onOpenLesson
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentDayScheduleCard(
    dayName: String,
    dayDate: LocalDate? = null,
    lessons: List<StudentLesson>,
    onOpenLesson: (disciplineId: String, periodId: String, groupId: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayName,
                color = MutedText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            dayDate?.let {
                Text(
                    text = it.format(DateTimeFormatter.ofPattern("d MMM", Locale("ru"))),
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

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

@Composable
private fun StudentLessonCard(
    lesson: StudentLesson,
    onClick: () -> Unit
) {
    val orderNumber = lesson.lessonOrderNumber ?: 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LessonBackground, RoundedCornerShape(12.dp))
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                LessonBadge(text = lessonTypeName(lesson.lessonType), background = BadgeBackground)
                LessonBadge(text = lesson.groupName, background = BadgeBackground)
            }
        }
        lesson.location?.takeIf { it.isNotBlank() }?.let { location ->
            LessonBadge(text = location, background = BadgeBackground)
        }
        lesson.myAttendanceStatus?.let { AttendanceChip(status = it) }
        lesson.topic?.takeIf { it.isNotBlank() }?.let {
            Text("Тема: $it", color = MutedText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LessonBadge(text: String, background: Color) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 3.dp)
    ) {
        Text(text, color = PrimaryText, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StudentDashboardContent(
    profile: StudentProfile?,
    subjects: List<StudentSubjectSummary>,
    jwtName: String? = null
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ProfileSummaryCard(profile = profile, subjects = subjects, jwtName = jwtName) }
        item { SubjectsCard(subjects) }
    }
}

@Composable
private fun ProfileSummaryCard(
    profile: StudentProfile?,
    subjects: List<StudentSubjectSummary>,
    jwtName: String? = null
) {
    val avgGrade = subjects.mapNotNull { it.avgGrade }.takeIf { it.isNotEmpty() }?.average()
    val attendance = subjects.takeIf { it.isNotEmpty() }?.map { it.attendancePct }?.average() ?: 0.0

    val profileName = PersonNameFormatter.formatFullName(profile?.fullName)
    val jwtDisplayName = PersonNameFormatter.formatFullName(jwtName)
    val displayName = profileName.takeIf(String::isNotBlank)
        ?: jwtDisplayName.takeIf(String::isNotBlank)
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
private fun SubjectsCard(subjects: List<StudentSubjectSummary>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
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
                subject.teacherName?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        PersonNameFormatter.formatFullName(it),
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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

