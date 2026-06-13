package com.journal.features.admin.periods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.data.repository.AdminRepository
import com.journal.core.data.util.Resource
import com.journal.core.data.util.userMessage
import com.journal.core.model.teacher.AdminPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class AdminPeriodsUiState(
    val periods: List<AdminPeriod> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class AdminPeriodsViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPeriodsUiState(isLoading = true))
    val uiState: StateFlow<AdminPeriodsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadPeriods() {
        loadJob?.cancel()
        loadJob = repository.getPeriods(includeClosed = true)
            .onEach { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> {
                            val data = resource.data
                            state.copy(
                                isLoading = data == null && state.periods.isEmpty(),
                                error = null,
                                periods = data?.periods ?: state.periods
                            )
                        }
                        is Resource.Success -> {
                            val data = resource.data
                            state.copy(
                                isLoading = false,
                                error = null,
                                isOffline = false,
                                periods = data?.periods.orEmpty()
                            )
                        }
                        is Resource.Error -> {
                            val data = resource.data
                            val hasData = data != null || state.periods.isNotEmpty()
                            state.copy(
                                isLoading = false,
                                error = if (hasData) null else resource.throwable.userMessage("Не удалось загрузить периоды"),
                                isOffline = hasData,
                                periods = data?.periods ?: state.periods
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
