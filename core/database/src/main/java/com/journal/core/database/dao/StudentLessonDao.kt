package com.journal.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.journal.core.database.entity.StudentLessonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentLessonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<StudentLessonEntity>)

    @Query("SELECT * FROM student_lessons ORDER BY scheduled_at ASC")
    fun observeAll(): Flow<List<StudentLessonEntity>>

    @Query("SELECT * FROM student_lessons ORDER BY scheduled_at ASC")
    suspend fun getAll(): List<StudentLessonEntity>

    @Query(
        "SELECT * FROM student_lessons " +
            "WHERE scheduled_at >= :dateFrom AND scheduled_at <= :dateTo " +
            "ORDER BY scheduled_at ASC"
    )
    fun observeByDateRange(dateFrom: String, dateTo: String): Flow<List<StudentLessonEntity>>

    @Query("DELETE FROM student_lessons")
    suspend fun deleteAll()

    @Query("SELECT MAX(cached_at) FROM student_lessons")
    suspend fun lastCachedAt(): Long?
}
