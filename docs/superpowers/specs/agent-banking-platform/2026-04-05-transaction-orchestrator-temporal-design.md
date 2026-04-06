# Technical Design Specification — Transaction Orchestrator (Temporal)
## Agent Banking Platform (Malaysia)

**Version:** 2.1
**Date:** 2026-04-05
**Status:** Draft — Pending Review
**BRD Reference:** `2026-04-05-transaction-brd-addendum.md`
**BDD Reference:** `2026-04-05-transaction-bdd-addendum.md`
**Original BRD:** `2026-03-25-agent-banking-platform-brd.md` (unchanged sections)
**Original BDD:** `2026-03-25-agent-banking-platform-bdd.md` (unchanged sections)
**Supersedes:** Section 12 of `2026-03-25-agent-banking-platform-design.md`

---

## 1. Problem Statement

The current Transaction Orchestrator implementation has critical defects that make it unsuitable for production banking:

1. **No durable execution state** — A JVM crash between `blockFloat()` and `commitFloat()` leaves zombie locks with no recovery path
2. **Incomplete compensation** — `catch (Exception e)` does NOT trigger rollback; float is permanently locked on unexpected exceptions
3. **No `@Transactional` boundaries** on financial operations
4. **Generic `Map<String, Object>` payloads** — zero compile-time type safety across all inter-service calls
5. **Single transaction type** — only withdrawal exists; deposit, bill payment, and DuitNow transfer are absent
6. **No idempotency key validation** on request DTOs
7. **No global error schema compliance**
8. **No Temporal workflow** — the architecture specifies Temporal for durable SAGA execution, but the implementation is a single synchronous method

This document specifies the complete redesign using **Temporal** as the durable execution engine, covering all four MVP+Phase2 transaction types: Withdrawal, Deposit, Bill Payment, and DuitNow Transfer.

---

## 2. Architecture Overview

### 2.1 Temporal as the Execution Engine

Temporal replaces the imperative `TransactionOrchestrator.executeSaga()` with **durable workflows** that:
- Persist state automatically after every Activity
- Survive JVM crashes, restarts, and network partitions
- Execute compensations in reverse order on failure
- Support human-in-the-loop signals for stuck transactions
- Provide built-in retry policies per Activity

### 2.2 System Context

```
┌─────────────────────────────────────────────────────────┐
│  POS Terminal                                            │
│  POST /api/v1/transactions                               │
│  GET  /api/v1/transactions/{workflowId}/status (poll)   │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Spring Cloud Gateway                                    │
│  - JWT validation, rate limiting, routing               │
│  - Idempotency key extraction                           │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Transaction Orchestrator Service                        │
│  ┌───────────────────────────────────────────────────┐  │
│  │ OrchestratorController (thin)                     │  │
│  │   ↓                                               │  │
│  │ WorkflowRouter (determine workflow type)          │  │
│  │   ↓                                               │  │
│  │ TemporalClient.startWorkflow()                    │  │
│  │   ↓                                               │  │
│  │ Returns WorkflowExecutionID immediately           │  │
│  └───────────────────────────────────────────────────┘  │
│                                                          │
│  Temporal Worker (same JVM):                            │
│  ┌───────────────────────────────────────────────────┐  │
│  │ WithdrawalWorkflow  │  DepositWorkflow            │  │
│  │ BillPaymentWorkflow │  DuitNowTransferWorkflow    │  │
│  │                      │                            │  │
│  │ Activities:          │                            │  │
│  │  - VelocityCheck     │  - FloatBlock              │  │
│  │  - FeeCalculation    │  - FloatCommit             │  │
│  │  - FloatBlock        │  - FloatRelease            │  │
│  │  - FloatCommit       │  - SwitchAuthorize         │  │
│  │  - FloatRelease      │  - SwitchReversal          │  │
│  │  - SwitchAuthorize   │  - PublishEvent            │  │
│  │  - SwitchReversal    │                            │  │
│  │  - PublishEvent      │                            │  │
│  └───────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │ Feign (typed DTOs, no more Map!)
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Domain Core Services                                    │
│  Rules Service  │  Ledger Service  │  Switch Adapter    │
│  Biller Service │                  │                    │
└─────────────────────────────────────────────────────────┘
```

### 2.3 Temporal Infrastructure

**Development:** Temporal runs as a Docker container via docker-compose:
```yaml
services:
  temporal:
    image: temporalio/auto-setup:1.25
    ports:
      - "7233:7233"
    environment:
      - DB=postgresql
      - POSTGRES_SEEDS=temporal-postgres
      - POSTGRES_USER=temporal
      - POSTGRES_PWD=temporal
    depends_on:
      - temporal-postgres

  temporal-postgres:
    image: postgres:16
    environment:
      - POSTGRES_USER=temporal
      - POSTGRES_PASSWORD=temporal
      - POSTGRES_DB=temporal

  temporal-ui:
    image: temporalio/ui:2.27
    ports:
      - "8082:8080"
    environment:
      - TEMPORAL_ADDRESS=temporal:7233
```

**Production:** Temporal Cloud or self-hosted cluster (same workflow code).

---

## 3. Transaction Routing (Dual-Leg Model)

Every transaction has two legs:
1. **Agent Leg** — Ledger Service (block/commit/release float)
2. **Target Leg** — varies by transaction type

### 3.1 Routing Table

| Transaction Type | Agent Leg | Target Leg | Workflow |
|-----------------|-----------|------------|----------|
| CASH_WITHDRAWAL (Off-Us) | BlockFloat → CommitFloat | SwitchAdapter → PayNet (ISO 8583) | WithdrawalWorkflow |
| CASH_WITHDRAWAL (On-Us) | BlockFloat → CommitFloat | CBS API (direct) | WithdrawalOnUsWorkflow |
| CASH_DEPOSIT | CreditAgentFloat | CBS API (direct) | DepositWorkflow |
| BILL_PAYMENT | BlockFloat → CommitFloat | BillerService → Biller Gateway | BillPaymentWorkflow |
| DUITNOW_TRANSFER | BlockFloat → CommitFloat | SwitchAdapter → PayNet (ISO 20022) | DuitNowTransferWorkflow |

### 3.2 WorkflowRouter

The `WorkflowRouter` inspects the incoming request and determines which Temporal workflow to start:

```
Input: TransactionRequest {
  transactionType: CASH_WITHDRAWAL | CASH_DEPOSIT | BILL_PAYMENT | DUITNOW_TRANSFER
  targetBIN: "0123" | null          // Bank Identification Number
  billerCode: "TNB" | null
  proxyType: MOBILE | MYKAD | BRN | null
}

Output: WorkflowType + WorkflowOptions
```

Routing logic:
- `CASH_WITHDRAWAL` + targetBIN == BSN → `WithdrawalOnUsWorkflow`
- `CASH_WITHDRAWAL` + targetBIN != BSN → `WithdrawalWorkflow`
- `CASH_DEPOSIT` → `DepositWorkflow`
- `BILL_PAYMENT` → `BillPaymentWorkflow`
- `DUITNOW_TRANSFER` → `DuitNowTransferWorkflow`

