package com.duong.udhoctap.feature.quiz.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duong.udhoctap.core.ui.components.QuizOptionButton
import com.duong.udhoctap.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kiểm tra", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        when {
            uiState.isComplete -> {
                // Results screen
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Amber60,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Kết quả", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        val percentage = if (uiState.totalQuestions > 0)
                            (uiState.correctCount * 100) / uiState.totalQuestions else 0

                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                percentage >= 80 -> Green60
                                percentage >= 50 -> Amber60
                                else -> Red60
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${uiState.correctCount}/${uiState.totalQuestions} câu đúng",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = onNavigateBack, shape = MaterialTheme.shapes.medium) {
                            Text("Quay lại", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            uiState.questions.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cần ít nhất 4 thẻ để tạo quiz", style = MaterialTheme.typography.titleMedium)
                }
            }
            else -> {
                val question = uiState.questions[uiState.currentIndex]

                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)
                ) {
                    // Progress
                    LinearProgressIndicator(
                        progress = { (uiState.currentIndex + 1).toFloat() / uiState.totalQuestions },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Câu ${uiState.currentIndex + 1}/${uiState.totalQuestions}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Question card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = question.questionText,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Options
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        question.options.forEachIndexed { index, option ->
                            QuizOptionButton(
                                text = option,
                                index = index,
                                isSelected = uiState.selectedAnswer == index,
                                isCorrect = if (uiState.hasAnswered) index == question.correctIndex else null,
                                enabled = !uiState.hasAnswered,
                                onClick = { viewModel.selectAnswer(index) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Next button
                    AnimatedVisibility(visible = uiState.hasAnswered) {
                        Button(
                            onClick = { viewModel.nextQuestion() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                if (uiState.currentIndex + 1 >= uiState.totalQuestions) "Xem kết quả" else "Câu tiếp theo",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
