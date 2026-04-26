package com.duong.udhoctap.feature.quiz.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.data.repository.FlashcardRepository
import com.duong.udhoctap.core.data.repository.ReviewRepository
import com.duong.udhoctap.core.database.entity.FlashcardEntity
import com.duong.udhoctap.core.database.entity.ReviewLogEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class QuizPhase { LOADING, PLAYING, COMPLETE }


data class QuizQuestion(
    val flashcardId: Long,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int
)

data class QuizUiState(
    val phase: QuizPhase = QuizPhase.LOADING,
    val totalAvailableCards: Int = 0,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val totalQuestions: Int = 0,
    val correctCount: Int = 0,
    val selectedAnswer: Int? = null,
    val hasAnswered: Boolean = false
)

@HiltViewModel
class QuizViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val flashcardRepository: FlashcardRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val deckId: Long = savedStateHandle.get<Long>("deckId") ?: 0L

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        startQuizImmediately()
    }

    private fun startQuizImmediately() {
        viewModelScope.launch {
            val allCards = flashcardRepository.getAllCardsForQuiz(deckId)
            _uiState.value = _uiState.value.copy(
                totalAvailableCards = allCards.size
            )
            if (allCards.size < 4) {
                _uiState.value = _uiState.value.copy(phase = QuizPhase.PLAYING)
                return@launch
            }
            val questions = allCards.shuffled().map { card ->
                val wrongAnswers = allCards
                    .filter { it.id != card.id }
                    .shuffled()
                    .take(3)
                    .map { it.back }
                val allOptions = (wrongAnswers + card.back).shuffled()
                QuizQuestion(
                    flashcardId = card.id,
                    questionText = card.front,
                    options = allOptions,
                    correctIndex = allOptions.indexOf(card.back)
                )
            }
            _uiState.value = _uiState.value.copy(
                phase = QuizPhase.PLAYING,
                questions = questions,
                currentIndex = 0,
                totalQuestions = questions.size,
                correctCount = 0,
                selectedAnswer = null,
                hasAnswered = false
            )
        }
    }

    fun restartQuizFull() {
        _uiState.value = QuizUiState()
        startQuizImmediately()
    }

    fun selectAnswer(index: Int) {
        val state = _uiState.value
        if (state.hasAnswered) return
        val isCorrect = index == state.questions[state.currentIndex].correctIndex
        viewModelScope.launch {
            reviewRepository.insertReviewLog(
                ReviewLogEntity(
                    flashcardId = state.questions[state.currentIndex].flashcardId,
                    rating = if (isCorrect) 3 else 1
                )
            )
        }
        _uiState.value = state.copy(
            selectedAnswer = index,
            hasAnswered = true,
            correctCount = state.correctCount + if (isCorrect) 1 else 0
        )
    }

    fun nextQuestion() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.totalQuestions) {
            _uiState.value = state.copy(phase = QuizPhase.COMPLETE)
        } else {
            _uiState.value = state.copy(
                currentIndex = nextIndex,
                selectedAnswer = null,
                hasAnswered = false
            )
        }
    }

}
