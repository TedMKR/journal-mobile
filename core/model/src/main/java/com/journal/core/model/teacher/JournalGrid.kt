package com.journal.core.model.teacher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JournalGridResponse(
    @SerialName("group") val group: JournalGridRef,
    @SerialName("discipline") val discipline: JournalGridRef,
    @SerialName("academic_period") val academicPeriod: JournalGridAcademicPeriod,
    @SerialName("teacher") val teacher: JournalGridTeacher,
    @SerialName("students") val students: List<JournalGridStudent>,
    @SerialName("lessons") val lessons: List<JournalGridLesson>,
    @SerialName("attendance") val attendance: List<JournalGridAttendance>,
    @SerialName("grades") val grades: List<JournalGridGrade>,
    @SerialName("assessment_forms") val assessmentForms: List<JournalGridAssessmentForm>,
    @SerialName("permissions") val permissions: JournalGridPermissions,
    @SerialName("meta") val meta: JournalGridMeta
)

@Serializable
data class JournalGridRef(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String
)

@Serializable
data class JournalGridAcademicPeriod(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    @SerialName("is_closed") val isClosed: Boolean
)

@Serializable
data class JournalGridTeacher(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String
)

@Serializable
data class JournalGridStudent(
    @SerialName("student_id") val studentId: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("external_id") val externalId: String? = null,
    @SerialName("subgroup_id") val subgroupId: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class JournalGridLesson(
    @SerialName("lesson_id") val lessonId: String,
    @SerialName("date") val date: String,
    @SerialName("scheduled_at") val scheduledAt: String,
    @SerialName("topic") val topic: String? = null,
    @SerialName("lesson_type") val lessonType: String,
    @SerialName("status") val status: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class JournalGridAttendance(
    @SerialName("attendance_id") val attendanceId: String,
    @SerialName("lesson_id") val lessonId: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("status") val status: String,
    @SerialName("comment") val comment: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("version") val version: String
)

@Serializable
data class JournalGridAssessmentForm(
    @SerialName("assessment_form_id") val assessmentFormId: String,
    @SerialName("title") val title: String,
    @SerialName("type") val type: String,
    @SerialName("max_score") val maxScore: Int? = null,
    @SerialName("date") val date: String,
    @SerialName("status") val status: String = "active",
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class JournalGridGrade(
    @SerialName("grade_id") val gradeId: String,
    @SerialName("assessment_form_id") val assessmentFormId: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("value") val value: String,
    @SerialName("comment") val comment: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("version") val version: String
)

@Serializable
data class JournalGridPermissions(
    @SerialName("can_edit_attendance") val canEditAttendance: Boolean,
    @SerialName("can_edit_grades") val canEditGrades: Boolean,
    @SerialName("can_view_private_comments") val canViewPrivateComments: Boolean
)

@Serializable
data class JournalGridMeta(
    @SerialName("generated_at") val generatedAt: String,
    @SerialName("version") val version: String
)
