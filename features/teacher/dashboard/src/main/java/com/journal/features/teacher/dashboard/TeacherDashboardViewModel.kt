package com.journal.features.teacher.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.common.config.userFacingMessage
import com.journal.core.data.repository.TeacherDashboardData
import com.journal.core.data.repository.TeacherDashboardRepository
import com.journal.core.data.util.Resource
import com.journal.core.model.teacher.GrantJournalAccessRequest
import com.journal.core.model.teacher.JournalGridResponse
import com.journal.core.model.teacher.TeacherProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class TeacherDashboardScreenState(
    val isLoading: Boolean = false,
    val dashboard: TeacherDashboardData? = null,
    val error: String? = null,
    val isOffline: Boolean = false
)

data class TeacherDashboardJournalState(
    val isLoading: Boolean = false,
    val journal: JournalGridResponse? = null,
    val error: String? = null,
    val isOffline: Boolean = false
)

data class TeacherDashboardTeachersState(
    val isLoading: Boolean = false,
    val teachers: List<TeacherProfile> = emptyList(),
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class TeacherDashboardViewModel @Inject constructor(
    private val repository: TeacherDashboardRepository
) : ViewModel() {

    private var currentUserId: String? = null
    private var loadJob: Job? = null
    private var selectedJournalJob: Job? = null
    private var teachersJob: Job? = null

    private val _uiState = MutableStateFlow(TeacherDashboardScreenState(isLoading = true))
    val uiState: StateFlow<TeacherDashboardScreenState> = _uiState.asStateFlow()

    private val _selectedJournalState = MutableStateFlow(TeacherDashboardJournalState())
    val selectedJournalState: StateFlow<TeacherDashboardJournalState> = _selectedJournalState.asStateFlow()

    private val _teachersState = MutableStateFlow(TeacherDashboardTeachersState())
    val teachersState: StateFlow<TeacherDashboardTeachersState> = _teachersState.asStateFlow()

    fun load(userId: String?) {
        if (loadJob != null && currentUserId == userId) return
        currentUserId = userId
        loadJob?.cancel()
        loadJob = repository.getDashboard(userId)
            .onEach { resource ->
                _uiState.value = when (resource) {
                    is Resource.Loading -> _uiState.value.copy(
                        isLoading = true,
                        dashboard = resource.data,
                        error = null,
                        isOffline = false
                    )

                    is Resource.Success -> TeacherDashboardScreenState(
                        isLoading = false,
                        dashboard = resource.data
                    )

                    is Resource.Error -> {
                        val cached = resource.data
                        if (cached != null) {
                            TeacherDashboardScreenState(
                                isLoading = false,
                                dashboard = cached,
                                isOffline = true
                            )
                        } else {
                            TeacherDashboardScreenState(
                                isLoading = false,
                                error = resource.throwable.userFacingMessage(
                                    "Не удалось загрузить личный кабинет"
                                )
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadSelectedJournal(groupId: String, disciplineId: String, periodId: String) {
        selectedJournalJob?.cancel()
        _selectedJournalState.value = TeacherDashboardJournalState(isLoading = true)
        selectedJournalJob = repository.getSelectedJournal(
            groupId = groupId,
            disciplineId = disciplineId,
            periodId = periodId
        ).onEach { resource ->
            _selectedJournalState.value = when (resource) {
                is Resource.Loading -> {
                    val data = resource.data
                    TeacherDashboardJournalState(
                        isLoading = data?.journal == null,
                        journal = data?.journal,
                        isOffline = false
                    )
                }
                is Resource.Success -> TeacherDashboardJournalState(
                    isLoading = false,
                    journal = resource.data?.journal
                )
                is Resource.Error -> {
                    val cached = resource.data
                    if (cached?.journal != null) {
                        TeacherDashboardJournalState(
                            isLoading = false,
                            journal = cached.journal,
                            isOffline = true
                        )
                    } else {
                        TeacherDashboardJournalState(
                            isLoading = false,
                            error = resource.throwable.userFacingMessage("Не удалось загрузить данные журнала")
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun loadAccessTeachers() {
        teachersJob?.cancel()
        teachersJob = repository.getAccessTeachers()
            .onEach { resource ->
                _teachersState.value = when (resource) {
                    is Resource.Loading -> {
                        val data = resource.data
                        TeacherDashboardTeachersState(
                            isLoading = data == null && _teachersState.value.teachers.isEmpty(),
                            teachers = data?.teachers ?: _teachersState.value.teachers,
                            isOffline = false
                        )
                    }
                    is Resource.Success -> TeacherDashboardTeachersState(
                        isLoading = false,
                        teachers = resource.data?.teachers.orEmpty()
                    )
                    is Resource.Error -> {
                        val data = resource.data
                        val teachers = data?.teachers ?: _teachersState.value.teachers
                        if (teachers.isNotEmpty()) {
                            TeacherDashboardTeachersState(
                                isLoading = false,
                                teachers = teachers,
                                isOffline = true
                            )
                        } else {
                            TeacherDashboardTeachersState(
                                isLoading = false,
                                error = resource.throwable.userFacingMessage("Не удалось загрузить преподавателей")
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    suspend fun grantJournalAccess(request: GrantJournalAccessRequest) {
        repository.grantJournalAccess(request)
    }
}
