-- ════════════════════════════════════════════════════════════════
-- Migration 004: Remote Server Provisioner
-- Cloud management — track and manage remote BizClaw instances
-- ════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS remote_servers (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    name TEXT NOT NULL,
    ip TEXT NOT NULL UNIQUE,
    domain TEXT,
    port INTEGER DEFAULT 3001,
    status TEXT DEFAULT 'provisioning',
    version TEXT,
    last_health_check TIMESTAMPTZ,
    tenant_count INTEGER DEFAULT 0,
    cpu_usage REAL,
    ram_usage REAL,
    disk_usage REAL,
    notes TEXT,
    ssh_key_fingerprint TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for health monitoring queries
CREATE INDEX IF NOT EXISTS idx_remote_servers_status ON remote_servers(status);
CREATE INDEX IF NOT EXISTS idx_remote_servers_ip ON remote_servers(ip);

-- Provision logs — track provision history
CREATE TABLE IF NOT EXISTS provision_logs (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    server_id TEXT REFERENCES remote_servers(id) ON DELETE CASCADE,
    action TEXT NOT NULL,  -- provision, update, restart, etc.
    status TEXT NOT NULL,  -- success, error
    output TEXT,           -- command output
    initiated_by TEXT,     -- admin user ID
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_provision_logs_server ON provision_logs(server_id);
