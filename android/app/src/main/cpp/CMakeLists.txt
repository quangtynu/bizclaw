cmake_minimum_required(VERSION 3.18)
project(picolm_jni C)

# ════════════════════════════════════════════════════════════════
# PicoLM JNI — Build picolm C engine + JNI bridge for Android NDK
#
# Architecture:
#   picolm C engine → JNI bridge (picolm_jni.c) → Kotlin (PicoLMEngine.kt)
#
# Supported ABIs: arm64-v8a (primary), armeabi-v7a, x86_64
# ════════════════════════════════════════════════════════════════

set(CMAKE_C_STANDARD 11)

# ── Compiler flags ──
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -ffast-math -DNDEBUG")

# ARM NEON SIMD — critical for performance on mobile
if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DUSE_NEON=1 -march=armv8-a+fp+simd")
    message(STATUS "PicoLM: ARM64 with NEON SIMD enabled")
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "armeabi-v7a")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DUSE_NEON=1 -mfpu=neon -mfloat-abi=softfp")
    message(STATUS "PicoLM: ARMv7 with NEON SIMD enabled")
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "x86_64")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DUSE_SSE2=1 -msse2")
    message(STATUS "PicoLM: x86_64 with SSE2 enabled")
endif()

# ── PicoLM source files ──
# These will be cloned from github.com/RightNow-AI/picolm into picolm/
set(PICOLM_DIR ${CMAKE_SOURCE_DIR}/picolm)

# Create stub sources if picolm not yet cloned
# The actual sources are: model.c, tensor.c, quant.c, tokenizer.c, sampler.c, grammar.c
set(PICOLM_SOURCES
    ${PICOLM_DIR}/model.c
    ${PICOLM_DIR}/tensor.c
    ${PICOLM_DIR}/quant.c
    ${PICOLM_DIR}/tokenizer.c
    ${PICOLM_DIR}/sampler.c
    ${PICOLM_DIR}/grammar.c
)

# ── JNI Bridge ──
add_library(picolm_jni SHARED
    picolm_jni.c
    ${PICOLM_SOURCES}
)

target_include_directories(picolm_jni PRIVATE
    ${PICOLM_DIR}
)

# Link Android log + pthreads
target_link_libraries(picolm_jni
    android
    log
)

# Multi-threaded matmul
find_package(Threads REQUIRED)
target_link_libraries(picolm_jni Threads::Threads)
