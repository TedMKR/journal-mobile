package com.journal.core.model.teacher

import kotlinx.serialization.Serializable

@Serializable
data class TeacherLesson(
    val id: String,
    val disciplineName: String,
    val groupName: String,
    val lessonType: String,
    val scheduledAt: String,
    val endsAt: String? = null,
    val disciplineId: String? = null,
    val groupId: String? = null,
    val periodId: String? = null
)
