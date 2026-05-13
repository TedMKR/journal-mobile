package com.journal.core.model.teacher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JournalsResponse(
    @SerialName("data") val data: List<JournalContext> = emptyList(),
    @SerialName("total") val total: Int? = null
)

@Serializable
data class JournalContext(
    @SerialName("id") val id: String? = null,
    @SerialName("journal_id") val journalId: String? = null,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("discipline_id") val disciplineId: String,
    @SerialName("discipline_name") val disciplineName: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("period_id") val periodId: String,
    @SerialName("period_name") val periodName: String? = null,
    @SerialName("lesson_type") val lessonType: String,
    @SerialName("lesson_count") val lessonCount: Int = 0,
    @SerialName("held_count") val heldCount: Int = 0,
    @SerialName("first_lesson") val firstLesson: String? = null,
    @SerialName("last_lesson") val lastLesson: String? = null
)