---

## 4. Workflow Specifications

### 4.1 WithdrawalWorkflow (Off-Us)

**Purpose:** Process cash withdrawals via PayNet (ISO 8583) with durable execution.

**Workflow Interface:**
```java
@WorkflowInterface
public interface WithdrawalWorkflow {
    @WorkflowMethod
    WorkflowResult execute(WithdrawalWorkflowInput input);

    @SignalMethod
    void forceResolve(ForceResolveSignal signal);

    @QueryMethod
    WorkflowStatus getStatus();
}
```

**Input:**
```java
record WithdrawalWorkflowInput(
    UUID agentId,
    String pan,
    String pinBlock,
    BigDecimal amount,
    String idempotencyKey,
    String customerCardMasked,
    BigDecimal geofenceLat,
    BigDecimal geofenceLng,
    String customerMykad
) {}
```

**Execution Flow:**
```
1. Activity: CheckVelocity(agentId, amount, customerMykad)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On failure: return FAILED immediately (no compensation needed)

2. Activity: CalculateFees("CASH_WITHDRAWAL", agentTier, amount)
   → StartToClose: 3s | Retry: 3x (1s→2s→4s)
   → On failure: return FAILED immediately (no compensation needed)

3. Activity: BlockFloat(agentId, amount + fees, idempotencyKey)
   → StartToClose: 5s | Retry: 0x (financial, idempotent)
   → Compensation: ReleaseFloat(agentId, amount + fees)
   → On failure: return FAILED (no compensation needed — float not blocked)

4. Activity: AuthorizeAtSwitch(pan, pinBlock, amount, internalTxnId)
   → StartToClose: 25s | Retry: 0x (financial)
   → On APPROVED: continue to step 5
   → On DECLINED: trigger compensation (ReleaseFloat), return FAILED
   → On TIMEOUT: trigger SafetyReversal + ReleaseFloat, return FAILED

5. Activity: CommitFloat(agentId, amount, internalTxnId)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On failure: trigger SafetyReversal + ReleaseFloat, return FAILED

6. Activity: PublishKafkaEvent(transactionDetails)
   → StartToClose: 3s | Retry: 3x (1s→2s→4s)
   → On failure: log and continue (non-critical)

7. Activity: CacheIdempotencyResponse(idempotencyKey, response)
   → StartToClose: 2s | Retry: 3x (1s→2s→4s)

8. Return WorkflowResult(COMPLETED, transactionId, referenceNumber)
```

**Compensation Chain (on failure after step 3):**
```
ReleaseFloat(agentId, amount + fees)
  → StartToClose: 5s | Retry: 3x (1s→2s→4s)
  → MUST succeed — if fails, workflow stays in COMPENSATING state
```

**Safety Reversal (on switch timeout at step 4):**
```
1. Activity: SendReversalToSwitch(internalTxnId)
   → StartToClose: 10s | ScheduleToClose: 60s
   → Retry: Infinite, 60s interval (Store & Forward)
   → This Activity NEVER gives up — it persists until PayNet acknowledges

2. Activity: ReleaseFloat(agentId, amount + fees)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
```

**Human-in-the-Loop:**
```
If workflow is stuck in COMPENSATING state for > 4 hours:
  → Backoffice admin sends ForceResolveSignal(COMMIT | REVERSE)
  → Workflow.await() unblocks
  → Workflow executes the forced action
  → Audit log records admin ID and action
```

### 4.2 WithdrawalOnUsWorkflow (On-Us)

**Purpose:** Process cash withdrawals where the customer has a BSN account (direct CBS call, no PayNet).

**Execution Flow:**
```
1. Activity: CheckVelocity(...)
2. Activity: CalculateFees(...)
3. Activity: BlockFloat(agentId, amount + fees, idempotencyKey)
   → Compensation: ReleaseFloat
4. Activity: AuthorizeAtCBS(customerAccount, amount, pinBlock)
   → StartToClose: 15s | Retry: 0x (financial)
   → On failure: ReleaseFloat, return FAILED
5. Activity: CommitFloat(...)
6. Activity: PublishKafkaEvent(...)
7. Return WorkflowResult(COMPLETED, ...)
```

**Key difference from Off-Us:** No ISO 8583, no Safety Reversal (CBS returns definitive success/failure).

### 4.3 DepositWorkflow

**Purpose:** Process cash deposits where the agent collects physical cash and credits the customer's account.

**Input:**
```java
record DepositWorkflowInput(
    UUID agentId,
    String destinationAccount,
    BigDecimal amount,
    String idempotencyKey,
    String customerMykad,
    BigDecimal geofenceLat,
    BigDecimal geofenceLng,
    boolean requiresBiometric  // true if amount > high-value threshold
) {}
```

**Execution Flow:**
```
1. Activity: CheckVelocity(...)
2. Activity: CalculateFees(...)
3. Activity: ValidateAccount(destinationAccount)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On failure: return FAILED (no compensation needed)

4. [Conditional] Activity: VerifyBiometric(customerMykad)
   → Only if requiresBiometric == true
   → StartToClose: 10s | Retry: 0x
   → On failure: return FAILED

5. Activity: CreditAgentFloat(agentId, amount)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On failure: return FAILED (no compensation — agent hasn't lost money)

6. Activity: PostToCBS(destinationAccount, amount)
   → StartToClose: 15s | Retry: 0x (financial)
   → On failure: ReverseCreditFloat(agentId, amount), return FAILED

7. Activity: PublishKafkaEvent(...)
8. Return WorkflowResult(COMPLETED, ...)
```

**Key difference:** No BlockFloat — deposits credit the agent's float. Compensation is `ReverseCreditFloat` (debit back).

### 4.4 BillPaymentWorkflow

**Purpose:** Process bill payments (JomPAY, ASTRO, TM, EPF) where the agent collects cash and pays the biller.

**Input:**
```java
record BillPaymentWorkflowInput(
    UUID agentId,
    String billerCode,
    String ref1,
    String ref2,
    BigDecimal amount,
    String idempotencyKey,
    String customerMykad,
    BigDecimal geofenceLat,
    BigDecimal geofenceLng
) {}
```

**Execution Flow:**
```
1. Activity: CheckVelocity(...)
2. Activity: CalculateFees(...)
3. Activity: BlockFloat(agentId, amount + fees, idempotencyKey)
   → Compensation: ReleaseFloat

4. Activity: ValidateBill(billerCode, ref1)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On failure: ReleaseFloat, return FAILED

5. Activity: PayBiller(billerCode, ref1, ref2, amount, idempotencyKey)
   → StartToClose: 15s | Retry: 0x (financial)
   → On failure: ReleaseFloat + NotifyBillerReversal(billerCode, ref1), return FAILED

6. Activity: CommitFloat(agentId, amount, internalTxnId)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On failure: ReleaseFloat + NotifyBillerReversal, return FAILED

7. Activity: NotifyBiller(internalTxnId, amount)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On failure: log and continue (non-critical — biller already paid)

8. Activity: PublishKafkaEvent(...)
9. Return WorkflowResult(COMPLETED, billerReference)
```

