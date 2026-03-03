//! ByteRover Context Tree Tool — stateful, curated memory for BizClaw agents.
//!
//! Integration with ByteRover CLI for 3rd-layer memory:
//!   Layer 1: Brain MEMORY.md (workspace memory — durable facts)
//!   Layer 2: Daily logs (memory/YYYY-MM-DD.md — session context)
//!   Layer 3: ByteRover Context Tree (.brv/context-tree/ — LLM-curated knowledge, 92% accuracy)
//!
//! ## Commands exposed to agents:
//! - `brv_query`  — Search the context tree for relevant knowledge
//! - `brv_curate` — Add/update knowledge in the context tree
//!
//! ## Architecture:
//!   Agent → ByteRoverTool::execute() → brv CLI (subprocess) → .brv/context-tree/
//!
//! ## Reference:
//!   https://www.byterover.dev/blog/curated-stateful-memory-for-openclaw

use async_trait::async_trait;
use bizclaw_core::error::{BizClawError, Result};
use bizclaw_core::traits::Tool;
use bizclaw_core::types::{ToolDefinition, ToolResult};
use std::path::PathBuf;
use std::process::Command;
use tracing::{debug, warn};

/// Check if ByteRover CLI is installed.
pub fn is_brv_available() -> bool {
    Command::new("brv")
        .arg("--version")
        .output()
        .map(|o| o.status.success())
        .unwrap_or(false)
}

/// ByteRover query tool — search the context tree for structured knowledge.
///
/// Uses tiered retrieval: Cache → Full-text → LLM-powered search
/// Returns relevant context from the hierarchical knowledge base.
pub struct ByteRoverQueryTool {
    workspace_dir: PathBuf,
}

impl ByteRoverQueryTool {
    pub fn new(workspace_dir: PathBuf) -> Self {
        Self { workspace_dir }
    }

    pub fn for_tenant(slug: &str) -> Self {
        let home = dirs::home_dir().unwrap_or_else(|| PathBuf::from("/root"));
        Self {
            workspace_dir: home.join(".bizclaw").join("tenants").join(slug).join("brain"),
        }
    }
}

#[async_trait]
impl Tool for ByteRoverQueryTool {
    fn name(&self) -> &str {
        "brv_query"
    }

    fn definition(&self) -> ToolDefinition {
        ToolDefinition {
            name: "brv_query".into(),
            description: "Search the ByteRover Context Tree for structured knowledge. \
                This is a curated, hierarchical knowledge base (92% retrieval accuracy). \
                Use when you need: architectural decisions, past bug fixes, coding patterns, \
                project-specific knowledge, or domain expertise. \
                More powerful than memory_search — uses LLM-powered search with reasoning."
                .into(),
            parameters: serde_json::json!({
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "Natural language query to search the context tree. Example: 'how does the authentication system work?'"
                    }
                },
                "required": ["query"]
            }),
        }
    }

    async fn execute(&self, arguments: &str) -> Result<ToolResult> {
        let args: serde_json::Value = serde_json::from_str(arguments)
            .map_err(|e| BizClawError::Tool(format!("Invalid arguments: {e}")))?;

        let query = args["query"]
            .as_str()
            .ok_or_else(|| BizClawError::Tool("Missing 'query' parameter".into()))?;

        debug!("🧠 ByteRover query: {}", query);

        // Try brv CLI first
        if is_brv_available() {
            match Command::new("brv")
                .arg("query")
                .arg(query)
                .current_dir(&self.workspace_dir)
                .output()
            {
                Ok(output) => {
                    let stdout = String::from_utf8_lossy(&output.stdout).to_string();
                    let stderr = String::from_utf8_lossy(&output.stderr).to_string();

                    if output.status.success() && !stdout.trim().is_empty() {
                        return Ok(ToolResult {
                            tool_call_id: String::new(),
                            output: format!(
                                "## ByteRover Context (Auto-Enriched)\n\n{}",
                                stdout.trim()
                            ),
                            success: true,
                        });
                    } else {
                        debug!("brv query returned empty or failed: {}", stderr);
                    }
                }
                Err(e) => {
                    warn!("brv query failed: {}", e);
                }
            }
        }

        // Fallback: manual search through .brv/context-tree/ markdown files
        let context_tree = self.workspace_dir.join(".brv").join("context-tree");
        if context_tree.exists() {
            let results = search_context_tree(&context_tree, query)?;
            if !results.is_empty() {
                return Ok(ToolResult {
                    tool_call_id: String::new(),
                    output: format!(
                        "## ByteRover Context (Local Fallback)\n\n{}",
                        results
                    ),
                    success: true,
                });
            }
        }

        Ok(ToolResult {
            tool_call_id: String::new(),
            output: format!(
                "No context found for '{}'. \
                 ByteRover context tree is empty or brv CLI is not installed. \
                 Install with: curl -fsSL https://byterover.dev/install.sh | sh",
                query
            ),
            success: true,
        })
    }
}

