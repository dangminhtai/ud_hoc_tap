package com.duong.udhoctap.feature.review.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import com.duong.udhoctap.core.ui.theme.*
import com.duong.udhoctap.feature.review.presentation.components.FlipCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isFlipped by remember { mutableStateOf(false) }

    // Reset flip when card changes
    LaunchedEffect(uiState.currentIndex) {
        isFlipped = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ôn tập", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (uiState.isComplete) {
            // Completion screen
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Green60,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Hoàn thành! 🎉", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Đã ôn ${uiState.totalCards} thẻ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = onNavigateBack, shape = MaterialTheme.shapes.medium) {
                        Text("Quay lại", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else if (uiState.cards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Không có thẻ nào để ôn tập", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Progress
                LinearProgressIndicator(
                    progress = { (uiState.currentIndex + 1).toFloat() / uiState.totalCards },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.primary,
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${uiState.currentIndex + 1} / ${uiState.totalCards}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Card
                val currentCard = uiState.cards[uiState.currentIndex]
                FlipCard(
                    frontText = currentCard.front,
                    backText = currentCard.back,
                    isFlipped = isFlipped,
                    onFlip = { isFlipped = !isFlipped },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Rating buttons (visible only when flipped)
                AnimatedVisibility(
                    visible = isFlipped,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RatingButton("Quên", RatingAgain, Modifier.weight(1f)) { viewModel.rateCard(1) }
                        RatingButton("Khó", RatingHard, Modifier.weight(1f)) { viewModel.rateCard(2) }
                        RatingButton("Tốt", RatingGood, Modifier.weight(1f)) { viewModel.rateCard(3) }
                        RatingButton("Dễ", RatingEasy, Modifier.weight(1f)) { viewModel.rateCard(4) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}
