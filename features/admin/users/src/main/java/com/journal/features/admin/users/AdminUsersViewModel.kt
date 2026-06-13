package com.journal.features.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.data.repository.AdminRepository
import com.journal.core.data.util.Resource
import com.journal.core.data.util.userMessage
import com.journal.core.model.teacher.AdminUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class AdminUsersUiState(
    val users: List<AdminUser> = emptyList(),
    val total: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class AdminUsersViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUsersUiState(isLoading = true))
    val uiState: StateFlow<AdminUsersUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadUsers(
        page: Int,
        pageSize: Int,
        query: String,
        userType: String,
        status: String
    ) {
        loadJob?.cancel()
        loadJob = adminRepository.getUsers(
            page = page,
            pageSize = pageSize,
            query = query.trim().ifBlank { null },
            userType = userType.ifBlank { null },
            status = status.ifBlank { null }
        ).onEach { resource ->
            _uiState.update { state ->
                when (resource) {
                    is Resource.Loading -> {
                        val data = resource.data
                        state.copy(
                            isLoading = data == null && state.users.isEmpty(),
                            error = null,
                            users = data?.users ?: state.users,
                            total = data?.total ?: state.total
                        )
                    }
                    is Resource.Success -> {
                        val data = resource.data
                        state.copy(
                            isLoading = false,
                            error = null,
                            isOffline = false,
                            users = data?.users.orEmpty(),
                            total = data?.total ?: 0
                        )
                    }
                    is Resource.Error -> {
                        val data = resource.data
                        val hasData = data != null || state.users.isNotEmpty()
                        state.copy(
                            isLoading = false,
                            error = if (hasData) null else resource.throwable.userMessage("Не удалось загрузить пользователей"),
                            isOffline = hasData,
                            users = data?.users ?: state.users,
                            total = data?.total ?: state.total
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }
}
