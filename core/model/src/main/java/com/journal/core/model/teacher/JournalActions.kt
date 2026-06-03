package com.journal.core.model.teacher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AcademicPeriodsResponse(
    @SerialName("data") val data: List<AcademicPeriod>
)

@Serializable
data class AcademicPeriod(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    @SerialName("is_closed") val isClosed: Boolean,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class MarkAttendanceRequest(
    @SerialName("student_id") val studentId: String,
    @SerialName("status") val status: String,
    @SerialName("comment") val comment: String? = null
)

@Serializable
data class BulkAttendanceRecordRequest(
    @SerialName("student_id") val studentId: String,
    @SerialName("status") val status: String,
    @SerialName("comment") val comment: String? = null,
    @SerialName("is_late_entry") val isLateEntry: Boolean = false,
    @SerialName("late_entry_reason") val lateEntryReason: String? = null
)

@Serializable
data class BulkMarkAttendanceRequest(
    @SerialName("records") val records: List<BulkAttendanceRecordRequest>
)

@Serializable
data class BulkAttendanceRecord(
    @SerialName("id") val id: String,
    @SerialName("lesson_id") val lessonId: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("status") val status: String,
    @SerialName("comment") val comment: String? = null,
    @SerialName("is_late_entry") val isLateEntry: Boolean = false,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class BulkAttendanceResponse(
    @SerialName("data") val data: List<BulkAttendanceRecord>
)

@Serializable
data class CreateGradeRequest(
    @SerialName("student_id") val studentId: String,
    @SerialName("assessment_form_id") val assessmentFormId: String,
    @SerialName("value") val value: Int,
    @SerialName("comment") val comment: String? = null,
    @SerialName("is_private") val isPrivate: Boolean = false
)

@Serializable
data class UpdateGradeRequest(
    @SerialName("value") val value: Int,
    @SerialName("comment") val comment: String? = null
)

@Serializable
data class CreateAssessmentFormRequest(
    @SerialName("title") val title: String,
    @SerialName("form_type") val formType: String,
    @SerialName("date") val date: String,
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("period_id") val periodId: String? = null
)

@Serializable
data class UpdateAssessmentFormRequest(
    @SerialName("title") val title: String? = null,
    @SerialName("form_type") val formType: String? = null,
    @SerialName("date") val date: String? = null,
    @SerialName("status") val status: String? = null
)

@Serializable
data class ArchiveRecordRequest(
    @SerialName("reason") val reason: String? = null
)

@Serializable
data class UpdateLessonTopicDetailsRequest(
    @SerialName("topic_custom_details") val topicCustomDetails: String
)

@Serializable
data class LessonTopicInfo(
    @SerialName("lesson_id") val lessonId: String,
    @SerialName("topic_custom_details") val topicCustomDetails: String? = null,
    @SerialName("topic_details_edited_by_teacher") val topicDetailsEditedByTeacher: Boolean,
    @SerialName("updated_at") val updatedAt: String
)
