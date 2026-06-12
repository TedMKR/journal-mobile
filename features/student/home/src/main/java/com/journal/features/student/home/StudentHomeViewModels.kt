package com.journal.features.student.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.common.config.userFacingMessage
import com.journal.core.data.repository.StudentDashboardData
import com.journal.core.data.repository.StudentRepository
import com.journal.core.data.util.Resource
import com.journal.core.model.teacher.StudentLesson
import com.journal.core.model.teacher.StudentProfile
import com.journal.core.model.teacher.StudentSubjectSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class StudentScheduleUiState(
    val isLoading: Boolean = false,
    val lessons: List<StudentLesson> = emptyList(),
    val error: String? = null,
    val isOffline: Boolean = false,
    val weekMonday: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY),
    val isCurrentWeek: Boolean = true
)

@HiltViewModel
class StudentScheduleViewModel @Inject constructor(
    private val studentRepository: StudentRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private var weekOffset = 0
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(StudentScheduleUiState(isLoading = true))
    val uiState: StateFlow<StudentScheduleUiState> = _uiState.asStateFlow()

    init {
        loadLessons()
    }

    fun navigateWeek(delta: Int) {
        weekOffset += delta
        loadLessons()
    }

    private fun loadLessons() {
        val monday = today.with(DayOfWeek.MONDAY).plusWeeks(weekOffset.toLong())
        val sunday = monday.plusDays(6)

        loadJob?.cancel()
        loadJob = studentRepository.getLessons(
            dateFrom = monday.toString(),
            dateTo = sunday.toString()
        ).onEach { resource ->
            _uiState.value = when (resource) {
                is Resource.Loading -> _uiState.value.copy(
                    isLoading = true,
                    lessons = resource.data ?: emptyList(),
                    error = null,
                    weekMonday = monday,
                    isCurrentWeek = weekOffset == 0
                )

                is Resource.Success -> StudentScheduleUiState(
                    isLoading = false,
                    lessons = resource.data,
                    weekMonday = monday,
                    isCurrentWeek = weekOffset == 0
                )

                is Resource.Error -> StudentScheduleUiState(
                    isLoading = false,
                    lessons = resource.data ?: emptyList(),
                    error = if (resource.data.isNullOrEmpty()) {
                        resource.throwable.userFacingMessage("Не удалось загрузить расписание")
                    } else null,
                    isOffline = resource.data?.isNotEmpty() == true,
                    weekMonday = monday,
                    isCurrentWeek = weekOffset == 0
                )
            }
        }.launchIn(viewModelScope)
    }
}

data class StudentDashboardUiState(
    val isLoading: Boolean = false,
    val profile: StudentProfile? = null,
    val subjects: List<StudentSubjectSummary> = emptyList(),
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class StudentDashboardViewModel @Inject constructor(
    private val studentRepository: StudentRepository
) : ViewModel() {

    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(StudentDashboardUiState(isLoading = true))
    val uiState: StateFlow<StudentDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        loadJob?.cancel()
        loadJob = studentRepository.getDashboard()
            .onEach { resource ->
                _uiState.value = when (resource) {
                    is Resource.Loading -> _uiState.value.copy(
                        isLoading = true,
                        profile = resource.data?.profile,
                        subjects = resource.data?.subjects.orEmpty(),
                        error = null
                    )

                    is Resource.Success -> resource.data.toUiState(isOffline = false)

                    is Resource.Error -> {
                        val cached = resource.data
                        if (cached != null) {
                            cached.toUiState(isOffline = true)
                        } else {
                            StudentDashboardUiState(
                                isLoading = false,
                                error = resource.throwable.userFacingMessage("Не удалось загрузить личный кабинет")
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}

private fun StudentDashboardData?.toUiState(isOffline: Boolean): StudentDashboardUiState =
    StudentDashboardUiState(
        isLoading = false,
        profile = this?.profile,
        subjects = this?.subjects.orEmpty(),
        isOffline = isOffline
    )
