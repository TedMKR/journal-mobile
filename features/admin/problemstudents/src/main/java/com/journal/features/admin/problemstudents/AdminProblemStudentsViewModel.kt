package com.journal.features.admin.problemstudents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.data.repository.AdminRepository
import com.journal.core.data.util.Resource
import com.journal.core.data.util.userMessage
import com.journal.core.model.teacher.AcademicGroup
import com.journal.core.model.teacher.AdminPeriod
import com.journal.core.model.teacher.Discipline
import com.journal.core.model.teacher.ProblemStudentEntry
import com.journal.core.model.teacher.ProblemStudentsMeta
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class AdminProblemStudentsUiState(
    val students: List<ProblemStudentEntry> = emptyList(),
    val meta: ProblemStudentsMeta? = null,
    val periods: List<AdminPeriod> = emptyList(),
    val groups: List<AcademicGroup> = emptyList(),
    val disciplines: List<Discipline> = emptyList(),
    val total: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class AdminProblemStudentsViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminProblemStudentsUiState(isLoading = true))
    val uiState: StateFlow<AdminProblemStudentsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadStudents(
        periodId: String,
        groupId: String,
        disciplineId: String,
        minGrades: String,
        minFailingGrades: String,
        failingPercentThreshold: String,
        limit: Int,
        offset: Int
    ) {
        loadJob?.cancel()
        loadJob = repository.getProblemStudents(
            periodId = periodId.ifBlank { null },
            groupId = groupId.ifBlank { null },
            disciplineId = disciplineId.ifBlank { null },
            minGrades = minGrades.toIntOrNull(),
            minFailingGrades = minFailingGrades.toIntOrNull(),
            failingPercentThreshold = failingPercentThreshold.toIntOrNull(),
            limit = limit,
            offset = offset
        ).onEach { resource ->
            _uiState.update { state ->
                when (resource) {
                    is Resource.Loading -> {
                        val data = resource.data
                        state.copy(
                            isLoading = data == null && state.students.isEmpty(),
                            error = null,
                            students = data?.students ?: state.students,
                            meta = data?.meta ?: state.meta,
                            periods = data?.periods ?: state.periods,
                            groups = data?.groups ?: state.groups,
                            disciplines = data?.disciplines ?: state.disciplines,
                            total = data?.meta?.total ?: state.total
                        )
                    }
                    is Resource.Success -> {
                        val data = resource.data
                        state.copy(
                            isLoading = false,
                            error = null,
                            isOffline = false,
                            students = data?.students.orEmpty(),
                            meta = data?.meta,
                            periods = data?.periods.orEmpty(),
                            groups = data?.groups.orEmpty(),
                            disciplines = data?.disciplines.orEmpty(),
                            total = data?.meta?.total ?: 0
                        )
                    }
                    is Resource.Error -> {
                        val data = resource.data
                        val hasData = data != null || state.students.isNotEmpty()
                        state.copy(
                            isLoading = false,
                            error = if (hasData) null else resource.throwable.userMessage("Не удалось загрузить данные"),
                            isOffline = hasData,
                            students = data?.students ?: state.students,
                            meta = data?.meta ?: state.meta,
                            periods = data?.periods ?: state.periods,
                            groups = data?.groups ?: state.groups,
                            disciplines = data?.disciplines ?: state.disciplines,
                            total = data?.meta?.total ?: state.total
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }
}
