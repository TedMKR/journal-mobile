package com.journal.features.teacher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.data.repository.TeacherRepository
import com.journal.core.data.util.Resource
import com.journal.core.model.teacher.TeacherLesson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate
import javax.inject.Inject

data class TeacherHomeUiState(
    val isLoading: Boolean = false,
    val lessons: List<TeacherLesson> = emptyList(),
    val error: String? = null,
    /** True if the data is from cache and network is unavailable */
    val isOffline: Boolean = false
)

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherHomeUiState(isLoading = true))
    val uiState: StateFlow<TeacherHomeUiState> = _uiState.asStateFlow()

    init {
        loadLessons()
    }

    fun loadLessons() {
        val (dateFrom, dateTo) = currentWeekRange()

        teacherRepository.getLessons(dateFrom = dateFrom, dateTo = dateTo)
            .onEach { resource ->
                _uiState.value = when (resource) {
                    is Resource.Loading -> TeacherHomeUiState(
                        isLoading = true,
                        lessons = resource.data ?: emptyList()
                    )
                    is Resource.Success -> TeacherHomeUiState(
                        isLoading = false,
                        lessons = resource.data,
                        isOffline = false
                    )
                    is Resource.Error -> TeacherHomeUiState(
                        isLoading = false,
                        lessons = resource.data ?: emptyList(),
                        error = if (resource.data.isNullOrEmpty()) {
                            resource.throwable.message
                                ?: "Не удалось загрузить расписание. Попробуйте позже."
                        } else null,
                        isOffline = resource.data?.isNotEmpty() == true
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun currentWeekRange(): Pair<String, String> {
        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val sunday = monday.plusDays(6)
        return monday.toString() to sunday.toString()
    }
}
