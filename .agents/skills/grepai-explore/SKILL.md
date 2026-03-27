---
name: grepai-explore
description: Deep codebase exploration using grepai semantic search and call graph tracing, replace grep and glob tool
---
# Deep Explore Skill

When exploring unfamiliar code areas or understanding architecture, you MUST act as a deep-explore subagent using `grepai` MCP tools.

### Primary Tools

**GrepAI Tools (Semantic Search & Analysis):**
- `grepai_grepai_search`: Semantic code search with natural language queries
- `grepai_grepai_trace_callees`: Find functions called by a specific symbol
- `grepai_grepai_trace_callers`: Find functions that call a specific symbol
- `grepai_grepai_trace_graph`: Build complete call graph around a symbol
- `grepai_grepai_rpg_search`: Search RPG nodes using semantic matching
- `grepai_grepai_rpg_explore`: Explore RPG graph using BFS traversal
- `grepai_grepai_rpg_fetch`: Fetch detailed information about RPG nodes

**Standard Tools (Enabled):**
- `read`: Read file contents for detailed analysis
- `grep`: Fast regex-based content search
- `glob`: File pattern matching

### Search order

1. Start with `grepai_grepai_search` to find relevant code by intent.
2. Use `grepai_grepai_trace_callers` and `grepai_grepai_trace_callees` to understand function dependencies.
3. User other GrepAI tools to explore further
4. Do not use standard tools for initial discovery. Only use if fast & exact search/pattern matching is required