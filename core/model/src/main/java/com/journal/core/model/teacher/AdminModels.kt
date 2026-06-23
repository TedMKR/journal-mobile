package com.journal.core.model.teacher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdminUser(
    @SerialName("id") val id: String,
    @SerialName("user_type") val userType: String? = null,
    @SerialName("keycloak_id") val keycloakId: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("patronymic") val patronymic: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("student_code") val studentCode: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("external_schedule_ref") val externalScheduleRef: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("blocked_reason") val blockedReason: String? = null,
    @SerialName("profile_sync_locked") val profileSyncLocked: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class AdminUsersResponse(
    @SerialName("data") val data: List<AdminUser> = emptyList(),
    @SerialName("meta") val meta: AdminPageMeta? = null
)

@Serializable
data class AdminPageMeta(
    @SerialName("total") val total: Int = 0,
    @SerialName("page") val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20
)

@Serializable
data class AuditEvent(
    @SerialName("id") val id: String,
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("action") val action: String? = null,
    @SerialName("entity_type") val entityType: String? = null,
    @SerialName("entity_id") val entityId: String? = null,
    @SerialName("before_state") val beforeState: String? = null,
    @SerialName("after_state") val afterState: String? = null,
    @SerialName("context") val context: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AdminAuditResponse(
    @SerialName("data") val data: List<AuditEvent> = emptyList(),
    @SerialName("meta") val meta: AdminPageMeta? = null
)

@Serializable
data class AdminUpdateUserRequest(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("patronymic") val patronymic: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("external_schedule_ref") val externalScheduleRef: String? = null,
    @SerialName("profile_sync_locked") val profileSyncLocked: Boolean? = null
)

@Serializable
data class AdminActionRequest(
    @SerialName("reason") val reason: String
)

@Serializable
data class AdminCreatePeriodRequest(
    @SerialName("name") val name: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    @SerialName("is_active") val isActive: Boolean? = null
)

@Serializable
data class AdminUpdatePeriodRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("reason") val reason: String? = null
)

@Serializable
data class AdminPeriod(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("is_closed") val isClosed: Boolean? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)

@Serializable
data class AdminPeriodsResponse(
    @SerialName("data") val data: List<AdminPeriod> = emptyList()
)

@Serializable
data class AdminJournalContext(
    @SerialName("id") val id: String,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("discipline_id") val disciplineId: String? = null,
    @SerialName("discipline_name") val disciplineName: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("period_id") val periodId: String? = null,
    @SerialName("period_name") val periodName: String? = null,
    @SerialName("lesson_type") val lessonType: String? = null,
    @SerialName("lesson_count") val lessonCount: Int = 0,
    @SerialName("status") val status: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AdminJournalsResponse(
    @SerialName("data") val data: List<AdminJournalContext> = emptyList(),
    @SerialName("meta") val meta: AdminPageMeta? = null
)

@Serializable
data class AdminDocumentTask(
    @SerialName("id") val id: String,
    @SerialName("job_type") val jobType: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("result") val result: AdminDocumentResult? = null,
    @SerialName("error_msg") val errorMsg: String? = null,
    @SerialName("retry_count") val retryCount: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null
)

@Serializable
data class AdminDocumentResult(
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("generated_at") val generatedAt: String? = null
)

@Serializable
data class AdminDocumentsResponse(
    @SerialName("data") val data: List<AdminDocumentTask> = emptyList(),
    @SerialName("meta") val meta: AdminPageMeta? = null
)

@Serializable
data class AdminImportSummary(
    @SerialName("total") val total: Int = 0,
    @SerialName("created") val created: Int = 0,
    @SerialName("updated") val updated: Int = 0,
    @SerialName("skipped") val skipped: Int = 0,
    @SerialName("conflicts") val conflicts: Int = 0,
    @SerialName("errors") val errors: Int = 0
)

@Serializable
data class AdminImportBatch(
    @SerialName("id") val id: String,
    @SerialName("import_type") val importType: String? = null,
    @SerialName("import_mode") val importMode: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("period_id") val periodId: String? = null,
    @SerialName("period_name") val periodName: String? = null,
    @SerialName("source_file_name") val sourceFileName: String? = null,
    @SerialName("started_by") val startedBy: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("summary") val summary: AdminImportSummary? = null,
    @SerialName("errors_count") val errorsCount: Int = 0,
    @SerialName("warnings_count") val warningsCount: Int = 0,
    @SerialName("conflicts_count") val conflictsCount: Int = 0
)

@Serializable
data class AdminImportsResponse(
    @SerialName("data") val data: List<AdminImportBatch> = emptyList(),
    @SerialName("meta") val meta: AdminPageMeta? = null
)

@Serializable
data class MobileAppDownloadResponse(
    @SerialName("is_visible") val isVisible: Boolean = false,
    @SerialName("download_url") val downloadUrl: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class UpdateMobileAppDownloadRequest(
    @SerialName("is_visible") val isVisible: Boolean,
    @SerialName("download_url") val downloadUrl: String? = null
)

@Serializable
data class BackupCapability(
    @SerialName("component") val component: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("reason") val reason: String? = null,
    @SerialName("actions") val actions: List<BackupAction> = emptyList()
)

@Serializable
data class BackupAction(
    @SerialName("id") val id: String? = null,
    @SerialName("label") val label: String? = null,
    @SerialName("method") val method: String? = null,
    @SerialName("href") val href: String? = null,
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("reason") val reason: String? = null,
    @SerialName("body") val body: Map<String, String>? = null
)

@Serializable
data class BackupArtifact(
    @SerialName("id") val id: String,
    @SerialName("component") val component: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("checked_at") val checkedAt: String? = null,
    @SerialName("error_message") val errorMessage: String? = null
)

@Serializable
data class AdminBackupsResponse(
    @SerialName("capabilities") val capabilities: List<BackupCapability> = emptyList(),
    @SerialName("artifacts") val artifacts: List<BackupArtifact> = emptyList(),
    @SerialName("storage_hint") val storageHint: String? = null
)

@Serializable
data class AdminCreateAccessBindingRequest(
    @SerialName("granter_id") val granterId: String,
    @SerialName("grantee_id") val granteeId: String,
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("period_id") val periodId: String,
    @SerialName("access_level") val accessLevel: String = "read",
    @SerialName("expires_at") val expiresAt: String? = null
)

@Serializable
data class AdminAccessBindingResponse(
    @SerialName("data") val data: AdminAccessBinding? = null
)

@Serializable
data class AdminAccessBinding(
    @SerialName("id") val id: String,
    @SerialName("granter_id") val granterId: String? = null,
    @SerialName("grantee_id") val granteeId: String? = null,
    @SerialName("discipline_id") val disciplineId: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("period_id") val periodId: String? = null,
    @SerialName("access_level") val accessLevel: String? = null,
    @SerialName("granted_at") val grantedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("revoked_at") val revokedAt: String? = null
)

@Serializable
data class AdminAccessBindingsResponse(
    @SerialName("data") val data: List<AdminAccessBinding> = emptyList()
)

@Serializable
data class AdminTeachingAssignment(
    @SerialName("id") val id: String,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("discipline_id") val disciplineId: String? = null,
    @SerialName("discipline_name") val disciplineName: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("period_id") val periodId: String? = null,
    @SerialName("period_name") val periodName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("revoked_at") val revokedAt: String? = null
)

@Serializable
data class AdminTeachingAssignmentsResponse(
    @SerialName("data") val data: List<AdminTeachingAssignment> = emptyList()
)

@Serializable
data class AdminTeachingAssignmentResponse(
    @SerialName("data") val data: AdminTeachingAssignment? = null
)

@Serializable
data class AdminCreateTeachingAssignmentRequest(
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("period_id") val periodId: String
)

@Serializable
data class ArchivedJournalEntry(
    @SerialName("journal_id") val journalId: String? = null,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("discipline_id") val disciplineId: String? = null,
    @SerialName("discipline_name") val disciplineName: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("period_id") val periodId: String? = null,
    @SerialName("period_name") val periodName: String? = null,
    @SerialName("lesson_type") val lessonType: String? = null,
    @SerialName("lesson_count") val lessonCount: Int = 0,
    @SerialName("archived_at") val archivedAt: String? = null
)

@Serializable
data class ArchivedJournalsResponse(
    @SerialName("data") val data: List<ArchivedJournalEntry> = emptyList(),
    @SerialName("total") val total: Int? = null,
    @SerialName("limit") val limit: Int? = null,
    @SerialName("offset") val offset: Int? = null,
    @SerialName("meta") val meta: AdminPageMeta? = null
)

@Serializable
data class FailingGradeEntry(
    @SerialName("grade_id") val gradeId: String? = null,
    @SerialName("assessment_form_id") val assessmentFormId: String? = null,
    @SerialName("assessment_title") val assessmentTitle: String? = null,
    @SerialName("form_type") val formType: String? = null,
    @SerialName("form_date") val formDate: String? = null,
    @SerialName("value") val value: Int = 2,
    @SerialName("comment") val comment: String? = null,
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ProblemStudentEntry(
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String? = null,
    @SerialName("student_code") val studentCode: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("discipline_id") val disciplineId: String? = null,
    @SerialName("discipline_name") val disciplineName: String? = null,
    @SerialName("period_id") val periodId: String? = null,
    @SerialName("period_name") val periodName: String? = null,
    @SerialName("grade_count") val gradeCount: Int = 0,
    @SerialName("failing_grade_count") val failingGradeCount: Int = 0,
    @SerialName("failing_grade_percent") val failingGradePercent: Float = 0f,
    @SerialName("max_consecutive_failing_grades") val maxConsecutiveFailingGrades: Int = 0,
    @SerialName("has_consecutive_failing_problem") val hasConsecutiveFailingProblem: Boolean = false,
    @SerialName("failing_grades") val failingGrades: List<FailingGradeEntry> = emptyList(),
    @SerialName("last_detected_at") val lastDetectedAt: String? = null
)

@Serializable
data class ProblemStudentsMeta(
    @SerialName("total") val total: Int = 0,
    @SerialName("limit") val limit: Int = 10,
    @SerialName("offset") val offset: Int = 0,
    @SerialName("min_grades") val minGrades: Int = 3,
    @SerialName("min_failing_grades") val minFailingGrades: Int = 3,
    @SerialName("failing_grade_value") val failingGradeValue: Int = 2,
    @SerialName("failing_percent_threshold") val failingPercentThreshold: Float = 50f
)

@Serializable
data class ProblemStudentsResponse(
    @SerialName("data") val data: List<ProblemStudentEntry> = emptyList(),
    @SerialName("meta") val meta: ProblemStudentsMeta = ProblemStudentsMeta()
)
