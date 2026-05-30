package com.journal.tests.data

import app.cash.turbine.test
import com.journal.core.data.repository.JournalRepository
import com.journal.core.data.util.Resource
import com.journal.core.database.dao.JournalGridCacheDao
import com.journal.core.database.dao.PendingActionDao
import com.journal.core.database.entity.JournalGridCacheEntity
import com.journal.core.database.entity.PendingActionEntity
import com.journal.core.database.entity.PendingActionType
import com.journal.core.model.teacher.CreateGradeRequest
import com.journal.core.model.teacher.JournalGridAcademicPeriod
import com.journal.core.model.teacher.JournalGridMeta
import com.journal.core.model.teacher.JournalGridPermissions
import com.journal.core.model.teacher.JournalGridRef
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.JournalGridTeacher
import com.journal.core.model.teacher.MarkAttendanceRequest
import com.journal.core.model.teacher.UpdateGradeRequest
import com.journal.core.network.api.JournalApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [JournalRepository].
 *
 * All external dependencies are mocked with MockK.
 * The offline-first write logic is the primary focus.
 */
class JournalRepositoryTest {

    private lateinit var api: JournalApi
    private lateinit var journalGridCacheDao: JournalGridCacheDao
    private lateinit var pendingActionDao: PendingActionDao
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private lateinit var repository: JournalRepository

    // ─── Shared params ────────────────────────────────────────────────────────
    private val groupId = "g1"
    private val disciplineId = "d1"
    private val periodId = "p1"
    private val lessonType = "lecture"

    /** A proper [JournalGridResponse] used when stubbing the API. */
    private val sampleGridResponse = JournalGridResponse(
        group = JournalGridRef(id = "g1", name = "G1"),
        discipline = JournalGridRef(id = "d1", name = "D1"),
        academicPeriod = JournalGridAcademicPeriod(
            id = "p1", name = "P1",
            startsAt = "2026-01-01", endsAt = "2026-06-30", isClosed = false
        ),
        teacher = JournalGridTeacher(id = "t1", fullName = "Teacher"),
        students = emptyList(),
        lessons = emptyList(),
        attendance = emptyList(),
        grades = emptyList(),
        assessmentForms = emptyList(),
        permissions = JournalGridPermissions(
            canEditAttendance = true,
            canEditGrades = true,
            canViewPrivateComments = false
        ),
        meta = JournalGridMeta(generatedAt = "2026-01-01T00:00:00Z", version = "1")
    )

    /**
     * Minimal valid JSON for [JournalGridResponse].
     * Required to make [JournalGridCacheEntity.toResponse()] succeed.
     */
    private val validGridJson = """
        {
          "group":{"id":"g1","name":"G1"},
          "discipline":{"id":"d1","name":"D1"},
          "academic_period":{"id":"p1","name":"P1","starts_at":"2026-01-01","ends_at":"2026-06-30","is_closed":false},
          "teacher":{"id":"t1","full_name":"Teacher"},
          "students":[],"lessons":[],"attendance":[],"grades":[],"assessment_forms":[],
          "permissions":{"can_edit_attendance":true,"can_edit_grades":true,"can_view_private_comments":false},
          "meta":{"generated_at":"2026-01-01T00:00:00Z","version":"1"}
        }
    """.trimIndent()

    @Before
    fun setUp() {
        api = mockk(relaxed = true)
        journalGridCacheDao = mockk(relaxed = true)
        pendingActionDao = mockk(relaxed = true)
        repository = JournalRepository(api, journalGridCacheDao, pendingActionDao, json)
    }

    // ─── getJournalGrid ───────────────────────────────────────────────────────

