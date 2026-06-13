package com.journal.features.teacher.ved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.data.repository.TeacherVedRepository
import com.journal.core.data.util.Resource
import com.journal.core.data.util.userMessage
import com.journal.core.model.teacher.AcademicPeriod
import com.journal.core.model.teacher.CurrentAttestationPrefill
import com.journal.core.model.teacher.DocumentTask
import com.journal.core.model.teacher.JobAccepted
import com.journal.core.model.teacher.RequestReportPayload
import com.journal.core.model.teacher.TeacherLesson
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import okhttp3.ResponseBody

data class TeacherVedUiState(
    val periods: List<AcademicPeriod> = emptyList(),
    val lessons: List<TeacherLesson> = emptyList(),
    val selectedPeriodId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class TeacherVedViewModel @Inject constructor(
    private val teacherVedRepository: TeacherVedRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherVedUiState(isLoading = true))
    val uiState: StateFlow<TeacherVedUiState> = _uiState.asStateFlow()

    private var catalogJob: Job? = null

    fun loadCatalog(periodId: String? = null) {
        catalogJob?.cancel()
        catalogJob = teacherVedRepository.getCatalog(periodId)
            .onEach { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> {
                            val data = resource.data
                            state.copy(
                                isLoading = data == null && state.periods.isEmpty(),
                                error = null,
                                periods = data?.periods ?: state.periods,
                                lessons = data?.lessons ?: state.lessons,
                                selectedPeriodId = data?.selectedPeriodId ?: state.selectedPeriodId
                            )
                        }
                        is Resource.Success -> {
                            val data = resource.data
                            state.copy(
                                isLoading = false,
                                error = null,
                                isOffline = false,
                                periods = data?.periods.orEmpty(),
                                lessons = data?.lessons.orEmpty(),
                                selectedPeriodId = data?.selectedPeriodId
                            )
                        }
                        is Resource.Error -> {
                            val data = resource.data
                            val hasData = data != null || state.periods.isNotEmpty()
                            state.copy(
                                isLoading = false,
                                error = if (hasData) null else resource.throwable.userMessage("Не удалось загрузить группы и предметы преподавателя"),
                                isOffline = hasData,
                                periods = data?.periods ?: state.periods,
                                lessons = data?.lessons ?: state.lessons,
                                selectedPeriodId = data?.selectedPeriodId ?: state.selectedPeriodId
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    suspend fun getPrefill(
        groupId: String,
        disciplineId: String,
        academicPeriodId: String
    ): CurrentAttestationPrefill = teacherVedRepository.getPrefill(groupId, disciplineId, academicPeriodId)

    suspend fun requestReport(idempotencyKey: String, payload: RequestReportPayload): JobAccepted =
        teacherVedRepository.requestReport(idempotencyKey, payload)

    suspend fun getReportStatus(jobId: String): DocumentTask = teacherVedRepository.getReportStatus(jobId)

    suspend fun downloadReportFile(jobId: String): ResponseBody = teacherVedRepository.downloadReportFile(jobId)
}
