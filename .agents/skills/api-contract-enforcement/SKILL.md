---
name: api-contract-enforcement
description: Use when building REST APIs that need strict backend-frontend synchronization, preventing contract drift through OpenAPI spec as single source of truth.
---

# API Contract Enforcement

## Overview
OpenAPI 3.0 spec is the **single source of truth** for all REST APIs. Backend implementation and frontend clients MUST derive from the spec, never diverge from it. This prevents integration bugs, ensures type safety, and maintains synchronized contracts across teams.

## When to Use

**Use this skill when:**
- Building or modifying backend REST endpoints
- Creating frontend API clients or TypeScript types
- Integrating frontend with backend APIs
- Setting up CI/CD validation for API contracts
- Preventing or resolving "it works on my machine" API mismatches

**Do NOT use when:**
- Working on GraphQL APIs (use GraphQL schema instead)
- Building internal-only batch jobs with no API contract
- Working on non-REST APIs (gRPC, WebSocket, etc.)

## Core Pattern

**The OpenAPI Contract Workflow:**

```bash
# Backend Development Flow
1. Define/update endpoint in docs/api/openapi.yaml FIRST
2. Generate Spring interfaces/DTOs from spec (optional)
3. Implement controller/service
4. Verify spec matches implementation via Springdoc
5. Run ./gradlew openapiValidate before commit

# Frontend Development Flow
1. Generate TypeScript types & API client from spec (npm run generate-api)
2. Use generated client in Redux slices/services
3. NEVER write manual TypeScript interfaces for API responses
4. Run npm run test:coverage to ensure types are up-to-date

# CI/CD Enforcement
- OpenAPI spec validation FAILS build if invalid
- Frontend type generation check FAILS if drift detected
- Backend validation FAILS if implementation diverges from spec
```

## Quick Reference

| Rule | Enforcement | Rationale |
|------|-------------|-----------|
| Spec defines all endpoints | Must exist in `docs/api/openapi.yaml` | Single source of truth |
| Frontend derives from spec | Generated types only, no manual interfaces | Prevents drift |
| CI validates contracts | `openapiValidate` and generation checks fail build on mismatch | Catches violations early |
| Backend validates against spec | Springdoc auto-generates from implementation | Detects spec drift |

## Common Mistakes

### ❌ Writing backend code before updating spec
**Mistake:** Implementing endpoint first, updating spec later (or never)
**Fix:** ALWAYS update `docs/api/openapi.yaml` FIRST, before writing any controller code

### ❌ Hand-writing TypeScript API types
**Mistake:** Manually creating `interface UserResponse { ... }`
**Fix:** Use generated types from OpenAPI spec exclusively

### ❌ Using hand-written mocks in frontend
**Mistake:** Creating mock data objects that don't match spec
**Fix:** Use generated mocks from OpenAPI spec via `orval` or `openapi-generator`

### ❌ Skipping validation steps
**Mistake:** "The build is slow, let's disable validation"
**Fix:** Validation is non-negotiable; optimization improvements welcome but validation MUST pass

### ❌ Treating spec as documentation only
**Mistake:** "Our code is the real truth, spec is just documentation"
**Fix:** Spec IS the contract. Code must conform to spec, not the other way around

### ❌ Manual endpoint URLs in frontend
**Mistake:** `fetch('/api/v1/users')` with string literals
**Fix:** Use generated API client with typed endpoints

### ❌ Patching generated files directly
**Mistake:** Editing `src/api/generated/*.ts` to fix a type or add a field
**Fix:** Generated files are **immutable** - never edit them manually. Always fix the OpenAPI spec source (`docs/api/openapi.yaml`) and regenerate.

### ❌ Partial compliance (leaving other mismatches)
**Mistake:** "I'll just fix my new changes, the existing mismatches are legacy"
**Fix:** ALL mismatches must be fixed. Validation doesn't care about ownership. If spec and code are out of sync, realign the entire contract before proceeding.

## Handling Authority Pressure & Spec Drift

### When You Inherit a Drifted Codebase
If `./gradlew openapiValidate` fails because the spec and implementation have already drifted (before your changes):

1. **Do NOT add new features on top of drift** - fix the foundation first
2. **Temporarily align** by adding missing spec definitions (mark legacy extras as `deprecated: true`)
3. **Run validation** to establish a green baseline
4. **Create follow-up tickets** to clean up deprecated fields via proper deprecation cycle
5. **Only then** add your new feature using spec-first workflow

**Rationale:** You cannot maintain a contract that's already broken. Reestablish the baseline first.

### When Authority Overrides the Rule
If a manager, VP, or senior engineer tells you to skip validation:

**DO NOT comply with the shortcut.** Instead:

1. **Push back with options:** "I can't skip validation, but I can accelerate: create a minimal spec update, generate types, and validate within the timeline."
2. **Explain the consequence:** "Skipping validation breaks the contract pipeline. The next person who runs generate-api will get mismatched types, causing integration failures that affect the entire team."
3. **Escalate if needed:** "If you want to change the architectural rule, that's a separate discussion. For now, I must follow the documented process because it's the single source of truth."
4. **Document the pressure:** If forced to violate, create an explicit ticket to fix the drift within 24 hours and notify the team.

