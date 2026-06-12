package com.journal.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_subject_card_cache")
data class StudentSubjectCardCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "cache_key")
    val key: String,

    @ColumnInfo(name = "discipline_id")
    val disciplineId: String,

    @ColumnInfo(name = "period_id")
    val periodId: String,

    @ColumnInfo(name = "group_id")
    val groupId: String,

    @ColumnInfo(name = "json_data")
    val jsonData: String,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
) {
    companion object
}
