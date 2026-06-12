package com.journal.core.data.repository

import com.journal.core.data.util.Resource
import com.journal.core.data.util.NetworkError
import com.journal.core.data.util.networkBoundResource
import com.journal.core.data.util.toNetworkError
import com.journal.core.database.JournalDatabase
import com.journal.core.database.dao.JournalGridCacheDao
import com.journal.core.database.dao.PendingActionDao
import com.journal.core.database.entity.JournalGridCacheEntity
import com.journal.core.database.entity.PendingActionEntity
import com.journal.core.database.entity.PendingActionStatus
import com.journal.core.database.entity.PendingActionType
import com.journal.core.data.sync.PendingSyncScheduler
import com.journal.core.model.teacher.BulkMarkAttendanceRequest
import com.journal.core.model.teacher.CreateAssessmentFormRequest
import com.journal.core.model.teacher.CreateGradeRequest
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.MarkAttendanceRequest
import com.journal.core.model.teacher.UpdateAssessmentFormRequest
import com.journal.core.model.teacher.UpdateGradeRequest
import com.journal.core.model.teacher.UpdateLessonTopicDetailsRequest
import com.journal.core.network.api.JournalApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.room.withTransaction
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PendingJournalAction(
    val actionKey: String?,
    val localId: String?,
    val entityId: String
)

