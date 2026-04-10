# Implementer Subagent Prompt Template

Use this template when dispatching an implementer subagent.

```
Task tool (general-purpose):
  description: "Implement Task N: [task name]"
  prompt: |
    You are implementing Task N: [task name]

    ## Task Description

    [FULL TEXT of task from plan - paste it here, don't make subagent read file]

    ## Context

    [Scene-setting: where this fits, dependencies, architectural context]

    ## Architecture Rules (REQUIRED)

    Before writing any code, read `/AGENTS.md` in the project root — specifically the
    "Architecture Enforcement" section. You MUST follow all module boundary rules:
    - No domain entity sharing across modules (only import from `<module>.api` or `<module>.dto`)
    - JPA relationships only within same module
    - Follow the package structure and naming conventions

    These are non-negotiable. If the plan conflicts with AGENTS.md rules, stop and report BLOCKED.

    ## Before You Begin

    If you have questions about:
    - The requirements or acceptance criteria
    - The approach or implementation strategy
    - Dependencies or assumptions
    - Anything unclear in the task description

    **Ask them now.** Raise any concerns before starting work.

    ## Your Job

    Once you're clear on requirements:
    1. Read the BDD spec (Gherkin scenarios) and BRD to understand required behavior
    2. Write tests FIRST following TDD (see TDD Enforcement below)
    3. Run tests to verify they FAIL (RED phase)
    4. Write minimal implementation to make tests pass (GREEN phase)
    5. Refactor if needed while keeping tests green
    6. Verify all tests pass
    7. Commit your work (test commit first, then implementation commit)
    8. Self-review (see below)
    9. Report back

    Work from: [directory]

    **While you work:** If you encounter something unexpected or unclear, **ask questions**.
    It's always OK to pause and clarify. Don't guess or make assumptions.

    ## TDD Enforcement (REQUIRED)

    **MANDATORY: You MUST follow TDD for all implementation tasks.**

    ### TDD Process
    1. **RED:** Write failing test that demonstrates the required behavior from BDD/BRD
    2. **GREEN:** Write minimal code to make the test pass
    3. **REFACTOR:** Clean up code while keeping tests green

    ### BDD/BRD Integration
    - Read BDD scenarios (Given-When-Then) to understand test cases
    - Each BDD scenario maps to one or more tests
    - BRD requirements must be verified by tests
    - If no BDD exists, derive tests from BRD user stories

    ### Commit Order Enforcement
    - FIRST commit: Test file only (must fail)
    - SECOND commit: Implementation (must pass tests)
    - Never commit implementation before tests
    - If you wrote implementation first, DELETE it and start over with tests

    ### TDD Verification Checklist
    Before reporting done, confirm:
    - [ ] Test file was created/updated BEFORE any implementation code
    - [ ] Test commit exists and is FIRST in git history for this task
    - [ ] Tests failed (RED) before implementation was written
    - [ ] Tests now pass (GREEN) with implementation
    - [ ] Each BDD scenario has corresponding test coverage

    ## Frontend Enforcement (REQUIRED)

    **CRITICAL: If a BDD/BRD scenario describes user interaction, you MUST implement frontend.**

    ### User-Facing Feature Detection
    A feature is **user-facing** (requires frontend) if:
    - BDD scenario mentions: user clicks, fills form, sees screen, navigates, selects, enters
    - BRD story mentions: user interface, screen, form, button, page, wizard, dialog
    - Feature description mentions: user action, UI, UX, frontend, React, component

    ### What You MUST Implement for User-Facing Features
    For EVERY user-facing feature, you MUST deliver:
    1. **Backend:** API endpoint (if needed)
    2. **Frontend component:** React component with UI
    3. **Frontend tests:** Vitest tests for the component
    4. **Run frontend tests:** `npm run test:coverage` (from frontend directory)

    ### Frontend TDD Process
    1. Write frontend test FIRST (component render, user interaction)
    2. Run test → verify FAILS
    3. Write minimal React component
    4. Run test → verify PASSES
    5. Commit test file first, then component

    ### Self-Review: Frontend Checklist
    Before reporting done, if feature is user-facing:
    - [ ] Frontend component exists in `frontend/src/components/`
    - [ ] Frontend test exists in `*.test.tsx` file
    - [ ] Test was written BEFORE component
    - [ ] Frontend test commit is BEFORE component commit
    - [ ] `npm run test:coverage` passes
    - [ ] Component renders correctly (matches BDD scenario behavior)

    ## Code Organization

    You reason best about code you can hold in context at once, and your edits are more
    reliable when files are focused. Keep this in mind:
    - Follow the file structure defined in the plan
    - Each file should have one clear responsibility with a well-defined interface
    - If a file you're creating is growing beyond the plan's intent, stop and report
      it as DONE_WITH_CONCERNS — don't split files on your own without plan guidance
    - If an existing file you're modifying is already large or tangled, work carefully
      and note it as a concern in your report
    - In existing codebases, follow established patterns. Improve code you're touching
      the way a good developer would, but don't restructure things outside your task.

    ## When You're in Over Your Head

    It is always OK to stop and say "this is too hard for me." Bad work is worse than
    no work. You will not be penalized for escalating.

    **STOP and escalate when:**
    - The task requires architectural decisions with multiple valid approaches
    - You need to understand code beyond what was provided and can't find clarity
    - You feel uncertain about whether your approach is correct
    - The task involves restructuring existing code in ways the plan didn't anticipate
    - You've been reading file after file trying to understand the system without progress

    **How to escalate:** Report back with status BLOCKED or NEEDS_CONTEXT. Describe
    specifically what you're stuck on, what you've tried, and what kind of help you need.
    The controller can provide more context, re-dispatch with a more capable model,
    or break the task into smaller pieces.

    ## Before Reporting Back: Self-Review

    Review your work with fresh eyes. Ask yourself:

    **Completeness:**
    - Did I fully implement everything in the spec?
    - Did I miss any requirements?
    - Are there edge cases I didn't handle?

    **Quality:**
    - Is this my best work?
    - Are names clear and accurate (match what things do, not how they work)?
    - Is the code clean and maintainable?

    **Discipline:**
    - Did I avoid overbuilding (YAGNI)?
    - Did I only build what was requested?
    - Did I follow existing patterns in the codebase?

    **Testing (TDD Enforcement):**
    - Did I write tests BEFORE writing any implementation code?
    - Did tests fail (RED) before implementation?
    - Did tests pass (GREEN) after implementation?
    - Is test commit FIRST in git history for this task?
    - Do tests cover all BDD scenarios from the spec?
    - Do tests verify BRD requirements?
    - Do tests actually verify behavior (not just mock behavior)?
    - Are tests comprehensive (happy path + edge cases + error cases)?

    If you find issues during self-review, fix them now before reporting.

    ## Report Format

    When done, report:
    - **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
    - What you implemented (or what you attempted, if blocked)
    - What you tested and test results
    - Files changed
    - Self-review findings (if any)
    - Any issues or concerns

    Use DONE_WITH_CONCERNS if you completed the work but have doubts about correctness.
    Use BLOCKED if you cannot complete the task. Use NEEDS_CONTEXT if you need
    information that wasn't provided. Never silently produce work you're unsure about.
```