**Key difference:** Two external calls (ValidateBill + PayBiller). Compensation includes notifying the biller of reversal.

### 4.5 DuitNowTransferWorkflow

**Purpose:** Process DuitNow fund transfers via PayNet (ISO 20022) with real-time settlement.

**Input:**
```java
record DuitNowTransferWorkflowInput(
    UUID agentId,
    String proxyType,      // MOBILE, MYKAD, BRN
    String proxyValue,     // phone number, MyKad, or BRN
    BigDecimal amount,
    String idempotencyKey,
    String customerMykad,
    BigDecimal geofenceLat,
    BigDecimal geofenceLng
) {}
```

**Execution Flow:**
```
1. Activity: CheckVelocity(...)
2. Activity: CalculateFees(...)
3. Activity: BlockFloat(agentId, amount + fees, idempotencyKey)
   → Compensation: ReleaseFloat

4. Activity: ProxyEnquiry(proxyType, proxyValue)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → Returns: recipient name (masked), bank code
   → On failure: ReleaseFloat, return FAILED

5. Activity: SendDuitNowTransfer(recipientBank, recipientAccount, amount, internalTxnId)
   → StartToClose: 25s | Retry: 0x (financial)
   → On APPROVED: continue to step 6
   → On DECLINED: ReleaseFloat, return FAILED
   → On TIMEOUT: SafetyReversal + ReleaseFloat, return FAILED

6. Activity: CommitFloat(agentId, amount, internalTxnId)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On failure: SafetyReversal + ReleaseFloat, return FAILED

7. Activity: PublishKafkaEvent(...)
8. Return WorkflowResult(COMPLETED, paynetReference)
```

**NFR compliance:** Must complete in < 15 seconds end-to-end (happy path).

---

## 5. Activity Specifications

### 5.1 Activity Interface Pattern

Each Activity is a pure function that:
- Accepts a typed input record
- Calls a port interface (Feign client adapter)
- Returns a typed output record
- Throws typed exceptions (never raw Exception)

```java
@ActivityInterface
public interface FloatBlockActivity {
    @ActivityMethod
    FloatBlockResult blockFloat(FloatBlockInput input);
}

record FloatBlockInput(UUID agentId, BigDecimal amount, String idempotencyKey) {}
record FloatBlockResult(boolean success, UUID transactionId, String errorCode) {}
```

### 5.2 Activity Catalog

| Activity | Input | Output | Exceptions | Compensation |
|----------|-------|--------|------------|--------------|
| CheckVelocityActivity | agentId, amount, mykad | passed, errorCode | VelocityCheckFailedException | None |
| CalculateFeesActivity | txnType, tier, amount | customerFee, agentCommission, bankShare | FeeConfigNotFoundException | None |
| BlockFloatActivity | agentId, amount, idempotencyKey | success, transactionId | InsufficientFloatException, FloatBlockFailedException | ReleaseFloat |
| CommitFloatActivity | agentId, amount, transactionId | success, newBalance | FloatCommitFailedException | None (forward-only) |
| ReleaseFloatActivity | agentId, amount, transactionId | success | FloatReleaseFailedException | None (must succeed) |
| CreditAgentFloatActivity | agentId, amount | success, newBalance | FloatCapExceededException | ReverseCreditFloat |
| AuthorizeAtSwitchActivity | pan, pinBlock, amount, txnId | approved, referenceCode, responseCode | SwitchDeclinedException, SwitchTimeoutException | SafetyReversal |
| SendReversalToSwitchActivity | internalTxnId | success | ReversalFailedException | None (infinite retry) |
| PublishKafkaEventActivity | eventDetails | success | EventPublishFailedException | None (non-critical) |
| ValidateAccountActivity | destinationAccount | valid, accountName | InvalidAccountException | None |
| VerifyBiometricActivity | customerMykad | match, status | BiometricMismatchException | None |
| PostToCBSActivity | account, amount | success, reference | CBSUnavailableException | ReverseCreditFloat |
| ValidateBillActivity | billerCode, ref1 | valid, accountName, amountDue | InvalidBillerException, InvalidRef1Exception | None |
| PayBillerActivity | billerCode, ref1, ref2, amount | success, billerReference | BillerPaymentFailedException, BillerTimeoutException | NotifyBillerReversal |
| NotifyBillerActivity | txnId, amount | success | NotificationFailedException | None (non-critical) |
| ProxyEnquiryActivity | proxyType, proxyValue | valid, recipientName, bankCode | ProxyNotFoundException | None |
| SendDuitNowTransferActivity | recipientBank, account, amount, txnId | success, paynetReference | DuitNowDeclinedException, DuitNowTimeoutException | SafetyReversal |
| CacheIdempotencyActivity | key, response | success | CacheFailedException | None |

### 5.3 Activity Timeout & Retry Policies

| Activity | StartToClose | ScheduleToClose | Retry Policy | Rationale |
|----------|-------------|-----------------|--------------|-----------|
| CheckVelocity | 5s | 10s | 3x, 1s→2s→4s | Non-financial, safe to retry |
| CalculateFees | 3s | 5s | 3x, 1s→2s→4s | Non-financial, cached in Redis |
| BlockFloat | 5s | 10s | 0x | Financial — idempotent via key |
| CommitFloat | 5s | 10s | 3x, 1s→2s→4s | Financial — idempotent |
| ReleaseFloat | 5s | 10s | 3x, 1s→2s→4s | Compensation — must succeed |
| AuthorizeAtSwitch | 25s | 30s | 0x | Financial — timeout → reversal |
| SendReversal | 10s | 60s | Infinite, 60s | Must never give up (S&F) |
| PublishEvent | 3s | 5s | 3x, 1s→2s→4s | Non-critical, fire-and-forget |
| ValidateAccount | 5s | 10s | 3x, 1s→2s→4s | Non-financial |
| PayBiller | 15s | 20s | 0x | Financial |
| SendDuitNow | 25s | 30s | 0x | Financial — timeout → reversal |

---

## 6. Error Handling

### 6.1 Typed Exception Hierarchy

All Activities throw typed exceptions that map to the Global Error Schema:

```
TransactionException (abstract)
├── ValidationException (ERR_VAL_xxx)
│   ├── InvalidAmountException
│   ├── InvalidMykadFormatException
│   ├── MissingIdempotencyKeyException
│   └── GeofenceViolationException
├── BusinessException (ERR_BIZ_xxx)
│   ├── InsufficientFloatException
│   ├── LimitExceededException
│   ├── VelocityCountExceededException
│   ├── FloatCapExceededException
│   ├── FeeConfigNotFoundException
│   └── AgentDeactivatedException
├── ExternalServiceException (ERR_EXT_xxx)
│   ├── SwitchDeclinedException
│   ├── SwitchTimeoutException
│   ├── CBSUnavailableException
│   ├── BillerPaymentFailedException
│   └── DuitNowDeclinedException
├── AuthenticationException (ERR_AUTH_xxx)
│   ├── InvalidPinException
│   └── BiometricMismatchException
└── SystemException (ERR_SYS_xxx)
    ├── ServiceUnavailableException
    └── InternalErrorException
```

