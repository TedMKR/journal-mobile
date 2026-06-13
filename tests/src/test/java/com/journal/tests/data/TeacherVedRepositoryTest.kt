package com.journal.tests.data

import app.cash.turbine.test
import com.journal.core.data.repository.TeacherVedCatalogData
import com.journal.core.data.repository.TeacherVedRepository
import com.journal.core.data.util.Resource
import com.journal.core.database.dao.DashboardCacheDao
import com.journal.core.database.entity.DashboardCacheEntity
import com.journal.core.model.teacher.AcademicPeriod
import com.journal.core.model.teacher.AcademicPeriodsResponse
import com.journal.core.model.teacher.LessonsResponse
import com.journal.core.model.teacher.TeacherLesson
import com.journal.core.network.api.JournalApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalSerializationApi::class)
class TeacherVedRepositoryTest {

    private lateinit var api: JournalApi
    private lateinit var dao: FakeDashboardCacheDao
    private lateinit var repository: TeacherVedRepository

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val period = AcademicPeriod(
        id = "period-1",
        name = "Spring",
        startsAt = "2026-01-01",
        endsAt = "2026-06-30",
        isClosed = false,
        isActive = true
    )
    private val lesson = TeacherLesson(
        id = "lesson-1",
        disciplineName = "Math",
        groupName = "IS-21",
        lessonType = "practice",
        scheduledAt = "2026-01-12T09:00:00Z",
        disciplineId = "discipline-1",
        groupId = "group-1",
        periodId = period.id
    )

    @Before
    fun setUp() {
        api = mockk()
        dao = FakeDashboardCacheDao()
        repository = TeacherVedRepository(api, dao, json)
    }

    @Test
    fun `getCatalog saves active period lessons to cache`() = runTest {
        stubCatalogApi()

        repository.getCatalog().test {
            assertTrue(awaitItem() is Resource.Loading)

            val success = awaitItem()
            assertTrue(success is Resource.Success)
            val data = (success as Resource.Success).data
            assertEquals(listOf(period), data?.periods)
            assertEquals(listOf(lesson), data?.lessons)
            assertEquals(period.id, data?.selectedPeriodId)

            cancelAndIgnoreRemainingEvents()
        }

        val cached = dao.get(TeacherVedRepository.catalogCacheKey(null))
        assertEquals("teacher_ved|auto", cached?.key)
    }

    @Test
    fun `getCatalog does not call API when cache is fresh`() = runTest {
        dao.upsert(cachedEntity(cachedAt = System.currentTimeMillis()))

        repository.getCatalog(period.id).test {
            assertTrue(awaitItem() is Resource.Loading)
            assertTrue(awaitItem() is Resource.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { api.getAcademicPeriods(any()) }
    }

    @Test
    fun `getCatalog emits Error with cached data when refresh fails`() = runTest {
        dao.upsert(cachedEntity(cachedAt = 0L))
        coEvery { api.getAcademicPeriods(any()) } throws RuntimeException("network down")

        repository.getCatalog(period.id, cacheMaxAgeMs = 1L).test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)
            assertEquals(listOf(lesson), (loading as Resource.Loading).data?.lessons)

            val error = awaitItem()
            assertTrue(error is Resource.Error)
            assertEquals(listOf(lesson), (error as Resource.Error).data?.lessons)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun stubCatalogApi() {
        coEvery { api.getAcademicPeriods(any()) } returns AcademicPeriodsResponse(listOf(period))
        coEvery {
            api.getLessons(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns LessonsResponse(lessons = listOf(lesson), total = 1)
    }

    private fun cachedEntity(cachedAt: Long): DashboardCacheEntity =
        DashboardCacheEntity(
            key = TeacherVedRepository.catalogCacheKey(period.id),
            jsonData = json.encodeToString(
                TeacherVedCatalogData(
                    periods = listOf(period),
                    lessons = listOf(lesson),
                    selectedPeriodId = period.id
                )
            ),
            cachedAt = cachedAt
        )

    private class FakeDashboardCacheDao(
        initial: DashboardCacheEntity? = null
    ) : DashboardCacheDao {
        private val state = MutableStateFlow(initial)

        override fun observe(key: String): Flow<DashboardCacheEntity?> = state

        override suspend fun get(key: String): DashboardCacheEntity? = state.value

        override suspend fun upsert(entity: DashboardCacheEntity) {
            state.value = entity
        }
    }
}
