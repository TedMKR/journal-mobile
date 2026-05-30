package com.journal.tests.features

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.journal.core.data.repository.JournalRepository
import com.journal.core.data.util.Resource
import com.journal.core.database.entity.PendingActionEntity
import com.journal.core.database.entity.PendingActionType
import com.journal.core.model.teacher.JournalGridAcademicPeriod
import com.journal.core.model.teacher.JournalGridMeta
import com.journal.core.model.teacher.JournalGridPermissions
import com.journal.core.model.teacher.JournalGridRef
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.JournalGridTeacher
import com.journal.features.teacher.journal.JournalUiState
import com.journal.features.teacher.journal.TeacherJournalViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TeacherJournalViewModel].
 *
 * [JournalRepository] is mocked. [SavedStateHandle] is constructed manually
 * with the required keys so Hilt is not needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TeacherJournalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: JournalRepository

    // ─── Fixed navigation params ───────────────────────────────────────────────
    private val groupId = "g1"
    private val disciplineId = "d1"
    private val periodId = "p1"
    private val lessonType = "lecture"

    private val savedStateHandle = SavedStateHandle(
        mapOf(
            "groupId" to groupId,
            "disciplineId" to disciplineId,
            "periodId" to periodId,
            "lessonType" to lessonType
        )
    )

    // ─── Sample data ──────────────────────────────────────────────────────────

    private val sampleJournal = JournalGridResponse(
        group = JournalGridRef(id = groupId, name = "ИС-21"),
        discipline = JournalGridRef(id = disciplineId, name = "Математика"),
        academicPeriod = JournalGridAcademicPeriod(
            id = periodId,
            name = "Семестр 1",
            startsAt = "2026-09-01",
            endsAt = "2027-01-31",
            isClosed = false
        ),
        teacher = JournalGridTeacher(id = "t1", fullName = "Иванов И.И."),
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
        meta = JournalGridMeta(generatedAt = "2026-05-01T00:00:00Z", version = "1")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        // Default: empty pending actions
        every {
            repository.observePendingForJournal(any(), any(), any())
        } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Navigation arguments ─────────────────────────────────────────────────

    @Test
    fun `ViewModel reads groupId from SavedStateHandle`() = runTest {
        every { repository.getJournalGrid(any(), any(), any(), any(), any(), any()) } returns
                flowOf(Resource.Loading(null))

        val vm = TeacherJournalViewModel(savedStateHandle, repository)
        assertEquals(groupId, vm.groupId)
    }

    @Test
    fun `ViewModel reads disciplineId from SavedStateHandle`() = runTest {
        every { repository.getJournalGrid(any(), any(), any(), any(), any(), any()) } returns
                flowOf(Resource.Loading(null))

        val vm = TeacherJournalViewModel(savedStateHandle, repository)
        assertEquals(disciplineId, vm.disciplineId)
    }

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun `initial uiState has isLoading = true`() = runTest {
        every { repository.getJournalGrid(any(), any(), any(), any(), any(), any()) } returns
                flowOf(Resource.Loading(null))

        val vm = TeacherJournalViewModel(savedStateHandle, repository)
        assertTrue(vm.uiState.value.isLoading)
    }

    // ─── Success state ────────────────────────────────────────────────────────

    @Test
    fun `uiState transitions to success with journal data`() = runTest {
        every {
            repository.getJournalGrid(any(), any(), any(), any(), any(), any())
        } returns flowOf(Resource.Success(sampleJournal))

        val vm = TeacherJournalViewModel(savedStateHandle, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertNotNull(journal)
            assertNull(error)
            assertFalse(isOffline)
        }
    }

    @Test
    fun `journal field matches the API response on success`() = runTest {
        every {
            repository.getJournalGrid(any(), any(), any(), any(), any(), any())
        } returns flowOf(Resource.Success(sampleJournal))

        val vm = TeacherJournalViewModel(savedStateHandle, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("ИС-21", vm.uiState.value.journal?.group?.name)
    }

    // ─── Error state ──────────────────────────────────────────────────────────

    @Test
    fun `uiState shows error when network fails with no cache`() = runTest {
        val exception = RuntimeException("connection refused")
        every {
            repository.getJournalGrid(any(), any(), any(), any(), any(), any())
        } returns flowOf(Resource.Error(exception, null))

        val vm = TeacherJournalViewModel(savedStateHandle, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertNull(journal)
            assertEquals("connection refused", error)
        }
    }

    @Test
    fun `uiState shows offline=true when network fails but cache exists`() = runTest {
        val exception = RuntimeException("offline")
        every {
            repository.getJournalGrid(any(), any(), any(), any(), any(), any())
        } returns flowOf(Resource.Error(exception, sampleJournal))

        val vm = TeacherJournalViewModel(savedStateHandle, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertNotNull(journal)
            assertNull(error)         // no error text when cached data is available
            assertTrue(isOffline)
        }
    }

    // ─── pendingCount ─────────────────────────────────────────────────────────

    @Test
    fun `pendingCount updates when observePendingForJournal emits`() = runTest {
        val pendingAction = PendingActionEntity(
            actionType = PendingActionType.MARK_ATTENDANCE,
            entityId = "l1",
            payloadJson = "{}",
            groupId = groupId,
            disciplineId = disciplineId,
            periodId = periodId
        )
        every {
            repository.observePendingForJournal(groupId, disciplineId, periodId)
        } returns flowOf(listOf(pendingAction))
        every {
            repository.getJournalGrid(any(), any(), any(), any(), any(), any())
        } returns flowOf(Resource.Success(sampleJournal))

        val vm = TeacherJournalViewModel(savedStateHandle, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.pendingCount)
    }

    @Test
    fun `pendingCount is zero when there are no pending actions`() = runTest {
        every {
            repository.observePendingForJournal(any(), any(), any())
        } returns flowOf(emptyList())
        every {
            repository.getJournalGrid(any(), any(), any(), any(), any(), any())
        } returns flowOf(Resource.Success(sampleJournal))

        val vm = TeacherJournalViewModel(savedStateHandle, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, vm.uiState.value.pendingCount)
    }

    // ─── markAttendance ───────────────────────────────────────────────────────

    @Test
    fun `markAttendance calls repository markAttendance with correct params`() = runTest {
        every {
            repository.getJournalGrid(any(), any(), any(), any(), any(), any())
        } returns flowOf(Resource.Success(sampleJournal))
        coEvery {
            repository.markAttendance(any(), any(), any(), any(), any(), any())
        } returns Unit

        val vm = TeacherJournalViewModel(savedStateHandle, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.markAttendance("lesson-1", "student-1", "present", null)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.markAttendance(
                lessonId = "lesson-1",
                request = any(),
                groupId = groupId,
                disciplineId = disciplineId,
                periodId = periodId,
                lessonType = lessonType
            )
        }
    }

    @Test
    fun `createGrade calls repository createGrade and reloads`() = runTest {
        var loadCallCount = 0
        every {
            repository.getJournalGrid(any(), any(), any(), any(), any(), any())
        } answers {
            loadCallCount++
            flowOf(Resource.Success(sampleJournal))
        }
        coEvery { repository.createGrade(any(), any(), any(), any(), any()) } returns Unit

        val vm = TeacherJournalViewModel(savedStateHandle, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val countBeforeAction = loadCallCount
        vm.createGrade("student-1", "af-1", 5, null)
        testDispatcher.scheduler.advanceUntilIdle()

        // loadJournal() called again after action
        assertTrue(loadCallCount > countBeforeAction)
    }

    // ─── deleteAssessmentForm ─────────────────────────────────────────────────

    @Test
    fun `deleteAssessmentForm calls repository with correct id`() = runTest {
        every {
            repository.getJournalGrid(any(), any(), any(), any(), any(), any())
        } returns flowOf(Resource.Success(sampleJournal))
        coEvery {
            repository.deleteAssessmentForm(any(), any(), any(), any(), any())
        } returns Unit

        val vm = TeacherJournalViewModel(savedStateHandle, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.deleteAssessmentForm("af-42")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.deleteAssessmentForm(
                assessmentFormId = "af-42",
                groupId = groupId,
                disciplineId = disciplineId,
                periodId = periodId,
                lessonType = lessonType
            )
        }
    }
}