### 6.2 Error Mapping from External Systems

Tier 4 (Translation Layer) normalizes legacy codes before they reach the Orchestrator:

| Legacy Source | External Code | Business Error | Action Category |
|--------------|--------------|---------------|----------------|
| ISO 8583 | 00 | SUCCESS | Finalize |
| ISO 8583 | 51 | INSUFFICIENT_FUNDS | Notify Customer |
| ISO 8583 | 05 | DECLINED_BY_ISSUER | Notify Customer |
| ISO 8583 | 13 | INVALID_TRANSACTION | Stop / Alert |
| ISO 8583 | 91 | NETWORK_TIMEOUT | Trigger Reversal |
| ISO 20022 | AB05 | NETWORK_TIMEOUT | Trigger Reversal |
| ISO 20022 | AC04 | ACCOUNT_INACTIVE | Notify Customer |
| CBS | E102 | ACCOUNT_FROZEN | Notify Customer |
| CBS | E999 | DOWNSTREAM_UNAVAILABLE | Retry / Alert |
| HSM | 15 | INVALID_PIN | Block / Security Alert |

### 6.3 Workflow Error Response

```java
record WorkflowResult(
    String status,           // COMPLETED, FAILED, REVERSED
    UUID transactionId,
    String errorCode,
    String errorMessage,
    String actionCode,       // DECLINE, RETRY, REVIEW
    String referenceNumber,
    Map<String, Object> metadata
) {}
```

---

## 7. Service Structure

```
orchestrator-service/
├── domain/
│   ├── model/
│   │   ├── TransactionType.java               # enum: all transaction types
│   │   ├── WorkflowResult.java                # record
│   │   ├── WorkflowStatus.java                # enum: PENDING, RUNNING, COMPLETED, FAILED, COMPENSATING
│   │   └── ForceResolveSignal.java            # record: COMMIT | REVERSE
│   ├── port/in/
│   │   ├── StartTransactionUseCase.java
│   │   └── QueryWorkflowStatusUseCase.java
│   ├── port/out/
│   │   ├── RulesServicePort.java              # typed interfaces (no Map!)
│   │   ├── LedgerServicePort.java
│   │   ├── SwitchAdapterPort.java
│   │   ├── BillerServicePort.java
│   │   ├── CbsServicePort.java
│   │   └── EventPublisherPort.java
│   └── service/
│       ├── WorkflowRouter.java                # determines workflow type
│       └── CompensationRegistry.java          # maps activities to compensations
├── application/
│   ├── workflow/
│   │   ├── WithdrawalWorkflow.java            # @WorkflowInterface
│   │   ├── WithdrawalOnUsWorkflow.java
│   │   ├── DepositWorkflow.java
│   │   ├── BillPaymentWorkflow.java
│   │   └── DuitNowTransferWorkflow.java
│   ├── activity/
│   │   ├── CheckVelocityActivity.java
│   │   ├── CalculateFeesActivity.java
│   │   ├── BlockFloatActivity.java
│   │   ├── CommitFloatActivity.java
│   │   ├── ReleaseFloatActivity.java
│   │   ├── CreditAgentFloatActivity.java
│   │   ├── AuthorizeAtSwitchActivity.java
│   │   ├── SendReversalToSwitchActivity.java
│   │   ├── PublishKafkaEventActivity.java
│   │   ├── ValidateAccountActivity.java
│   │   ├── VerifyBiometricActivity.java
│   │   ├── PostToCBSActivity.java
│   │   ├── ValidateBillActivity.java
│   │   ├── PayBillerActivity.java
│   │   ├── NotifyBillerActivity.java
│   │   ├── ProxyEnquiryActivity.java
│   │   ├── SendDuitNowTransferActivity.java
│   │   └── CacheIdempotencyActivity.java
│   └── usecase/
│       ├── StartTransactionUseCaseImpl.java
│       └── QueryWorkflowStatusUseCaseImpl.java
├── infrastructure/
│   ├── web/
│   │   ├── OrchestratorController.java
│   │   └── dto/
│   │       ├── TransactionRequest.java
│   │       ├── TransactionResponse.java
│   │       └── WorkflowStatusResponse.java
│   ├── temporal/
│   │   ├── TemporalConfig.java
│   │   ├── WorkflowFactory.java
│   │   ├── WorkflowImpl/
│   │   │   ├── WithdrawalWorkflowImpl.java
│   │   │   ├── DepositWorkflowImpl.java
│   │   │   ├── BillPaymentWorkflowImpl.java
│   │   │   └── DuitNowTransferWorkflowImpl.java
│   │   └── ActivityImpl/
│   │       └── (all activity implementations)
│   ├── external/
│   │   ├── RulesServiceClient.java            # @FeignClient
│   │   ├── LedgerServiceClient.java
│   │   ├── SwitchAdapterClient.java
│   │   ├── BillerServiceClient.java
│   │   └── adapters/
│   │       └── (implement domain ports)
│   └── messaging/
│       └── KafkaEventPublisher.java
├── config/
│   ├── DomainServiceConfig.java
│   └── TemporalWorkerConfig.java
└── src/test/
    ├── domain/service/
    │   └── WorkflowRouterTest.java
    ├── application/workflow/
    │   ├── WithdrawalWorkflowTest.java
    │   └── (workflow replay tests)
    ├── application/activity/
    │   └── (activity unit tests)
    └── architecture/
        └── HexagonalArchitectureTest.java
```

---

## 8. Database Schema (orchestrator_db)

Temporal persists workflow state. We maintain a lightweight query table for backoffice and reporting:

```sql
-- V1__transaction_record.sql
CREATE TABLE transaction_record (
    id UUID PRIMARY KEY,
    workflow_id VARCHAR(128) NOT NULL UNIQUE,
    transaction_type VARCHAR(50) NOT NULL,
    agent_id UUID NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    customer_fee DECIMAL(10,2),
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(50),
    error_message TEXT,
    external_reference VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_txn_record_agent ON transaction_record(agent_id);
CREATE INDEX idx_txn_record_status ON transaction_record(status);
CREATE INDEX idx_txn_record_created ON transaction_record(created_at);
CREATE INDEX idx_txn_record_type ON transaction_record(transaction_type);
```

---

## 9. API Contract

### 9.1 Start Transaction

```
POST /api/v1/transactions
Authorization: Bearer <JWT>
X-Idempotency-Key: <UUID>
X-POS-Terminal-Id: <terminal-id>
X-GPS-Latitude: <lat>
X-GPS-Longitude: <lng>
Content-Type: application/json

Request body (polymorphic — determined by transactionType):
{
  "transactionType": "CASH_WITHDRAWAL",
  "amount": 500.00,
  "pan": "411111******1111",
  "pinBlock": "<encrypted>",
  "customerCardMasked": "411111******1111"
}

Response (202 Accepted):
{
  "status": "PENDING",
  "workflowId": "IDEM-uuid-123",
  "pollUrl": "/api/v1/transactions/IDEM-uuid-123/status",
  "message": "Transaction initiated. Poll for status."
}
```

### 9.2 Poll Transaction Status

