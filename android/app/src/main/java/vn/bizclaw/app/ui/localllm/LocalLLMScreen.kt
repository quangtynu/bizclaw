package vn.bizclaw.app.ui.localllm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import vn.bizclaw.app.engine.*

/**
 * Local LLM Screen — Download GGUF models and run AI inference on-device.
 *
 * Features:
 * - Browse recommended models (Qwen3, TinyLlama, DeepSeek)
 * - Download with progress tracking
 * - Manage downloaded models (load/unload/delete)
 * - Quick chat to test loaded model
 * - Storage usage display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalLLMScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Initialize managers
    val downloadManager = remember { ModelDownloadManager(context) }
    val downloadState by downloadManager.downloadState.collectAsState(initial = DownloadState.Idle)
    val downloadedModels by downloadManager.downloadedModels.collectAsState(initial = emptyList())

    // Local state
    var testPrompt by remember { mutableStateOf("") }
    var testResponse by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<LocalModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🧠 Local LLM", fontWeight = FontWeight.Bold)
                        Text(
                            if (PicoLMEngine.isLoaded) "Model loaded • ${PicoLMEngine.modelInfo?.nParams ?: 0} params"
                            else "No model loaded",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1117),
                    titleContentColor = Color.White,
                ),
            )
        },
        containerColor = Color(0xFF0D1117),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Storage Info ──
            item {
                StorageCard(downloadManager)
            }

            // ── Downloaded Models ──
            if (downloadedModels.isNotEmpty()) {
                item {
                    Text(
                        "📦 Downloaded Models",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }

                items(downloadedModels) { model ->
                    DownloadedModelCard(
                        model = model,
                        isCurrentlyLoaded = PicoLMEngine.isLoaded && PicoLMEngine.modelInfo != null,
                        onLoad = {
                            scope.launch {
                                val threads = Runtime.getRuntime().availableProcessors() / 2
                                PicoLMEngine.loadModel(model.path, threads.coerceAtLeast(2))
                            }
                        },
                        onUnload = { PicoLMEngine.unloadModel() },
                        onDelete = { showDeleteDialog = model },
                    )
                }
            }

            // ── Quick Test ──
            if (PicoLMEngine.isLoaded) {
                item {
                    Text(
                        "💬 Quick Test",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }

                item {
                    QuickTestCard(
                        prompt = testPrompt,
                        response = testResponse,
                        isGenerating = isGenerating,
                        onPromptChange = { testPrompt = it },
                        onGenerate = {
                            scope.launch {
                                isGenerating = true
                                testResponse = ""
                                val formatted = PicoLMEngine.formatQwen3(
                                    systemPrompt = "You are a helpful assistant.",
                                    messages = listOf("user" to testPrompt)
                                )
                                testResponse = PicoLMEngine.generate(
                                    prompt = formatted,
                                    maxTokens = 256,
                                    temperature = 0.7f
                                )
                                isGenerating = false
                            }
                        },
                        onCancel = {
                            PicoLMEngine.cancelGeneration()
                            isGenerating = false
                        },
                    )
                }
            }

            // ── Available Models to Download ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "🌐 Available Models",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    "Download from HuggingFace — runs 100% offline after download",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                )
            }

            items(PicoLMEngine.RECOMMENDED_MODELS) { model ->
                val isDownloaded = downloadedModels.any { it.fileName == model.url.substringAfterLast("/") }
                AvailableModelCard(
                    model = model,
                    isDownloaded = isDownloaded,
                    downloadState = downloadState,
                    onDownload = { downloadManager.downloadModel(model) },
                    onCancel = { downloadManager.cancelDownload() },
                )
            }

            // Bottom spacer
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { model ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete ${model.name}?") },
            text = { Text("This will delete the ${model.sizeDisplay} model file from your device.") },
            confirmButton = {
                TextButton(onClick = {
                    downloadManager.deleteModel(model)
                    showDeleteDialog = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            },
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Sub-components
// ═══════════════════════════════════════════════════════════════

@Composable
private fun StorageCard(manager: ModelDownloadManager) {
    val usedSpace = remember { manager.getUsedSpace() }
    val freeSpace = remember { manager.getAvailableSpace() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Storage, "Storage", tint = Color(0xFF6366F1))

            Column(modifier = Modifier.weight(1f)) {
                Text("Storage", color = Color.White, fontWeight = FontWeight.Medium)
                Text(
                    "Models: ${usedSpace / 1_000_000}MB • Free: ${freeSpace / 1_000_000_000}GB",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                )
            }

            val cpuCores = Runtime.getRuntime().availableProcessors()
            Text("$cpuCores cores", color = Color(0xFF6366F1), fontSize = 12.sp)
        }
    }
}

@Composable
private fun DownloadedModelCard(
    model: LocalModel,
    isCurrentlyLoaded: Boolean,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyLoaded) Color(0xFF1A2332) else Color(0xFF161B22)
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        model.name,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isCurrentlyLoaded) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "LOADED",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    "${model.sizeDisplay} • ${model.fileName}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                )
            }

            // Load/Unload button
            if (isCurrentlyLoaded) {
                IconButton(onClick = onUnload) {
                    Icon(Icons.Default.Stop, "Unload", tint = Color(0xFFF59E0B))
                }
            } else {
                IconButton(onClick = onLoad) {
                    Icon(Icons.Default.PlayArrow, "Load", tint = Color(0xFF10B981))
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFEF4444))
            }
        }
    }
}

@Composable
private fun QuickTestCard(
    prompt: String,
    response: String,
    isGenerating: Boolean,
    onPromptChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enter a prompt...") },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF6366F1),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                ),
            )

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (isGenerating) {
                    OutlinedButton(onClick = onCancel) {
                        Text("⏹ Cancel")
                    }
                } else {
                    Button(
                        onClick = onGenerate,
                        enabled = prompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1)
                        ),
                    ) {
                        Text("⚡ Generate")
                    }
                }
            }

            if (response.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Divider(color = Color(0xFF30363D))
                Spacer(Modifier.height(12.dp))
                Text(
                    response,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                )
            }

            if (isGenerating) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF6366F1),
                    trackColor = Color(0xFF30363D),
                )
            }
        }
    }
}

@Composable
private fun AvailableModelCard(
    model: DownloadableModel,
    isDownloaded: Boolean,
    downloadState: DownloadState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.name, color = Color.White, fontWeight = FontWeight.Medium)
                    Text(
                        model.description,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                    )
                }

                // Size badge
                Text(
                    model.sizeDisplay,
                    color = Color(0xFF6366F1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFF6366F1).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Tags row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Tag(model.paramCount, Color(0xFF10B981))
                Tag(model.quantization, Color(0xFFF59E0B))
                Tag(model.chatTemplate, Color(0xFF6366F1))
            }

            Spacer(Modifier.height(12.dp))

            // Action button
            if (isDownloaded) {
                Text(
                    "✅ Downloaded",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Medium,
                )
            } else {
                when (downloadState) {
                    is DownloadState.Downloading -> {
                        Column {
                            LinearProgressIndicator(
                                progress = { downloadState.progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF6366F1),
                                trackColor = Color(0xFF30363D),
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    downloadState.downloadedDisplay,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                )
                                TextButton(onClick = onCancel) {
                                    Text("Cancel", color = Color(0xFFEF4444), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1)
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(Icons.Default.Download, "Download", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Download ${model.sizeDisplay}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
