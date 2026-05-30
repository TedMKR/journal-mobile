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
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AdminDocumentsResponse(
    @SerialName("data") val data: List<AdminDocumentTask> = emptyList(),
    @SerialName("meta") val meta: AdminPageMeta? = null
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
