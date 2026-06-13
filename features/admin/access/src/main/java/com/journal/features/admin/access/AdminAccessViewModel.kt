package com.journal.features.admin.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.data.repository.AdminRepository
import com.journal.core.data.util.Resource
import com.journal.core.data.util.userMessage
import com.journal.core.model.teacher.AdminAccessBinding
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class AdminAccessUiState(
    val bindings: List<AdminAccessBinding> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class AdminAccessViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminAccessUiState(isLoading = true))
    val uiState: StateFlow<AdminAccessUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadBindings(activeOnly: Boolean) {
        loadJob?.cancel()
        loadJob = repository.getAccessBindings(activeOnly = activeOnly)
            .onEach { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> {
                            val data = resource.data
                            state.copy(
                                isLoading = data == null && state.bindings.isEmpty(),
                                error = null,
                                bindings = data?.bindings ?: state.bindings
                            )
                        }
                        is Resource.Success -> {
                            val data = resource.data
                            state.copy(
                                isLoading = false,
                                error = null,
                                isOffline = false,
                                bindings = data?.bindings.orEmpty()
                            )
                        }
                        is Resource.Error -> {
                            val data = resource.data
                            val hasData = data != null || state.bindings.isNotEmpty()
                            state.copy(
                                isLoading = false,
                                error = if (hasData) null else resource.throwable.userMessage("Не удалось загрузить доступы"),
                                isOffline = hasData,
                                bindings = data?.bindings ?: state.bindings
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
