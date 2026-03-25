
---
description: Resume an in progress implementation plan from a previous sessio
---

# Overview

Load a plan file requested by human partner and resume the executing.

**Note:** 
You MUST use either /subagent-driven-development skill or /executing-plans skill to execute the plan
The quality of its work will be significantly higher if run on a platform with subagent support. If subagents are available, use /subagent-driven-development to do the execution instead of the /executing-plans skill.

## Resuming from a previous session

1. Read the plan file
2. Skip all `[DONE]` tasks
3. Resume from first `[IN_PROGRESS]` task (or first `[PENDING]` if none)
4. Create TodoWrite with all remaining tasks on the plan (using the same Task name)
5. Dispatch implementer for the first [IN_PROGRESS] Task

The plan file is the single source of truth — [DONE] markers survive sessions.
