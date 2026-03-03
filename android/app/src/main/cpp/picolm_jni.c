/**
 * PicoLM JNI Bridge — connects Kotlin to the picolm C inference engine.
 *
 * Architecture:
 *   Kotlin PicoLMEngine.kt → JNI → picolm_jni.c → picolm C engine
 *
 * Functions exposed to Kotlin:
 *   1. picolm_load_model(path)     → load GGUF model via mmap
 *   2. picolm_generate(prompt, max_tokens, temp, top_p, json_mode, callback)
 *   3. picolm_unload_model()       → free resources
 *   4. picolm_get_info()           → model name, params, quant
 *   5. picolm_set_threads(n)       → configure thread count
 *
 * Thread safety: model access serialized, generation runs on calling thread
 * (Kotlin dispatches to Dispatchers.Default coroutine)
 */

#include <jni.h>
#include <android/log.h>
#include <string.h>
#include <stdlib.h>
#include <pthread.h>

// PicoLM headers
#include "model.h"
#include "tokenizer.h"
#include "sampler.h"
#include "grammar.h"

#define TAG "PicoLM"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// ════════════════════════════════════════════════════════════════
// Global state — single model loaded at a time
// ════════════════════════════════════════════════════════════════

static Model *g_model = NULL;
static Tokenizer *g_tokenizer = NULL;
static pthread_mutex_t g_mutex = PTHREAD_MUTEX_INITIALIZER;
static volatile int g_cancel = 0;
static int g_num_threads = 4; // default, updated based on device cores

// ════════════════════════════════════════════════════════════════
// JNI: Load Model
// ════════════════════════════════════════════════════════════════

JNIEXPORT jboolean JNICALL
Java_vn_bizclaw_app_engine_PicoLMEngine_nativeLoadModel(
    JNIEnv *env, jobject thiz, jstring model_path) {

    pthread_mutex_lock(&g_mutex);

    // Unload previous model if any
    if (g_model) {
        model_free(g_model);
        g_model = NULL;
    }
    if (g_tokenizer) {
        tokenizer_free(g_tokenizer);
        g_tokenizer = NULL;
    }

    const char *path = (*env)->GetStringUTFChars(env, model_path, NULL);
    LOGI("Loading model: %s", path);

    // Load GGUF model via mmap — minimal RAM usage
    g_model = model_load(path, g_num_threads);
    if (!g_model) {
        LOGE("Failed to load model: %s", path);
        (*env)->ReleaseStringUTFChars(env, model_path, path);
        pthread_mutex_unlock(&g_mutex);
        return JNI_FALSE;
    }

    // Initialize tokenizer from the same GGUF file
    g_tokenizer = tokenizer_load(path);
    if (!g_tokenizer) {
        LOGE("Failed to load tokenizer from: %s", path);
        model_free(g_model);
        g_model = NULL;
        (*env)->ReleaseStringUTFChars(env, model_path, path);
        pthread_mutex_unlock(&g_mutex);
        return JNI_FALSE;
    }

    LOGI("Model loaded successfully — %d params, %d layers",
         g_model->config.n_params, g_model->config.n_layers);

    (*env)->ReleaseStringUTFChars(env, model_path, path);
    pthread_mutex_unlock(&g_mutex);
    return JNI_TRUE;
}

// ════════════════════════════════════════════════════════════════
// JNI: Generate (streaming via callback)
// ════════════════════════════════════════════════════════════════

