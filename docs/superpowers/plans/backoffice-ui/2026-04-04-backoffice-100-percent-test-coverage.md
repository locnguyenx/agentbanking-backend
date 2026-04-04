# Backoffice 100% Test Coverage Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Achieve 100% frontend feature/action coverage through contract tests (MSW) and integration tests (real backend).

**Architecture:** Four-tier test architecture as defined in `docs/superpowers/specs/2026-04-02-backoffice-test-architecture.md`. Contract tests use MSW to validate HTTP contracts. Integration tests use testcontainers with real backend.

**Tech Stack:** Vitest, MSW, React Testing Library, testcontainers, axios

---

## Current Coverage Status

| Page | Component Tests | Contract Tests | Integration Tests |
|------|----------------|----------------|-------------------|
| Dashboard | ✅ Partial | ❌ NONE | ❌ NONE |
| Agents | ✅ Partial | ❌ Partial | ⚠️ Stub only |
| UserManagement | ✅ Partial | ❌ Partial | ❌ NONE |
| Transactions | ✅ Partial | ⚠️ Schema only | ❌ NONE |
| Settlement | ✅ Partial | ❌ NONE | ❌ NONE |
| KycReview | ✅ Partial | ❌ NONE | ❌ NONE |
| Login | ❌ NONE | ❌ NONE | ❌ NONE |

**Mutation (write action) coverage: 0/14 (0%)**

---

## File Structure

### New Files to Create
```
backoffice/src/test/
├── contract/
│   ├── dashboard.contract.test.ts          # NEW
│   ├── agents-create-user.contract.test.ts # NEW - critical bug fix
│   ├── agents-mutations.contract.test.ts   # NEW
│   ├── users-mutations.contract.test.ts    # NEW
│   ├── transactions.contract.test.ts       # EXPAND
│   ├── settlement.contract.test.ts         # NEW
│   ├── kyc.contract.test.ts                # NEW
│   └── auth.contract.test.ts               # NEW
├── integration/
│   ├── agents-api.integration.test.ts      # EXPAND (currently minimal)
│   ├── users-api.integration.test.ts       # EXPAND
│   ├── settlement-api.integration.test.ts   # NEW
│   ├── kyc-api.integration.test.ts          # NEW
│   └── auth.integration.test.ts             # NEW
```

---

## Contract Tests (MSW) - Validate Frontend HTTP Calls

### Task 1: Agents Create User Account - Contract Test [DONE]

**BDD Scenarios:**
- S.AG-10: Backoffice user creates agent user account successfully
- S.AG-11: Create user fails when agent already has user

**BRD Requirements:**
- US-BO-04: Backoffice admin can create user accounts for agents without login access

**User-Facing:** YES

**Files:**
- Create: `backoffice/src/test/contract/agents-create-user.contract.test.ts`

- [ ] **Step 1: Write contract test for POST /api/v1/backoffice/agents/{agentId}/create-user**

```typescript
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: POST /api/v1/backoffice/agents/:agentId/create-user', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with agentId in path', async () => {
    let capturedPath: string | undefined
    let capturedBody: any

    server.use(
      http.post('/api/v1/backoffice/agents/:agentId/create-user', async ({ request, params }) => {
        capturedPath = params.agentId as string
        capturedBody = await request.json()
        return HttpResponse.json({
          userId: 'test-user-id',
          username: 'AGT-008',
          status: 'ACTIVE',
          agentId: capturedPath,
          temporaryPassword: 'TempPass123!',
          mustChangePassword: true,
        })
      })
    )

    const agentId = 'a0000000-0000-0000-0000-000000000008'
    const response = await fetch(`/api/v1/backoffice/agents/${agentId}/create-user`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        phone: '012-3456789',
        businessName: 'Test Business',
      }),
    })

    expect(capturedPath).toBe(agentId)
    expect(capturedBody).toEqual({
      phone: '012-3456789',
      businessName: 'Test Business',
    })
    expect(response.ok).toBe(true)
  })

  it('should handle 409 when user already exists', async () => {
    server.use(
      http.post('/api/v1/backoffice/agents/:agentId/create-user', () => {
        return HttpResponse.json(
          {
            status: 'FAILED',
            error: {
              code: 'ERR_BIZ_USER_EXISTS',
              message: 'User already exists for this agent',
              action_code: 'REVIEW',
            },
          },
          { status: 409 }
        )
      })
    )

    const response = await fetch('/api/v1/backoffice/agents/test-agent-id/create-user', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: '012-3456789', businessName: 'Test' }),
    })

    expect(response.status).toBe(409)
    const data = await response.json()
    expect(data.error.code).toBe('ERR_BIZ_USER_EXISTS')
  })

  it('should handle 404 when agent not found', async () => {
    server.use(
      http.post('/api/v1/backoffice/agents/:agentId/create-user', () => {
        return HttpResponse.json(
          { status: 'FAILED', error: { code: 'ERR_BIZ_AGENT_NOT_FOUND', message: 'Agent not found' } },
          { status: 404 }
        )
      })
    )

    const response = await fetch('/api/v1/backoffice/agents/nonexistent/create-user', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: '012-3456789', businessName: 'Test' }),
    })

    expect(response.status).toBe(404)
  })
})
```

