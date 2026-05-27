package com.journal.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.journal.core.database.dao.JournalGridCacheDao
import com.journal.core.database.dao.PendingActionDao
import com.journal.core.database.dao.SessionDao
import com.journal.core.database.dao.StudentLessonDao
import com.journal.core.database.dao.TeacherLessonDao
import com.journal.core.database.entity.JournalGridCacheEntity
import com.journal.core.database.entity.PendingActionEntity
import com.journal.core.database.entity.SessionEntity
import com.journal.core.database.entity.StudentLessonEntity
import com.journal.core.database.entity.TeacherLessonEntity

@Database(
    entities = [
        SessionEntity::class,
        TeacherLessonEntity::class,
        StudentLessonEntity::class,
        JournalGridCacheEntity::class,
        PendingActionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun teacherLessonDao(): TeacherLessonDao
    abstract fun studentLessonDao(): StudentLessonDao
    abstract fun journalGridCacheDao(): JournalGridCacheDao
    abstract fun pendingActionDao(): PendingActionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE teacher_lessons ADD COLUMN lesson_order_number INTEGER")
                db.execSQL("ALTER TABLE student_lessons ADD COLUMN lesson_order_number INTEGER")
            }
        }
    }
}
