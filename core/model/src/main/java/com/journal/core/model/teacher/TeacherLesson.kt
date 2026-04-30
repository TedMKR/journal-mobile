package com.journal.core.model.teacher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeacherLesson(
    @SerialName("id") val id: String,
    @SerialName("discipline_name") val disciplineName: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("lesson_type") val lessonType: String,
    @SerialName("scheduled_at") val scheduledAt: String,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("discipline_id") val disciplineId: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("period_id") val periodId: String? = null
)