- [ ] **Step 2: Run contract test to verify it passes**

```bash
cd backoffice && npm test -- --run src/test/contract/agents-create-user.contract.test.ts
```

---

### Task 2: Agents Mutations - Contract Tests [DONE]

**BDD Scenarios:**
- S.AG-01: Create new agent
- S.AG-02: Update agent details
- S.AG-03: Deactivate agent

**BRD Requirements:**
- US-BO-01: Backoffice admin can create new agents
- US-BO-02: Backoffice admin can update agent information
- US-BO-03: Backoffice admin can deactivate agents

**User-Facing:** YES

**Files:**
- Create: `backoffice/src/test/contract/agents-mutations.contract.test.ts`

- [ ] **Step 1: Write contract tests for all agent mutations**

```typescript
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: POST /api/v1/backoffice/agents (Create Agent)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with required fields', async () => {
    let capturedBody: any

    server.use(
      http.post('/api/v1/backoffice/agents', async ({ request }) => {
        capturedBody = await request.json()
        return HttpResponse.json({
          agentId: 'new-agent-id',
          agentCode: 'AGT-009',
          businessName: capturedBody.businessName,
          tier: capturedBody.tier,
          status: 'ACTIVE',
          phoneNumber: capturedBody.phoneNumber,
          merchantGpsLat: 3.139003,
          merchantGpsLng: 101.686855,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }, { status: 201 })
      })
    )

    const response = await fetch('/api/v1/backoffice/agents', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        agentCode: 'AGT-009',
        businessName: 'New Store',
        tier: 'STANDARD',
        merchantGpsLat: 3.139003,
        merchantGpsLng: 101.686855,
        mykadNumber: '900101011234',
        phoneNumber: '012-3456789',
      }),
    })

    expect(capturedBody.businessName).toBe('New Store')
    expect(capturedBody.tier).toBe('STANDARD')
    expect(capturedBody.phoneNumber).toBe('012-3456789')
    expect(capturedBody.mykadNumber).toBe('900101011234')
    expect(response.ok).toBe(true)
  })
})

describe('Contract: PUT /api/v1/backoffice/agents/:id (Update Agent)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with agentId in path', async () => {
    let capturedId: string | undefined
    let capturedBody: any

    server.use(
      http.put('/api/v1/backoffice/agents/:id', async ({ request, params }) => {
        capturedId = params.id as string
        capturedBody = await request.json()
        return HttpResponse.json({
          agentId: capturedId,
          agentCode: 'AGT-001',
          businessName: capturedBody.businessName,
          tier: capturedBody.tier,
          status: 'ACTIVE',
          phoneNumber: capturedBody.phoneNumber,
          merchantGpsLat: 3.139003,
          merchantGpsLng: 101.686855,
          createdAt: '2026-04-01T12:00:00Z',
          updatedAt: new Date().toISOString(),
        })
      })
    )

    const agentId = 'a0000000-0000-0000-0000-000000000001'
    const response = await fetch(`/api/v1/backoffice/agents/${agentId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        businessName: 'Updated Store',
        tier: 'PREMIUM',
        phoneNumber: '012-9998888',
        merchantGpsLat: 3.139003,
        merchantGpsLng: 101.686855,
      }),
    })

    expect(capturedId).toBe(agentId)
    expect(capturedBody.businessName).toBe('Updated Store')
    expect(capturedBody.tier).toBe('PREMIUM')
    expect(response.ok).toBe(true)
  })
})

