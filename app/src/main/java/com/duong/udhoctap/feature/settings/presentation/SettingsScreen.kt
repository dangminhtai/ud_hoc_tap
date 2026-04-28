package com.duong.udhoctap.feature.settings.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChanged: (Boolean) -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToReminderSettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showHourPicker by remember { mutableStateOf(false) }
    var showGoalPicker by remember { mutableStateOf(false) }
    var showKbPicker by remember { mutableStateOf(false) }
    var backendUrlDraft by remember(state.backendUrl) { mutableStateOf(state.backendUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Section 1: Giao diện & Ứng dụng ─────────────────────────────
            SettingsSectionTitle("Giao diện")

            SettingsToggleItem(
                icon = Icons.Default.DarkMode,
                title = "Chế độ tối",
                subtitle = if (state.darkTheme) "Đang bật" else "Đang tắt",
                checked = state.darkTheme,
                onCheckedChange = { viewModel.setDarkTheme(it); onThemeChanged(it) }
            )

            SettingsClickItem(
                icon = Icons.Default.Language,
                title = "Ngôn ngữ",
                subtitle = if (state.language == "vi") "Tiếng Việt" else "English",
                onClick = {
                    viewModel.setLanguage(if (state.language == "vi") "en" else "vi")
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Section 2: Thông báo ──────────────────────────────────────────
            SettingsSectionTitle("Thông báo")

            SettingsToggleItem(
                icon = Icons.Default.Notifications,
                title = "Nhắc nhở học tập",
                subtitle = if (state.notificationsEnabled) "Đang bật" else "Đang tắt",
                checked = state.notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
            )

            SettingsClickItem(
                icon = Icons.Default.Schedule,
                title = "Giờ nhắc nhở",
                subtitle = "${state.reminderHour}:00",
                enabled = state.notificationsEnabled,
                onClick = { showHourPicker = true }
            )

            SettingsClickItem(
                icon = Icons.Default.NotificationsActive,
                title = "Cài đặt nhắc nhở chi tiết",
                subtitle = "Chế độ yên tĩnh, bộ lọc thông báo...",
                onClick = onNavigateToReminderSettings
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Section 3: Học tập ────────────────────────────────────────────
            SettingsSectionTitle("Học tập")

            SettingsClickItem(
                icon = Icons.Default.Flag,
                title = "Mục tiêu hàng ngày",
                subtitle = "${state.dailyGoal} thẻ/ngày",
                onClick = { showGoalPicker = true }
            )

            SettingsClickItem(
                icon = Icons.Default.Assessment,
                title = "Thống kê nâng cao",
                subtitle = "Biểu đồ, điểm yếu, lịch sử...",
                onClick = onNavigateToStats
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Section 4: Kết nối Backend ────────────────────────────────────
            SettingsSectionTitle("Kết nối Backend")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Địa chỉ máy chủ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                    OutlinedTextField(
                        value = backendUrlDraft,
                        onValueChange = { backendUrlDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("http://10.0.2.2:8001") },
                        shape = MaterialTheme.shapes.small,
                        trailingIcon = {
                            if (backendUrlDraft != state.backendUrl) {
                                TextButton(onClick = { viewModel.setBackendUrl(backendUrlDraft) }) {
                                    Text("Lưu", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ConnectionStatusChip(state.connectionStatus)
                        Button(
                            onClick = { viewModel.testConnection() },
                            enabled = state.connectionStatus != ConnectionStatus.TESTING,
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            if (state.connectionStatus == ConnectionStatus.TESTING) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text("Kiểm tra kết nối", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Section 5: AI & Kiến thức ─────────────────────────────────────
            SettingsSectionTitle("AI & Kiến thức")

            SettingsClickItem(
                icon = Icons.Default.LibraryBooks,
                title = "Kho tài liệu mặc định",
                subtitle = state.defaultKbName.ifBlank { "Chưa chọn" },
                onClick = { showKbPicker = true }
            )

            SettingsToggleItem(
                icon = Icons.Default.MenuBook,
                title = "Bật RAG theo mặc định",
                subtitle = "Tìm kiếm trong kho tài liệu khi chat/giải toán",
                checked = state.defaultEnableRag,
                onCheckedChange = { viewModel.setDefaultEnableRag(it) }
            )

            SettingsToggleItem(
                icon = Icons.Default.Language,
                title = "Tìm kiếm web theo mặc định",
                subtitle = "Bật web search khi khởi tạo chat",
                checked = state.defaultEnableWebSearch,
                onCheckedChange = { viewModel.setDefaultEnableWebSearch(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Section 6: Hệ thống ───────────────────────────────────────────
            SettingsSectionTitle("Hệ thống")

            SettingsClickItem(
                icon = Icons.Default.Search,
                title = "Tìm kiếm toàn cầu",
                subtitle = "Tìm thẻ ghi nhớ, bộ thẻ, tài liệu",
                onClick = onNavigateToSearch
            )

            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Đăng Xuất")
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    if (showHourPicker) {
        HourPickerDialog(
            currentHour = state.reminderHour,
            onConfirm = { viewModel.setReminderHour(it); showHourPicker = false },
            onDismiss = { showHourPicker = false }
        )
    }

    if (showGoalPicker) {
        GoalPickerDialog(
            currentGoal = state.dailyGoal,
            onConfirm = { viewModel.setDailyGoal(it); showGoalPicker = false },
            onDismiss = { showGoalPicker = false }
        )
    }

    if (showKbPicker && state.availableKbs.isNotEmpty()) {
        KbPickerDialog(
            kbs = state.availableKbs,
            current = state.defaultKbName,
            onConfirm = { viewModel.setDefaultKb(it); showKbPicker = false },
            onDismiss = { showKbPicker = false }
        )
    }
}

@Composable
private fun ConnectionStatusChip(status: ConnectionStatus) {
    val (color, label) = when (status) {
        ConnectionStatus.IDLE    -> Pair(MaterialTheme.colorScheme.outline, "Chưa kiểm tra")
        ConnectionStatus.TESTING -> Pair(MaterialTheme.colorScheme.primary, "Đang kiểm tra…")
        ConnectionStatus.OK      -> Pair(Color(0xFF2E7D32), "Kết nối thành công")
        ConnectionStatus.ERROR   -> Pair(MaterialTheme.colorScheme.error, "Không kết nối được")
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier.size(6.dp),
                shape = CircleShape,
                color = color
            ) {}
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(0.38f)
                )
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.38f)
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun HourPickerDialog(currentHour: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableIntStateOf(currentHour) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Giờ nhắc nhở", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Chọn giờ: $selected:00", style = MaterialTheme.typography.bodyLarge)
                Slider(value = selected.toFloat(), onValueChange = { selected = it.toInt() }, valueRange = 0f..23f, steps = 22)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0:00", style = MaterialTheme.typography.labelSmall)
                    Text("23:00", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Xác nhận", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun GoalPickerDialog(currentGoal: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(5, 10, 15, 20, 30, 50)
    var selected by remember { mutableIntStateOf(currentGoal) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mục tiêu hàng ngày", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { goal ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected == goal, onClick = { selected = goal })
                        Spacer(Modifier.width(8.dp))
                        Text("$goal thẻ/ngày", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Xác nhận", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun KbPickerDialog(kbs: List<String>, current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kho tài liệu mặc định", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected.isEmpty(), onClick = { selected = "" })
                    Spacer(Modifier.width(8.dp))
                    Text("Không dùng kho tài liệu", style = MaterialTheme.typography.bodyLarge)
                }
                kbs.forEach { kb ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected == kb, onClick = { selected = kb })
                        Spacer(Modifier.width(8.dp))
                        Text(kb, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Xác nhận", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}
