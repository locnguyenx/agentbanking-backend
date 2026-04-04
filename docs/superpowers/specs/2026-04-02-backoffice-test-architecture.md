# Backoffice Test Architecture

**Date:** 2026-04-02
**Status:** Implemented

---

## Problem Statement

Previous tests mocked the API client with fabricated data that matched the frontend's *expected* interface, not the backend's *actual* response. This created false confidence — tests passed while production broke due to:
- Missing fields in backend responses (`userType`, `agentId`)
- Status enum mismatches (`DISABLED` vs `DELETED`)
- Impossible status values in UI logic (`CREATED`, `PENDING` that backend never returns)

## Solution: Four-Tier Test Architecture

```
                    ┌─────────────────────┐
                   │    E2E Tests (5-10)  │  ← Full browser, real system
                  │    (Playwright)      │     Critical user journeys
                 └───────────────────────┘
               ┌─────────────────────────┐
              │  Integration Tests (20+) │  ← Components + REAL backend via testcontainers
             │  (testcontainers)         │     Catches actual integration bugs
            └───────────────────────────┘
           ┌─────────────────────────────┐
          │    Contract Tests (1/endpoint)│  ← MSW (derive from OpenAPI spec)
         │    (MSW)                     │     Fast, validates schema contracts
        └───────────────────────────────┘
       ┌─────────────────────────────────┐
      │      Unit Tests (current+)       │  ← UI logic with schema-generated mocks
     │  (Schema-generated mocks)        │     Fast, isolated, no infrastructure
    └─────────────────────────────────┘
```

### Tier Breakdown

| Tier | Purpose | Runs Against | When Fails |
|------|---------|--------------|------------|
| **E2E Tests** | Critical user journeys (login, create agent, process transaction) | Full system (Docker Compose + Playwright) | User-facing bugs |
| **Integration Tests** | Full component flows with real backend | **Real backend (testcontainers)** | UI/backend integration bugs |
| **Contract Tests** | Validate frontend's HTTP call behavior against contract | **MSW** (mock HTTP, fast) | Frontend calling wrong endpoint/params |
| **Unit Tests** | UI logic, rendering, state management | Mock data (generated from schemas) | Frontend logic bugs |

### Why This Distribution?

| Tier | Tool | Rationale |
|------|------|-----------|
| **Contract Tests** | MSW | Defines HTTP contract (URL, method, headers, response). Fast to run. Can be derived from OpenAPI spec. No Docker needed. |
| **Integration Tests** | testcontainers | Spins up real backend containers. Tests real HTTP calls with actual services. Catches DB queries, Kafka messages, service-to-service calls that MSW can't simulate. |

If contract tests use real backend, they're slow and duplicate integration tests. If integration tests only use MSW, they don't catch real integration bugs — which is why we have this architecture.

## File Structure

```
backoffice/src/test/
├── setup.ts                          # Test setup (@testing-library/jest-dom, MSW setup)
├── test-utils.tsx                    # Shared utilities (isolated QueryClient, render helpers)
├── mocks/
│   ├── schemas.ts                    # Backend API schemas (Zod) — single source of truth
│   ├── generators.ts                 # Schema → mock data generator
│   ├── handlers.ts                   # MSW handlers — derive from OpenAPI spec
│   ├── users.mock.ts                 # Generated user mock data
│   ├── agents.mock.ts                # Generated agent mock data
│   └── transactions.mock.ts          # Generated transaction mock data
├── contract/
│   ├── users.contract.test.ts        # MSW: validates /backoffice/admin/users HTTP contract
│   ├── agents.contract.test.ts        # MSW: validates /backoffice/agents HTTP contract
│   ├── transactions.contract.test.ts # MSW: validates /backoffice/transactions HTTP contract
│   └── agent-user-status.contract.test.ts
├── integration/                      # 🔜 Not started
│   └── (testcontainers tests)
├── Agents.test.tsx                   # Unit tests (schema-generated mocks)
├── UserManagement.test.tsx           # Unit tests (schema-generated mocks)
├── Transactions.test.tsx             # Unit tests (schema-generated mocks)
└── e2e/                              # 🔜 Not started (Playwright)
```

## Key Principles

1. **Schemas are source of truth** — Mock data is generated from Zod schemas that mirror backend DTOs
2. **Contract tests define HTTP behavior** — MSW validates frontend makes correct HTTP calls
3. **Integration tests verify real behavior** — testcontainers catches actual backend behavior
4. **Isolated test state** — Fresh QueryClient per test, no shared state
5. **Type-safe mocks** — Generated from schemas, TypeScript validates consistency
6. **Fast feedback loop** — Unit + contract tests run in <5s, integration needs Docker

