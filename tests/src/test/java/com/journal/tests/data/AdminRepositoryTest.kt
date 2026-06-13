package com.journal.tests.data

import app.cash.turbine.test
import com.journal.core.data.repository.AdminDashboardData
import com.journal.core.data.repository.AdminRepository
import com.journal.core.data.repository.AdminUsersData
import com.journal.core.data.util.Resource
import com.journal.core.database.dao.DashboardCacheDao
import com.journal.core.database.entity.DashboardCacheEntity
import com.journal.core.model.teacher.AdminDocumentTask
import com.journal.core.model.teacher.AdminDocumentsResponse
import com.journal.core.model.teacher.AdminJournalContext
import com.journal.core.model.teacher.AdminJournalsResponse
import com.journal.core.model.teacher.AdminPageMeta
import com.journal.core.model.teacher.AdminPeriod
import com.journal.core.model.teacher.AdminPeriodsResponse
import com.journal.core.model.teacher.AdminUser
import com.journal.core.model.teacher.AdminUsersResponse
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
class AdminRepositoryTest {

    private lateinit var api: JournalApi
    private lateinit var dao: FakeDashboardCacheDao
    private lateinit var repository: AdminRepository

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        api = mockk()
        dao = FakeDashboardCacheDao()
        repository = AdminRepository(api, dao, json)
    }

    @Test
    fun `getDashboard saves remote snapshot to cache`() = runTest {
        stubDashboardApi()

        repository.getDashboard(userId = "admin-1").test {
            assertTrue(awaitItem() is Resource.Loading)

            val success = awaitItem()
            assertTrue(success is Resource.Success)
            val data = (success as Resource.Success).data
            assertEquals(12, data?.journalsCount)
            assertEquals(8, data?.usersCount)
            assertEquals(2, data?.periodsCount)
            assertEquals(5, data?.documentsCount)

            cancelAndIgnoreRemainingEvents()
        }

        val cached = dao.get(AdminRepository.dashboardCacheKey("admin-1"))
        assertEquals("admin_dashboard|admin-1", cached?.key)
    }

    @Test
    fun `getDashboard does not call API when cache is fresh`() = runTest {
        dao.upsert(cachedEntity(cachedAt = System.currentTimeMillis()))

        repository.getDashboard(userId = "admin-1").test {
            assertTrue(awaitItem() is Resource.Loading)
            assertTrue(awaitItem() is Resource.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { api.getAdminJournals(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `getDashboard emits Error with cached data when refresh fails`() = runTest {
        dao.upsert(cachedEntity(cachedAt = 0L))
        coEvery { api.getAdminJournals(any(), any(), any(), any(), any(), any(), any()) } throws RuntimeException("network down")

        repository.getDashboard(userId = "admin-1", cacheMaxAgeMs = 1L).test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)
            assertEquals(4, (loading as Resource.Loading).data?.journalsCount)

            val error = awaitItem()
            assertTrue(error is Resource.Error)
            assertEquals(4, (error as Resource.Error).data?.journalsCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getUsers saves filtered snapshot to cache`() = runTest {
        coEvery {
            api.getAdminUsers(any(), any(), any(), any(), any())
        } returns AdminUsersResponse(
            data = listOf(AdminUser(id = "user-1", fullName = "Ada Lovelace")),
            meta = AdminPageMeta(total = 1, page = 2, pageSize = 20)
        )

        repository.getUsers(page = 2, query = "ada", userType = "teacher", status = "active").test {
            assertTrue(awaitItem() is Resource.Loading)

            val success = awaitItem()
            assertTrue(success is Resource.Success)
            val data = (success as Resource.Success).data
            assertEquals(1, data?.users?.size)
            assertEquals(1, data?.total)
            assertEquals(2, data?.page)

            cancelAndIgnoreRemainingEvents()
        }

        val cached = dao.get(AdminRepository.usersCacheKey(2, 20, "ada", "teacher", "active"))
        assertEquals("admin_users|2|20|ada|teacher|active", cached?.key)
    }

    @Test
    fun `getUsers emits Error with cached data when refresh fails`() = runTest {
        dao.upsert(cachedUsersEntity(cachedAt = 0L))
        coEvery { api.getAdminUsers(any(), any(), any(), any(), any()) } throws RuntimeException("network down")

        repository.getUsers(page = 1, cacheMaxAgeMs = 1L).test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)
            assertEquals(1, (loading as Resource.Loading).data?.users?.size)

            val error = awaitItem()
            assertTrue(error is Resource.Error)
            assertEquals(1, (error as Resource.Error).data?.users?.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun stubDashboardApi() {
        coEvery {
            api.getAdminJournals(any(), any(), any(), any(), any(), any(), any())
        } returns AdminJournalsResponse(
            data = listOf(AdminJournalContext(id = "journal-1")),
            meta = AdminPageMeta(total = 12)
        )
        coEvery {
            api.getAdminUsers(any(), any(), any(), any(), any())
        } returns AdminUsersResponse(
            data = listOf(AdminUser(id = "user-1")),
            meta = AdminPageMeta(total = 8)
        )
        coEvery {
            api.getAdminPeriods(any())
        } returns AdminPeriodsResponse(
            data = listOf(AdminPeriod(id = "period-1"), AdminPeriod(id = "period-2"))
        )
        coEvery {
            api.getAdminDocuments(any(), any())
        } returns AdminDocumentsResponse(
            data = listOf(AdminDocumentTask(id = "document-1")),
            meta = AdminPageMeta(total = 5)
        )
    }

    private fun cachedEntity(cachedAt: Long): DashboardCacheEntity =
        DashboardCacheEntity(
            key = AdminRepository.dashboardCacheKey("admin-1"),
            jsonData = json.encodeToString(
                AdminDashboardData(
                    journalsCount = 4,
                    usersCount = 3,
                    periodsCount = 2,
                    documentsCount = 1
                )
            ),
            cachedAt = cachedAt
        )

    private fun cachedUsersEntity(cachedAt: Long): DashboardCacheEntity =
        DashboardCacheEntity(
            key = AdminRepository.usersCacheKey(1, 20, null, null, null),
            jsonData = json.encodeToString(
                AdminUsersData(
                    users = listOf(AdminUser(id = "user-1", fullName = "Cached User")),
                    total = 1,
                    page = 1,
                    pageSize = 20
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
