package com.journal.core.database

import android.content.Context
import androidx.room.Room
import com.journal.core.database.dao.DashboardCacheDao
import com.journal.core.database.dao.JournalGridCacheDao
import com.journal.core.database.dao.NotificationDao
import com.journal.core.database.dao.PendingActionDao
import com.journal.core.database.dao.SessionDao
import com.journal.core.database.dao.StudentLessonDao
import com.journal.core.database.dao.StudentProfileCacheDao
import com.journal.core.database.dao.StudentSubjectCardCacheDao
import com.journal.core.database.dao.TeacherLessonDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): JournalDatabase =
        Room.databaseBuilder(
            context,
            JournalDatabase::class.java,
            "journal.db"
        )
            .addMigrations(JournalDatabase.MIGRATION_1_2)
            .addMigrations(JournalDatabase.MIGRATION_2_3)
            .addMigrations(JournalDatabase.MIGRATION_3_4)
            .addMigrations(JournalDatabase.MIGRATION_4_5)
            .addMigrations(JournalDatabase.MIGRATION_5_6)
            .addMigrations(JournalDatabase.MIGRATION_6_7)
            .build()

    @Provides
    fun provideSessionDao(db: JournalDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideTeacherLessonDao(db: JournalDatabase): TeacherLessonDao = db.teacherLessonDao()

    @Provides
    fun provideStudentLessonDao(db: JournalDatabase): StudentLessonDao = db.studentLessonDao()

    @Provides
    fun provideJournalGridCacheDao(db: JournalDatabase): JournalGridCacheDao =
        db.journalGridCacheDao()

    @Provides
    fun providePendingActionDao(db: JournalDatabase): PendingActionDao = db.pendingActionDao()

    @Provides
    fun provideStudentSubjectCardCacheDao(db: JournalDatabase): StudentSubjectCardCacheDao =
        db.studentSubjectCardCacheDao()

    @Provides
    fun provideStudentProfileCacheDao(db: JournalDatabase): StudentProfileCacheDao =
        db.studentProfileCacheDao()

    @Provides
    fun provideDashboardCacheDao(db: JournalDatabase): DashboardCacheDao =
        db.dashboardCacheDao()

    @Provides
    fun provideNotificationDao(db: JournalDatabase): NotificationDao =
        db.notificationDao()
}
