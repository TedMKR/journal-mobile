package com.journal.tests.data

import app.cash.turbine.test
import com.journal.core.data.repository.MethodistDashboardData
import com.journal.core.data.repository.MethodistRepository
import com.journal.core.data.util.Resource
import com.journal.core.database.dao.DashboardCacheDao
import com.journal.core.database.entity.DashboardCacheEntity
import com.journal.core.model.teacher.AcademicGroup
import com.journal.core.model.teacher.AcademicPeriod
import com.journal.core.model.teacher.AcademicPeriodsResponse
import com.journal.core.model.teacher.CatalogResponse
import com.journal.core.model.teacher.Discipline
import com.journal.core.model.teacher.JournalContext
import com.journal.core.model.teacher.JournalsResponse
import com.journal.core.model.teacher.TeacherProfile
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
class MethodistRepositoryTest {

    private lateinit var api: JournalApi
    private lateinit var dao: FakeDashboardCacheDao
    private lateinit var repository: MethodistRepository

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val samplePeriod = AcademicPeriod(
        id = "period-1",
        name = "Spring",
        startsAt = "2026-01-01",
        endsAt = "2026-06-30",
        isClosed = false,
        isActive = true
    )
    private val sampleDiscipline = Discipline(id = "discipline-1", name = "Math")
    private val sampleTeacher = TeacherProfile(id = "teacher-1", fullName = "Ada Lovelace")
    private val sampleGroup = AcademicGroup(id = "group-1", name = "IS-21")
    private val sampleJournal = JournalContext(
        journalId = "journal-1",
        disciplineId = sampleDiscipline.id,
        disciplineName = sampleDiscipline.name,
        groupId = sampleGroup.id,
        groupName = sampleGroup.name,
        periodId = samplePeriod.id
    )

    @Before
    fun setUp() {
        api = mockk()
        dao = FakeDashboardCacheDao()
        repository = MethodistRepository(api, dao, json)
    }

    @Test
    fun `getDashboard saves remote snapshot to cache`() = runTest {
        stubDashboardApi()

        repository.getDashboard(userId = "methodist-1").test {
            assertTrue(awaitItem() is Resource.Loading)

            val success = awaitItem()
            assertTrue(success is Resource.Success)
            val data = (success as Resource.Success).data
            assertEquals(listOf(samplePeriod), data?.periods)
            assertEquals(listOf(sampleDiscipline), data?.disciplines)
            assertEquals(listOf(sampleTeacher), data?.teachers)
            assertEquals(listOf(sampleGroup), data?.groups)
            assertEquals(listOf(sampleJournal), data?.journals)

            cancelAndIgnoreRemainingEvents()
        }

        val cached = dao.get(MethodistRepository.dashboardCacheKey("methodist-1"))
        assertEquals("methodist_dashboard|methodist-1", cached?.key)
    }

    @Test
    fun `getDashboard does not call API when cache is fresh`() = runTest {
        dao.upsert(cachedEntity(cachedAt = System.currentTimeMillis()))

        repository.getDashboard(userId = "methodist-1").test {
            assertTrue(awaitItem() is Resource.Loading)
            assertTrue(awaitItem() is Resource.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { api.getAcademicPeriods(any()) }
    }

    @Test
    fun `getDashboard emits Error with cached data when refresh fails`() = runTest {
        dao.upsert(cachedEntity(cachedAt = 0L))
        coEvery { api.getAcademicPeriods(any()) } throws RuntimeException("network down")

        repository.getDashboard(userId = "methodist-1", cacheMaxAgeMs = 1L).test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)
            assertEquals(listOf(samplePeriod), (loading as Resource.Loading).data?.periods)

            val error = awaitItem()
            assertTrue(error is Resource.Error)
            assertEquals(listOf(samplePeriod), (error as Resource.Error).data?.periods)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun stubDashboardApi() {
        coEvery { api.getAcademicPeriods(any()) } returns AcademicPeriodsResponse(listOf(samplePeriod))
        coEvery {
            api.getDisciplines(any(), any(), any(), any())
        } returns CatalogResponse(data = listOf(sampleDiscipline))
        coEvery {
            api.getTeachers(any(), any(), any(), any(), any())
        } returns CatalogResponse(data = listOf(sampleTeacher))
        coEvery {
            api.getGroups(any(), any(), any(), any(), any())
        } returns CatalogResponse(data = listOf(sampleGroup))
        coEvery {
            api.getJournals(any(), any(), any(), any(), any(), any(), any(), any())
        } returns JournalsResponse(data = listOf(sampleJournal))
    }

    private fun cachedEntity(cachedAt: Long): DashboardCacheEntity =
        DashboardCacheEntity(
            key = MethodistRepository.dashboardCacheKey("methodist-1"),
            jsonData = json.encodeToString(
                MethodistDashboardData(
                    periods = listOf(samplePeriod),
                    disciplines = listOf(sampleDiscipline),
                    teachers = listOf(sampleTeacher),
                    groups = listOf(sampleGroup),
                    journals = listOf(sampleJournal)
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
