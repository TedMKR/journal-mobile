package com.journal.core.data.repository

import com.journal.core.data.util.Resource
import com.journal.core.data.util.networkBoundResource
import com.journal.core.database.dao.DashboardCacheDao
import com.journal.core.database.entity.DashboardCacheEntity
import com.journal.core.model.teacher.AcademicPeriod
import com.journal.core.model.teacher.CurrentAttestationPrefill
import com.journal.core.model.teacher.DocumentTask
import com.journal.core.model.teacher.JobAccepted
import com.journal.core.model.teacher.RequestReportPayload
import com.journal.core.model.teacher.TeacherLesson
import com.journal.core.network.api.JournalApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody

@Serializable
data class TeacherVedCatalogData(
    val periods: List<AcademicPeriod> = emptyList(),
    val lessons: List<TeacherLesson> = emptyList(),
    val selectedPeriodId: String? = null
)

@Singleton
class TeacherVedRepository @Inject constructor(
    private val api: JournalApi,
    private val dashboardCacheDao: DashboardCacheDao,
    private val json: Json
) {

    fun getCatalog(
        periodId: String? = null,
        cacheMaxAgeMs: Long = CACHE_MAX_AGE_MS
    ): Flow<Resource<TeacherVedCatalogData?>> {
        val key = catalogCacheKey(periodId)
        return networkBoundResource(
            localFlow = {
                dashboardCacheDao.observe(key)
                    .map { entity -> entity?.toTeacherVedCatalogData(json) }
            },
            shouldFetch = { cached ->
                if (cached == null) return@networkBoundResource true
                val entity = dashboardCacheDao.get(key)
                entity == null || System.currentTimeMillis() - entity.cachedAt > cacheMaxAgeMs
            },
            fetch = {
                val periods = api.getAcademicPeriods(includeClosed = true).data
                val selectedPeriodId = periodId?.takeIf { it.isNotBlank() }
                    ?: periods.firstOrNull { it.isActive }?.id
                    ?: periods.firstOrNull()?.id
                TeacherVedCatalogData(
                    periods = periods,
                    lessons = selectedPeriodId?.let { loadLessonsForPeriod(it, periods) }.orEmpty(),
                    selectedPeriodId = selectedPeriodId
                )
            },
            saveFetchResult = { data ->
                dashboardCacheDao.upsert(data.toEntity(key, json))
            }
        )
    }

    suspend fun getPrefill(
        groupId: String,
        disciplineId: String,
        academicPeriodId: String
    ): CurrentAttestationPrefill = api.getCurrentAttestationPrefill(
        groupId = groupId,
        disciplineId = disciplineId,
        academicPeriodId = academicPeriodId
    )

    suspend fun requestReport(
        idempotencyKey: String,
        payload: RequestReportPayload
    ): JobAccepted = api.requestCurrentAttestationReport(idempotencyKey, payload)

    suspend fun getReportStatus(jobId: String): DocumentTask = api.getReportStatus(jobId)

    suspend fun downloadReportFile(jobId: String): ResponseBody = api.downloadReportFile(jobId)

    private suspend fun loadLessonsForPeriod(
        periodId: String,
        periods: List<AcademicPeriod>
    ): List<TeacherLesson> {
        val period = periods.find { it.id == periodId }
        if (period != null) {
            val dateFrom = period.startsAt.take(10)
            val dateTo = period.endsAt.take(10)
            val all = mutableListOf<TeacherLesson>()
            val pageSize = 200
            var offset = 0
            while (true) {
                val page = api.getLessons(
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    limit = pageSize,
                    offset = offset
                )
                all.addAll(page.lessons)
                val total = page.total ?: break
                if (all.size >= total) break
                offset += pageSize
            }
            return all
        }

        return api.getLessons(periodId = periodId, limit = 500).lessons
    }

    companion object {
        const val CACHE_MAX_AGE_MS = 5 * 60 * 1000L
        private const val SCREEN_KEY = "teacher_ved"
        private const val AUTO_PERIOD_KEY = "auto"

        fun catalogCacheKey(periodId: String?): String =
            "$SCREEN_KEY|${periodId?.takeIf { it.isNotBlank() } ?: AUTO_PERIOD_KEY}"
    }
}

private fun TeacherVedCatalogData.toEntity(key: String, json: Json) = DashboardCacheEntity(
    key = key,
    jsonData = json.encodeToString(this)
)

private fun DashboardCacheEntity.toTeacherVedCatalogData(json: Json): TeacherVedCatalogData =
    json.decodeFromString(TeacherVedCatalogData.serializer(), jsonData)