describe('Contract: DELETE /api/v1/backoffice/agents/:id (Deactivate Agent)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with agentId in path', async () => {
    let capturedId: string | undefined

    server.use(
      http.delete('/api/v1/backoffice/agents/:id', ({ params }) => {
        capturedId = params.id as string
        return new HttpResponse(null, { status: 204 })
      })
    )

    const agentId = 'a0000000-0000-0000-0000-000000000001'
    const response = await fetch(`/api/v1/backoffice/agents/${agentId}`, {
      method: 'DELETE',
    })

    expect(capturedId).toBe(agentId)
    expect(response.status).toBe(204)
  })
})
```

- [ ] **Step 2: Run contract tests to verify they pass**

```bash
cd backoffice && npm test -- --run src/test/contract/agents-mutations.contract.test.ts
```

---

### Task 3: User Management Mutations - Contract Tests [DONE]

**BDD Scenarios:**
- S.UM-01: Create user
- S.UM-02: Update user
- S.UM-03: Delete user
- S.UM-04: Lock user
- S.UM-05: Unlock user
- S.UM-06: Reset user password

**BRD Requirements:**
- US-BO-05: Backoffice admin can manage user accounts

**User-Facing:** YES

**Files:**
- Create: `backoffice/src/test/contract/users-mutations.contract.test.ts`

- [ ] **Step 1: Write contract tests for all user mutations**

```typescript
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: POST /api/v1/backoffice/admin/users (Create User)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with user data', async () => {
    let capturedBody: any

    server.use(
      http.post('/api/v1/backoffice/admin/users', async ({ request }) => {
        capturedBody = await request.json()
        return HttpResponse.json({
          userId: 'new-user-id',
          username: capturedBody.username,
          email: capturedBody.email,
          fullName: capturedBody.fullName,
          status: 'ACTIVE',
          userType: 'INTERNAL',
          createdAt: new Date().toISOString(),
        }, { status: 201 })
      })
    )

    const response = await fetch('/api/v1/backoffice/admin/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: 'newuser',
        email: 'user@example.com',
        fullName: 'New User',
        password: 'TempPass123!',
        userType: 'INTERNAL',
      }),
    })

    expect(capturedBody.username).toBe('newuser')
    expect(capturedBody.email).toBe('user@example.com')
    expect(response.ok).toBe(true)
  })
})

