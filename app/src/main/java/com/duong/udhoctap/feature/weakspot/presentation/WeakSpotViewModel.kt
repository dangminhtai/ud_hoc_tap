package com.duong.udhoctap.feature.weakspot.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.data.repository.DeckRepository
import com.duong.udhoctap.core.data.repository.FlashcardRepository
import com.duong.udhoctap.core.data.repository.ReviewRepository
import com.duong.udhoctap.core.database.entity.DeckEntity
import com.duong.udhoctap.core.database.entity.FlashcardEntity
import com.duong.udhoctap.core.network.BackendApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeakCardUiModel(
    val id: Long,
    val front: String,
    val back: String,
    val againCount: Int,
    val deckName: String
)

data class WeakSpotUiState(
    val weakCards: List<WeakCardUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isCreatingDeck: Boolean = false,
    val deckCreatedName: String? = null
)

@HiltViewModel
class WeakSpotViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val flashcardRepository: FlashcardRepository,
    private val deckRepository: DeckRepository,
    private val backendApi: BackendApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeakSpotUiState())
    val uiState: StateFlow<WeakSpotUiState> = _uiState.asStateFlow()

    init {
        loadWeakCards()
    }

    private fun loadWeakCards() {
        viewModelScope.launch {
            val stats = reviewRepository.getWeakCardStats()
            val cards = stats.mapNotNull { stat ->
                val card = flashcardRepository.getFlashcardById(stat.flashcardId) ?: return@mapNotNull null
                val deck = deckRepository.getDeckById(card.deckId)
                WeakCardUiModel(
                    id = card.id,
                    front = card.front,
                    back = card.back,
                    againCount = stat.againCount,
                    deckName = deck?.name ?: "Unknown"
                )
            }
            _uiState.value = WeakSpotUiState(weakCards = cards, isLoading = false)
        }
    }

    fun createWeakSpotDeck() {
        viewModelScope.launch {
            val weakCards = _uiState.value.weakCards
            if (weakCards.isEmpty()) return@launch

            _uiState.value = _uiState.value.copy(isCreatingDeck = true)
            try {
                val deckName = "Điểm Yếu"
                val newDeckId = deckRepository.insertDeck(
                    DeckEntity(
                        name = deckName,
                        description = "Tự động tạo từ phân tích điểm yếu — ${weakCards.size} thẻ khó nhất",
                        color = "#FF5252"
                    )
                )
                weakCards.forEach { card ->
                    flashcardRepository.insertFlashcard(
                        FlashcardEntity(
                            deckId = newDeckId,
                            front = card.front,
                            back = card.back,
                            state = "New",
                            dueDate = System.currentTimeMillis()
                        )
                    )
                }
                _uiState.value = _uiState.value.copy(isCreatingDeck = false, deckCreatedName = deckName)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isCreatingDeck = false)
            }
        }
    }

    fun dismissDeckCreatedMessage() {
        _uiState.value = _uiState.value.copy(deckCreatedName = null)
    }

    fun loadWeakSpotsFromBackend(deckId: String) {
        viewModelScope.launch {
            try {
                // TODO: Call backend API when implemented
                // val response = backendApi.analyzeDeckWeakSpots(deckId)
                // Map backend response to WeakCardUiModel and update UI

                // For now, the local analysis is used
                // Backend will provide more advanced analysis in the future
            } catch (e: Exception) {
                // Silently fall back to local analysis
            }
        }
    }
}