```
GET /api/v1/transactions/{workflowId}/status

Response (200 OK):
{
  "status": "COMPLETED",
  "workflowId": "IDEM-uuid-123",
  "transactionId": "TXN-uuid-456",
  "amount": 500.00,
  "customerFee": 1.00,
  "referenceNumber": "PAYNET-REF-789",
  "completedAt": "2026-04-05T14:30:00+08:00"
}

Response (200 OK) — Failed:
{
  "status": "FAILED",
  "workflowId": "IDEM-uuid-123",
  "error": {
    "code": "ERR_INSUFFICIENT_FUNDS",
    "message": "Customer account balance too low.",
    "action_code": "DECLINE"
  }
}
```

### 9.3 Force Resolve (Backoffice)

```
POST /api/v1/backoffice/transactions/{workflowId}/resolve
Authorization: Bearer <backoffice-JWT>

{
  "action": "COMMIT",  // or "REVERSE"
  "reason": "PayNet confirmed approval after timeout",
  "adminId": "admin-001"
}
```

---

## 10. Idempotency Strategy

**Dual-layer idempotency:**

1. **Temporal level:** `workflowIdReusePolicy = WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE`
   - Uses `idempotencyKey` as the workflow ID
   - Temporal rejects duplicate workflow starts with `WorkflowExecutionAlreadyStartedError`
   - The controller catches this and returns the existing workflow's status

2. **Redis cache level:** Response caching for fast duplicate detection
   - TTL: 24 hours
   - Checked before starting workflow (fast path)
   - Updated after workflow completes

---

## 11. Human-in-the-Loop (Signal Handling)

**When needed:**
- Workflow stuck in COMPENSATING state for > 4 hours
- Workflow stuck waiting for external system that is down
- Admin needs to force-commit or force-reverse a transaction

**Flow:**
```
1. Admin views stuck transaction in backoffice UI
2. Admin clicks "Force Resolve" → selects COMMIT or REVERSE
3. Backoffice sends POST /api/v1/backoffice/transactions/{workflowId}/resolve
4. Orchestrator sends Temporal Signal to the workflow
5. Workflow.await() unblocks
6. Workflow executes the forced action
7. Audit log records: adminId, action, reason, timestamp
```

**Workflow implementation:**
```java
@WorkflowMethod
public WorkflowResult execute(WithdrawalWorkflowInput input) {
    // ... normal execution ...

    // Wait for admin signal if stuck
    Workflow.await(Duration.ofHours(4), () -> forceResolveSignal != null);

    if (forceResolveSignal != null) {
        log.info("Admin force-resolve: {}", forceResolveSignal.action());
        if (forceResolveSignal.action() == Action.COMMIT) {
            return commitTransaction();
        } else {
            return reverseTransaction();
        }
    }

    return WorkflowResult.failed("TIMEOUT_WAITING_FOR_ADMIN");
}
```

---

## 12. Migration Strategy

### Phase 1: Add Temporal (Week 1-2)
- Add Temporal SDK dependency
- Add Temporal services to docker-compose
- Create TemporalConfig, TemporalWorkerConfig
- Implement all 4 workflows + activities
- Create `TransactionRecord` entity + Flyway migration
- Keep existing `TransactionOrchestrator` (marked `@Deprecated`)
- New endpoint: `POST /api/v2/transactions` → starts Temporal workflow
- Polling endpoint: `GET /api/v2/transactions/{workflowId}/status`

### Phase 2: Switch Primary Endpoint (Week 3)
- `POST /api/v1/withdraw` routes to Temporal WithdrawalWorkflow
- Replace all `Map<String, Object>` with typed DTOs
- Add comprehensive ArchUnit tests
- Integration tests with Temporal test server

### Phase 3: Cleanup (Week 4)
- Remove deprecated `TransactionOrchestrator`
- Remove legacy saga code
- Remove `RedisIdempotencyCache` (kept only for response caching)
- Load testing and performance validation

---

## 13. Testing Strategy

### 13.1 Workflow Tests
- **WorkflowReplayer:** Replay workflow from event history to verify determinism
- **TemporalTestEnvironment:** In-memory Temporal server for integration tests
- Test each happy path and failure path per workflow

### 13.2 Activity Tests
- Unit test each Activity with mocked port interfaces
- Verify correct exception throwing
- Verify timeout and retry behavior

### 13.3 Integration Tests
- End-to-end: POS request → Temporal workflow → Feign calls → response
- Use mock server for downstream systems (PayNet, CBS, Billers)
- Test idempotency, duplicate detection, polling

### 13.4 ArchUnit Tests
- `domain/` has ZERO Spring/JPA/Temporal imports
- `application/workflow/` contains only Temporal workflow classes
- `application/activity/` contains only Activity implementations
- All port implementations in `infrastructure/`

### 13.5 Coverage Requirements
- All workflows: 100% branch coverage
- All activities: 100% line coverage
- Controller: 100% line coverage
- Router: 100% branch coverage

---

## 14. Observability

### 14.1 Temporal UI
- Access at `http://localhost:8081`
- View workflow execution history, activity results, signals
- Search by workflowId, status, time range

### 14.2 Logging
- Workflow start: `log.info("Workflow started: type={}, workflowId={}", type, workflowId)`
- Activity start: `log.info("Activity started: workflowId={}, activity={}", workflowId, activityName)`
- Activity completion: `log.info("Activity completed: workflowId={}, activity={}, result={}", ...)`
- Compensation: `log.warn("Compensation triggered: workflowId={}, step={}", workflowId, stepName)`
- Human-in-the-loop: `log.warn("Admin force-resolve: workflowId={}, action={}, adminId={}", ...)`

### 14.3 Metrics (Micrometer)
- `temporal.workflow.started` (counter, by type)
- `temporal.workflow.completed` (counter, by type, status)
- `temporal.workflow.duration` (timer, by type)
- `temporal.activity.failed` (counter, by activity, error)
- `temporal.compensation.triggered` (counter, by workflow, step)

---

## 15. Non-Functional Requirements

| ID | Requirement | How Met |
|----|------------|---------|
| NFR-1.1 | API Gateway response < 500ms (p95) | Workflow start returns in < 100ms |
| NFR-1.2 | DuitNow < 15 seconds end-to-end | Workflow timeout 5m, happy path < 15s |
| NFR-1.3 | Balance inquiry < 200ms | Not applicable (separate service) |
| NFR-2.1 | 99.9% uptime | Temporal durable execution survives crashes |
| NFR-2.2 | Circuit breaker on all inter-service calls | Resilience4j on all Feign clients |
| NFR-2.3 | Store & Forward for reversals | SendReversalActivity: infinite retry |
| NFR-3.2 | PINs never logged | pinBlock never appears in logs |
| NFR-3.3 | PAN masking | customerCardMasked field only |
| NFR-4.2 | Geofencing within 100m | GeofenceValidationActivity |

---

## 16. Constraints & Dependencies

### 16.1 Temporal Version
- Lock to Temporal SDK 1.25.x across all services
- Event schemas versioned in `docs/events/`
- One Temporal namespace per environment (dev, staging, prod)

