package com.duong.udhoctap.feature.draft.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.data.repository.FlashcardRepository
import com.duong.udhoctap.core.database.entity.FlashcardEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DraftCardUiModel(
    val id: Long,
    val front: String,
    val back: String
)

data class DraftReviewUiState(
    val cards: List<DraftCardUiModel> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DraftReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val flashcardRepository: FlashcardRepository
) : ViewModel() {

    private val deckId: Long = savedStateHandle.get<Long>("deckId") ?: 0L

    val uiState: StateFlow<DraftReviewUiState> = flashcardRepository.getDraftCards(deckId)
        .map { cards ->
            DraftReviewUiState(
                cards = cards.map { DraftCardUiModel(it.id, it.front, it.back) },
                isLoading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DraftReviewUiState())

    fun approveCard(cardId: Long) {
        viewModelScope.launch {
            val card = flashcardRepository.getFlashcardById(cardId) ?: return@launch
            flashcardRepository.updateFlashcard(card.copy(isDraft = false))
        }
    }

    fun rejectCard(cardId: Long) {
        viewModelScope.launch {
            flashcardRepository.deleteFlashcardById(cardId)
        }
    }

    fun approveAll() {
        viewModelScope.launch {
            uiState.value.cards.forEach { card ->
                val entity = flashcardRepository.getFlashcardById(card.id) ?: return@forEach
                flashcardRepository.updateFlashcard(entity.copy(isDraft = false))
            }
        }
    }

    fun rejectAll() {
        viewModelScope.launch {
            uiState.value.cards.forEach { card ->
                flashcardRepository.deleteFlashcardById(card.id)
            }
        }
    }
}
