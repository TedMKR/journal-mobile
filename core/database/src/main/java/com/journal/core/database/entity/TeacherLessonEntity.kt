package com.journal.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teacher_lessons")
data class TeacherLessonEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "discipline_name")
    val disciplineName: String,

    @ColumnInfo(name = "group_name")
    val groupName: String,

    @ColumnInfo(name = "lesson_type")
    val lessonType: String,

    @ColumnInfo(name = "scheduled_at")
    val scheduledAt: String,

    @ColumnInfo(name = "ends_at")
    val endsAt: String? = null,

    @ColumnInfo(name = "discipline_id")
    val disciplineId: String? = null,

    @ColumnInfo(name = "group_id")
    val groupId: String? = null,

    @ColumnInfo(name = "period_id")
    val periodId: String? = null,

    @ColumnInfo(name = "location")
    val location: String? = null,

    @ColumnInfo(name = "status")
    val status: String? = null,

    @ColumnInfo(name = "attendance_marked")
    val attendanceMarked: Boolean? = null,

    @ColumnInfo(name = "topic")
    val topic: String? = null,

    @ColumnInfo(name = "teacher_id")
    val teacherId: String? = null,

    @ColumnInfo(name = "teacher_name")
    val teacherName: String? = null,

    @ColumnInfo(name = "lesson_order_number")
    val lessonOrderNumber: Int? = null,

    /** Timestamp (ms) when this row was fetched from the server */
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)
