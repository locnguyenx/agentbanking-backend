---
name: executing-plans
description: Use when you have a written implementation plan to execute in a separate session with review checkpoints
---

# Executing Plans

## Overview

Load plan, review critically, execute all tasks, report when complete.

**Announce at start:** "I'm using the executing-plans skill to implement this plan."

**Note:** Tell your human partner that Superpowers works much better with access to subagents. The quality of its work will be significantly higher if run on a platform with subagent support (such as Claude Code or Codex). If subagents are available, use superpowers:subagent-driven-development instead of this skill.

## Progress Persistence

Plan progress MUST be persisted in the plan file itself so it survives session interruptions.

**Status markers:**
- `[DONE]` — task completed and verified
- `[IN_PROGRESS]` — task currently being worked on
- `[BLOCKED]` — task blocked (add reason after marker)
- `[PENDING]` — not yet started

**When to update the plan file:**
1. Before starting a task → mark `[IN_PROGRESS]`
2. After task completes and passes verification → mark `[DONE]`
3. If blocked → mark `[BLOCKED]` with brief reason

**Resuming from a previous session:**
1. Read the plan file
2. Skip all `[DONE]` tasks
3. Resume from first `[IN_PROGRESS]` task (or first `[PENDING]` if none)
4. Create TodoWrite with remaining tasks only

## The Process

### Step 1: Load and Review Plan
1. Read plan file
2. Skip all `[DONE]` tasks — resume from first `[IN_PROGRESS]` or `[PENDING]`
3. Review critically - identify any questions or concerns about the plan
4. If concerns: Raise them with your human partner before starting
5. If no concerns: Create TodoWrite with remaining tasks and proceed

### Step 2: Execute Tasks

For each task:
1. Mark as `[IN_PROGRESS]` in plan file and TodoWrite
2. Follow each step exactly (plan has bite-sized steps)
3. Run verifications as specified
4. Mark as `[DONE]` in plan file and TodoWrite

### Step 3: Complete Development

After all tasks complete and verified:
- Announce: "I'm using the finishing-a-development-branch skill to complete this work."
- **REQUIRED SUB-SKILL:** Use superpowers:finishing-a-development-branch
- Follow that skill to verify tests, generate test report, present options, execute choice

## When to Stop and Ask for Help

**STOP executing immediately when:**
- Hit a blocker (missing dependency, test fails, instruction unclear)
- Plan has critical gaps preventing starting
- You don't understand an instruction
- Verification fails repeatedly

**Ask for clarification rather than guessing.**

## When to Revisit Earlier Steps

**Return to Review (Step 1) when:**
- Partner updates the plan based on your feedback
- Fundamental approach needs rethinking

**Don't force through blockers** - stop and ask.

## Remember
- Review plan critically first
- Follow plan steps exactly
- Don't skip verifications
- Reference skills when plan says to
- Stop when blocked, don't guess
- Never start implementation on main/master branch without explicit user consent
- **Update plan file with [DONE]/[IN_PROGRESS] markers for every task** — progress must persist across sessions

## Integration

**Required workflow skills:**
- **superpowers:using-git-worktrees** - REQUIRED: Set up isolated workspace before starting
- **superpowers:writing-plans** - Creates the plan this skill executes
- **superpowers:finishing-a-development-branch** - Complete development after all tasks
