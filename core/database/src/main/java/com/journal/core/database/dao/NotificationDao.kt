package com.journal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.journal.core.database.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(notification: NotificationEntity)

    @Query(
        """
        SELECT * FROM notifications
        WHERE owner_key = :ownerKey AND deleted_at IS NULL
        ORDER BY CASE WHEN read_at IS NULL THEN 0 ELSE 1 END, created_at DESC
        """
    )
    fun observeActive(ownerKey: String): Flow<List<NotificationEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM notifications
        WHERE owner_key = :ownerKey AND deleted_at IS NULL AND read_at IS NULL
        """
    )
    fun observeUnreadCount(ownerKey: String): Flow<Int>

    @Query("UPDATE notifications SET read_at = :readAt WHERE id = :id AND read_at IS NULL")
    suspend fun markRead(id: String, readAt: Long)

    @Query(
        """
        UPDATE notifications
        SET read_at = :readAt
        WHERE owner_key = :ownerKey AND deleted_at IS NULL AND read_at IS NULL
        """
    )
    suspend fun markAllRead(ownerKey: String, readAt: Long)

    @Query("UPDATE notifications SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun delete(id: String, deletedAt: Long)

    @Query(
        """
        UPDATE notifications
        SET deleted_at = :deletedAt
        WHERE owner_key = :ownerKey AND source_key = :sourceKey AND deleted_at IS NULL
        """
    )
    suspend fun deleteBySource(ownerKey: String, sourceKey: String, deletedAt: Long)

    @Query(
        """
        UPDATE notifications
        SET deleted_at = :deletedAt
        WHERE owner_key = :ownerKey AND deleted_at IS NULL AND read_at IS NOT NULL
        """
    )
    suspend fun deleteRead(ownerKey: String, deletedAt: Long)
}
