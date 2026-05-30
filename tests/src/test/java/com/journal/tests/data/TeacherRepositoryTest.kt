package com.journal.tests.data

import app.cash.turbine.test
import com.journal.core.data.repository.TeacherRepository
import com.journal.core.data.util.Resource
import com.journal.core.database.dao.TeacherLessonDao
import com.journal.core.database.entity.TeacherLessonEntity
import com.journal.core.model.teacher.LessonsResponse
import com.journal.core.model.teacher.TeacherLesson
import com.journal.core.network.api.JournalApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TeacherRepository].
 *
 * [JournalApi] and [TeacherLessonDao] are mocked with MockK.
 *
 * Note: [JournalApi.getLessons] has 9 optional parameters; MockK requires matching
 * all of them. We use `any()` for all 9 in stub/verify calls.
 */
class TeacherRepositoryTest {

    private lateinit var api: JournalApi
    private lateinit var dao: TeacherLessonDao
    private lateinit var repository: TeacherRepository

    // ─── Sample data ──────────────────────────────────────────────────────────

    private val sampleEntity = TeacherLessonEntity(
        id = "lesson-1",
        disciplineName = "Математика",
        groupName = "ИС-21",
        lessonType = "lecture",
        scheduledAt = "2026-05-01T08:00:00"
    )

    private val sampleLesson = TeacherLesson(
        id = "lesson-1",
        disciplineName = "Математика",
        groupName = "ИС-21",
        lessonType = "lecture",
        scheduledAt = "2026-05-01T08:00:00"
    )

    private val sampleResponse = LessonsResponse(lessons = listOf(sampleLesson))

    @Before
    fun setUp() {
        api = mockk()
        dao = mockk(relaxed = true)
        repository = TeacherRepository(api, dao)
    }

    // ─── getLessons — cache hit (no fetch) ────────────────────────────────────

    @Test
    fun `getLessons emits Success from cache when cache is fresh`() = runTest {
        every { dao.observeAll() } returns flowOf(listOf(sampleEntity))
        coEvery { dao.lastCachedAt() } returns System.currentTimeMillis() // fresh

        repository.getLessons(cacheMaxAgeMs = 5 * 60 * 1000L).test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)

            val success = awaitItem()
            assertTrue(success is Resource.Success)
            assertEquals(1, (success as Resource.Success).data.size)
            assertEquals("lesson-1", success.data.first().id)

            awaitComplete()
        }
    }

    @Test
    fun `getLessons does not call API when cache is fresh`() = runTest {
        every { dao.observeAll() } returns flowOf(listOf(sampleEntity))
        coEvery { dao.lastCachedAt() } returns System.currentTimeMillis()

        repository.getLessons(cacheMaxAgeMs = 5 * 60 * 1000L).test {
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) {
            api.getLessons(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    // ─── getLessons — stale cache (fetch triggered) ───────────────────────────

    @Test
    fun `getLessons fetches from API when cache is empty`() = runTest {
        every { dao.observeAll() } returns flowOf(emptyList())
        coEvery {
            api.getLessons(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns sampleResponse

        repository.getLessons().test {
            awaitItem() // Loading
            val success = awaitItem()
            assertTrue(success is Resource.Success)
            awaitComplete()
        }

        coVerify {
            api.getLessons(
                dateFrom = null,
                dateTo = null,
                limit = 200
            )
        }
    }

    @Test
    fun `getLessons saves API result to DAO`() = runTest {
        every { dao.observeAll() } returns flowOf(emptyList())
        coEvery {
            api.getLessons(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns sampleResponse

        repository.getLessons().test { cancelAndIgnoreRemainingEvents() }

        coVerify { dao.deleteAll() }
        coVerify { dao.insertAll(any()) }
    }

    @Test
    fun `getLessons emits Error with cached data when API throws`() = runTest {
        every { dao.observeAll() } returns flowOf(listOf(sampleEntity))
        coEvery { dao.lastCachedAt() } returns 0L // stale
        coEvery {
            api.getLessons(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } throws RuntimeException("no network")

        repository.getLessons(cacheMaxAgeMs = 1L).test {
            awaitItem() // Loading
            val error = awaitItem()
            assertTrue(error is Resource.Error)
            assertEquals(1, (error as Resource.Error).data?.size)
            awaitComplete()
        }
    }

    // ─── getLessons — date range ──────────────────────────────────────────────

    @Test
    fun `getLessons uses date-range DAO query when dates are provided`() = runTest {
        every {
            dao.observeByDateRange("2026-05-01", "2026-05-07")
        } returns flowOf(listOf(sampleEntity))
        coEvery { dao.lastCachedAt() } returns System.currentTimeMillis()

        repository.getLessons(dateFrom = "2026-05-01", dateTo = "2026-05-07").test {
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { dao.observeByDateRange("2026-05-01", "2026-05-07") }
    }

    @Test
    fun `getLessons passes date range to API on fetch`() = runTest {
        every { dao.observeByDateRange(any(), any()) } returns flowOf(emptyList())
        coEvery {
            api.getLessons(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns LessonsResponse(emptyList())

        repository.getLessons(dateFrom = "2026-05-01", dateTo = "2026-05-07").test {
            cancelAndIgnoreRemainingEvents()
        }

        coVerify {
            api.getLessons(
                dateFrom = "2026-05-01",
                dateTo = "2026-05-07",
                limit = 200
            )
        }
    }
}
