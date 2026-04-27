package com.duong.udhoctap.feature.library.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duong.udhoctap.feature.knowledgebase.presentation.KnowledgeBaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToKnowledgeBase: () -> Unit
) {
    val kbVm = hiltViewModel<KnowledgeBaseViewModel>()
    val kbState by kbVm.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                kbVm.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thư viện", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section header
            Text(
                "Quản lý tài nguyên học tập",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )

            // Knowledge Base card
            LibraryCard(
                icon = Icons.Default.LibraryBooks,
                title = "Kho tài liệu",
                subtitle = "Tài liệu RAG cho AI Chat",
                gradient = listOf(Color(0xFF1565C0), Color(0xFF0097A7)),
                stats = buildList {
                    val total = kbState.kbs.size
                    if (total > 0) add("$total kho")
                    val ready = kbState.kbs.count { it.status == "ready" }
                    if (ready > 0) add("$ready sẵn sàng")
                    if (kbState.defaultKbName.isNotBlank()) add("Mặc định: ${kbState.defaultKbName}")
                },
                badge = if (kbState.kbs.isNotEmpty()) kbState.kbs.size.toString() else null,
                onClick = onNavigateToKnowledgeBase
            )

            // Quick tips
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Mẹo sử dụng", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            QuickTip(icon = Icons.Default.TipsAndUpdates, text = "Upload tài liệu vào Kho để AI có thể trích dẫn chính xác hơn khi trả lời")
            QuickTip(icon = Icons.Default.Star, text = "Đặt một kho tài liệu làm mặc định trong Cài đặt để tiết kiệm thời gian")

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun LibraryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    stats: List<String>,
    badge: String?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    badge?.let { b ->
                        Surface(shape = CircleShape, color = gradient.first()) {
                            Text(b, style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                if (stats.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        stats.take(3).forEach { stat ->
                            Surface(shape = CircleShape, color = gradient.first().copy(alpha = 0.1f)) {
                                Text(stat, style = MaterialTheme.typography.labelSmall, color = gradient.first(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                }
            }

            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun QuickTip(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

