package com.journal.core.data.notification

import com.journal.core.model.teacher.StudentJournalGrade
import com.journal.core.model.teacher.StudentSubjectCard
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentGradeChangeDetector @Inject constructor(
    private val notifier: AndroidSystemNotifier
) {
    fun notifyGradeChanges(previous: StudentSubjectCard?, current: StudentSubjectCard) {
        if (previous == null) return

        val oldGrades = previous.journalGrades.associateBy { it.assessmentFormId }
        val newGrades = current.journalGrades.associateBy { it.assessmentFormId }

        newGrades.values.forEach { newGrade ->
            val oldGrade = oldGrades[newGrade.assessmentFormId]
            val oldValue = oldGrade?.normalizedValue()
            val newValue = newGrade.normalizedValue()

            when {
                oldValue == null && newValue != null -> notify(
                    current = current,
                    grade = newGrade,
                    type = StudentGradeNotificationType.CREATED,
                    oldValue = null,
                    newValue = newValue
                )
                oldValue != null && newValue != null && oldValue != newValue -> notify(
                    current = current,
                    grade = newGrade,
                    type = StudentGradeNotificationType.UPDATED,
                    oldValue = oldValue,
                    newValue = newValue
                )
            }
        }

        oldGrades.values.forEach { oldGrade ->
            val oldValue = oldGrade.normalizedValue() ?: return@forEach
            val newValue = newGrades[oldGrade.assessmentFormId]?.normalizedValue()
            if (newValue == null) {
                notify(
                    current = current,
                    grade = oldGrade,
                    type = StudentGradeNotificationType.DELETED,
                    oldValue = oldValue,
                    newValue = null
                )
            }
        }
    }

    private fun notify(
        current: StudentSubjectCard,
        grade: StudentJournalGrade,
        type: StudentGradeNotificationType,
        oldValue: String?,
        newValue: String?
    ) {
        notifier.notifyGradeChange(
            StudentGradeNotificationEvent(
                type = type,
                disciplineId = current.disciplineId,
                disciplineName = current.disciplineName,
                assessmentFormId = grade.assessmentFormId,
                assessmentTitle = grade.title,
                oldValue = oldValue,
                newValue = newValue
            )
        )
    }
}

private fun StudentJournalGrade.normalizedValue(): String? =
    value?.trim()?.takeIf { it.isNotBlank() }
