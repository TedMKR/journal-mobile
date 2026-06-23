package com.journal.core.data.repository

import com.journal.core.data.util.Resource
import com.journal.core.data.util.networkBoundResource
import com.journal.core.database.dao.DashboardCacheDao
import com.journal.core.database.entity.DashboardCacheEntity
import com.journal.core.model.teacher.GrantJournalAccessRequest
import com.journal.core.model.teacher.GroupPerformanceEntry
import com.journal.core.model.teacher.JournalAccessGrantResponse
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.TeacherLesson
import com.journal.core.model.teacher.TeacherProfile
import com.journal.core.model.teacher.TeacherStats
import com.journal.core.network.api.JournalApi
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TeacherDashboardData(
    val lessons: List<TeacherLesson> = emptyList(),
    val stats: TeacherStats? = null,
    val defaultJournal: JournalGridResponse? = null,
    val activePeriodId: String? = null,
    val groupsPerformance: List<GroupPerformanceEntry> = emptyList()
)

@Serializable
data class TeacherDashboardJournalData(
    val journal: JournalGridResponse? = null
)

@Serializable
data class TeacherDashboardTeachersData(
    val teachers: List<TeacherProfile> = emptyList()
)

@Singleton
class TeacherDashboardRepository @Inject constructor(
    private val api: JournalApi,
    private val dashboardCacheDao: DashboardCacheDao,
    private val json: Json
) {

    fun getDashboard(
        userId: String? = null,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<TeacherDashboardData?>> {
        val key = cacheKey(userId)
        return networkBoundResource(
            localFlow = {
                dashboardCacheDao.observe(key)
                    .map { entity -> entity?.toDashboardData(json) }
            },
            shouldFetch = { cached ->
                if (cached == null) return@networkBoundResource true
                val entity = dashboardCacheDao.get(key)
                entity == null || System.currentTimeMillis() - entity.cachedAt > cacheMaxAgeMs
            },
            fetch = {
                val (dateFrom, dateTo) = currentWeekRange()
                val lessons = api.getLessons(
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    limit = 200
                ).lessons
                val stats = runCatching { api.getTeacherStats() }.getOrNull()
                val defaultJournal = lessons.firstOrNull {
                    it.groupId != null && it.disciplineId != null && it.periodId != null
                }?.let { lesson ->
                    runCatching {
                        api.getGroupJournalGrid(
                            groupId = lesson.groupId.orEmpty(),
                            disciplineId = lesson.disciplineId.orEmpty(),
                            academicPeriodId = lesson.periodId.orEmpty()
                        )
                    }.getOrNull()
                }
                val activePeriodId = runCatching {
                    api.getAcademicPeriods(includeClosed = false).data
                        .firstOrNull { it.isActive }?.id
                }.getOrNull()
                val groupsPerformance = runCatching {
                    api.getGroupsPerformance(activePeriodId).groups
                }.getOrDefault(emptyList())

                TeacherDashboardData(
                    lessons = lessons,
                    stats = stats,
                    defaultJournal = defaultJournal,
                    activePeriodId = activePeriodId,
                    groupsPerformance = groupsPerformance
                )
            },
            saveFetchResult = { dashboard ->
                dashboardCacheDao.upsert(dashboard.toEntity(key, json))
            }
        )
    }

    fun getSelectedJournal(
        groupId: String,
        disciplineId: String,
        periodId: String,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<TeacherDashboardJournalData?>> {
        val key = selectedJournalCacheKey(groupId, disciplineId, periodId)
        return networkBoundResource(
            localFlow = {
                dashboardCacheDao.observe(key)
                    .map { entity -> entity?.toTeacherDashboardJournalData(json) }
            },
            shouldFetch = { cached ->
                if (cached == null) return@networkBoundResource true
                val entity = dashboardCacheDao.get(key)
                entity == null || System.currentTimeMillis() - entity.cachedAt > cacheMaxAgeMs
            },
            fetch = {
                TeacherDashboardJournalData(
                    journal = api.getGroupJournalGrid(
                        groupId = groupId,
                        disciplineId = disciplineId,
                        academicPeriodId = periodId
                    )
                )
            },
            saveFetchResult = { data ->
                dashboardCacheDao.upsert(data.toEntity(key, json))
            }
        )
    }

    fun getAccessTeachers(
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<TeacherDashboardTeachersData?>> {
        val key = teachersCacheKey()
        return networkBoundResource(
            localFlow = {
                dashboardCacheDao.observe(key)
                    .map { entity -> entity?.toTeacherDashboardTeachersData(json) }
            },
            shouldFetch = { cached ->
                if (cached == null) return@networkBoundResource true
                val entity = dashboardCacheDao.get(key)
                entity == null || System.currentTimeMillis() - entity.cachedAt > cacheMaxAgeMs
            },
            fetch = {
                TeacherDashboardTeachersData(
                    teachers = api.getTeachers(limit = 200).data
                        .filter { !it.keycloakId.isNullOrBlank() }
                )
            },
            saveFetchResult = { data ->
                dashboardCacheDao.upsert(data.toEntity(key, json))
            }
        )
    }

    suspend fun grantJournalAccess(request: GrantJournalAccessRequest): JournalAccessGrantResponse =
        api.grantJournalAccess(request)

    companion object {
        const val CACHE_MAX_AGE_MS = 5 * 60 * 1000L
        private const val SCREEN_KEY = "teacher_dashboard"
        private const val SELECTED_JOURNAL_SCREEN_KEY = "teacher_dashboard_journal"
        private const val TEACHERS_SCREEN_KEY = "teacher_dashboard_teachers"
        private const val FALLBACK_USER_KEY = "current"

        fun cacheKey(userId: String?): String =
            "$SCREEN_KEY|${userId?.takeIf(String::isNotBlank) ?: FALLBACK_USER_KEY}"

        fun selectedJournalCacheKey(
            groupId: String,
            disciplineId: String,
            periodId: String
        ): String = listOf(
            SELECTED_JOURNAL_SCREEN_KEY,
            groupId.cachePart(),
            disciplineId.cachePart(),
            periodId.cachePart()
        ).joinToString("|")

        fun teachersCacheKey(): String = "$TEACHERS_SCREEN_KEY|all"
    }
}

private fun TeacherDashboardData.toEntity(key: String, json: Json) = DashboardCacheEntity(
    key = key,
    jsonData = json.encodeToString(this)
)

private fun DashboardCacheEntity.toDashboardData(json: Json): TeacherDashboardData =
    json.decodeFromString(TeacherDashboardData.serializer(), jsonData)

private fun TeacherDashboardJournalData.toEntity(key: String, json: Json) = DashboardCacheEntity(
    key = key,
    jsonData = json.encodeToString(this)
)

private fun DashboardCacheEntity.toTeacherDashboardJournalData(json: Json): TeacherDashboardJournalData =
    json.decodeFromString(TeacherDashboardJournalData.serializer(), jsonData)

private fun TeacherDashboardTeachersData.toEntity(key: String, json: Json) = DashboardCacheEntity(
    key = key,
    jsonData = json.encodeToString(this)
)

private fun DashboardCacheEntity.toTeacherDashboardTeachersData(json: Json): TeacherDashboardTeachersData =
    json.decodeFromString(TeacherDashboardTeachersData.serializer(), jsonData)

private fun currentWeekRange(): Pair<String, String> {
    val today = LocalDate.now()
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val sunday = monday.plusDays(6)
    return monday.toString() to sunday.toString()
}

private fun String?.cachePart(): String = this?.trim()?.takeIf { it.isNotBlank() } ?: "_"
