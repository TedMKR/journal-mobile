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
