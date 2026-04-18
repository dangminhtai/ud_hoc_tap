package com.duong.udhoctap.feature.deck.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFlashcardScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditFlashcardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var newTagName by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditing) "Sửa thẻ" else "Thêm thẻ mới",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Front side
            Text("Mặt trước", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = uiState.front,
                onValueChange = { viewModel.updateFront(it) },
                placeholder = { Text("Nhập câu hỏi hoặc từ vựng…") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                shape = MaterialTheme.shapes.medium,
                maxLines = 5
            )

            // Back side
            Text("Mặt sau", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = uiState.back,
                onValueChange = { viewModel.updateBack(it) },
                placeholder = { Text("Nhập câu trả lời hoặc định nghĩa…") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                shape = MaterialTheme.shapes.medium,
                maxLines = 5
            )

            // Tags
            Text("Tag", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            if (uiState.availableTags.isEmpty()) {
                Text(
                    text = "Chưa có tag, hãy thêm tag mới bên dưới",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items = uiState.availableTags, key = { it.id }) { tag ->
                        FilterChip(
                            selected = uiState.selectedTagIds.contains(tag.id),
                            onClick = { viewModel.toggleTagSelection(tag.id) },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Thêm tag mới") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                Button(
                    onClick = {
                        viewModel.createTag(newTagName)
                        newTagName = ""
                    },
                    enabled = newTagName.isNotBlank(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Thêm")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = { viewModel.saveFlashcard() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = uiState.front.isNotBlank() && uiState.back.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lưu thẻ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
