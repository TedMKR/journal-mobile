package com.journal.features.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.common.config.userFacingMessage
import com.journal.core.data.repository.AdminDashboardData
import com.journal.core.data.repository.AdminRepository
import com.journal.core.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class AdminDashboardScreenState(
    val isLoading: Boolean = false,
    val dashboard: AdminDashboardData? = null,
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private var currentUserId: String? = null
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(AdminDashboardScreenState(isLoading = true))
    val uiState: StateFlow<AdminDashboardScreenState> = _uiState.asStateFlow()

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

                    is Resource.Success -> AdminDashboardScreenState(
                        isLoading = false,
                        dashboard = resource.data
                    )

                    is Resource.Error -> {
                        val cached = resource.data
                        if (cached != null) {
                            AdminDashboardScreenState(
                                isLoading = false,
                                dashboard = cached,
                                isOffline = true
                            )
                        } else {
                            AdminDashboardScreenState(
                                isLoading = false,
                                error = resource.throwable.userFacingMessage(
                                    "Не удалось загрузить кабинет администратора"
                                )
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
