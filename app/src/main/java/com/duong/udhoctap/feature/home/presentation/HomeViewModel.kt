package com.duong.udhoctap.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.data.repository.DeckRepository
import com.duong.udhoctap.core.database.entity.DeckEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeckUiModel(
    val id: Long,
    val name: String,
    val description: String?,
    val color: String,
    val cardCount: Int,
    val dueCount: Int
)

data class HomeUiState(
    val decks: List<DeckUiModel> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    private val deckRepository: DeckRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = _searchQuery
        .flatMapLatest { query -> observeDecks(query) }
        .map { decks -> HomeUiState(decks = decks, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private fun observeDecks(query: String): Flow<List<DeckUiModel>> {
        val deckFlow = if (query.isBlank()) {
            deckRepository.getAllDecks()
        } else {
            deckRepository.searchDecks(query)
        }

        // Tối ưu: Không dùng combine lồng nhau bên trong flatMapLatest của từng deck
        // vì nó sẽ tạo ra hàng trăm Flow nếu có nhiều deck, gây treo Main Thread.
        return deckFlow.map { decks ->
            decks.map { deck ->
                DeckUiModel(
                    id = deck.id,
                    name = deck.name,
                    description = deck.description,
                    color = deck.color,
                    cardCount = 0, // Sẽ cập nhật sau bằng SQL tối ưu
                    dueCount = 0
                )
            }
        }
    }

    fun searchDecks(query: String) {
        _searchQuery.value = query.trim()
    }

    fun createDeck(name: String, description: String, color: String) {
        viewModelScope.launch {
            deckRepository.insertDeck(
                DeckEntity(
                    name = name.trim(),
                    description = description.ifBlank { null },
                    color = color
                )
            )
        }
    }

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            deckRepository.deleteDeckById(deckId)
        }
    }
}
