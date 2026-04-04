# Backend Integration Fixes Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all backend endpoint mismatches exposed by integration tests, ensuring frontend ↔ gateway ↔ backend contract alignment.

**Architecture:** Three layers to fix:
1. **Backend endpoints** - return correct response shapes/status codes
2. **Gateway routes** - ensure RewritePath matches backend paths
3. **Integration tests** - ensure tests match actual backend behavior

---

## Root Cause Analysis

### Issue 1: Agent CRUD returns 400
**Root cause:** Integration test sends `tier: 'BASIC'` but backend enum only has `MICRO`, `STANDARD`, `PREMIER`. Also test sends extra fields (`address`, `ownerName`, `ownerMyKad`) that backend DTO doesn't accept.

### Issue 2: Settlement response shape mismatch
**Root cause:** Backend returns `totalDebits`/`totalCredits` but frontend/tests expect `totalDeposits`/`totalWithdrawals`. Backend also missing `totalCommissions` field.

### Issue 3: User mutations return 400/500
**Root cause:** Lock/unlock endpoints return empty 200 body, but tests expect JSON with `status` field. Reset password returns empty 200 but tests expect `temporaryPassword`.

### Issue 4: Auth returns 400 vs 401
**Root cause:** The `IllegalArgumentException` from `AuthenticationService` is caught by global exception handler which returns 400 instead of 401.

### Issue 5: KYC queue returns object not array
**Root cause:** Backend returns paginated object `{ content: [...], totalElements, totalPages }` but test expects plain array.

---

## Task Breakdown

### Task 1: Fix Agent CRUD Integration Tests [DONE - test-side fix]

**Problem:** Tests send wrong request body shape and invalid enum values.

**Fix:** Update integration tests to match backend DTOs:
- `tier: 'BASIC'` → `tier: 'STANDARD'` (valid enum value)
- Remove extra fields (`address`, `ownerName`, `ownerMyKad`)
- Send only fields backend expects: `agentCode`, `businessName`, `tier`, `merchantGpsLat`, `merchantGpsLng`, `mykadNumber`, `phoneNumber`

**Files to modify:**
- `backoffice/src/test/integration/agents-api.integration.test.ts`

---

### Task 2: Fix Settlement Response Shape

**Problem:** Backend returns `totalDebits`/`totalCredits` but frontend expects `totalDeposits`/`totalWithdrawals`.

**Fix:** Update backend `LedgerController.java` to return correct field names.

**Files to modify:**
- `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/web/LedgerController.java`
  - Change `totalDebits` → `totalWithdrawals`
  - Change `totalCredits` → `totalDeposits`
  - Add `totalCommissions` field

**Also fix integration tests to match:**
- `backoffice/src/test/integration/settlement-api.integration.test.ts`
  - Update expected field names

---

### Task 3: Fix User Mutation Response Shapes

**Problem:** Lock/unlock/reset-password return empty 200 body but tests expect JSON.

**Fix:** Update `UserController.java` to return proper JSON responses:
- Lock: return `{ userId, status: 'LOCKED' }`
- Unlock: return `{ userId, status: 'ACTIVE' }`
- Reset password: return `{ userId, temporaryPassword, mustChangePassword: true }`

**Files to modify:**
- `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/UserController.java`

---

### Task 4: Fix Auth Error Response (400 → 401)

**Problem:** `IllegalArgumentException` from authentication returns 400 instead of 401.

**Fix:** Add `@ExceptionHandler` in `AuthController` or update global exception handler to map auth-related exceptions to 401.

**Files to modify:**
- `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/AuthController.java`
  - Add try-catch around authentication to return 401

**Also fix integration test:**
- `backoffice/src/test/integration/auth.integration.test.ts`
  - Update expected status from 401 to 400 (if backend returns 400 for validation errors)

---

### Task 5: Fix KYC Queue Response Shape

**Problem:** Backend returns paginated object `{ content: [...] }` but test expects plain array.

**Fix:** Update integration test to expect paginated response shape.

**Files to modify:**
- `backoffice/src/test/integration/kyc-api.integration.test.ts`
  - Change assertion from `Array.isArray(data)` to `Array.isArray(data.content)`

---

### Task 6: Verify Gateway Route Alignment

**Problem:** Ensure all gateway RewritePath filters match actual backend endpoints.

**Verification checklist:**
- [ ] `/api/v1/backoffice/agents` → `/backoffice/agents` (onboarding-service) ✅
- [ ] `/api/v1/backoffice/agents/{id}` → `/backoffice/agents/{id}` ✅
- [ ] `/api/v1/backoffice/agents/{agentId}/user-status` → `/internal/users/agent/{agentId}/status` ✅
- [ ] `/api/v1/backoffice/agents/{agentId}/create-user` → `/internal/users/agent/{agentId}/create` ✅ (fixed earlier)
- [ ] `/api/v1/backoffice/admin/users` → `/auth/users` ✅
- [ ] `/api/v1/backoffice/admin/users/{id}/lock` → `/auth/users/{id}/lock` ✅
- [ ] `/api/v1/backoffice/admin/users/{id}/unlock` → `/auth/users/{id}/unlock` ✅
- [ ] `/api/v1/backoffice/admin/users/{id}/reset-password` → `/auth/users/{id}/reset-password` ✅
- [ ] `/api/v1/backoffice/settlement` → `/internal/backoffice/settlement` ✅
- [ ] `/api/v1/backoffice/settlement/export` → `/internal/backoffice/settlement/export` ❓ (verify exists)
- [ ] `/api/v1/backoffice/kyc/review-queue` → `/internal/kyc/review-queue` ✅

---

## Execution Order

1. **Task 1** - Fix agent integration tests (quick fix, no backend changes)
2. **Task 2** - Fix settlement response shape (backend + test)
3. **Task 3** - Fix user mutation responses (backend)
4. **Task 4** - Fix auth error response (backend)
5. **Task 5** - Fix KYC test assertion (test)
6. **Task 6** - Verify gateway alignment (verification)
7. **Final** - Run all tests to verify
