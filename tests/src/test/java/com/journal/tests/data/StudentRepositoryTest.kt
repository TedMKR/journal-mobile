package com.journal.tests.data

import app.cash.turbine.test
import com.journal.core.data.repository.StudentRepository
import com.journal.core.data.util.Resource
import com.journal.core.database.dao.StudentLessonDao
import com.journal.core.database.entity.StudentLessonEntity
import com.journal.core.model.teacher.StudentLesson
import com.journal.core.model.teacher.StudentLessonsResponse
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
 * Unit tests for [StudentRepository].
 *
 * Mirrors [TeacherRepositoryTest] with student-specific entities.
 */
class StudentRepositoryTest {

    private lateinit var api: JournalApi
    private lateinit var dao: StudentLessonDao
    private lateinit var repository: StudentRepository

    // ─── Sample data ──────────────────────────────────────────────────────────

    private val sampleEntity = StudentLessonEntity(
        id = "sl-1",
        disciplineId = "d1",
        disciplineName = "Физика",
        groupId = "g1",
        groupName = "ИС-21",
        scheduledAt = "2026-05-03T10:00:00",
        lessonType = "lecture"
    )

    private val sampleLesson = StudentLesson(
        id = "sl-1",
        disciplineId = "d1",
        disciplineName = "Физика",
        groupId = "g1",
        groupName = "ИС-21",
        scheduledAt = "2026-05-03T10:00:00",
        lessonType = "lecture"
    )

    private val sampleResponse = StudentLessonsResponse(lessons = listOf(sampleLesson))

    @Before
    fun setUp() {
        api = mockk()
        dao = mockk(relaxed = true)
        repository = StudentRepository(api, dao)
    }

    // ─── Cache hit ────────────────────────────────────────────────────────────

    @Test
    fun `getLessons emits Success from cache when cache is fresh`() = runTest {
        every { dao.observeAll() } returns flowOf(listOf(sampleEntity))
        coEvery { dao.lastCachedAt() } returns System.currentTimeMillis()

        repository.getLessons(cacheMaxAgeMs = 5 * 60 * 1000L).test {
            awaitItem() // Loading
            val success = awaitItem()
            assertTrue(success is Resource.Success)
            assertEquals(1, (success as Resource.Success).data.size)
            assertEquals("sl-1", success.data.first().id)
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

        coVerify(exactly = 0) { api.getStudentLessons(any(), any(), any()) }
    }

    // ─── Cache miss / stale ────────────────────────────────��──────────────────

    @Test
    fun `getLessons fetches from API when cache is empty`() = runTest {
        every { dao.observeAll() } returns flowOf(emptyList())
        coEvery { api.getStudentLessons(any(), any(), any()) } returns sampleResponse

        repository.getLessons().test {
            awaitItem() // Loading
            val success = awaitItem()
            assertTrue(success is Resource.Success)
            awaitComplete()
        }

        coVerify { api.getStudentLessons(dateFrom = null, dateTo = null, limit = 200) }
    }

    @Test
    fun `getLessons saves fetched lessons to DAO`() = runTest {
        every { dao.observeAll() } returns flowOf(emptyList())
        coEvery { api.getStudentLessons(any(), any(), any()) } returns sampleResponse

        repository.getLessons().test { cancelAndIgnoreRemainingEvents() }

        coVerify { dao.deleteAll() }
        coVerify { dao.insertAll(any()) }
    }

    @Test
    fun `getLessons emits Error with cached data when API throws`() = runTest {
        every { dao.observeAll() } returns flowOf(listOf(sampleEntity))
        coEvery { dao.lastCachedAt() } returns 0L // stale
        coEvery { api.getStudentLessons(any(), any(), any()) } throws RuntimeException("offline")

        repository.getLessons(cacheMaxAgeMs = 1L).test {
            awaitItem() // Loading
            val error = awaitItem()
            assertTrue(error is Resource.Error)
            assertEquals(1, (error as Resource.Error).data?.size)
            awaitComplete()
        }
    }

    // ─── Date range ───────────────────────────────────────────────────────────

    @Test
    fun `getLessons uses date-range DAO query when dates provided`() = runTest {
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
        coEvery { api.getStudentLessons(any(), any(), any()) } returns StudentLessonsResponse()

        repository.getLessons(dateFrom = "2026-05-01", dateTo = "2026-05-07").test {
            cancelAndIgnoreRemainingEvents()
        }

        coVerify {
            api.getStudentLessons(
                dateFrom = "2026-05-01",
                dateTo = "2026-05-07",
                limit = 200
            )
        }
    }
}