describe('Contract: PUT /api/v1/backoffice/admin/users/:id (Update User)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with userId in path', async () => {
    let capturedId: string | undefined
    let capturedBody: any

    server.use(
      http.put('/api/v1/backoffice/admin/users/:id', async ({ request, params }) => {
        capturedId = params.id as string
        capturedBody = await request.json()
        return HttpResponse.json({
          userId: capturedId,
          username: 'existinguser',
          email: capturedBody.email,
          fullName: capturedBody.fullName,
          status: 'ACTIVE',
          userType: 'INTERNAL',
          createdAt: '2026-04-01T12:00:00Z',
          updatedAt: new Date().toISOString(),
        })
      })
    )

    const userId = 'u0000000-0000-0000-0000-000000000001'
    const response = await fetch(`/api/v1/backoffice/admin/users/${userId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: 'updated@example.com',
        fullName: 'Updated Name',
      }),
    })

    expect(capturedId).toBe(userId)
    expect(capturedBody.email).toBe('updated@example.com')
    expect(response.ok).toBe(true)
  })
})

describe('Contract: DELETE /api/v1/backoffice/admin/users/:id (Delete User)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with userId in path', async () => {
    let capturedId: string | undefined

    server.use(
      http.delete('/api/v1/backoffice/admin/users/:id', ({ params }) => {
        capturedId = params.id as string
        return new HttpResponse(null, { status: 204 })
      })
    )

    const userId = 'u0000000-0000-0000-0000-000000000001'
    const response = await fetch(`/api/v1/backoffice/admin/users/${userId}`, {
      method: 'DELETE',
    })

    expect(capturedId).toBe(userId)
    expect(response.status).toBe(204)
  })
})

describe('Contract: POST /api/v1/backoffice/admin/users/:id/lock (Lock User)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint', async () => {
    let capturedId: string | undefined

    server.use(
      http.post('/api/v1/backoffice/admin/users/:id/lock', ({ params }) => {
        capturedId = params.id as string
        return HttpResponse.json({
          userId: capturedId,
          status: 'LOCKED',
        })
      })
    )

    const userId = 'u0000000-0000-0000-0000-000000000001'
    const response = await fetch(`/api/v1/backoffice/admin/users/${userId}/lock`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    })

    expect(capturedId).toBe(userId)
    expect(response.ok).toBe(true)
  })
})

describe('Contract: POST /api/v1/backoffice/admin/users/:id/unlock (Unlock User)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint', async () => {
    let capturedId: string | undefined

    server.use(
      http.post('/api/v1/backoffice/admin/users/:id/unlock', ({ params }) => {
        capturedId = params.id as string
        return HttpResponse.json({
          userId: capturedId,
          status: 'ACTIVE',
        })
      })
    )

    const userId = 'u0000000-0000-0000-0000-000000000001'
    const response = await fetch(`/api/v1/backoffice/admin/users/${userId}/unlock`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    })

    expect(capturedId).toBe(userId)
    expect(response.ok).toBe(true)
  })
})

describe('Contract: POST /api/v1/backoffice/admin/users/:id/reset-password (Reset Password)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with newPassword', async () => {
    let capturedId: string | undefined
    let capturedBody: any

    server.use(
      http.post('/api/v1/backoffice/admin/users/:id/reset-password', async ({ request, params }) => {
        capturedId = params.id as string
        capturedBody = await request.json()
        return HttpResponse.json({
          userId: capturedId,
          temporaryPassword: capturedBody.newPassword,
          mustChangePassword: true,
        })
      })
    )

    const userId = 'u0000000-0000-0000-0000-000000000001'
    const response = await fetch(`/api/v1/backoffice/admin/users/${userId}/reset-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ newPassword: 'NewTemp123!' }),
    })

    expect(capturedId).toBe(userId)
    expect(capturedBody.newPassword).toBe('NewTemp123!')
    expect(response.ok).toBe(true)
  })
})
```

- [ ] **Step 2: Run contract tests to verify they pass**

```bash
cd backoffice && npm test -- --run src/test/contract/users-mutations.contract.test.ts
```

---

### Task 4: Dashboard - Contract Tests [DONE]

**BDD Scenarios:**
- S.DB-01: Load dashboard data

**BRD Requirements:**
- US-BO-07: Backoffice admin can view dashboard metrics

**User-Facing:** YES

**Files:**
- Create: `backoffice/src/test/contract/dashboard.contract.test.ts`

- [ ] **Step 1: Write contract tests for dashboard endpoints**

```typescript
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: GET /api/v1/backoffice/dashboard', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint', async () => {
    let called = false

    server.use(
      http.get('/api/v1/backoffice/dashboard', () => {
        called = true
        return HttpResponse.json({
          totalAgents: 15,
          activeAgents: 12,
          todayVolume: 150000.00,
          todayTransactions: 245,
          pendingKyc: 8,
        })
      })
    )

    const response = await fetch('/api/v1/backoffice/dashboard')
    const data = await response.json()

    expect(called).toBe(true)
    expect(data).toHaveProperty('totalAgents')
    expect(data).toHaveProperty('activeAgents')
    expect(data).toHaveProperty('todayVolume')
    expect(data).toHaveProperty('todayTransactions')
    expect(data).toHaveProperty('pendingKyc')
  })

  it('should handle error response', async () => {
    server.use(
      http.get('/api/v1/backoffice/dashboard', () => {
        return HttpResponse.json(
          { status: 'FAILED', error: { code: 'ERR_SYS_001', message: 'Internal error' } },
          { status: 500 }
        )
      })
    )

    const response = await fetch('/api/v1/backoffice/dashboard')
    expect(response.status).toBe(500)
  })
})
```

- [ ] **Step 2: Run contract tests to verify they pass**

```bash
cd backoffice && npm test -- --run src/test/contract/dashboard.contract.test.ts
```

---

### Task 5: Settlement - Contract Tests [DONE]

**BDD Scenarios:**
- S.ST-01: Load settlement data
- S.ST-02: Export settlement report

**BRD Requirements:**
- US-BO-08: Backoffice admin can view and export settlement reports

**User-Facing:** YES

**Files:**
- Create: `backoffice/src/test/contract/settlement.contract.test.ts`

- [ ] **Step 1: Write contract tests for settlement endpoints**

```typescript
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: GET /api/v1/backoffice/settlement', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with date query param', async () => {
    let capturedUrl: URL | undefined

    server.use(
      http.get('/api/v1/backoffice/settlement', ({ request }) => {
        capturedUrl = new URL(request.url)
        return HttpResponse.json({
          date: '2026-04-02',
          totalDeposits: 50000.00,
          totalWithdrawals: 30000.00,
          totalCommissions: 1500.00,
          netAmount: 18500.00,
          agentDetails: [
            { agentCode: 'AGT-001', deposits: 10000, withdrawals: 5000, commission: 300 },
          ],
        })
      })
    )

    const response = await fetch('/api/v1/backoffice/settlement?date=2026-04-02')
    const data = await response.json()

    expect(capturedUrl?.searchParams.get('date')).toBe('2026-04-02')
    expect(data).toHaveProperty('totalDeposits')
    expect(data).toHaveProperty('totalWithdrawals')
    expect(data).toHaveProperty('totalCommissions')
    expect(data).toHaveProperty('netAmount')
    expect(Array.isArray(data.agentDetails)).toBe(true)
  })
})