## Running Tests

```bash
# Unit tests + contract tests (fast, no Docker needed)
cd backoffice && npm test -- --run

# Integration tests (requires Docker running)
cd backoffice && npm run test:integration

# E2E tests (requires full Docker Compose)
cd backoffice && npm run test:e2e
```

## Implementation Status

| Tier | Status | Notes |
|------|--------|-------|
| Unit Tests | ✅ Implemented | 60 tests passing, schema-generated mocks |
| Contract Tests | ⚠️ Partial | Current tests validate schema structure, needs MSW handlers |
| Integration Tests | 🔜 Not started | Requires testcontainers setup |
| E2E Tests | 🔜 Not started | Requires Playwright + full Docker Compose |

## Contract Tests Implementation (MSW)

Contract tests validate that the frontend makes correct HTTP calls:

```typescript
// src/test/contract/users.contract.test.ts
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'
import { UserListSchema } from '../mocks/schemas'

describe('Contract: GET /api/v1/backoffice/admin/users', () => {
  it('should return data matching UserListSchema', async () => {
    // Setup MSW to return mock data that matches schema
    server.use(
      http.get('/api/v1/backoffice/admin/users', () => {
        return HttpResponse.json([
          { userId: '...', username: 'admin', status: 'ACTIVE', ... }
        ])
      })
    )

    // Component makes real HTTP call, MSW intercepts
    const response = await fetch('/api/v1/backoffice/admin/users')
    const data = await response.json()
    
    // Validate against schema
    expect(UserListSchema.safeParse(data).success).toBe(true)
  })

  it('should handle auth errors correctly', async () => {
    server.use(
      http.get('/api/v1/backoffice/admin/users', () => {
        return HttpResponse.json(
          { status: 'FAILED', error: { code: 'ERR_AUTH_INVALID_TOKEN', message: '...' } },
          { status: 401 }
        )
      })
    )

    const response = await fetch('/api/v1/backoffice/admin/users')
    expect(response.status).toBe(401)
  })
})
```

### MSW Server Setup

```typescript
// src/test/mocks/server.ts
import { setupServer } from 'msw/node'
import { handlers } from './handlers'

export const server = setupServer(...handlers)
```

```typescript
// src/test/setup.ts
import '@testing-library/jest-dom'
import { server } from './mocks/server'

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
```

## Integration Tests Implementation (testcontainers)

Integration tests spin up real backend containers to test full component flows:

```typescript
// src/test/integration/agents.integration.test.tsx
import { describe, it, expect, beforeAll } from 'vitest'
import { startTestContainers, stopTestContainers, getGatewayUrl } from './containers'

describe('Agents Integration Tests', () => {
  let gatewayUrl: string

  beforeAll(async () => {
    const { url } = await startTestContainers()
    gatewayUrl = url
  })

  afterAll(() => stopTestContainers())

  it('should load and display agents from real backend', async () => {
    // Set API base URL to testcontainers instance
    process.env.VITE_API_BASE_URL = gatewayUrl

    render(<Agents />)
    
    // Wait for real HTTP call to actual backend
    await waitFor(() => {
      expect(screen.getByText('Agent Management')).toBeInTheDocument()
    })
    
    // Real data from real backend
    const rows = await screen.findAllByRole('row')
    expect(rows.length).toBeGreaterThan(1)
  })
})
```

This catches real integration issues: DB queries, Kafka messages, service-to-service calls that MSW mocks can't simulate.

## Schema-Generated Mocks

Instead of manually crafting mock data that drifts from reality:

```typescript
// BEFORE (manual, drifts):
const MOCK_USERS = [
  { userId: '...', userType: 'INTERNAL', status: 'DISABLED' } // userType doesn't exist!
]

// AFTER (schema-generated, always accurate):
const MOCK_USERS = generateMocks(UserSchema, {
  count: 7,
  variations: { status: ['ACTIVE', 'LOCKED', 'DELETED'] }
})
```

## CI Pipeline Recommendation

```yaml
# .github/workflows/test.yml
jobs:
  unit-and-contract:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: cd backoffice && npm install && npm test -- --run
        # Runs unit + contract tests in <5min, no Docker needed

  integration:
    runs-on: ubuntu-latest
    services:
      docker:
        image: docker:24-cli
        options: --privileged
    steps:
      - uses: actions/checkout@v4
      - name: Start testcontainers
        run: cd backoffice && npm run test:integration
        # Spins up backend containers, runs integration tests
```

This gives:
- **Fast feedback** (<5min) for every PR (unit + contract)
- **Full validation** for main/master branches (integration)