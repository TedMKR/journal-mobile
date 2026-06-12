package com.journal.features.teacher.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.common.config.userFacingMessage
import com.journal.core.data.repository.TeacherDashboardData
import com.journal.core.data.repository.TeacherDashboardRepository
import com.journal.core.data.util.Resource
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

@HiltViewModel
class TeacherDashboardViewModel @Inject constructor(
    private val repository: TeacherDashboardRepository
) : ViewModel() {

    private var currentUserId: String? = null
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(TeacherDashboardScreenState(isLoading = true))
    val uiState: StateFlow<TeacherDashboardScreenState> = _uiState.asStateFlow()

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
}
