package com.journal.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.journal.core.database.dao.DashboardCacheDao
import com.journal.core.database.dao.JournalGridCacheDao
import com.journal.core.database.dao.NotificationDao
import com.journal.core.database.dao.PendingActionDao
import com.journal.core.database.dao.SessionDao
import com.journal.core.database.dao.StudentLessonDao
import com.journal.core.database.dao.StudentProfileCacheDao
import com.journal.core.database.dao.StudentSubjectCardCacheDao
import com.journal.core.database.dao.TeacherLessonDao
import com.journal.core.database.entity.DashboardCacheEntity
import com.journal.core.database.entity.JournalGridCacheEntity
import com.journal.core.database.entity.NotificationEntity
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
        StudentProfileCacheEntity::class,
        DashboardCacheEntity::class,
        NotificationEntity::class
    ],
    version = 7,
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
    abstract fun dashboardCacheDao(): DashboardCacheDao
    abstract fun notificationDao(): NotificationDao

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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS dashboard_cache (
                        cache_key TEXT NOT NULL PRIMARY KEY,
                        json_data TEXT NOT NULL,
                        cached_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notifications (
                        id TEXT NOT NULL PRIMARY KEY,
                        owner_key TEXT NOT NULL,
                        role TEXT NOT NULL,
                        source_key TEXT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        category TEXT NOT NULL,
                        target_route TEXT,
                        created_at INTEGER NOT NULL,
                        read_at INTEGER,
                        deleted_at INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notifications_owner_key_created_at " +
                        "ON notifications(owner_key, created_at)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notifications_owner_key_read_at " +
                        "ON notifications(owner_key, read_at)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_notifications_owner_key_source_key " +
                        "ON notifications(owner_key, source_key)"
                )
            }
        }
    }
}
