package com.duong.udhoctap.feature.home.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duong.udhoctap.core.ui.components.DeckCard
import com.duong.udhoctap.core.ui.theme.DeckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDeckClick: (Long) -> Unit,
    onNavigateToStats: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val deckCount = uiState.decks.size
    val totalCards = uiState.decks.sumOf { it.cardCount }
    val dueCards = uiState.decks.sumOf { it.dueCount }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm bộ thẻ")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Xin chào!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Học thông minh mỗi ngày",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("$deckCount bộ thẻ") },
                            leadingIcon = {
                                Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("$dueCards cần ôn") },
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        AssistChip(
                            onClick = onNavigateToStats,
                            label = { Text("Thống kê") },
                            leadingIcon = {
                                Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            Text(
                text = "Tìm kiếm nhanh",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchDecks(it)
                },
                placeholder = { Text("Nhập tên bộ thẻ...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                shape = MaterialTheme.shapes.large,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            )

            // Content
            when {
                uiState.decks.isEmpty() && !uiState.isLoading -> {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📚", style = MaterialTheme.typography.displayLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Chưa có bộ thẻ nào",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nhấn + để tạo bộ thẻ đầu tiên",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 168.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.decks,
                            key = { it.id }
                        ) { deck ->
                            DeckCard(
                                name = deck.name,
                                cardCount = deck.cardCount,
                                dueCount = deck.dueCount,
                                color = deck.color,
                                onClick = { onDeckClick(deck.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Deck Dialog
    if (showAddDialog) {
        AddDeckDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, description, color ->
                viewModel.createDeck(name, description, color)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddDeckDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(DeckColors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo bộ thẻ mới", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên bộ thẻ") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả (tùy chọn)") },
                    maxLines = 3,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                // Color picker
                Text(
                    text = "Chọn màu",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DeckColors.take(6).forEach { color ->
                        val bgColor = try {
                            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(color))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        FilledIconButton(
                            onClick = { selectedColor = color },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = bgColor
                            )
                        ) {
                            if (selectedColor == color) {
                                Text("✓", color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description, selectedColor) },
                enabled = name.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Tạo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
        shape = MaterialTheme.shapes.medium
    )
}
