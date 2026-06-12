package com.journal.core.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.journal.core.data.util.NetworkError
import com.journal.core.data.util.toNetworkError
import com.journal.core.database.dao.JournalGridCacheDao
import com.journal.core.database.dao.PendingActionDao
import com.journal.core.database.entity.PendingActionEntity
import com.journal.core.database.entity.PendingActionStatus
import com.journal.core.database.entity.PendingActionType
import com.journal.core.model.teacher.CreateAssessmentFormRequest
import com.journal.core.model.teacher.CreateGradeRequest
import com.journal.core.model.teacher.MarkAttendanceRequest
import com.journal.core.model.teacher.UpdateAssessmentFormRequest
import com.journal.core.model.teacher.UpdateGradeRequest
import com.journal.core.model.teacher.UpdateLessonTopicDetailsRequest
import com.journal.core.network.api.JournalApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.json.Json

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: JournalApi,
    private val pendingActionDao: PendingActionDao,
    private val journalGridCacheDao: JournalGridCacheDao,
    private val json: Json
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pending = pendingActionDao.getPendingDue(System.currentTimeMillis())

        if (pending.isEmpty()) {
            Log.d(TAG, "No pending actions to sync")
            return Result.success()
        }

        Log.d(TAG, "Syncing ${pending.size} pending action(s)")

        var hasFailures = false

        for (action in pending) {
            try {
                pendingActionDao.updateStatus(action.id, PendingActionStatus.SYNCING)
                dispatch(action)

                pendingActionDao.delete(action.id)
                journalGridCacheDao.delete(
                    action.groupId,
                    action.disciplineId,
                    action.periodId,
                    action.lessonType
                )
                Log.d(TAG, "Synced action ${action.id} (${action.actionType})")
            } catch (e: Exception) {
                val error = e.toNetworkError()
                Log.w(TAG, "Failed to sync action ${action.id}: ${error.message}")

                when (error) {
                    is NetworkError.NetworkUnavailable,
                    is NetworkError.ServerError -> {
                        val retryCount = action.retryCount + 1
                        val exhausted = retryCount >= MAX_RETRY_COUNT
                        pendingActionDao.update(
                            action.copy(
                                status = if (exhausted) {
                                    PendingActionStatus.FAILED
                                } else {
                                    PendingActionStatus.PENDING
                                },
                                retryCount = retryCount,
                                nextAttemptAt = if (exhausted) {
                                    action.nextAttemptAt
                                } else {
                                    System.currentTimeMillis() + backoffMs(retryCount)
                                },
                                errorMessage = if (exhausted) error.message else null
                            )
                        )
                        if (!exhausted) hasFailures = true
                    }

                    is NetworkError.ConflictError,
                    is NetworkError.ValidationError -> {
                        pendingActionDao.update(
                            action.copy(
                                status = PendingActionStatus.CONFLICT,
                                errorMessage = error.message
                            )
                        )
                    }

                    is NetworkError.AuthError,
                    is NetworkError.Unknown -> {
                        pendingActionDao.update(
                            action.copy(
                                status = PendingActionStatus.FAILED,
                                errorMessage = error.message
                            )
                        )
                    }
                }
            }
        }

        return if (hasFailures) Result.retry() else Result.success()
    }

    private suspend fun dispatch(action: PendingActionEntity) {
        when (action.actionType) {
            PendingActionType.MARK_ATTENDANCE -> {
                val req = json.decodeFromString<MarkAttendanceRequest>(action.payloadJson)
                api.markAttendance(action.entityId, req)
            }

            PendingActionType.CREATE_GRADE -> {
                val req = json.decodeFromString<CreateGradeRequest>(action.payloadJson)
                api.createGrade(req)
            }

            PendingActionType.UPDATE_GRADE -> {
                val req = json.decodeFromString<UpdateGradeRequest>(action.payloadJson)
                api.updateGrade(action.entityId, req)
            }

            PendingActionType.DELETE_GRADE -> {
                throw NetworkError.ValidationError(
                    code = 501,
                    body = "DELETE_GRADE is not supported by JournalApi"
                )
            }

            PendingActionType.CREATE_ASSESSMENT_FORM -> {
                val req = json.decodeFromString<CreateAssessmentFormRequest>(action.payloadJson)
                api.createAssessmentForm(req)
            }

            PendingActionType.UPDATE_ASSESSMENT_FORM -> {
                val req = json.decodeFromString<UpdateAssessmentFormRequest>(action.payloadJson)
                api.updateAssessmentForm(action.entityId, req)
            }

            PendingActionType.DELETE_ASSESSMENT_FORM -> {
                api.deleteAssessmentForm(action.entityId)
            }

            PendingActionType.UPDATE_LESSON_TOPIC -> {
                val req = json.decodeFromString<UpdateLessonTopicDetailsRequest>(action.payloadJson)
                api.updateLessonTopicDetails(action.entityId, req)
            }

            else -> {
                throw NetworkError.ValidationError(
                    code = 400,
                    body = "Unknown action type: ${action.actionType}"
                )
            }
        }
    }

    private fun backoffMs(retryCount: Int): Long {
        val multiplier = 1L shl (retryCount - 1).coerceIn(0, 5)
        return BASE_BACKOFF_MS * multiplier
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "journal_sync"
        private const val MAX_RETRY_COUNT = 5
        private const val BASE_BACKOFF_MS = 30_000L

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
