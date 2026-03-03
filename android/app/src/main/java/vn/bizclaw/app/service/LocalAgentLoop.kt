package vn.bizclaw.app.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import vn.bizclaw.app.engine.BizClawLLM

/**
 * LocalAgentLoop — Think-Act-Observe loop powered by on-device LLM.
 *
 * This is the CORE of BizClaw Android running 100% offline:
 *
 * ```
 *  User query
 *    ↓
 *  LLM thinks → generates response with optional <tool_call>
 *    ↓
 *  Parse tool_call → ToolDispatcher executes
 *    ↓
 *  Feed result back to LLM as "tool" message
 *    ↓
 *  Repeat until LLM responds without tool_call (max 5 rounds)
 *    ↓
 *  Return final text to user
 * ```
 *
 * Architecture:
 *   LocalAgentLoop → BizClawLLM (llama.cpp) → ToolDispatcher → AppController/AccessibilityService
 *
 * Everything runs ON THE PHONE. No server. No API keys. $0 cost.
 */
class LocalAgentLoop(
    private val llm: BizClawLLM,
    private val context: Context,
    private val maxRounds: Int = 5,
) {
    private val tag = "LocalAgentLoop"
    private val dispatcher = ToolDispatcher(context)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Tool call parsing regex: <tool_call>...</tool_call>
    private val toolCallRegex = Regex(
        """<tool_call>\s*\{.*?}\s*</tool_call>""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )

    // Alternative format: ```json ... ``` with "name" and "arguments"
    private val jsonToolCallRegex = Regex(
        """\{"name"\s*:\s*"([^"]+)"\s*,\s*"arguments"\s*:\s*(\{.*?})\s*}""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )

    /**
     * System prompt that teaches the LLM about available tools.
     * Injected when loading the model.
     */
    val agentSystemPrompt: String = buildString {
        appendLine("You are BizClaw, an AI assistant running directly on an Android phone.")
        appendLine("You can control the phone, post on social media, send messages, and more.")
        appendLine("You respond in the user's language (Vietnamese or English).")
        appendLine()
        appendLine("IMPORTANT RULES:")
        appendLine("1. When you need to perform an action, use a tool call.")
        appendLine("2. After each tool call, wait for the result before proceeding.")
        appendLine("3. Always confirm what you did after completing an action.")
        appendLine("4. If a tool fails, try an alternative approach or inform the user.")
        appendLine("5. Be concise but helpful.")
        appendLine()
        append(dispatcher.toolDefinitions)
    }

    /**
     * Run the agent loop for a user query.
     *
     * Returns a Flow that emits:
     * - Partial text tokens (for streaming display)
     * - Special markers: [TOOL_START], [TOOL_END], [ROUND_N]
     *
     * @param query User's message
     * @return Flow of streaming tokens + final response
     */
    fun run(query: String): Flow<AgentToken> = flow {
        var round = 0
        var currentQuery = query
        var pendingToolCalls = true

        while (pendingToolCalls && round < maxRounds) {
            round++
            Log.i(tag, "🔄 Round $round — processing: ${currentQuery.take(80)}")
            emit(AgentToken.Round(round))

            // === THINK: Get LLM response ===
            val responseBuilder = StringBuilder()
            llm.getResponseAsFlow(currentQuery)
                .flowOn(Dispatchers.IO)
                .collect { token ->
                    responseBuilder.append(token)
                    emit(AgentToken.Text(token))
                }

            val fullResponse = responseBuilder.toString()
            Log.d(tag, "📝 LLM response ($round): ${fullResponse.take(200)}")

            // === ACT: Check for tool calls ===
            val toolCalls = parseToolCalls(fullResponse)

            if (toolCalls.isEmpty()) {
                // No tool calls → LLM is done, this is the final answer
                pendingToolCalls = false
                Log.i(tag, "✅ Round $round — final answer (no tool calls)")
            } else {
                // Execute tool calls
                val toolResults = StringBuilder()
                for (call in toolCalls) {
                    Log.i(tag, "🔧 Executing tool: ${call.name}")
                    emit(AgentToken.ToolStart(call.name))

                    val result = withContext(Dispatchers.Main) {
                        dispatcher.dispatch(call.name, call.arguments)
                    }

                    Log.i(tag, "📋 Tool result: ${result.message.take(100)}")
                    emit(AgentToken.ToolEnd(call.name, result))

                    toolResults.appendLine("Tool '${call.name}' result:")
                    toolResults.appendLine(if (result.success) "✅ ${result.message}" else "❌ ${result.message}")
                    toolResults.appendLine()
                }

                // === OBSERVE: Feed results back to LLM ===
                currentQuery = toolResults.toString().trimEnd()
                // Add tool result as a message (BizClawLLM.addChatMessage handles this)
                llm.addSystemPrompt("") // Clear for next round
            }
        }

        if (round >= maxRounds && pendingToolCalls) {
            emit(AgentToken.Text("\n\n⚠️ Đã đạt giới hạn $maxRounds rounds. Hãy thử lại với yêu cầu đơn giản hơn."))
        }

        emit(AgentToken.Done(round))
    }

    // ═══════════════════════════════════════════════════════════════
    // Tool Call Parsing
    // ═══════════════════════════════════════════════════════════════

    private fun parseToolCalls(response: String): List<ParsedToolCall> {
        val calls = mutableListOf<ParsedToolCall>()

        // Method 1: <tool_call>{...}</tool_call>
        toolCallRegex.findAll(response).forEach { match ->
            val jsonStr = match.value
                .removePrefix("<tool_call>")
                .removeSuffix("</tool_call>")
                .trim()
            parseJsonToolCall(jsonStr)?.let { calls.add(it) }
        }

        // Method 2: {"name": "...", "arguments": {...}}  (without tags)
        if (calls.isEmpty()) {
            jsonToolCallRegex.findAll(response).forEach { match ->
                val name = match.groupValues[1]
                val argsStr = match.groupValues[2]
                try {
                    val args = json.parseToJsonElement(argsStr).jsonObject
                    calls.add(ParsedToolCall(name, args))
                } catch (e: Exception) {
                    Log.w(tag, "Failed to parse tool args: $argsStr", e)
                }
            }
        }

        return calls
    }

    private fun parseJsonToolCall(jsonStr: String): ParsedToolCall? {
        return try {
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            val name = obj["name"]?.jsonPrimitive?.content ?: return null
            val args = obj["arguments"]?.jsonObject ?: JsonObject(emptyMap())
            ParsedToolCall(name, args)
        } catch (e: Exception) {
            Log.w(tag, "Failed to parse tool call JSON: $jsonStr", e)
            null
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Data Types
// ═══════════════════════════════════════════════════════════════

data class ParsedToolCall(
    val name: String,
    val arguments: JsonObject,
)

/**
 * Tokens emitted by the agent loop for UI consumption.
 */
sealed class AgentToken {
    /** Regular text token from LLM */
    data class Text(val content: String) : AgentToken()

    /** Tool execution started */
    data class ToolStart(val toolName: String) : AgentToken()

    /** Tool execution completed */
    data class ToolEnd(val toolName: String, val result: ToolResult) : AgentToken()

    /** New thinking round started */
    data class Round(val number: Int) : AgentToken()

    /** Agent loop completed */
    data class Done(val totalRounds: Int) : AgentToken()
}
