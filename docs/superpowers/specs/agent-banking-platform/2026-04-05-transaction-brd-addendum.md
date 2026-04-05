# Business Requirements Document — Addendum: Transaction Orchestrator (Temporal)
## Agent Banking Platform (Malaysia)

**Version:** 1.0
**Date:** 2026-04-05
**Status:** Draft — Pending Review
**Supplements:** `2026-03-25-agent-banking-platform-brd.md` (original BRD)
**Design Reference:** `2026-04-05-transaction-orchestrator-temporal-design.md`
**BDD Reference:** `2026-04-05-transaction-bdd-addendum.md`

---

## Scope

This addendum covers **only** the Transaction Orchestrator domain: workflow routing, Temporal durable execution, compensation, safety reversal, human-in-the-loop, polling, and all transaction type workflows (Withdrawal, Deposit, Bill Payment, DuitNow Transfer).

**Unchanged content** (Rules Service, Ledger & Float, e-KYC, Backoffice, etc.) remains in the original BRD at `2026-03-25-agent-banking-platform-brd.md`.

---

## 1. User Stories — Transaction Orchestrator

### 1.1 New User Stories

| ID | User Story | STP Category | Phase |
|----|-----------|-------------|-------|
| US-TO01 | As the system, I want to route each incoming transaction to the correct Temporal workflow based on transaction type and target leg (On-Us, Off-Us, VAS) | 100% STP | MVP |
| US-TO02 | As the system, I want to persist workflow state durably so that JVM crashes, restarts, and network partitions do not lose in-flight transactions | 100% STP | MVP |
| US-TO03 | As the system, I want to execute compensations in reverse order when a workflow step fails, ensuring no zombie locks remain on agent float | 100% STP | MVP |
| US-TO04 | As the system, I want to trigger a Safety Reversal (MTI 0400) with infinite retry when switch communication times out after float was blocked (Store & Forward) | 100% STP | MVP |
| US-TO05 | As a bank operator (Admin), I want to force-resolve stuck transactions via the backoffice UI when a workflow is in COMPENSATING state for too long | Non-STP | Phase 2 |
| US-TO06 | As a POS terminal, I want to poll for transaction status until the workflow completes, so I can display the final result to the agent and customer | 100% STP | MVP |
| US-TO07 | As the system, I want to reject duplicate transaction starts using the idempotency key as the Temporal workflow ID, preventing double-processing | 100% STP | MVP |
| US-TO08 | As the system, I want to expose workflow execution history through Temporal UI for audit and debugging purposes | N/A | MVP |

### 1.2 Revised User Stories (Updated for Temporal Execution Model)

| ID | Original User Story | Change |
|----|--------------------|--------|
| US-L05 | As a customer, I want to withdraw cash using my ATM card (EMV + PIN) at an agent location | **Updated:** Execution now handled by Temporal WithdrawalWorkflow (Off-Us) or WithdrawalOnUsWorkflow (On-Us) with durable state, automatic compensation, and safety reversal. See US-TO02, US-TO03, US-TO04. |
| US-L06 | As a customer, I want to withdraw cash using MyKad at an agent location | **Updated:** Execution now handled by Temporal WithdrawalOnUsWorkflow with CBS integration. See US-TO02, US-TO03. |
| US-L07 | As a customer, I want to deposit cash at an agent location with account validation before funds move | **Updated:** Execution now handled by Temporal DepositWorkflow with conditional biometric verification and CBS posting. See US-TO02, US-TO03. |
| US-D01 | As a customer, I want to transfer funds via DuitNow (mobile, MyKad, BRN proxies) with real-time settlement | **Updated:** Execution now handled by Temporal DuitNowTransferWorkflow with proxy enquiry, ISO 20022 routing, and safety reversal. Must complete in < 15 seconds (happy path). See US-TO02, US-TO03, US-TO04. |
| US-B01 | As a customer, I want to pay utility bills (JomPAY) via cash or card with automatic routing to biller | **Updated:** Execution now handled by Temporal BillPaymentWorkflow with bill validation, payment, and compensation including biller notification. See US-TO02, US-TO03. |
| US-V01 | As the system, I want to trigger an MTI 0400 reversal if a network timeout occurs after float was blocked (Store & Forward) | **Updated:** Now implemented as Temporal Safety Reversal Activity with infinite retry (60s interval). Workflow persists reversal state durably. See US-TO04. |

