package com.journal.core.network.api

import com.journal.core.model.teacher.AcademicGroup
import com.journal.core.model.teacher.AcademicPeriodsResponse
import com.journal.core.model.teacher.AssignLessonTemplateRequest
import com.journal.core.model.teacher.CatalogResponse
import com.journal.core.model.teacher.CreateAssessmentFormRequest
import com.journal.core.model.teacher.CreateJournalRequest
import com.journal.core.model.teacher.CreateJournalResponse
import com.journal.core.model.teacher.CreateLessonTemplateBulkRequest
import com.journal.core.model.teacher.CreateGradeRequest
import com.journal.core.model.teacher.Discipline
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.LessonTemplate
import com.journal.core.model.teacher.LessonTemplateDetail
import com.journal.core.model.teacher.LessonsResponse
import com.journal.core.model.teacher.MarkAttendanceRequest
import com.journal.core.model.teacher.TeacherProfile
import com.journal.core.model.teacher.TopicPayload
import com.journal.core.model.teacher.UpdateAssessmentFormRequest
import com.journal.core.model.teacher.UpdateGradeRequest
import com.journal.core.model.teacher.UpdateLessonTemplateRequest
import com.journal.core.model.teacher.UpdateLessonTopicDetailsRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface JournalApi {

    @GET("academic-periods")
    suspend fun getAcademicPeriods(
        @Query("include_closed") includeClosed: Boolean = false
    ): AcademicPeriodsResponse

    @GET("disciplines")
    suspend fun getDisciplines(
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 200
    ): CatalogResponse<Discipline>

    @GET("teachers")
    suspend fun getTeachers(
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 200
    ): CatalogResponse<TeacherProfile>

    @GET("groups")
    suspend fun getGroups(
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
}