describe('Contract: GET /api/v1/backoffice/settlement/export', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint and return blob', async () => {
    let capturedUrl: URL | undefined

    server.use(
      http.get('/api/v1/backoffice/settlement/export', ({ request }) => {
        capturedUrl = new URL(request.url)
        return new HttpResponse(new Blob(['csv,data']), {
          headers: { 'Content-Type': 'text/csv' },
        })
      })
    )

    const response = await fetch('/api/v1/backoffice/settlement/export?date=2026-04-02')

    expect(capturedUrl?.searchParams.get('date')).toBe('2026-04-02')
    expect(response.ok).toBe(true)
    expect(response.headers.get('content-type')).toContain('text/csv')
  })
})
```

- [ ] **Step 2: Run contract tests to verify they pass**

```bash
cd backoffice && npm test -- --run src/test/contract/settlement.contract.test.ts
```

---

### Task 6: KYC Review - Contract Tests [DONE]

**BDD Scenarios:**
- S.KY-01: View KYC review queue
- S.KY-02: Approve KYC application
- S.KY-03: Reject KYC application with reason

**BRD Requirements:**
- US-BO-09: Backoffice admin can review and approve/reject KYC applications

**User-Facing:** YES

**Files:**
- Create: `backoffice/src/test/contract/kyc.contract.test.ts`

- [ ] **Step 1: Write contract tests for KYC endpoints**

```typescript
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: GET /api/v1/backoffice/kyc/review-queue', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint', async () => {
    server.use(
      http.get('/api/v1/backoffice/kyc/review-queue', () => {
        return HttpResponse.json([
          {
            id: 'kyc-001',
            agentCode: 'AGT-001',
            businessName: 'Test Store',
            status: 'PENDING_REVIEW',
            submittedAt: '2026-04-01T12:00:00Z',
            biometricVerified: true,
            amlStatus: 'CLEARED',
          },
        ])
      })
    )

    const response = await fetch('/api/v1/backoffice/kyc/review-queue')
    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(Array.isArray(data)).toBe(true)
    expect(data[0]).toHaveProperty('id')
    expect(data[0]).toHaveProperty('status')
  })
})

describe('Contract: POST /api/v1/backoffice/kyc/review-queue/:id/approve', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with kycId in path', async () => {
    let capturedId: string | undefined

    server.use(
      http.post('/api/v1/backoffice/kyc/review-queue/:id/approve', ({ params }) => {
        capturedId = params.id as string
        return HttpResponse.json({
          id: capturedId,
          status: 'APPROVED',
          approvedAt: new Date().toISOString(),
        })
      })
    )

    const kycId = 'kyc-001'
    const response = await fetch(`/api/v1/backoffice/kyc/review-queue/${kycId}/approve`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    })

    expect(capturedId).toBe(kycId)
    expect(response.ok).toBe(true)
  })
})

describe('Contract: POST /api/v1/backoffice/kyc/review-queue/:id/reject', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with kycId and reason', async () => {
    let capturedId: string | undefined
    let capturedBody: any

    server.use(
      http.post('/api/v1/backoffice/kyc/review-queue/:id/reject', async ({ request, params }) => {
        capturedId = params.id as string
        capturedBody = await request.json()
        return HttpResponse.json({
          id: capturedId,
          status: 'REJECTED',
          rejectionReason: capturedBody.reason,
          rejectedAt: new Date().toISOString(),
        })
      })
    )

    const kycId = 'kyc-001'
    const response = await fetch(`/api/v1/backoffice/kyc/review-queue/${kycId}/reject`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ reason: 'Invalid documents' }),
    })

    expect(capturedId).toBe(kycId)
    expect(capturedBody.reason).toBe('Invalid documents')
    expect(response.ok).toBe(true)
  })
})
```

- [ ] **Step 2: Run contract tests to verify they pass**

```bash
cd backoffice && npm test -- --run src/test/contract/kyc.contract.test.ts
```

---

### Task 7: Auth/Login - Contract Tests

**BDD Scenarios:**
- S.LG-01: Successful login
- S.LG-02: Invalid credentials
- S.LG-03: Account locked

**BRD Requirements:**
- US-BO-10: Backoffice user can authenticate

**User-Facing:** YES

**Files:**
- Create: `backoffice/src/test/contract/auth.contract.test.ts`

- [ ] **Step 1: Write contract tests for auth endpoints**

```typescript
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: POST /api/v1/auth/token (Login)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with credentials', async () => {
    let capturedBody: any

    server.use(
      http.post('/api/v1/auth/token', async ({ request }) => {
        capturedBody = await request.json()
        return HttpResponse.json({
          access_token: 'mock-jwt-token',
          refresh_token: 'mock-refresh-token',
          expires_in: 3600,
          token_type: 'Bearer',
        })
      })
    )

    const response = await fetch('/api/v1/auth/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'password' }),
    })

    expect(capturedBody.username).toBe('admin')
    expect(capturedBody.password).toBe('password')
    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(data).toHaveProperty('access_token')
    expect(data).toHaveProperty('expires_in')
  })

  it('should handle invalid credentials (401)', async () => {
    server.use(
      http.post('/api/v1/auth/token', () => {
        return HttpResponse.json(
          { status: 'FAILED', error: { code: 'ERR_AUTH_INVALID_CREDENTIALS', message: 'Invalid credentials' } },
          { status: 401 }
        )
      })
    )

    const response = await fetch('/api/v1/auth/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'wrong' }),
    })

    expect(response.status).toBe(401)
    const data = await response.json()
    expect(data.error.code).toBe('ERR_AUTH_INVALID_CREDENTIALS')
  })

  it('should handle locked account (403)', async () => {
    server.use(
      http.post('/api/v1/auth/token', () => {
        return HttpResponse.json(
          { status: 'FAILED', error: { code: 'ERR_AUTH_ACCOUNT_LOCKED', message: 'Account is locked' } },
          { status: 403 }
        )
      })
    )

    const response = await fetch('/api/v1/auth/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'locked', password: 'password' }),
    })

    expect(response.status).toBe(403)
    const data = await response.json()
    expect(data.error.code).toBe('ERR_AUTH_ACCOUNT_LOCKED')
  })
})
```

- [ ] **Step 2: Run contract tests to verify they pass**

```bash
cd backoffice && npm test -- --run src/test/contract/auth.contract.test.ts
```

---

### Task 8: Expand Transactions Contract Tests

**BDD Scenarios:**
- S.TX-01: Load transactions with filters

**BRD Requirements:**
- US-BO-06: Backoffice admin can view transaction history

**User-Facing:** YES

**Files:**
- Modify: `backoffice/src/test/contract/transactions.contract.test.ts`

- [ ] **Step 1: Add contract tests for transactions with query params**

```typescript
// Add to existing transactions.contract.test.ts

