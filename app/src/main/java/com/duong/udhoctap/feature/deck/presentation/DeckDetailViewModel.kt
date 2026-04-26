package com.duong.udhoctap.feature.deck.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Intent
import com.duong.udhoctap.core.data.repository.DeckRepository
import com.duong.udhoctap.core.data.repository.FlashcardRepository
import com.duong.udhoctap.core.database.entity.FlashcardEntity
import com.duong.udhoctap.core.export.ExportCard
import com.duong.udhoctap.core.export.ExportManager
import com.duong.udhoctap.core.network.GenerateFromTextRequest
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
    val draftCount: Int = 0,
    val isLoading: Boolean = true,
    val isAiLoading: Boolean = false,
    val aiError: String? = null
)

@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val documentApi: com.duong.udhoctap.core.network.DocumentApi,
    private val exportManager: ExportManager
) : ViewModel() {

    private val deckId: Long = savedStateHandle.get<Long>("deckId") ?: 0L
    private val _isAiLoading = MutableStateFlow(false)
    private val _aiError = MutableStateFlow<String?>(null)

    private val _showTextDialog = MutableStateFlow(false)
    val showTextDialog: StateFlow<Boolean> = _showTextDialog.asStateFlow()

    val uiState: StateFlow<DeckDetailUiState> = combine(
        deckRepository.getDeckByIdFlow(deckId),
        flashcardRepository.getFlashcardsByDeckId(deckId),
        flashcardRepository.getDraftCards(deckId),
        _isAiLoading,
        _aiError
    ) { deck, flashcards, drafts, isAiLoading, aiError ->
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
            draftCount = drafts.size,
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

    fun generateFlashcardsFromFile(
        context: android.content.Context,
        uri: android.net.Uri,
        count: Int = 10,
        difficulty: String = "medium"
    ) {
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

                val countBody = count.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val difficultyBody = difficulty.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = documentApi.generateFlashcards(filePart, countBody, difficultyBody)

                response.flashcards.forEach { card ->
                    flashcardRepository.insertFlashcard(
                        FlashcardEntity(
                            deckId = deckId,
                            front = card.front,
                            back = card.back,
                            isDraft = true,
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

    fun openTextDialog() {
        _showTextDialog.value = true
    }

    fun closeTextDialog() {
        _showTextDialog.value = false
    }

    fun generateFlashcardsFromText(text: String, count: Int = 10, difficulty: String = "medium") {
        viewModelScope.launch {
            _showTextDialog.value = false
            _isAiLoading.value = true
            _aiError.value = null
            try {
                val isUrl = text.startsWith("http://") || text.startsWith("https://")
                val response = documentApi.generateFromText(
                    GenerateFromTextRequest(text = text, isUrl = isUrl, count = count, difficulty = difficulty)
                )
                response.flashcards.forEach { card ->
                    flashcardRepository.insertFlashcard(
                        FlashcardEntity(
                            deckId = deckId,
                            front = card.front,
                            back = card.back,
                            isDraft = true,
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
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }

    fun clearAiError() {
        _aiError.value = null
    }

    fun getExportIntent(): Intent {
        val state = uiState.value
        val cards = state.flashcards.map { ExportCard(it.front, it.back) }
        return exportManager.exportToCsv(state.deckName, cards)
    }
}
