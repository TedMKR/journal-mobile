package com.journal.features.methodist.journals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.data.repository.MethodistRepository
import com.journal.core.data.util.Resource
import com.journal.core.data.util.userMessage
import com.journal.core.model.teacher.AcademicGroup
import com.journal.core.model.teacher.AcademicPeriod
import com.journal.core.model.teacher.Discipline
import com.journal.core.model.teacher.JournalContext
import com.journal.core.model.teacher.TeacherProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class MethodistJournalsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false,
    val journals: List<JournalContext> = emptyList(),
    val periods: List<AcademicPeriod> = emptyList(),
    val disciplines: List<Discipline> = emptyList(),
    val groups: List<AcademicGroup> = emptyList(),
    val teachers: List<TeacherProfile> = emptyList()
)

@HiltViewModel
class MethodistJournalsViewModel @Inject constructor(
    private val methodistRepository: MethodistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MethodistJournalsUiState(isLoading = true))
    val uiState: StateFlow<MethodistJournalsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadJournals(
        periodId: String,
        disciplineId: String,
        groupId: String,
        teacherId: String,
        lessonType: String,
        query: String
    ) {
        loadJob?.cancel()
        loadJob = methodistRepository.getJournals(
            periodId = periodId.ifBlank { null },
            disciplineId = disciplineId.ifBlank { null },
            groupId = groupId.ifBlank { null },
            teacherId = teacherId.ifBlank { null },
            lessonType = lessonType.ifBlank { null },
            query = query.trim().ifBlank { null }
        ).onEach { resource ->
            _uiState.update { state ->
                when (resource) {
                    is Resource.Loading -> {
                        val data = resource.data
                        state.copy(
                            isLoading = data == null && state.journals.isEmpty(),
                            error = null,
                            journals = data?.journals ?: state.journals,
                            periods = data?.periods ?: state.periods,
                            disciplines = data?.disciplines ?: state.disciplines,
                            groups = data?.groups ?: state.groups,
                            teachers = data?.teachers ?: state.teachers
                        )
                    }
                    is Resource.Success -> {
                        val data = resource.data
                        state.copy(
                            isLoading = false,
                            error = null,
                            isOffline = false,
                            journals = data?.journals.orEmpty(),
                            periods = data?.periods.orEmpty(),
                            disciplines = data?.disciplines.orEmpty(),
                            groups = data?.groups.orEmpty(),
                            teachers = data?.teachers.orEmpty()
                        )
                    }
                    is Resource.Error -> {
                        val data = resource.data
                        val hasData = data != null || state.journals.isNotEmpty()
                        state.copy(
                            isLoading = false,
                            error = if (hasData) null else resource.throwable.userMessage("Не удалось загрузить журналы"),
                            isOffline = hasData,
                            journals = data?.journals ?: state.journals,
                            periods = data?.periods ?: state.periods,
                            disciplines = data?.disciplines ?: state.disciplines,
                            groups = data?.groups ?: state.groups,
                            teachers = data?.teachers ?: state.teachers
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }
}
