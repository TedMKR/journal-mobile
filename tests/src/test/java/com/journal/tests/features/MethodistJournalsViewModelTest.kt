package com.journal.tests.features

import com.journal.core.data.repository.MethodistJournalsData
import com.journal.core.data.repository.MethodistRepository
import com.journal.core.data.util.Resource
import com.journal.core.model.teacher.AcademicPeriod
import com.journal.core.model.teacher.JournalContext
import com.journal.features.methodist.journals.MethodistJournalsViewModel
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
class MethodistJournalsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: MethodistRepository

    private val period = AcademicPeriod(
        id = "period-1",
        name = "Spring",
        startsAt = "2026-01-01",
        endsAt = "2026-06-30",
        isClosed = false,
        isActive = true
    )
    private val journal = JournalContext(
        journalId = "journal-1",
        disciplineId = "discipline-1",
        disciplineName = "Math",
        groupId = "group-1",
        groupName = "IS-21",
        periodId = period.id
    )
    private val data = MethodistJournalsData(
        periods = listOf(period),
        journals = listOf(journal)
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
    fun `success updates journals and filters`() = runTest {
        every {
            repository.getJournals(any(), any(), any(), any(), any(), any(), any())
        } returns flowOf(Resource.Success(data))

        val vm = MethodistJournalsViewModel(repository)
        vm.loadJournals("", "", "", "", "", "")
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertEquals(listOf(journal), journals)
            assertEquals(listOf(period), periods)
            assertNull(error)
            assertFalse(isOffline)
        }
    }

    @Test
    fun `error with cached data marks state offline`() = runTest {
        every {
            repository.getJournals(any(), any(), any(), any(), any(), any(), any())
        } returns flowOf(Resource.Error(RuntimeException("offline"), data))

        val vm = MethodistJournalsViewModel(repository)
        vm.loadJournals("", "", "", "", "", "")
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertEquals(listOf(journal), journals)
            assertNull(error)
            assertTrue(isOffline)
        }
    }
}
