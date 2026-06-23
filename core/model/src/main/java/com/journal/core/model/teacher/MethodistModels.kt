package com.journal.core.model.teacher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CatalogResponse<T>(
    @SerialName("data") val data: List<T> = emptyList(),
    @SerialName("total") val total: Int? = null
)

@Serializable
data class Discipline(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("code") val code: String? = null
)

@Serializable
data class TeacherProfile(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("email") val email: String? = null,
    @SerialName("keycloak_id") val keycloakId: String? = null
)

@Serializable
data class AcademicGroup(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("faculty") val faculty: String? = null,
    @SerialName("year") val year: Int? = null
)

@Serializable
data class LessonTemplate(
    @SerialName("id") val id: String,
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("discipline_name") val disciplineName: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("total_lessons") val totalLessons: Int = 0,
    @SerialName("topics_count") val topicsCount: Int = 0,
    @SerialName("has_assignments") val hasAssignments: Boolean = false,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class LessonTemplateDetail(
    @SerialName("id") val id: String,
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("discipline_name") val disciplineName: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("total_lessons") val totalLessons: Int = 0,
    @SerialName("topics_count") val topicsCount: Int = 0,
    @SerialName("has_assignments") val hasAssignments: Boolean = false,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("topics") val topics: List<LessonTopic> = emptyList()
)

@Serializable
data class LessonTopic(
    @SerialName("id") val id: String? = null,
    @SerialName("plan_id") val planId: String? = null,
    @SerialName("topic_name") val topicName: String,
    @SerialName("topic_description") val topicDescription: String? = null,
    @SerialName("lesson_count") val lessonCount: Int,
    @SerialName("order_index") val orderIndex: Int,
    @SerialName("is_frozen") val isFrozen: Boolean = false
)

@Serializable
data class TopicPayload(
    @SerialName("topic_name") val topicName: String,
    @SerialName("topic_description") val topicDescription: String? = null,
    @SerialName("lesson_count") val lessonCount: Int,
    @SerialName("order_index") val orderIndex: Int
)

@Serializable
data class CreateLessonTemplateBulkRequest(
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("topics") val topics: List<TopicPayload>
)

@Serializable
data class UpdateLessonTemplateRequest(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null
)

@Serializable
data class AssignLessonTemplateRequest(
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("plan_id") val planId: String,
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("period_id") val periodId: String,
    @SerialName("notes") val notes: String? = null
)

@Serializable
data class TemplateAssignment(
    @SerialName("id") val id: String,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("discipline_id") val disciplineId: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("period_id") val periodId: String? = null,
    @SerialName("plan_id") val planId: String? = null,
    @SerialName("plan_name") val planName: String? = null,
    @SerialName("assigned_by") val assignedBy: String? = null,
    @SerialName("assigned_at") val assignedAt: String? = null,
    @SerialName("revoked_at") val revokedAt: String? = null,
    @SerialName("notes") val notes: String? = null
)

@Serializable
data class StudentImportRow(
    @SerialName("row_number") val rowNumber: Int,
    @SerialName("last_name") val lastName: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("middle_name") val middleName: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("student_code") val studentCode: String? = null,
    @SerialName("subgroup_number") val subgroupNumber: Int? = null,
    @SerialName("start_date") val startDate: String? = null
)

@Serializable
data class StartStudentImportRequest(
    @SerialName("group_id") val groupId: String,
    @SerialName("academic_period_id") val academicPeriodId: String,
    @SerialName("import_mode") val importMode: String,
    @SerialName("source_file_name") val sourceFileName: String? = null,
    @SerialName("students") val students: List<StudentImportRow>
)

@Serializable
data class ImportSummary(
    @SerialName("total") val total: Int = 0,
    @SerialName("created") val created: Int = 0,
    @SerialName("updated") val updated: Int = 0,
    @SerialName("skipped") val skipped: Int = 0,
    @SerialName("conflicts") val conflicts: Int = 0,
    @SerialName("errors") val errors: Int = 0
)

@Serializable
data class ImportPreviewRow(
    @SerialName("row_number") val rowNumber: Int = 0,
    @SerialName("action") val action: String? = null,
    @SerialName("conflict_id") val conflictId: String? = null,
    @SerialName("conflict_type") val conflictType: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("student") val student: StudentImportRow? = null
)

@Serializable
data class ImportConflict(
    @SerialName("id") val id: String,
    @SerialName("batch_id") val batchId: String? = null,
    @SerialName("row_number") val rowNumber: Int = 0,
    @SerialName("conflict_type") val conflictType: String? = null,
    @SerialName("resolution") val resolution: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("resolved_by") val resolvedBy: String? = null
)

@Serializable
data class ImportBatchPreview(
    @SerialName("batch_id") val batchId: String,
    @SerialName("status") val status: String,
    @SerialName("summary") val summary: ImportSummary = ImportSummary(),
    @SerialName("rows") val rows: List<ImportPreviewRow> = emptyList(),
    @SerialName("conflicts") val conflicts: List<ImportConflict>? = null
)

@Serializable
data class ImportApplyResult(
    @SerialName("batch_id") val batchId: String,
    @SerialName("status") val status: String,
    @SerialName("summary") val summary: ImportSummary = ImportSummary()
)

@Serializable
data class CreateJournalRequest(
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("period_id") val periodId: String,
    @SerialName("lesson_type") val lessonType: String
)

@Serializable
data class CreateJournalResponse(
    @SerialName("journal_id") val journalId: String? = null,
    @SerialName("lessons_created") val lessonsCreated: Int? = null,
    @SerialName("lessons_updated") val lessonsUpdated: Int? = null
)
