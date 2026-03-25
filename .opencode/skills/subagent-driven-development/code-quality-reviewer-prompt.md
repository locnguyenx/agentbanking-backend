# Code Quality Reviewer Prompt Template

Use this template when dispatching a code quality reviewer subagent.

**Purpose:** Verify implementation is well-built (clean, tested, maintainable)

**Only dispatch after spec compliance review passes.**

```
Task tool (superpowers:code-reviewer):
  Use template at requesting-code-review/code-reviewer.md

  WHAT_WAS_IMPLEMENTED: [from implementer's report]
  PLAN_OR_REQUIREMENTS: Task N from [plan-file]
  BASE_SHA: [commit before task]
  HEAD_SHA: [current commit]
  DESCRIPTION: [task summary]
```

**In addition to standard code quality concerns, the reviewer should check:**
- Does each file have one clear responsibility with a well-defined interface?
- Are units decomposed so they can be understood and tested independently?
- Is the implementation following the file structure from the plan?
- Did this implementation create new files that are already large, or significantly grow existing files? (Don't flag pre-existing file sizes — focus on what this change contributed.)

**Architecture compliance (REQUIRED):**
Read `/AGENTS.md` in the project root — specifically the "Architecture Enforcement" section. Verify:
- No cross-module domain entity imports (must use `<module>.api` or `<module>.dto` only)
- No JPA relationships (`@ManyToOne`, `@OneToMany`) referencing entities from other modules
- Follows package structure: `com.banking.<module>.<layer>`
- Entities include `AuditFields` embeddable
- Uses centralized error handling (MessageCatalog + BaseException pattern)

**TDD compliance (REQUIRED):**
Verify the implementation followed TDD:
- [ ] Git history shows test commit BEFORE implementation commit
- [ ] Test files exist and were created before implementation code
- [ ] Tests cover the required behavior from BDD/BRD specs
- [ ] Tests verify business requirements, not just implementation details

**Frontend compliance (for User-Facing features):**
If task is marked User-Facing: YES or BDD/BRD describes user interaction:
- [ ] Frontend component exists in `frontend/src/components/`
- [ ] Frontend test file exists (*.test.tsx)
- [ ] Frontend test commit is BEFORE component commit
- [ ] Frontend test covers BDD user interaction scenario
- [ ] `npm run test:coverage` passes
- [ ] Component follows project conventions (Ant Design, TypeScript, functional components)

**Code reviewer returns:** Strengths, Issues (Critical/Important/Minor), Assessment
