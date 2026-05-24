package com.journal.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = false
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun teacherLessonDao(): TeacherLessonDao
    abstract fun studentLessonDao(): StudentLessonDao
    abstract fun journalGridCacheDao(): JournalGridCacheDao
    abstract fun pendingActionDao(): PendingActionDao
}
