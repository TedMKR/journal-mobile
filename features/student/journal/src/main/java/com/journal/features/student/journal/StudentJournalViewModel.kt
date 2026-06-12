package com.journal.features.student.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.common.config.userFacingMessage
import com.journal.core.data.repository.StudentRepository
import com.journal.core.data.util.Resource
import com.journal.core.model.teacher.StudentSubjectCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class StudentJournalUiState(
    val isLoading: Boolean = false,
    val data: StudentSubjectCard? = null,
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class StudentJournalViewModel @Inject constructor(
    private val studentRepository: StudentRepository
) : ViewModel() {

    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(StudentJournalUiState(isLoading = true))
    val uiState: StateFlow<StudentJournalUiState> = _uiState.asStateFlow()

    fun load(disciplineId: String, periodId: String, groupId: String) {
        loadJob?.cancel()
        loadJob = studentRepository.getSubjectCard(disciplineId, periodId, groupId)
            .onEach { resource ->
                _uiState.value = when (resource) {
                    is Resource.Loading -> _uiState.value.copy(
                        isLoading = true,
                        data = resource.data,
                        error = null
                    )

                    is Resource.Success -> StudentJournalUiState(
                        isLoading = false,
                        data = resource.data
                    )

                    is Resource.Error -> {
                        val cached = resource.data
                        StudentJournalUiState(
                            isLoading = false,
                            data = cached,
                            isOffline = cached != null,
                            error = if (cached == null) resource.throwable.studentJournalMessage() else null
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}

private fun Throwable.studentJournalMessage(): String =
    if (httpCodeOrNull() == 403) {
        "Нет журнала для этого занятия"
    } else {
        userFacingMessage("Не удалось загрузить журнал")
    }

private fun Throwable.httpCodeOrNull(): Int? =
    runCatching {
        javaClass.methods
            .firstOrNull { it.name == "code" && it.parameterCount == 0 }
            ?.invoke(this) as? Int
    }.getOrNull()
