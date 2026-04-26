package com.duong.udhoctap.feature.deck.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import com.duong.udhoctap.core.ui.theme.Green60
import com.duong.udhoctap.core.ui.theme.Amber60
import com.duong.udhoctap.core.ui.theme.Red60

private val AI_COUNT_OPTIONS = listOf(5, 10, 15, 20)

private data class DifficultyInfo(
    val key: String,
    val label: String,
    val description: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    onNavigateBack: () -> Unit,
    onAddFlashcard: (Long) -> Unit,
    onEditFlashcard: (Long, Long) -> Unit,
    onStartReview: (Long) -> Unit,
    onStartQuiz: (Long) -> Unit,
    onReviewDrafts: (Long) -> Unit,
    viewModel: DeckDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showTextDialog by viewModel.showTextDialog.collectAsStateWithLifecycle()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // AI config state (shared between file and text flows)
    var showFileAiConfigDialog by remember { mutableStateOf(false) }
    var aiCount by remember { mutableIntStateOf(10) }
    var aiDifficulty by remember { mutableStateOf("medium") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.generateFlashcardsFromFile(context, uri, aiCount, aiDifficulty)
    }

    LaunchedEffect(uiState.aiError) {
        if (uiState.aiError != null) {
            android.widget.Toast.makeText(context, uiState.aiError, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearAiError()
        }
    }

    // ── File AI config dialog ──────────────────────────────────────
    if (showFileAiConfigDialog) {
        AiGenerationConfigDialog(
            title = "Cấu hình tạo thẻ từ tệp",
            count = aiCount,
            difficulty = aiDifficulty,
            onCountChange = { aiCount = it },
            onDifficultyChange = { aiDifficulty = it },
            confirmLabel = "Chọn tệp",
            onConfirm = {
                showFileAiConfigDialog = false
                filePickerLauncher.launch("*/*")
            },
            onDismiss = { showFileAiConfigDialog = false }
        )
    }

    // ── URL / Text dialog (with config) ───────────────────────────
    if (showTextDialog) {
        var inputText by remember { mutableStateOf(TextFieldValue("")) }
        var textCount by remember { mutableIntStateOf(10) }
        var textDifficulty by remember { mutableStateOf("medium") }
        val clipText = clipboardManager.getText()?.text ?: ""

        AlertDialog(
            onDismissRequest = { viewModel.closeTextDialog() },
            title = { Text("Thêm từ URL / Văn bản", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Nhập URL bài viết hoặc dán đoạn văn bản — AI sẽ tạo flashcard tự động.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("https://... hoặc dán văn bản tại đây") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        shape = MaterialTheme.shapes.medium
                    )
                    if (clipText.isNotBlank()) {
                        OutlinedButton(
                            onClick = { inputText = TextFieldValue(clipText) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dán từ clipboard", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    HorizontalDivider()

                    AiConfigSection(
                        count = textCount,
                        difficulty = textDifficulty,
                        onCountChange = { textCount = it },
                        onDifficultyChange = { textDifficulty = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = inputText.text.trim()
                        if (text.isNotEmpty()) viewModel.generateFlashcardsFromText(text, textCount, textDifficulty)
                    },
                    enabled = inputText.text.trim().isNotEmpty(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Tạo thẻ", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeTextDialog() }) { Text("Hủy") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.deckName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(
                        onClick = {
                            val intent = viewModel.getExportIntent()
                            context.startActivity(Intent.createChooser(intent, "Xuất bộ thẻ"))
                        },
                        enabled = uiState.flashcards.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Share, "Xuất CSV", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, "Xóa bộ thẻ", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddFlashcard(uiState.deckId) },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Add, "Thêm thẻ")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onStartReview(uiState.deckId) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    enabled = uiState.flashcards.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.School, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ôn tập", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { onStartQuiz(uiState.deckId) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    enabled = uiState.flashcards.size >= 4
                ) {
                    Icon(Icons.Default.Quiz, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quiz", fontWeight = FontWeight.SemiBold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showFileAiConfigDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Từ tệp (AI)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { viewModel.openTextDialog() },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Link, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("URL / Text", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                }
            }

            if (uiState.draftCount > 0) {
                Card(
                    onClick = { onReviewDrafts(uiState.deckId) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Thẻ AI chờ duyệt", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("${uiState.draftCount} thẻ mới từ tài liệu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                            }
                        }
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip("Tổng thẻ", "${uiState.flashcards.size}")
                StatChip("Mới", "${uiState.flashcards.count { it.state == "New" }}")
                StatChip("Cần ôn", "${uiState.dueCount}")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (uiState.flashcards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🃏", style = MaterialTheme.typography.displayLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Chưa có thẻ nào", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nhấn + để thêm thẻ mới", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = uiState.flashcards, key = { it.id }) { card ->
                        SwipeToDeleteFlashcardItem(
                            front = card.front,
                            back = card.back,
                            onEdit = { onEditFlashcard(uiState.deckId, card.id) },
                            onDelete = { viewModel.deleteFlashcard(card.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Xóa bộ thẻ", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa bộ thẻ này? Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false; viewModel.deleteDeck(onSuccess = onNavigateBack) }) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Hủy") }
            }
        )
    }

    if (uiState.isAiLoading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Đang phân tích") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("AI đang đọc và tạo flashcard. Vui lòng đợi trong giây lát...")
                }
            },
            confirmButton = {}
        )
    }
}

// ── Reusable AI config dialog ──────────────────────────────────────────────
@Composable
private fun AiGenerationConfigDialog(
    title: String,
    count: Int,
    difficulty: String,
    onCountChange: (Int) -> Unit,
    onDifficultyChange: (String) -> Unit,
    confirmLabel: String = "Tiếp tục",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AiConfigSection(
                    count = count,
                    difficulty = difficulty,
                    onCountChange = onCountChange,
                    onDifficultyChange = onDifficultyChange
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, shape = MaterialTheme.shapes.medium) {
                Text(confirmLabel, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

// ── Shared count + difficulty config UI ───────────────────────────────────
@Composable
private fun AiConfigSection(
    count: Int,
    difficulty: String,
    onCountChange: (Int) -> Unit,
    onDifficultyChange: (String) -> Unit
) {
    val difficulties = listOf(
        DifficultyInfo("easy", "Dễ", "Câu hỏi cơ bản, dễ nhớ", Green60),
        DifficultyInfo("medium", "Trung bình", "Kết hợp nhận biết và hiểu biết", Amber60),
        DifficultyInfo("hard", "Khó", "Phân tích, so sánh, ứng dụng sâu", Red60)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Số lượng thẻ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AI_COUNT_OPTIONS.forEach { option ->
                FilterChip(
                    selected = count == option,
                    onClick = { onCountChange(option) },
                    label = { Text("$option", fontWeight = if (count == option) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Mức độ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        difficulties.forEach { info ->
            val isSelected = difficulty == info.key
            Card(
                onClick = { onDifficultyChange(info.key) },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainer
                ),
                border = if (isSelected) CardDefaults.outlinedCardBorder() else null
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onDifficultyChange(info.key) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                info.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = info.color.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    info.key.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = info.color,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            info.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwipeToDeleteFlashcardItem(front: String, back: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.35f },
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().padding(vertical = 2.dp).clip(MaterialTheme.shapes.medium).padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Xóa", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(front, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(back, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Sửa", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}
