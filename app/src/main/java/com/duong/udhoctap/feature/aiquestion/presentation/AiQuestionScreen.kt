package com.duong.udhoctap.feature.aiquestion.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duong.udhoctap.core.data.repository.DeckRepository
import com.duong.udhoctap.core.network.dto.GeneratedQuestion
import com.duong.udhoctap.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiQuestionScreen(
    onNavigateBack: () -> Unit,
    deckRepository: DeckRepository,
    viewModel: AiQuestionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val decks by deckRepository.getAllDecks().collectAsState(initial = emptyList())
    var showDeckPickerForBulk by remember { mutableStateOf(false) }
    var editingQuestion by remember { mutableStateOf<GeneratedQuestion?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo câu hỏi", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                actions = {
                    if (state.isComplete && state.questions.isNotEmpty()) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Outlined.Refresh, "Đặt lại")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Bulk save bar
            AnimatedVisibility(
                visible = state.isComplete && state.questions.isNotEmpty() && !state.allSaved,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                Surface(tonalElevation = 6.dp, shadowElevation = 6.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${state.questions.size} câu hỏi đã tạo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${state.savedIds.size} đã lưu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { showDeckPickerForBulk = true },
                            enabled = decks.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Lưu tất cả", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (state.allSaved) {
                Surface(tonalElevation = 6.dp, color = Color(0xFF2E7D32)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("Đã lưu tất cả vào bộ thẻ!", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Config form ────────────────────────────────────────────────
            item {
                QuestionConfigCard(
                    topic = state.topic, onTopicChange = { viewModel.setTopic(it) },
                    count = state.count, onCountChange = { viewModel.setCount(it) },
                    difficulty = state.difficulty, onDifficultyChange = { viewModel.setDifficulty(it) },
                    questionType = state.questionType, onTypeChange = { viewModel.setQuestionType(it) },
                    onGenerate = { viewModel.generate() },
                    isGenerating = state.isGenerating
                )
            }

            // ── Progress ───────────────────────────────────────────────────
            if (state.isGenerating || (state.statusMessage.isNotBlank() && !state.isComplete)) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (state.isGenerating) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Purple60)
                                else Icon(Icons.Default.CheckCircle, null, tint = Green60, modifier = Modifier.size(14.dp))
                                Text(state.statusMessage, style = MaterialTheme.typography.labelMedium, color = if (state.isGenerating) Purple60 else Green60)
                            }
                            if (state.isGenerating && state.count > 0) {
                                LinearProgressIndicator(
                                    progress = { state.generatedCount.toFloat() / state.count },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("${state.generatedCount}/${state.count} câu hỏi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // ── Questions ──────────────────────────────────────────────────
            items(state.questions, key = { it.id }) { question ->
                QuestionCard(
                    question = question,
                    isSaved = question.id in state.savedIds,
                    decks = decks,
                    onSave = { deckId -> viewModel.saveAsFlashcard(question, deckId) },
                    onEdit = { editingQuestion = question }
                )
            }

            // ── Error ──────────────────────────────────────────────────────
            state.error?.let { err ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(err, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.clearError() }) { Icon(Icons.Default.Close, null) }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }

    // Bulk deck picker
    if (showDeckPickerForBulk && decks.isNotEmpty()) {
        DeckPickerSheet(
            decks = decks,
            title = "Lưu tất cả vào bộ thẻ",
            onSelect = { deckId -> viewModel.saveAllAsFlashcards(deckId); showDeckPickerForBulk = false },
            onDismiss = { showDeckPickerForBulk = false }
        )
    }

    // Edit question dialog
    editingQuestion?.let { q ->
        EditQuestionDialog(
            question = q,
            onConfirm = { updated -> viewModel.updateQuestion(updated); editingQuestion = null },
            onDismiss = { editingQuestion = null }
        )
    }
}

@Composable
private fun QuestionConfigCard(
    topic: String, onTopicChange: (String) -> Unit,
    count: Int, onCountChange: (Int) -> Unit,
    difficulty: String, onDifficultyChange: (String) -> Unit,
    questionType: String, onTypeChange: (String) -> Unit,
    onGenerate: () -> Unit, isGenerating: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Cài đặt câu hỏi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = topic, onValueChange = onTopicChange,
                modifier = Modifier.fillMaxWidth(), label = { Text("Chủ đề / Kiến thức") },
                placeholder = { Text("VD: Giải tích, Machine Learning…") },
                maxLines = 2, shape = RoundedCornerShape(12.dp), enabled = !isGenerating,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple60)
            )

            // Count slider
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Số câu hỏi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(shape = CircleShape, color = Purple60) {
                        Text("$count", style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                    }
                }
                Slider(value = count.toFloat(), onValueChange = { onCountChange(it.toInt()) }, valueRange = 1f..20f, steps = 18, enabled = !isGenerating)
            }

            // Difficulty chips
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Độ khó", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("easy" to "Dễ", "medium" to "Vừa", "hard" to "Khó").forEach { (value, label) ->
                        FilterChip(selected = difficulty == value, onClick = { onDifficultyChange(value) }, label = { Text(label) }, enabled = !isGenerating)
                    }
                }
            }

            // Type chips
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Loại câu hỏi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("" to "Tất cả", "multiple_choice" to "Trắc nghiệm", "essay" to "Tự luận", "true_false" to "Đúng/Sai").forEach { (value, label) ->
                        FilterChip(selected = questionType == value, onClick = { onTypeChange(value) }, label = { Text(label, style = MaterialTheme.typography.labelSmall) }, enabled = !isGenerating)
                    }
                }
            }

            Button(
                onClick = onGenerate,
                enabled = topic.isNotBlank() && !isGenerating,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple60)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Đang tạo…")
                } else {
                    Icon(Icons.Filled.Quiz, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tạo câu hỏi", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: GeneratedQuestion,
    isSaved: Boolean,
    decks: List<com.duong.udhoctap.core.database.entity.DeckEntity>,
    onSave: (Long) -> Unit,
    onEdit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeckPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSaved) 0.dp else 2.dp),
        border = if (isSaved) CardDefaults.outlinedCardBorder() else null,
        colors = if (isSaved) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Difficulty badge
                val diffColor = when (question.difficulty) {
                    "easy" -> Color(0xFF2E7D32); "hard" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
                Surface(shape = CircleShape, color = diffColor.copy(alpha = 0.12f)) {
                    Text(
                        when (question.difficulty) { "easy" -> "Dễ"; "hard" -> "Khó"; else -> "Vừa" },
                        style = MaterialTheme.typography.labelSmall, color = diffColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                if (question.questionType.isNotBlank()) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(question.questionType.replace("_", " "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                if (isSaved) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                } else {
                    // Edit button
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, "Sửa", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Question text
            Text(question.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)

            // Options (if multiple choice)
            AnimatedVisibility(visible = expanded || question.options.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    question.options.forEachIndexed { i, opt ->
                        val isCorrect = opt.equals(question.answer, ignoreCase = true) || (i == 0 && question.answer == "A")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCorrect && expanded) Color(0xFF2E7D32).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${('A' + i)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isCorrect && expanded) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(opt, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Expand/Collapse + Save
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (expanded) "Thu gọn" else "Xem đáp án", style = MaterialTheme.typography.labelMedium)
                }

                if (!isSaved) {
                    Button(
                        onClick = { if (decks.isNotEmpty()) showDeckPicker = true },
                        enabled = decks.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple60)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Lưu", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Answer (when expanded)
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(shape = MaterialTheme.shapes.small, color = Color(0xFF2E7D32).copy(alpha = 0.08f)) {
                        Text("Đáp án: ${question.answer}", modifier = Modifier.fillMaxWidth().padding(10.dp), style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                    }
                    if (question.explanation.isNotBlank()) {
                        Text("Giải thích: ${question.explanation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showDeckPicker && decks.isNotEmpty()) {
        DeckPickerSheet(decks = decks, title = "Chọn bộ thẻ để lưu", onSelect = { onSave(it); showDeckPicker = false }, onDismiss = { showDeckPicker = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckPickerSheet(
    decks: List<com.duong.udhoctap.core.database.entity.DeckEntity>,
    title: String,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            decks.forEach { deck ->
                ListItem(
                    headlineContent = { Text(deck.name) },
                    leadingContent = { Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { onSelect(deck.id) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun EditQuestionDialog(question: GeneratedQuestion, onConfirm: (GeneratedQuestion) -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf(question.question) }
    var a by remember { mutableStateOf(question.answer) }
    var exp by remember { mutableStateOf(question.explanation) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sửa câu hỏi", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = q, onValueChange = { q = it }, label = { Text("Câu hỏi") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("Đáp án") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = exp, onValueChange = { exp = it }, label = { Text("Giải thích (tuỳ chọn)") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(question.copy(question = q.trim(), answer = a.trim(), explanation = exp.trim())) }) { Text("Xác nhận", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}
