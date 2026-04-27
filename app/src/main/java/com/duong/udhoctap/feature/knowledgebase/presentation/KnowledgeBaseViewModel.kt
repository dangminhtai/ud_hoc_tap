package com.duong.udhoctap.feature.knowledgebase.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.data.repository.KnowledgeBaseRepository
import com.duong.udhoctap.core.network.dto.KbItemDto
import com.duong.udhoctap.core.network.dto.KbProgressEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class KbUiState(
    val kbs: List<KbItemDto> = emptyList(),
    val defaultKbName: String = "",
    val isLoading: Boolean = false,
    val creatingName: String? = null,       // name of KB being created
    val progressMap: Map<String, Int> = emptyMap(),   // name → percent
    val progressMsgMap: Map<String, String> = emptyMap(),
    val error: String? = null
)

@HiltViewModel
class KnowledgeBaseViewModel @Inject constructor(
    private val repository: KnowledgeBaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KbUiState())
    val uiState: StateFlow<KbUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val kbs = repository.listKbs()
                val default = repository.getDefault()
                _uiState.value = _uiState.value.copy(
                    kbs = kbs,
                    defaultKbName = default?.name ?: "",
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Không thể tải dữ liệu: ${e.message}")
            }
        }
    }

    fun setDefault(name: String) {
        viewModelScope.launch {
            try {
                repository.setDefault(name)
                _uiState.value = _uiState.value.copy(defaultKbName = name)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteKb(name: String) {
        viewModelScope.launch {
            try {
                repository.deleteKb(name)
                _uiState.value = _uiState.value.copy(kbs = _uiState.value.kbs.filter { it.name != name })
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun createKb(name: String, files: List<File>) {
        _uiState.value = _uiState.value.copy(creatingName = name)
        viewModelScope.launch {
            try {
                val kb = repository.createKb(name, files)
                _uiState.value = _uiState.value.copy(
                    kbs = listOf(kb.copy(status = "processing")) + _uiState.value.kbs
                )
                watchProgress(name)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(creatingName = null, error = e.message)
            }
        }
    }

    private fun watchProgress(name: String) {
        viewModelScope.launch {
            repository.watchProgress(name)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        creatingName = null,
                        error = e.message
                    )
                }
                .collect { event ->
                    when (event) {
                        is KbProgressEvent.Progress -> {
                            _uiState.value = _uiState.value.copy(
                                progressMap = _uiState.value.progressMap + (name to event.data.percent),
                                progressMsgMap = _uiState.value.progressMsgMap + (name to event.data.message)
                            )
                        }
                        is KbProgressEvent.Completed -> {
                            _uiState.value = _uiState.value.copy(
                                creatingName = null,
                                kbs = _uiState.value.kbs.map { if (it.name == name) it.copy(status = "ready") else it },
                                progressMap = _uiState.value.progressMap - name,
                                progressMsgMap = _uiState.value.progressMsgMap - name
                            )
                        }
                        is KbProgressEvent.AppError -> {
                            _uiState.value = _uiState.value.copy(
                                creatingName = null,
                                kbs = _uiState.value.kbs.map { if (it.name == name) it.copy(status = "error") else it },
                                error = event.message
                            )
                        }
                    }
                }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
