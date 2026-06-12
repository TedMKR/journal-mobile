package com.journal.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_profile_cache")
data class StudentProfileCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "cache_key")
    val key: String = CURRENT_PROFILE_KEY,

    @ColumnInfo(name = "profile_json")
    val profileJson: String,

    @ColumnInfo(name = "subjects_json")
    val subjectsJson: String,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val CURRENT_PROFILE_KEY = "current"
    }
}
