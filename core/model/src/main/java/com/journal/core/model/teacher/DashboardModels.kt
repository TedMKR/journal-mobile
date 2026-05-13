package com.journal.core.model.teacher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeacherStats(
    @SerialName("total_students") val totalStudents: Int = 0,
    @SerialName("total_disciplines") val totalDisciplines: Int = 0,
    @SerialName("hours_this_week") val hoursThisWeek: Int = 0,
    @SerialName("avg_grade") val avgGrade: Float? = null
)

@Serializable
data class AttendanceSummaryResponse(
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("discipline_id") val disciplineId: String? = null,
    @SerialName("period_id") val periodId: String? = null,
    @SerialName("months") val months: List<AttendanceSummaryMonth> = emptyList()
)

@Serializable
data class AttendanceSummaryMonth(
    @SerialName("month") val month: String,
    @SerialName("attendance_percent") val attendancePercent: Float = 0f,
    @SerialName("present_count") val presentCount: Int = 0,
    @SerialName("absent_count") val absentCount: Int = 0,
    @SerialName("excused_count") val excusedCount: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0
)

@Serializable
data class GroupsPerformanceResponse(
    @SerialName("period_id") val periodId: String? = null,
    @SerialName("groups") val groups: List<GroupPerformanceEntry> = emptyList()
)

@Serializable
data class GroupPerformanceEntry(
    @SerialName("group_id") val groupId: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("avg_grade") val avgGrade: Float = 0f,
    @SerialName("grade_count") val gradeCount: Int = 0
)
