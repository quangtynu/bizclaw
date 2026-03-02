//! # BizClaw Platform
//!
//! Multi-tenant management platform — run multiple BizClaw agents on a single VPS.
//! Includes admin dashboard, tenant lifecycle management, pairing security,
//! subdomain routing, resource monitoring, and audit logging.
//! Now with PostgreSQL support, ReMe Memory, Heartbeat/Cron, and Skills.

pub mod admin;
pub mod auth;
pub mod config;
pub mod db;
pub mod db_pg;
pub mod enterprise;
pub mod mission_control;
pub mod tenant;
pub mod self_serve;

pub use admin::AdminServer;
pub use db::PlatformDb;
pub use db_pg::PgDb;
pub use enterprise::{TenantRole, TenantMember, TenantInvitation, HandoffSession, HandoffMessage, AnalyticsSummary, QuotaStatus};
pub use mission_control::{Task, TaskComment, QualityReview, AgentSession, GithubSync, KANBAN_COLUMNS};
pub use tenant::TenantManager;