### 16.2 Service Dependencies
| Orchestrator Activity | Depends On | Protocol |
|----------------------|------------|----------|
| CheckVelocity | Rules Service | Feign (sync) |
| CalculateFees | Rules Service | Feign (sync) |
| BlockFloat/CommitFloat/ReleaseFloat | Ledger Service | Feign (sync) |
| AuthorizeAtSwitch | Switch Adapter Service | Feign (sync) |
| PayBiller/ValidateBill | Biller Service | Feign (sync) |
| PostToCBS | CBS Connector (Tier 4) | Feign (sync) |
| PublishKafkaEvent | Kafka | Spring Cloud Stream |

### 16.3 Hexagonal Architecture Compliance
- `domain/` — ZERO imports from Spring, Temporal, JPA, Kafka
- `application/workflow/` — Temporal workflow interfaces only
- `application/activity/` — Activity implementations (call domain ports)
- `infrastructure/temporal/` — WorkflowImpl classes (Temporal annotations)
- `infrastructure/external/` — Feign clients and port adapters

---

## 17. Open Questions

1. **Temporal namespace naming** — Should we use `default` for dev and environment-specific namespaces for prod?
2. **Workflow history retention** — How long should Temporal retain workflow history? (Default: 1 year, configurable)
3. **Backoffice polling interval** — What should the recommended polling interval be for POS terminals? (Recommended: 500ms)
4. **Force-resolve SLA** — What is the maximum time a transaction can remain in COMPENSATING state before requiring admin intervention? (Recommended: 4 hours)

---

## 18. Backoffice UI — Transaction Resolution Dashboard

### 18.1 Purpose

Provide backoffice admins (Maker + Checker roles) with a UI to view and resolve stuck transactions (COMPENSATING, FAILED, or PENDING_REVIEW states) through a Four-Eyes Principle workflow, fulfilling FR-17.4–FR-17.7 and FR-18.3.

### 18.2 Architecture

**Location:** `backoffice/src/pages/TransactionResolution.tsx` (new page in existing React + Vite backoffice app)

**Pattern:** Reuses existing Maker-Checker pattern from ledger discrepancy resolution:
- Backend: `PENDING_CHECKER` → `APPROVED` / `REJECTED` state machine
- Frontend: `KycReview.tsx` pattern (list view, detail modal, approve/reject mutations)
- Auth: Separate maker/checker JWT tokens with role enforcement

### 18.3 Backend API (orchestrator-service)

Three endpoints following the discrepancy resolution pattern:

```
POST /api/v1/backoffice/transactions/{workflowId}/maker-propose
Authorization: Bearer <maker-JWT>
{
  "action": "COMMIT",           // or "REVERSE"
  "reasonCode": "PAYNET_CONFIRMED",
  "reason": "PayNet confirmed approval after timeout",
  "evidenceUrl": "https://..."  // optional
}

POST /api/v1/backoffice/transactions/{workflowId}/checker-approve
Authorization: Bearer <checker-JWT>
{
  "reason": "Verified with PayNet support ticket #12345"
}

POST /api/v1/backoffice/transactions/{workflowId}/checker-reject
Authorization: Bearer <checker-JWT>
{
  "reason": "Insufficient evidence, please provide PayNet confirmation"
}
```

**Four-Eyes Enforcement:** API rejects `checker-approve` and `checker-reject` if the checker's user ID matches the maker's user ID (FR-17.6). Returns `ERR_SELF_APPROVAL_PROHIBITED`.

### 18.4 Database Schema

```sql
-- V2__transaction_resolution_case.sql
CREATE TABLE transaction_resolution_case (
    id UUID PRIMARY KEY,
    workflow_id VARCHAR(128) NOT NULL,
    transaction_id UUID NOT NULL,
    proposed_action VARCHAR(20) NOT NULL,    -- COMMIT or REVERSE
    reason_code VARCHAR(50) NOT NULL,
    reason TEXT NOT NULL,
    evidence_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_CHECKER',  -- PENDING_MAKER, PENDING_CHECKER, APPROVED, REJECTED
    maker_user_id VARCHAR(128) NOT NULL,
    maker_created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    checker_user_id VARCHAR(128),
    checker_action VARCHAR(20),              -- APPROVED or REJECTED
    checker_reason TEXT,
    checker_completed_at TIMESTAMP,
    temporal_signal_sent BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_resolution_case_status ON transaction_resolution_case(status);
CREATE INDEX idx_resolution_case_workflow ON transaction_resolution_case(workflow_id);
```

### 18.5 Frontend Components

**Page:** `TransactionResolution.tsx`

| Component | Description |
|-----------|-------------|
| `ResolutionList` | Table of stuck transactions with status, type, amount, time stuck, SLA badge |
| `ResolutionDetail` | Modal showing Temporal event history, workflow state, compensation chain |
| `MakerProposalForm` | Form: action selector (COMMIT/REVERSE), reason code dropdown, reason text, evidence upload |
| `CheckerReviewPanel` | Review maker's proposal, approve/reject buttons with reason text |
| `StatsPanel` | Counters: Pending Maker, Pending Checker, Resolved Today, Overdue SLA |

**Navigation:** Add "Transaction Resolution" to sidebar Layout, between "KYC Review" and "Settlement"

**API client:** Add to `backoffice/src/api/client.ts`:
```typescript
proposeResolution: (workflowId: string, data: MakerProposal) => api.post(...)
approveResolution: (workflowId: string, data: CheckerAction) => api.post(...)
rejectResolution: (workflowId: string, data: CheckerAction) => api.post(...)
getStuckTransactions: (params: QueryParams) => api.get(...)
```

### 18.6 Flow

```
1. Maker views stuck transaction in ResolutionList
2. Maker clicks "Propose Resolution" → opens MakerProposalForm
3. Maker selects COMMIT/REVERSE, enters reason, submits
4. Status → PENDING_CHECKER, transaction_resolution_case created
5. Checker sees item in list with "Pending Review" badge
6. Checker opens ResolutionDetail, reviews maker's proposal
7. Checker clicks "Approve" → checker-approve endpoint called
   a. API verifies checker ≠ maker (Four-Eyes)
   b. Status → APPROVED, temporal_signal_sent = true
   c. Orchestrator sends Temporal ForceResolveSignal to workflow
8. OR Checker clicks "Reject" → checker-reject endpoint called
   a. Status → PENDING_MAKER (case returns to maker for re-proposal)
   b. Back to step 2
```

### 18.7 Reason Codes

| Code | Description | Applicable Actions |
|------|-------------|-------------------|
| `PAYNET_CONFIRMED` | PayNet confirmed approval after timeout | COMMIT |
| `PAYNET_DECLINED` | PayNet confirmed decline after timeout | REVERSE |
| `CBS_CONFIRMED` | CBS confirmed transaction after timeout | COMMIT |
| `CUSTOMER_DISPUTE` | Customer reported double deduction | REVERSE |
| `SYSTEM_ERROR` | Internal error detected, manual correction needed | COMMIT or REVERSE |
| `FRAUD_SUSPECTED` | Fraud indicators detected, freeze and investigate | REVERSE |