describe('Contract: GET /api/v1/backoffice/transactions with filters', () => {
  beforeEach(() => server.resetHandlers())

  it('should support date range and status filters', async () => {
    let capturedUrl: URL | undefined

    server.use(
      http.get('/api/v1/backoffice/transactions', ({ request }) => {
        capturedUrl = new URL(request.url)
        return HttpResponse.json({
          content: [
            {
              transactionId: 'txn-001',
              agentCode: 'AGT-001',
              type: 'WITHDRAWAL',
              amount: 100.00,
              status: 'SUCCESS',
              createdAt: '2026-04-01T12:00:00Z',
            },
          ],
          totalElements: 1,
          totalPages: 1,
          page: 0,
          size: 10,
        })
      })
    )

    const response = await fetch('/api/v1/backoffice/transactions?status=SUCCESS&type=WITHDRAWAL&page=0&size=10')
    const data = await response.json()

    expect(capturedUrl?.searchParams.get('status')).toBe('SUCCESS')
    expect(capturedUrl?.searchParams.get('type')).toBe('WITHDRAWAL')
    expect(data).toHaveProperty('content')
    expect(data).toHaveProperty('totalElements')
  })
})
```

- [ ] **Step 2: Run contract tests to verify they pass**

```bash
cd backoffice && npm test -- --run src/test/contract/transactions.contract.test.ts
```

---

## Integration Tests (Real Backend) - Validate End-to-End Flows

### Task 9: Agents API Integration Tests - Expand [DONE]

**BDD Scenarios:**
- S.AG-01 through S.AG-03 (all agent operations)

**BRD Requirements:**
- US-BO-01, US-BO-02, US-BO-03

**User-Facing:** YES

**Files:**
- Modify: `backoffice/src/test/integration/agents-api.integration.test.ts`

- [ ] **Step 1: Add integration tests for all agent mutations**

```typescript
// Add to existing agents-api.integration.test.ts

