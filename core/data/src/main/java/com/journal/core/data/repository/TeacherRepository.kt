package com.journal.core.data.repository

import com.journal.core.data.util.Resource
import com.journal.core.data.util.networkBoundResource
import com.journal.core.database.dao.TeacherLessonDao
import com.journal.core.database.entity.TeacherLessonEntity
import com.journal.core.model.teacher.TeacherLesson
import com.journal.core.network.api.JournalApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeacherRepository @Inject constructor(
    private val api: JournalApi,
    private val teacherLessonDao: TeacherLessonDao
) {

    /**
     * Returns teacher lessons as an offline-first flow.
     * Cache is refreshed when it is empty or older than [cacheMaxAgeMs].
     */
    fun getLessons(
        dateFrom: String? = null,
        dateTo: String? = null,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<List<TeacherLesson>>> = networkBoundResource(
        localFlow = {
            if (dateFrom != null && dateTo != null) {
                teacherLessonDao.observeByDateRange(dateFrom, dateTo)
            } else {
                teacherLessonDao.observeAll()
            }.map { entities -> entities.map { it.toDomain() } }
        },
        shouldFetch = { cached ->
            if (cached.isNullOrEmpty()) return@networkBoundResource true
            val lastCached = teacherLessonDao.lastCachedAt() ?: 0L
            System.currentTimeMillis() - lastCached > cacheMaxAgeMs
        },
        fetch = {
            api.getLessons(
                dateFrom = dateFrom,
                dateTo = dateTo,
                limit = 200
            )
        },
        saveFetchResult = { response ->
            val entities = response.lessons.map { it.toEntity() }
            teacherLessonDao.deleteAll()
            teacherLessonDao.insertAll(entities)
        }
    )

    companion object {
        /** 5 minutes */
        const val CACHE_MAX_AGE_MS = 5 * 60 * 1000L
    }
}

// ─── Mappers ────────────────────────────────────────────────────────────────

private fun TeacherLessonEntity.toDomain() = TeacherLesson(
    id = id,
    disciplineName = disciplineName,
    groupName = groupName,
    lessonType = lessonType,
    scheduledAt = scheduledAt,
    endsAt = endsAt,
    disciplineId = disciplineId,
    groupId = groupId,
    periodId = periodId,
    location = location,
    status = status,
    attendanceMarked = attendanceMarked,
    topic = topic,
    teacherId = teacherId,
    teacherName = teacherName,
    lessonOrderNumber = lessonOrderNumber
)

private fun TeacherLesson.toEntity() = TeacherLessonEntity(
    id = id,
    disciplineName = disciplineName,
    groupName = groupName,
    lessonType = lessonType,
    scheduledAt = scheduledAt,
    endsAt = endsAt,
    disciplineId = disciplineId,
    groupId = groupId,
    periodId = periodId,
    location = location,
    status = status,
    attendanceMarked = attendanceMarked,
    topic = topic,
    teacherId = teacherId,
    teacherName = teacherName,
    lessonOrderNumber = lessonOrderNumber
)