---

## 19. Missing Transaction Types (Phase 3+)

The current design covers 4 MVP+Phase2 transaction types. The BRD defines 10 additional types:

### 19.1 Transaction Type Inventory

| Transaction Type | FR Reference | Phase | Workflow Complexity |
|-----------------|-------------|-------|-------------------|
| **BALANCE_INQUIRY** | FR-5 | MVP | Low — no float operations, read-only |
| **PREPAID_TOPUP** (CELCOM, M1) | FR-8 | Phase 2 | Medium — BlockFloat → Telco API → CommitFloat |
| **EWALLET_WITHDRAWAL** (Sarawak Pay) | FR-10.1 | Phase 2 | Medium — BlockFloat → eWallet API → CommitFloat |
| **EWALLET_TOPUP** (Sarawak Pay) | FR-10.2 | Phase 2 | Medium — CreditAgentFloat → eWallet API |
| **ESSP_PURCHASE** (BSN eSSP) | FR-10.3 | Phase 2 | Medium — BlockFloat → eSSP API → CommitFloat |
| **CASHLESS_PAYMENT** | FR-11.1 | Phase 2 | Medium — similar to withdrawal flow |
| **PIN_BASED_PURCHASE** | FR-11.2 | Phase 2 | Medium — similar to withdrawal with PIN |
| **RETAIL_SALE** (Merchant) | FR-15.1 | Phase 2 | Medium — float increases via MDR |
| **PIN_PURCHASE** (Merchant) | FR-15.2 | Phase 2 | Medium — float decreases via commission |
| **HYBRID_CASHBACK** | FR-15.5 | Phase 2 | High — combines sale + withdrawal |

### 19.2 Extension Pattern

Each new transaction type follows the same pattern:

1. Add to `TransactionType` enum
2. Create workflow interface + implementation in `application/workflow/`
3. Create workflow-specific activities (reuse shared activities where possible)
4. Add routing rule to `WorkflowRouter`
5. Add BDD scenarios
6. Update OpenAPI spec

### 19.3 Detailed Design Process

When a new transaction type enters the implementation queue:

1. Create a design addendum at `docs/superpowers/specs/agent-banking-platform/YYYY-MM-DD-<txn-type>-design.md`
2. The addendum references this master design and specifies only what's unique:
   - Workflow execution flow (like Section 4)
   - New activities required (with input/output/exceptions)
   - Compensation chain
   - Cross-service dependencies and endpoint analysis
   - BDD scenarios
3. Follow the brainstorming → writing-plans → implementation workflow

**Do NOT create these addendums now.** Each type needs its own cross-service dependency analysis (e.g., telco API for prepaid, eWallet provider integration) that should be done when requirements are confirmed and the type is actively being implemented.

### 19.4 Out of Scope

These types are explicitly **out of scope** for the current implementation phase. They should be added in subsequent phases using the extension pattern above.

---

## 20. Cross-Service Dependency Analysis

### 20.1 Endpoint Mismatches

The orchestrator's Feign client interfaces reference endpoints that do not match actual service implementations:

| Orchestrator Feign Client | Expected Endpoint | Actual Service Endpoint | Resolution |
|---------------------------|------------------|------------------------|------------|
| `SwitchAdapterClient` | `POST /internal/authorize` | `POST /internal/auth` | **Rename Feign URL** |
| `SwitchAdapterClient` | `POST /internal/proxy-enquiry` | `GET /internal/transfer/proxy/enquiry` | **Rename Feign URL + method** |
| `SwitchAdapterClient` | `POST /internal/duitnow-transfer` | `POST /internal/duitnow` | **Rename Feign URL** |
| `BillerServiceClient` | `POST /internal/validate-bill` | `POST /internal/validate-ref` | **Rename Feign URL** |
| `BillerServiceClient` | `POST /internal/notify-biller` | Does NOT exist | **New endpoint in biller-service** |
| `BillerServiceClient` | `POST /internal/notify-biller-reversal` | Does NOT exist | **New endpoint in biller-service** |
| `LedgerServiceClient` | `POST /internal/credit-agent` | Does NOT exist | **New endpoint in ledger-service** |
| `LedgerServiceClient` | `POST /internal/reverse` | `POST /internal/reverse/{transactionId}` | **Align path parameter** |
| `LedgerServiceClient` | `POST /internal/validate-account` | Does NOT exist | **New endpoint in ledger-service** |

### 20.2 New Endpoints Required

#### biller-service
```
POST /internal/notify-biller
Request: { billerCode, ref1, internalTxnId, amount }
Response: { success: boolean }

POST /internal/notify-biller-reversal
Request: { billerCode, ref1, internalTxnId, amount, reason }
Response: { success: boolean }
```

#### ledger-service
```
POST /internal/credit-agent
Request: { agentId, amount, idempotencyKey }
Response: { success: boolean, newBalance: BigDecimal }

POST /internal/validate-account
Request: { accountNumber: String }
Response: { valid: boolean, accountName: String }
```

### 20.3 Missing Configuration Parameters

| Parameter | Current Location | Required By | Action |
|-----------|-----------------|-------------|--------|
| Biometric threshold (RM 5,000) | Not defined | DepositWorkflow (VerifyBiometricActivity) | Add to rules-service fee config or as dedicated config endpoint |
| STP evaluation endpoint | `/internal/stp/evaluate` exists | All workflows (pre-financial step) | Add `EvaluateStpActivity` to workflows |
| Geofence validation | Onboarding service has geofence adapter | All workflows (geofence validation) | Add `GeofenceValidationActivity` or integrate into existing velocity check |

### 20.4 Biometric Verification Flow

For deposits above RM 5,000 (BDD-WF-EC-D02):

1. `DepositWorkflow` checks amount against threshold
2. If above threshold, invokes `VerifyBiometricActivity`
3. Activity calls onboarding service's `/internal/biometric` endpoint
4. On success → continue to `CreditAgentFloat`
5. On failure → return `FAILED` with `ERR_BIOMETRIC_MISMATCH`

The onboarding service already has this endpoint. The orchestrator needs a Feign client to call it.

---

## 21. STP Processing Integration

### 21.1 Current Gap

The existing workflows do NOT evaluate STP (Straight-Through Processing) classification. Per BDD-S01, BDD-S02, and BDD-S03:

- **100% STP:** Auto-process with zero human intervention
- **Conditional STP:** Evaluate rules matrix; auto-approve if all criteria pass, else route to manual review
- **Non-STP:** Route to Maker-Checker workflow (PENDING_REVIEW status)

### 21.2 Solution: Add STP Evaluation Step

Add `EvaluateStpActivity` as step 1.5 in every workflow (after velocity check, before financial operations):

```
1. Activity: CheckVelocity(...)
2. Activity: EvaluateStp(transactionType, agentId, amount, customerProfile)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → Returns: StpDecision { category, approved, reason }
   → On FULL_STP: continue to next step
   → On CONDITIONAL_STP + approved: continue to next step
   → On CONDITIONAL_STP + NOT approved: create review case, return PENDING_REVIEW
   → On NON_STP: create review case, return PENDING_REVIEW
3. Activity: CalculateFees(...)
...
```

