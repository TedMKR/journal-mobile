package com.journal.features.teacher.studentcard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.journal.core.data.repository.JournalRepository
import com.journal.core.data.util.Resource
import com.journal.core.data.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class TeacherStudentCardScreenState(
    val isLoading: Boolean = false,
    val card: StudentCardUiState? = null,
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class TeacherStudentCardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])
    val disciplineId: String = checkNotNull(savedStateHandle["disciplineId"])
    val periodId: String = checkNotNull(savedStateHandle["periodId"])
    val lessonType: String = checkNotNull(savedStateHandle["lessonType"])
    private val studentId: String = checkNotNull(savedStateHandle["studentId"])

    private val _uiState = MutableStateFlow(TeacherStudentCardScreenState(isLoading = true))
    val uiState: StateFlow<TeacherStudentCardScreenState> = _uiState.asStateFlow()

    init {
        loadCard()
    }

    fun loadCard() {
        journalRepository.getJournalGrid(
            groupId = groupId,
            disciplineId = disciplineId,
            periodId = periodId,
            lessonType = lessonType
        ).onEach { resource ->
            _uiState.update { state ->
                when (resource) {
                    is Resource.Loading -> {
                        val cachedCard = resource.data?.let { buildStudentCard(it, studentId) }
                        state.copy(
                            isLoading = cachedCard == null && state.card == null,
                            card = cachedCard ?: state.card,
                            error = null
                        )
                    }
                    is Resource.Success -> {
                        val card = resource.data?.let { buildStudentCard(it, studentId) }
                        state.copy(
                            isLoading = false,
                            card = card,
                            error = if (card == null) "Студент не найден в сохраненном журнале" else null,
                            isOffline = false
                        )
                    }
                    is Resource.Error -> {
                        val cachedCard = resource.data?.let { buildStudentCard(it, studentId) } ?: state.card
                        state.copy(
                            isLoading = false,
                            card = cachedCard,
                            error = if (cachedCard == null) {
                                resource.throwable.userMessage("Не удалось загрузить карточку студента")
                            } else null,
                            isOffline = cachedCard != null
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }
}