/// ByteRover curate tool — add/update knowledge in the context tree.
///
/// The LLM-powered curation engine:
/// - Structures new knowledge into the hierarchy (domain → topic → subtopic)
/// - ADD / UPDATE / MERGE / DELETE with reasoning
/// - Every operation is auditable
pub struct ByteRoverCurateTool {
    workspace_dir: PathBuf,
}

impl ByteRoverCurateTool {
    pub fn new(workspace_dir: PathBuf) -> Self {
        Self { workspace_dir }
    }

    pub fn for_tenant(slug: &str) -> Self {
        let home = dirs::home_dir().unwrap_or_else(|| PathBuf::from("/root"));
        Self {
            workspace_dir: home.join(".bizclaw").join("tenants").join(slug).join("brain"),
        }
    }
}

#[async_trait]
impl Tool for ByteRoverCurateTool {
    fn name(&self) -> &str {
        "brv_curate"
    }

    fn definition(&self) -> ToolDefinition {
        ToolDefinition {
            name: "brv_curate".into(),
            description: "Save knowledge to the ByteRover Context Tree. \
                Use this to persist: architectural decisions, bug fixes, API patterns, \
                project rules, or any reusable knowledge. \
                The knowledge is curated by LLM reasoning into a structured hierarchy. \
                Examples: 'The auth module uses JWT with bcrypt, tokens expire in 24h' \
                or 'Database migration 005 adds the scheduler_tasks table with retry_count column'."
                .into(),
            parameters: serde_json::json!({
                "type": "object",
                "properties": {
                    "summary": {
                        "type": "string",
                        "description": "Knowledge to curate — describe what was learned, decided, or discovered. Be specific and factual."
                    }
                },
                "required": ["summary"]
            }),
        }
    }

    async fn execute(&self, arguments: &str) -> Result<ToolResult> {
        let args: serde_json::Value = serde_json::from_str(arguments)
            .map_err(|e| BizClawError::Tool(format!("Invalid arguments: {e}")))?;

        let summary = args["summary"]
            .as_str()
            .ok_or_else(|| BizClawError::Tool("Missing 'summary' parameter".into()))?;

        debug!("🧠 ByteRover curate: {}", summary);

        // Try brv CLI
        if is_brv_available() {
            match Command::new("brv")
                .arg("curate")
                .arg(summary)
                .current_dir(&self.workspace_dir)
                .output()
            {
                Ok(output) => {
                    let stdout = String::from_utf8_lossy(&output.stdout).to_string();
                    let stderr = String::from_utf8_lossy(&output.stderr).to_string();

                    if output.status.success() {
                        return Ok(ToolResult {
                            tool_call_id: String::new(),
                            output: format!(
                                "✅ Knowledge curated successfully.\n\n{}",
                                stdout.trim()
                            ),
                            success: true,
                        });
                    } else {
                        warn!("brv curate failed: {}", stderr);
                    }
                }
                Err(e) => {
                    warn!("brv curate failed: {}", e);
                }
            }
        }

        // Fallback: save directly to .brv/context-tree/ as markdown
        let context_tree = self.workspace_dir.join(".brv").join("context-tree");
        std::fs::create_dir_all(&context_tree)
            .map_err(|e| BizClawError::Tool(format!("Create context-tree dir: {e}")))?;

        let timestamp = chrono::Utc::now().format("%Y%m%d_%H%M%S");
        let filename = format!("curated_{}.md", timestamp);
        let filepath = context_tree.join(&filename);

        let content = format!(
            "# Curated Knowledge\n\n\
             **Date:** {}\n\
             **Source:** BizClaw Agent (auto-curated)\n\n\
             ## Summary\n\n\
             {}\n",
            chrono::Utc::now().format("%Y-%m-%d %H:%M:%S UTC"),
            summary,
        );

        std::fs::write(&filepath, &content)
            .map_err(|e| BizClawError::Tool(format!("Write curated file: {e}")))?;

        Ok(ToolResult {
            tool_call_id: String::new(),
            output: format!(
                "✅ Knowledge saved to context tree (local fallback).\n\
                 File: {}\n\n\
                 💡 Install brv CLI for full LLM-powered curation:\n\
                 curl -fsSL https://byterover.dev/install.sh | sh",
                filepath.display()
            ),
            success: true,
        })
    }
}

