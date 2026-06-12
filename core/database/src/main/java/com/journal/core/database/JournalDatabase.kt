package com.journal.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.journal.core.database.dao.JournalGridCacheDao
import com.journal.core.database.dao.PendingActionDao
import com.journal.core.database.dao.SessionDao
import com.journal.core.database.dao.StudentLessonDao
import com.journal.core.database.dao.StudentProfileCacheDao
import com.journal.core.database.dao.StudentSubjectCardCacheDao
import com.journal.core.database.dao.TeacherLessonDao
import com.journal.core.database.entity.JournalGridCacheEntity
import com.journal.core.database.entity.PendingActionEntity
import com.journal.core.database.entity.SessionEntity
import com.journal.core.database.entity.StudentLessonEntity
import com.journal.core.database.entity.StudentProfileCacheEntity
import com.journal.core.database.entity.StudentSubjectCardCacheEntity
import com.journal.core.database.entity.TeacherLessonEntity

@Database(
    entities = [
        SessionEntity::class,
        TeacherLessonEntity::class,
        StudentLessonEntity::class,
        JournalGridCacheEntity::class,
        PendingActionEntity::class,
        StudentSubjectCardCacheEntity::class,
        StudentProfileCacheEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun teacherLessonDao(): TeacherLessonDao
    abstract fun studentLessonDao(): StudentLessonDao
    abstract fun journalGridCacheDao(): JournalGridCacheDao
    abstract fun pendingActionDao(): PendingActionDao
    abstract fun studentSubjectCardCacheDao(): StudentSubjectCardCacheDao
    abstract fun studentProfileCacheDao(): StudentProfileCacheDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE teacher_lessons ADD COLUMN lesson_order_number INTEGER")
                db.execSQL("ALTER TABLE student_lessons ADD COLUMN lesson_order_number INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE session ADD COLUMN last_online_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE session ADD COLUMN offline_allowed_until INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_actions ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL("ALTER TABLE pending_actions ADD COLUMN local_id TEXT")
                db.execSQL("ALTER TABLE pending_actions ADD COLUMN server_id TEXT")
                db.execSQL("ALTER TABLE pending_actions ADD COLUMN next_attempt_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_actions ADD COLUMN entity_type TEXT")
                db.execSQL("ALTER TABLE pending_actions ADD COLUMN action_key TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS student_subject_card_cache (
                        cache_key TEXT NOT NULL PRIMARY KEY,
                        discipline_id TEXT NOT NULL,
                        period_id TEXT NOT NULL,
                        group_id TEXT NOT NULL,
                        json_data TEXT NOT NULL,
                        cached_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS student_profile_cache (
                        cache_key TEXT NOT NULL PRIMARY KEY,
                        profile_json TEXT NOT NULL,
                        subjects_json TEXT NOT NULL,
                        cached_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
