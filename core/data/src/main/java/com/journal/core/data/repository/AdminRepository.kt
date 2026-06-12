package com.journal.core.data.repository

import com.journal.core.data.util.Resource
import com.journal.core.data.util.networkBoundResource
import com.journal.core.database.dao.DashboardCacheDao
import com.journal.core.database.entity.DashboardCacheEntity
import com.journal.core.network.api.JournalApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AdminDashboardData(
    val journalsCount: Int = 0,
    val usersCount: Int = 0,
    val periodsCount: Int = 0,
    val documentsCount: Int = 0
)

@Singleton
class AdminRepository @Inject constructor(
    private val api: JournalApi,
    private val dashboardCacheDao: DashboardCacheDao,
    private val json: Json
) {

    fun getDashboard(
        userId: String? = null,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<AdminDashboardData?>> {
        val key = dashboardCacheKey(userId)
        return networkBoundResource(
            localFlow = {
                dashboardCacheDao.observe(key)
                    .map { entity -> entity?.toAdminDashboardData(json) }
            },
            shouldFetch = { cached ->
                if (cached == null) return@networkBoundResource true
                val entity = dashboardCacheDao.get(key)
                entity == null || System.currentTimeMillis() - entity.cachedAt > cacheMaxAgeMs
            },
            fetch = {
                val journals = api.getAdminJournals(pageSize = 1)
                val users = api.getAdminUsers(pageSize = 1)
                val periods = api.getAdminPeriods(includeClosed = true)
                val documents = api.getAdminDocuments(pageSize = 1)
                AdminDashboardData(
                    journalsCount = journals.meta?.total ?: journals.data.size,
                    usersCount = users.meta?.total ?: users.data.size,
                    periodsCount = periods.data.size,
                    documentsCount = documents.meta?.total ?: documents.data.size
                )
            },
            saveFetchResult = { dashboard ->
                dashboardCacheDao.upsert(dashboard.toEntity(key, json))
            }
        )
    }

    companion object {
        const val CACHE_MAX_AGE_MS = 5 * 60 * 1000L
        private const val DASHBOARD_SCREEN_KEY = "admin_dashboard"
        private const val FALLBACK_USER_KEY = "current"

        fun dashboardCacheKey(userId: String?): String =
            "$DASHBOARD_SCREEN_KEY|${userId?.takeIf(String::isNotBlank) ?: FALLBACK_USER_KEY}"
    }
}

private fun AdminDashboardData.toEntity(key: String, json: Json) = DashboardCacheEntity(
    key = key,
    jsonData = json.encodeToString(this)
)

private fun DashboardCacheEntity.toAdminDashboardData(json: Json): AdminDashboardData =
    json.decodeFromString(AdminDashboardData.serializer(), jsonData)