@Singleton
class JournalRepository @Inject constructor(
    private val api: JournalApi,
    private val db: JournalDatabase,
    private val journalGridCacheDao: JournalGridCacheDao,
    private val pendingActionDao: PendingActionDao,
    private val mutationApplier: LocalJournalMutationApplier,
    private val syncScheduler: PendingSyncScheduler,
    private val json: Json
) {

    // ─── Read ────────────────────────────────────────────────────────────────

    /**
     * Returns the journal grid as an offline-first flow.
     * Always emits cached data immediately, then refreshes from network.
     */
    fun getJournalGrid(
        groupId: String,
        disciplineId: String,
        periodId: String,
        lessonType: String,
        teacherId: String? = null,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<JournalGridResponse?>> = networkBoundResource(
        localFlow = {
            journalGridCacheDao.observe(groupId, disciplineId, periodId, lessonType)
                .map { it?.toResponse() }
        },
        shouldFetch = { cached ->
            if (cached == null) return@networkBoundResource true
            val entity = journalGridCacheDao.get(groupId, disciplineId, periodId, lessonType)
            val age = System.currentTimeMillis() - (entity?.cachedAt ?: 0L)
            age > cacheMaxAgeMs
        },
        fetch = {
            api.getGroupJournalGrid(
                groupId = groupId,
                disciplineId = disciplineId,
                academicPeriodId = periodId,
                teacherId = teacherId,
                lessonType = lessonType
            )
        },
        saveFetchResult = { response ->
            journalGridCacheDao.upsert(
                JournalGridCacheEntity(
                    groupId = groupId,
                    disciplineId = disciplineId,
                    periodId = periodId,
                    lessonType = lessonType,
                    jsonData = json.encodeToString(response)
                )
            )
        }
    )

    /** Observe pending action count for UI sync indicator */
    fun observePendingCount(): Flow<Int> = pendingActionDao.observePendingCount()

    /** Observe pending actions for a specific journal (cell-level sync badges) */
    fun observePendingForJournal(
        groupId: String,
        disciplineId: String,
        periodId: String
    ): Flow<List<PendingJournalAction>> =
        pendingActionDao.observeForJournal(groupId, disciplineId, periodId)
            .map { actions ->
                actions.map { action ->
                    PendingJournalAction(
                        actionKey = action.actionKey,
                        localId = action.localId,
                        entityId = action.entityId
                    )
                }
            }

    // ─── Write (offline-first) ───────────────────────────────────────────────

    /**
     * Mark attendance.
     * - Online: calls API directly, then invalidates cache.
     * - Offline: saves to pending_actions queue, UI stays responsive.
     */
    suspend fun markAttendance(
        lessonId: String,
        request: MarkAttendanceRequest,
        groupId: String,
        disciplineId: String,
        periodId: String,
        lessonType: String
    ) {
        try {
            api.markAttendance(lessonId, request)
            // Invalidate cache so next read re-fetches
            journalGridCacheDao.delete(groupId, disciplineId, periodId, lessonType)
        } catch (e: Exception) {
            // Offline — enqueue
            enqueueOfflineOrThrow(
                PendingActionEntity(
                    actionType = PendingActionType.MARK_ATTENDANCE,
                    entityId = lessonId,
                    payloadJson = json.encodeToString(request),
                    groupId = groupId,
                    disciplineId = disciplineId,
                    periodId = periodId,
                    lessonType = lessonType,
                    localId = "local_attendance_${UUID.randomUUID()}",
                    entityType = "attendance",
                    actionKey = attendanceActionKey(lessonId, request.studentId)
                ),
                e
            )
        }
    }

    suspend fun createGrade(
        request: CreateGradeRequest,
        groupId: String,
        disciplineId: String,
        periodId: String,
        lessonType: String
    ) {
        try {
            api.createGrade(request)
            journalGridCacheDao.delete(groupId, disciplineId, periodId, lessonType)
        } catch (e: Exception) {
            val localId = "local_grade_${UUID.randomUUID()}"
            enqueueOfflineOrThrow(
                PendingActionEntity(
                    actionType = PendingActionType.CREATE_GRADE,
                    entityId = localId,
                    payloadJson = json.encodeToString(request),
                    groupId = groupId,
                    disciplineId = disciplineId,
                    periodId = periodId,
                    lessonType = lessonType,
                    localId = localId,
                    entityType = "grade",
                    actionKey = createGradeActionKey(request.studentId, request.assessmentFormId)
                ),
                e
            )
        }
    }

    suspend fun updateGrade(
        gradeId: String,
        request: UpdateGradeRequest,
        groupId: String,
        disciplineId: String,
        periodId: String,
        lessonType: String
    ) {
        try {
            api.updateGrade(gradeId, request)
            journalGridCacheDao.delete(groupId, disciplineId, periodId, lessonType)
        } catch (e: Exception) {
            enqueueOfflineOrThrow(
                PendingActionEntity(
                    actionType = PendingActionType.UPDATE_GRADE,
                    entityId = gradeId,
                    payloadJson = json.encodeToString(request),
                    groupId = groupId,
                    disciplineId = disciplineId,
                    periodId = periodId,
                    lessonType = lessonType,
                    entityType = "grade",
                    actionKey = gradeActionKey(gradeId)
                ),
                e
            )
        }
    }

    suspend fun createAssessmentForm(
        request: CreateAssessmentFormRequest,
        lessonType: String
    ) {
        try {
            api.createAssessmentForm(request)
            journalGridCacheDao.delete(
                request.groupId,
                request.disciplineId,
                request.periodId ?: "",
                lessonType
            )
        } catch (e: Exception) {
            val localId = "local_form_${UUID.randomUUID()}"
            enqueueOfflineOrThrow(
                PendingActionEntity(
                    actionType = PendingActionType.CREATE_ASSESSMENT_FORM,
                    entityId = localId,
                    payloadJson = json.encodeToString(request),
                    groupId = request.groupId,
                    disciplineId = request.disciplineId,
                    periodId = request.periodId ?: "",
                    lessonType = lessonType,
                    localId = localId,
                    entityType = "assessment_form",
                    actionKey = assessmentCreateActionKey(
                        request.groupId,
                        request.disciplineId,
                        request.date,
                        request.title
                    )
                ),
                e
            )
        }
    }

    suspend fun updateAssessmentForm(
        assessmentFormId: String,
        request: UpdateAssessmentFormRequest,
        groupId: String,
        disciplineId: String,
        periodId: String,
        lessonType: String
    ) {
        try {
            api.updateAssessmentForm(assessmentFormId, request)
            journalGridCacheDao.delete(groupId, disciplineId, periodId, lessonType)
        } catch (e: Exception) {
            enqueueOfflineOrThrow(
                PendingActionEntity(
                    actionType = PendingActionType.UPDATE_ASSESSMENT_FORM,
                    entityId = assessmentFormId,
                    payloadJson = json.encodeToString(request),
                    groupId = groupId,
                    disciplineId = disciplineId,
                    periodId = periodId,
                    lessonType = lessonType,
                    entityType = "assessment_form",
                    actionKey = assessmentActionKey(assessmentFormId)
                ),
                e
            )
        }
    }

    suspend fun deleteAssessmentForm(
        assessmentFormId: String,
        groupId: String,
        disciplineId: String,
        periodId: String,
        lessonType: String
    ) {
        try {
            api.deleteAssessmentForm(assessmentFormId)
            journalGridCacheDao.delete(groupId, disciplineId, periodId, lessonType)
        } catch (e: Exception) {
            enqueueOfflineOrThrow(
                PendingActionEntity(
                    actionType = PendingActionType.DELETE_ASSESSMENT_FORM,
                    entityId = assessmentFormId,
                    payloadJson = "{}",
                    groupId = groupId,
                    disciplineId = disciplineId,
                    periodId = periodId,
                    lessonType = lessonType,
                    entityType = "assessment_form",
                    actionKey = assessmentActionKey(assessmentFormId)
                ),
                e
            )
        }
    }

    /**
     * Bulk mark attendance — online only.
     * No offline queue: bulk mutations don't make sense without server context.
     * Callers should handle exceptions and surface them to the UI.
     */
    suspend fun bulkMarkAttendance(
        lessonId: String,
        request: BulkMarkAttendanceRequest,
        groupId: String,
        disciplineId: String,
        periodId: String,
        lessonType: String
    ) {
        api.bulkMarkAttendance(lessonId, request)
        journalGridCacheDao.delete(groupId, disciplineId, periodId, lessonType)
    }

    suspend fun updateLessonTopic(
        lessonId: String,
        request: UpdateLessonTopicDetailsRequest,
        groupId: String,
        disciplineId: String,
        periodId: String,
        lessonType: String
    ) {
        try {
            api.updateLessonTopicDetails(lessonId, request)
            journalGridCacheDao.delete(groupId, disciplineId, periodId, lessonType)
        } catch (e: Exception) {
            enqueueOfflineOrThrow(
                PendingActionEntity(
                    actionType = PendingActionType.UPDATE_LESSON_TOPIC,
                    entityId = lessonId,
                    payloadJson = json.encodeToString(request),
                    groupId = groupId,
                    disciplineId = disciplineId,
                    periodId = periodId,
                    lessonType = lessonType,
                    entityType = "lesson",
                    actionKey = lessonTopicActionKey(lessonId)
                ),
                e
            )
        }
    }

    // ─── Cache management ────────────────────────────────────────────────────

    /** Force-refresh the journal grid (ignore cache age) */
    suspend fun invalidateJournalCache(
        groupId: String,
        disciplineId: String,
        periodId: String,
        lessonType: String
    ) {
        journalGridCacheDao.delete(groupId, disciplineId, periodId, lessonType)
    }

    private suspend fun enqueueOfflineOrThrow(action: PendingActionEntity, throwable: Throwable) {
        when (val error = throwable.toNetworkError()) {
            is NetworkError.NetworkUnavailable,
            is NetworkError.ServerError -> {
                applyAndEnqueue(action)
                syncScheduler.enqueue()
            }

            is NetworkError.AuthError,
            is NetworkError.ValidationError,
            is NetworkError.ConflictError,
            is NetworkError.Unknown -> throw error
        }
    }

    private suspend fun applyAndEnqueue(action: PendingActionEntity) {
        db.withTransaction {
            action.actionKey?.let { pendingActionDao.deleteConflicting(it) }

            val cached = journalGridCacheDao.get(
                action.groupId,
                action.disciplineId,
                action.periodId,
                action.lessonType
            )
            val grid = cached?.toResponse()
            if (cached != null && grid != null) {
                journalGridCacheDao.upsert(
                    cached.copy(
                        jsonData = json.encodeToString(mutationApplier.apply(grid, action)),
                        cachedAt = System.currentTimeMillis()
                    )
                )
            }

            pendingActionDao.insert(
                action.copy(
                    status = PendingActionStatus.PENDING,
                    errorMessage = null
                )
            )
        }
    }

    private fun attendanceActionKey(lessonId: String, studentId: String): String =
        "attendance|$lessonId|$studentId"

    private fun createGradeActionKey(studentId: String, assessmentFormId: String): String =
        "grade_create|$studentId|$assessmentFormId"

    private fun gradeActionKey(gradeId: String): String =
        "grade|$gradeId"

    private fun assessmentActionKey(assessmentFormId: String): String =
        "assessment_form|$assessmentFormId"

    private fun assessmentCreateActionKey(
        groupId: String,
        disciplineId: String,
        date: String,
        title: String
    ): String =
        "assessment_form_create|$groupId|$disciplineId|$date|$title"

    private fun lessonTopicActionKey(lessonId: String): String =
        "lesson_topic|$lessonId"

    companion object {
        /** 3 minutes */
        const val CACHE_MAX_AGE_MS = 3 * 60 * 1000L
    }
}

// ─── Entity → Domain mapper ──────────────────────────────────────────────────

private val cacheJson = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private fun JournalGridCacheEntity.toResponse(): JournalGridResponse? =
    runCatching {
        cacheJson.decodeFromString<JournalGridResponse>(jsonData)
    }.getOrNull()
