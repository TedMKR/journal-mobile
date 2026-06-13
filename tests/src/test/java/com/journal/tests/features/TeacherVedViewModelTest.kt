package com.journal.tests.features

import com.journal.core.data.repository.TeacherVedCatalogData
import com.journal.core.data.repository.TeacherVedRepository
import com.journal.core.data.util.Resource
import com.journal.core.model.teacher.AcademicPeriod
import com.journal.core.model.teacher.TeacherLesson
import com.journal.features.teacher.ved.TeacherVedViewModel
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
class TeacherVedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TeacherVedRepository

    private val period = AcademicPeriod(
        id = "period-1",
        name = "Spring",
        startsAt = "2026-01-01",
        endsAt = "2026-06-30",
        isClosed = false,
        isActive = true
    )
    private val lesson = TeacherLesson(
        id = "lesson-1",
        disciplineName = "Math",
        groupName = "IS-21",
        lessonType = "practice",
        scheduledAt = "2026-01-12T09:00:00Z",
        disciplineId = "discipline-1",
        groupId = "group-1",
        periodId = period.id
    )
    private val data = TeacherVedCatalogData(
        periods = listOf(period),
        lessons = listOf(lesson),
        selectedPeriodId = period.id
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
    fun `success updates catalog`() = runTest {
        every { repository.getCatalog(any(), any()) } returns flowOf(Resource.Success(data))

        val vm = TeacherVedViewModel(repository)
        vm.loadCatalog()
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertEquals(listOf(period), periods)
            assertEquals(listOf(lesson), lessons)
            assertEquals(period.id, selectedPeriodId)
            assertNull(error)
            assertFalse(isOffline)
        }
    }

    @Test
    fun `error with cached data marks state offline`() = runTest {
        every { repository.getCatalog(any(), any()) } returns flowOf(Resource.Error(RuntimeException("offline"), data))

        val vm = TeacherVedViewModel(repository)
        vm.loadCatalog()
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertEquals(listOf(period), periods)
            assertEquals(listOf(lesson), lessons)
            assertNull(error)
            assertTrue(isOffline)
        }
    }
}