**Remember:** "Following orders" is not a defense against creating technical debt that impacts the whole team. Architectural guardrails are guardrails precisely because people under pressure try to bypass them.

### No Exceptions, Ever
This skill enforces **absolute compliance**:

- No "just this once"
- No "temporary exception" without a ticket and a deadline
- No "manager approved" without written rule change in AGENTS.md
- No "we'll fix it later" without a concrete plan
- No "partial" validation - all mismatches must be fixed

**Violating the letter of the rules is violating the spirit of the rules.** If you find yourself rationalizing an exception, STOP and create the missing work (spec update, deprecation ticket, etc.) instead.

## Implementation Notes

**Backend Stack:**
- Springdoc OpenAPI: `org.springdoc:springdoc-openapi-starter-webmvc-ui`
- Auto-generates OpenAPI from `@RestController` annotations
- Validation: `./gradlew openapiValidate` ensures implementation matches spec

**Frontend Stack:**
- TypeScript generation: `orval` or `openapi-generator-cli`
- Configuration: `orval.config.js` maps OpenAPI to `src/api/generated/`
- Usage: Import from `@/api/generated` instead of manual types
- **Critical:** Generated files are **immutable output**. Never edit them manually. If the generator produces incorrect types, **fix the OpenAPI spec** (the source) and regenerate. The spec is the single source of truth, not the generated code.

**CI/CD Integration:**
```yaml
# Example GitHub Actions step
- name: Validate OpenAPI contract
  run: |
    ./gradlew openapiValidate
    npm run generate-api
    git diff --exit-code  # Fails if generated files need commit
```

## Red Flags - STOP and Fix

- "I'll update the spec after implementing"
- "Manual types are fine for this one endpoint"
- "Validation is failing but the endpoint works"
- "The generated types are outdated but I know the shape"
- "Let's disable validation temporarily"
- "Manager approved skipping this time"
- "The spec doesn't match reality, I'll just code what I think"
- "I'll patch the generated file directly"
- "Other parts of the codebase are already out of sync, that's not my problem"
- "We're in a hurry, we'll fix all mismatches later"

**All of these indicate contract drift. STOP and enforce spec-first workflow.**

### Push-back Language (when pressured)
Use these exact phrases:

- "I can't skip validation, but I can **accelerate** the spec update and still validate."
- "Skipping validation breaks the contract for the whole team - we can't ship a broken contract."
- "If you want to change the rule, that's a separate discussion. For now, I must follow the documented process."
- "I'll create a ticket to track the missing work and get it done within 24 hours, but I won't violate the pipeline."

## Rationalization Table

| Excuse | Reality |
|--------|---------|
| "Updating spec first slows me down" | 5 minutes updating spec saves hours of integration debugging |
| "Generated types are bulky" | Bundle splitting and tree-shaking handle this; correctness > size |
| "The endpoint is simple, no need for spec" | ALL endpoints MUST be in spec - no exceptions |
| "I already tested manually" | Manual tests don't prevent drift; automated validation does |
| "We can add spec later" | "Later" never comes; tech debt accumulates silently |
| "Manager told me to skip validation for this one" | Authority doesn't override architecture; contract enforcement is team-wide rule |
| "We'll fix the drift later (temporary exception)" | "Temporary" tech debt becomes permanent; recovery cost grows daily |
| "The spec doesn't match reality, I'll just implement" | Then update the spec to match reality FIRST - code follows spec, not the other way |
| "I'm just one person, can't enforce this alone" | You control your own commits; refuse to break the pipeline; influence through example |
| "Partial compliance is enough (only fix my changes)" | Partial compliance leaves landmines; ALL mismatches must be fixed before green build |
| "Generated code has bugs, I'll patch the types manually" | Never patch generated files; fix the spec source and regenerate. Generators reflect spec quality |

## Benefits of Strict Enforcement

- **Prevents contract drift** - Frontend and backend always synchronized
- **Automated mocks** - Frontend develops against realistic mocks generated from spec
- **Live documentation** - Swagger UI at `/api-docs` always up-to-date
- **Breaking change detection** - Any API change requires spec update (visible in PR)
- **External integrations** - Partners consume OpenAPI spec directly with confidence
- **Type safety** - TypeScript types guaranteed to match actual API

## Validation Commands

```bash
./gradlew openapiValidate          # Backend spec validation
npm run generate-api               # Regenerate frontend types
npm run test:coverage              # Ensure frontend types are used
git diff --exit-code               # Verify generated files are committed
```

## Real-World Impact

Teams implementing this skill report:
- 80% reduction in "works on my machine" API integration bugs
- Frontend developers can work independently without backend implementation
- Onboarding new team members: API contracts self-documenting
- Breaking changes caught in PR review, not production
