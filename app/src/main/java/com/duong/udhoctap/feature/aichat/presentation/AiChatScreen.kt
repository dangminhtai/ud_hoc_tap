package com.duong.udhoctap.feature.aichat.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.duong.udhoctap.core.network.dto.ChatMessage
import com.duong.udhoctap.core.ui.components.LatexMarkdownViewer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showHistory by remember { mutableStateOf(false) }
    var showKbSheet by remember { mutableStateOf(false) }

    // Tự động cuộn xuống khi có tin nhắn mới
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
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
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
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
                onToggleWeb = { viewModel.toggleWebSearch() },
                selectedKbs = state.selectedKbs,
                onOpenKbSelect = { showKbSheet = true }
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

            if (showKbSheet) {
                ModalBottomSheet(onDismissRequest = { showKbSheet = false }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
                        Text("Chọn nguồn tri thức (Có thể chọn nhiều)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        state.availableKbs.forEach { kb ->
                            val isSelected = state.selectedKbs.contains(kb.name)
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleKbSelection(kb.name) }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isSelected, onCheckedChange = { viewModel.toggleKbSelection(kb.name) })
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(kb.name, style = MaterialTheme.typography.bodyLarge)
                                    Text("${kb.fileCount} tài liệu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            if (showHistory) {
                ChatHistoryDialog(
                    sessions = state.sessions,
                    isLoading = state.isLoadingHistory,
                    onSessionSelect = { 
                        viewModel.loadSession(it)
                        showHistory = false 
                    },
                    onDeleteSession = { viewModel.deleteSession(it) },
                    onDismiss = { showHistory = false }
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String, onTextChange: (String) -> Unit, onSend: () -> Unit,
    isStreaming: Boolean, enableRag: Boolean, enableWebSearch: Boolean,
    onToggleRag: () -> Unit, onToggleWeb: () -> Unit,
    selectedKbs: Set<String>, onOpenKbSelect: () -> Unit
) {
    Surface(tonalElevation = 4.dp, shadowElevation = 4.dp) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = enableRag, onClick = onToggleRag,
                    label = { Text("RAG", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (enableRag) { { Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp)) } } else null
                )
                
                if (enableRag) {
                    AssistChip(
                        onClick = onOpenKbSelect,
                        label = { 
                            val label = if (selectedKbs.size == 1) selectedKbs.first() else "${selectedKbs.size} kho tri thức"
                            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) 
                        },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.primary)
                    )
                }

                FilterChip(
                    selected = enableWebSearch, onClick = onToggleWeb,
                    label = { Text("Web", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (enableWebSearch) { { Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp)) } } else null
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text("Hỏi AI bất cứ điều gì...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 4
                )

                IconButton(
                    onClick = onSend,
                    enabled = !isStreaming && text.isNotBlank(),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(CircleShape)
                        .background(if (text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (isStreaming) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = bubbleColor,
            shape = shape,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isUser) {
                    Text(text = message.content, color = textColor, style = MaterialTheme.typography.bodyLarge)
                } else {
                    LatexMarkdownViewer(content = message.content, modifier = Modifier.widthIn(max = 300.dp))
                    
                    // Hiển thị nguồn tham khảo
                    val context = LocalContext.current
                    message.sources?.let { sources ->
                        val rag = sources["rag"] ?: emptyList()
                        val web = sources["web"] ?: emptyList()
                        
                        if (rag.isNotEmpty() || web.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                            Text("Nguồn tham khảo:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            
                            // Nguồn RAG
                            rag.forEach { s ->
                                Text("• ${s["title"]}", style = MaterialTheme.typography.labelSmall)
                            }
                            
                            // Nguồn Web
                            web.forEach { s ->
                                val title = s["title"] as? String ?: "Nguồn tin"
                                val url = s["url"] as? String ?: ""
                                Text(
                                    text = "• $title",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { 
                                        if (url.isNotEmpty()) {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                            context.startActivity(intent)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusIndicator(status: ConnectionStatus, message: String) {
    val color = when (status) {
        ConnectionStatus.CONNECTING -> Color.Blue
        ConnectionStatus.THINKING -> Color(0xFFFFC107) // Yellow/Amber
        ConnectionStatus.STREAMING -> Color.Green
        ConnectionStatus.ERROR -> Color.Red
        else -> Color.Gray
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(text = if (message.isNotEmpty()) message else status.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyChatPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Chat, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        Spacer(Modifier.height(16.dp))
        Text("Bắt đầu đặt câu hỏi cho AI", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Tôi có thể giúp bạn giải bài tập, tóm tắt tài liệu...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryDialog(
    sessions: List<com.duong.udhoctap.core.network.dto.SessionDto>,
    isLoading: Boolean,
    onSessionSelect: (com.duong.udhoctap.core.network.dto.SessionDto) -> Unit,
    onDeleteSession: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
            Text("Lịch sử trò chuyện", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (sessions.isEmpty()) {
                Text("Chưa có cuộc trò chuyện nào", modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sessions) { session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { onSessionSelect(session) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(session.title ?: "Trò chuyện không tên", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${session.messageCount} tin nhắn", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { onDeleteSession(session.sessionId) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
