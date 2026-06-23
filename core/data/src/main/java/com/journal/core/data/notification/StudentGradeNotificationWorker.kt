package com.journal.core.data.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.journal.core.database.dao.StudentSubjectCardCacheDao
import com.journal.core.database.entity.StudentSubjectCardCacheEntity
import com.journal.core.model.teacher.StudentSubjectCard
import com.journal.core.network.api.JournalApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@HiltWorker
class StudentGradeNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: JournalApi,
    private val subjectCardCacheDao: StudentSubjectCardCacheDao,
    private val gradeChangeDetector: StudentGradeChangeDetector,
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val json: Json
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!notificationSettingsRepository.areGradeNotificationsEnabled()) {
            return Result.success()
        }

        return try {
            val subjects = api.getStudentSubjects().subjects
            subjects.forEach { subject ->
                val disciplineId = subject.disciplineId
                val periodId = subject.periodId
                val groupId = subject.groupId
                if (periodId.isNullOrBlank() || groupId.isNullOrBlank()) return@forEach

                val key = StudentSubjectCardCacheEntity.cacheKey(disciplineId, periodId, groupId)
                val previous = subjectCardCacheDao.get(key)?.toDomain(json)
                val current = api.getStudentSubjectCard(disciplineId, periodId, groupId)

                gradeChangeDetector.notifyGradeChanges(previous, current)
                subjectCardCacheDao.upsert(current.toEntity(key, disciplineId, periodId, groupId, json))
            }
            Result.success()
        } catch (throwable: Throwable) {
            Log.w(TAG, "Failed to check student grade notifications", throwable)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "StudentGradeNotificationWorker"
        private const val PERIODIC_WORK_NAME = "student_grade_notifications_periodic"
        private const val ONE_TIME_WORK_NAME = "student_grade_notifications_once"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<StudentGradeNotificationWorker>(
                30,
                TimeUnit.MINUTES
            )
                .setConstraints(networkConstraints())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun enqueueOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<StudentGradeNotificationWorker>()
                .setConstraints(networkConstraints())
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_WORK_NAME)
        }

        private fun networkConstraints(): Constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
    }
}

private fun StudentSubjectCard.toEntity(
    key: String,
    disciplineId: String,
    periodId: String,
    groupId: String,
    json: Json
) = StudentSubjectCardCacheEntity(
    key = key,
    disciplineId = disciplineId,
    periodId = periodId,
    groupId = groupId,
    jsonData = json.encodeToString(this)
)

private fun StudentSubjectCardCacheEntity.toDomain(json: Json): StudentSubjectCard =
    json.decodeFromString(StudentSubjectCard.serializer(), jsonData)

private fun StudentSubjectCardCacheEntity.Companion.cacheKey(
    disciplineId: String,
    periodId: String,
    groupId: String
): String = "$disciplineId|$periodId|$groupId"
