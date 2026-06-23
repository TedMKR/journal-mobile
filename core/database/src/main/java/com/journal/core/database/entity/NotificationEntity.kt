package com.journal.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["owner_key", "created_at"]),
        Index(value = ["owner_key", "read_at"]),
        Index(value = ["owner_key", "source_key"], unique = true)
    ]
)
data class NotificationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "owner_key")
    val ownerKey: String,

    @ColumnInfo(name = "role")
    val role: String,

    @ColumnInfo(name = "source_key")
    val sourceKey: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "target_route")
    val targetRoute: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "read_at")
    val readAt: Long? = null,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null
)
