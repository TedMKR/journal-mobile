package com.journal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.journal.core.database.entity.StudentProfileCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentProfileCacheDao {

    @Query("SELECT * FROM student_profile_cache WHERE cache_key = :key")
    fun observe(key: String): Flow<StudentProfileCacheEntity?>

    @Query("SELECT * FROM student_profile_cache WHERE cache_key = :key")
    suspend fun get(key: String): StudentProfileCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StudentProfileCacheEntity)
}
