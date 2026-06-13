package com.journal.core.data.repository

import com.journal.core.data.util.Resource
import com.journal.core.data.util.networkBoundResource
import com.journal.core.database.dao.DashboardCacheDao
import com.journal.core.database.entity.DashboardCacheEntity
import com.journal.core.model.teacher.AdminUser
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

@Serializable
data class AdminUsersData(
    val users: List<AdminUser> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20
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

    fun getUsers(
        page: Int = 1,
        pageSize: Int = 20,
        query: String? = null,
        userType: String? = null,
        status: String? = null,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<AdminUsersData?>> {
        val key = usersCacheKey(page, pageSize, query, userType, status)
        return networkBoundResource(
            localFlow = {
                dashboardCacheDao.observe(key)
                    .map { entity -> entity?.toAdminUsersData(json) }
            },
            shouldFetch = { cached ->
                if (cached == null) return@networkBoundResource true
                val entity = dashboardCacheDao.get(key)
                entity == null || System.currentTimeMillis() - entity.cachedAt > cacheMaxAgeMs
            },
            fetch = {
                val response = api.getAdminUsers(
                    page = page,
                    pageSize = pageSize,
                    query = query,
                    userType = userType,
                    status = status
                )
                AdminUsersData(
                    users = response.data,
                    total = response.meta?.total ?: response.data.size,
                    page = response.meta?.page ?: page,
                    pageSize = response.meta?.pageSize ?: pageSize
                )
            },
            saveFetchResult = { users ->
                dashboardCacheDao.upsert(users.toEntity(key, json))
            }
        )
    }

    companion object {
        const val CACHE_MAX_AGE_MS = 5 * 60 * 1000L
        private const val DASHBOARD_SCREEN_KEY = "admin_dashboard"
        private const val USERS_SCREEN_KEY = "admin_users"
        private const val FALLBACK_USER_KEY = "current"

        fun dashboardCacheKey(userId: String?): String =
            "$DASHBOARD_SCREEN_KEY|${userId?.takeIf(String::isNotBlank) ?: FALLBACK_USER_KEY}"

        fun usersCacheKey(
            page: Int,
            pageSize: Int,
            query: String?,
            userType: String?,
            status: String?
        ): String = listOf(
            USERS_SCREEN_KEY,
            page.toString(),
            pageSize.toString(),
            query.cachePart(),
            userType.cachePart(),
            status.cachePart()
        ).joinToString("|")
    }
}

private fun AdminDashboardData.toEntity(key: String, json: Json) = DashboardCacheEntity(
    key = key,
    jsonData = json.encodeToString(this)
)

private fun DashboardCacheEntity.toAdminDashboardData(json: Json): AdminDashboardData =
    json.decodeFromString(AdminDashboardData.serializer(), jsonData)

private fun AdminUsersData.toEntity(key: String, json: Json) = DashboardCacheEntity(
    key = key,
    jsonData = json.encodeToString(this)
)

private fun DashboardCacheEntity.toAdminUsersData(json: Json): AdminUsersData =
    json.decodeFromString(AdminUsersData.serializer(), jsonData)

private fun String?.cachePart(): String = this?.trim()?.takeIf { it.isNotBlank() } ?: "_"
