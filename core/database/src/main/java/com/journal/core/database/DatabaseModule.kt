package com.journal.core.database

import android.content.Context
import androidx.room.Room
import com.journal.core.database.dao.JournalGridCacheDao
import com.journal.core.database.dao.PendingActionDao
import com.journal.core.database.dao.SessionDao
import com.journal.core.database.dao.StudentLessonDao
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
}