---

## 2. Functional Requirements — Transaction Orchestrator

### FR-19: Transaction Orchestrator (Temporal)

| ID | Requirement | US |
|----|------------|-----|
| FR-19.1 | System shall route each transaction to the appropriate Temporal workflow based on transaction type and target leg (On-Us, Off-Us, VAS) using a WorkflowRouter component | US-TO01 |
| FR-19.2 | System shall persist workflow state durably using Temporal so that JVM crashes, restarts, and network partitions do not lose in-flight transactions | US-TO02 |
| FR-19.3 | System shall execute compensations in reverse order when a workflow step fails, ensuring no zombie locks remain on agent float | US-TO03 |
| FR-19.4 | System shall trigger Safety Reversal (MTI 0400) with infinite retry (60s interval, Store & Forward) when switch communication times out after float was blocked | US-TO04, US-V01 |
| FR-19.5 | System shall support human-in-the-loop signals allowing authorized admins to force-resolve (COMMIT or REVERSE) stuck workflows in COMPENSATING state | US-TO05 |
| FR-19.6 | System shall expose a polling endpoint (`GET /api/v1/transactions/{workflowId}/status`) for transaction status queries, returning workflow state and result | US-TO06 |
| FR-19.7 | System shall reject duplicate transaction starts using `idempotencyKey` as the Temporal workflow ID with `WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE` | US-TO07 |
| FR-19.8 | System shall expose workflow execution history through Temporal UI (port 8081) for audit and debugging purposes | US-TO08 |
| FR-19.9 | System shall complete DuitNow transfers in under 15 seconds end-to-end (happy path) | US-D01 |
| FR-19.10 | System shall use typed DTOs (Java Records) for all inter-service communication — no `Map<String, Object>` payloads | US-TO01 |
| FR-19.11 | System shall enforce the Global Error Schema for all error responses, mapping external legacy codes (ISO 8583, ISO 20022, CBS) to business error codes | US-L05, US-D01 |
| FR-19.12 | System shall log all compensation actions, safety reversals, and human-in-the-loop resolutions with admin ID, action, reason, and timestamp for audit | US-TO03, US-TO04, US-TO05 |

### FR-20: Withdrawal Workflow (Off-Us)

| ID | Requirement | US |
|----|------------|-----|
| FR-20.1 | System shall process Off-Us cash withdrawals via Temporal WithdrawalWorkflow with the following steps: CheckVelocity → CalculateFees → BlockFloat → AuthorizeAtSwitch → CommitFloat → PublishKafkaEvent | US-L05 |
| FR-20.2 | System shall trigger ReleaseFloat compensation when switch returns DECLINED after float was blocked | US-L05, US-TO03 |
| FR-20.3 | System shall trigger Safety Reversal (MTI 0400) + ReleaseFloat when switch communication times out (25s timeout) | US-L05, US-V01, US-TO04 |
| FR-20.4 | System shall trigger Safety Reversal + ReleaseFloat when CommitFloat fails after switch approval | US-L05, US-TO03 |
| FR-20.5 | System shall return the original cached response when a duplicate withdrawal request arrives with the same idempotency key | US-L05, US-TO07 |

### FR-21: Withdrawal Workflow (On-Us)

| ID | Requirement | US |
|----|------------|-----|
| FR-21.1 | System shall process On-Us cash withdrawals via Temporal WithdrawalOnUsWorkflow with direct CBS authorization (no PayNet/ISO 8583) | US-L06 |
| FR-21.2 | System shall trigger ReleaseFloat compensation when CBS authorization fails after float was blocked | US-L06, US-TO03 |
| FR-21.3 | System shall not trigger Safety Reversal for On-Us withdrawals (CBS returns definitive success/failure) | US-L06 |

### FR-22: Deposit Workflow

| ID | Requirement | US |
|----|------------|-----|
| FR-22.1 | System shall process cash deposits via Temporal DepositWorkflow with the following steps: CheckVelocity → CalculateFees → ValidateAccount → [Conditional: VerifyBiometric] → CreditAgentFloat → PostToCBS → PublishKafkaEvent | US-L07 |
| FR-22.2 | System shall require biometric verification for deposits above the high-value threshold (configurable, default RM 5,000) | US-L07 |
| FR-22.3 | System shall trigger ReverseCreditFloat compensation when PostToCBS fails after agent float was credited | US-L07, US-TO03 |
| FR-22.4 | System shall return FAILED immediately when account validation fails (no compensation needed — no money moved) | US-L07 |

