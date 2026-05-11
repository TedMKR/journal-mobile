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
