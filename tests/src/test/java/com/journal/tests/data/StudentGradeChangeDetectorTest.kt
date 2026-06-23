package com.journal.tests.data

import com.journal.core.data.notification.AndroidSystemNotifier
import com.journal.core.data.notification.StudentGradeChangeDetector
import com.journal.core.data.notification.StudentGradeNotificationEvent
import com.journal.core.data.notification.StudentGradeNotificationType
import com.journal.core.model.teacher.StudentJournalGrade
import com.journal.core.model.teacher.StudentSubjectCard
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StudentGradeChangeDetectorTest {

    private lateinit var notifier: AndroidSystemNotifier
    private lateinit var detector: StudentGradeChangeDetector

    @Before
    fun setUp() {
        notifier = mockk(relaxed = true)
        detector = StudentGradeChangeDetector(notifier)
    }

    @Test
    fun `does not notify on initial card load`() {
        detector.notifyGradeChanges(
            previous = null,
            current = card(grades = listOf(grade("assessment-1", "Контрольная", "5")))
        )

        verify(exactly = 0) { notifier.notifyGradeChange(any()) }
    }

    @Test
    fun `notifies about created updated and deleted grades`() {
        val events = mutableListOf<StudentGradeNotificationEvent>()
        every { notifier.notifyGradeChange(capture(events)) } just Runs

        detector.notifyGradeChanges(
            previous = card(
                grades = listOf(
                    grade("assessment-1", "Контрольная", "3"),
                    grade("assessment-2", "Практика", "4"),
                    grade("assessment-3", "Доклад", "5")
                )
            ),
            current = card(
                grades = listOf(
                    grade("assessment-1", "Контрольная", "5"),
                    grade("assessment-2", "Практика", "4"),
                    grade("assessment-4", "Лабораторная", "2")
                )
            )
        )

        assertEquals(
            listOf(
                StudentGradeNotificationType.UPDATED,
                StudentGradeNotificationType.CREATED,
                StudentGradeNotificationType.DELETED
            ),
            events.map { it.type }
        )
        assertEquals("3", events[0].oldValue)
        assertEquals("5", events[0].newValue)
        assertEquals("2", events[1].newValue)
        assertEquals("5", events[2].oldValue)
        assertEquals("Математика", events[0].disciplineName)
    }

    private fun card(
        grades: List<StudentJournalGrade>
    ) = StudentSubjectCard(
        disciplineId = "discipline-1",
        disciplineName = "Математика",
        groupId = "group-1",
        groupName = "Группа 1",
        periodId = "period-1",
        journalGrades = grades
    )

    private fun grade(
        assessmentFormId: String,
        title: String,
        value: String?
    ) = StudentJournalGrade(
        assessmentFormId = assessmentFormId,
        title = title,
        type = "test",
        date = "2026-06-23",
        value = value
    )
}
