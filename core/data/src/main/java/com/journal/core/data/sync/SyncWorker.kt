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
import com.journal.core.database.dao.PendingActionDao
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
    private val json: Json
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pending = pendingActionDao.getPending()

        if (pending.isEmpty()) {
            Log.d(TAG, "No pending actions to sync")
            return Result.success()
        }

        Log.d(TAG, "Syncing ${pending.size} pending action(s)")

        var hasFailures = false

        for (action in pending) {
            try {
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
                        // Grade deletion is not yet in JournalApi — skip safely
                        Log.w(TAG, "DELETE_GRADE not implemented in API, skipping ${action.id}")
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
                        Log.w(TAG, "Unknown action type: ${action.actionType}, skipping")
                    }
                }

                // Success — remove from queue
                pendingActionDao.delete(action.id)
                Log.d(TAG, "Synced action ${action.id} (${action.actionType})")

            } catch (e: Exception) {
                val isUnrecoverable = isUnrecoverableError(e)
                Log.w(TAG, "Failed to sync action ${action.id}: ${e.message}")

                pendingActionDao.update(
                    action.copy(
                        retryCount = action.retryCount + 1,
                        errorMessage = if (isUnrecoverable) e.message else null
                    )
                )

                if (!isUnrecoverable) hasFailures = true
            }
        }

        return if (hasFailures) Result.retry() else Result.success()
    }

    /**
     * True for HTTP 4xx errors that won't be fixed by retrying
     * (e.g. 404 entity not found, 409 conflict).
     */
    private fun isUnrecoverableError(e: Exception): Boolean {
        val message = e.message ?: return false
        return message.contains("404") || message.contains("409") || message.contains("422")
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "journal_sync"

        /**
         * Enqueues a one-time sync task that runs as soon as the device
         * has network connectivity. Safe to call multiple times — WorkManager
         * will keep only one instance running at a time.
         */
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
