package com.journal.core.data.repository

import com.journal.core.data.util.Resource
import com.journal.core.data.util.networkBoundResource
import com.journal.core.database.dao.JournalGridCacheDao
import com.journal.core.database.dao.PendingActionDao
import com.journal.core.database.entity.JournalGridCacheEntity
import com.journal.core.database.entity.PendingActionEntity
import com.journal.core.database.entity.PendingActionType
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepository @Inject constructor(
    private val api: JournalApi,
    private val journalGridCacheDao: JournalGridCacheDao,
    private val pendingActionDao: PendingActionDao,
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
    ): Flow<List<PendingActionEntity>> =
        pendingActionDao.observeForJournal(groupId, disciplineId, periodId)

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
        } catch (_: Exception) {
            // Offline — enqueue
            pendingActionDao.insert(
                PendingActionEntity(
                    actionType = PendingActionType.MARK_ATTENDANCE,
                    entityId = lessonId,
                    payloadJson = json.encodeToString(request),
                    groupId = groupId,
                    disciplineId = disciplineId,
                    periodId = periodId,
                    lessonType = lessonType
                )
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
        } catch (_: Exception) {
            pendingActionDao.insert(
                PendingActionEntity(
                    actionType = PendingActionType.CREATE_GRADE,
                    entityId = "",
                    payloadJson = json.encodeToString(request),
                    groupId = groupId,
                    disciplineId = disciplineId,
                    periodId = periodId,
                    lessonType = lessonType
                )
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
        } catch (_: Exception) {
            pendingActionDao.insert(
                PendingActionEntity(
                    actionType = PendingActionType.UPDATE_GRADE,
                    entityId = gradeId,
                    payloadJson = json.encodeToString(request),
                    groupId = groupId,
                    disciplineId = disciplineId,
                    periodId = periodId,
                    lessonType = lessonType
                )
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
        } catch (_: Exception) {
            pendingActionDao.insert(
                PendingActionEntity(
                    actionType = PendingActionType.CREATE_ASSESSMENT_FORM,
                    entityId = "",
                    payloadJson = json.encodeToString(request),
                    groupId = request.groupId,
                    disciplineId = request.disciplineId,
                    periodId = request.periodId ?: "",
                    lessonType = lessonType
                )
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
        } catch (_: Exception) {
            pendingActionDao.insert(
                PendingActionEntity(
                    actionType = PendingActionType.UPDATE_ASSESSMENT_FORM,
                    entityId = assessmentFormId,
                    payloadJson = json.encodeToString(request),
                    groupId = groupId,
                    disciplineId = disciplineId,
                    periodId = periodId,
                    lessonType = lessonType
                )
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
        } catch (_: Exception) {
            pendingActionDao.insert(
                PendingActionEntity(
                    actionType = PendingActionType.DELETE_ASSESSMENT_FORM,
                    entityId = assessmentFormId,
                    payloadJson = "{}",
                    groupId = groupId,
                    disciplineId = disciplineId,
                    periodId = periodId,
                    lessonType = lessonType
                )
            )
        }
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
        } catch (_: Exception) {
            pendingActionDao.insert(
                PendingActionEntity(
                    actionType = PendingActionType.UPDATE_LESSON_TOPIC,
                    entityId = lessonId,
                    payloadJson = json.encodeToString(request),
                    groupId = groupId,
                    disciplineId = disciplineId,
                    periodId = periodId,
                    lessonType = lessonType
                )
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
