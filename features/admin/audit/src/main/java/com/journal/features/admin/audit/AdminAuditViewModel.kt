package com.journal.features.admin.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.data.repository.AdminRepository
import com.journal.core.data.util.Resource
import com.journal.core.data.util.userMessage
import com.journal.core.model.teacher.AuditEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class AdminAuditUiState(
    val events: List<AuditEvent> = emptyList(),
    val total: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class AdminAuditViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminAuditUiState(isLoading = true))
    val uiState: StateFlow<AdminAuditUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadAudit(
        page: Int,
        pageSize: Int,
        action: String,
        entityType: String
    ) {
        loadJob?.cancel()
        loadJob = repository.getAudit(
            page = page,
            pageSize = pageSize,
            action = action.ifBlank { null },
            entityType = entityType.ifBlank { null }
        ).onEach { resource ->
            _uiState.update { state ->
                when (resource) {
                    is Resource.Loading -> {
                        val data = resource.data
                        state.copy(
                            isLoading = data == null && state.events.isEmpty(),
                            error = null,
                            events = data?.events ?: state.events,
                            total = data?.total ?: state.total
                        )
                    }
                    is Resource.Success -> {
                        val data = resource.data
                        state.copy(
                            isLoading = false,
                            error = null,
                            isOffline = false,
                            events = data?.events.orEmpty(),
                            total = data?.total ?: 0
                        )
                    }
                    is Resource.Error -> {
                        val data = resource.data
                        val hasData = data != null || state.events.isNotEmpty()
                        state.copy(
                            isLoading = false,
                            error = if (hasData) null else resource.throwable.userMessage("Не удалось загрузить аудит"),
                            isOffline = hasData,
                            events = data?.events ?: state.events,
                            total = data?.total ?: state.total
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }
}