### FR-23: Bill Payment Workflow

| ID | Requirement | US |
|----|------------|-----|
| FR-23.1 | System shall process bill payments via Temporal BillPaymentWorkflow with the following steps: CheckVelocity → CalculateFees → BlockFloat → ValidateBill → PayBiller → CommitFloat → NotifyBiller → PublishKafkaEvent | US-B01 |
| FR-23.2 | System shall trigger ReleaseFloat compensation when bill validation fails after float was blocked | US-B01, US-TO03 |
| FR-23.3 | System shall trigger ReleaseFloat + NotifyBillerReversal when PayBiller fails after float was blocked | US-B01, US-TO03 |
| FR-23.4 | System shall continue workflow even if NotifyBiller fails (non-critical — biller already paid) | US-B01 |

### FR-24: DuitNow Transfer Workflow

| ID | Requirement | US |
|----|------------|-----|
| FR-24.1 | System shall process DuitNow transfers via Temporal DuitNowTransferWorkflow with the following steps: CheckVelocity → CalculateFees → BlockFloat → ProxyEnquiry → SendDuitNowTransfer → CommitFloat → PublishKafkaEvent | US-D01 |
| FR-24.2 | System shall support DuitNow proxy types: Mobile Number, MyKad Number, and BRN | US-D01 |
| FR-24.3 | System shall trigger ReleaseFloat compensation when proxy enquiry fails after float was blocked | US-D01, US-TO03 |
| FR-24.4 | System shall trigger Safety Reversal (MTI 0400) + ReleaseFloat when DuitNow transfer times out (25s timeout) | US-D01, US-V01, US-TO04 |
| FR-24.5 | System shall complete DuitNow transfers in under 15 seconds end-to-end (happy path) | US-D01 |

---

## 3. Processing Requirements — Step-by-Step

### 3.1 Withdrawal (Off-Us) — Detailed Processing

| Step | Action | Service | Success Path | Failure Path |
|------|--------|---------|-------------|--------------|
| 1 | Validate idempotency key (Redis cache) | Orchestrator | Return cached response if found | Continue to step 2 |
| 2 | Start WithdrawalWorkflow (Temporal) | Orchestrator | Returns workflowId immediately | WorkflowExecutionAlreadyStarted → return existing status |
| 3 | Check velocity limits | Rules Service | Pass → continue | Return FAILED, no compensation |
| 4 | Calculate fees | Rules Service | Return fee breakdown | Return FAILED, no compensation |
| 5 | Block float (PESSIMISTIC_WRITE) | Ledger Service | Float reserved → continue | Return FAILED, no compensation |
| 6 | Authorize at switch (ISO 8583) | Switch Adapter | APPROVED → step 7 | DECLINED → ReleaseFloat, return FAILED |
| 6a | Switch timeout (25s) | Switch Adapter | — | Safety Reversal (MTI 0400) + ReleaseFloat, return FAILED |
| 7 | Commit float | Ledger Service | Float committed → step 8 | Safety Reversal + ReleaseFloat, return FAILED |
| 8 | Publish Kafka event | Kafka | Event published → step 9 | Log and continue (non-critical) |
| 9 | Cache response in Redis | Orchestrator | Response cached | Log and continue |
| 10 | Return COMPLETED | Orchestrator | WorkflowResult(COMPLETED) | — |

### 3.2 Deposit — Detailed Processing

| Step | Action | Service | Success Path | Failure Path |
|------|--------|---------|-------------|--------------|
| 1 | Validate idempotency key | Orchestrator | Return cached if found | Continue |
| 2 | Start DepositWorkflow | Orchestrator | Returns workflowId | Duplicate → return existing |
| 3 | Check velocity limits | Rules Service | Pass → continue | Return FAILED |
| 4 | Calculate fees | Rules Service | Return fee breakdown | Return FAILED |
| 5 | Validate destination account | Ledger/CBS | Valid → continue | Return FAILED, no compensation |
| 6 | [Conditional] Verify biometric | Onboarding | MATCH → continue | Return FAILED, no compensation |
| 7 | Credit agent float | Ledger Service | Float credited → step 8 | Return FAILED, no compensation |
| 8 | Post to CBS | CBS Connector | Posted → step 9 | ReverseCreditFloat, return FAILED |
| 9 | Publish Kafka event | Kafka | Event published | Log and continue |
| 10 | Return COMPLETED | Orchestrator | WorkflowResult(COMPLETED) | — |

