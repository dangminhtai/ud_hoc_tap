package com.duong.udhoctap.feature.knowledgebase.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duong.udhoctap.core.network.dto.KbItemDto
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseScreen(
    onNavigateBack: () -> Unit,
    viewModel: KnowledgeBaseViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kho tài liệu", fontWeight = FontWeight.Bold)
                        Text("${state.kbs.size} kho", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } },
                actions = {
                    IconButton(onClick = { viewModel.load() }) { Icon(Icons.Default.Refresh, "Làm mới") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateSheet = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Thêm tài liệu") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Default KB banner
                    if (state.defaultKbName.isNotBlank()) {
                        item {
                            DefaultKbBanner(name = state.defaultKbName)
                        }
                    }

                    if (state.kbs.isEmpty()) {
                        item { KbEmptyState() }
                    } else {
                        items(state.kbs, key = { it.name }) { kb ->
                            KbItemCard(
                                kb = kb,
                                isDefault = kb.name == state.defaultKbName,
                                progress = state.progressMap[kb.name],
                                progressMsg = state.progressMsgMap[kb.name],
                                onSetDefault = { viewModel.setDefault(kb.name) },
                                onDelete = { viewModel.deleteKb(kb.name) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } }
                ) { Text(error) }
            }
        }
    }

    if (showCreateSheet) {
        CreateKbSheet(
            isCreating = state.creatingName != null,
            onConfirm = { name, files -> viewModel.createKb(name, files); showCreateSheet = false },
            onDismiss = { showCreateSheet = false }
        )
    }
}

@Composable
private fun DefaultKbBanner(name: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Kho mặc định", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun KbItemCard(
    kb: KbItemDto,
    isDefault: Boolean,
    progress: Int?,
    progressMsg: String?,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDefault) 4.dp else 1.dp),
        border = if (isDefault) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                when (kb.status) {
                                    "ready" -> listOf(Color(0xFF1565C0), Color(0xFF0097A7))
                                    "error" -> listOf(Color(0xFFC62828), Color(0xFFAD1457))
                                    else    -> listOf(Color(0xFFF57C00), Color(0xFFEF6C00))
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LibraryBooks, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(kb.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (isDefault) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                Text("mặc định", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        KbStatusChip(kb.status ?: "ready")
                        if (kb.fileCount > 0) {
                            Text("${kb.fileCount} tệp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (!isDefault) {
                            DropdownMenuItem(
                                text = { Text("Đặt làm mặc định") },
                                leadingIcon = { Icon(Icons.Default.Star, null) },
                                onClick = { onSetDefault(); showMenu = false }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Xóa", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { onDelete(); showMenu = false }
                        )
                    }
                }
            }

            // Progress bar while processing
            if (progress != null && kb.status != "ready") {
                Spacer(Modifier.height(10.dp))
                Text(progressMsg ?: "Đang xử lý…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun KbStatusChip(status: String) {
    val (color, label) = when (status) {
        "ready"      -> Pair(Color(0xFF2E7D32), "Sẵn sàng")
        "processing" -> Pair(MaterialTheme.colorScheme.primary, "Đang xử lý")
        "error"      -> Pair(MaterialTheme.colorScheme.error, "Lỗi")
        else         -> Pair(MaterialTheme.colorScheme.outline, status)
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.12f)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
private fun KbEmptyState() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 32.dp, end = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.LibraryBooks, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Spacer(Modifier.height(16.dp))
        Text("Chưa có kho tài liệu nào", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("Upload tài liệu PDF, DOCX… để AI có thể tìm kiếm và trích dẫn khi trả lời", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateKbSheet(isCreating: Boolean, onConfirm: (String, List<File>) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var kbName by remember { mutableStateOf("") }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedUris = uris
    }

    ModalBottomSheet(onDismissRequest = { if (!isCreating) onDismiss() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Tạo kho tài liệu mới", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = kbName,
                onValueChange = { kbName = it },
                label = { Text("Tên kho tài liệu *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedCard(
                onClick = { launcher.launch("*/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.UploadFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (selectedUris.isEmpty()) "Chọn tệp (PDF, DOCX, TXT…)" else "${selectedUris.size} tệp đã chọn",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedUris.isNotEmpty()) FontWeight.Medium else FontWeight.Normal
                        )
                        if (selectedUris.isEmpty()) {
                            Text("Nhấn để chọn từ thiết bị", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (selectedUris.isNotEmpty()) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32))
                    }
                }
            }

            Button(
                onClick = {
                    if (kbName.isNotBlank() && selectedUris.isNotEmpty()) {
                        val files = selectedUris.mapNotNull { uri ->
                            try {
                                val inputStream = context.contentResolver.openInputStream(uri) ?: return@mapNotNull null
                                val name = uri.lastPathSegment ?: "file_${System.currentTimeMillis()}"
                                val tempFile = File(context.cacheDir, name)
                                tempFile.outputStream().use { inputStream.copyTo(it) }
                                tempFile
                            } catch (e: Exception) { null }
                        }
                        onConfirm(kbName.trim(), files)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = kbName.isNotBlank() && selectedUris.isNotEmpty() && !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Tạo kho tài liệu", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
