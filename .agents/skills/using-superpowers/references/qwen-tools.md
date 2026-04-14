# Qwen Code Tool Mapping

Skills use Claude Code tool names. When you encounter these in a skill, use your Qwen Code equivalent:

| Skill references | Qwen Code equivalent |
|-----------------|---------------------|
| `Read` (file reading) | `read_file` |
| `Write` (file creation) | `write_file` |
| `Edit` (file editing) | `edit` |
| `Bash` (run commands) | `run_shell_command` |
| `Grep` (search file content) | `grep_search` |
| `Glob` (search files by name) | `glob` |
| `TodoWrite` (task tracking) | `todo_write` |
| `Skill` tool (invoke a skill) | `skill` |
| `WebSearch` | `web_search` |
| `WebFetch` | `web_fetch` |
| `Task` tool (dispatch subagent) | `agent` |

## Subagent support

Qwen Code supports subagent dispatch via the `agent` tool. Skills that rely on subagent dispatch (`subagent-driven-development`, `dispatching-parallel-agents`) work natively.

## Additional Qwen Code tools

These tools are available in Qwen Code but have no Claude Code equivalent:

| Tool | Purpose |
|------|---------|
| `list_directory` | List files and subdirectories |
| `save_memory` | Persist facts to QWEN.md across sessions |
| `ask_user_question` | Request structured input from the user |
| `exit_plan_mode` | Switch from plan mode to implementation |
