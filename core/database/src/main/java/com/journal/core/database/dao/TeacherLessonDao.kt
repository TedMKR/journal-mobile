package com.journal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.journal.core.database.entity.TeacherLessonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherLessonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<TeacherLessonEntity>)

    @Query("SELECT * FROM teacher_lessons ORDER BY scheduled_at ASC")
    fun observeAll(): Flow<List<TeacherLessonEntity>>

    @Query("SELECT * FROM teacher_lessons ORDER BY scheduled_at ASC")
    suspend fun getAll(): List<TeacherLessonEntity>

    @Query(
        "SELECT * FROM teacher_lessons " +
            "WHERE scheduled_at >= :dateFrom AND scheduled_at <= :dateTo " +
            "ORDER BY scheduled_at ASC"
    )
    fun observeByDateRange(dateFrom: String, dateTo: String): Flow<List<TeacherLessonEntity>>

    @Query("DELETE FROM teacher_lessons")
    suspend fun deleteAll()

    @Query("SELECT MAX(cached_at) FROM teacher_lessons")
    suspend fun lastCachedAt(): Long?
}