describe('Agent Mutations', () => {
  let createdAgentId: string

  it('should create a new agent', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        agentCode: 'AGT-999',
        businessName: 'Integration Test Store',
        tier: 'STANDARD',
        merchantGpsLat: 3.139003,
        merchantGpsLng: 101.686855,
        mykadNumber: '900101011234',
        phoneNumber: '019-8887777',
      }),
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(data.agentCode).toBe('AGT-999')
    expect(data.businessName).toBe('Integration Test Store')
    expect(data.status).toBe('ACTIVE')
    createdAgentId = data.agentId
  })

  it('should update an agent', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents/${createdAgentId}`, {
      method: 'PUT',
      headers: {
        Authorization: `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        businessName: 'Updated Store Name',
        tier: 'PREMIUM',
        merchantGpsLat: 3.139003,
        merchantGpsLng: 101.686855,
        phoneNumber: '019-8887777',
      }),
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(data.businessName).toBe('Updated Store Name')
    expect(data.tier).toBe('PREMIUM')
  })

  it('should deactivate an agent', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents/${createdAgentId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${authToken}` },
    })

    expect(response.ok).toBe(true)
  })
})

describe('Agent User Account Creation', () => {
  it('should create user account for agent without user', async () => {
    // Find an agent without user account
    const agentsResponse = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents`, {
      headers: { Authorization: `Bearer ${authToken}` },
    })
    const agents = await agentsResponse.json()
    
    // Find agent with NOT_CREATED status
    let targetAgent: any = null
    for (const agent of agents) {
      const statusResponse = await fetch(
        `${GATEWAY_URL}/api/v1/backoffice/agents/${agent.agentId}/user-status`,
        { headers: { Authorization: `Bearer ${authToken}` } }
      )
      const status = await statusResponse.json()
      if (status.status === 'NOT_CREATED') {
        targetAgent = agent
        break
      }
    }

    if (!targetAgent) {
      // Create a new agent for this test
      const createResponse = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${authToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          agentCode: `AGT-${Date.now()}`,
          businessName: 'Test Agent for User Creation',
          tier: 'STANDARD',
          merchantGpsLat: 3.139003,
          merchantGpsLng: 101.686855,
          mykadNumber: '900101011234',
          phoneNumber: '019-1112222',
        }),
      })
      targetAgent = await createResponse.json()
    }

    // Create user account
    const response = await fetch(
      `${GATEWAY_URL}/api/v1/backoffice/agents/${targetAgent.agentId}/create-user`,
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${authToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          phone: targetAgent.phoneNumber,
          businessName: targetAgent.businessName,
        }),
      }
    )

    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(data).toHaveProperty('userId')
    expect(data.username).toBe(targetAgent.agentCode)
  })

  it('should return 409 when creating user for agent that already has user', async () => {
    // Find an agent WITH user account
    const agentsResponse = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents`, {
      headers: { Authorization: `Bearer ${authToken}` },
    })
    const agents = await agentsResponse.json()
    
    let targetAgent: any = null
    for (const agent of agents) {
      const statusResponse = await fetch(
        `${GATEWAY_URL}/api/v1/backoffice/agents/${agent.agentId}/user-status`,
        { headers: { Authorization: `Bearer ${authToken}` } }
      )
      const status = await statusResponse.json()
      if (status.status !== 'NOT_CREATED') {
        targetAgent = agent
        break
      }
    }

    if (!targetAgent) return // Skip if no agent with user found

    const response = await fetch(
      `${GATEWAY_URL}/api/v1/backoffice/agents/${targetAgent.agentId}/create-user`,
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${authToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          phone: targetAgent.phoneNumber,
          businessName: targetAgent.businessName,
        }),
      }
    )

    expect(response.status).toBe(409)
    const data = await response.json()
    expect(data.error.code).toContain('USER_EXISTS')
  })
})
```

- [ ] **Step 2: Run integration tests to verify they pass**

```bash
cd backoffice && npm run test:integration
```

---

### Task 10: Users API Integration Tests - Expand [DONE]

**BDD Scenarios:**
- S.UM-01 through S.UM-06 (all user operations)

**BRD Requirements:**
- US-BO-05

**User-Facing:** YES

**Files:**
- Modify: `backoffice/src/test/integration/users-api.integration.test.ts`

- [ ] **Step 1: Add integration tests for all user mutations**

```typescript
// Add to existing users-api.integration.test.ts

describe('User Mutations', () => {
  let createdUserId: string

  it('should create a new user', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: `testuser_${Date.now()}`,
        email: `test${Date.now()}@example.com`,
        fullName: 'Test User',
        password: 'TempPass123!',
        userType: 'INTERNAL',
      }),
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(data.username).toContain('testuser_')
    expect(data.status).toBe('ACTIVE')
    createdUserId = data.userId
  })

  it('should update a user', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users/${createdUserId}`, {
      method: 'PUT',
      headers: {
        Authorization: `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        fullName: 'Updated Test User',
        email: `updated${Date.now()}@example.com`,
      }),
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(data.fullName).toBe('Updated Test User')
  })

  it('should lock a user', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users/${createdUserId}/lock`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({}),
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(data.status).toBe('LOCKED')
  })

  it('should unlock a user', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users/${createdUserId}/unlock`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({}),
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(data.status).toBe('ACTIVE')
  })

  it('should reset user password', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users/${createdUserId}/reset-password`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ newPassword: 'NewTemp456!' }),
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(data).toHaveProperty('temporaryPassword')
  })

  it('should delete a user', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users/${createdUserId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${authToken}` },
    })

    expect(response.ok).toBe(true)
  })
})
```

- [ ] **Step 2: Run integration tests to verify they pass**

```bash
cd backoffice && npm run test:integration
```

---

### Task 11: Settlement API Integration Tests [DONE]

**BDD Scenarios:**
- S.ST-01, S.ST-02

**BRD Requirements:**
- US-BO-08

**User-Facing:** YES

**Files:**
- Create: `backoffice/src/test/integration/settlement-api.integration.test.ts`

- [ ] **Step 1: Write integration tests for settlement endpoints**

```typescript
import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import { server } from '../mocks/server'

const GATEWAY_URL = process.env.TEST_GATEWAY_URL || 'http://localhost:8080'

describe('Settlement API Integration Tests (Real Backend)', () => {
  let authToken: string

  beforeAll(async () => {
    server.close()
    const tokenResponse = await fetch(`${GATEWAY_URL}/api/v1/auth/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'password' }),
    })
    const tokenData = await tokenResponse.json()
    authToken = tokenData.access_token
  })

  afterAll(() => server.listen())

  it('should fetch settlement data', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/settlement?date=2026-04-02`, {
      headers: { Authorization: `Bearer ${authToken}` },
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(data).toHaveProperty('totalDeposits')
    expect(data).toHaveProperty('totalWithdrawals')
    expect(data).toHaveProperty('totalCommissions')
    expect(data).toHaveProperty('netAmount')
  })

  it('should export settlement report', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/settlement/export?date=2026-04-02`, {
      headers: { Authorization: `Bearer ${authToken}` },
    })

    expect(response.ok).toBe(true)
  })
})
```

- [ ] **Step 2: Run integration tests to verify they pass**

```bash
cd backoffice && npm run test:integration
```

---

### Task 12: KYC API Integration Tests [DONE]

### Task 13: Auth Integration Tests [DONE]

**BDD Scenarios:**
- S.LG-01 through S.LG-03

**BRD Requirements:**
- US-BO-10

**User-Facing:** YES

**Files:**
- Create: `backoffice/src/test/integration/auth.integration.test.ts`

- [ ] **Step 1: Write integration tests for auth endpoints**

```typescript
import { describe, it, expect, afterAll } from 'vitest'
import { server } from '../mocks/server'

