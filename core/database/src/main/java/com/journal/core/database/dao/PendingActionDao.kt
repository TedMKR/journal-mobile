package com.journal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.journal.core.database.entity.PendingActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingActionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: PendingActionEntity): Long

    @Update
    suspend fun update(action: PendingActionEntity)

    /** All syncable actions (no fatal error, not exceeded max retries) */
    @Query(
        "SELECT * FROM pending_actions " +
            "WHERE status = 'PENDING' " +
            "AND error_message IS NULL AND retry_count < 5 " +
            "ORDER BY created_at ASC"
    )
    suspend fun getPending(): List<PendingActionEntity>

    @Query(
        "SELECT * FROM pending_actions " +
            "WHERE status = 'PENDING' " +
            "AND error_message IS NULL AND retry_count < 5 " +
            "AND next_attempt_at <= :now " +
            "ORDER BY created_at ASC"
    )
    suspend fun getPendingDue(now: Long): List<PendingActionEntity>

    /** Observe count for UI badges */
    @Query(
        "SELECT COUNT(*) FROM pending_actions " +
            "WHERE status IN ('PENDING', 'SYNCING') " +
            "AND error_message IS NULL AND retry_count < 5"
    )
    fun observePendingCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM pending_actions " +
            "WHERE status IN ('PENDING', 'SYNCING') " +
            "AND error_message IS NULL AND retry_count < 5"
    )
    suspend fun getPendingCount(): Int

    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_actions")
    suspend fun deleteAll()

    /** Actions for a specific journal (used for per-cell sync indicators) */
    @Query(
        "SELECT * FROM pending_actions " +
        "WHERE group_id = :groupId " +
            "AND discipline_id = :disciplineId " +
            "AND period_id = :periodId " +
            "AND status IN ('PENDING', 'SYNCING') " +
            "AND error_message IS NULL AND retry_count < 5"
    )
    fun observeForJournal(
        groupId: String,
        disciplineId: String,
        periodId: String
    ): Flow<List<PendingActionEntity>>

    @Query(
        "DELETE FROM pending_actions " +
            "WHERE action_key = :actionKey " +
            "AND status = 'PENDING'"
    )
    suspend fun deleteConflicting(actionKey: String)

    @Query("UPDATE pending_actions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
