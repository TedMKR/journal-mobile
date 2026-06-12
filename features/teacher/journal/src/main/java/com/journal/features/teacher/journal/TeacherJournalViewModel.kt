package com.journal.features.teacher.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.data.repository.JournalRepository
import com.journal.core.data.repository.PendingJournalAction
import com.journal.core.data.util.Resource
import com.journal.core.data.util.userMessage
import com.journal.core.model.teacher.BulkAttendanceRecordRequest
import com.journal.core.model.teacher.BulkMarkAttendanceRequest
import com.journal.core.model.teacher.CreateAssessmentFormRequest
import com.journal.core.model.teacher.CreateGradeRequest
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.MarkAttendanceRequest
import com.journal.core.model.teacher.UpdateAssessmentFormRequest
import com.journal.core.model.teacher.UpdateGradeRequest
import com.journal.core.model.teacher.UpdateLessonTopicDetailsRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class JournalUiState(
    val isLoading: Boolean = false,
    val journal: JournalGridResponse? = null,
    val error: String? = null,
    /** True when showing cached data because the network is unavailable */
    val isOffline: Boolean = false,
    /** Number of actions waiting to sync */
    val pendingCount: Int = 0,
    val pendingActions: List<PendingJournalAction> = emptyList()
)

@HiltViewModel
class TeacherJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])
    val disciplineId: String = checkNotNull(savedStateHandle["disciplineId"])
    val periodId: String = checkNotNull(savedStateHandle["periodId"])
    val lessonType: String = savedStateHandle["lessonType"] ?: ""
    val teacherId: String? = savedStateHandle["teacherId"]

    private val _uiState = MutableStateFlow(JournalUiState(isLoading = true))
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        loadJournal()
        observePending()
    }

    private fun observePending() {
        journalRepository.observePendingForJournal(groupId, disciplineId, periodId)
            .onEach { actions ->
                _uiState.update {
                    it.copy(
                        pendingCount = actions.size,
                        pendingActions = actions
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadJournal() {
        journalRepository.getJournalGrid(
            groupId = groupId,
            disciplineId = disciplineId,
            periodId = periodId,
            lessonType = lessonType,
            teacherId = teacherId?.takeIf { it.isNotBlank() }
        ).onEach { resource ->
            _uiState.update { state ->
                when (resource) {
                    is Resource.Loading -> state.copy(
                        isLoading = resource.data == null,
                        journal = resource.data ?: state.journal,
                        error = null
                    )
                    is Resource.Success -> state.copy(
                        isLoading = false,
                        journal = resource.data,
                        error = null,
                        isOffline = false
                    )
                    is Resource.Error -> state.copy(
                        isLoading = false,
                        // Keep showing cached data if we have it
                        journal = resource.data ?: state.journal,
                        error = if ((resource.data ?: state.journal) == null) {
                            if ((resource.throwable as? HttpException)?.code() == 403) {
                                "Нет журнала для этого занятия"
                            } else {
                                resource.throwable.userMessage("Не удалось загрузить журнал")
                            }
                        } else null,
                        isOffline = (resource.data ?: state.journal) != null
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    // ─── Write operations ─────────────────────────────────────────────────────
    // Each call is non-blocking: online → sends directly; offline → queues to Room

    private fun launchWrite(defaultError: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                loadJournal()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.userMessage(defaultError)) }
            }
        }
    }

    fun markAttendance(lessonId: String, studentId: String, status: String, comment: String?) {
        launchWrite("Не удалось сохранить посещаемость") {
            journalRepository.markAttendance(
                lessonId = lessonId,
                request = MarkAttendanceRequest(
                    studentId = studentId,
                    status = status,
                    comment = comment?.takeIf { it.isNotBlank() }
                ),
                groupId = groupId,
                disciplineId = disciplineId,
                periodId = periodId,
                lessonType = lessonType
            )
        }
    }

    fun createGrade(studentId: String, assessmentFormId: String, value: Int, comment: String?) {
        launchWrite("Не удалось создать оценку") {
            journalRepository.createGrade(
                request = CreateGradeRequest(
                    studentId = studentId,
                    assessmentFormId = assessmentFormId,
                    value = value,
                    comment = comment?.takeIf { it.isNotBlank() }
                ),
                groupId = groupId,
                disciplineId = disciplineId,
                periodId = periodId,
                lessonType = lessonType
            )
        }
    }

    fun updateGrade(gradeId: String, value: Int, comment: String?) {
        launchWrite("Не удалось обновить оценку") {
            journalRepository.updateGrade(
                gradeId = gradeId,
                request = UpdateGradeRequest(
                    value = value,
                    comment = comment?.takeIf { it.isNotBlank() }
                ),
                groupId = groupId,
                disciplineId = disciplineId,
                periodId = periodId,
                lessonType = lessonType
            )
        }
    }

    fun createAssessmentForm(title: String, type: String, date: String) {
        launchWrite("Не удалось создать форму контроля") {
            journalRepository.createAssessmentForm(
                request = CreateAssessmentFormRequest(
                    title = title,
                    formType = type,
                    date = date,
                    disciplineId = disciplineId,
                    groupId = groupId,
                    periodId = periodId
                ),
                lessonType = lessonType
            )
        }
    }

    fun updateAssessmentForm(assessmentFormId: String, title: String, type: String, date: String) {
        launchWrite("Не удалось обновить форму контроля") {
            journalRepository.updateAssessmentForm(
                assessmentFormId = assessmentFormId,
                request = UpdateAssessmentFormRequest(title = title, formType = type, date = date),
                groupId = groupId,
                disciplineId = disciplineId,
                periodId = periodId,
                lessonType = lessonType
            )
        }
    }

    fun deleteAssessmentForm(assessmentFormId: String) {
        launchWrite("Не удалось удалить форму контроля") {
            journalRepository.deleteAssessmentForm(
                assessmentFormId = assessmentFormId,
                groupId = groupId,
                disciplineId = disciplineId,
                periodId = periodId,
                lessonType = lessonType
            )
        }
    }

    fun bulkMarkAttendance(lessonId: String, status: String, target: String) {
        viewModelScope.launch {
            try {
                val journal = _uiState.value.journal ?: return@launch
                // Determine which students to mark based on the target filter
                val existingForLesson = journal.attendance.filter { it.lessonId == lessonId }
                val targetStudents = when (target) {
                    "unmarked" -> journal.students.filter { student ->
                        existingForLesson.none { it.studentId == student.studentId }
                    }
                    else -> journal.students   // "all"
                }
                if (targetStudents.isEmpty()) return@launch
                val records = targetStudents.map { student ->
                    BulkAttendanceRecordRequest(studentId = student.studentId, status = status)
                }
                journalRepository.bulkMarkAttendance(
                    lessonId = lessonId,
                    request = BulkMarkAttendanceRequest(records = records),
                    groupId = groupId,
                    disciplineId = disciplineId,
                    periodId = periodId,
                    lessonType = lessonType
                )
                loadJournal()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.userMessage("Не удалось выполнить массовую отметку"))
                }
            }
        }
    }

    fun updateLessonTopic(lessonId: String, topic: String) {
        launchWrite("Не удалось обновить тему занятия") {
            journalRepository.updateLessonTopic(
                lessonId = lessonId,
                request = UpdateLessonTopicDetailsRequest(topicCustomDetails = topic),
                groupId = groupId,
                disciplineId = disciplineId,
                periodId = periodId,
                lessonType = lessonType
            )
        }
    }
}
