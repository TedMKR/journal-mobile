package com.journal.tests.data

import app.cash.turbine.test
import com.journal.core.data.notification.StudentGradeChangeDetector
import com.journal.core.data.repository.StudentRepository
import com.journal.core.data.util.Resource
import com.journal.core.database.dao.StudentLessonDao
import com.journal.core.database.dao.StudentProfileCacheDao
import com.journal.core.database.dao.StudentSubjectCardCacheDao
import com.journal.core.database.entity.StudentLessonEntity
import com.journal.core.database.entity.StudentProfileCacheEntity
import com.journal.core.database.entity.StudentSubjectCardCacheEntity
import com.journal.core.model.teacher.StudentLesson
import com.journal.core.model.teacher.StudentLessonsResponse
import com.journal.core.model.teacher.StudentProfile
import com.journal.core.model.teacher.StudentSubjectCard
import com.journal.core.model.teacher.StudentSubjectSummary
import com.journal.core.model.teacher.StudentSubjectsResponse
import com.journal.core.network.api.JournalApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalSerializationApi::class)
class StudentRepositoryTest {

    private lateinit var api: JournalApi
    private lateinit var dao: StudentLessonDao
    private lateinit var subjectCardCacheDao: StudentSubjectCardCacheDao
    private lateinit var profileCacheDao: StudentProfileCacheDao
    private lateinit var gradeChangeDetector: StudentGradeChangeDetector
    private lateinit var repository: StudentRepository

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

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
        subjectCardCacheDao = mockk(relaxed = true)
        profileCacheDao = mockk(relaxed = true)
        gradeChangeDetector = mockk(relaxed = true)
        repository = StudentRepository(
            api = api,
            studentLessonDao = dao,
            subjectCardCacheDao = subjectCardCacheDao,
            profileCacheDao = profileCacheDao,
            gradeChangeDetector = gradeChangeDetector,
            json = json
        )
    }

    @Test
    fun `getLessons emits Success from cache when cache is fresh`() = runTest {
        every { dao.observeAll() } returns flowOf(listOf(sampleEntity))
        coEvery { dao.lastCachedAt() } returns System.currentTimeMillis()

        repository.getLessons(cacheMaxAgeMs = 5 * 60 * 1000L).test {
            awaitItem()
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

    @Test
    fun `getLessons fetches from API when cache is empty`() = runTest {
        every { dao.observeAll() } returns flowOf(emptyList())
        coEvery { api.getStudentLessons(any(), any(), any()) } returns sampleResponse

        repository.getLessons().test {
            awaitItem()
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
        coEvery { dao.lastCachedAt() } returns 0L
        coEvery { api.getStudentLessons(any(), any(), any()) } throws RuntimeException("offline")

        repository.getLessons(cacheMaxAgeMs = 1L).test {
            awaitItem()
            val error = awaitItem()
            assertTrue(error is Resource.Error)
            assertEquals(1, (error as Resource.Error).data?.size)
            awaitComplete()
        }
    }

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

    @Test
    fun `getSubjectCard emits cached data when refresh fails`() = runTest {
        val disciplineId = "discipline-1"
        val periodId = "period-1"
        val groupId = "group-1"
        val key = "$disciplineId|$periodId|$groupId"
        val card = sampleSubjectCard(disciplineId, periodId, groupId)
        val entity = StudentSubjectCardCacheEntity(
            key = key,
            disciplineId = disciplineId,
            periodId = periodId,
            groupId = groupId,
            jsonData = json.encodeToString(card),
            cachedAt = 0L
        )

        every { subjectCardCacheDao.observe(key) } returns flowOf(entity)
        coEvery { subjectCardCacheDao.get(key) } returns entity
        coEvery { api.getStudentSubjectCard(disciplineId, periodId, groupId) } throws IOException("offline")

        repository.getSubjectCard(
            disciplineId = disciplineId,
            periodId = periodId,
            groupId = groupId,
            cacheMaxAgeMs = 0L
        ).test {
            val loading = awaitItem() as Resource.Loading<StudentSubjectCard?>
            assertEquals(card, loading.data)

            val error = awaitItem() as Resource.Error<StudentSubjectCard?>
            assertEquals(card, error.data)
            assertTrue(error.throwable is IOException)

            awaitComplete()
        }
    }

    @Test
    fun `getDashboard saves remote profile and subjects to cache`() = runTest {
        val profile = StudentProfile(
            id = "student-1",
            fullName = "Student One",
            groupId = "group-1",
            groupName = "Group 1"
        )
        val subjects = listOf(
            StudentSubjectSummary(
                disciplineId = "discipline-1",
                disciplineName = "Math",
                groupId = "group-1",
                periodId = "period-1",
                lessonsAttended = 3,
                lessonsTotal = 4,
                attendancePct = 75.0,
                avgGrade = 4.5
            )
        )

        every { profileCacheDao.observe(StudentProfileCacheEntity.CURRENT_PROFILE_KEY) } returns flowOf(null)
        coEvery { profileCacheDao.get(StudentProfileCacheEntity.CURRENT_PROFILE_KEY) } returns null
        coEvery { api.getStudentProfile() } returns profile
        coEvery { api.getStudentSubjects() } returns StudentSubjectsResponse(subjects)

        val slot = slot<StudentProfileCacheEntity>()
        coEvery { profileCacheDao.upsert(capture(slot)) } returns Unit

        repository.getDashboard().test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }

        coVerify { profileCacheDao.upsert(any()) }
        assertTrue(slot.captured.profileJson.contains("student-1"))
        assertTrue(slot.captured.subjectsJson.contains("discipline-1"))
    }

    private fun sampleSubjectCard(
        disciplineId: String,
        periodId: String,
        groupId: String
    ) = StudentSubjectCard(
        disciplineId = disciplineId,
        disciplineName = "Math",
        groupId = groupId,
        groupName = "Group 1",
        periodId = periodId,
        lessonsAttended = 3,
        lessonsTotal = 4
    )
}
