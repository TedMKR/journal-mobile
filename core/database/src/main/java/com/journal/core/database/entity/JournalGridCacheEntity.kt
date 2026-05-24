package com.journal.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Caches the entire JournalGridResponse as a JSON blob.
 * Key: combination of groupId + disciplineId + periodId + lessonType.
 */
@Entity(
    tableName = "journal_grid_cache",
    primaryKeys = ["group_id", "discipline_id", "period_id", "lesson_type"]
)
data class JournalGridCacheEntity(
    @ColumnInfo(name = "group_id")
    val groupId: String,

    @ColumnInfo(name = "discipline_id")
    val disciplineId: String,

    @ColumnInfo(name = "period_id")
    val periodId: String,

    @ColumnInfo(name = "lesson_type")
    val lessonType: String,

    /** Full JSON of JournalGridResponse */
    @ColumnInfo(name = "json_data")
    val jsonData: String,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)
