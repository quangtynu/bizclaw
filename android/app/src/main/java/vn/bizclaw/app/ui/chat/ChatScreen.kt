package vn.bizclaw.app.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenAgents: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenDashboard: () -> Unit = {},
    onOpenLocalLLM: () -> Unit = {},
) {
    val messages = viewModel.messages
    val isLoading by viewModel.isLoading
    val currentAgent by viewModel.currentAgent
    val isConnected by viewModel.isConnected
    val isLocalMode by viewModel.isLocalMode
    val localModelName by viewModel.localModelName
    val localGenSpeed by viewModel.localGenSpeed
    val localContextUsed by viewModel.localContextUsed
    val error by viewModel.error
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BizClaw", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Status dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isLocalMode && viewModel.localLLM.isLoaded -> Color(0xFF00E676) // Green
                                            isLocalMode -> Color(0xFFFF9800)   // Orange (local but no model)
                                            isConnected -> MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.error
                                        }
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isLocalMode && viewModel.localLLM.isLoaded ->
                                        "🧠 $currentAgent — ${localModelName ?: "local"}"
                                    isLocalMode -> "🧠 Local (no model)"
                                    isConnected -> "🤖 $currentAgent"
                                    else -> "Disconnected"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    // Toggle local/cloud mode
                    IconButton(onClick = {
                        if (isLocalMode) {
                            viewModel.isLocalMode.value = false
                            viewModel.currentAgent.value = "default"
                        } else if (viewModel.localLLM.isLoaded) {
                            viewModel.isLocalMode.value = true
                            viewModel.currentAgent.value = "local"
                        } else {
                            onOpenLocalLLM()
                        }
                    }) {
                        Icon(
                            Icons.Default.Memory,
                            contentDescription = "Local LLM",
                            tint = if (isLocalMode) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenDashboard) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Dashboard")
                    }
                    IconButton(onClick = onOpenAgents) {
                        Icon(Icons.Default.Group, contentDescription = "Agents")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Local mode performance stats bar
            AnimatedVisibility(visible = isLocalMode && localGenSpeed > 0) {
                Surface(
                    color = Color(0xFF1B5E20).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp, 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🧠", fontSize = 16.sp)
                        Text(
                            "⚡ %.1f tok/s".format(localGenSpeed),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676),
                        )
                        Text(
                            "📊 Context: $localContextUsed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "💰 $0",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00E676),
                        )
                    }
                }
            }

            // Error/info banner
            AnimatedVisibility(visible = error != null) {
                Surface(
                    color = if (error?.contains("auto-switch") == true)
                        Color(0xFF1B5E20).copy(alpha = 0.2f)
                    else
                        MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = error ?: "",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        IconButton(
                            onClick = { viewModel.error.value = null },
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(Icons.Default.Close, "Dismiss", Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Welcome message
                if (messages.isEmpty()) {
                    item {
                        WelcomeCard(isLocalMode = isLocalMode, localModelName = localModelName)
                    }
                }

                items(messages) { message ->
                    ChatBubble(message)
                }
            }

            // Input bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                if (isLocalMode) "Hỏi BizClaw local..."
                                else "Nhắn tin cho $currentAgent..."
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isLocalMode) Color(0xFF00E676)
                            else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isLocalMode) Color(0xFF00E676)
                            else MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: UiMessage) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            // Agent avatar
            Surface(
                shape = CircleShape,
                color = if (message.isLocal) Color(0xFF1B5E20) else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(32.dp)
                    .padding(top = 4.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (message.isLocal) "🧠" else "🤖", fontSize = 16.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            color = if (isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Agent name for assistant messages
                if (!isUser && message.agentName.isNotEmpty()) {
                    Text(
                        text = message.agentName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.isLocal) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(2.dp))
                }

                Text(
                    text = message.content.ifEmpty { "..." },
                    color = if (isUser)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )

                // Streaming indicator
                if (message.isStreaming) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = if (message.isLocal) Color(0xFF00E676) else MaterialTheme.colorScheme.secondary,
                    )
                }

                // Stats row (tokens or tok/s)
                if (message.tokensUsed > 0 || message.tokPerSec > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (message.tokPerSec > 0) {
                            Text(
                                text = "⚡ %.1f tok/s".format(message.tokPerSec),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00E676),
                            )
                        }
                        if (message.tokensUsed > 0) {
                            Text(
                                text = "${message.tokensUsed} tokens",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                        if (message.isLocal) {
                            Text(
                                text = "📱 local",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00E676).copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeCard(isLocalMode: Boolean = false, localModelName: String? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (isLocalMode) "🧠" else "🤖", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                if (isLocalMode) "BizClaw Local AI"
                else "Chào mừng đến với BizClaw",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (isLocalMode)
                    "On-Device • ${localModelName ?: "No model"} • $0 cost"
                else
                    "AI Agent Platform — Nhẹ, Nhanh, Tiết Kiệm",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (isLocalMode)
                    "100% offline, no API keys. Your data stays on your phone."
                else
                    "Gửi tin nhắn để bắt đầu trò chuyện với agent.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
