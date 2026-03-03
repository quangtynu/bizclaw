package vn.bizclaw.app.engine

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * PicoLM Engine — On-device LLM inference for BizClaw Android.
 *
 * Architecture:
 *   PicoLMEngine.kt → JNI → picolm_jni.c → picolm C engine (mmap GGUF)
 *
 * Key features:
 * - Load GGUF models from device storage (mmap = minimal RAM)
 * - Stream tokens in real-time to UI
 * - JSON mode for tool-calling with grammar constraints
 * - Cancel generation mid-stream
 * - ARM NEON SIMD acceleration (auto-detected)
 * - Supports Qwen3, TinyLlama, DeepSeek, and any LLaMA-architecture GGUF
 *
 * Memory usage: ~45MB runtime + model on disk via mmap
 * Quantization: Q2_K, Q3_K, Q4_K, Q4_0, Q5_K, Q6_K, Q8_0, F16, F32
 */
object PicoLMEngine {
    private const val TAG = "PicoLM"

    // Native library
    init {
        try {
            System.loadLibrary("picolm_jni")
            Log.i(TAG, "⚡ PicoLM native library loaded")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load picolm_jni: ${e.message}")
        }
    }

    // ── Token streaming ──
    private val _tokenFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val tokenFlow: Flow<String> = _tokenFlow.asSharedFlow()

    // ── State ──
    var isLoaded: Boolean = false
        private set
    var isGenerating: Boolean = false
        private set
    var modelInfo: ModelInfo? = null
        private set

    // ═══════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════

    /**
     * Load a GGUF model from device storage.
     * Uses mmap — model stays on disk, only ~45MB RAM used.
     *
     * @param modelPath absolute path to .gguf file on device
     * @param threads number of CPU threads (default: device cores / 2)
     */
    suspend fun loadModel(modelPath: String, threads: Int = 4): Boolean {
        return withContext(Dispatchers.IO) {
            Log.i(TAG, "Loading model: $modelPath with $threads threads")
            nativeSetThreads(threads)
            val success = nativeLoadModel(modelPath)
            if (success) {
                isLoaded = true
                val infoJson = nativeGetModelInfo()
                modelInfo = try {
                    Json.decodeFromString<ModelInfo>(infoJson)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse model info: $infoJson")
                    null
                }
                Log.i(TAG, "✅ Model loaded: ${modelInfo?.nParams} params, ${modelInfo?.nLayers} layers")
            } else {
                isLoaded = false
                Log.e(TAG, "❌ Failed to load model")
            }
            success
        }
    }

    /**
     * Generate text from prompt with real-time token streaming.
     *
     * @param prompt the input prompt (use ChatML format for chat models)
     * @param maxTokens max tokens to generate (default 512)
     * @param temperature sampling temperature (0.0 = deterministic, 1.0 = creative)
     * @param topP nucleus sampling (0.9 is a good default)
     * @param jsonMode enable grammar-constrained JSON output for tool-calling
     * @return full generated text
     */
    suspend fun generate(
        prompt: String,
        maxTokens: Int = 512,
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        jsonMode: Boolean = false,
    ): String {
        if (!isLoaded) {
            return "[ERROR] No model loaded. Call loadModel() first."
        }
        if (isGenerating) {
            return "[ERROR] Generation already in progress."
        }

        return withContext(Dispatchers.Default) {
            isGenerating = true
            try {
                val callback = object : GenerationCallback {
                    override fun onToken(token: String) {
                        _tokenFlow.tryEmit(token)
                    }
                    override fun onComplete(fullText: String) {
                        // Final notification
                    }
                }

                val result = nativeGenerate(prompt, maxTokens, temperature, topP, jsonMode, callback)
                Log.i(TAG, "Generation complete: ${result.length} chars")
                result
            } finally {
                isGenerating = false
            }
        }
    }

    /**
     * Cancel ongoing generation.
     */
    fun cancelGeneration() {
        if (isGenerating) {
            nativeCancelGeneration()
            isGenerating = false
            Log.i(TAG, "🛑 Generation cancelled")
        }
    }

    /**
     * Unload model and free native resources.
     */
    fun unloadModel() {
        nativeUnloadModel()
        isLoaded = false
        modelInfo = null
        Log.i(TAG, "Model unloaded")
    }

    /**
     * Format a chat prompt using ChatML template.
     * Works with Qwen3, TinyLlama, and other ChatML models.
     */
    fun formatChatML(
        systemPrompt: String,
        messages: List<Pair<String, String>>, // role -> content
    ): String {
        val sb = StringBuilder()

        // System prompt
        if (systemPrompt.isNotBlank()) {
            sb.append("<|system|>\n$systemPrompt</s>\n")
        }

        // Chat history
        for ((role, content) in messages) {
            sb.append("<|$role|>\n$content</s>\n")
        }

        // Assistant turn
        sb.append("<|assistant|>\n")
        return sb.toString()
    }