### 3.3 Bill Payment — Detailed Processing

| Step | Action | Service | Success Path | Failure Path |
|------|--------|---------|-------------|--------------|
| 1 | Validate idempotency key | Orchestrator | Return cached if found | Continue |
| 2 | Start BillPaymentWorkflow | Orchestrator | Returns workflowId | Duplicate → return existing |
| 3 | Check velocity limits | Rules Service | Pass → continue | Return FAILED |
| 4 | Calculate fees | Rules Service | Return fee breakdown | Return FAILED |
| 5 | Block float | Ledger Service | Float reserved → continue | Return FAILED |
| 6 | Validate bill (Ref-1) | Biller Service | Valid → continue | ReleaseFloat, return FAILED |
| 7 | Pay biller | Biller Service | Paid → step 8 | ReleaseFloat + NotifyBillerReversal, return FAILED |
| 8 | Commit float | Ledger Service | Float committed → step 9 | ReleaseFloat + NotifyBillerReversal, return FAILED |
| 9 | Notify biller | Biller Service | Notification sent → step 10 | Log and continue (non-critical) |
| 10 | Publish Kafka event | Kafka | Event published | Log and continue |
| 11 | Return COMPLETED | Orchestrator | WorkflowResult(COMPLETED) | — |

### 3.4 DuitNow Transfer — Detailed Processing

| Step | Action | Service | Success Path | Failure Path |
|------|--------|---------|-------------|--------------|
| 1 | Validate idempotency key | Orchestrator | Return cached if found | Continue |
| 2 | Start DuitNowTransferWorkflow | Orchestrator | Returns workflowId | Duplicate → return existing |
| 3 | Check velocity limits | Rules Service | Pass → continue | Return FAILED |
| 4 | Calculate fees | Rules Service | Return fee breakdown | Return FAILED |
| 5 | Block float | Ledger Service | Float reserved → continue | Return FAILED |
| 6 | Proxy enquiry | Switch Adapter | Valid → step 7 | ReleaseFloat, return FAILED |
| 7 | Send DuitNow transfer (ISO 20022) | Switch Adapter | APPROVED → step 8 | DECLINED → ReleaseFloat, return FAILED |
| 7a | Transfer timeout (25s) | Switch Adapter | — | Safety Reversal + ReleaseFloat, return FAILED |
| 8 | Commit float | Ledger Service | Float committed → step 9 | Safety Reversal + ReleaseFloat, return FAILED |
| 9 | Publish Kafka event | Kafka | Event published | Log and continue |
| 10 | Return COMPLETED | Orchestrator | WorkflowResult(COMPLETED) | — |

---

## 4. Entity Definitions — Transaction Orchestrator

### ENT-13: TransactionRecord

Lightweight query table for backoffice and reporting. Temporal persists the authoritative workflow state.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | UUID | Yes | Primary key |
| workflowId | String(128) | Yes | Temporal workflow ID (= idempotency key), unique |
| transactionType | Enum | Yes | CASH_WITHDRAWAL, CASH_DEPOSIT, BILL_PAYMENT, DUITNOW_TRANSFER |
| agentId | UUID | Yes | References Agent.agentId |
| amount | BigDecimal(15,2) | Yes | Transaction amount |
| customerFee | BigDecimal(10,2) | Conditional | Fee charged to customer |
| status | Enum | Yes | PENDING, COMPLETED, FAILED, REVERSED |
| errorCode | String(50) | Conditional | Error code if status=FAILED |
| errorMessage | Text | Conditional | Human-readable error message |
| externalReference | String(128) | Conditional | PayNet RRN, Biller ref, CBS ref |
| createdAt | Timestamp | Yes | Workflow start timestamp |
| completedAt | Timestamp | Conditional | Workflow completion timestamp |
| updatedAt | Timestamp | Yes | Last status update timestamp |

---

## 5. Traceability Matrix: User Stories → Functional Requirements

### New User Stories

