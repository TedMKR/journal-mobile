package com.journal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.journal.core.database.entity.JournalGridCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalGridCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: JournalGridCacheEntity)

    @Query(
        "SELECT * FROM journal_grid_cache " +
            "WHERE group_id = :groupId " +
            "AND discipline_id = :disciplineId " +
            "AND period_id = :periodId " +
            "AND lesson_type = :lessonType " +
            "LIMIT 1"
    )
    fun observe(
        groupId: String,
        disciplineId: String,
        periodId: String,
        lessonType: String
    ): Flow<JournalGridCacheEntity?>

    @Query(
        "SELECT * FROM journal_grid_cache " +
            "WHERE group_id = :groupId " +
            "AND discipline_id = :disciplineId " +
            "AND period_id = :periodId " +
            "AND lesson_type = :lessonType " +
            "LIMIT 1"
    )
    suspend fun get(
        groupId: String,
        disciplineId: String,
        periodId: String,
        lessonType: String
    ): JournalGridCacheEntity?

    @Query("DELETE FROM journal_grid_cache")
    suspend fun deleteAll()

    @Query(
        "DELETE FROM journal_grid_cache " +
            "WHERE group_id = :groupId " +
            "AND discipline_id = :disciplineId " +
            "AND period_id = :periodId " +
            "AND lesson_type = :lessonType"
    )
    suspend fun delete(
        groupId: String,
        disciplineId: String,
        periodId: String,
        lessonType: String
    )
}
