package com.journal.core.data.repository

import com.journal.core.data.util.Resource
import com.journal.core.data.util.networkBoundResource
import com.journal.core.database.dao.DashboardCacheDao
import com.journal.core.database.entity.DashboardCacheEntity
import com.journal.core.model.teacher.AcademicGroup
import com.journal.core.model.teacher.AdminAccessBinding
import com.journal.core.model.teacher.AdminJournalContext
import com.journal.core.model.teacher.AdminPeriod
import com.journal.core.model.teacher.AdminUser
import com.journal.core.model.teacher.AuditEvent
import com.journal.core.model.teacher.Discipline
import com.journal.core.model.teacher.ProblemStudentEntry
import com.journal.core.model.teacher.ProblemStudentsMeta
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

@Serializable
data class AdminAuditData(
    val events: List<AuditEvent> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20
)

@Serializable
data class AdminJournalsData(
    val journals: List<AdminJournalContext> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20
)

@Serializable
data class AdminPeriodsData(
    val periods: List<AdminPeriod> = emptyList()
)

@Serializable
data class AdminAccessData(
    val bindings: List<AdminAccessBinding> = emptyList()
)

@Serializable
data class AdminProblemStudentsData(
    val students: List<ProblemStudentEntry> = emptyList(),
    val meta: ProblemStudentsMeta = ProblemStudentsMeta(),
    val periods: List<AdminPeriod> = emptyList(),
    val groups: List<AcademicGroup> = emptyList(),
    val disciplines: List<Discipline> = emptyList()
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
                val periods = api.getAdminPeriods(includeClosed = true)
                val summary = runCatching { api.getAdminDashboardSummary() }.getOrNull()
                if (summary != null) {
                    AdminDashboardData(
                        journalsCount = summary.journals.total,
                        usersCount = summary.users.total,
                        periodsCount = periods.data.size,
                        documentsCount = summary.documents.total
                    )
                } else {
                    val journals = api.getAdminJournals(pageSize = 1)
                    val users = api.getAdminUsers(pageSize = 1)
                    val documents = api.getAdminDocuments(pageSize = 1)
                    AdminDashboardData(
                        journalsCount = journals.meta?.total ?: journals.data.size,
                        usersCount = users.meta?.total ?: users.data.size,
                        periodsCount = periods.data.size,
                        documentsCount = documents.meta?.total ?: documents.data.size
                    )
                }
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

    fun getAudit(
        page: Int = 1,
        pageSize: Int = 20,
        action: String? = null,
        entityType: String? = null,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<AdminAuditData?>> {
        val key = auditCacheKey(page, pageSize, action, entityType)
        return networkBoundResource(
            localFlow = {
                dashboardCacheDao.observe(key)
                    .map { entity -> entity?.toAdminAuditData(json) }
            },
            shouldFetch = { cached ->
                if (cached == null) return@networkBoundResource true
                val entity = dashboardCacheDao.get(key)
                entity == null || System.currentTimeMillis() - entity.cachedAt > cacheMaxAgeMs
            },
            fetch = {
                val response = api.getAdminAudit(
                    page = page,
                    pageSize = pageSize,
                    action = action,
                    entityType = entityType
                )
                AdminAuditData(
                    events = response.data,
                    total = response.meta?.total ?: response.data.size,
                    page = response.meta?.page ?: page,
                    pageSize = response.meta?.pageSize ?: pageSize
                )
            },
            saveFetchResult = { audit ->
                dashboardCacheDao.upsert(audit.toEntity(key, json))
            }
        )
    }

    fun getJournals(
        page: Int = 1,
        pageSize: Int = 20,
        status: String? = null,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<AdminJournalsData?>> {
        val key = journalsCacheKey(page, pageSize, status)
        return networkBoundResource(
            localFlow = {
                dashboardCacheDao.observe(key)
                    .map { entity -> entity?.toAdminJournalsData(json) }
            },
            shouldFetch = { cached ->
                if (cached == null) return@networkBoundResource true
                val entity = dashboardCacheDao.get(key)
                entity == null || System.currentTimeMillis() - entity.cachedAt > cacheMaxAgeMs
            },
            fetch = {
                val response = api.getAdminJournals(
                    page = page,
                    pageSize = pageSize,
                    status = status
                )
                AdminJournalsData(
                    journals = response.data,
                    total = response.meta?.total ?: response.data.size,
                    page = response.meta?.page ?: page,
                    pageSize = response.meta?.pageSize ?: pageSize
                )
            },
            saveFetchResult = { journals ->
                dashboardCacheDao.upsert(journals.toEntity(key, json))
            }
        )
    }

    fun getPeriods(
        includeClosed: Boolean = true,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<AdminPeriodsData?>> {
        val key = periodsCacheKey(includeClosed)
        return networkBoundResource(
            localFlow = {
                dashboardCacheDao.observe(key)
                    .map { entity -> entity?.toAdminPeriodsData(json) }
            },
            shouldFetch = { cached ->
                if (cached == null) return@networkBoundResource true
                val entity = dashboardCacheDao.get(key)
                entity == null || System.currentTimeMillis() - entity.cachedAt > cacheMaxAgeMs
            },
            fetch = {
                AdminPeriodsData(periods = api.getAdminPeriods(includeClosed = includeClosed).data)
            },
            saveFetchResult = { periods ->
                dashboardCacheDao.upsert(periods.toEntity(key, json))
            }
        )
    }

    fun getAccessBindings(
        activeOnly: Boolean,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<AdminAccessData?>> {
        val key = accessCacheKey(activeOnly)
        return networkBoundResource(
            localFlow = {
                dashboardCacheDao.observe(key)
                    .map { entity -> entity?.toAdminAccessData(json) }
            },
            shouldFetch = { cached ->
                if (cached == null) return@networkBoundResource true
                val entity = dashboardCacheDao.get(key)
                entity == null || System.currentTimeMillis() - entity.cachedAt > cacheMaxAgeMs
            },
            fetch = {
                AdminAccessData(bindings = api.getAdminAccessBindings(activeOnly = activeOnly).data)
            },
            saveFetchResult = { access ->
                dashboardCacheDao.upsert(access.toEntity(key, json))
            }
        )
    }

    fun getProblemStudents(
        periodId: String? = null,
        groupId: String? = null,
        disciplineId: String? = null,
        minGrades: Int? = null,
        minFailingGrades: Int? = null,
        failingPercentThreshold: Int? = null,
        limit: Int = 10,
        offset: Int = 0,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<AdminProblemStudentsData?>> {
        val key = problemStudentsCacheKey(
            periodId = periodId,
            groupId = groupId,
            disciplineId = disciplineId,
            minGrades = minGrades,
            minFailingGrades = minFailingGrades,
            failingPercentThreshold = failingPercentThreshold,
            limit = limit,
            offset = offset
        )
        return networkBoundResource(
            localFlow = {
                dashboardCacheDao.observe(key)
                    .map { entity -> entity?.toAdminProblemStudentsData(json) }
            },
            shouldFetch = { cached ->
                if (cached == null) return@networkBoundResource true
                val entity = dashboardCacheDao.get(key)
                entity == null || System.currentTimeMillis() - entity.cachedAt > cacheMaxAgeMs
            },
            fetch = {
                val response = api.getAdminProblemStudents(
                    periodId = periodId,
                    groupId = groupId,
                    disciplineId = disciplineId,
                    minGrades = minGrades,
                    minFailingGrades = minFailingGrades,
                    failingPercentThreshold = failingPercentThreshold,
                    limit = limit,
                    offset = offset
                )
                AdminProblemStudentsData(
                    students = response.data,
                    meta = response.meta,
                    periods = api.getAdminPeriods(includeClosed = true).data,
                    groups = api.getGroups(limit = 200).data,
                    disciplines = api.getDisciplines(limit = 200).data
                )
            },
            saveFetchResult = { data ->
                dashboardCacheDao.upsert(data.toEntity(key, json))
            }
        )
    }

    companion object {
        const val CACHE_MAX_AGE_MS = 5 * 60 * 1000L
        private const val DASHBOARD_SCREEN_KEY = "admin_dashboard"
        private const val USERS_SCREEN_KEY = "admin_users"
        private const val AUDIT_SCREEN_KEY = "admin_audit"
        private const val JOURNALS_SCREEN_KEY = "admin_journals"
        private const val PERIODS_SCREEN_KEY = "admin_periods"
        private const val ACCESS_SCREEN_KEY = "admin_access"
        private const val PROBLEM_STUDENTS_SCREEN_KEY = "admin_problem_students"
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

        fun auditCacheKey(
            page: Int,
            pageSize: Int,
            action: String?,
            entityType: String?
        ): String = listOf(
            AUDIT_SCREEN_KEY,
            page.toString(),
            pageSize.toString(),
            action.cachePart(),
            entityType.cachePart()
        ).joinToString("|")

        fun journalsCacheKey(
            page: Int,
            pageSize: Int,
            status: String?
        ): String = listOf(
            JOURNALS_SCREEN_KEY,
            page.toString(),
            pageSize.toString(),
            status.cachePart()
        ).joinToString("|")

        fun periodsCacheKey(includeClosed: Boolean): String =
            "$PERIODS_SCREEN_KEY|$includeClosed"

        fun accessCacheKey(activeOnly: Boolean): String =
            "$ACCESS_SCREEN_KEY|$activeOnly"

        fun problemStudentsCacheKey(
            periodId: String?,
            groupId: String?,
            disciplineId: String?,
            minGrades: Int?,
            minFailingGrades: Int?,
            failingPercentThreshold: Int?,
            limit: Int,
            offset: Int
        ): String = listOf(
            PROBLEM_STUDENTS_SCREEN_KEY,
            periodId.cachePart(),
            groupId.cachePart(),
            disciplineId.cachePart(),
            minGrades?.toString() ?: "_",
            minFailingGrades?.toString() ?: "_",
            failingPercentThreshold?.toString() ?: "_",
            limit.toString(),
            offset.toString()
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

private fun AdminAuditData.toEntity(key: String, json: Json) = DashboardCacheEntity(
    key = key,
    jsonData = json.encodeToString(this)
)

private fun DashboardCacheEntity.toAdminAuditData(json: Json): AdminAuditData =
    json.decodeFromString(AdminAuditData.serializer(), jsonData)

private fun AdminJournalsData.toEntity(key: String, json: Json) = DashboardCacheEntity(
    key = key,
    jsonData = json.encodeToString(this)
)

private fun DashboardCacheEntity.toAdminJournalsData(json: Json): AdminJournalsData =
    json.decodeFromString(AdminJournalsData.serializer(), jsonData)

private fun AdminPeriodsData.toEntity(key: String, json: Json) = DashboardCacheEntity(
    key = key,
    jsonData = json.encodeToString(this)
)

private fun DashboardCacheEntity.toAdminPeriodsData(json: Json): AdminPeriodsData =
    json.decodeFromString(AdminPeriodsData.serializer(), jsonData)

private fun AdminAccessData.toEntity(key: String, json: Json) = DashboardCacheEntity(
    key = key,
    jsonData = json.encodeToString(this)
)

private fun DashboardCacheEntity.toAdminAccessData(json: Json): AdminAccessData =
    json.decodeFromString(AdminAccessData.serializer(), jsonData)

private fun AdminProblemStudentsData.toEntity(key: String, json: Json) = DashboardCacheEntity(
    key = key,
    jsonData = json.encodeToString(this)
)

private fun DashboardCacheEntity.toAdminProblemStudentsData(json: Json): AdminProblemStudentsData =
    json.decodeFromString(AdminProblemStudentsData.serializer(), jsonData)

private fun String?.cachePart(): String = this?.trim()?.takeIf { it.isNotBlank() } ?: "_"
