---
name: writing-plans
description: Use when you have a spec or requirements for a multi-step task, before touching code
---

# Writing Plans

## Overview

Write comprehensive implementation plans assuming the engineer has zero context for our codebase and questionable taste. Document everything they need to know: which files to touch for each task, code, testing, docs they might need to check, how to test it. Give them the whole plan as bite-sized tasks. DRY. YAGNI. TDD. Frequent commits. Reference BDD spec for Given-When-Then test scenarios; fall back to BRD/user stories. All specs are in `docs/superpowers/specs/`.

Assume they are a skilled developer, but know almost nothing about our toolset or problem domain. Assume they don't know good test design very well.

**Announce at start:** "I'm using the writing-plans skill to create the implementation plan."

**Context:** This should be run in a dedicated worktree (created by brainstorming skill).

**Save plans to:** `docs/superpowers/plans/YYYY-MM-DD-<feature-name>.md`
- (User preferences for plan location override this default)

## Scope Check

- If the spec covers multiple independent subsystems, it should have been broken into sub-project specs during brainstorming. If it wasn't, suggest breaking this into separate plans — one per subsystem. Each plan should produce working, testable software on its own.
- Check for BDD spec first (Gherkin scenarios drive TDD tests); if missing, flag for business-analysis skill.

## File Structure

Before defining tasks, map out which files will be created or modified and what each one is responsible for. This is where decomposition decisions get locked in.

- Design units with clear boundaries and well-defined interfaces. Each file should have one clear responsibility.
- You reason best about code you can hold in context at once, and your edits are more reliable when files are focused. Prefer smaller, focused files over large ones that do too much.
- Files that change together should live together. Split by responsibility, not by technical layer.
- In existing codebases, follow established patterns. If the codebase uses large files, don't unilaterally restructure - but if a file you're modifying has grown unwieldy, including a split in the plan is reasonable.

This structure informs the task decomposition. Each task should produce self-contained changes that make sense independently.

## Bite-Sized Task Granularity

**Each step is one action (2-5 minutes):**
- "Write the failing test" - step
- "Run it to make sure it fails" - step
- "Implement the minimal code to make the test pass" - step
- "Run the tests and make sure they pass" - step
- "Commit" - step

## Plan Document Header

**Every plan MUST start with this header:**

```markdown
# [Feature Name] Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** [One sentence describing what this builds]

**Architecture:** [2-3 sentences about approach]

**Tech Stack:** [Key technologies/libraries]

---
```

## Task Structure

**IMPORTANT: Every task MUST reference BDD scenarios or BRD requirements that drive the tests.**

````markdown
### Task N: [Component Name]

**BDD Scenarios:** Reference the specific BDD scenarios this task implements
- e.g., "Implements BDD Scenario S.1: Successful login, S.2: Invalid credentials"

**BRD Requirements:** Reference the BRD user stories/requirements
- e.g., "Fulfills BRD Story US-101: User can authenticate"

**User-Facing:** YES/NO (Does this task involve user interaction? If YES, frontend is REQUIRED)

**Files:**
- Create: `exact/path/to/file.py`
- Modify: `exact/path/to/existing.py:123-145`
- Test: `tests/exact/path/to/test.py`
- Frontend: `frontend/src/components/ComponentName.tsx` (if user-facing)
- Frontend Test: `frontend/src/components/ComponentName.test.tsx` (if user-facing)

- [ ] **Step 1: Write the failing backend test**

```python
def test_specific_behavior():
    result = function(input)
    assert result == expected
```
Align test with BDD scenario: e.g., Given [state], When [action], Then [outcome].
Example:

```text
# From BDD Scenario S.1: Successful login
def test_successful_login():
    # Given user credentials
    # When submit login
    # Then authenticated
```

- [ ] **Step 2: Run backend test to verify it fails**

Run: `pytest tests/path/test.py::test_name -v`
Expected: FAIL with "function not defined"

- [ ] **Step 3: Write minimal backend implementation**

```python
def function(input):
    return expected
```

- [ ] **Step 4: Run backend test to verify it passes**

Run: `pytest tests/path/test.py::test_name -v`
Expected: PASS

**IF USER-FACING (User-Facing: YES):**

- [ ] **Step 5: Write failing frontend test**

```tsx
// Align with BDD: "Given [state], When [user action], Then [expected UI]"
test('renders component and handles user interaction', () => {
  render(<ComponentName />);
  expect(screen.getByRole('button')).toBeInTheDocument();
  // Test user interaction from BDD scenario
});
```

- [ ] **Step 6: Run frontend test to verify it fails**

Run: `npm run test:coverage` (from frontend directory)
Expected: FAIL with "component not found"

- [ ] **Step 7: Write minimal frontend component**

```tsx
export const ComponentName = () => {
  return <button>Action</button>;
};
```

- [ ] **Step 8: Run frontend test to verify it passes**

Run: `npm run test:coverage`
Expected: PASS

- [ ] **Step 9: Commit (test-first order)**

```bash
# First commit: all test files
git add tests/path/test.py frontend/src/components/ComponentName.test.tsx
git commit -m "test: add tests for [feature]"

# Second commit: implementation files
git add src/path/file.py frontend/src/components/ComponentName.tsx
git commit -m "feat: implement [feature]"
```

**IF NOT USER-FACING (User-Facing: NO):**

- [ ] **Step 5: Commit (test-first order)**

```bash
# First commit: test file
git add tests/path/test.py
git commit -m "test: add tests for [feature]"

# Second commit: implementation file
git add src/path/file.py
git commit -m "feat: implement [feature]"
```
````

## Remember
- Exact file paths always
- Complete code in plan (not "add validation")
- Exact commands with expected output
- Reference relevant skills with @ syntax
- DRY, YAGNI, TDD, frequent commits
- **EVERY task must reference BDD scenarios or BRD requirements that drive the tests**
- **If BDD/BRD describes user interaction, task MUST be marked User-Facing: YES and include frontend steps**

## Plan Review Loop

After writing the complete plan:

1. Dispatch a single plan-document-reviewer subagent (see plan-document-reviewer-prompt.md) with precisely crafted review context — never your session history. This keeps the reviewer focused on the plan, not your thought process.
   - Provide: path to the plan document, path to specs document including BDD, BRD, and design.
2. If ❌ Issues Found: fix the issues, re-dispatch reviewer for the whole plan
3. If ✅ Approved: proceed to execution handoff

**Review loop guidance:**
- Same agent that wrote the plan fixes it (preserves context)
- If loop exceeds 3 iterations, surface to human for guidance
- Reviewers are advisory — explain disagreements if you believe feedback is incorrect

## Execution Handoff

After saving the plan, offer execution choice:

**"Plan complete and saved to `docs/superpowers/plans/<filename>.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?"**

**If Subagent-Driven chosen:**
- **REQUIRED SUB-SKILL:** Use superpowers:subagent-driven-development
- Fresh subagent per task + two-stage review

**If Inline Execution chosen:**
- **REQUIRED SUB-SKILL:** Use superpowers:executing-plans
- Batch execution with checkpoints for review
