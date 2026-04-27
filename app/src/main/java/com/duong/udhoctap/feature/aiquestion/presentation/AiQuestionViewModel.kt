package com.duong.udhoctap.feature.aiquestion.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.data.repository.AiQuestionRepository
import com.duong.udhoctap.core.data.repository.DeckRepository
import com.duong.udhoctap.core.data.repository.FlashcardRepository
import com.duong.udhoctap.core.database.entity.FlashcardEntity
import com.duong.udhoctap.core.network.dto.GeneratedQuestion
import com.duong.udhoctap.core.network.dto.QuestionEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiQuestionUiState(
    val topic: String = "",
    val count: Int = 5,
    val difficulty: String = "medium",
    val questionType: String = "",
    val questions: List<GeneratedQuestion> = emptyList(),
    val isGenerating: Boolean = false,
    val isComplete: Boolean = false,
    val statusMessage: String = "",
    val generatedCount: Int = 0,
    val error: String? = null,
    val savedIds: Set<String> = emptySet(),
    val allSaved: Boolean = false
)

@HiltViewModel
class AiQuestionViewModel @Inject constructor(
    private val questionRepository: AiQuestionRepository,
    private val flashcardRepository: FlashcardRepository,
    private val deckRepository: DeckRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiQuestionUiState())
    val uiState: StateFlow<AiQuestionUiState> = _uiState.asStateFlow()

    private var generateJob: Job? = null

    fun setTopic(t: String) = _uiState.update { it.copy(topic = t) }
    fun setCount(c: Int) = _uiState.update { it.copy(count = c.coerceIn(1, 20)) }
    fun setDifficulty(d: String) = _uiState.update { it.copy(difficulty = d) }
    fun setQuestionType(t: String) = _uiState.update { it.copy(questionType = t) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun generate() {
        val topic = _uiState.value.topic.trim()
        if (topic.isBlank() || _uiState.value.isGenerating) return
        generateJob?.cancel()

        _uiState.update {
            it.copy(
                isGenerating = true, isComplete = false,
                questions = emptyList(), generatedCount = 0,
                statusMessage = "Đang kết nối…", error = null,
                savedIds = emptySet(), allSaved = false
            )
        }

        generateJob = viewModelScope.launch {
            val state = _uiState.value
            questionRepository.generate(
                topic = topic, count = state.count,
                difficulty = state.difficulty, questionType = state.questionType
            ).collect { event ->
                when (event) {
                    is QuestionEvent.Status -> _uiState.update { it.copy(statusMessage = event.content) }
                    is QuestionEvent.Log -> { /* suppress logs */ }
                    is QuestionEvent.QuestionGenerated -> _uiState.update { s ->
                        val newQuestions = s.questions + event.question
                        s.copy(questions = newQuestions, generatedCount = newQuestions.size, statusMessage = "Đang tạo…")
                    }
                    is QuestionEvent.BatchSummary -> _uiState.update {
                        it.copy(statusMessage = "${event.completed} câu hỏi, ${event.failed} lỗi")
                    }
                    is QuestionEvent.Complete -> _uiState.update {
                        it.copy(isGenerating = false, isComplete = true, statusMessage = "Hoàn thành!")
                    }
                    is QuestionEvent.AppError -> _uiState.update {
                        it.copy(isGenerating = false, error = event.message, statusMessage = "Lỗi")
                    }
                    else -> Unit
                }
            }
        }
    }

    fun updateQuestion(updated: GeneratedQuestion) {
        _uiState.update { state ->
            state.copy(questions = state.questions.map { if (it.id == updated.id) updated else it })
        }
    }

    fun saveAsFlashcard(question: GeneratedQuestion, deckId: Long) {
        viewModelScope.launch {
            val front = question.question
            val back = buildString {
                append(question.answer)
                if (question.explanation.isNotBlank()) { append("\n\n---\n"); append(question.explanation) }
                if (question.options.isNotEmpty()) { append("\n\nOptions: "); append(question.options.joinToString(", ")) }
            }
            flashcardRepository.insertFlashcard(FlashcardEntity(deckId = deckId, front = front, back = back, isDraft = false))
            _uiState.update { it.copy(savedIds = it.savedIds + question.id) }
        }
    }

    fun saveAllAsFlashcards(deckId: Long) {
        viewModelScope.launch {
            val questions = _uiState.value.questions
            questions.forEach { question ->
                val front = question.question
                val back = buildString {
                    append(question.answer)
                    if (question.explanation.isNotBlank()) { append("\n\n---\n"); append(question.explanation) }
                    if (question.options.isNotEmpty()) { append("\n\nOptions: "); append(question.options.joinToString(", ")) }
                }
                flashcardRepository.insertFlashcard(FlashcardEntity(deckId = deckId, front = front, back = back, isDraft = false))
            }
            _uiState.update { it.copy(savedIds = questions.map { it.id }.toSet(), allSaved = true) }
        }
    }

    fun reset() {
        generateJob?.cancel()
        _uiState.value = AiQuestionUiState()
    }

    override fun onCleared() {
        super.onCleared()
        generateJob?.cancel()
    }
}
