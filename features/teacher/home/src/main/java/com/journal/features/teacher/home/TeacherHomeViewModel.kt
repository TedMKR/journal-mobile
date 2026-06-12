package com.journal.features.teacher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.data.repository.TeacherRepository
import com.journal.core.data.util.Resource
import com.journal.core.data.util.userMessage
import com.journal.core.model.teacher.TeacherLesson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class TeacherHomeUiState(
    val isLoading: Boolean = false,
    val lessons: List<TeacherLesson> = emptyList(),
    val error: String? = null,
    /** True if the data is from cache and network is unavailable */
    val isOffline: Boolean = false,
    val weekMonday: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY),
    val isCurrentWeek: Boolean = true
)

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private var weekOffset: Int = 0

    private val _uiState = MutableStateFlow(TeacherHomeUiState(isLoading = true))
    val uiState: StateFlow<TeacherHomeUiState> = _uiState.asStateFlow()

    init {
        loadLessons()
    }

    fun navigateWeek(delta: Int) {
        weekOffset += delta
        loadLessons()
    }

    fun loadLessons() {
        val monday = today.with(DayOfWeek.MONDAY).plusWeeks(weekOffset.toLong())
        val sunday = monday.plusDays(6)

        teacherRepository.getLessons(dateFrom = monday.toString(), dateTo = sunday.toString())
            .onEach { resource ->
                _uiState.value = when (resource) {
                    is Resource.Loading -> _uiState.value.copy(
                        isLoading = true,
                        lessons = resource.data ?: emptyList(),
                        weekMonday = monday,
                        isCurrentWeek = weekOffset == 0
                    )
                    is Resource.Success -> TeacherHomeUiState(
                        isLoading = false,
                        lessons = resource.data,
                        isOffline = false,
                        weekMonday = monday,
                        isCurrentWeek = weekOffset == 0
                    )
                    is Resource.Error -> TeacherHomeUiState(
                        isLoading = false,
                        lessons = resource.data ?: emptyList(),
                        error = if (resource.data.isNullOrEmpty()) {
                            resource.throwable.userMessage("Не удалось загрузить расписание. Попробуйте позже.")
                        } else null,
                        isOffline = resource.data?.isNotEmpty() == true,
                        weekMonday = monday,
                        isCurrentWeek = weekOffset == 0
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