    /**
     * Format prompt for Qwen3 models.
     * Qwen uses a slightly different chat template.
     */
    fun formatQwen3(
        systemPrompt: String,
        messages: List<Pair<String, String>>,
    ): String {
        val sb = StringBuilder()

        if (systemPrompt.isNotBlank()) {
            sb.append("<|im_start|>system\n$systemPrompt<|im_end|>\n")
        }

        for ((role, content) in messages) {
            sb.append("<|im_start|>$role\n$content<|im_end|>\n")
        }

        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    // ═══════════════════════════════════════════════════════════
    // Recommended models for on-device inference
    // ═══════════════════════════════════════════════════════════

    val RECOMMENDED_MODELS = listOf(
        DownloadableModel(
            name = "Qwen3 4B Q4_K_M",
            description = "Qwen3 4B — Best balance of speed and quality for mobile",
            url = "https://huggingface.co/Qwen/Qwen3-4B-GGUF/resolve/main/qwen3-4b-q4_k_m.gguf",
            sizeBytes = 2_700_000_000L,
            paramCount = "4B",
            quantization = "Q4_K_M",
            chatTemplate = "qwen3",
        ),
        DownloadableModel(
            name = "Qwen3 1.7B Q4_K_M",
            description = "Qwen3 1.7B — Fast, lightweight, good for basic tasks",
            url = "https://huggingface.co/Qwen/Qwen3-1.7B-GGUF/resolve/main/qwen3-1.7b-q4_k_m.gguf",
            sizeBytes = 1_200_000_000L,
            paramCount = "1.7B",
            quantization = "Q4_K_M",
            chatTemplate = "qwen3",
        ),
        DownloadableModel(
            name = "Qwen3 8B Q4_K_M",
            description = "Qwen3 8B — Powerful, needs 8GB+ RAM phone with NPU",
            url = "https://huggingface.co/Qwen/Qwen3-8B-GGUF/resolve/main/qwen3-8b-q4_k_m.gguf",
            sizeBytes = 5_100_000_000L,
            paramCount = "8B",
            quantization = "Q4_K_M",
            chatTemplate = "qwen3",
        ),
        DownloadableModel(
            name = "TinyLlama 1.1B Q4_K_M",
            description = "TinyLlama — Smallest, runs on any phone, 638MB",
            url = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            sizeBytes = 638_000_000L,
            paramCount = "1.1B",
            quantization = "Q4_K_M",
            chatTemplate = "chatml",
        ),
        DownloadableModel(
            name = "DeepSeek R1 1.5B Q4_K_M",
            description = "DeepSeek R1 — Reasoning model, great for logic tasks",
            url = "https://huggingface.co/bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            sizeBytes = 1_100_000_000L,
            paramCount = "1.5B",
            quantization = "Q4_K_M",
            chatTemplate = "qwen3",
        ),
    )

    // ═══════════════════════════════════════════════════════════
    // Native JNI methods
    // ═══════════════════════════════════════════════════════════

    private external fun nativeLoadModel(modelPath: String): Boolean
    private external fun nativeGenerate(
        prompt: String, maxTokens: Int,
        temperature: Float, topP: Float,
        jsonMode: Boolean, callback: GenerationCallback
    ): String
    private external fun nativeCancelGeneration()
    private external fun nativeUnloadModel()
    private external fun nativeGetModelInfo(): String
    private external fun nativeSetThreads(threads: Int)
    private external fun nativeIsModelLoaded(): Boolean
}

// ═══════════════════════════════════════════════════════════════
// Data classes
// ═══════════════════════════════════════════════════════════════

@Serializable
data class ModelInfo(
    val n_params: Int = 0,
    val n_layers: Int = 0,
    val n_heads: Int = 0,
    val n_embd: Int = 0,
    val vocab_size: Int = 0,
    val context_length: Int = 0,
    val threads: Int = 0,
) {
    val nParams get() = n_params
    val nLayers get() = n_layers
}

data class DownloadableModel(
    val name: String,
    val description: String,
    val url: String,
    val sizeBytes: Long,
    val paramCount: String,
    val quantization: String,
    val chatTemplate: String,
) {
    val sizeDisplay: String
        get() {
            val gb = sizeBytes / 1_000_000_000.0
            return if (gb >= 1.0) "%.1f GB".format(gb) else "${sizeBytes / 1_000_000} MB"
        }
}

/**
 * Callback interface for streaming generation.
 * Implemented in JNI bridge — C code calls these methods.
 */
interface GenerationCallback {
    fun onToken(token: String)
    fun onComplete(fullText: String)
}
