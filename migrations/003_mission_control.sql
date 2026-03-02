-- ═══════════════════════════════════════════════════════════════
-- BizClaw Platform — Mission Control Features Schema v0.5
-- Ports: Kanban Task Board, Quality Gate, Session Monitor, GitHub Sync
-- ═══════════════════════════════════════════════════════════════

-- ════════════════════════════════════════════════
-- 1. KANBAN TASK BOARD
-- 6 columns: inbox → backlog → todo → in_progress → review → done
-- ════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS tasks (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID REFERENCES tenants(id) ON DELETE CASCADE,    -- NULL = platform-wide
    title           VARCHAR(500) NOT NULL,
    description     TEXT DEFAULT '',
    status          VARCHAR(20) DEFAULT 'inbox',          -- inbox|backlog|todo|in_progress|review|done
    priority        VARCHAR(10) DEFAULT 'normal',          -- low|normal|high|urgent
    assigned_to     UUID REFERENCES users(id),
    assigned_agent  VARCHAR(100),                           -- agent name (not a DB user)
    tags            TEXT DEFAULT '',                        -- comma-separated tags
    due_at          TIMESTAMPTZ,
    github_issue_id BIGINT,                                -- from GitHub sync
    github_repo     VARCHAR(255),
    source          VARCHAR(50) DEFAULT 'manual',           -- manual|github|agent|heartbeat
    position        INTEGER DEFAULT 0,                      -- drag-drop ordering within column
    quality_gate    BOOLEAN DEFAULT false,                  -- needs review sign-off to move to done
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_tasks_tenant     ON tasks(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_tasks_assigned   ON tasks(assigned_to, status);
CREATE INDEX IF NOT EXISTS idx_tasks_status     ON tasks(status, priority);
CREATE INDEX IF NOT EXISTS idx_tasks_github     ON tasks(github_issue_id) WHERE github_issue_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS task_comments (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id     UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    author_id   UUID REFERENCES users(id),
    author_name VARCHAR(100) NOT NULL DEFAULT 'system',
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_task_comments_task ON task_comments(task_id);

-- ════════════════════════════════════════════════
-- 2. QUALITY GATE (Review Sign-off before done)
-- ════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS quality_reviews (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id     UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    reviewer_id UUID REFERENCES users(id),
    reviewer    VARCHAR(100) NOT NULL,                      -- user email or agent name
    status      VARCHAR(20) DEFAULT 'pending',              -- pending|approved|rejected
    notes       TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_quality_reviews_task ON quality_reviews(task_id, status);

-- ════════════════════════════════════════════════
-- 3. AGENT SESSION MONITOR
-- Track live sessions per tenant/agent
-- ════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS agent_sessions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID REFERENCES tenants(id) ON DELETE CASCADE,
    agent_name      VARCHAR(100) NOT NULL,
    session_key     VARCHAR(255) UNIQUE,                    -- external identifier (e.g. PID or UUID)
    status          VARCHAR(20) DEFAULT 'active',           -- active|idle|paused|terminated
    started_at      TIMESTAMPTZ DEFAULT NOW(),
    last_heartbeat  TIMESTAMPTZ DEFAULT NOW(),
    terminated_at   TIMESTAMPTZ,
    prompt_tokens   BIGINT DEFAULT 0,                       -- rolling total for this session
    completion_tokens BIGINT DEFAULT 0,
    total_cost_usd  REAL DEFAULT 0,
    model           VARCHAR(100),
    metadata        JSONB DEFAULT '{}'
);
CREATE INDEX IF NOT EXISTS idx_sessions_tenant     ON agent_sessions(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_sessions_key        ON agent_sessions(session_key);
CREATE INDEX IF NOT EXISTS idx_sessions_heartbeat  ON agent_sessions(last_heartbeat);

-- ════════════════════════════════════════════════
-- 4. GITHUB SYNC
-- Inbound from GitHub Issues → Task Board
-- ════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS github_syncs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID REFERENCES tenants(id) ON DELETE CASCADE,
    repo            VARCHAR(255) NOT NULL,                  -- "owner/repo"
    access_token    TEXT,                                   -- encrypted GitHub PAT
    label_filter    VARCHAR(255) DEFAULT '',                -- only sync issues with these labels
    auto_assign     VARCHAR(100) DEFAULT '',                -- default agent to assign
    last_synced_at  TIMESTAMPTZ,
    issues_synced   INTEGER DEFAULT 0,
    enabled         BOOLEAN DEFAULT true,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_github_syncs_repo ON github_syncs(tenant_id, repo);

-- ════════════════════════════════════════════════
-- 5. OUTBOUND WEBHOOKS (mirroring MC's webhook panel)
-- ════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS webhooks (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    url         TEXT NOT NULL,
    events      TEXT DEFAULT '[]',                          -- JSON array of event names
    secret      VARCHAR(255),
    enabled     BOOLEAN DEFAULT true,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_webhooks_tenant ON webhooks(tenant_id, enabled);

CREATE TABLE IF NOT EXISTS webhook_deliveries (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    webhook_id      UUID NOT NULL REFERENCES webhooks(id) ON DELETE CASCADE,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB DEFAULT '{}',
    status_code     INTEGER,
    response_body   TEXT,
    duration_ms     INTEGER,
    success         BOOLEAN DEFAULT false,
    error           TEXT,
    delivered_at    TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries ON webhook_deliveries(webhook_id, delivered_at DESC);

-- ════════════════════════════════════════════════
-- 6. ALERT RULES (Configurable threshold alerts)
-- ════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS alert_rules (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    metric          VARCHAR(100) NOT NULL,                  -- 'token_cost', 'error_rate', 'handoff_count'
    operator        VARCHAR(10) NOT NULL,                   -- '>', '<', '>=', '<='
    threshold       REAL NOT NULL,
    cooldown_mins   INTEGER DEFAULT 60,
    channel         VARCHAR(50) DEFAULT 'dashboard',        -- dashboard|telegram|email
    target          VARCHAR(255),
    enabled         BOOLEAN DEFAULT true,
    last_fired_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_alert_rules_tenant ON alert_rules(tenant_id, enabled);
