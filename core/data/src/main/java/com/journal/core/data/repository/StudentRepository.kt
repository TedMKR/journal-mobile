package com.journal.core.data.repository

import com.journal.core.data.util.Resource
import com.journal.core.data.util.networkBoundResource
import com.journal.core.database.dao.StudentLessonDao
import com.journal.core.database.entity.StudentLessonEntity
import com.journal.core.model.teacher.StudentLesson
import com.journal.core.network.api.JournalApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRepository @Inject constructor(
    private val api: JournalApi,
    private val studentLessonDao: StudentLessonDao
) {

    fun getLessons(
        dateFrom: String? = null,
        dateTo: String? = null,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<List<StudentLesson>>> = networkBoundResource(
        localFlow = {
            if (dateFrom != null && dateTo != null) {
                studentLessonDao.observeByDateRange(dateFrom, dateTo)
            } else {
                studentLessonDao.observeAll()
            }.map { entities -> entities.map { it.toDomain() } }
        },
        shouldFetch = { cached ->
            if (cached.isNullOrEmpty()) return@networkBoundResource true
            val lastCached = studentLessonDao.lastCachedAt() ?: 0L
            System.currentTimeMillis() - lastCached > cacheMaxAgeMs
        },
        fetch = {
            api.getStudentLessons(
                dateFrom = dateFrom,
                dateTo = dateTo,
                limit = 200
            )
        },
        saveFetchResult = { response ->
            val entities = response.lessons.map { it.toEntity() }
            studentLessonDao.deleteAll()
            studentLessonDao.insertAll(entities)
        }
    )

    companion object {
        /** 5 minutes */
        const val CACHE_MAX_AGE_MS = 5 * 60 * 1000L
    }
}

// ─── Mappers ────────────────────────────────────────────────────────────────

private fun StudentLessonEntity.toDomain() = StudentLesson(
    id = id,
    disciplineId = disciplineId,
    disciplineName = disciplineName,
    groupId = groupId,
    groupName = groupName,
    teacherId = teacherId,
    teacherName = teacherName,
    periodId = periodId,
    scheduledAt = scheduledAt,
    endsAt = endsAt,
    lessonType = lessonType,
    status = status,
    location = location,
    topic = topic,
    myAttendanceStatus = myAttendanceStatus
)

private fun StudentLesson.toEntity() = StudentLessonEntity(
    id = id,
    disciplineId = disciplineId,
    disciplineName = disciplineName,
    groupId = groupId,
    groupName = groupName,
    teacherId = teacherId,
    teacherName = teacherName,
    periodId = periodId,
    scheduledAt = scheduledAt,
    endsAt = endsAt,
    lessonType = lessonType,
    status = status,
    location = location,
    topic = topic,
    myAttendanceStatus = myAttendanceStatus
)
