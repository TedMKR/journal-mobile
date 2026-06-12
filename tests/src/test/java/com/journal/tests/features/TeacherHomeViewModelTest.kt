package com.journal.tests.features

import app.cash.turbine.test
import com.journal.core.data.repository.TeacherRepository
import com.journal.core.data.util.Resource
import com.journal.core.model.teacher.TeacherLesson
import com.journal.features.teacher.home.TeacherHomeViewModel
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

/**
 * Unit tests for [TeacherHomeViewModel].
 *
 * [TeacherRepository] is mocked so no DB or network is involved.
 * [Dispatchers.Main] is replaced with a [StandardTestDispatcher] so
 * coroutines launched in [viewModelScope] run under test control.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TeacherHomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: TeacherRepository

    // ─── Sample data ──────────────────────────────────────────────────────────

    private val lesson1 = TeacherLesson(
        id = "l1",
        disciplineName = "Математика",
        groupName = "ИС-21",
        lessonType = "lecture",
        scheduledAt = "2026-05-01T08:00:00"
    )
    private val lesson2 = TeacherLesson(
        id = "l2",
        disciplineName = "Физика",
        groupName = "ИС-21",
        lessonType = "practice",
        scheduledAt = "2026-05-02T10:00:00"
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

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun `initial uiState has isLoading = true`() = runTest {
        every { repository.getLessons(any(), any(), any()) } returns flowOf(
            Resource.Loading(null)
        )

        val vm = TeacherHomeViewModel(repository)
        assertTrue(vm.uiState.value.isLoading)
    }

    // ─── Success state ────────────────────────────────────────────────────────

    @Test
    fun `uiState transitions to success after successful load`() = runTest {
        every { repository.getLessons(any(), any(), any()) } returns flowOf(
            Resource.Loading(null),
            Resource.Success(listOf(lesson1, lesson2))
        )

        val vm = TeacherHomeViewModel(repository)

        vm.uiState.test {
            // Initial Loading from MutableStateFlow initializer
            val initial = awaitItem()
            assertTrue(initial.isLoading)

            // After coroutine runs
            testDispatcher.scheduler.advanceUntilIdle()

            val success = awaitItem()
            assertFalse(success.isLoading)
            assertEquals(2, success.lessons.size)
            assertNull(success.error)
            assertFalse(success.isOffline)
        }
    }

    @Test
    fun `uiState contains correct lesson data on success`() = runTest {
        every { repository.getLessons(any(), any(), any()) } returns flowOf(
            Resource.Success(listOf(lesson1))
        )

        val vm = TeacherHomeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("l1", vm.uiState.value.lessons.first().id)
        assertEquals("Математика", vm.uiState.value.lessons.first().disciplineName)
    }

    // ─── Error state ──────────────────────────────────────────────────────────

    @Test
    fun `uiState shows error message when network fails with empty cache`() = runTest {
        val exception = RuntimeException("no network")
        every { repository.getLessons(any(), any(), any()) } returns flowOf(
            Resource.Error(exception, null)
        )

        val vm = TeacherHomeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertTrue(lessons.isEmpty())
            assertEquals("Не удалось загрузить расписание. Попробуйте позже.", error)
        }
    }

    @Test
    fun `uiState shows offline when network fails but cache is available`() = runTest {
        val exception = RuntimeException("offline")
        every { repository.getLessons(any(), any(), any()) } returns flowOf(
            Resource.Error(exception, listOf(lesson1))
        )

        val vm = TeacherHomeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertEquals(1, lessons.size)
            assertNull(error)        // no error message shown when cache exists
            assertTrue(isOffline)
        }
    }

    // ─── Loading state ────────────────────────────────────────────────────────

    @Test
    fun `uiState shows cached lessons during Loading`() = runTest {
        every { repository.getLessons(any(), any(), any()) } returns flowOf(
            Resource.Loading(listOf(lesson1, lesson2))
        )

        val vm = TeacherHomeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertTrue(isLoading)
            assertEquals(2, lessons.size)
        }
    }

    // ─── loadLessons ─────────────────────────────────────────────────────────

    @Test
    fun `loadLessons can be called again to refresh`() = runTest {
        var callCount = 0
        every { repository.getLessons(any(), any(), any()) } answers {
            callCount++
            flowOf(Resource.Success(listOf(lesson1)))
        }

        val vm = TeacherHomeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.loadLessons()
        testDispatcher.scheduler.advanceUntilIdle()

        // Called once in init, once in manual loadLessons()
        assertEquals(2, callCount)
    }
}
