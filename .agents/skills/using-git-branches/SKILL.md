---
name: using-git-branches
description: Use when starting feature work or before executing implementation plans - creates isolated git branch with safety verification
---

# Using Git Branches

## Overview

Git branches create isolated workspaces sharing the same repository

**Core principle:** Systematic directory selection + safety verification = reliable isolation.

**Announce at start:** "I'm using the using-git-branches skill to set up an safe workspace for changes"

## Safety Verification

**MUST Checking the current branch before creating branch:**

```bash
# check which branches exist in your local repository and identify which one you’re currently on
git branch
```

**If NOT exist:**

```bash
# list all branches on local and remote
git branch -a
```

**Why critical:** Prevents accidentally forget to switch to new branch, all changed files will be ignored in the commit


## Creation Steps

### 1. Detect Project Name

```bash
project=$(basename "$(git rev-parse --show-toplevel)")
```

### 2. Create branch

```bash
# Create new branch if the branch specified does not exist, then switch to new branch
git switch -c  "$BRANCH_NAME"
```

### 3. Run Project Setup

Auto-detect and run appropriate setup:

```bash
# Node.js
if [ -f package.json ]; then npm install; fi

# Rust
if [ -f Cargo.toml ]; then cargo build; fi

# Python
if [ -f requirements.txt ]; then pip install -r requirements.txt; fi
if [ -f pyproject.toml ]; then poetry install; fi

# Go
if [ -f go.mod ]; then go mod download; fi
```

### 4. Verify Clean Baseline

Run tests to ensure worktree starts clean:

```bash
# Examples - use project-appropriate command
npm test
cargo test
pytest
go test ./...
```

**If tests fail:** Report failures, ask whether to proceed or investigate.

**If tests pass:** Report ready.

### 5. Report Location

```
Branch ready with name = $BRANCH_NAME
Tests passing (<N> tests, 0 failures)
Ready to implement <feature-name>
```

## Quick Reference

| Situation | Action |
|-----------|--------|
| Branch exists | Use it (verify ignored) |
| Directory not ignored | Add to .gitignore + commit |
| Tests fail during baseline | Report failures + ask |
| No package.json/Cargo.toml | Skip dependency install |

## Common Mistakes

### Proceeding with failing tests

- **Problem:** Can't distinguish new bugs from pre-existing issues
- **Fix:** Report failures, get explicit permission to proceed

### Hardcoding setup commands

- **Problem:** Breaks on projects using different tools
- **Fix:** Auto-detect from project files (package.json, etc.)

## Example Workflow

```
You: I'm using the using-git-branches skill to set up an working branch for changes.

[Check branch exists]
[Create branch: git switch -c feature/auth]
[Run npm install]
[Run npm test - 47 passing]

feature/auth
Tests passing (47 tests, 0 failures)
Ready to implement auth feature
```

## Red Flags

**Never:**
- Create branch without verifying it's existing
- Skip baseline test verification
- Proceed with failing tests without asking

**Always:**
- Verify branch is existing
- Auto-detect and run project setup
- Verify clean test baseline

## Integration

**Called by:**
- **brainstorming** (Phase 4) - REQUIRED when design is approved and implementation follows
- **subagent-driven-development** - REQUIRED before executing any tasks
- **executing-plans** - REQUIRED before executing any tasks
- Any skill needing isolated workspace

**Pairs with:**
- **finishing-a-development-branch** - REQUIRED for cleanup after work complete
