package com.journal.core.model.teacher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentAttestationPrefill(
    @SerialName("report_type") val reportType: String = "current_attestation_statement",
    @SerialName("available_formats") val availableFormats: List<String> = emptyList(),
    @SerialName("context") val context: CurrentAttestationContext,
    @SerialName("defaults") val defaults: CurrentAttestationDefaults,
    @SerialName("students") val students: List<CurrentAttestationStudent> = emptyList()
)

@Serializable
data class CurrentAttestationContext(
    @SerialName("group_id") val groupId: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("discipline_name") val disciplineName: String,
    @SerialName("academic_period_id") val academicPeriodId: String,
    @SerialName("academic_period_name") val academicPeriodName: String,
    @SerialName("academic_period_start") val academicPeriodStart: String? = null,
    @SerialName("academic_period_end") val academicPeriodEnd: String? = null,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("course") val course: Int? = null,
    @SerialName("faculty_name") val facultyName: String? = null,
    @SerialName("department_name") val departmentName: String? = null,
    @SerialName("lecture_teacher_name") val lectureTeacherName: String? = null,
    @SerialName("practice_teacher_name") val practiceTeacherName: String? = null
)

@Serializable
data class CurrentAttestationDefaults(
    @SerialName("format") val format: String = "docx",
    @SerialName("overrides") val overrides: CurrentAttestationOverrides = CurrentAttestationOverrides(),
    @SerialName("options") val options: CurrentAttestationOptions = CurrentAttestationOptions()
)

@Serializable
data class CurrentAttestationOverrides(
    @SerialName("semester_label") val semesterLabel: String? = null,
    @SerialName("faculty_name") val facultyName: String? = null,
    @SerialName("department_name") val departmentName: String? = null,
    @SerialName("lecture_teacher_name") val lectureTeacherName: String? = null,
    @SerialName("practice_teacher_name") val practiceTeacherName: String? = null
)

@Serializable
data class CurrentAttestationOptions(
    @SerialName("include_lecture_absences") val includeLectureAbsences: Boolean = true,
    @SerialName("include_practice_absences") val includePracticeAbsences: Boolean = true,
    @SerialName("include_colloquiums") val includeColloquiums: Boolean = true,
    @SerialName("include_labs") val includeLabs: Boolean = true,
    @SerialName("include_control_works") val includeControlWorks: Boolean = true,
    @SerialName("include_final_grade") val includeFinalGrade: Boolean = true
)

@Serializable
data class CurrentAttestationStudent(
    @SerialName("number") val number: Int,
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("student_code") val studentCode: String? = null
)

@Serializable
data class RequestReportPayload(
    @SerialName("report_type") val reportType: String = "current_attestation_statement",
    @SerialName("format") val format: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("academic_period_id") val academicPeriodId: String? = null,
    @SerialName("period_id") val periodId: String? = null,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("return_to_dean_by") val returnToDeanBy: String? = null,
    @SerialName("progress_as_of") val progressAsOf: String? = null,
    @SerialName("overrides") val overrides: CurrentAttestationOverrides = CurrentAttestationOverrides(),
    @SerialName("options") val options: CurrentAttestationOptions = CurrentAttestationOptions()
)

@Serializable
data class JobAccepted(
    @SerialName("job_id") val jobId: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("status") val status: String? = null
)

@Serializable
data class DocumentTask(
    @SerialName("id") val id: String,
    @SerialName("job_type") val jobType: String? = null,
    @SerialName("status") val status: String,
    @SerialName("result") val result: DocumentTaskResult? = null,
    @SerialName("error_msg") val errorMessage: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class DocumentTaskResult(
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_path") val filePath: String? = null,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("generated_at") val generatedAt: String? = null
)
