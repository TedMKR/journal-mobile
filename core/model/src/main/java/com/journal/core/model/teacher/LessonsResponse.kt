package com.journal.core.model.teacher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LessonsResponse(
    @SerialName("data") val lessons: List<TeacherLesson>,
    @SerialName("total") val total: Int? = null
)