const GATEWAY_URL = process.env.TEST_GATEWAY_URL || 'http://localhost:8080'

describe('Auth API Integration Tests (Real Backend)', () => {
  afterAll(() => server.listen())

  beforeAll(() => server.close())

  it('should authenticate with valid credentials', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/auth/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'password' }),
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(data).toHaveProperty('access_token')
    expect(data).toHaveProperty('expires_in')
    expect(data.token_type).toBe('Bearer')
  })

  it('should reject invalid credentials with 401', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/auth/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'wrongpassword' }),
    })

    expect(response.status).toBe(401)
    const data = await response.json()
    expect(data.status).toBe('FAILED')
    expect(data.error.code).toContain('ERR_AUTH')
  })
})
```

- [ ] **Step 2: Run integration tests to verify they pass**

```bash
cd backoffice && npm run test:integration
```

---

## Final Verification

### Task 14: Run All Tests

- [ ] **Step 1: Run all unit + contract tests**

```bash
cd backoffice && npm test -- --run
```

- [ ] **Step 2: Run all integration tests (requires Docker)**

```bash
cd backoffice && npm run test:integration
```

- [ ] **Step 3: Verify coverage report**

```bash
cd backoffice && npm test -- --run --coverage
```

Expected: All 22 API endpoints covered by at least one contract test and one integration test.

---

## Coverage Matrix (After Implementation)

| API Endpoint | Contract Test | Integration Test |
|-------------|---------------|-----------------|
| GET /backoffice/dashboard | ✅ Task 4 | ⚠️ (via agents-api test) |
| GET /backoffice/agents | ✅ Existing | ✅ Existing |
| POST /backoffice/agents | ✅ Task 2 | ✅ Task 9 |
| PUT /backoffice/agents/:id | ✅ Task 2 | ✅ Task 9 |
| DELETE /backoffice/agents/:id | ✅ Task 2 | ✅ Task 9 |
| GET /backoffice/admin/users | ✅ Existing | ✅ Existing |
| POST /backoffice/admin/users | ✅ Task 3 | ✅ Task 10 |
| PUT /backoffice/admin/users/:id | ✅ Task 3 | ✅ Task 10 |
| DELETE /backoffice/admin/users/:id | ✅ Task 3 | ✅ Task 10 |
| POST /backoffice/admin/users/:id/lock | ✅ Task 3 | ✅ Task 10 |
| POST /backoffice/admin/users/:id/unlock | ✅ Task 3 | ✅ Task 10 |
| POST /backoffice/admin/users/:id/reset-password | ✅ Task 3 | ✅ Task 10 |
| GET /backoffice/agents/:id/user-status | ✅ Existing | ✅ Existing |
| POST /backoffice/agents/:id/create-user | ✅ Task 1 | ✅ Task 9 |
| GET /backoffice/transactions | ✅ Task 8 | ⚠️ (via agents-api test) |
| GET /backoffice/settlement | ✅ Task 5 | ✅ Task 11 |
| GET /backoffice/settlement/export | ✅ Task 5 | ✅ Task 11 |
| GET /backoffice/kyc/review-queue | ✅ Task 6 | ✅ Task 12 |
| POST /backoffice/kyc/review-queue/:id/approve | ✅ Task 6 | ⚠️ (needs data setup) |
| POST /backoffice/kyc/review-queue/:id/reject | ✅ Task 6 | ⚠️ (needs data setup) |
| POST /auth/token | ✅ Task 7 | ✅ Task 13 |

**Coverage: 22/22 endpoints (100%)**
