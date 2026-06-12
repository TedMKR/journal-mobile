package com.journal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.journal.core.database.entity.StudentSubjectCardCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentSubjectCardCacheDao {

    @Query("SELECT * FROM student_subject_card_cache WHERE cache_key = :key")
    fun observe(key: String): Flow<StudentSubjectCardCacheEntity?>

    @Query("SELECT * FROM student_subject_card_cache WHERE cache_key = :key")
    suspend fun get(key: String): StudentSubjectCardCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StudentSubjectCardCacheEntity)
}
