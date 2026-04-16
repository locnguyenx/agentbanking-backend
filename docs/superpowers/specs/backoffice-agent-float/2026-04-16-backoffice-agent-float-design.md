# Design Specification: Backoffice Agent Float Management

**Version:** 1.0
**Date:** 2026-04-16
**Status:** Approved
**Parent Spec:** `2026-03-25-agent-banking-platform-design.md`

---

## 1. Overview

Add AgentFloat display and management to the Backoffice UI Agents page:
- Table view: show agent float balance
- View Details modal: show full AgentFloat details + this month's transaction summary
- Create AgentFloat: button to provision float for agents without existing float

## 2. UI Changes

### 2.1 Agents Table

**Column Addition:**
| Column | Display |
|--------|---------|
| Float Balance | `RM 1,500.00` or `No Float` if not provisioned |

**Action Dropdown Enhancement:**
Add "Create AgentFloat" action when agent has no float.

### 2.2 View Details Modal

**New Section: Agent Float**

| Field | Display |
|-------|---------|
| Balance | `RM X,XXX.XX` |
| Reserved Balance | `RM X,XXX.XX` |
| Available Balance | `RM X,XXX.XX` (balance - reserved) |
| Currency | `MYR` |
| GPS Location | `lat, lng` |

**New Section: This Month's Transactions**

| Metric | Value |
|--------|-------|
| Transaction Count | `N transactions` |
| Total Volume | `RM X,XXX.XX` |
| Withdrawals | `N transactions / RM X,XXX.XX` |
| Deposits | `N transactions / RM X,XXX.XX` |
| Other Types | `N transactions / RM X,XXX.XX` |

**Create Button:**
"Create AgentFloat" button if float not provisioned.

### 2.3 Create AgentFloat Modal

**Form Fields:**
| Field | Type | Default | Validation |
|-------|------|---------|------------|
| Initial Balance | Currency input | - | Required, positive number |
| Currency | Dropdown | `MYR` | Required |

**Buttons:** Cancel | Create

## 3. Backend API Changes

### 3.1 New Endpoints (Ledger Service)

**GET /internal/backoffice/agents/{agentId}/float**
```json
{
  "exists": true,
  "float": {
    "floatId": "uuid",
    "balance": 1500.00,
    "reservedBalance": 100.00,
    "availableBalance": 1400.00,
    "currency": "MYR",
    "gpsLat": 3.139,
    "gpsLng": 101.687,
    "updatedAt": "2026-04-16T10:00:00+08:00"
  }
}
```

**GET /internal/backoffice/agents/{agentId}/float/transactions?period=month**
```json
{
  "agentId": "uuid",
  "period": "2026-04",
  "totalCount": 45,
  "totalVolume": 12500.00,
  "byType": [
    { "type": "CASH_WITHDRAWAL", "count": 30, "volume": 9000.00 },
    { "type": "CASH_DEPOSIT", "count": 15, "volume": 3500.00 }
  ]
}
```

**POST /internal/backoffice/agents/{agentId}/float**
```json
{
  "initialBalance": 5000.00,
  "currency": "MYR"
}
```
Response: `201 Created` with created float object.

### 3.2 Gateway Route Addition

```yaml
- id: backoffice-agent-float
  uri: ${ledger-service.url:http://ledger-service:8082}
  predicates:
    - Path=/api/v1/backoffice/agents/*/float
  filters:
    - JwtAuth
    - RewritePath=/api/v1/backoffice/agents/(?<agentId>.*)/float, /internal/backoffice/agents/${agentId}/float
```

## 4. Frontend API Client Changes

### 4.1 client.ts Additions

```typescript
// Agent Float
getAgentFloat: (agentId: string) =>
  client.get(`/backoffice/agents/${agentId}/float`).then((r) => r.data),
getAgentFloatTransactions: (agentId: string) =>
  client.get(`/backoffice/agents/${agentId}/float/transactions`).then((r) => r.data),
createAgentFloat: (agentId: string, data: { initialBalance: number; currency: string }) =>
  client.post(`/backoffice/agents/${agentId}/float`, data).then((r) => r.data),
```

## 5. Data Model

### 5.1 AgentFloat Display Record (TypeScript)

```typescript
interface AgentFloatDisplay {
  exists: boolean;
  float?: {
    floatId: string;
    balance: number;
    reservedBalance: number;
    availableBalance: number;
    currency: string;
    gpsLat: number;
    gpsLng: number;
    updatedAt: string;
  };
}

interface AgentFloatTransactionSummary {
  agentId: string;
  period: string;
  totalCount: number;
  totalVolume: number;
  byType: Array<{
    type: string;
    count: number;
    volume: number;
  }>;
}
```

## 6. Implementation Tasks

### Backend (Ledger Service)
- [ ] Add `BackofficeFloatController` with three endpoints
- [ ] Add `GetAgentFloatUseCase` and `GetAgentFloatTransactionsUseCase`
- [ ] Add `CreateAgentFloatUseCase` (wraps `provisionAgentFloat`)
- [ ] Add gateway route in `gateway/src/main/resources/application.yaml`

### Frontend (Backoffice UI)
- [ ] Add API methods to `client.ts`
- [ ] Add `AgentFloatDisplay` and `AgentFloatTransactionSummary` interfaces
- [ ] Add state management for float data (queries)
- [ ] Add "Float Balance" column to Agents table
- [ ] Add "Create AgentFloat" action in table dropdown
- [ ] Add "Create AgentFloat" button in View Details modal
- [ ] Add "Create AgentFloat" modal with form
- [ ] Add AgentFloat section in View Details modal
- [ ] Add Transaction Summary section in View Details modal

### Testing
- [ ] Add unit tests for new use cases
- [ ] Add integration tests for new endpoints
- [ ] Add contract tests for new API endpoints
- [ ] Update Agents page tests

## 7. Error Handling

| Scenario | Behavior |
|----------|----------|
| Float not found | Return `{ exists: false }`, show "No Float" + Create button |
| Create float fails | Show toast error with message |
| Transactions API fails | Show "Unable to load transactions" in summary section |