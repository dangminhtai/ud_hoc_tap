package com.duong.udhoctap.feature.deck.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.data.repository.FlashcardRepository
import com.duong.udhoctap.core.data.repository.TagRepository
import com.duong.udhoctap.core.database.entity.FlashcardTagCrossRef
import com.duong.udhoctap.core.database.entity.FlashcardEntity
import com.duong.udhoctap.core.database.entity.TagEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagUiModel(
    val id: Long,
    val name: String,
    val color: String
)

data class AddEditFlashcardUiState(
    val front: String = "",
    val back: String = "",
    val isEditing: Boolean = false,
    val isSaved: Boolean = false,
    val availableTags: List<TagUiModel> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet()
)

@HiltViewModel
class AddEditFlashcardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val flashcardRepository: FlashcardRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val deckId: Long = savedStateHandle.get<Long>("deckId") ?: 0L
    private val flashcardId: Long = savedStateHandle.get<Long>("flashcardId") ?: -1L

    private val _uiState = MutableStateFlow(AddEditFlashcardUiState())
    val uiState: StateFlow<AddEditFlashcardUiState> = _uiState.asStateFlow()

    init {
        observeTags()
        if (flashcardId > 0) {
            loadFlashcard()
        }
    }

    private fun observeTags() {
        viewModelScope.launch {
            tagRepository.getAllTags().collectLatest { tags ->
                _uiState.update { state ->
                    val uiTags = tags.map { tag ->
                        TagUiModel(
                            id = tag.id,
                            name = tag.name,
                            color = tag.color
                        )
                    }
                    val validTagIds = uiTags.map { it.id }.toSet()
                    state.copy(
                        availableTags = uiTags,
                        selectedTagIds = state.selectedTagIds.intersect(validTagIds)
                    )
                }
            }
        }
    }

    private fun loadFlashcard() {
        viewModelScope.launch {
            flashcardRepository.getFlashcardById(flashcardId)?.let { card ->
                val selectedTagIds = tagRepository
                    .getTagsForFlashcard(flashcardId)
                    .first()
                    .map { it.id }
                    .toSet()
                _uiState.value = _uiState.value.copy(
                    front = card.front,
                    back = card.back,
                    isEditing = true,
                    selectedTagIds = selectedTagIds
                )
            }
        }
    }

    fun updateFront(value: String) {
        _uiState.value = _uiState.value.copy(front = value)
    }

    fun updateBack(value: String) {
        _uiState.value = _uiState.value.copy(back = value)
    }

    fun toggleTagSelection(tagId: Long) {
        _uiState.update { state ->
            val nextSelection = if (state.selectedTagIds.contains(tagId)) {
                state.selectedTagIds - tagId
            } else {
                state.selectedTagIds + tagId
            }
            state.copy(selectedTagIds = nextSelection)
        }
    }

    fun createTag(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return

        viewModelScope.launch {
            val existingTagId = _uiState.value.availableTags
                .firstOrNull { it.name.equals(normalizedName, ignoreCase = true) }
                ?.id

            val tagId = existingTagId ?: tagRepository.insertTag(
                TagEntity(
                    name = normalizedName
                )
            )

            _uiState.update { state ->
                state.copy(selectedTagIds = state.selectedTagIds + tagId)
            }
        }
    }

    fun saveFlashcard() {
        viewModelScope.launch {
            val state = _uiState.value
            val targetFlashcardId = if (state.isEditing && flashcardId > 0) {
                val existing = flashcardRepository.getFlashcardById(flashcardId)
                if (existing != null) {
                    flashcardRepository.updateFlashcard(
                        existing.copy(
                            front = state.front,
                            back = state.back,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    existing.id
                } else {
                    flashcardRepository.insertFlashcard(
                        FlashcardEntity(
                            deckId = deckId,
                            front = state.front,
                            back = state.back
                        )
                    )
                }
            } else {
                flashcardRepository.insertFlashcard(
                    FlashcardEntity(
                        deckId = deckId,
                        front = state.front,
                        back = state.back
                    )
                )
            }
            syncFlashcardTags(
                flashcardId = targetFlashcardId,
                selectedTagIds = state.selectedTagIds
            )
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    private suspend fun syncFlashcardTags(
        flashcardId: Long,
        selectedTagIds: Set<Long>
    ) {
        val existingTagIds = tagRepository
            .getTagsForFlashcard(flashcardId)
            .first()
            .map { it.id }
            .toSet()

        val tagsToAdd = selectedTagIds - existingTagIds
        val tagsToRemove = existingTagIds - selectedTagIds

        tagsToAdd.forEach { tagId ->
            tagRepository.insertFlashcardTagCrossRef(
                FlashcardTagCrossRef(
                    flashcardId = flashcardId,
                    tagId = tagId
                )
            )
        }

        tagsToRemove.forEach { tagId ->
            tagRepository.deleteFlashcardTagCrossRef(
                FlashcardTagCrossRef(
                    flashcardId = flashcardId,
                    tagId = tagId
                )
            )
        }
    }
}
