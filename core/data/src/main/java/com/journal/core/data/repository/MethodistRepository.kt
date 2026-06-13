package com.journal.core.data.repository

import com.journal.core.data.util.Resource
import com.journal.core.data.util.networkBoundResource
import com.journal.core.database.dao.DashboardCacheDao
import com.journal.core.database.entity.DashboardCacheEntity
import com.journal.core.model.teacher.AcademicGroup
import com.journal.core.model.teacher.AcademicPeriod
import com.journal.core.model.teacher.Discipline
import com.journal.core.model.teacher.JournalContext
import com.journal.core.model.teacher.TeacherProfile
import com.journal.core.network.api.JournalApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MethodistDashboardData(
    val periods: List<AcademicPeriod> = emptyList(),
    val disciplines: List<Discipline> = emptyList(),
    val teachers: List<TeacherProfile> = emptyList(),
    val groups: List<AcademicGroup> = emptyList(),
    val journals: List<JournalContext> = emptyList()
)

@Serializable
data class MethodistJournalsData(
    val periods: List<AcademicPeriod> = emptyList(),
    val disciplines: List<Discipline> = emptyList(),
    val teachers: List<TeacherProfile> = emptyList(),
    val groups: List<AcademicGroup> = emptyList(),
    val journals: List<JournalContext> = emptyList()
)

@Singleton
class MethodistRepository @Inject constructor(
    private val api: JournalApi,
    private val dashboardCacheDao: DashboardCacheDao,
    private val json: Json
) {

    fun getDashboard(
        userId: String? = null,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<MethodistDashboardData?>> {
        val key = dashboardCacheKey(userId)
        return networkBoundResource(
            localFlow = {
                dashboardCacheDao.observe(key)
                    .map { entity -> entity?.toMethodistDashboardData(json) }
            },
            shouldFetch = { cached ->
                if (cached == null) return@networkBoundResource true
                val entity = dashboardCacheDao.get(key)
                entity == null || System.currentTimeMillis() - entity.cachedAt > cacheMaxAgeMs
            },
            fetch = {
                val periods = api.getAcademicPeriods(includeClosed = true).data
                val activePeriodId = periods.firstOrNull { it.isActive }?.id
                    ?: periods.firstOrNull()?.id
                MethodistDashboardData(
                    periods = periods,
                    disciplines = api.getDisciplines(limit = 200).data,
                    teachers = api.getTeachers(limit = 200).data,
                    groups = api.getGroups(limit = 200).data,
                    journals = api.getJournals(
                        periodId = activePeriodId,
                        limit = 200,
                        offset = 0
                    ).data
                )
            },
            saveFetchResult = { dashboard ->
                dashboardCacheDao.upsert(dashboard.toEntity(key, json))
            }
        )
    }

    fun getJournals(
        periodId: String? = null,
        disciplineId: String? = null,
        groupId: String? = null,
        teacherId: String? = null,
        lessonType: String? = null,
        query: String? = null,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<MethodistJournalsData?>> {
        val key = journalsCacheKey(periodId, disciplineId, groupId, teacherId, lessonType, query)
        return networkBoundResource(
            localFlow = {
                dashboardCacheDao.observe(key)
                    .map { entity -> entity?.toMethodistJournalsData(json) }
            },
            shouldFetch = { cached ->
                if (cached == null) return@networkBoundResource true
                val entity = dashboardCacheDao.get(key)
                entity == null || System.currentTimeMillis() - entity.cachedAt > cacheMaxAgeMs
            },
            fetch = {
                MethodistJournalsData(
                    periods = api.getAcademicPeriods(includeClosed = true).data,
                    disciplines = api.getDisciplines(limit = 200).data,
                    teachers = api.getTeachers(limit = 200).data,
                    groups = api.getGroups(limit = 200).data,
                    journals = api.getJournals(
                        periodId = periodId,
                        disciplineId = disciplineId,
                        groupId = groupId,
                        teacherId = teacherId,
                        lessonType = lessonType,
                        query = query,
                        limit = 200,
                        offset = 0
                    ).data
                )
            },
            saveFetchResult = { data ->
                dashboardCacheDao.upsert(data.toEntity(key, json))
            }
        )
    }

    companion object {
        const val CACHE_MAX_AGE_MS = 5 * 60 * 1000L
        private const val DASHBOARD_SCREEN_KEY = "methodist_dashboard"
        private const val JOURNALS_SCREEN_KEY = "methodist_journals"
        private const val FALLBACK_USER_KEY = "current"

        fun dashboardCacheKey(userId: String?): String =
            "$DASHBOARD_SCREEN_KEY|${userId?.takeIf(String::isNotBlank) ?: FALLBACK_USER_KEY}"

        fun journalsCacheKey(
            periodId: String?,
            disciplineId: String?,
            groupId: String?,
            teacherId: String?,
            lessonType: String?,
            query: String?
        ): String = listOf(
            JOURNALS_SCREEN_KEY,
            periodId.cachePart(),
            disciplineId.cachePart(),
            groupId.cachePart(),
            teacherId.cachePart(),
            lessonType.cachePart(),
            query.cachePart()
        ).joinToString("|")
    }
}

private fun MethodistDashboardData.toEntity(key: String, json: Json) = DashboardCacheEntity(
    key = key,
    jsonData = json.encodeToString(this)
)

private fun DashboardCacheEntity.toMethodistDashboardData(json: Json): MethodistDashboardData =
    json.decodeFromString(MethodistDashboardData.serializer(), jsonData)

private fun MethodistJournalsData.toEntity(key: String, json: Json) = DashboardCacheEntity(
    key = key,
    jsonData = json.encodeToString(this)
)

private fun DashboardCacheEntity.toMethodistJournalsData(json: Json): MethodistJournalsData =
    json.decodeFromString(MethodistJournalsData.serializer(), jsonData)

private fun String?.cachePart(): String = this?.trim()?.takeIf { it.isNotBlank() } ?: "_"
