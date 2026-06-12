package com.journal.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores current authenticated user session.
 * Only one row is kept (id = 1). Cleared on logout.
 */
@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    @ColumnInfo(name = "role")
    val role: String,

    @ColumnInfo(name = "user_id")
    val userId: String? = null,

    @ColumnInfo(name = "full_name")
    val fullName: String? = null,

    @ColumnInfo(name = "last_online_at")
    val lastOnlineAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "offline_allowed_until")
    val offlineAllowedUntil: Long = System.currentTimeMillis() + DEFAULT_OFFLINE_TTL_MS,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_OFFLINE_TTL_MS = 30L * 24 * 60 * 60 * 1000
    }
}
