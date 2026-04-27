package com.duong.udhoctap.feature.aihub.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duong.udhoctap.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHubScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToQuestion: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hub_anim")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "gradient"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("AI Studio", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Hero Banner ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Purple60, Teal60, Purple40),
                            start = androidx.compose.ui.geometry.Offset(gradientOffset * 200, 0f),
                            end = androidx.compose.ui.geometry.Offset(gradientOffset * 200 + 600, 400f)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Trợ lý học tập AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(
                        "Chat thông minh, giải bài toán, tạo câu hỏi và lộ trình học — tất cả trong một nơi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 20.sp
                    )
                }
            }

            // ── Section label ────────────────────────────────────────────────
            Text(
                "Chọn công cụ",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            // ── Grid ─────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AiGridCard(
                    icon = Icons.Filled.Chat,
                    title = "AI Chat",
                    subtitle = "Hỏi đáp và giải thích",
                    gradient = listOf(Color(0xFF5B5AF7), Color(0xFF7C5CFC)),
                    tag = "RAG • Web",
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToChat
                )
                AiGridCard(
                    icon = Icons.Filled.Quiz,
                    title = "Tạo câu hỏi",
                    subtitle = "Luyện tập từ chủ đề",
                    gradient = listOf(Color(0xFFFF6B8A), Color(0xFFD84E6D)),
                    tag = "→ Flashcards",
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToQuestion
                )
            }

            // ── How it works ─────────────────────────────────────────────────
            HorizontalDivider()
            Text("Cách hoạt động", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HowItWorksItem(number = "1", text = "Upload tài liệu vào Thư viện → Kho tài liệu để AI có kiến thức nền")
                HowItWorksItem(number = "2", text = "Dùng AI Chat với RAG bật để AI trích dẫn tài liệu của bạn")
                HowItWorksItem(number = "3", text = "Tạo câu hỏi và lưu vào deck flashcard để ôn tập định kỳ")
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AiGridCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                onClick = onClick,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), maxLines = 2, lineHeight = 15.sp)
                Spacer(Modifier.height(4.dp))
                Surface(shape = CircleShape, color = gradient.first().copy(alpha = 0.12f)) {
                    Text(tag, style = MaterialTheme.typography.labelSmall, color = gradient.first(), modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun HowItWorksItem(number: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(22.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(number, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            }
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
    }
}
