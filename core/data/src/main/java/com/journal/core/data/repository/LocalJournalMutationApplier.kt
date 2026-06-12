package com.journal.core.data.repository

import com.journal.core.database.entity.PendingActionEntity
import com.journal.core.database.entity.PendingActionType
import com.journal.core.model.teacher.CreateAssessmentFormRequest
import com.journal.core.model.teacher.CreateGradeRequest
import com.journal.core.model.teacher.JournalGridAssessmentForm
import com.journal.core.model.teacher.JournalGridAttendance
import com.journal.core.model.teacher.JournalGridGrade
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.MarkAttendanceRequest
import com.journal.core.model.teacher.UpdateAssessmentFormRequest
import com.journal.core.model.teacher.UpdateGradeRequest
import com.journal.core.model.teacher.UpdateLessonTopicDetailsRequest
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalJournalMutationApplier @Inject constructor(
    private val json: Json
) {

    fun apply(grid: JournalGridResponse, action: PendingActionEntity): JournalGridResponse =
        when (action.actionType) {
            PendingActionType.MARK_ATTENDANCE -> applyAttendance(grid, action)
            PendingActionType.CREATE_GRADE -> applyCreateGrade(grid, action)
            PendingActionType.UPDATE_GRADE -> applyUpdateGrade(grid, action)
            PendingActionType.DELETE_GRADE -> grid.copy(
                grades = grid.grades.filterNot { it.gradeId == action.entityId }
            )
            PendingActionType.CREATE_ASSESSMENT_FORM -> applyCreateAssessmentForm(grid, action)
            PendingActionType.UPDATE_ASSESSMENT_FORM -> applyUpdateAssessmentForm(grid, action)
            PendingActionType.DELETE_ASSESSMENT_FORM -> grid.copy(
                assessmentForms = grid.assessmentForms.filterNot {
                    it.assessmentFormId == action.entityId
                },
                grades = grid.grades.filterNot { it.assessmentFormId == action.entityId }
            )
            PendingActionType.UPDATE_LESSON_TOPIC -> applyLessonTopic(grid, action)
            else -> grid
        }

    private fun applyAttendance(
        grid: JournalGridResponse,
        action: PendingActionEntity
    ): JournalGridResponse {
        val request = json.decodeFromString<MarkAttendanceRequest>(action.payloadJson)
        val now = nowIso()
        var replaced = false
        val updated = grid.attendance.map { record ->
            if (record.lessonId == action.entityId && record.studentId == request.studentId) {
                replaced = true
                record.copy(
                    status = request.status,
                    comment = request.comment,
                    updatedAt = now,
                    version = record.version.ifBlank { "local" }
                )
            } else {
                record
            }
        }.toMutableList()

        if (!replaced) {
            updated += JournalGridAttendance(
                attendanceId = action.localId ?: "local_attendance_${action.createdAt}",
                lessonId = action.entityId,
                studentId = request.studentId,
                status = request.status,
                comment = request.comment,
                updatedAt = now,
                version = "local"
            )
        }

        return grid.copy(attendance = updated)
    }

    private fun applyCreateGrade(
        grid: JournalGridResponse,
        action: PendingActionEntity
    ): JournalGridResponse {
        val request = json.decodeFromString<CreateGradeRequest>(action.payloadJson)
        val localId = action.localId ?: "local_grade_${action.createdAt}"
        if (grid.grades.any { it.gradeId == localId }) return grid

        return grid.copy(
            grades = grid.grades + JournalGridGrade(
                gradeId = localId,
                assessmentFormId = request.assessmentFormId,
                studentId = request.studentId,
                value = request.value.toString(),
                comment = request.comment,
                updatedAt = nowIso(),
                version = "local"
            )
        )
    }

    private fun applyUpdateGrade(
        grid: JournalGridResponse,
        action: PendingActionEntity
    ): JournalGridResponse {
        val request = json.decodeFromString<UpdateGradeRequest>(action.payloadJson)
        return grid.copy(
            grades = grid.grades.map { grade ->
                if (grade.gradeId == action.entityId) {
                    grade.copy(
                        value = request.value.toString(),
                        comment = request.comment,
                        updatedAt = nowIso(),
                        version = grade.version.ifBlank { "local" }
                    )
                } else {
                    grade
                }
            }
        )
    }

    private fun applyCreateAssessmentForm(
        grid: JournalGridResponse,
        action: PendingActionEntity
    ): JournalGridResponse {
        val request = json.decodeFromString<CreateAssessmentFormRequest>(action.payloadJson)
        val localId = action.localId ?: "local_form_${action.createdAt}"
        if (grid.assessmentForms.any { it.assessmentFormId == localId }) return grid

        return grid.copy(
            assessmentForms = grid.assessmentForms + JournalGridAssessmentForm(
                assessmentFormId = localId,
                title = request.title,
                type = request.formType,
                maxScore = null,
                date = request.date,
                status = "active",
                updatedAt = nowIso()
            )
        )
    }

    private fun applyUpdateAssessmentForm(
        grid: JournalGridResponse,
        action: PendingActionEntity
    ): JournalGridResponse {
        val request = json.decodeFromString<UpdateAssessmentFormRequest>(action.payloadJson)
        return grid.copy(
            assessmentForms = grid.assessmentForms.map { form ->
                if (form.assessmentFormId == action.entityId) {
                    form.copy(
                        title = request.title ?: form.title,
                        type = request.formType ?: form.type,
                        date = request.date ?: form.date,
                        status = request.status ?: form.status,
                        updatedAt = nowIso()
                    )
                } else {
                    form
                }
            }
        )
    }

    private fun applyLessonTopic(
        grid: JournalGridResponse,
        action: PendingActionEntity
    ): JournalGridResponse {
        val request = json.decodeFromString<UpdateLessonTopicDetailsRequest>(action.payloadJson)
        return grid.copy(
            lessons = grid.lessons.map { lesson ->
                if (lesson.lessonId == action.entityId) {
                    lesson.copy(
                        topic = request.topicCustomDetails,
                        updatedAt = nowIso()
                    )
                } else {
                    lesson
                }
            }
        )
    }

    private fun nowIso(): String = Instant.now().toString()
}
