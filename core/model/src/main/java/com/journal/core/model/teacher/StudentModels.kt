package com.journal.core.model.teacher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StudentProfile(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("student_code") val studentCode: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("is_head_student") val isHeadStudent: Boolean = false
)

@Serializable
data class StudentLesson(
    @SerialName("id") val id: String,
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("discipline_name") val disciplineName: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("period_id") val periodId: String? = null,
    @SerialName("scheduled_at") val scheduledAt: String,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("lesson_type") val lessonType: String,
    @SerialName("status") val status: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("topic") val topic: String? = null,
    @SerialName("my_attendance_status") val myAttendanceStatus: String? = null,
    @SerialName("lesson_order_number") val lessonOrderNumber: Int? = null
)

@Serializable
data class StudentLessonsResponse(
    @SerialName("data") val lessons: List<StudentLesson> = emptyList()
)

@Serializable
data class StudentSubjectSummary(
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("discipline_name") val disciplineName: String,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("period_id") val periodId: String? = null,
    @SerialName("lesson_type") val lessonType: String? = null,
    @SerialName("lessons_attended") val lessonsAttended: Int = 0,
    @SerialName("lessons_total") val lessonsTotal: Int = 0,
    @SerialName("attendance_pct") val attendancePct: Double = 0.0,
    @SerialName("avg_grade") val avgGrade: Double? = null
)

@Serializable
data class StudentSubjectsResponse(
    @SerialName("data") val subjects: List<StudentSubjectSummary> = emptyList()
)

@Serializable
data class StudentSubjectCard(
    @SerialName("student") val student: StudentCardProfile? = null,
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("discipline_name") val disciplineName: String,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("group_id") val groupId: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("period_id") val periodId: String,
    @SerialName("lessons_attended") val lessonsAttended: Int = 0,
    @SerialName("lessons_total") val lessonsTotal: Int = 0,
    @SerialName("absences_total") val absencesTotal: Int = 0,
    @SerialName("absences_valid_excuse") val absencesValidExcuse: Int = 0,
    @SerialName("avg_grade") val avgGrade: Double? = null,
    @SerialName("attendance_by_month") val attendanceByMonth: List<StudentAttendanceByMonth> = emptyList(),
    @SerialName("journal_lessons") val journalLessons: List<StudentJournalLesson> = emptyList(),
    @SerialName("journal_grades") val journalGrades: List<StudentJournalGrade> = emptyList()
)

@Serializable
data class StudentCardProfile(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("student_code") val studentCode: String? = null,
    @SerialName("is_head_student") val isHeadStudent: Boolean = false
)

@Serializable
data class StudentAttendanceByMonth(
    @SerialName("month") val month: String,
    @SerialName("attendance_pct") val attendancePct: Double = 0.0
)

@Serializable
data class StudentJournalLesson(
    @SerialName("lesson_id") val lessonId: String,
    @SerialName("date") val date: String,
    @SerialName("scheduled_at") val scheduledAt: String,
    @SerialName("lesson_type") val lessonType: String,
    @SerialName("topic") val topic: String? = null,
    @SerialName("attendance_status") val attendanceStatus: String? = null,
    @SerialName("attendance_comment") val attendanceComment: String? = null
)

@Serializable
data class StudentJournalGrade(
    @SerialName("assessment_form_id") val assessmentFormId: String,
    @SerialName("title") val title: String,
    @SerialName("type") val type: String,
    @SerialName("date") val date: String,
    @SerialName("value") val value: String? = null,
    @SerialName("comment") val comment: String? = null
)

@Serializable
data class StudentJournalData(
    @SerialName("discipline") val discipline: JournalGridRef,
    @SerialName("academic_period") val academicPeriod: JournalGridAcademicPeriod,
    @SerialName("lessons") val lessons: List<JournalGridLesson> = emptyList(),
    @SerialName("assessment_forms") val assessmentForms: List<JournalGridAssessmentForm> = emptyList(),
    @SerialName("attendance") val attendance: List<JournalGridAttendance> = emptyList(),
    @SerialName("grades") val grades: List<JournalGridGrade> = emptyList(),
    @SerialName("my_attendance") val myAttendance: List<JournalGridAttendance> = emptyList(),
    @SerialName("my_grades") val myGrades: List<JournalGridGrade> = emptyList()
)