JNIEXPORT jstring JNICALL
Java_vn_bizclaw_app_engine_PicoLMEngine_nativeGenerate(
    JNIEnv *env, jobject thiz,
    jstring prompt_str,
    jint max_tokens,
    jfloat temperature,
    jfloat top_p,
    jboolean json_mode,
    jobject callback) {

    if (!g_model || !g_tokenizer) {
        return (*env)->NewStringUTF(env, "[ERROR] No model loaded");
    }

    const char *prompt = (*env)->GetStringUTFChars(env, prompt_str, NULL);
    g_cancel = 0;

    // Tokenize prompt
    int *tokens = NULL;
    int n_tokens = tokenizer_encode(g_tokenizer, prompt, &tokens);
    (*env)->ReleaseStringUTFChars(env, prompt_str, prompt);

    if (n_tokens <= 0 || !tokens) {
        return (*env)->NewStringUTF(env, "[ERROR] Tokenization failed");
    }

    // Configure sampler
    Sampler sampler;
    sampler_init(&sampler, g_tokenizer->vocab_size, temperature, top_p);

    // Configure grammar constraints for JSON mode
    Grammar *grammar = NULL;
    if (json_mode) {
        grammar = grammar_init(g_tokenizer);
    }

    // Prefill — process prompt tokens
    for (int i = 0; i < n_tokens; i++) {
        model_forward(g_model, tokens[i], i);
    }

    // Get callback method ID for streaming
    jclass callbackClass = (*env)->GetObjectClass(env, callback);
    jmethodID onTokenMethod = (*env)->GetMethodID(env, callbackClass, "onToken", "(Ljava/lang/String;)V");
    jmethodID onCompleteMethod = (*env)->GetMethodID(env, callbackClass, "onComplete", "(Ljava/lang/String;)V");

    // Build response
    char *response = (char *)calloc(max_tokens * 32, sizeof(char)); // generous buffer
    int response_len = 0;
    int pos = n_tokens;

    // Generate tokens
    int prev_token = tokens[n_tokens - 1];
    for (int i = 0; i < max_tokens && !g_cancel; i++) {
        float *logits = model_forward(g_model, prev_token, pos);

        // Apply grammar mask if JSON mode
        if (grammar) {
            grammar_mask_logits(grammar, logits, g_tokenizer->vocab_size);
        }

        int next_token = sampler_sample(&sampler, logits, g_tokenizer->vocab_size);

        // Check for EOS
        if (next_token == g_tokenizer->eos_token) {
            break;
        }

        // Decode token to string
        const char *token_str = tokenizer_decode(g_tokenizer, next_token);
        if (token_str) {
            int len = strlen(token_str);
            memcpy(response + response_len, token_str, len);
            response_len += len;
            response[response_len] = '\0';

            // Stream token to Kotlin callback
            if (onTokenMethod && callback) {
                jstring jtoken = (*env)->NewStringUTF(env, token_str);
                (*env)->CallVoidMethod(env, callback, onTokenMethod, jtoken);
                (*env)->DeleteLocalRef(env, jtoken);
            }
        }

        // Update grammar state
        if (grammar) {
            grammar_accept(grammar, next_token);
        }

        prev_token = next_token;
        pos++;
    }

    // Notify completion
    if (onCompleteMethod && callback) {
        jstring jfull = (*env)->NewStringUTF(env, response);
        (*env)->CallVoidMethod(env, callback, onCompleteMethod, jfull);
        (*env)->DeleteLocalRef(env, jfull);
    }

    // Cleanup
    jstring result = (*env)->NewStringUTF(env, response);
    free(response);
    free(tokens);
    sampler_free(&sampler);
    if (grammar) grammar_free(grammar);

    return result;
}

// ════════════════════════════════════════════════════════════════
// JNI: Cancel generation
// ════════════════════════════════════════════════════════════════

JNIEXPORT void JNICALL
Java_vn_bizclaw_app_engine_PicoLMEngine_nativeCancelGeneration(
    JNIEnv *env, jobject thiz) {
    g_cancel = 1;
    LOGI("Generation cancelled by user");
}

// ════════════════════════════════════════════════════════════════
// JNI: Unload Model
// ════════════════════════════════════════════════════════════════

JNIEXPORT void JNICALL
Java_vn_bizclaw_app_engine_PicoLMEngine_nativeUnloadModel(
    JNIEnv *env, jobject thiz) {

    pthread_mutex_lock(&g_mutex);
    if (g_model) {
        model_free(g_model);
        g_model = NULL;
        LOGI("Model unloaded");
    }
    if (g_tokenizer) {
        tokenizer_free(g_tokenizer);
        g_tokenizer = NULL;
    }
    pthread_mutex_unlock(&g_mutex);
}

// ════════════════════════════════════════════════════════════════
// JNI: Get Model Info
// ════════════════════════════════════════════════════════════════

JNIEXPORT jstring JNICALL
Java_vn_bizclaw_app_engine_PicoLMEngine_nativeGetModelInfo(
    JNIEnv *env, jobject thiz) {

    if (!g_model) {
        return (*env)->NewStringUTF(env, "{}");
    }

    char info[512];
    snprintf(info, sizeof(info),
        "{\"n_params\":%d,\"n_layers\":%d,\"n_heads\":%d,"
        "\"n_embd\":%d,\"vocab_size\":%d,\"context_length\":%d,"
        "\"threads\":%d}",
        g_model->config.n_params,
        g_model->config.n_layers,
        g_model->config.n_heads,
        g_model->config.n_embd,
        g_model->config.vocab_size,
        g_model->config.n_ctx,
        g_num_threads
    );

    return (*env)->NewStringUTF(env, info);
}

// ════════════════════════════════════════════════════════════════
// JNI: Set Thread Count
// ════════════════════════════════════════════════════════════════

JNIEXPORT void JNICALL
Java_vn_bizclaw_app_engine_PicoLMEngine_nativeSetThreads(
    JNIEnv *env, jobject thiz, jint threads) {

    g_num_threads = threads > 0 ? threads : 1;
    if (g_model) {
        g_model->n_threads = g_num_threads;
    }
    LOGI("Thread count set to %d", g_num_threads);
}

// ════════════════════════════════════════════════════════════════
// JNI: Check if model is loaded
// ════════════════════════════════════════════════════════════════

JNIEXPORT jboolean JNICALL
Java_vn_bizclaw_app_engine_PicoLMEngine_nativeIsModelLoaded(
    JNIEnv *env, jobject thiz) {
    return g_model != NULL ? JNI_TRUE : JNI_FALSE;
}
