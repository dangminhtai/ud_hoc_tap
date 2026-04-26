package com.duong.udhoctap.feature.review.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.data.repository.FlashcardRepository
import com.duong.udhoctap.core.data.repository.ReviewRepository
import com.duong.udhoctap.core.database.entity.ReviewLogEntity
import com.duong.udhoctap.core.network.DocumentApi
import com.duong.udhoctap.core.network.ExplainCardRequest
import com.duong.udhoctap.core.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewCardUi(
    val id: Long,
    val front: String,
    val back: String
)

data class ReviewUiState(
    val cards: List<ReviewCardUi> = emptyList(),
    val currentIndex: Int = 0,
    val totalCards: Int = 0,
    val isComplete: Boolean = false,
    val isLoading: Boolean = true,
    val autoRead: Boolean = false,
    val showExplainSheet: Boolean = false,
    val isExplaining: Boolean = false,
    val explanation: String? = null
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val flashcardRepository: FlashcardRepository,
    private val reviewRepository: ReviewRepository,
    private val ttsManager: TtsManager,
    private val documentApi: DocumentApi
) : ViewModel() {

    private val deckId: Long = savedStateHandle.get<Long>("deckId") ?: 0L

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        loadCards()
    }

    private fun loadCards() {
        viewModelScope.launch {
            val cards = flashcardRepository.getAllCardsForQuiz(deckId)
            val reviewCards = cards.shuffled().map { card ->
                ReviewCardUi(id = card.id, front = card.front, back = card.back)
            }
            _uiState.value = ReviewUiState(
                cards = reviewCards,
                totalCards = reviewCards.size,
                isLoading = false,
                isComplete = reviewCards.isEmpty()
            )
        }
    }

    fun speakCurrentCard(speakBack: Boolean = false) {
        val state = _uiState.value
        if (state.cards.isEmpty() || state.currentIndex >= state.cards.size) return
        val card = state.cards[state.currentIndex]
        ttsManager.speak(if (speakBack) card.back else card.front)
    }

    fun toggleAutoRead() {
        _uiState.value = _uiState.value.copy(autoRead = !_uiState.value.autoRead)
    }

    fun explainCurrentCard() {
        val state = _uiState.value
        if (state.cards.isEmpty() || state.currentIndex >= state.cards.size) return
        val card = state.cards[state.currentIndex]

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showExplainSheet = true, isExplaining = true, explanation = null)
            try {
                val response = documentApi.explainCard(
                    ExplainCardRequest(front = card.front, back = card.back)
                )
                _uiState.value = _uiState.value.copy(isExplaining = false, explanation = response.explanation)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExplaining = false,
                    explanation = "Không thể lấy giải thích: ${e.message}"
                )
            }
        }
    }

    fun dismissExplanation() {
        _uiState.value = _uiState.value.copy(showExplainSheet = false, explanation = null)
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }

    fun rateCard(rating: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val currentCard = state.cards[state.currentIndex]

            reviewRepository.insertReviewLog(
                ReviewLogEntity(
                    flashcardId = currentCard.id,
                    rating = rating,
                    reviewedAt = System.currentTimeMillis()
                )
            )

            flashcardRepository.getFlashcardById(currentCard.id)?.let { card ->
                val intervalDays = when (rating) {
                    1 -> 0L
                    2 -> 1L
                    3 -> 3L
                    4 -> 7L
                    else -> 1L
                }
                val newDueDate = System.currentTimeMillis() + (intervalDays * 24 * 60 * 60 * 1000)
                flashcardRepository.updateFlashcard(
                    card.copy(
                        dueDate = newDueDate,
                        reps = card.reps + 1,
                        state = if (card.state == "New") "Learning" else card.state,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            val nextIndex = state.currentIndex + 1
            if (nextIndex >= state.totalCards) {
                _uiState.value = state.copy(isComplete = true)
            } else {
                _uiState.value = state.copy(currentIndex = nextIndex)
            }
        }
    }
}
