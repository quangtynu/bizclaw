#!/bin/bash
# ════════════════════════════════════════════════════════════════
# Setup PicoLM C engine for BizClaw Android NDK build
#
# This script clones the picolm C inference engine source code
# into the Android cpp directory for NDK compilation.
#
# Usage: ./setup_picolm.sh
# ════════════════════════════════════════════════════════════════

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CPP_DIR="$SCRIPT_DIR/app/src/main/cpp"
PICOLM_DIR="$CPP_DIR/picolm"

echo "🧠 Setting up PicoLM C engine for BizClaw Android..."

# Clone or update picolm
if [ -d "$PICOLM_DIR" ]; then
    echo "📦 Updating existing PicoLM source..."
    cd "$PICOLM_DIR"
    git pull origin main 2>/dev/null || true
else
    echo "📥 Cloning PicoLM source..."
    git clone --depth 1 https://github.com/RightNow-AI/picolm.git "$PICOLM_DIR"
fi

# Copy only the C source files we need (from picolm/ subdirectory)
PICOLM_SRC="$PICOLM_DIR/picolm"
if [ -d "$PICOLM_SRC" ]; then
    echo "📋 Copying C sources from picolm/picolm/ to build directory..."
    # The actual source is inside picolm/picolm/ directory
    cp -f "$PICOLM_SRC"/*.c "$PICOLM_DIR/" 2>/dev/null || true
    cp -f "$PICOLM_SRC"/*.h "$PICOLM_DIR/" 2>/dev/null || true
fi

# Verify key files exist
REQUIRED_FILES=("model.h" "model.c" "tensor.h" "tensor.c" "quant.h" "quant.c" "tokenizer.h" "tokenizer.c" "sampler.h" "sampler.c" "grammar.h" "grammar.c")
MISSING=0
for f in "${REQUIRED_FILES[@]}"; do
    if [ ! -f "$PICOLM_DIR/$f" ]; then
        echo "⚠️  Missing: $f"
        MISSING=$((MISSING + 1))
    fi
done

if [ $MISSING -eq 0 ]; then
    echo "✅ All PicoLM source files present!"
    echo ""
    echo "📊 Source file stats:"
    wc -l "$PICOLM_DIR"/*.c "$PICOLM_DIR"/*.h 2>/dev/null | tail -1
    echo ""
    echo "🔨 Ready to build with: ./gradlew assembleDebug"
else
    echo "❌ $MISSING files missing — check picolm repository structure"
    exit 1
fi

echo ""
echo "════════════════════════════════════════════════════════"
echo "  PicoLM setup complete!"
echo ""
echo "  Supported models (GGUF Q4_K_M):"
echo "    • Qwen3 4B   — 2.7 GB — Best balance for mobile"
echo "    • Qwen3 8B   — 5.1 GB — Powerful, needs 8GB+ RAM"
echo "    • TinyLlama   — 638 MB — Runs on any phone"
echo "    • DeepSeek R1 — 1.1 GB — Reasoning model"
echo ""
echo "  Build: cd android && ./gradlew assembleDebug"
echo "════════════════════════════════════════════════════════"
