package com.journal.tests.features

import com.journal.core.data.repository.TeacherDashboardJournalData
import com.journal.core.data.repository.TeacherDashboardRepository
import com.journal.core.data.repository.TeacherDashboardTeachersData
import com.journal.core.data.util.Resource
import com.journal.core.model.teacher.JournalGridAcademicPeriod
import com.journal.core.model.teacher.JournalGridMeta
import com.journal.core.model.teacher.JournalGridPermissions
import com.journal.core.model.teacher.JournalGridRef
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.JournalGridTeacher
import com.journal.core.model.teacher.TeacherProfile
import com.journal.features.teacher.dashboard.TeacherDashboardViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeacherDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TeacherDashboardRepository

    private val sampleJournal = JournalGridResponse(
        group = JournalGridRef("group-1", "IS-21"),
        discipline = JournalGridRef("discipline-1", "Math"),
        academicPeriod = JournalGridAcademicPeriod("period-1", "Spring", "2026-01-01", "2026-06-30", false),
        teacher = JournalGridTeacher("teacher-1", "Ada Lovelace"),
        students = emptyList(),
        lessons = emptyList(),
        attendance = emptyList(),
        grades = emptyList(),
        assessmentForms = emptyList(),
        permissions = JournalGridPermissions(true, true, true),
        meta = JournalGridMeta("2026-01-01T00:00:00Z", "1")
    )
    private val sampleTeacher = TeacherProfile(
        id = "teacher-2",
        fullName = "Grace Hopper",
        keycloakId = "keycloak-2"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selected journal success updates state`() = runTest {
        every {
            repository.getSelectedJournal(any(), any(), any(), any())
        } returns flowOf(Resource.Success(TeacherDashboardJournalData(sampleJournal)))

        val vm = TeacherDashboardViewModel(repository)
        vm.loadSelectedJournal("group-1", "discipline-1", "period-1")
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.selectedJournalState.value) {
            assertFalse(isLoading)
            assertEquals(sampleJournal, journal)
            assertNull(error)
            assertFalse(isOffline)
        }
    }

    @Test
    fun `selected journal cached error marks state offline`() = runTest {
        every {
            repository.getSelectedJournal(any(), any(), any(), any())
        } returns flowOf(Resource.Error(RuntimeException("offline"), TeacherDashboardJournalData(sampleJournal)))

        val vm = TeacherDashboardViewModel(repository)
        vm.loadSelectedJournal("group-1", "discipline-1", "period-1")
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.selectedJournalState.value) {
            assertEquals(sampleJournal, journal)
            assertNull(error)
            assertTrue(isOffline)
        }
    }

    @Test
    fun `selected journal reload clears previous journal while loading`() = runTest {
        every {
            repository.getSelectedJournal("group-1", "discipline-1", "period-1", any())
        } returns flowOf(Resource.Success(TeacherDashboardJournalData(sampleJournal)))
        every {
            repository.getSelectedJournal("group-2", "discipline-1", "period-1", any())
        } returns flowOf(Resource.Loading(null))

        val vm = TeacherDashboardViewModel(repository)
        vm.loadSelectedJournal("group-1", "discipline-1", "period-1")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.loadSelectedJournal("group-2", "discipline-1", "period-1")
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.selectedJournalState.value) {
            assertTrue(isLoading)
            assertNull(journal)
            assertNull(error)
        }
    }

    @Test
    fun `teachers success updates state`() = runTest {
        every { repository.getAccessTeachers(any()) } returns
            flowOf(Resource.Success(TeacherDashboardTeachersData(listOf(sampleTeacher))))

        val vm = TeacherDashboardViewModel(repository)
        vm.loadAccessTeachers()
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.teachersState.value) {
            assertFalse(isLoading)
            assertEquals(listOf(sampleTeacher), teachers)
            assertNull(error)
            assertFalse(isOffline)
        }
    }

    @Test
    fun `teachers cached error marks state offline`() = runTest {
        every { repository.getAccessTeachers(any()) } returns
            flowOf(Resource.Error(RuntimeException("offline"), TeacherDashboardTeachersData(listOf(sampleTeacher))))

        val vm = TeacherDashboardViewModel(repository)
        vm.loadAccessTeachers()
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.teachersState.value) {
            assertEquals(listOf(sampleTeacher), teachers)
            assertNull(error)
            assertTrue(isOffline)
        }
    }
}
