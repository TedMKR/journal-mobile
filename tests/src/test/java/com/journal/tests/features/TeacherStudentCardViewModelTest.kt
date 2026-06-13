package com.journal.tests.features

import androidx.lifecycle.SavedStateHandle
import com.journal.core.data.repository.JournalRepository
import com.journal.core.data.util.Resource
import com.journal.core.model.teacher.JournalGridAcademicPeriod
import com.journal.core.model.teacher.JournalGridAssessmentForm
import com.journal.core.model.teacher.JournalGridAttendance
import com.journal.core.model.teacher.JournalGridGrade
import com.journal.core.model.teacher.JournalGridLesson
import com.journal.core.model.teacher.JournalGridMeta
import com.journal.core.model.teacher.JournalGridPermissions
import com.journal.core.model.teacher.JournalGridRef
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.JournalGridStudent
import com.journal.core.model.teacher.JournalGridTeacher
import com.journal.features.teacher.studentcard.TeacherStudentCardViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class TeacherStudentCardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: JournalRepository

    private val groupId = "g1"
    private val disciplineId = "d1"
    private val periodId = "p1"
    private val lessonType = "lecture"
    private val studentId = "s1"

    private val savedStateHandle = SavedStateHandle(
        mapOf(
            "groupId" to groupId,
            "disciplineId" to disciplineId,
            "periodId" to periodId,
            "lessonType" to lessonType,
            "studentId" to studentId
        )
    )

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
        students = listOf(
            JournalGridStudent(studentId = studentId, fullName = "Петров Петр Петрович")
        ),
        lessons = listOf(
            JournalGridLesson(
                lessonId = "l1",
                date = "2026-09-01",
                scheduledAt = "2026-09-01T09:00:00Z",
                topic = "Введение",
                lessonType = lessonType,
                status = "completed",
                updatedAt = "2026-09-01T10:00:00Z"
            ),
            JournalGridLesson(
                lessonId = "l2",
                date = "2026-10-01",
                scheduledAt = "2026-10-01T09:00:00Z",
                topic = "Практика",
                lessonType = lessonType,
                status = "completed",
                updatedAt = "2026-10-01T10:00:00Z"
            )
        ),
        attendance = listOf(
            JournalGridAttendance(
                attendanceId = "a1",
                lessonId = "l1",
                studentId = studentId,
                status = "present",
                updatedAt = "2026-09-01T10:00:00Z",
                version = "1"
            )
        ),
        grades = listOf(
            JournalGridGrade(
                gradeId = "gr1",
                assessmentFormId = "af1",
                studentId = studentId,
                value = "4",
                updatedAt = "2026-09-01T10:00:00Z",
                version = "1"
            ),
            JournalGridGrade(
                gradeId = "gr2",
                assessmentFormId = "af2",
                studentId = studentId,
                value = "5",
                updatedAt = "2026-10-01T10:00:00Z",
                version = "1"
            )
        ),
        assessmentForms = listOf(
            JournalGridAssessmentForm(
                assessmentFormId = "af1",
                title = "Контрольная 1",
                type = "test",
                date = "2026-09-01"
            ),
            JournalGridAssessmentForm(
                assessmentFormId = "af2",
                title = "Контрольная 2",
                type = "test",
                date = "2026-10-01"
            )
        ),
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `success builds student card from cached journal grid`() = runTest {
        every {
            repository.getJournalGrid(groupId, disciplineId, periodId, lessonType, null, any())
        } returns flowOf(Resource.Success(sampleJournal))

        val vm = TeacherStudentCardViewModel(savedStateHandle, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertNotNull(card)
            assertEquals("ИС-21", card?.groupName)
            assertEquals("4.5", card?.avgGrade)
            assertEquals(1, card?.absencesCount)
            assertEquals(1, card?.attendedLessons)
            assertEquals(2, card?.completedAssessments)
            assertNull(error)
            assertFalse(isOffline)
        }
    }

    @Test
    fun `network error with cache shows card as offline`() = runTest {
        every {
            repository.getJournalGrid(groupId, disciplineId, periodId, lessonType, null, any())
        } returns flowOf(Resource.Error(RuntimeException("offline"), sampleJournal))

        val vm = TeacherStudentCardViewModel(savedStateHandle, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertNotNull(card)
            assertNull(error)
            assertTrue(isOffline)
        }
    }

    @Test
    fun `network error without cache shows friendly error`() = runTest {
        every {
            repository.getJournalGrid(groupId, disciplineId, periodId, lessonType, null, any())
        } returns flowOf(Resource.Error(RuntimeException("offline"), null))

        val vm = TeacherStudentCardViewModel(savedStateHandle, repository)
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertNull(card)
            assertEquals("Не удалось загрузить карточку студента", error)
            assertFalse(isOffline)
        }
    }
}
