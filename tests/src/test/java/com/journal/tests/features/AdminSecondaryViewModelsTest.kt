package com.journal.tests.features

import com.journal.core.data.repository.AdminAccessData
import com.journal.core.data.repository.AdminAuditData
import com.journal.core.data.repository.AdminJournalsData
import com.journal.core.data.repository.AdminPeriodsData
import com.journal.core.data.repository.AdminProblemStudentsData
import com.journal.core.data.repository.AdminRepository
import com.journal.core.data.util.Resource
import com.journal.core.model.teacher.AdminAccessBinding
import com.journal.core.model.teacher.AdminJournalContext
import com.journal.core.model.teacher.AdminPeriod
import com.journal.core.model.teacher.AuditEvent
import com.journal.core.model.teacher.ProblemStudentEntry
import com.journal.core.model.teacher.ProblemStudentsMeta
import com.journal.features.admin.access.AdminAccessViewModel
import com.journal.features.admin.audit.AdminAuditViewModel
import com.journal.features.admin.journals.AdminJournalsViewModel
import com.journal.features.admin.periods.AdminPeriodsViewModel
import com.journal.features.admin.problemstudents.AdminProblemStudentsViewModel
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
class AdminSecondaryViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AdminRepository

    private val auditEvent = AuditEvent(id = "audit-1", action = "USER_BLOCKED")
    private val journal = AdminJournalContext(id = "journal-1", disciplineName = "Math")
    private val period = AdminPeriod(id = "period-1", name = "Spring")
    private val binding = AdminAccessBinding(id = "binding-1", accessLevel = "read")
    private val problemStudent = ProblemStudentEntry(studentId = "student-1", studentName = "Ada")

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
    fun `audit success updates events and total`() = runTest {
        val data = AdminAuditData(events = listOf(auditEvent), total = 1)
        every { repository.getAudit(any(), any(), any(), any(), any()) } returns flowOf(Resource.Success(data))

        val vm = AdminAuditViewModel(repository)
        vm.loadAudit(page = 1, pageSize = 20, action = "", entityType = "")
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertEquals(listOf(auditEvent), events)
            assertEquals(1, total)
            assertNull(error)
            assertFalse(isOffline)
        }
    }

    @Test
    fun `audit cached error marks state offline`() = runTest {
        val data = AdminAuditData(events = listOf(auditEvent), total = 1)
        every { repository.getAudit(any(), any(), any(), any(), any()) } returns
            flowOf(Resource.Error(RuntimeException("offline"), data))

        val vm = AdminAuditViewModel(repository)
        vm.loadAudit(page = 1, pageSize = 20, action = "", entityType = "")
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertEquals(listOf(auditEvent), events)
            assertNull(error)
            assertTrue(isOffline)
        }
    }

    @Test
    fun `journals success updates list and total`() = runTest {
        val data = AdminJournalsData(journals = listOf(journal), total = 1)
        every { repository.getJournals(any(), any(), any(), any()) } returns flowOf(Resource.Success(data))

        val vm = AdminJournalsViewModel(repository)
        vm.loadJournals(page = 1, pageSize = 20, status = "")
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertEquals(listOf(journal), journals)
            assertEquals(1, total)
            assertNull(error)
            assertFalse(isOffline)
        }
    }

    @Test
    fun `journals cached error marks state offline`() = runTest {
        val data = AdminJournalsData(journals = listOf(journal), total = 1)
        every { repository.getJournals(any(), any(), any(), any()) } returns
            flowOf(Resource.Error(RuntimeException("offline"), data))

        val vm = AdminJournalsViewModel(repository)
        vm.loadJournals(page = 1, pageSize = 20, status = "")
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertEquals(listOf(journal), journals)
            assertNull(error)
            assertTrue(isOffline)
        }
    }

    @Test
    fun `periods success updates list`() = runTest {
        val data = AdminPeriodsData(periods = listOf(period))
        every { repository.getPeriods(any(), any()) } returns flowOf(Resource.Success(data))

        val vm = AdminPeriodsViewModel(repository)
        vm.loadPeriods()
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertEquals(listOf(period), periods)
            assertNull(error)
            assertFalse(isOffline)
        }
    }

    @Test
    fun `periods cached error marks state offline`() = runTest {
        val data = AdminPeriodsData(periods = listOf(period))
        every { repository.getPeriods(any(), any()) } returns
            flowOf(Resource.Error(RuntimeException("offline"), data))

        val vm = AdminPeriodsViewModel(repository)
        vm.loadPeriods()
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertEquals(listOf(period), periods)
            assertNull(error)
            assertTrue(isOffline)
        }
    }

    @Test
    fun `access success updates bindings`() = runTest {
        val data = AdminAccessData(bindings = listOf(binding))
        every { repository.getAccessBindings(any(), any()) } returns flowOf(Resource.Success(data))

        val vm = AdminAccessViewModel(repository)
        vm.loadBindings(activeOnly = true)
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertEquals(listOf(binding), bindings)
            assertNull(error)
            assertFalse(isOffline)
        }
    }

    @Test
    fun `access cached error marks state offline`() = runTest {
        val data = AdminAccessData(bindings = listOf(binding))
        every { repository.getAccessBindings(any(), any()) } returns
            flowOf(Resource.Error(RuntimeException("offline"), data))

        val vm = AdminAccessViewModel(repository)
        vm.loadBindings(activeOnly = true)
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertEquals(listOf(binding), bindings)
            assertNull(error)
            assertTrue(isOffline)
        }
    }

    @Test
    fun `problem students success updates results and filters`() = runTest {
        val meta = ProblemStudentsMeta(total = 1)
        val data = AdminProblemStudentsData(
            students = listOf(problemStudent),
            meta = meta,
            periods = listOf(period)
        )
        every {
            repository.getProblemStudents(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns flowOf(Resource.Success(data))

        val vm = AdminProblemStudentsViewModel(repository)
        vm.loadStudents("", "", "", "3", "3", "50", limit = 10, offset = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertEquals(listOf(problemStudent), students)
            assertEquals(meta, this.meta)
            assertEquals(listOf(period), periods)
            assertEquals(1, total)
            assertNull(error)
            assertFalse(isOffline)
        }
    }

    @Test
    fun `problem students cached error marks state offline`() = runTest {
        val data = AdminProblemStudentsData(
            students = listOf(problemStudent),
            meta = ProblemStudentsMeta(total = 1)
        )
        every {
            repository.getProblemStudents(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns flowOf(Resource.Error(RuntimeException("offline"), data))

        val vm = AdminProblemStudentsViewModel(repository)
        vm.loadStudents("", "", "", "3", "3", "50", limit = 10, offset = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertEquals(listOf(problemStudent), students)
            assertEquals(1, total)
            assertNull(error)
            assertTrue(isOffline)
        }
    }
}
