# ⚡ BizClaw

<p align="center">
  <img src="docs/images/hero-banner.png" alt="BizClaw — 1 codebase, 3 platforms" width="800">
</p>

<p align="center">
  <strong>1 codebase. 3 nền tảng. Doanh nghiệp nào cũng chạy được.</strong><br>
  Raspberry Pi ($0) • Android (24/7) • VPS (Production)
</p>

> **Nền tảng AI Agent duy nhất chạy được trên cả Raspberry Pi, điện thoại Android, và VPS — từ 1 codebase Rust duy nhất.**

BizClaw là hạ tầng AI Agent module hoá, kiến trúc trait-driven. Không phải toy project — đây là production platform cho doanh nghiệp, chạy trên phần cứng từ 512MB RAM.


[![Rust](https://img.shields.io/badge/Rust-100%25-orange?logo=rust)](https://www.rust-lang.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/tests-240%20passing-brightgreen)]()
[![Crates](https://img.shields.io/badge/crates-17-success)]()
[![LOC](https://img.shields.io/badge/lines-39K+-informational)]()
[![Clippy](https://img.shields.io/badge/clippy-0%20warnings-brightgreen)]()
[![Android](https://img.shields.io/badge/Android-Agent%20Platform-34A853?logo=android)](android/)
[![Website](https://img.shields.io/badge/🌐_Website-bizclaw.vn-blue)](https://bizclaw.vn)
[![Facebook](https://img.shields.io/badge/📘_Fanpage-bizclaw.vn-1877F2?logo=facebook)](https://www.facebook.com/bizclaw.vn)

<!-- AUTO-GENERATED STATS — updated 2026-02-26 @ 03070b6 -->

---

## 🍓📱🖥️ Chạy MỌI NƠI — 1 Codebase, 3 Nền Tảng

> **BizClaw là nền tảng AI Agent DUY NHẤT triển khai được trên cả 3:**

| Nền tảng | Chi phí | Use Case | Đặc điểm |
|----------|---------|----------|----------|
| 🍓 **Raspberry Pi** | **$0/tháng** | Doanh nghiệp nhỏ, startup, cá nhân | Binary 12MB, 512MB RAM, Ollama local |
| 📱 **Android** | **$0/tháng** | Agent bỏ túi, điều khiển Facebook/Zalo | Foreground 24/7, Accessibility Service |
| 🖥️ **VPS** | **Tuỳ nhu cầu** | Production, multi-tenant, agency | Tự thuê VPS từ đối tác — BizClaw miễn phí |

```
 Cùng 1 codebase Rust →  cargo build  →  chạy trên cả 3
          │
   ┌──────┼──────────────────┐
   ▼      ▼                  ▼
  🍓 Pi  📱 Android          🖥️ VPS
  $0      $0                 Tuỳ nhu cầu
  1 agent Agent bỏ túi       50+ agents
  Offline 24/7 + App ctrl    Multi-tenant
```

> **Tuyệt đối KHÔNG cần tạo tài khoản trên server trung gian.** Không telemetry. Không tracking. Dữ liệu 100% của bạn.

| | Chi tiết |
|--|---------|
| 🔒 **Local & Bảo Mật** | Dữ liệu chat, API Keys mã hoá AES-256 trên ổ cứng. SQLite database nằm ngay trên máy bạn. |
| 🌐 **Chạy Độc Lập** | Không cloud, không trung gian. Offline 100% với Ollama + Brain Engine. |
| 📱 **Android Agent** | App điều khiển Facebook/Messenger/Zalo — AI tự post, tự reply, tự nhắn tin. |
| 🍓 **Edge Device** | Raspberry Pi 4/5 chạy production. $0 chi phí hạ tầng. |

**3 cách cài đặt:**

```bash
# 📥 Method 1: One-Click Install (VPS/Pi)
curl -sSL https://bizclaw.vn/install.sh | sudo bash

# 🐳 Method 2: Docker
git clone https://github.com/nguyenduchoai/bizclaw
cd bizclaw && docker-compose up -d

# 🔧 Method 3: Build from Source
git clone https://github.com/nguyenduchoai/bizclaw.git
cd bizclaw && cargo build --release
./target/release/bizclaw-platform --port 3001
```

### 🎯 Tính năng chính

| Hạng mục | Chi tiết |
|----------|----------|
| **🔌 16 Providers** | OpenAI, Anthropic, Gemini, DeepSeek, Groq, OpenRouter, Together, MiniMax, xAI (Grok), Mistral, **BytePlus ModelArk** 🔥, Ollama, llama.cpp, Brain Engine, CLIProxy, vLLM + custom endpoint |
| **💬 9 Channels** | CLI, Telegram, Discord, Email (IMAP/SMTP), Webhook, WhatsApp, Zalo (Personal + Official) |
| **🛠️ 13 Tools** | Shell, File, Edit File, Glob, Grep, Web Search, HTTP Request, Config Manager, Execute Code (9 ngôn ngữ), Plan Mode, Group Summarizer, Calendar, Document Reader, Memory Search, Session Context |
| **🔗 MCP** | Model Context Protocol client — kết nối MCP servers bên ngoài, mở rộng tools không giới hạn |
| **🏢 Multi-Tenant** | Admin Platform, JWT Auth, Tenant Manager, Pairing Codes, Audit Log, RBAC, Per-tenant isolation |
| **🐘 PostgreSQL** | **MỚI v0.3** — Production database với connection pooling, 19 tables, async sqlx, persistent data |
| **🧠 ReMe Memory** | **MỚI v0.3** — 4-type memory: Personal (preferences), Task (patterns), Tool (usage stats), Working (context compaction) + Hybrid Search |
| **⏰ Heartbeat/Cron** | **MỚI v0.3** — Agent tự thức dậy, scheduled tasks, proactive notifications, cron expressions |
| **🔧 Skills System** | **MỚI v0.3** — Plug-and-play skills, 10 built-in, hot-reload, per-tenant, multi-language (Python/JS/Shell) |
| **👥 Agent Teams** | **MỚI v0.3** — Multi-agent collaboration, 5 team templates, workflow tracking, pipeline execution |
| **🌐 Web Dashboard** | 20+ trang UI (VI/EN), WebSocket real-time, **Full CRUD trên tất cả pages**, LLM Traces, Cost Tracking, Activity Feed, **📖 Wiki & Guide tích hợp**, **🤖 AI Chat hướng dẫn sử dụng** |
| **📱 Android Agent** | App chạy agent 24/7, Foreground Service, Accessibility Service điều khiển Facebook/Messenger/Zalo, device tools (battery/GPS/notification) |
| **🤖 51 Agent Templates** | 13 danh mục nghiệp vụ, system prompt chuyên sâu, cài 1 click |
| **👥 Group Chat** | Tạo nhóm agent cộng tác — gửi 1 câu hỏi, tất cả agent trong nhóm phản hồi |
| **🧠 3-Tier Memory** | Brain workspace (SOUL.md/MEMORY.md), Daily auto-compaction, FTS5 search |
| **📚 Knowledge RAG** | Dual-mode: FTS5/BM25 (instant) + PageIndex MCP (reasoning-based, 98.7% accuracy) |
| **⏰ Scheduler** | Tác vụ hẹn giờ, agent tự chạy background, **retry mechanism với exponential backoff** |
| **💾 Persistence** | PostgreSQL (production) + SQLite (edge), gateway.db, agents.json backup, auto-restore |
| **🧠 Brain Engine** | GGUF inference: mmap, quantization, Flash Attention, SIMD (ARM NEON, x86 SSE2/AVX2) |
| **🔄 Think-Act-Observe** | Agent loop 5 rounds max — tự suy luận, hành động, đánh giá |
| **✅ Quality Gates** | Evaluator LLM tự review response, auto-revision nếu chưa đạt |
| **📊 Prompt Caching** | Anthropic `cache_control` — tiết kiệm 60-90% token lặp |
| **🔌 OpenAI-Compatible API** | Drop-in `/v1/chat/completions` — dùng với Cursor, Aider, Continue... |
| **🔒 Security** | Command allowlist, AES-256, HMAC-SHA256, JWT + bcrypt, CORS, rate limiting |

### 🌐 Dashboard — Full CRUD trên mọi trang

Mọi trang trong dashboard tenant đều có đầy đủ thao tác **Thêm/Sửa/Xoá**, không chỉ xem:

| Trang | Thao tác |
|-------|----------|
| 🤖 **AI Agents** | Tạo, Sửa, Xoá agent |
| 📚 **Knowledge** | Thêm, Xoá tài liệu |
| 📱 **Channels** | Cấu hình kênh liên lạc |
| ⚙️ **Settings** | Lưu cấu hình, System Prompt |
| 🔌 **Providers** | API Key, Activate provider |
| 🛠️ **Tools** | Bật/Tắt từng tool |
| 🔗 **MCP** | Thêm/Xoá server, Quick Add |
| 🤚 **Hands** | Tạo/Sửa/Xoá auto-agent |
| 🔀 **Orchestration** | Tạo/Xoá delegation |
| 📦 **Gallery** | Duyệt 51 template, xem chi tiết, **Clone thành Agent** |
| 🔄 **Workflows** | Chạy/Xoá workflow |
| 🧩 **Skills** | Install/Uninstall |
| 🧠 **Brain** | Tạo/Sửa file |
| 📖 **Wiki & Guide** | 6 bài hướng dẫn tích hợp |
| 🤖 **AI Chat** | Chatbot hướng dẫn sử dụng (floating button 💬) |

### 🤖 Agent Gallery — 51 Mẫu Nghiệp vụ

Cài đặt agent chuyên biệt chỉ 1 click. Mỗi agent có **system prompt** tích hợp skill chuyên sâu cho doanh nghiệp Việt Nam:

| Danh mục | Số lượng | Ví dụ |
|----------|----------|-------|
| 🧑‍💼 **HR** | 5 | Tuyển dụng, Onboarding, Lương & Phúc lợi, KPI, Nội quy |
| 💰 **Sales** | 5 | CRM, Báo giá, Doanh số, Telesales, Đối tác |
| 📊 **Finance** | 5 | Kế toán, Thuế, Dòng tiền, Hóa đơn, Kiểm soát nội bộ |
| 🏭 **Operations** | 5 | Kho, Mua hàng, Vận chuyển, QC, Bảo trì |
| ⚖️ **Legal** | 4 | Hợp đồng, Tuân thủ, Sở hữu trí tuệ, Tranh chấp |
| 📞 **Customer Service** | 3 | Hỗ trợ KH, Ticket, CSAT & Feedback |
| 📣 **Marketing** | 5 | Content, SEO, Ads, Social Media, Thương hiệu |
| 🛒 **E-commerce** | 3 | Sản phẩm, Đơn hàng, Sàn TMĐT |
| 💼 **Management** | 5 | Họp, Báo cáo, Chiến lược, Dự án, OKR |
| 📝 **Admin** | 3 | Văn thư, Tài sản, Công tác phí |
| 💻 **IT** | 3 | Helpdesk, An ninh mạng, Hạ tầng |
| 📧 **Business** | 3 | Email, Dịch thuật, Phân tích dữ liệu |
| 🎓 **Training** | 2 | Đào tạo, SOP |

### 💰 Tiết kiệm token — Mỗi Agent chọn Provider riêng

> **Điểm khác biệt lớn nhất của BizClaw:** Mỗi agent có thể chọn nhà cung cấp & mô hình riêng.
> Thay vì dùng 1 provider đắt tiền cho mọi agent, hãy **tối ưu chi phí theo từng vai trò**.

```
┌─────────────────────────────────────────────────────────────────┐
│  Agent           │  Provider             │  Chi phí     │  Lý do     │
├──────────────────┼───────────────────────┼──────────────┼────────────┤
│  Dịch thuật      │  Ollama/qwen3         │  $0 (local)  │  Free      │
│  Full-Stack Dev  │  Anthropic/claude     │  $$$         │  Mạnh      │
│  Social Media    │  Gemini/flash         │  $           │  Nhanh     │
│  Kế toán         │  DeepSeek/chat        │  $$          │  Giá tốt   │
│  Helpdesk        │  Groq/llama-3.3-70b   │  $           │  Nhanh     │
│  Agentic Tasks   │  ModelArk/seed-2.0    │  $$          │  Agentic   │
│  Nội bộ          │  Brain Engine         │  $0 (offline)│  Bảo mật   │
└─────────────────────────────────────────────────────────────────────┘
```

**Kết quả:** Tiết kiệm **60-80% chi phí API** so với dùng 1 provider cho tất cả agent.

**Cách hoạt động:**
1. Vào **Nhà cung cấp** → nhập API key cho từng provider (💾 Save riêng)
2. Vào **AI Agent** → chọn provider & model riêng cho mỗi agent
3. Backend tự đọc credentials từ DB — không cần cấu hình thêm

### 👥 Group Chat — Đội ngũ Agent cộng tác

Tạo nhóm nhiều agent cùng nhà cung cấp khác nhau làm việc cùng lúc. Gửi 1 câu hỏi → tất cả agent trong nhóm phản hồi theo chuyên môn.

```
Bạn: "Chuẩn bị pitch cho nhà đầu tư Series A"
  │
  ├── 🧑‍💼 Agent "Chiến lược" (Claude)  → Phân tích thị trường, USP
  ├── 📊 Agent "Tài chính" (DeepSeek)  → Unit economics, projections
  ├── 📣 Agent "Marketing" (Gemini)    → Brand story, go-to-market
  └── ⚖️ Agent "Pháp lý" (Groq)       → Term sheet, cap table
```

### 🏗️ Kiến trúc

```
┌──────────────────────────────────────────────────────────┐
│              bizclaw-platform (Admin)                     │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐                    │
│  │ Tenant 1│ │ Tenant 2│ │ Tenant N│  ← JWT + Audit Log │
│  └────┬────┘ └────┬────┘ └────┬────┘                    │
│       └───────────┼───────────┘                          │
│                   ▼                                      │
│            bizclaw (Gateway)                             │
│  ┌────────────────────────────────────────┐              │
│  │ Axum HTTP + WebSocket + Dashboard      │              │
│  │ SQLite gateway.db (per-tenant)         │              │
│  └────────────────┬───────────────────────┘              │
│    ┌──────────────┼──────────────┐                       │
│    ▼              ▼              ▼                       │
│  bizclaw-agent  bizclaw-agent  bizclaw-agent             │
│  (Orchestrator manages N agents)                         │
│    ┌──────────────┼──────────────┐                       │
│    ▼              ▼              ▼                       │
│ 16 Providers   9 Channels    13 Tools + MCP              │
│    ▼              ▼              ▼                       │
│ Memory         Security      Knowledge                   │
│  (SQLite+FTS5) (Allowlist)   (RAG+FTS5)                  │
│    ▼                                                     │
│ Brain Engine (GGUF+SIMD) — offline inference             │
│    ▼                                                     │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ 📱 Android Agent Platform                            │ │
│ │ bizclaw-ffi → Foreground Service → Device Control    │ │
│ │ Accessibility Service → Facebook/Messenger/Zalo      │ │
│ └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

### 🚀 Bắt đầu nhanh

```bash
# Clone và build
git clone https://github.com/nguyenduchoai/bizclaw.git
cd bizclaw
cargo build --release

# Cài đặt (wizard tương tác)
./target/release/bizclaw init

# Chat ngay
./target/release/bizclaw agent --interactive

# Mở Web Dashboard
./target/release/bizclaw serve
```

### 🏢 Chế độ triển khai

| Mode | Binary | Use Case |
|------|--------|----------|
| **Standalone** | `bizclaw` only | 1 bot, cá nhân, test |
| **Platform** | `bizclaw` + `bizclaw-platform` | Nhiều bots, agency, production |

**Platform mode** cung cấp:
- Admin Dashboard tại `/admin/` — quản lý tenants, users, audit log
- Mỗi tenant có subdomain riêng (demo.bizclaw.vn, sales.bizclaw.vn)
- JWT authentication + per-tenant SQLite DB

### 🔗 MCP (Model Context Protocol) Support

BizClaw hỗ trợ kết nối **MCP Servers** — mở rộng tools không giới hạn mà không cần rebuild:

```toml
# config.toml
[[mcp_servers]]
name = "pageindex"
command = "npx"
args = ["-y", "@pageindex/mcp"]
# 📑 PageIndex — Vectorless, Reasoning-based RAG
# Upload PDF → LLM suy luận qua tree structure → tìm context chính xác
# 98.7% accuracy trên FinanceBench (vs vector RAG ~70%)

[[mcp_servers]]
name = "github"
command = "npx"
args = ["-y", "@modelcontextprotocol/server-github"]

[[mcp_servers]]
name = "database"
command = "npx"
args = ["-y", "@modelcontextprotocol/server-postgres"]
env = { DATABASE_URL = "postgresql://..." }
```

### 🧠 Ollama / Brain Engine — Chạy AI Offline

Ollama models được **dùng chung** giữa tất cả tenants. Pull 1 lần → tất cả dùng được.

```bash
curl -fsSL https://ollama.ai/install.sh | sh
ollama pull llama3.2      # ~3.8GB
ollama pull qwen3         # ~4.7GB
```

### 📦 Crate Map

| Crate | Mô tả | Status |
|-------|--------|--------|
| `bizclaw-core` | Traits, types, config, errors | ✅ |
| `bizclaw-brain` | GGUF inference + SIMD (ARM NEON, x86 AVX2) | ✅ |
| `bizclaw-providers` | 16 LLM providers (OpenAI-compatible unified) | ✅ |
| `bizclaw-channels` | 9 channel types (CLI, Telegram, Discord, Email, Webhook, WhatsApp, Zalo) | ✅ |
| `bizclaw-memory` | SQLite + FTS5, Brain workspace, daily auto-compaction | ✅ |
| `bizclaw-tools` | 13 native tools + MCP bridge | ✅ |
| `bizclaw-mcp` | MCP client (JSON-RPC 2.0 via stdio) | ✅ |
| `bizclaw-security` | AES-256, Command allowlist, Sandbox | ✅ |
| `bizclaw-agent` | Think-Act-Observe loop (5 rounds), Quality Gates, auto-compaction | ✅ |
| `bizclaw-gateway` | Axum HTTP + WS + Dashboard (15 pages, i18n VI/EN), OpenAI-compatible API, LLM Tracing | ✅ |
| `bizclaw-knowledge` | Knowledge RAG with FTS5, document chunking | ✅ |
| `bizclaw-scheduler` | Scheduled tasks, agent integration, notifications | ✅ |
| `bizclaw-runtime` | Process adapters | ✅ |
| `bizclaw-platform` | Multi-tenant admin platform, JWT, audit log | ✅ |
| `bizclaw-ffi` | Android/Edge FFI layer — 7 functions, cdylib, catch_unwind | ✅ |
| `bizclaw-hands` | Process adapters, runtime execution | ✅ |

### 📱 Android Agent Platform — Không chỉ chat. Mà ĐIỀU KHIỂN.

<p align="center">
  <img src="docs/images/android-agent-platform.png" alt="BizClaw Android — 3 modes" width="500">
</p>

**3 chế độ hoạt động:**

| Mode | Emoji | Mô tả |
|------|-------|--------|
| LOCAL | 📱 | Rust engine chạy trên phone, Ollama local, $0, không cần internet |
| REMOTE | 🌐 | Kết nối VPS/Pi, chat & điều khiển agent từ xa |
| HYBRID | 🔀 | Engine local + agent cloud cùng lúc |

**Điều khiển BẤT KỲ app nào trên phone:**

| App | Khả năng |
|-----|----------|
| Facebook | Tự đăng bài, bình luận, like |
| Messenger | Tự trả lời tin nhắn |
| Zalo | Tự nhắn tin |
| Bất kỳ app | Đọc màn hình, click, gõ text, swipe |

| Component | Chức năng |
|-----------|----------|
| `BizClawDaemonService` | Foreground service 24/7, WakeLock, auto-restart |
| `BizClawAccessibilityService` | Điều khiển UI: đọc, click, gõ, swipe, tap toạ độ |
| `AppController` | Workflow: Facebook post, Messenger reply, Zalo send |
| `DeviceCapabilities` | Battery, storage, GPS, CPU, OEM battery killer |
| `BootReceiver` | Tự khởi động lại agent sau reboot |

### 📊 Stats

| Metric | Value |
|--------|-------|
| **Language** | 100% Rust + Kotlin (Android) |
| **Crates** | 17 |
| **Lines of Code** | ~41,000 (Rust 38K + Kotlin 3K) |
| **Tests** | 240 passing |
| **Clippy Warnings** | **0** ✅ |
| **Providers** | 16 built-in + custom endpoint |
| **Channels** | 25+ types (33 registered) |
| **Tools** | 13 native + MCP (unlimited) + 10 device tools |
| **Scheduler** | Background tasks + retry |
| **Gallery** | 51 business agent templates |
| **Dashboard** | 20+ pages, bilingual (VI/EN), Full CRUD, Wiki & AI Guide |
| **Android** | 16 Kotlin files, Material 3, Compose |
| **Binary Size** | bizclaw 12M, platform 7.7M, APK ~8MB |
| **Last Updated** | 2026-03-03 |

---

## 🇬🇧 English

### What is BizClaw?

BizClaw is a **self-hosted AI Agent platform** built entirely in Rust. Run AI agents on your own infrastructure — no cloud lock-in, no data leaving your servers.

### Key Features

- **🔌 16 Providers** — OpenAI, Anthropic, Gemini, DeepSeek, Groq, OpenRouter, Together, MiniMax, xAI, Mistral, **BytePlus ModelArk**, Ollama, llama.cpp, Brain, CLIProxy, vLLM
- **💬 9 Channels** — CLI, Telegram, Discord, Email, Webhook, WhatsApp, Zalo
- **🛠️ 13 Tools** — Shell, File, Edit, Glob, Grep, Web Search, HTTP, Config, Execute Code (9 langs), Plan Mode, Group Summarizer, Calendar, Doc Reader, Memory Search, Session Context
- **🔗 MCP Support** — Connect any MCP server for unlimited tool extensions
- **🏢 Multi-Tenant Platform** — Admin dashboard, JWT auth, per-tenant isolated DB
- **🌐 Web Dashboard** — 12-page bilingual UI (Vietnamese/English), real-time WebSocket chat
- **🤖 51 Agent Templates** — Pre-built agents for HR, Sales, Finance, Ops, Legal, Marketing, IT
- **💰 Per-Agent Provider Selection** — Each agent picks its own LLM provider → save 60-80% on API costs
- **👥 Group Chat** — Multi-agent collaboration with mixed providers
- **🧠 3-Tier Memory** — Brain workspace + daily auto-compaction + FTS5 search
- **📚 Knowledge RAG** — Upload documents for retrieval-augmented generation
- **⏰ Scheduler** — Automated tasks with agent integration
- **🔒 Security** — AES-256, command allowlists, HMAC-SHA256, JWT + bcrypt

### Quick Start

```bash
git clone https://github.com/nguyenduchoai/bizclaw.git
cd bizclaw && cargo build --release
./target/release/bizclaw init
./target/release/bizclaw serve
# Open http://localhost:3579 for dashboard
```

### Deployment

BizClaw is deployed on **2 independent domains** from the same codebase:

| Domain | Landing Page | Platform Dashboard |
|--------|-------------|--------------------|
| **BizClaw** | [bizclaw.vn](https://bizclaw.vn) | [apps.bizclaw.vn](https://apps.bizclaw.vn) |
| **ViAgent** | [viagent.vn](https://viagent.vn) | [apps.viagent.vn](https://apps.viagent.vn) |

- Demo Tenant: `demo.bizclaw.vn` / `demo.viagent.vn`
- Each domain runs independently with its own database, tenants, and pairing codes

### 🔗 Links

| | |
|--|--|
| 🌐 **Website** | [https://bizclaw.vn](https://bizclaw.vn) |
| 📘 **Fanpage** | [https://www.facebook.com/bizclaw.vn](https://www.facebook.com/bizclaw.vn) |
| 💻 **GitHub** | [https://github.com/nguyenduchoai/bizclaw](https://github.com/nguyenduchoai/bizclaw) |

| 🟢 **ViAgent** | [https://viagent.vn](https://viagent.vn) |

---


---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

---

**BizClaw** v0.3.0 — *AI nhanh, mọi nơi. / Fast AI, everywhere.*