| User Story | Functional Requirements | STP Category | Phase |
|-----------|------------------------|-------------|-------|
| US-TO01 | FR-19.1, FR-19.10 | 100% STP | MVP |
| US-TO02 | FR-19.2 | 100% STP | MVP |
| US-TO03 | FR-19.3, FR-19.12 | 100% STP | MVP |
| US-TO04 | FR-19.4, FR-19.12 | 100% STP | MVP |
| US-TO05 | FR-19.5, FR-19.12 | Non-STP | Phase 2 |
| US-TO06 | FR-19.6 | 100% STP | MVP |
| US-TO07 | FR-19.7 | 100% STP | MVP |
| US-TO08 | FR-19.8 | N/A | MVP |

### Revised User Stories

| User Story | Functional Requirements | STP Category | Phase |
|-----------|------------------------|-------------|-------|
| US-L05 | FR-19.2, FR-19.3, FR-19.4, FR-19.7, FR-19.11, FR-20.1, FR-20.2, FR-20.3, FR-20.4, FR-20.5 | 100% STP | MVP |
| US-L06 | FR-19.2, FR-19.3, FR-21.1, FR-21.2, FR-21.3 | 100% STP | Phase 2 |
| US-L07 | FR-19.2, FR-19.3, FR-22.1, FR-22.2, FR-22.3, FR-22.4 | 100% STP | MVP |
| US-D01 | FR-19.2, FR-19.3, FR-19.4, FR-19.9, FR-19.11, FR-24.1, FR-24.2, FR-24.3, FR-24.4, FR-24.5 | 100% STP | Phase 2 |
| US-B01 | FR-19.2, FR-19.3, FR-23.1, FR-23.2, FR-23.3, FR-23.4 | 100% STP | Phase 2 |
| US-V01 | FR-19.4, FR-19.12, FR-20.3, FR-24.4 | 100% STP | MVP |

---

## 6. Non-Functional Requirements — Transaction Orchestrator

| ID | Requirement | How Met |
|----|------------|---------|
| NFR-TO-1 | Workflow start must return in < 100ms (async initiation) | TemporalClient.startWorkflow() returns immediately |
| NFR-TO-2 | DuitNow transfers must complete in < 15 seconds (happy path) | Workflow timeout 5m, actual path < 15s |
| NFR-TO-3 | System must survive JVM crashes without losing in-flight transactions | Temporal durable execution persists state after each Activity |
| NFR-TO-4 | Safety Reversal must never give up — persists until PayNet acknowledges | SendReversalActivity: infinite retry, 60s interval |
| NFR-TO-5 | All inter-service calls must use typed DTOs (no Map) | Java Records for all inputs/outputs |
| NFR-TO-6 | All error responses must follow Global Error Schema | Typed exception hierarchy mapped to error codes |
| NFR-TO-7 | PIN blocks must never appear in logs | pinBlock field excluded from all logging |
| NFR-TO-8 | PAN must be masked in all responses and logs | customerCardMasked field only (first 6, last 4) |

---

## 7. Constraints

| ID | Constraint |
|----|-----------|
| C-TO-1 | Temporal SDK version locked to 1.25.x |
| C-TO-2 | One Temporal namespace per environment (dev, staging, prod) |
| C-TO-3 | Workflow history retention: 1 year (configurable) |
| C-TO-4 | Financial Activities (BlockFloat, CommitFloat, AuthorizeAtSwitch, PayBiller, SendDuitNow) must have ZERO retries — timeout triggers reversal |
| C-TO-5 | Non-financial Activities (CheckVelocity, CalculateFees) may retry up to 3x with exponential backoff |
| C-TO-6 | Human-in-the-loop force-resolve requires admin authentication and audit logging |
| C-TO-7 | Polling endpoint must support response caching (Redis, TTL 5s) to reduce Temporal query load |

---

## 8. Assumptions

| ID | Assumption |
|----|-----------|
| A-TO-1 | Temporal server runs as Docker container in development (docker-compose) |
| A-TO-2 | Temporal Cloud or self-hosted cluster used in production |
| A-TO-3 | POS terminals poll for status every 500ms (configurable) |
| A-TO-4 | Workflows stuck in COMPENSATING state for > 4 hours require admin intervention |
| A-TO-5 | The existing `TransactionOrchestrator` will be deprecated and removed after migration (Phase 3) |
| A-TO-6 | All downstream services (Rules, Ledger, Switch, Biller) expose typed internal APIs compatible with Temporal Activity calls |
