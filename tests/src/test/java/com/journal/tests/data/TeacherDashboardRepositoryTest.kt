package com.journal.tests.data

import app.cash.turbine.test
import com.journal.core.data.repository.TeacherDashboardData
import com.journal.core.data.repository.TeacherDashboardJournalData
import com.journal.core.data.repository.TeacherDashboardRepository
import com.journal.core.data.repository.TeacherDashboardTeachersData
import com.journal.core.data.util.Resource
import com.journal.core.database.dao.DashboardCacheDao
import com.journal.core.database.entity.DashboardCacheEntity
import com.journal.core.model.teacher.AcademicPeriod
import com.journal.core.model.teacher.AcademicPeriodsResponse
import com.journal.core.model.teacher.CatalogResponse
import com.journal.core.model.teacher.JournalGridAcademicPeriod
import com.journal.core.model.teacher.JournalGridMeta
import com.journal.core.model.teacher.JournalGridPermissions
import com.journal.core.model.teacher.JournalGridRef
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.JournalGridTeacher
import com.journal.core.model.teacher.LessonsResponse
import com.journal.core.model.teacher.TeacherLesson
import com.journal.core.model.teacher.TeacherProfile
import com.journal.core.model.teacher.TeacherStats
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
class TeacherDashboardRepositoryTest {

    private lateinit var api: JournalApi
    private lateinit var dao: FakeDashboardCacheDao
    private lateinit var repository: TeacherDashboardRepository

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val sampleLesson = TeacherLesson(
        id = "lesson-1",
        disciplineName = "Math",
        groupName = "IS-21",
        lessonType = "lecture",
        scheduledAt = "2026-05-01T08:00:00Z"
    )

    private val sampleStats = TeacherStats(
        totalStudents = 12,
        totalDisciplines = 2,
        hoursThisWeek = 6f,
        avgGrade = 4.5f
    )

    private val sampleJournal = JournalGridResponse(
        group = JournalGridRef("group-1", "IS-21"),
        discipline = JournalGridRef("discipline-1", "Math"),
        academicPeriod = JournalGridAcademicPeriod(
            id = "period-1",
            name = "Spring",
            startsAt = "2026-01-01",
            endsAt = "2026-06-30",
            isClosed = false
        ),
        teacher = JournalGridTeacher("teacher-1", "Ada Lovelace"),
        students = emptyList(),
        lessons = emptyList(),
        attendance = emptyList(),
        grades = emptyList(),
        assessmentForms = emptyList(),
        permissions = JournalGridPermissions(
            canEditAttendance = true,
            canEditGrades = true,
            canViewPrivateComments = true
        ),
        meta = JournalGridMeta(generatedAt = "2026-01-01T00:00:00Z", version = "1")
    )

    private val sampleTeacher = TeacherProfile(
        id = "teacher-2",
        fullName = "Grace Hopper",
        keycloakId = "keycloak-2"
    )

    @Before
    fun setUp() {
        api = mockk()
        dao = FakeDashboardCacheDao()
        repository = TeacherDashboardRepository(api, dao, json)
    }

    @Test
    fun `getDashboard saves remote snapshot to cache`() = runTest {
        stubDashboardApi()

        repository.getDashboard(userId = "teacher-1").test {
            assertTrue(awaitItem() is Resource.Loading)

            val success = awaitItem()
            assertTrue(success is Resource.Success)
            val data = (success as Resource.Success).data
            assertEquals(listOf(sampleLesson), data?.lessons)
            assertEquals(sampleStats, data?.stats)
            assertEquals("period-1", data?.activePeriodId)

            cancelAndIgnoreRemainingEvents()
        }

        val cached = dao.get(TeacherDashboardRepository.cacheKey("teacher-1"))
        assertEquals("teacher_dashboard|teacher-1", cached?.key)
    }

