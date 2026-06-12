package com.journal.core.data.repository

import com.journal.core.data.util.Resource
import com.journal.core.data.util.networkBoundResource
import com.journal.core.database.dao.StudentProfileCacheDao
import com.journal.core.database.dao.StudentLessonDao
import com.journal.core.database.dao.StudentSubjectCardCacheDao
import com.journal.core.database.entity.StudentProfileCacheEntity
import com.journal.core.database.entity.StudentLessonEntity
import com.journal.core.database.entity.StudentSubjectCardCacheEntity
import com.journal.core.model.teacher.StudentLesson
import com.journal.core.model.teacher.StudentProfile
import com.journal.core.model.teacher.StudentSubjectCard
import com.journal.core.model.teacher.StudentSubjectSummary
import com.journal.core.network.api.JournalApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class StudentDashboardData(
    val profile: StudentProfile? = null,
    val subjects: List<StudentSubjectSummary> = emptyList()
)

@Singleton
class StudentRepository @Inject constructor(
    private val api: JournalApi,
    private val studentLessonDao: StudentLessonDao,
    private val subjectCardCacheDao: StudentSubjectCardCacheDao,
    private val profileCacheDao: StudentProfileCacheDao,
    private val json: Json
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

    fun getDashboard(
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<StudentDashboardData?>> = networkBoundResource(
        localFlow = {
            profileCacheDao.observe(StudentProfileCacheEntity.CURRENT_PROFILE_KEY)
                .map { entity -> entity?.toDashboardData(json) }
        },
        shouldFetch = { cached ->
            if (cached == null) return@networkBoundResource true
            val entity = profileCacheDao.get(StudentProfileCacheEntity.CURRENT_PROFILE_KEY)
            entity == null || System.currentTimeMillis() - entity.cachedAt > cacheMaxAgeMs
        },
        fetch = {
            StudentDashboardData(
                profile = api.getStudentProfile(),
                subjects = api.getStudentSubjects().subjects
            )
        },
        saveFetchResult = { dashboard ->
            profileCacheDao.upsert(dashboard.toEntity(json))
        }
    )

    fun getSubjectCard(
        disciplineId: String,
        periodId: String,
        groupId: String,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<StudentSubjectCard?>> {
        val key = StudentSubjectCardCacheEntity.cacheKey(disciplineId, periodId, groupId)
        return networkBoundResource(
            localFlow = {
                subjectCardCacheDao.observe(key).map { entity -> entity?.toDomain(json) }
            },
            shouldFetch = { cached ->
                if (cached == null) return@networkBoundResource true
                val entity = subjectCardCacheDao.get(key)
                entity == null || System.currentTimeMillis() - entity.cachedAt > cacheMaxAgeMs
            },
            fetch = {
                api.getStudentSubjectCard(disciplineId, periodId, groupId)
            },
            saveFetchResult = { card ->
                subjectCardCacheDao.upsert(card.toEntity(key, disciplineId, periodId, groupId, json))
            }
        )
    }

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
    myAttendanceStatus = myAttendanceStatus,
    lessonOrderNumber = lessonOrderNumber
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
    myAttendanceStatus = myAttendanceStatus,
    lessonOrderNumber = lessonOrderNumber
)

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

private fun StudentDashboardData.toEntity(json: Json) = StudentProfileCacheEntity(
    profileJson = json.encodeToString(StudentProfile.serializer(), requireNotNull(profile)),
    subjectsJson = json.encodeToString(
        ListSerializer(StudentSubjectSummary.serializer()),
        subjects
    )
)

private fun StudentProfileCacheEntity.toDashboardData(json: Json) = StudentDashboardData(
    profile = json.decodeFromString(StudentProfile.serializer(), profileJson),
    subjects = json.decodeFromString(
        ListSerializer(StudentSubjectSummary.serializer()),
        subjectsJson
    )
)

private fun StudentSubjectCardCacheEntity.Companion.cacheKey(
    disciplineId: String,
    periodId: String,
    groupId: String
): String = "$disciplineId|$periodId|$groupId"
