package com.duong.udhoctap.feature.aichat.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duong.udhoctap.core.network.dto.ChatMessage
import com.duong.udhoctap.core.ui.theme.*
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        ConnectionStatusIndicator(state.status, state.statusMessage)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadSessionHistory(); showHistory = true }) {
                        Icon(Icons.Outlined.History, "Lịch sử")
                    }
                    IconButton(onClick = { viewModel.newSession() }) {
                        Icon(Icons.Outlined.Refresh, "Cuộc trò chuyện mới")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText.trim())
                        inputText = ""
                    }
                },
                isStreaming = state.status == ConnectionStatus.STREAMING || state.status == ConnectionStatus.CONNECTING,
                enableRag = state.enableRag,
                enableWebSearch = state.enableWebSearch,
                onToggleRag = { viewModel.toggleRag() },
                onToggleWeb = { viewModel.toggleWebSearch() }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.messages.isEmpty()) {
                EmptyChatPlaceholder()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.messages, key = { it.hashCode() }) { message ->
                        ChatBubble(message = message)
                    }
                }
            }

            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                    action = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } }
                ) { Text(error) }
            }
        }
    }

    // History sheet
    if (showHistory) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Lịch sử cuộc trò chuyện", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (state.isLoadingHistory) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (state.sessions.isEmpty()) {
                    Text("Chưa có lịch sử", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    state.sessions.forEach { session ->
                        ListItem(
                            headlineContent = {
                                Text(session.title ?: "Cuộc trò chuyện #${session.sessionId.take(6)}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = { Text("${session.messageCount ?: 0} tin nhắn", style = MaterialTheme.typography.labelSmall) },
                            leadingContent = { Icon(Icons.Default.Chat, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.deleteSession(session.sessionId) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            },
                            modifier = Modifier.clickable { viewModel.loadSession(session); showHistory = false }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Purple60, Teal60))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            shape = if (isUser) RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                    else RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (message.isStreaming && message.content.isEmpty()) {
                    TypingIndicator()
                } else if (isUser) {
                    Text(message.content, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodyMedium)
                } else {
                    MarkdownText(markdown = message.content, style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp))
                    if (message.isStreaming) { Spacer(Modifier.height(4.dp)); StreamingCursor() }
                }
            }
        }
        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { i ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.6f, targetValue = 1f,
                animationSpec = infiniteRepeatable(animation = keyframes { durationMillis = 600; 1f at 300 }, repeatMode = RepeatMode.Reverse, initialStartOffset = StartOffset(i * 120)),
                label = "dot_$i"
            )
            Box(modifier = Modifier.size((8 * scale).dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)))
        }
    }
}

@Composable
private fun StreamingCursor() {
    val alpha by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(500, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "cursor_alpha"
    )
    Box(modifier = Modifier.size(width = 2.dp, height = 14.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)))
}

@Composable
private fun ConnectionStatusIndicator(status: ConnectionStatus, message: String) {
    val (color, label) = when (status) {
        ConnectionStatus.IDLE       -> Pair(Color.Gray, "Sẵn sàng")
        ConnectionStatus.CONNECTING -> Pair(Amber60, "Đang kết nối…")
        ConnectionStatus.STREAMING  -> Pair(Green60, message.ifBlank { "Đang trả lời…" })
        ConnectionStatus.DONE       -> Pair(Green60, "Hoàn thành")
        ConnectionStatus.ERROR      -> Pair(Red60, "Lỗi")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun ChatInputBar(
    text: String, onTextChange: (String) -> Unit, onSend: () -> Unit,
    isStreaming: Boolean, enableRag: Boolean, enableWebSearch: Boolean,
    onToggleRag: () -> Unit, onToggleWeb: () -> Unit
) {
    Surface(tonalElevation = 4.dp, shadowElevation = 4.dp) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = enableRag, onClick = onToggleRag,
                    label = { Text("RAG", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (enableRag) { { Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp)) } } else null
                )
                FilterChip(
                    selected = enableWebSearch, onClick = onToggleWeb,
                    label = { Text("Web", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (enableWebSearch) { { Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp)) } } else null
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = text, onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Hỏi bất kỳ điều gì…") },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple60, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onSend, enabled = text.isNotBlank() && !isStreaming,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Purple60)
                ) {
                    if (isStreaming) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    else Icon(Icons.Filled.Send, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EmptyChatPlaceholder() {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))
        Text("Bắt đầu cuộc trò chuyện", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.height(8.dp))
        Text("Hỏi bất kỳ câu hỏi nào về tài liệu học tập của bạn", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}
