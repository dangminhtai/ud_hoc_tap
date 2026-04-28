package com.duong.udhoctap.feature.quiz.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duong.udhoctap.core.ui.components.QuizOptionButton
import com.duong.udhoctap.core.ui.theme.*
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

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
                title = {
                    Text(
                        when (uiState.phase) {
                            QuizPhase.LOADING  -> "Đang tải..."
                            QuizPhase.PLAYING  -> "Kiểm tra"
                            QuizPhase.COMPLETE -> "Kết quả"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    ) { padding ->
        when (uiState.phase) {
            QuizPhase.LOADING -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            QuizPhase.PLAYING -> {
                if (uiState.questions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Cần ít nhất 1 thẻ để làm quiz",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    QuizPlayingContent(
                        uiState = uiState,
                        onSelectAnswer = viewModel::selectAnswer,
                        onShowEssayAnswer = viewModel::showEssayAnswer,
                        onSubmitEssayResult = viewModel::submitEssayResult,
                        onEssayInputChange = viewModel::updateEssayInput,
                        onNext = viewModel::nextQuestion,
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            QuizPhase.COMPLETE -> QuizCompleteContent(
                uiState = uiState,
                onBack = onNavigateBack,
                onRetry = viewModel::restartQuizFull,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun QuizPlayingContent(
    uiState: QuizUiState,
    onSelectAnswer: (Int) -> Unit,
    onShowEssayAnswer: () -> Unit,
    onSubmitEssayResult: (Boolean) -> Unit,
    onEssayInputChange: (String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val question = uiState.questions[uiState.currentIndex]

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Progress
            LinearProgressIndicator(
                progress = { (uiState.currentIndex + 1).toFloat() / uiState.totalQuestions },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Câu ${uiState.currentIndex + 1}/${uiState.totalQuestions}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${uiState.correctCount} đúng",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Green60,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Question Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (question.isEssay) {
                        Text(
                            "Tự luận",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Text(
                        text = question.questionText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Question content by type
            when {
                question.isEssay -> EssayQuestionContent(
                    uiState = uiState,
                    question = question,
                    onEssayInputChange = onEssayInputChange,
                    onShowAnswer = onShowEssayAnswer,
                    onSubmitResult = onSubmitEssayResult
                )
                question.isMultipleChoice -> MultipleChoiceContent(
                    uiState = uiState,
                    question = question,
                    onSelectAnswer = onSelectAnswer
                )
                else -> FlashcardContent(
                    uiState = uiState,
                    question = question,
                    onShowAnswer = onShowEssayAnswer,
                    onSubmitResult = onSubmitEssayResult
                )
            }

            // Explanation after answering
            if (uiState.hasAnswered && question.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Giải thích",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        MarkdownText(
                            markdown = question.explanation,
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        AnimatedVisibility(visible = uiState.hasAnswered) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 20.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    if (uiState.currentIndex + 1 >= uiState.totalQuestions) "Xem kết quả" else "Câu tiếp theo",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MultipleChoiceContent(
    uiState: QuizUiState,
    question: QuizQuestion,
    onSelectAnswer: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        question.options.forEachIndexed { index, option ->
            QuizOptionButton(
                text = option,
                index = index,
                isSelected = uiState.selectedAnswer == index,
                isCorrect = if (uiState.hasAnswered) index == question.correctIndex else null,
                enabled = !uiState.hasAnswered,
                onClick = { onSelectAnswer(index) }
            )
        }
    }
}

@Composable
private fun EssayQuestionContent(
    uiState: QuizUiState,
    question: QuizQuestion,
    onEssayInputChange: (String) -> Unit,
    onShowAnswer: () -> Unit,
    onSubmitResult: (Boolean) -> Unit
) {
    Column {
        OutlinedTextField(
            value = uiState.essayInput,
            onValueChange = onEssayInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Câu trả lời của bạn") },
            minLines = 3,
            maxLines = 6,
            enabled = !uiState.showEssayAnswer,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!uiState.showEssayAnswer) {
            OutlinedButton(
                onClick = onShowAnswer,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Xem đáp án")
            }
        } else {
            // Show correct answer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Đáp án mẫu",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        question.correctAnswer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (!uiState.hasAnswered) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Bạn trả lời có đúng không?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSubmitResult(false) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Red60)
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Chưa đúng", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onSubmitResult(true) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = Green60)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Đúng rồi", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardContent(
    uiState: QuizUiState,
    question: QuizQuestion,
    onShowAnswer: () -> Unit,
    onSubmitResult: (Boolean) -> Unit
) {
    if (!uiState.showEssayAnswer) {
        OutlinedButton(
            onClick = onShowAnswer,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Lật thẻ / Xem đáp án")
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text(
                question.correctAnswer,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
        }

        if (!uiState.hasAnswered) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onSubmitResult(false) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Red60)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Chưa nhớ")
                }
                Button(
                    onClick = { onSubmitResult(true) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = Green60)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nhớ rồi")
                }
            }
        }
    }
}

@Composable
private fun QuizCompleteContent(
    uiState: QuizUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percentage = if (uiState.totalQuestions > 0)
        (uiState.correctCount * 100) / uiState.totalQuestions else 0

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Amber60,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Kết quả", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Làm lại", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Quay lại", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