### 21.3 New Workflow Status

Add `PENDING_REVIEW` to `WorkflowStatus` enum:
```java
public enum WorkflowStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    PENDING_REVIEW    // NEW: awaiting manual backoffice review
}
```

### 21.4 Review Case Creation

When STP evaluation returns `NON_STP` or `CONDITIONAL_STP` (not approved):

1. Create `TransactionResolutionCase` with status `PENDING_MAKER` (awaiting a maker to propose a resolution)
2. Return `WorkflowResult` with status `PENDING_REVIEW`
3. Backoffice UI shows transaction in Resolution List with "Needs Review" badge
4. Maker reviews, proposes action → status → `PENDING_CHECKER` → Checker approves/rejects → Temporal signal sent

**Note:** This differs from the Force Resolve flow (Section 18.6) where a maker initiates the proposal. In the STP fallback flow, the system auto-creates the case and a maker must first pick it up and propose an action.

### 21.5 BDD Compliance

| BDD Scenario | Coverage | How |
|-------------|----------|-----|
| BDD-S01 (100% STP) | Covered | `EvaluateStpActivity` returns FULL_STP → auto-process |
| BDD-S01-EC-01 (velocity fail) | Covered | `CheckVelocity` fails before STP eval → auto-decline |
| BDD-S02 (Conditional STP) | Covered | `EvaluateStpActivity` returns CONDITIONAL_STP + approved → continue |
| BDD-S02-EC-01 (criteria fail) | Covered | `EvaluateStpActivity` returns CONDITIONAL_STP + not approved → PENDING_REVIEW |
| BDD-S03 (Non-STP) | Covered | `EvaluateStpActivity` returns NON_STP → PENDING_REVIEW |

---

## 22. E2E Integration Test Plan

### 22.1 Gateway E2E Tests

**Location:** `gateway/src/test/java/com/agentbanking/gateway/integration/`

| Test Class | Scenario | Auth Role |
|-----------|----------|-----------|
| `TransactionWithdrawalTest` | Full withdrawal flow: POS → Gateway → Orchestrator → Temporal → Switch → Response | Agent |
| `TransactionDepositTest` | Full deposit flow with biometric check | Agent |
| `TransactionBillPaymentTest` | Bill payment flow through biller-service | Agent |
| `TransactionDuitNowTest` | DuitNow transfer with proxy enquiry | Agent |
| `TransactionIdempotencyTest` | Duplicate request returns same workflowId | Agent |
| `TransactionStatusPollTest` | Poll workflow status until COMPLETED | Agent |

### 22.2 Cross-Service Integration Tests

**Location:** `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/integration/`

| Test | Services Involved | Verification |
|------|------------------|--------------|
| `WithdrawalEndToEndTest` | Orchestrator → Rules → Ledger → Switch | Float blocked, switch authorized, float committed |
| `DepositEndToEndTest` | Orchestrator → Rules → Ledger → CBS | Float credited, CBS posted |
| `BillPaymentEndToEndTest` | Orchestrator → Rules → Ledger → Biller | Float blocked, biller paid, float committed |
| `CompensationEndToEndTest` | Orchestrator → Ledger (fail) | Float released on failure |
| `StpEvaluationTest` | Orchestrator → Rules (STP) | Correct routing based on STP category |

### 22.3 Backoffice Maker-Checker E2E Tests

**Location:** `gateway/src/test/java/com/agentbanking/gateway/integration/backoffice/`

| Test | Scenario | Auth Roles |
|------|----------|-----------|
| `TransactionResolutionMakerTest` | Maker proposes COMMIT for stuck transaction | Maker |
| `TransactionResolutionCheckerApproveTest` | Checker approves → Temporal signal sent | Checker |
| `TransactionResolutionCheckerRejectTest` | Checker rejects → back to PENDING_MAKER | Checker |
| `TransactionResolutionFourEyesTest` | Same user as maker+checker → rejected | Maker (as checker) |

### 22.4 Temporal Workflow Replay Tests

**Location:** `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/workflow/`

| Test | Purpose |
|------|---------|
| `WithdrawalWorkflowReplayTest` | Replay withdrawal event history, verify determinism |
| `DepositWorkflowReplayTest` | Replay deposit event history |
| `CompensationReplayTest` | Replay failed workflow with compensation chain |

### 22.5 Test Infrastructure

- **Temporal:** Use `TemporalTestEnvironment` (in-memory test server) for unit/integration tests
- **Downstream mocks:** Use WireMock for PayNet, CBS, Biller endpoints
- **Auth:** Reuse existing `AuthTokenProvider` with maker/checker/agent tokens
- **Database:** TestContainers with PostgreSQL for each service

---

## 23. OpenAPI & Gateway Updates

### 23.1 Already Completed

From the previous implementation phase:
- `docs/api/openapi.yaml` updated with `POST /api/v1/transactions` and `GET /api/v1/transactions/{workflowId}/status`
- 7 legacy endpoints marked as `deprecated: true`
- Gateway routes configured for orchestrator-service at `/api/v1/transactions`
- API changelog at `docs/api/CHANGELOG-2026-04-05-transaction-orchestrator.md`

### 23.2 Still Needed

**New backoffice endpoints** (from Section 18):
```yaml
/api/v1/backoffice/transactions/{workflowId}/maker-propose
/api/v1/backoffice/transactions/{workflowId}/checker-approve
/api/v1/backoffice/transactions/{workflowId}/checker-reject
```

**Gateway route additions:**
```yaml
- id: orchestrator-backoffice
  uri: lb://orchestrator-service
  predicates:
    - Path=/api/v1/backoffice/transactions/**
  filters:
    - RewritePath=/api/v1/backoffice/transactions/(?<segment>.*), /api/v1/backoffice/transactions/${segment}
```

---

## 24. Review Notes from Human Partner (resolved)

The following review notes from the human partner have been addressed in this revision:

| # | Review Note | Resolution |
|---|-------------|------------|
| 1 | Backoffice UI for Force Resolve actions (FR-17/FR-18) | **Resolved** — Section 18: Full Transaction Resolution Dashboard with Maker-Checker flow, Four-Eyes enforcement, new `transaction_resolution_case` table, and React page spec |
| 2 | Missing transaction types (Prepaid Top-up, eWallet, Merchant) | **Resolved** — Section 19: Complete inventory of 10 additional types with extension pattern; marked out of scope for current phase |
| 3 | Update openapi.yaml and API Gateway | **Resolved** — Section 23: Documents what's done and what's still needed (backoffice endpoints) |
| 4 | Cross-service dependency analysis | **Resolved** — Section 20: 9 endpoint mismatches identified, 4 new endpoints specified, missing config parameters documented |
| 5 | Conditional STP handling | **Resolved** — Section 21: `EvaluateStpActivity` added to workflows, `PENDING_REVIEW` status, BDD compliance matrix |
| 6 | E2E integration tests | **Resolved** — Section 22: Gateway E2E, cross-service, backoffice Maker-Checker, and Temporal replay test plans |