// ── Helper: manual FTS search through context tree markdown files ──

fn search_context_tree(dir: &std::path::Path, query: &str) -> Result<String> {
    let query_lower = query.to_lowercase();
    let keywords: Vec<&str> = query_lower.split_whitespace().collect();
    let mut results = Vec::new();

    fn walk_md(dir: &std::path::Path, keywords: &[&str], results: &mut Vec<(String, f32, String)>) {
        if let Ok(entries) = std::fs::read_dir(dir) {
            for entry in entries.flatten() {
                let path = entry.path();
                if path.is_dir() {
                    walk_md(&path, keywords, results);
                } else if path.extension().map_or(false, |e| e == "md") {
                    if let Ok(content) = std::fs::read_to_string(&path) {
                        let content_lower = content.to_lowercase();
                        let matches: usize = keywords
                            .iter()
                            .filter(|kw| content_lower.contains(**kw))
                            .count();

                        if matches > 0 {
                            let score = matches as f32 / keywords.len() as f32;
                            let filename = path
                                .file_name()
                                .unwrap_or_default()
                                .to_string_lossy()
                                .to_string();

                            // Extract first ~500 chars
                            let snippet = if content.len() > 500 {
                                format!("{}...", &content[..500])
                            } else {
                                content.clone()
                            };

                            results.push((filename, score, snippet));
                        }
                    }
                }
            }
        }
    }

    walk_md(dir, &keywords, &mut results);
    results.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap_or(std::cmp::Ordering::Equal));

    let mut output = String::new();
    for (i, (filename, score, snippet)) in results.iter().take(5).enumerate() {
        output.push_str(&format!(
            "### {}. {} (relevance: {:.0}%)\n\n{}\n\n",
            i + 1,
            filename,
            score * 100.0,
            snippet,
        ));
    }

    Ok(output)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_brv_available_check() {
        // Just verify the function doesn't panic
        let _ = is_brv_available();
    }

    #[test]
    fn test_context_tree_search_empty() {
        let tmp = std::env::temp_dir().join("brv_test_empty");
        let _ = std::fs::create_dir_all(&tmp);
        let result = search_context_tree(&tmp, "test query").unwrap();
        assert!(result.is_empty());
        let _ = std::fs::remove_dir_all(&tmp);
    }

    #[test]
    fn test_context_tree_search_with_files() {
        let tmp = std::env::temp_dir().join("brv_test_search");
        let _ = std::fs::create_dir_all(&tmp);
        std::fs::write(
            tmp.join("auth.md"),
            "# Authentication\nJWT tokens with bcrypt password hashing.\nTokens expire in 24 hours.\n",
        )
        .unwrap();
        std::fs::write(
            tmp.join("database.md"),
            "# Database\nPostgreSQL with connection pooling.\nMigrations run on startup.\n",
        )
        .unwrap();

        let result = search_context_tree(&tmp, "JWT authentication bcrypt").unwrap();
        assert!(result.contains("auth.md"), "Should find auth.md");

        let result2 = search_context_tree(&tmp, "PostgreSQL database").unwrap();
        assert!(result2.contains("database.md"), "Should find database.md");

        let _ = std::fs::remove_dir_all(&tmp);
    }
}
