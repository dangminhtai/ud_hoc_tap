package com.duong.udhoctap.feature.deck.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.data.repository.DeckRepository
import com.duong.udhoctap.core.data.repository.FlashcardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

data class FlashcardUiModel(
    val id: Long,
    val front: String,
    val back: String,
    val state: String,
    val isDraft: Boolean
)

data class DeckDetailUiState(
    val deckId: Long = 0,
    val deckName: String = "",
    val deckDescription: String? = null,
    val flashcards: List<FlashcardUiModel> = emptyList(),
    val dueCount: Int = 0,
    val isLoading: Boolean = true,
    val isAiLoading: Boolean = false,
    val aiError: String? = null
)

@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val documentApi: com.duong.udhoctap.core.network.DocumentApi
) : ViewModel() {

    private val deckId: Long = savedStateHandle.get<Long>("deckId") ?: 0L
    private val _isAiLoading = MutableStateFlow(false)
    private val _aiError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DeckDetailUiState> = combine(
        deckRepository.getDeckByIdFlow(deckId),
        flashcardRepository.getFlashcardsByDeckId(deckId),
        _isAiLoading,
        _aiError
    ) { deck, flashcards, isAiLoading, aiError ->
        val dueCards = flashcards.count {
            it.dueDate <= System.currentTimeMillis() && !it.isDraft
        }
        DeckDetailUiState(
            deckId = deckId,
            deckName = deck?.name ?: "",
            deckDescription = deck?.description,
            flashcards = flashcards.map { card ->
                FlashcardUiModel(
                    id = card.id,
                    front = card.front,
                    back = card.back,
                    state = card.state,
                    isDraft = card.isDraft
                )
            },
            dueCount = dueCards,
            isLoading = false,
            isAiLoading = isAiLoading,
            aiError = aiError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DeckDetailUiState())

    fun deleteFlashcard(flashcardId: Long) {
        viewModelScope.launch {
            flashcardRepository.deleteFlashcardById(flashcardId)
        }
    }

    fun deleteDeck(onSuccess: () -> Unit) {
        viewModelScope.launch {
            deckRepository.deleteDeckById(deckId)
            onSuccess()
        }
    }

    fun generateFlashcardsFromFile(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiError.value = null
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw Exception("Cannot read file")
                val fileName = getFileName(context, uri) ?: "document.pdf"
                
                val contentType = context.contentResolver.getType(uri)?.toMediaTypeOrNull()
                    ?: "application/octet-stream".toMediaTypeOrNull()
                val requestBody = bytes.toRequestBody(contentType)
                val filePart = okhttp3.MultipartBody.Part.createFormData("file", fileName, requestBody)
                
                val response = documentApi.generateFlashcards(filePart)
                
                response.flashcards.forEach { card ->
                    flashcardRepository.insertFlashcard(
                        com.duong.udhoctap.core.database.entity.FlashcardEntity(
                            deckId = deckId,
                            front = card.front,
                            back = card.back,
                            isDraft = true, // Wait for user review
                            state = "New",
                            dueDate = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                _aiError.value = e.message ?: "An error occurred"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    private fun getFileName(context: android.content.Context, uri: android.net.Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                }
            } catch (_: Exception) {
                // Ignore
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
    
    fun clearAiError() {
        _aiError.value = null
    }
}
