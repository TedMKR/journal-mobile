package com.journal.features.teacher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.model.teacher.TeacherLesson
import com.journal.core.network.api.JournalApi
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherHomeUiState(
    val isLoading: Boolean = false,
    val lessons: List<TeacherLesson> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val journalApi: JournalApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherHomeUiState(isLoading = true))
    val uiState: StateFlow<TeacherHomeUiState> = _uiState.asStateFlow()

    init {
        loadLessons()
    }

    fun loadLessons() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val (dateFrom, dateTo) = currentWeekRange()
            runCatching {
                journalApi.getLessons(
                    dateFrom = dateFrom.toString(),
                    dateTo = dateTo.toString(),
                    limit = 200
                )
            }.onSuccess { response ->
                _uiState.value = TeacherHomeUiState(
                    isLoading = false,
                    lessons = response.lessons
                )
            }.onFailure { throwable ->
                _uiState.value = TeacherHomeUiState(
                    isLoading = false,
                    error = throwable.message ?: "Не удалось загрузить расписание. Попробуйте позже."
                )
            }
        }
    }

    private fun currentWeekRange(): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now()
        val monday = now.minusDays((now.dayOfWeek.value - 1).toLong())
        return monday to monday.plusDays(6)
    }
}
