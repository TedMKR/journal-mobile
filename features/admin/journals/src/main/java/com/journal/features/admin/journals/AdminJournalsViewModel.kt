package com.journal.features.admin.journals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.data.repository.AdminRepository
import com.journal.core.data.util.Resource
import com.journal.core.data.util.userMessage
import com.journal.core.model.teacher.AdminJournalContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class AdminJournalsUiState(
    val journals: List<AdminJournalContext> = emptyList(),
    val total: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class AdminJournalsViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminJournalsUiState(isLoading = true))
    val uiState: StateFlow<AdminJournalsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadJournals(page: Int, pageSize: Int, status: String) {
        loadJob?.cancel()
        loadJob = repository.getJournals(
            page = page,
            pageSize = pageSize,
            status = status.ifBlank { null }
        ).onEach { resource ->
            _uiState.update { state ->
                when (resource) {
                    is Resource.Loading -> {
                        val data = resource.data
                        state.copy(
                            isLoading = data == null && state.journals.isEmpty(),
                            error = null,
                            journals = data?.journals ?: state.journals,
                            total = data?.total ?: state.total
                        )
                    }
                    is Resource.Success -> {
                        val data = resource.data
                        state.copy(
                            isLoading = false,
                            error = null,
                            isOffline = false,
                            journals = data?.journals.orEmpty(),
                            total = data?.total ?: 0
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
                            total = data?.total ?: state.total
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }
}