    @Test
    fun `getJournalGrid emits Loading when cache is null`() = runTest {
        every {
            journalGridCacheDao.observe(groupId, disciplineId, periodId, lessonType)
        } returns flowOf(null)

        repository.getJournalGrid(groupId, disciplineId, periodId, lessonType).test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getJournalGrid fetches from API when cache is absent`() = runTest {
        every {
            journalGridCacheDao.observe(groupId, disciplineId, periodId, lessonType)
        } returns flowOf(null)
        coEvery {
            journalGridCacheDao.get(groupId, disciplineId, periodId, lessonType)
        } returns null
        // Explicit stub so json.encodeToString(response) works in saveFetchResult
        coEvery {
            api.getGroupJournalGrid(any(), any(), any(), any(), any(), any(), any(), any())
        } returns sampleGridResponse

        repository.getJournalGrid(groupId, disciplineId, periodId, lessonType).test {
            cancelAndIgnoreRemainingEvents()
        }

        coVerify {
            api.getGroupJournalGrid(
                groupId = groupId,
                disciplineId = disciplineId,
                academicPeriodId = periodId,
                teacherId = null,
                lessonType = lessonType
            )
        }
    }

    @Test
    fun `getJournalGrid does not fetch when cache is fresh`() = runTest {
        val freshEntity = JournalGridCacheEntity(
            groupId = groupId,
            disciplineId = disciplineId,
            periodId = periodId,
            lessonType = lessonType,
            jsonData = validGridJson,          // valid JSON so toResponse() succeeds
            cachedAt = System.currentTimeMillis()  // fresh
        )

        every {
            journalGridCacheDao.observe(groupId, disciplineId, periodId, lessonType)
        } returns flowOf(freshEntity)
        coEvery {
            journalGridCacheDao.get(groupId, disciplineId, periodId, lessonType)
        } returns freshEntity

        repository.getJournalGrid(
            groupId, disciplineId, periodId, lessonType,
            cacheMaxAgeMs = 5 * 60 * 1000L
        ).test { cancelAndIgnoreRemainingEvents() }

        coVerify(exactly = 0) {
            api.getGroupJournalGrid(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    // ─── markAttendance (online) ──────────────────────────────────────────────

    @Test
    fun `markAttendance calls API and invalidates cache when online`() = runTest {
        val request = MarkAttendanceRequest(studentId = "s1", status = "present", comment = null)
        coEvery { api.markAttendance(any(), any()) } returns Unit

        repository.markAttendance("lesson-1", request, groupId, disciplineId, periodId, lessonType)

        coVerify { api.markAttendance("lesson-1", request) }
        coVerify { journalGridCacheDao.delete(groupId, disciplineId, periodId, lessonType) }
    }

    @Test
    fun `markAttendance does NOT enqueue pending action when online`() = runTest {
        val request = MarkAttendanceRequest(studentId = "s1", status = "present", comment = null)
        coEvery { api.markAttendance(any(), any()) } returns Unit

        repository.markAttendance("lesson-1", request, groupId, disciplineId, periodId, lessonType)

        coVerify(exactly = 0) { pendingActionDao.insert(any()) }
    }

    // ─── markAttendance (offline) ─────────────────────────────────────────────

    @Test
    fun `markAttendance enqueues pending action when API throws`() = runTest {
        val request = MarkAttendanceRequest(studentId = "s1", status = "absent", comment = null)
        coEvery { api.markAttendance(any(), any()) } throws RuntimeException("offline")

        val slot = slot<PendingActionEntity>()
        coEvery { pendingActionDao.insert(capture(slot)) } returns 1L

        repository.markAttendance("lesson-1", request, groupId, disciplineId, periodId, lessonType)

        with(slot.captured) {
            assertEquals(PendingActionType.MARK_ATTENDANCE, actionType)
            assertEquals("lesson-1", entityId)
            assertEquals(groupId, this.groupId)
            assertEquals(disciplineId, this.disciplineId)
            assertEquals(periodId, this.periodId)
        }
    }

    @Test
    fun `markAttendance does NOT delete cache when API throws`() = runTest {
        val request = MarkAttendanceRequest(studentId = "s1", status = "present", comment = null)
        coEvery { api.markAttendance(any(), any()) } throws RuntimeException("offline")
        coEvery { pendingActionDao.insert(any()) } returns 1L

        repository.markAttendance("lesson-1", request, groupId, disciplineId, periodId, lessonType)

        coVerify(exactly = 0) { journalGridCacheDao.delete(any(), any(), any(), any()) }
    }

    // ─── createGrade ─────────────────────────────────────────────────────────

    @Test
    fun `createGrade calls API and invalidates cache when online`() = runTest {
        val request = CreateGradeRequest(
            studentId = "s1",
            assessmentFormId = "af1",
            value = 5,
            comment = null
        )
        coEvery { api.createGrade(any()) } returns Unit

        repository.createGrade(request, groupId, disciplineId, periodId, lessonType)

        coVerify { api.createGrade(request) }
        coVerify { journalGridCacheDao.delete(groupId, disciplineId, periodId, lessonType) }
    }

    @Test
    fun `createGrade enqueues pending action when API throws`() = runTest {
        val request = CreateGradeRequest(
            studentId = "s1",
            assessmentFormId = "af1",
            value = 4,
            comment = null
        )
        coEvery { api.createGrade(any()) } throws RuntimeException("offline")

        val slot = slot<PendingActionEntity>()
        coEvery { pendingActionDao.insert(capture(slot)) } returns 1L

        repository.createGrade(request, groupId, disciplineId, periodId, lessonType)

        assertEquals(PendingActionType.CREATE_GRADE, slot.captured.actionType)
    }

    // ─── updateGrade ─────────────────────────────────────────────────────────

    @Test
    fun `updateGrade calls API with correct gradeId`() = runTest {
        val request = UpdateGradeRequest(value = 3, comment = null)
        coEvery { api.updateGrade(any(), any()) } returns Unit

        repository.updateGrade("grade-42", request, groupId, disciplineId, periodId, lessonType)

        coVerify { api.updateGrade("grade-42", request) }
    }

    @Test
    fun `updateGrade enqueues pending action with correct entityId when offline`() = runTest {
        val request = UpdateGradeRequest(value = 3, comment = null)
        coEvery { api.updateGrade(any(), any()) } throws RuntimeException("offline")

        val slot = slot<PendingActionEntity>()
        coEvery { pendingActionDao.insert(capture(slot)) } returns 1L

        repository.updateGrade("grade-42", request, groupId, disciplineId, periodId, lessonType)

        assertEquals(PendingActionType.UPDATE_GRADE, slot.captured.actionType)
        assertEquals("grade-42", slot.captured.entityId)
    }

    // ─── invalidateJournalCache ───────────────────────────────────────────────

    @Test
    fun `invalidateJournalCache deletes cache for given params`() = runTest {
        repository.invalidateJournalCache(groupId, disciplineId, periodId, lessonType)

        coVerify { journalGridCacheDao.delete(groupId, disciplineId, periodId, lessonType) }
    }

    // ─── observePendingCount ─────────────────────────────────────────────────

    @Test
    fun `observePendingCount delegates to pendingActionDao`() = runTest {
        every { pendingActionDao.observePendingCount() } returns flowOf(3)

        repository.observePendingCount().test {
            assertEquals(3, awaitItem())
            awaitComplete()
        }
    }
}