    @Test
    fun `getDashboard does not call API when cache is fresh`() = runTest {
        dao.upsert(cachedEntity(cachedAt = System.currentTimeMillis()))

        repository.getDashboard(userId = "teacher-1").test {
            assertTrue(awaitItem() is Resource.Loading)
            assertTrue(awaitItem() is Resource.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) {
            api.getLessons(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `getDashboard emits Error with cached data when refresh fails`() = runTest {
        dao.upsert(cachedEntity(cachedAt = 0L))
        coEvery {
            api.getLessons(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } throws RuntimeException("network down")

        repository.getDashboard(userId = "teacher-1", cacheMaxAgeMs = 1L).test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)
            assertEquals(listOf(sampleLesson), (loading as Resource.Loading).data?.lessons)

            val error = awaitItem()
            assertTrue(error is Resource.Error)
            assertEquals(listOf(sampleLesson), (error as Resource.Error).data?.lessons)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getSelectedJournal saves remote journal to cache`() = runTest {
        coEvery {
            api.getGroupJournalGrid("group-1", "discipline-1", "period-1")
        } returns sampleJournal

        repository.getSelectedJournal("group-1", "discipline-1", "period-1").test {
            assertTrue(awaitItem() is Resource.Loading)

            val success = awaitItem()
            assertTrue(success is Resource.Success)
            assertEquals(sampleJournal, (success as Resource.Success).data?.journal)

            cancelAndIgnoreRemainingEvents()
        }

        val cached = dao.get(TeacherDashboardRepository.selectedJournalCacheKey("group-1", "discipline-1", "period-1"))
        assertEquals("teacher_dashboard_journal|group-1|discipline-1|period-1", cached?.key)
    }

    @Test
    fun `getSelectedJournal emits Error with cached data when refresh fails`() = runTest {
        dao.upsert(
            DashboardCacheEntity(
                key = TeacherDashboardRepository.selectedJournalCacheKey("group-1", "discipline-1", "period-1"),
                jsonData = json.encodeToString(TeacherDashboardJournalData(sampleJournal)),
                cachedAt = 0L
            )
        )
        coEvery {
            api.getGroupJournalGrid("group-1", "discipline-1", "period-1")
        } throws RuntimeException("network down")

        repository.getSelectedJournal("group-1", "discipline-1", "period-1", cacheMaxAgeMs = 1L).test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)
            assertEquals(sampleJournal, (loading as Resource.Loading).data?.journal)

            val error = awaitItem()
            assertTrue(error is Resource.Error)
            assertEquals(sampleJournal, (error as Resource.Error).data?.journal)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAccessTeachers filters and saves teachers to cache`() = runTest {
        coEvery { api.getTeachers(any(), any(), any(), any(), any()) } returns CatalogResponse(
            data = listOf(sampleTeacher, TeacherProfile(id = "teacher-3", fullName = "No Keycloak"))
        )

        repository.getAccessTeachers().test {
            assertTrue(awaitItem() is Resource.Loading)

            val success = awaitItem()
            assertTrue(success is Resource.Success)
            assertEquals(listOf(sampleTeacher), (success as Resource.Success).data?.teachers)

            cancelAndIgnoreRemainingEvents()
        }

        val cached = dao.get(TeacherDashboardRepository.teachersCacheKey())
        assertEquals("teacher_dashboard_teachers|all", cached?.key)
    }

    @Test
    fun `getAccessTeachers emits Error with cached data when refresh fails`() = runTest {
        dao.upsert(
            DashboardCacheEntity(
                key = TeacherDashboardRepository.teachersCacheKey(),
                jsonData = json.encodeToString(TeacherDashboardTeachersData(listOf(sampleTeacher))),
                cachedAt = 0L
            )
        )
        coEvery { api.getTeachers(any(), any(), any(), any(), any()) } throws RuntimeException("network down")

        repository.getAccessTeachers(cacheMaxAgeMs = 1L).test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)
            assertEquals(listOf(sampleTeacher), (loading as Resource.Loading).data?.teachers)

            val error = awaitItem()
            assertTrue(error is Resource.Error)
            assertEquals(listOf(sampleTeacher), (error as Resource.Error).data?.teachers)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun stubDashboardApi() {
        coEvery {
            api.getLessons(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns LessonsResponse(lessons = listOf(sampleLesson))
        coEvery { api.getTeacherStats() } returns sampleStats
        coEvery { api.getAcademicPeriods(any()) } returns AcademicPeriodsResponse(
            data = listOf(
                AcademicPeriod(
                    id = "period-1",
                    name = "Spring",
                    startsAt = "2026-01-01",
                    endsAt = "2026-06-30",
                    isClosed = false,
                    isActive = true
                )
            )
        )
    }

    private fun cachedEntity(cachedAt: Long): DashboardCacheEntity =
        DashboardCacheEntity(
            key = TeacherDashboardRepository.cacheKey("teacher-1"),
            jsonData = json.encodeToString(
                TeacherDashboardData(
                    lessons = listOf(sampleLesson),
                    stats = sampleStats,
                    activePeriodId = "period-1"
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
