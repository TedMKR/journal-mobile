package com.journal.features.methodist.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.common.config.userFacingMessage
import com.journal.core.data.repository.MethodistDashboardData
import com.journal.core.data.repository.MethodistRepository
import com.journal.core.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class MethodistDashboardScreenState(
    val isLoading: Boolean = false,
    val dashboard: MethodistDashboardData? = null,
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class MethodistDashboardViewModel @Inject constructor(
    private val repository: MethodistRepository
) : ViewModel() {

    private var currentUserId: String? = null
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(MethodistDashboardScreenState(isLoading = true))
    val uiState: StateFlow<MethodistDashboardScreenState> = _uiState.asStateFlow()

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

                    is Resource.Success -> MethodistDashboardScreenState(
                        isLoading = false,
                        dashboard = resource.data
                    )

                    is Resource.Error -> {
                        val cached = resource.data
                        if (cached != null) {
                            MethodistDashboardScreenState(
                                isLoading = false,
                                dashboard = cached,
                                isOffline = true
                            )
                        } else {
                            MethodistDashboardScreenState(
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
