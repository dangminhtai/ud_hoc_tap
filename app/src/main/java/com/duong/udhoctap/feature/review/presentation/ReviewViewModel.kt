package com.duong.udhoctap.feature.review.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.data.repository.FlashcardRepository
import com.duong.udhoctap.core.data.repository.ReviewRepository
import com.duong.udhoctap.core.database.entity.ReviewLogEntity
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
    val isLoading: Boolean = true
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val flashcardRepository: FlashcardRepository,
    private val reviewRepository: ReviewRepository
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

    fun rateCard(rating: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val currentCard = state.cards[state.currentIndex]

            // Save review log
            reviewRepository.insertReviewLog(
                ReviewLogEntity(
                    flashcardId = currentCard.id,
                    rating = rating,
                    reviewedAt = System.currentTimeMillis()
                )
            )

            // Update flashcard due date based on simple rating logic
            flashcardRepository.getFlashcardById(currentCard.id)?.let { card ->
                val intervalDays = when (rating) {
                    1 -> 0L   // Again: review again today
                    2 -> 1L   // Hard: tomorrow
                    3 -> 3L   // Good: 3 days
                    4 -> 7L   // Easy: 1 week
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

            // Move to next card
            val nextIndex = state.currentIndex + 1
            if (nextIndex >= state.totalCards) {
                _uiState.value = state.copy(isComplete = true)
            } else {
                _uiState.value = state.copy(currentIndex = nextIndex)
            }
        }
    }
}
