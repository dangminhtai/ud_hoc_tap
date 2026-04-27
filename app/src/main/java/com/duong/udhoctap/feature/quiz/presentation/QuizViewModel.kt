package com.duong.udhoctap.feature.quiz.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.data.repository.FlashcardRepository
import com.duong.udhoctap.core.data.repository.ReviewRepository
import com.duong.udhoctap.core.database.entity.ReviewLogEntity
import com.duong.udhoctap.core.network.BackendApiService
import com.duong.udhoctap.core.network.dto.KbQuizRequest
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
    val questionType: String = "flashcard", // "flashcard", "essay", "multiple_choice"
    val correctAnswer: String = "",
    val explanation: String = "",
    // Multiple choice only
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1
) {
    val isEssay: Boolean get() = questionType == "essay"
    val isMultipleChoice: Boolean get() = questionType == "multiple_choice" || options.isNotEmpty()
}

data class QuizUiState(
    val phase: QuizPhase = QuizPhase.LOADING,
    val totalAvailableCards: Int = 0,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val totalQuestions: Int = 0,
    val correctCount: Int = 0,
    // Multiple choice state
    val selectedAnswer: Int? = null,
    val hasAnswered: Boolean = false,
    // Essay state
    val essayInput: String = "",
    val showEssayAnswer: Boolean = false
)

@HiltViewModel
class QuizViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val flashcardRepository: FlashcardRepository,
    private val reviewRepository: ReviewRepository,
    private val backendApi: BackendApiService
) : ViewModel() {

    private val deckId: Long = savedStateHandle.get<Long>("deckId") ?: 0L
    private val kbName: String = savedStateHandle.get<String>("kbName") ?: ""
    private val kbTopic: String = savedStateHandle.get<String>("topic") ?: ""
    private val kbCount: Int = savedStateHandle.get<Int>("count") ?: 5
    private val kbDifficulty: String = savedStateHandle.get<String>("difficulty") ?: "medium"

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        if (kbName.isNotBlank()) startKbQuiz() else startQuizImmediately()
    }

    private fun startKbQuiz() {
        viewModelScope.launch {
            try {
                val response = backendApi.generateQuizFromKb(
                    KbQuizRequest(
                        kbName = kbName,
                        topic = kbTopic,
                        count = kbCount,
                        difficulty = kbDifficulty,
                        questionType = "mixed"
                    )
                )
                val questions = response.questions.map { q ->
                    QuizQuestion(
                        flashcardId = -1L,
                        questionText = q.question,
                        questionType = q.questionType,
                        correctAnswer = q.correctAnswer,
                        explanation = q.explanation,
                        options = q.options,
                        correctIndex = q.correctIndex
                    )
                }
                _uiState.value = _uiState.value.copy(
                    phase = QuizPhase.PLAYING,
                    questions = questions,
                    currentIndex = 0,
                    totalQuestions = questions.size,
                    correctCount = 0,
                    selectedAnswer = null,
                    hasAnswered = false,
                    essayInput = "",
                    showEssayAnswer = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    phase = QuizPhase.PLAYING,
                    questions = emptyList()
                )
            }
        }
    }

    private fun startQuizImmediately() {
        viewModelScope.launch {
            val allCards = flashcardRepository.getAllCardsForQuiz(deckId)
            _uiState.value = _uiState.value.copy(totalAvailableCards = allCards.size)

            val hasEnoughForMultipleChoice = allCards.size >= 4
            val questions = allCards.shuffled().map { card ->
                when {
                    card.questionType == "essay" -> QuizQuestion(
                        flashcardId = card.id,
                        questionText = card.front,
                        questionType = "essay",
                        correctAnswer = card.back,
                        explanation = card.explanation
                    )
                    hasEnoughForMultipleChoice -> {
                        val wrongAnswers = allCards
                            .filter { it.id != card.id }
                            .shuffled()
                            .take(3)
                            .map { it.back }
                        val allOptions = (wrongAnswers + card.back).shuffled()
                        QuizQuestion(
                            flashcardId = card.id,
                            questionText = card.front,
                            questionType = "multiple_choice",
                            correctAnswer = card.back,
                            explanation = card.explanation,
                            options = allOptions,
                            correctIndex = allOptions.indexOf(card.back)
                        )
                    }
                    else -> QuizQuestion(
                        flashcardId = card.id,
                        questionText = card.front,
                        questionType = "flashcard",
                        correctAnswer = card.back,
                        explanation = card.explanation
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                phase = QuizPhase.PLAYING,
                questions = questions,
                currentIndex = 0,
                totalQuestions = questions.size,
                correctCount = 0,
                selectedAnswer = null,
                hasAnswered = false,
                essayInput = "",
                showEssayAnswer = false
            )
        }
    }

    fun restartQuizFull() {
        _uiState.value = QuizUiState()
        if (kbName.isNotBlank()) startKbQuiz() else startQuizImmediately()
    }

    // Multiple choice
    fun selectAnswer(index: Int) {
        val state = _uiState.value
        if (state.hasAnswered) return
        val question = state.questions[state.currentIndex]
        val isCorrect = index == question.correctIndex
        viewModelScope.launch {
            if (question.flashcardId > 0) {
                reviewRepository.insertReviewLog(
                    ReviewLogEntity(flashcardId = question.flashcardId, rating = if (isCorrect) 3 else 1)
                )
            }
        }
        _uiState.value = state.copy(
            selectedAnswer = index,
            hasAnswered = true,
            correctCount = state.correctCount + if (isCorrect) 1 else 0
        )
    }

    // Essay / Flashcard
    fun updateEssayInput(text: String) {
        _uiState.update { it.copy(essayInput = text) }
    }

    fun showEssayAnswer() {
        _uiState.update { it.copy(showEssayAnswer = true) }
    }

    fun submitEssayResult(isCorrect: Boolean) {
        val state = _uiState.value
        if (state.hasAnswered) return
        val question = state.questions[state.currentIndex]
        viewModelScope.launch {
            if (question.flashcardId > 0) {
                reviewRepository.insertReviewLog(
                    ReviewLogEntity(flashcardId = question.flashcardId, rating = if (isCorrect) 3 else 1)
                )
            }
        }
        _uiState.update {
            it.copy(
                hasAnswered = true,
                correctCount = it.correctCount + if (isCorrect) 1 else 0
            )
        }
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
                hasAnswered = false,
                essayInput = "",
                showEssayAnswer = false
            )
        }
    }

    private fun MutableStateFlow<QuizUiState>.update(transform: (QuizUiState) -> QuizUiState) {
        value = transform(value)
    }
}
