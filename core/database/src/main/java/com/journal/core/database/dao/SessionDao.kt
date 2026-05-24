package com.journal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.journal.core.database.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Query("SELECT * FROM session WHERE id = 1 LIMIT 1")
    fun observe(): Flow<SessionEntity?>

    @Query("SELECT * FROM session WHERE id = 1 LIMIT 1")
    suspend fun get(): SessionEntity?

    @Query("DELETE FROM session")
    suspend fun clear()
}
