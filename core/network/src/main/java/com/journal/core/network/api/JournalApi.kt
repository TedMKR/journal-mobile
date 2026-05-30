package com.journal.core.network.api

import com.journal.core.model.teacher.AcademicGroup
import com.journal.core.model.teacher.AcademicPeriodsResponse
import com.journal.core.model.teacher.AdminAccessBinding
import com.journal.core.model.teacher.AdminAccessBindingsResponse
import com.journal.core.model.teacher.ProblemStudentsResponse
import com.journal.core.model.teacher.AdminActionRequest
import com.journal.core.model.teacher.AdminAuditResponse
import com.journal.core.model.teacher.AdminDocumentsResponse
import com.journal.core.model.teacher.AdminJournalContext
import com.journal.core.model.teacher.AdminJournalsResponse
import com.journal.core.model.teacher.AdminPeriod
import com.journal.core.model.teacher.AdminPeriodsResponse
import com.journal.core.model.teacher.AdminUpdateUserRequest
import com.journal.core.model.teacher.AdminUser
import com.journal.core.model.teacher.AdminUsersResponse
import com.journal.core.model.teacher.AssignLessonTemplateRequest
import com.journal.core.model.teacher.CatalogResponse
import com.journal.core.model.teacher.CreateAssessmentFormRequest
import com.journal.core.model.teacher.CreateJournalRequest
import com.journal.core.model.teacher.CreateJournalResponse
import com.journal.core.model.teacher.CreateLessonTemplateBulkRequest
import com.journal.core.model.teacher.CurrentAttestationPrefill
import com.journal.core.model.teacher.CreateGradeRequest
import com.journal.core.model.teacher.Discipline
import com.journal.core.model.teacher.GrantJournalAccessRequest
import com.journal.core.model.teacher.GroupsPerformanceResponse
import com.journal.core.model.teacher.JournalAccessGrantResponse
import com.journal.core.model.teacher.JournalAccessGrantsResponse
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.JournalsResponse
import com.journal.core.model.teacher.JobAccepted
import com.journal.core.model.teacher.LessonTemplate
import com.journal.core.model.teacher.LessonTemplateDetail
import com.journal.core.model.teacher.LessonsResponse
import com.journal.core.model.teacher.MarkAttendanceRequest
import com.journal.core.model.teacher.DocumentTask
import com.journal.core.model.teacher.RequestReportPayload
import com.journal.core.model.teacher.StudentJournalData
import com.journal.core.model.teacher.StudentLessonsResponse
import com.journal.core.model.teacher.StudentProfile
import com.journal.core.model.teacher.StudentSubjectCard
import com.journal.core.model.teacher.StudentSubjectsResponse
import com.journal.core.model.teacher.TeacherProfile
import com.journal.core.model.teacher.TeacherStats
import com.journal.core.model.teacher.TopicPayload
import com.journal.core.model.teacher.AttendanceSummaryResponse
import com.journal.core.model.teacher.UpdateAssessmentFormRequest
import com.journal.core.model.teacher.UpdateGradeRequest
import com.journal.core.model.teacher.UpdateLessonTemplateRequest
import com.journal.core.model.teacher.UpdateLessonTopicDetailsRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface JournalApi {

    @GET("students/me")
    suspend fun getStudentProfile(): StudentProfile

    @GET("students/me/lessons")
    suspend fun getStudentLessons(
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("limit") limit: Int = 200
    ): StudentLessonsResponse

    @GET("students/me/subjects")
    suspend fun getStudentSubjects(
        @Query("period_id") periodId: String? = null
    ): StudentSubjectsResponse

    @GET("students/me/subjects/{disciplineId}/card")
    suspend fun getStudentSubjectCard(
        @Path("disciplineId") disciplineId: String,
        @Query("period_id") periodId: String,
        @Query("group_id") groupId: String
    ): StudentSubjectCard

    @GET("students/me/journal")
    suspend fun getStudentJournal(
        @Query("discipline_id") disciplineId: String,
        @Query("academic_period_id") academicPeriodId: String,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): StudentJournalData

    @GET("academic-periods")
    suspend fun getAcademicPeriods(
        @Query("include_closed") includeClosed: Boolean = false
    ): AcademicPeriodsResponse

    @GET("disciplines")
    suspend fun getDisciplines(
        @Query("period_id") periodId: String? = null,
        @Query("teacher_id") teacherId: String? = null,
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 200
    ): CatalogResponse<Discipline>

    @GET("teachers")
    suspend fun getTeachers(
        @Query("discipline_id") disciplineId: String? = null,
        @Query("group_id") groupId: String? = null,
        @Query("period_id") periodId: String? = null,
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 200
    ): CatalogResponse<TeacherProfile>

    @GET("groups")
    suspend fun getGroups(
        @Query("period_id") periodId: String? = null,
        @Query("discipline_id") disciplineId: String? = null,
        @Query("teacher_id") teacherId: String? = null,
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 200
    ): CatalogResponse<AcademicGroup>

    @GET("lessons")
    suspend fun getLessons(
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("period_id") periodId: String? = null,
        @Query("group_id") groupId: String? = null,
        @Query("discipline_id") disciplineId: String? = null,
        @Query("teacher_id") teacherId: String? = null,
        @Query("lesson_type") lessonType: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): LessonsResponse

    @GET("groups/{group_id}/journal")
    suspend fun getGroupJournalGrid(
        @Path("group_id") groupId: String,
        @Query("discipline_id") disciplineId: String,
        @Query("academic_period_id") academicPeriodId: String,
        @Query("teacher_id") teacherId: String? = null,
        @Query("lesson_type") lessonType: String? = null,
        @Query("subgroup_id") subgroupId: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): JournalGridResponse

    @POST("lessons/{lesson_id}/attendance/mark")
    suspend fun markAttendance(
        @Path("lesson_id") lessonId: String,
        @Body request: MarkAttendanceRequest
    )

    @POST("grades")
    suspend fun createGrade(
        @Body request: CreateGradeRequest
    )

    @PUT("grades/{grade_id}")
    suspend fun updateGrade(
        @Path("grade_id") gradeId: String,
        @Body request: UpdateGradeRequest
    )

    @POST("assessment-forms")
    suspend fun createAssessmentForm(
        @Body request: CreateAssessmentFormRequest
    )

    @PATCH("assessment-forms/{assessment_form_id}")
    suspend fun updateAssessmentForm(
        @Path("assessment_form_id") assessmentFormId: String,
        @Body request: UpdateAssessmentFormRequest
    )

    @DELETE("assessment-forms/{assessment_form_id}")
    suspend fun deleteAssessmentForm(
        @Path("assessment_form_id") assessmentFormId: String
    )

    @PUT("lessons/{lesson_id}/topic-details")
    suspend fun updateLessonTopicDetails(
        @Path("lesson_id") lessonId: String,
        @Body request: UpdateLessonTopicDetailsRequest
    )

    @GET("teacher/stats")
    suspend fun getTeacherStats(): TeacherStats

    @GET("teacher/attendance-summary")
    suspend fun getAttendanceSummary(
        @Query("group_id") groupId: String,
        @Query("discipline_id") disciplineId: String,
        @Query("period_id") periodId: String? = null
    ): AttendanceSummaryResponse

    @GET("teacher/groups-performance")
    suspend fun getGroupsPerformance(
        @Query("period_id") periodId: String? = null
    ): GroupsPerformanceResponse

    @GET("current-attestation/prefill")
    suspend fun getCurrentAttestationPrefill(
        @Query("group_id") groupId: String,
        @Query("discipline_id") disciplineId: String,
        @Query("academic_period_id") academicPeriodId: String,
        @Query("teacher_id") teacherId: String? = null
    ): CurrentAttestationPrefill

    @POST("reports")
    suspend fun requestCurrentAttestationReport(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: RequestReportPayload
    ): JobAccepted

    @GET("reports/{job_id}/status")
    suspend fun getReportStatus(
        @Path("job_id") jobId: String
    ): DocumentTask

    @GET("reports/{job_id}/file")
    suspend fun downloadReportFile(
        @Path("job_id") jobId: String
    ): ResponseBody

    @GET("journal-access-grants")
    suspend fun getJournalAccessGrants(
        @Query("grantee_id") granteeId: String? = null,
        @Query("granter_id") granterId: String? = null,
        @Query("discipline_id") disciplineId: String? = null,
        @Query("group_id") groupId: String? = null,
        @Query("active_only") activeOnly: Boolean? = null
    ): JournalAccessGrantsResponse

    @POST("journal-access-grants")
    suspend fun grantJournalAccess(
        @Body request: GrantJournalAccessRequest
    ): JournalAccessGrantResponse

    @DELETE("journal-access-grants/{grant_id}")
    suspend fun revokeJournalAccessGrant(
        @Path("grant_id") grantId: String
    )

    @GET("journals")
    suspend fun getJournals(
        @Query("period_id") periodId: String? = null,
        @Query("discipline_id") disciplineId: String? = null,
        @Query("group_id") groupId: String? = null,
        @Query("teacher_id") teacherId: String? = null,
        @Query("lesson_type") lessonType: String? = null,
        @Query("q") query: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): JournalsResponse

    @GET("lesson-templates")
    suspend fun getLessonTemplates(
        @Query("discipline_id") disciplineId: String? = null,
        @Query("include_deleted") includeDeleted: Boolean = false
    ): CatalogResponse<LessonTemplate>

    @GET("lesson-templates/{template_id}")
    suspend fun getLessonTemplate(
        @Path("template_id") templateId: String
    ): LessonTemplateDetail

    @POST("lesson-templates/bulk")
    suspend fun createLessonTemplateBulk(
        @Body request: CreateLessonTemplateBulkRequest
    ): LessonTemplateDetail

    @PUT("lesson-templates/{template_id}")
    suspend fun updateLessonTemplate(
        @Path("template_id") templateId: String,
        @Body request: UpdateLessonTemplateRequest
    ): LessonTemplate

    @DELETE("lesson-templates/{template_id}")
    suspend fun deleteLessonTemplate(
        @Path("template_id") templateId: String
    )

    @POST("lesson-templates/{template_id}/topics")
    suspend fun addLessonTopic(
        @Path("template_id") templateId: String,
        @Body request: TopicPayload
    )

    @PUT("lesson-templates/{template_id}/topics/{topic_id}")
    suspend fun updateLessonTopic(
        @Path("template_id") templateId: String,
        @Path("topic_id") topicId: String,
        @Body request: TopicPayload
    )

    @DELETE("lesson-templates/{template_id}/topics/{topic_id}")
    suspend fun deleteLessonTopic(
        @Path("template_id") templateId: String,
        @Path("topic_id") topicId: String
    )

    @POST("lesson-template-assignments")
    suspend fun assignLessonTemplate(
        @Body request: AssignLessonTemplateRequest
    )

    @POST("journals")
    suspend fun createJournal(
        @Body request: CreateJournalRequest
    ): CreateJournalResponse

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    @GET("admin/users")
    suspend fun getAdminUsers(
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
        @Query("q") query: String? = null,
        @Query("user_type") userType: String? = null,
        @Query("status") status: String? = null
    ): AdminUsersResponse

    @PATCH("admin/users/{id}")
    suspend fun updateAdminUser(
        @Path("id") userId: String,
        @Body request: AdminUpdateUserRequest
    ): AdminUser

    @POST("admin/users/{id}/block")
    suspend fun blockAdminUser(
        @Path("id") userId: String,
        @Body request: AdminActionRequest
    ): AdminUser

    @POST("admin/users/{id}/unblock")
    suspend fun unblockAdminUser(
        @Path("id") userId: String,
        @Body request: AdminActionRequest
    ): AdminUser

    @GET("admin/audit")
    suspend fun getAdminAudit(
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
        @Query("action") action: String? = null,
        @Query("entity_type") entityType: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): AdminAuditResponse

    @GET("admin/journals")
    suspend fun getAdminJournals(
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
        @Query("status") status: String? = null,
        @Query("period_id") periodId: String? = null,
        @Query("teacher_id") teacherId: String? = null,
        @Query("group_id") groupId: String? = null,
        @Query("discipline_id") disciplineId: String? = null
    ): AdminJournalsResponse

    @GET("admin/journals/{id}/export")
    suspend fun exportAdminJournal(
        @Path("id") journalId: String
    ): ResponseBody

    @POST("admin/journals/{id}/{action}")
    suspend fun adminJournalAction(
        @Path("id") journalId: String,
        @Path("action") action: String,
        @Body request: AdminActionRequest
    ): AdminJournalContext

    @GET("admin/periods")
    suspend fun getAdminPeriods(
        @Query("include_closed") includeClosed: Boolean = true
    ): AdminPeriodsResponse

    @POST("admin/periods/{id}/close")
    suspend fun closeAdminPeriod(
        @Path("id") periodId: String,
        @Body request: AdminActionRequest
    ): AdminPeriod

    @POST("admin/periods/{id}/reopen")
    suspend fun reopenAdminPeriod(
        @Path("id") periodId: String,
        @Body request: AdminActionRequest
    ): AdminPeriod

    @GET("admin/documents")
    suspend fun getAdminDocuments(
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null
    ): AdminDocumentsResponse

    @GET("admin/access-bindings")
    suspend fun getAdminAccessBindings(
        @Query("active_only") activeOnly: Boolean = false
    ): AdminAccessBindingsResponse

    @DELETE("admin/access-bindings/{id}")
    suspend fun revokeAdminAccessBinding(
        @Path("id") bindingId: String
    )

    @GET("analytics/problem-students")
    suspend fun getAdminProblemStudents(
        @Query("period_id") periodId: String? = null,
        @Query("group_id") groupId: String? = null,
        @Query("discipline_id") disciplineId: String? = null,
        @Query("min_grades") minGrades: Int? = null,
        @Query("min_failing_grades") minFailingGrades: Int? = null,
        @Query("failing_percent_threshold") failingPercentThreshold: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): ProblemStudentsResponse
}
