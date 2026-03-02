-- ═══════════════════════════════════════════════════════════════
-- BizClaw Platform — Enterprise Schema v0.4
-- Adds: Multi-user RBAC, Human Handoff, BI Analytics, Budget Quota
-- ═══════════════════════════════════════════════════════════════

-- ════════════════════════════════════════════════
-- FEATURE 1: MULTI-USER PER TENANT (RBAC)
-- ════════════════════════════════════════════════

-- Extend existing tenant_members table
ALTER TABLE tenant_members ADD COLUMN IF NOT EXISTS invited_by UUID REFERENCES users(id);
ALTER TABLE tenant_members ADD COLUMN IF NOT EXISTS joined_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE tenant_members ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'active';
-- Roles: owner, admin, operator, viewer

-- Invitation system — invite members via email
CREATE TABLE IF NOT EXISTS tenant_invitations (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email       VARCHAR(255) NOT NULL,
    role        VARCHAR(20) DEFAULT 'operator',
    token       VARCHAR(255) UNIQUE NOT NULL,
    invited_by  UUID REFERENCES users(id),
    expires_at  TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(tenant_id, email)
);
CREATE INDEX IF NOT EXISTS idx_invitations_token ON tenant_invitations(token);
CREATE INDEX IF NOT EXISTS idx_invitations_tenant ON tenant_invitations(tenant_id);

-- ════════════════════════════════════════════════
-- FEATURE 2: HUMAN HANDOFF ("Chuyển còi")
-- ════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS handoff_sessions (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    channel             VARCHAR(50) NOT NULL,
    external_user_id    VARCHAR(255) NOT NULL,
    external_user_name  VARCHAR(255),
    status              VARCHAR(20) DEFAULT 'pending', -- pending|active|resolved|timeout
    assigned_to         UUID REFERENCES users(id),
    trigger_reason      TEXT,
    conversation_context JSONB DEFAULT '[]',
    resolved_by         UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW(),
    resolved_at         TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_handoff_tenant ON handoff_sessions(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_handoff_assigned ON handoff_sessions(assigned_to, status);

CREATE TABLE IF NOT EXISTS handoff_messages (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id  UUID NOT NULL REFERENCES handoff_sessions(id) ON DELETE CASCADE,
    sender_type VARCHAR(20) NOT NULL, -- user|operator|bot
    sender_id   VARCHAR(255),
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_handoff_msgs_session ON handoff_messages(session_id);

-- ════════════════════════════════════════════════
-- FEATURE 3: BI ANALYTICS
-- ════════════════════════════════════════════════

-- Token usage tracking per conversation / per day
CREATE TABLE IF NOT EXISTS token_usage_log (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    date                DATE NOT NULL DEFAULT CURRENT_DATE,
    provider            VARCHAR(50) NOT NULL DEFAULT 'openai',
    model               VARCHAR(100) NOT NULL,
    prompt_tokens       INTEGER DEFAULT 0,
    completion_tokens   INTEGER DEFAULT 0,
    total_tokens        INTEGER DEFAULT 0,
    estimated_cost_usd  REAL DEFAULT 0,
    session_id          VARCHAR(255),
    channel             VARCHAR(50),
    created_at          TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_token_usage_tenant_date ON token_usage_log(tenant_id, date);
CREATE INDEX IF NOT EXISTS idx_token_usage_session    ON token_usage_log(session_id);

-- Conversation outcomes for funnel tracking
CREATE TABLE IF NOT EXISTS conversation_outcomes (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id        UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    session_id       VARCHAR(255) NOT NULL,
    channel          VARCHAR(50),
    outcome          VARCHAR(50) DEFAULT 'resolved', -- resolved|handoff|abandoned|converted
    sentiment        VARCHAR(20) DEFAULT 'neutral',  -- positive|neutral|negative
    duration_seconds INTEGER DEFAULT 0,
    message_count    INTEGER DEFAULT 0,
    tokens_total     INTEGER DEFAULT 0,
    date             DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at       TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_conv_outcomes_tenant ON conversation_outcomes(tenant_id, date);
CREATE INDEX IF NOT EXISTS idx_conv_outcomes_session ON conversation_outcomes(session_id);

-- Daily aggregated analytics (pre-computed for fast Dashboard queries)
CREATE TABLE IF NOT EXISTS analytics_daily (
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    date                DATE NOT NULL,
    total_conversations INTEGER DEFAULT 0,
    resolved_count      INTEGER DEFAULT 0,
    handoff_count       INTEGER DEFAULT 0,
    abandoned_count     INTEGER DEFAULT 0,
    converted_count     INTEGER DEFAULT 0,
    positive_sentiment  INTEGER DEFAULT 0,
    neutral_sentiment   INTEGER DEFAULT 0,
    negative_sentiment  INTEGER DEFAULT 0,
    total_tokens        BIGINT DEFAULT 0,
    total_cost_usd      REAL DEFAULT 0,
    avg_duration_seconds REAL DEFAULT 0,
    updated_at          TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (tenant_id, date)
);
CREATE INDEX IF NOT EXISTS idx_analytics_daily_tenant ON analytics_daily(tenant_id, date DESC);

-- ════════════════════════════════════════════════
-- FEATURE 4: BUDGET QUOTA CONTROL
-- ════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS tenant_quotas (
    tenant_id        UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    resource         VARCHAR(50) NOT NULL, -- tokens_per_month|messages_per_day|handoffs_per_month
    limit_value      BIGINT NOT NULL DEFAULT 100000,
    used_value       BIGINT DEFAULT 0,
    reset_at         TIMESTAMPTZ,
    alert_threshold  REAL DEFAULT 0.8,  -- alert when 80% used
    alert_sent       BOOLEAN DEFAULT false,
    hard_stop        BOOLEAN DEFAULT true, -- block when limit exceeded
    updated_at       TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (tenant_id, resource)
);

-- Budget alert log
CREATE TABLE IF NOT EXISTS quota_alerts (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    resource    VARCHAR(50) NOT NULL,
    used_pct    REAL NOT NULL,  -- percentage used when alert was sent
    channel     VARCHAR(50),   -- how alert was sent: email|telegram|zalo
    recipient   VARCHAR(255),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_quota_alerts_tenant ON quota_alerts(tenant_id, created_at DESC);

-- ════════════════════════════════════════════════
-- DEFAULT QUOTAS for new tenants (trigger)
-- ════════════════════════════════════════════════

-- Function to seed default quotas when a tenant is created
CREATE OR REPLACE FUNCTION seed_tenant_quotas()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO tenant_quotas (tenant_id, resource, limit_value, reset_at)
    VALUES
        (NEW.id, 'tokens_per_month',   100000, DATE_TRUNC('month', NOW()) + INTERVAL '1 month'),
        (NEW.id, 'messages_per_day',   500,    DATE_TRUNC('day',   NOW()) + INTERVAL '1 day'),
        (NEW.id, 'handoffs_per_month', 100,    DATE_TRUNC('month', NOW()) + INTERVAL '1 month')
    ON CONFLICT DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_seed_tenant_quotas
    AFTER INSERT ON tenants
    FOR EACH ROW EXECUTE FUNCTION seed_tenant_quotas();
