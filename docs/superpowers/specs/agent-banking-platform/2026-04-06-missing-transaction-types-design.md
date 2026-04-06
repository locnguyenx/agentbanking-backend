# Design Addendum — Missing Transaction Types (Phase 3+)
## Agent Banking Platform (Malaysia)

**Version:** 1.0
**Date:** 2026-04-06
**Status:** Draft — Pending Review
**Master Design:** `2026-04-05-transaction-orchestrator-temporal-design.md`
**BRD Reference:** `2026-03-25-agent-banking-platform-brd.md` (FR-8, FR-10, FR-11, FR-15)
**BDD Reference:** `2026-04-06-missing-transaction-types-bdd-addendum.md`

---

## 1. Overview

This addendum specifies 9 missing transaction types organized by implementation complexity. Each type follows the extension pattern defined in the master design (Section 19.2) and inherits common patterns (error handling, STP evaluation, idempotency, observability).

### 1.1 Transaction Type Inventory

| Group | Transaction Type | FR Reference | BDD Scenarios | Complexity |
|-------|-----------------|-------------|---------------|------------|
| **Simple** | CASHLESS_PAYMENT | FR-11.1 | BDD-WF-HP-CP01, BDD-WF-EC-CP01, BDD-WF-EC-CP02 | Low — card-funded, no float ops |
| **Simple** | PIN_BASED_PURCHASE | FR-11.2 | BDD-WF-HP-PBP01, BDD-WF-EC-PBP01 | Low — card-funded, no float ops |
| **Medium** | PREPAID_TOPUP | FR-8 | BDD-T01, T01-CARD, T02, T02-EC-01 | Medium — BlockFloat → Telco API → CommitFloat |
| **Medium** | EWALLET_WITHDRAWAL | FR-10.1 | BDD-WAL-01, WAL-01-CARD, WAL-01-EC-01 | Medium — BlockFloat → eWallet API → CommitFloat |
| **Medium** | EWALLET_TOPUP | FR-10.2 | BDD-WAL-02, WAL-02-CARD | Medium — BlockFloat → eWallet API → CommitFloat |
| **Medium** | ESSP_PURCHASE | FR-10.3 | BDD-ESSP-01, ESSP-01-CARD, ESSP-01-EC-01 | Medium — BlockFloat → BSN API → CommitFloat |
| **Medium** | PIN_PURCHASE | FR-15.2 | BDD-M02, M02-CARD, M02-EC-01 | Medium — BlockFloat → PIN Generation → CommitFloat |
| **Complex** | RETAIL_SALE | FR-15.1 | BDD-M01, M01-QR, M01-RTP, M01-EC-01 | High — multiple payment methods, MDR calculation, float credit |
| **Complex** | HYBRID_CASHBACK | FR-15.5 | BDD-M03, M03-EC-01, M03-NET | High — split accounting, sale + withdrawal |

### 1.2 Shared Inheritance

All workflows inherit from the master design:
- **STP Evaluation:** `EvaluateStpActivity` as step 1.5 (Section 21 of master design)
- **Idempotency:** Dual-layer (Temporal + Redis) per Section 10
- **Error Handling:** Typed exception hierarchy per Section 6
- **Observability:** Logging, metrics, Temporal UI per Section 14
- **Human-in-the-Loop:** Force-resolve signals per Section 11
- **Hexagonal Architecture:** Zero framework imports in `domain/` per Section 16.3

---

## 2. Simple Group — Card-Funded Transactions

These transactions are card-funded (customer pays via card), requiring no agent float operations. The agent acts as a payment facilitator.

### 2.1 CASHLESS_PAYMENT Workflow

**Purpose:** Process card-based payments where the agent processes payment on behalf of the customer without handling cash (FR-11.1). Examples: utility bill payments via card, government fee payments, insurance premiums.

**Key Difference from Withdrawal:** No float operations — the customer's card is debited directly, and the merchant/biller receives payment via the switch. The agent earns commission but does not touch float.

**Input:**
```java
record CashlessPaymentWorkflowInput(
    UUID agentId,
    String pan,
    String pinBlock,
    BigDecimal amount,
    String idempotencyKey,
    String customerCardMasked,
    String merchantCategoryCode,  // MCC for transaction classification
    BigDecimal geofenceLat,
    BigDecimal geofenceLng,
    String customerMykad
) {}
```

**Execution Flow:**
```
1. Activity: CheckVelocity(agentId, amount, customerMykad)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On failure: return FAILED immediately

2. Activity: EvaluateStp(CASHLESS_PAYMENT, agentId, amount, customerProfile)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On NON_STP or CONDITIONAL_STP (not approved): create review case, return PENDING_REVIEW

3. Activity: CalculateFees("CASHLESS_PAYMENT", agentTier, amount)
   → StartToClose: 3s | Retry: 3x (1s→2s→4s)
   → Returns: customerFee, agentCommission, bankShare

4. Activity: AuthorizeAtSwitch(pan, pinBlock, amount, internalTxnId)
   → StartToClose: 25s | Retry: 0x (financial)
   → On APPROVED: continue to step 5
   → On DECLINED: return FAILED (no compensation — no float blocked)
   → On TIMEOUT: SafetyReversal, return FAILED

5. Activity: PublishKafkaEvent(transactionDetails)
   → StartToClose: 3s | Retry: 3x (1s→2s→4s)
   → On failure: log and continue (non-critical)

6. Activity: CacheIdempotencyResponse(idempotencyKey, response)
   → StartToClose: 2s | Retry: 3x (1s→2s→4s)

7. Return WorkflowResult(COMPLETED, transactionId, referenceNumber)
```

**Safety Reversal (on switch timeout at step 4):**
```
1. Activity: SendReversalToSwitch(internalTxnId)
   → StartToClose: 10s | ScheduleToClose: 60s
   → Retry: Infinite, 60s interval (Store & Forward)
```

**Compensation:** None required — no float operations. Only reversal on switch timeout.

**BDD Scenarios:** See `2026-04-06-missing-transaction-types-bdd-addendum.md` Section 2 (BDD-WF-CP).

### 2.2 PIN_BASED_PURCHASE Workflow

**Purpose:** Process PIN-based purchases where the customer enters PIN on POS terminal for goods/services payment (FR-11.2). Similar to CASHLESS_PAYMENT but specifically for retail purchases with PIN authentication.

**Key Difference from CASHLESS_PAYMENT:** Same flow, but different merchant category codes and fee structures. PIN-based purchases typically have lower MDR rates than cashless payments.

**Input:**
```java
record PinBasedPurchaseWorkflowInput(
    UUID agentId,
    String pan,
    String pinBlock,
    BigDecimal amount,
    String idempotencyKey,
    String customerCardMasked,
    String merchantCategoryCode,
    BigDecimal geofenceLat,
    BigDecimal geofenceLng,
    String customerMykad
) {}
```

**Execution Flow:** Identical to CASHLESS_PAYMENT (Section 2.1), with `PIN_BASED_PURCHASE` as the transaction type for fee calculation and routing.

**BDD Scenarios:** See `2026-04-06-missing-transaction-types-bdd-addendum.md` Section 3 (BDD-WF-PBP).

---

## 3. Medium Group — BlockFloat → External API → CommitFloat

These transactions follow the standard SAGA pattern: block float, call external system, commit float on success, release float on failure.

### 3.1 PREPAID_TOPUP Workflow

**Purpose:** Process prepaid mobile top-ups for CELCOM and M1 telcos via cash or card (FR-8, BDD-T01, BDD-T02).

**Key Characteristics:**
- Cash-funded: Agent float decreases by top-up amount (agent pays telco on behalf of customer)
- Card-funded: Customer card debited directly, no float modification (BDD-T01-CARD)
- Requires phone number validation against telco aggregator before processing
- Telco aggregator may timeout or reject invalid numbers

**Input:**
```java
record PrepaidTopupWorkflowInput(
    UUID agentId,
    String telcoProvider,      // "CELCOM" or "M1"
    String phoneNumber,
    BigDecimal amount,
    String idempotencyKey,
    String customerMykad,
    BigDecimal geofenceLat,
    BigDecimal geofenceLng,
    boolean isCardFunded       // true if customer pays via card
) {}
```

**Execution Flow (Cash-Funded):**
```
1. Activity: CheckVelocity(agentId, amount, customerMykad)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)

2. Activity: EvaluateStp(PREPAID_TOPUP, agentId, amount, customerProfile)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On NON_STP or CONDITIONAL_STP (not approved): create review case, return PENDING_REVIEW

3. Activity: CalculateFees("PREPAID_TOPUP", agentTier, amount)
   → StartToClose: 3s | Retry: 3x (1s→2s→4s)

4. Activity: ValidatePhoneNumber(phoneNumber, telcoProvider)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On failure: return FAILED (no compensation — float not blocked)

5. Activity: BlockFloat(agentId, amount + fees, idempotencyKey)
   → StartToClose: 5s | Retry: 0x (financial, idempotent)
   → Compensation: ReleaseFloat(agentId, amount + fees)

6. Activity: TopUpTelco(telcoProvider, phoneNumber, amount, idempotencyKey)
   → StartToClose: 30s | Retry: 0x (financial)
   → On SUCCESS: continue to step 7
   → On FAILURE (invalid number, insufficient telco balance): ReleaseFloat, return FAILED
   → On TIMEOUT: ReleaseFloat, return FAILED (no reversal — telco will auto-reject if not received)

7. Activity: CommitFloat(agentId, amount, internalTxnId)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On failure: ReleaseFloat, return FAILED

8. Activity: PublishKafkaEvent(transactionDetails)
9. Return WorkflowResult(COMPLETED, transactionId, telcoReference)
```

**Execution Flow (Card-Funded):**
```
1. Activity: CheckVelocity(...)
2. Activity: EvaluateStp(...)
3. Activity: CalculateFees(...)
4. Activity: ValidatePhoneNumber(phoneNumber, telcoProvider)
5. Activity: AuthorizeAtSwitch(pan, pinBlock, amount, internalTxnId)
   → On APPROVED: continue to step 6
   → On DECLINED: return FAILED
6. Activity: TopUpTelco(telcoProvider, phoneNumber, amount, idempotencyKey)
   → On SUCCESS: continue to step 7
   → On FAILURE: SendReversalToSwitch, return FAILED
7. Activity: PublishKafkaEvent(...)
8. Return WorkflowResult(COMPLETED, ...)
```

**Compensation Chain:**
```
ReleaseFloat(agentId, amount + fees)
  → StartToClose: 5s | Retry: 3x (1s→2s→4s)
  → MUST succeed — if fails, workflow stays in COMPENSATING state
```

**New Activities:**
| Activity | Input | Output | Exceptions |
|----------|-------|--------|------------|
| ValidatePhoneNumberActivity | phoneNumber, telcoProvider | valid, operatorName | InvalidPhoneNumberException, TelcoUnavailableException |
| TopUpTelcoActivity | telcoProvider, phoneNumber, amount, idempotencyKey | success, telcoReference | TelcoTopupFailedException, TelcoTimeoutException |

**New Port Interfaces:**
```java
// domain/port/out/TelcoAggregatorPort.java
public interface TelcoAggregatorPort {
    TelcoValidationResult validatePhoneNumber(String phoneNumber, String telcoProvider);
    TelcoTopupResult processTopup(String telcoProvider, String phoneNumber, BigDecimal amount, String idempotencyKey);
}

record TelcoValidationResult(boolean valid, String operatorName, String errorCode) {}
record TelcoTopupResult(boolean success, String telcoReference, String errorCode) {}
```

**BDD Scenarios:** See `2026-04-06-missing-transaction-types-bdd-addendum.md` Section 4 (BDD-WF-PT).

### 3.2 EWALLET_WITHDRAWAL Workflow

**Purpose:** Process eWallet withdrawals where the agent gives physical cash to the customer and debits their eWallet (Sarawak Pay) (FR-10.1, BDD-WAL-01).

**Key Characteristics:**
- Cash-funded: Agent float increases (customer pays agent cash, agent's eWallet is debited)
- Card-funded: Customer card debited directly, no float modification (BDD-WAL-01-CARD)
- Requires eWallet validation before processing
- eWallet may have insufficient balance

**Input:**
```java
record EWalletWithdrawalWorkflowInput(
    UUID agentId,
    String ewalletProvider,    // "SARAWAK_PAY"
    String ewalletId,          // customer's eWallet ID
    BigDecimal amount,
    String idempotencyKey,
    String customerMykad,
    BigDecimal geofenceLat,
    BigDecimal geofenceLng,
    boolean isCardFunded
) {}
```

**Execution Flow (Cash-Funded):**
```
1. Activity: CheckVelocity(agentId, amount, customerMykad)
2. Activity: EvaluateStp(EWALLET_WITHDRAWAL, agentId, amount, customerProfile)
3. Activity: CalculateFees("EWALLET_WITHDRAWAL", agentTier, amount)
4. Activity: ValidateEWallet(ewalletProvider, ewalletId)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On failure: return FAILED
5. Activity: BlockFloat(agentId, amount + fees, idempotencyKey)
   → Compensation: ReleaseFloat
6. Activity: WithdrawFromEWallet(ewalletProvider, ewalletId, amount, idempotencyKey)
   → StartToClose: 15s | Retry: 0x (financial)
   → On SUCCESS: continue to step 7
   → On FAILURE (insufficient eWallet balance): ReleaseFloat, return FAILED
   → On TIMEOUT: ReleaseFloat, return FAILED
7. Activity: CommitFloat(agentId, amount, internalTxnId)
8. Activity: PublishKafkaEvent(...)
9. Return WorkflowResult(COMPLETED, transactionId, ewalletReference)
```

**Execution Flow (Card-Funded):**
```
1. Activity: CheckVelocity(...)
2. Activity: EvaluateStp(...)
3. Activity: CalculateFees(...)
4. Activity: ValidateEWallet(ewalletProvider, ewalletId)
5. Activity: AuthorizeAtSwitch(pan, pinBlock, amount, internalTxnId)
6. Activity: WithdrawFromEWallet(ewalletProvider, ewalletId, amount, idempotencyKey)
   → On FAILURE: SendReversalToSwitch, return FAILED
7. Activity: PublishKafkaEvent(...)
8. Return WorkflowResult(COMPLETED, ...)
```

**New Activities:**
| Activity | Input | Output | Exceptions |
|----------|-------|--------|------------|
| ValidateEWalletActivity | ewalletProvider, ewalletId | valid, walletBalance | InvalidEWalletException, EWalletUnavailableException |
| WithdrawFromEWalletActivity | ewalletProvider, ewalletId, amount, idempotencyKey | success, ewalletReference | EWalletInsufficientException, EWalletWithdrawFailedException |

**New Port Interfaces:**
```java
// domain/port/out/EWalletProviderPort.java
public interface EWalletProviderPort {
    EWalletValidationResult validateWallet(String provider, String walletId);
    EWalletWithdrawResult withdraw(String provider, String walletId, BigDecimal amount, String idempotencyKey);
}

record EWalletValidationResult(boolean valid, BigDecimal walletBalance, String errorCode) {}
record EWalletWithdrawResult(boolean success, String ewalletReference, String errorCode) {}
```

**BDD Scenarios:** See `2026-04-06-missing-transaction-types-bdd-addendum.md` Section 5 (BDD-WF-EWW).

### 3.3 EWALLET_TOPUP Workflow

**Purpose:** Process eWallet top-ups where the agent receives cash from the customer and credits their eWallet (Sarawak Pay) (FR-10.2, BDD-WAL-02).

**Key Characteristics:**
- Cash-funded: Agent float decreases (agent receives cash, credits customer's eWallet)
- Card-funded: Customer card debited directly, no float modification (BDD-WAL-02-CARD)
- Similar to PREPAID_TOPUP but with eWallet provider instead of telco

**Input:**
```java
record EWalletTopupWorkflowInput(
    UUID agentId,
    String ewalletProvider,
    String ewalletId,
    BigDecimal amount,
    String idempotencyKey,
    String customerMykad,
    BigDecimal geofenceLat,
    BigDecimal geofenceLng,
    boolean isCardFunded
) {}
```

**Execution Flow (Cash-Funded):**
```
1. Activity: CheckVelocity(agentId, amount, customerMykad)
2. Activity: EvaluateStp(EWALLET_TOPUP, agentId, amount, customerProfile)
3. Activity: CalculateFees("EWALLET_TOPUP", agentTier, amount)
4. Activity: ValidateEWallet(ewalletProvider, ewalletId)
5. Activity: BlockFloat(agentId, amount + fees, idempotencyKey)
   → Compensation: ReleaseFloat
6. Activity: TopUpEWallet(ewalletProvider, ewalletId, amount, idempotencyKey)
   → StartToClose: 15s | Retry: 0x (financial)
   → On SUCCESS: continue to step 7
   → On FAILURE: ReleaseFloat, return FAILED
7. Activity: CommitFloat(agentId, amount, internalTxnId)
8. Activity: PublishKafkaEvent(...)
9. Return WorkflowResult(COMPLETED, transactionId, ewalletReference)
```

**Execution Flow (Card-Funded):**
```
1. Activity: CheckVelocity(...)
2. Activity: EvaluateStp(...)
3. Activity: CalculateFees(...)
4. Activity: ValidateEWallet(ewalletProvider, ewalletId)
5. Activity: AuthorizeAtSwitch(pan, pinBlock, amount, internalTxnId)
6. Activity: TopUpEWallet(ewalletProvider, ewalletId, amount, idempotencyKey)
   → On FAILURE: SendReversalToSwitch, return FAILED
7. Activity: PublishKafkaEvent(...)
8. Return WorkflowResult(COMPLETED, ...)
```

**New Activities:**
| Activity | Input | Output | Exceptions |
|----------|-------|--------|------------|
| TopUpEWalletActivity | ewalletProvider, ewalletId, amount, idempotencyKey | success, ewalletReference | EWalletTopupFailedException, EWalletTimeoutException |

**BDD Scenarios:** See `2026-04-06-missing-transaction-types-bdd-addendum.md` Section 6 (BDD-WF-EWT).

### 3.4 ESSP_PURCHASE Workflow

**Purpose:** Process BSN eSSP (Sijil Simpanan Premium) certificate purchases where the agent collects cash and routes to BSN (FR-10.3, BDD-ESSP-01).

**Key Characteristics:**
- Cash-funded: Agent float decreases (agent receives cash, pays BSN)
- Card-funded: Customer card debited directly, no float modification (BDD-ESSP-01-CARD)
- BSN system may be unavailable (BDD-ESSP-01-EC-01)
- Generates receipt with eSSP certificate details

**Input:**
```java
record ESSPPurchaseWorkflowInput(
    UUID agentId,
    BigDecimal amount,
    String idempotencyKey,
    String customerMykad,
    BigDecimal geofenceLat,
    BigDecimal geofenceLng,
    boolean isCardFunded
) {}
```

**Execution Flow (Cash-Funded):**
```
1. Activity: CheckVelocity(agentId, amount, customerMykad)
2. Activity: EvaluateStp(ESSP_PURCHASE, agentId, amount, customerProfile)
3. Activity: CalculateFees("ESSP_PURCHASE", agentTier, amount)
4. Activity: ValidateESSPPurchase(amount)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → Validates amount against BSN minimum/maximum limits
   → On failure: return FAILED
5. Activity: BlockFloat(agentId, amount + fees, idempotencyKey)
   → Compensation: ReleaseFloat
6. Activity: PurchaseESSP(amount, customerMykad, idempotencyKey)
   → StartToClose: 20s | Retry: 0x (financial)
   → On SUCCESS: continue to step 7
   → On FAILURE (BSN unavailable): ReleaseFloat, return FAILED
   → On TIMEOUT: ReleaseFloat, return FAILED
7. Activity: CommitFloat(agentId, amount, internalTxnId)
8. Activity: PublishKafkaEvent(transactionDetails)
9. Return WorkflowResult(COMPLETED, transactionId, esspCertificateNumber)
```

**Execution Flow (Card-Funded):**
```
1. Activity: CheckVelocity(...)
2. Activity: EvaluateStp(...)
3. Activity: CalculateFees(...)
4. Activity: ValidateESSPPurchase(amount)
5. Activity: AuthorizeAtSwitch(pan, pinBlock, amount, internalTxnId)
6. Activity: PurchaseESSP(amount, customerMykad, idempotencyKey)
   → On FAILURE: SendReversalToSwitch, return FAILED
7. Activity: PublishKafkaEvent(...)
8. Return WorkflowResult(COMPLETED, esspCertificateNumber)
```

**New Activities:**
| Activity | Input | Output | Exceptions |
|----------|-------|--------|------------|
| ValidateESSPPurchaseActivity | amount | valid, minAmount, maxAmount | ESSPInvalidAmountException |
| PurchaseESSPActivity | amount, customerMykad, idempotencyKey | success, certificateNumber | ESSPServiceUnavailableException, ESSPPurchaseFailedException |

**New Port Interfaces:**
```java
// domain/port/out/ESSPServicePort.java
public interface ESSPServicePort {
    ESSPValidationResult validatePurchase(BigDecimal amount);
    ESSPPurchaseResult purchase(BigDecimal amount, String customerMykad, String idempotencyKey);
}

record ESSPValidationResult(boolean valid, BigDecimal minAmount, BigDecimal maxAmount, String errorCode) {}
record ESSPPurchaseResult(boolean success, String certificateNumber, String errorCode) {}
```

**BDD Scenarios:** See `2026-04-06-missing-transaction-types-bdd-addendum.md` Section 7 (BDD-WF-ESSP).

### 3.5 PIN_PURCHASE Workflow

**Purpose:** Process digital PIN voucher purchases where the agent pays from float and receives physical cash from customer (FR-15.2, BDD-M02).

**Key Characteristics:**
- Cash-funded: Agent float decreases by PIN face value (agent receives cash, generates PIN)
- Card-funded: Customer card debited directly, agent float decreases (BDD-M02-CARD)
- Requires PIN inventory management (BDD-M02-EC-01: inventory depleted)
- Agent earns commission per PIN sold
- Generates 16-digit PIN code printed on receipt slip

**Input:**
```java
record PINPurchaseWorkflowInput(
    UUID agentId,
    String pinProvider,        // "DIGI", "MAXIS", "CELCOM", etc.
    BigDecimal faceValue,
    BigDecimal amount,         // faceValue + fee
    String idempotencyKey,
    String customerMykad,
    BigDecimal geofenceLat,
    BigDecimal geofenceLng,
    boolean isCardFunded
) {}
```

**Execution Flow (Cash-Funded):**
```
1. Activity: CheckVelocity(agentId, amount, customerMykad)
2. Activity: EvaluateStp(PIN_PURCHASE, agentId, amount, customerProfile)
3. Activity: CalculateFees("PIN_PURCHASE", agentTier, faceValue)
   → Returns: customerFee, agentCommission, bankShare
4. Activity: ValidatePINInventory(pinProvider, faceValue)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On failure (inventory depleted): return FAILED with ERR_PIN_INVENTORY_DEPLETED
5. Activity: BlockFloat(agentId, faceValue, idempotencyKey)
   → Compensation: ReleaseFloat
6. Activity: GeneratePIN(pinProvider, faceValue, idempotencyKey)
   → StartToClose: 10s | Retry: 0x (financial)
   → Returns: 16-digit PIN code, serial number, expiry date
   → On SUCCESS: continue to step 7
   → On FAILURE (inventory depleted): ReleaseFloat, return FAILED
7. Activity: CommitFloat(agentId, faceValue, internalTxnId)
8. Activity: PublishKafkaEvent(transactionDetails)
9. Return WorkflowResult(COMPLETED, transactionId, pinCode, serialNumber)
```

**Execution Flow (Card-Funded):**
```
1. Activity: CheckVelocity(...)
2. Activity: EvaluateStp(...)
3. Activity: CalculateFees(...)
4. Activity: ValidatePINInventory(pinProvider, faceValue)
5. Activity: AuthorizeAtSwitch(pan, pinBlock, amount, internalTxnId)
6. Activity: GeneratePIN(pinProvider, faceValue, idempotencyKey)
   → On FAILURE: SendReversalToSwitch, return FAILED
7. Activity: PublishKafkaEvent(...)
8. Return WorkflowResult(COMPLETED, pinCode, serialNumber)
```

**New Activities:**
| Activity | Input | Output | Exceptions |
|----------|-------|--------|------------|
| ValidatePINInventoryActivity | pinProvider, faceValue | available, stockCount | PINInventoryDepletedException, PINProviderUnavailableException |
| GeneratePINActivity | pinProvider, faceValue, idempotencyKey | success, pinCode, serialNumber, expiryDate | PINGenerationFailedException, PINInventoryDepletedException |

**New Port Interfaces:**
```java
// domain/port/out/PINInventoryPort.java
public interface PINInventoryPort {
    PINInventoryResult validateInventory(String provider, BigDecimal faceValue);
    PINGenerationResult generatePIN(String provider, BigDecimal faceValue, String idempotencyKey);
}

record PINInventoryResult(boolean available, int stockCount, String errorCode) {}
record PINGenerationResult(boolean success, String pinCode, String serialNumber, LocalDate expiryDate, String errorCode) {}
```

**BDD Scenarios:** See `2026-04-06-missing-transaction-types-bdd-addendum.md` Section 8 (BDD-WF-PIN).

---

## 4. Complex Group — Multi-Step, Multi-Payment Workflows

These transactions involve multiple payment methods, split accounting, or complex float operations.

### 4.1 RETAIL_SALE Workflow

**Purpose:** Process retail card/QR purchases where the agent acts as merchant, crediting agent float instantly minus MDR fee (FR-15.1, BDD-M01).

**Key Characteristics:**
- **Multiple Payment Methods:**
  - Card-based (debit card + PIN) via switch
  - DuitNow QR via PayNet
  - DuitNow Request-to-Pay via PayNet
- **Float Operation:** Agent float INCREASES by (saleAmount - MDR)
- **MDR Calculation:** Variable rate based on payment method and merchant category
- **No BlockFloat:** Payment is received first, then float is credited
- **MerchantTransaction Record:** Created alongside Transaction for MDR tracking

**Input:**
```java
record RetailSaleWorkflowInput(
    UUID agentId,
    String paymentMethod,      // "CARD", "DUITNOW_QR", "DUITNOW_RTP"
    String pan,                // for CARD
    String pinBlock,           // for CARD
    String qrReference,        // for DUITNOW_QR
    String rtpProxy,           // for DUITNOW_RTP (mobile number)
    BigDecimal amount,
    String idempotencyKey,
    String customerMykad,
    BigDecimal geofenceLat,
    BigDecimal geofenceLng
) {}
```

**Execution Flow (Card-Based):**
```
1. Activity: CheckVelocity(agentId, amount, customerMykad)
2. Activity: EvaluateStp(RETAIL_SALE, agentId, amount, customerProfile)
3. Activity: CalculateMDR("RETAIL_SALE", agentTier, amount, "CARD")
   → StartToClose: 3s | Retry: 3x (1s→2s→4s)
   → Returns: mdrRate, mdrAmount, netCreditToFloat
4. Activity: AuthorizeAtSwitch(pan, pinBlock, amount, internalTxnId)
   → StartToClose: 25s | Retry: 0x (financial)
   → On APPROVED: continue to step 5
   → On DECLINED: return FAILED (no compensation — no float blocked)
   → On TIMEOUT: SafetyReversal, return FAILED
5. Activity: CreditAgentFloat(agentId, netCreditToFloat)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On failure: ReverseCreditFloat, return FAILED
6. Activity: CreateMerchantTransactionRecord(...)
   → StartToClose: 3s | Retry: 3x (1s→2s→4s)
   → Records: grossAmount, mdrRate, mdrAmount, netCreditToFloat, receiptType
7. Activity: PublishKafkaEvent(transactionDetails)
8. Return WorkflowResult(COMPLETED, transactionId, referenceNumber)
```

**Execution Flow (DuitNow QR):**
```
1. Activity: CheckVelocity(...)
2. Activity: EvaluateStp(...)
3. Activity: CalculateMDR("RETAIL_SALE", agentTier, amount, "DUITNOW_QR")
4. Activity: GenerateDynamicQR(amount, agentId, idempotencyKey)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → Returns: qrCode, qrReference
5. Activity: WaitForQRPayment(qrReference)
   → StartToClose: 120s | Retry: 0x (waiting for customer)
   → Polls PayNet for payment status every 2 seconds
   → On PAYMENT_RECEIVED: continue to step 6
   → On TIMEOUT: return FAILED (no compensation — no float blocked)
6. Activity: CreditAgentFloat(agentId, netCreditToFloat)
7. Activity: CreateMerchantTransactionRecord(...)
8. Activity: PublishKafkaEvent(...)
9. Return WorkflowResult(COMPLETED, transactionId, paynetReference)
```

**Execution Flow (DuitNow Request-to-Pay):**
```
1. Activity: CheckVelocity(...)
2. Activity: EvaluateStp(...)
3. Activity: CalculateMDR("RETAIL_SALE", agentTier, amount, "DUITNOW_RTP")
4. Activity: SendRequestToPay(rtpProxy, amount, idempotencyKey)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → Sends RTP to customer's bank via PayNet
5. Activity: WaitForRTPApproval(rtpReference)
   → StartToClose: 300s | Retry: 0x (waiting for customer)
   → Polls PayNet for approval status every 2 seconds
   → On APPROVED: continue to step 6
   → On DECLINED: return FAILED
   → On TIMEOUT: return FAILED
6. Activity: CreditAgentFloat(agentId, netCreditToFloat)
7. Activity: CreateMerchantTransactionRecord(...)
8. Activity: PublishKafkaEvent(...)
9. Return WorkflowResult(COMPLETED, transactionId, paynetReference)
```

**New Activities:**
| Activity | Input | Output | Exceptions |
|----------|-------|--------|------------|
| CalculateMDRActivity | txnType, tier, amount, paymentMethod | mdrRate, mdrAmount, netCreditToFloat | MDRConfigNotFoundException |
| GenerateDynamicQRActivity | amount, agentId, idempotencyKey | qrCode, qrReference | QRGenerationFailedException |
| WaitForQRPaymentActivity | qrReference | success, paynetReference | QRPaymentTimeoutException, QRPaymentFailedException |
| SendRequestToPayActivity | rtpProxy, amount, idempotencyKey | success, rtpReference | RTPSendFailedException, RTPInvalidProxyException |
| WaitForRTPApprovalActivity | rtpReference | success, paynetReference | RTPApprovalTimeoutException, RTPDeclinedException |
| CreateMerchantTransactionRecordActivity | txnId, grossAmount, mdrRate, mdrAmount, netCredit | success | RecordCreationFailedException |

**New Port Interfaces:**
```java
// domain/port/out/QRPaymentPort.java
public interface QRPaymentPort {
    QRGenerationResult generateDynamicQR(BigDecimal amount, UUID agentId, String idempotencyKey);
    QRPaymentStatus checkPaymentStatus(String qrReference);
}

record QRGenerationResult(String qrCode, String qrReference, String errorCode) {}

// domain/port/out/RequestToPayPort.java
public interface RequestToPayPort {
    RTPResult sendRequestToPay(String proxy, BigDecimal amount, String idempotencyKey);
    RTPStatus checkRTPStatus(String rtpReference);
}

record RTPResult(boolean success, String rtpReference, String errorCode) {}

// domain/port/out/MerchantTransactionPort.java
public interface MerchantTransactionPort {
    MerchantTransactionResult createRecord(MerchantTransactionRecord record);
}

record MerchantTransactionRecord(
    UUID transactionId,
    String merchantType,
    BigDecimal grossAmount,
    BigDecimal mdrRate,
    BigDecimal mdrAmount,
    BigDecimal netCreditToFloat,
    String receiptType
) {}

record MerchantTransactionResult(boolean success, UUID recordId, String errorCode) {}
```

**BDD Scenarios:** See `2026-04-06-missing-transaction-types-bdd-addendum.md` Section 9 (BDD-WF-RS).

### 4.2 HYBRID_CASHBACK Workflow

**Purpose:** Process hybrid cash-back transactions combining product purchase and cash withdrawal in a single card swipe (FR-15.5, BDD-M03).

**Key Characteristics:**
- **Split Accounting:** Single transaction with two components:
  - Sale portion: Customer pays for goods, agent float credited (after MDR)
  - Cashback portion: Agent gives physical cash, agent float credited (full amount)
- **Single Authorization:** One card authorization for total amount (sale + cashback)
- **Float Check:** Agent must have sufficient float capacity for cashback portion
- **Complex Compensation:** Reversal affects both sale and cashback portions

**Input:**
```java
record HybridCashbackWorkflowInput(
    UUID agentId,
    String pan,
    String pinBlock,
    BigDecimal saleAmount,
    BigDecimal cashbackAmount,
    BigDecimal totalAmount,      // saleAmount + cashbackAmount
    String idempotencyKey,
    String customerCardMasked,
    String merchantCategoryCode,
    BigDecimal geofenceLat,
    BigDecimal geofenceLng,
    String customerMykad
) {}
```

**Execution Flow:**
```
1. Activity: CheckVelocity(agentId, totalAmount, customerMykad)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)

2. Activity: EvaluateStp(HYBRID_CASHBACK, agentId, totalAmount, customerProfile)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → On NON_STP or CONDITIONAL_STP (not approved): create review case, return PENDING_REVIEW

3. Activity: CalculateMDR("RETAIL_SALE", agentTier, saleAmount, "CARD")
   → StartToClose: 3s | Retry: 3x (1s→2s→4s)
   → Returns: mdrRate, mdrAmount, netSaleCredit

4. Activity: ValidateFloatCapacity(agentId, cashbackAmount)
   → StartToClose: 3s | Retry: 3x (1s→2s→4s)
   → Ensures agent can handle cashback (float cap check)
   → On failure: return FAILED with ERR_INSUFFICIENT_FLOAT_CAPACITY

5. Activity: AuthorizeAtSwitch(pan, pinBlock, totalAmount, internalTxnId)
   → StartToClose: 25s | Retry: 0x (financial)
   → On APPROVED: continue to step 6
   → On DECLINED: return FAILED (no compensation — no float blocked)
   → On TIMEOUT: SafetyReversal, return FAILED

6. Activity: CreditAgentFloat(agentId, netSaleCredit)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → Credits agent for sale portion (after MDR)
   → Compensation: ReverseCreditFloat(agentId, netSaleCredit)

7. Activity: CreditAgentFloat(agentId, cashbackAmount)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
   → Credits agent for cashback portion (full amount)
   → Compensation: ReverseCreditFloat(agentId, cashbackAmount)

8. Activity: CreateMerchantTransactionRecord(...)
   → Records: grossAmount=totalAmount, saleAmount, cashbackAmount, mdrRate, mdrAmount, netCreditToFloat
   → StartToClose: 3s | Retry: 3x (1s→2s→4s)

9. Activity: PublishKafkaEvent(transactionDetails)
10. Return WorkflowResult(COMPLETED, transactionId, referenceNumber)
```

**Compensation Chain (on failure after step 5):**
```
1. ReverseCreditFloat(agentId, cashbackAmount)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
2. ReverseCreditFloat(agentId, netSaleCredit)
   → StartToClose: 5s | Retry: 3x (1s→2s→4s)
3. SendReversalToSwitch(internalTxnId)
   → StartToClose: 10s | ScheduleToClose: 60s
   → Retry: Infinite, 60s interval (Store & Forward)
```

**Safety Reversal (on switch timeout at step 5):**
```
1. Activity: SendReversalToSwitch(internalTxnId)
   → StartToClose: 10s | ScheduleToClose: 60s
   → Retry: Infinite, 60s interval (Store & Forward)
```

**New Activities:**
| Activity | Input | Output | Exceptions |
|----------|-------|--------|------------|
| ValidateFloatCapacityActivity | agentId, cashbackAmount | hasCapacity, availableCapacity | FloatCapacityCheckFailedException |

**BDD Scenarios:** See `2026-04-06-missing-transaction-types-bdd-addendum.md` Section 10 (BDD-WF-HCB).

---

## 5. Shared Infrastructure Updates

### 5.1 TransactionType Enum Extension

```java
// domain/model/TransactionType.java
public enum TransactionType {
    // Existing (MVP + Phase 2)
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    BILL_PAYMENT,
    DUITNOW_TRANSFER,
    
    // New (Phase 3+)
    CASHLESS_PAYMENT,
    PIN_BASED_PURCHASE,
    PREPAID_TOPUP,
    EWALLET_WITHDRAWAL,
    EWALLET_TOPUP,
    ESSP_PURCHASE,
    PIN_PURCHASE,
    RETAIL_SALE,
    HYBRID_CASHBACK
}
```

### 5.2 WorkflowRouter Extension

```java
// domain/service/WorkflowRouter.java
public WorkflowType route(TransactionRequest request) {
    return switch (request.transactionType()) {
        case CASH_WITHDRAWAL -> routeWithdrawal(request);
        case CASH_DEPOSIT -> WorkflowType.DEPOSIT;
        case BILL_PAYMENT -> WorkflowType.BILL_PAYMENT;
        case DUITNOW_TRANSFER -> WorkflowType.DUITNOW_TRANSFER;
        
        // New routes
        case CASHLESS_PAYMENT -> WorkflowType.CASHLESS_PAYMENT;
        case PIN_BASED_PURCHASE -> WorkflowType.PIN_BASED_PURCHASE;
        case PREPAID_TOPUP -> WorkflowType.PREPAID_TOPUP;
        case EWALLET_WITHDRAWAL -> WorkflowType.EWALLET_WITHDRAWAL;
        case EWALLET_TOPUP -> WorkflowType.EWALLET_TOPUP;
        case ESSP_PURCHASE -> WorkflowType.ESSP_PURCHASE;
        case PIN_PURCHASE -> WorkflowType.PIN_PURCHASE;
        case RETAIL_SALE -> WorkflowType.RETAIL_SALE;
        case HYBRID_CASHBACK -> WorkflowType.HYBRID_CASHBACK;
    };
}
```

### 5.3 New Workflow Types

```java
// application/workflow/WorkflowType.java
public enum WorkflowType {
    WITHDRAWAL,
    WITHDRAWAL_ON_US,
    DEPOSIT,
    BILL_PAYMENT,
    DUITNOW_TRANSFER,
    
    // New (Phase 3+)
    CASHLESS_PAYMENT,
    PIN_BASED_PURCHASE,
    PREPAID_TOPUP,
    EWALLET_WITHDRAWAL,
    EWALLET_TOPUP,
    ESSP_PURCHASE,
    PIN_PURCHASE,
    RETAIL_SALE,
    HYBRID_CASHBACK
}
```

### 5.4 New Exception Types

```java
// domain/model/exceptions/
public class InvalidPhoneNumberException extends ExternalServiceException {}
public class TelcoTopupFailedException extends ExternalServiceException {}
public class TelcoTimeoutException extends ExternalServiceException {}
public class TelcoUnavailableException extends ExternalServiceException {}
public class EWalletInsufficientException extends BusinessException {}
public class EWalletWithdrawFailedException extends ExternalServiceException {}
public class EWalletTopupFailedException extends ExternalServiceException {}
public class EWalletTimeoutException extends ExternalServiceException {}
public class EWalletUnavailableException extends ExternalServiceException {}
public class InvalidEWalletException extends ValidationException {}
public class ESSPInvalidAmountException extends ValidationException {}
public class ESSPServiceUnavailableException extends ExternalServiceException {}
public class ESSPPurchaseFailedException extends ExternalServiceException {}
public class PINInventoryDepletedException extends BusinessException {}
public class PINProviderUnavailableException extends ExternalServiceException {}
public class PINGenerationFailedException extends ExternalServiceException {}
public class MDRConfigNotFoundException BusinessException {}
public class QRGenerationFailedException extends ExternalServiceException {}
public class QRPaymentTimeoutException extends ExternalServiceException {}
public class QRPaymentFailedException extends ExternalServiceException {}
public class RTPSendFailedException extends ExternalServiceException {}
public class RTPInvalidProxyException extends ValidationException {}
public class RTPApprovalTimeoutException extends ExternalServiceException {}
public class RTPDeclinedException extends ExternalServiceException {}
public class RecordCreationFailedException extends SystemException {}
public class FloatCapacityCheckFailedException extends BusinessException {}
```

### 5.5 Activity Catalog Extension

| Activity | Input | Output | Exceptions | Compensation |
|----------|-------|--------|------------|--------------|
| ValidatePhoneNumberActivity | phoneNumber, telcoProvider | valid, operatorName | InvalidPhoneNumberException, TelcoUnavailableException | None |
| TopUpTelcoActivity | telcoProvider, phoneNumber, amount, idempotencyKey | success, telcoReference | TelcoTopupFailedException, TelcoTimeoutException | ReleaseFloat |
| ValidateEWalletActivity | ewalletProvider, ewalletId | valid, walletBalance | InvalidEWalletException, EWalletUnavailableException | None |
| WithdrawFromEWalletActivity | ewalletProvider, ewalletId, amount, idempotencyKey | success, ewalletReference | EWalletInsufficientException, EWalletWithdrawFailedException | ReleaseFloat |
| TopUpEWalletActivity | ewalletProvider, ewalletId, amount, idempotencyKey | success, ewalletReference | EWalletTopupFailedException, EWalletTimeoutException | ReleaseFloat |
| ValidateESSPPurchaseActivity | amount | valid, minAmount, maxAmount | ESSPInvalidAmountException | None |
| PurchaseESSPActivity | amount, customerMykad, idempotencyKey | success, certificateNumber | ESSPServiceUnavailableException, ESSPPurchaseFailedException | ReleaseFloat |
| ValidatePINInventoryActivity | pinProvider, faceValue | available, stockCount | PINInventoryDepletedException, PINProviderUnavailableException | None |
| GeneratePINActivity | pinProvider, faceValue, idempotencyKey | success, pinCode, serialNumber | PINGenerationFailedException, PINInventoryDepletedException | ReleaseFloat |
| CalculateMDRActivity | txnType, tier, amount, paymentMethod | mdrRate, mdrAmount, netCredit | MDRConfigNotFoundException | None |
| GenerateDynamicQRActivity | amount, agentId, idempotencyKey | qrCode, qrReference | QRGenerationFailedException | None |
| WaitForQRPaymentActivity | qrReference | success, paynetReference | QRPaymentTimeoutException, QRPaymentFailedException | None |
| SendRequestToPayActivity | rtpProxy, amount, idempotencyKey | success, rtpReference | RTPSendFailedException, RTPInvalidProxyException | None |
| WaitForRTPApprovalActivity | rtpReference | success, paynetReference | RTPApprovalTimeoutException, RTPDeclinedException | None |
| CreateMerchantTransactionRecordActivity | record | success, recordId | RecordCreationFailedException | None |
| ValidateFloatCapacityActivity | agentId, cashbackAmount | hasCapacity, availableCapacity | FloatCapacityCheckFailedException | None |

### 5.6 Cross-Service Dependencies

| Orchestrator Activity | Depends On | Protocol | New Endpoint Required |
|----------------------|------------|----------|----------------------|
| ValidatePhoneNumberActivity | Telco Aggregator Service | Feign (sync) | `POST /internal/validate-phone` |
| TopUpTelcoActivity | Telco Aggregator Service | Feign (sync) | `POST /internal/topup` |
| ValidateEWalletActivity | eWallet Provider Service | Feign (sync) | `POST /internal/validate-wallet` |
| WithdrawFromEWalletActivity | eWallet Provider Service | Feign (sync) | `POST /internal/withdraw` |
| TopUpEWalletActivity | eWallet Provider Service | Feign (sync) | `POST /internal/topup` |
| ValidateESSPPurchaseActivity | BSN eSSP Service | Feign (sync) | `POST /internal/validate-purchase` |
| PurchaseESSPActivity | BSN eSSP Service | Feign (sync) | `POST /internal/purchase` |
| ValidatePINInventoryActivity | PIN Inventory Service | Feign (sync) | `POST /internal/validate-inventory` |
| GeneratePINActivity | PIN Inventory Service | Feign (sync) | `POST /internal/generate-pin` |
| CalculateMDRActivity | Rules Service | Feign (sync) | Existing `/internal/fee-config` (extend) |
| GenerateDynamicQRActivity | Switch Adapter Service | Feign (sync) | `POST /internal/qr/generate` |
| WaitForQRPaymentActivity | Switch Adapter Service | Feign (sync) | `GET /internal/qr/status/{qrReference}` |
| SendRequestToPayActivity | Switch Adapter Service | Feign (sync) | `POST /internal/rtp/send` |
| WaitForRTPApprovalActivity | Switch Adapter Service | Feign (sync) | `GET /internal/rtp/status/{rtpReference}` |
| CreateMerchantTransactionRecordActivity | Ledger Service | Feign (sync) | `POST /internal/merchant-transaction` |
| ValidateFloatCapacityActivity | Ledger Service | Feign (sync) | `GET /internal/float-capacity/{agentId}` |

### 5.7 Service Structure Extension

```
orchestrator-service/
├── domain/
│   ├── model/
│   │   ├── TransactionType.java               # EXTENDED: 9 new types
│   │   ├── WorkflowType.java                  # EXTENDED: 9 new types
│   │   ├── WorkflowResult.java                # unchanged
│   │   ├── WorkflowStatus.java                # unchanged
│   │   ├── ForceResolveSignal.java            # unchanged
│   │   └── exceptions/                        # NEW: 24 new exception types
│   ├── port/out/
│   │   ├── RulesServicePort.java              # EXTENDED: MDR calculation
│   │   ├── LedgerServicePort.java             # EXTENDED: merchant transaction, float capacity
│   │   ├── SwitchAdapterPort.java             # EXTENDED: QR, RTP
│   │   ├── BillerServicePort.java             # unchanged
│   │   ├── CbsServicePort.java                # unchanged
│   │   ├── EventPublisherPort.java            # unchanged
│   │   ├── TelcoAggregatorPort.java           # NEW
│   │   ├── EWalletProviderPort.java           # NEW
│   │   ├── ESSPServicePort.java               # NEW
│   │   ├── PINInventoryPort.java              # NEW
│   │   ├── QRPaymentPort.java                 # NEW
│   │   ├── RequestToPayPort.java              # NEW
│   │   └── MerchantTransactionPort.java       # NEW
│   └── service/
│       ├── WorkflowRouter.java                # EXTENDED: 9 new routes
│       └── CompensationRegistry.java          # EXTENDED: new compensations
├── application/
│   ├── workflow/
│   │   ├── CashlessPaymentWorkflow.java       # NEW
│   │   ├── PinBasedPurchaseWorkflow.java      # NEW
│   │   ├── PrepaidTopupWorkflow.java          # NEW
│   │   ├── EWalletWithdrawalWorkflow.java     # NEW
│   │   ├── EWalletTopupWorkflow.java          # NEW
│   │   ├── ESSPPurchaseWorkflow.java          # NEW
│   │   ├── PINPurchaseWorkflow.java           # NEW
│   │   ├── RetailSaleWorkflow.java            # NEW
│   │   └── HybridCashbackWorkflow.java        # NEW
│   └── activity/
│       ├── ValidatePhoneNumberActivity.java   # NEW
│       ├── TopUpTelcoActivity.java            # NEW
│       ├── ValidateEWalletActivity.java       # NEW
│       ├── WithdrawFromEWalletActivity.java   # NEW
│       ├── TopUpEWalletActivity.java          # NEW
│       ├── ValidateESSPPurchaseActivity.java  # NEW
│       ├── PurchaseESSPActivity.java          # NEW
│       ├── ValidatePINInventoryActivity.java  # NEW
│       ├── GeneratePINActivity.java           # NEW
│       ├── CalculateMDRActivity.java          # NEW
│       ├── GenerateDynamicQRActivity.java     # NEW
│       ├── WaitForQRPaymentActivity.java      # NEW
│       ├── SendRequestToPayActivity.java      # NEW
│       ├── WaitForRTPApprovalActivity.java    # NEW
│       ├── CreateMerchantTransactionRecordActivity.java  # NEW
│       └── ValidateFloatCapacityActivity.java # NEW
├── infrastructure/
│   ├── temporal/
│   │   ├── WorkflowImpl/
│   │   │   ├── CashlessPaymentWorkflowImpl.java       # NEW
│   │   │   ├── PinBasedPurchaseWorkflowImpl.java      # NEW
│   │   │   ├── PrepaidTopupWorkflowImpl.java          # NEW
│   │   │   ├── EWalletWithdrawalWorkflowImpl.java     # NEW
│   │   │   ├── EWalletTopupWorkflowImpl.java          # NEW
│   │   │   ├── ESSPPurchaseWorkflowImpl.java          # NEW
│   │   │   ├── PINPurchaseWorkflowImpl.java           # NEW
│   │   │   ├── RetailSaleWorkflowImpl.java            # NEW
│   │   │   └── HybridCashbackWorkflowImpl.java        # NEW
│   │   └── ActivityImpl/
│   │       └── (all 16 new activity implementations)  # NEW
│   └── external/
│       ├── TelcoAggregatorClient.java         # NEW @FeignClient
│       ├── EWalletProviderClient.java         # NEW @FeignClient
│       ├── ESSPServiceClient.java             # NEW @FeignClient
│       ├── PINInventoryClient.java            # NEW @FeignClient
│       └── adapters/
│           ├── TelcoAggregatorPortAdapter.java        # NEW
│           ├── EWalletProviderPortAdapter.java        # NEW
│           ├── ESSPServicePortAdapter.java            # NEW
│           ├── PINInventoryPortAdapter.java           # NEW
│           ├── QRPaymentPortAdapter.java              # NEW
│           ├── RequestToPayPortAdapter.java           # NEW
│           └── MerchantTransactionPortAdapter.java    # NEW
└── config/
    └── DomainServiceConfig.java               # EXTENDED: 9 workflows + 16 activities + 7 ports
```

---

## 6. Testing Strategy

### 6.1 Workflow Tests (Per Transaction Type)

| Workflow | Happy Path Tests | Error Path Tests | Compensation Tests |
|----------|-----------------|------------------|-------------------|
| CashlessPaymentWorkflow | Card authorized successfully | Card declined, switch timeout | Safety reversal on timeout |
| PinBasedPurchaseWorkflow | PIN authorized successfully | Invalid PIN, insufficient funds | Safety reversal on timeout |
| PrepaidTopupWorkflow | CELCOM/M1 top-up success (cash + card) | Invalid phone, aggregator timeout | ReleaseFloat on telco failure |
| EWalletWithdrawalWorkflow | Sarawak Pay withdrawal success (cash + card) | Insufficient eWallet balance | ReleaseFloat on eWallet failure |
| EWalletTopupWorkflow | Sarawak Pay top-up success (cash + card) | Invalid eWallet, timeout | ReleaseFloat on eWallet failure |
| ESSPPurchaseWorkflow | eSSP purchase success (cash + card) | BSN unavailable, invalid amount | ReleaseFloat on BSN failure |
| PINPurchaseWorkflow | PIN generation success (cash + card) | Inventory depleted, generation failed | ReleaseFloat on PIN failure |
| RetailSaleWorkflow | Card sale, QR sale, RTP sale success | Card declined, QR timeout, RTP declined | ReverseCreditFloat on failure |
| HybridCashbackWorkflow | Sale + cashback success | Insufficient float capacity, card declined | ReverseCreditFloat (both portions) + reversal |

### 6.2 Activity Tests

All 16 new activities require:
- Unit tests with mocked port interfaces
- Verification of correct exception throwing
- Timeout and retry behavior validation
- Input validation tests

### 6.3 Integration Tests

- End-to-end: POS request → Temporal workflow → Feign calls → response
- Mock servers for: Telco Aggregator, eWallet Provider, BSN eSSP, PIN Inventory
- Idempotency, duplicate detection, polling tests
- STP evaluation integration tests

### 6.4 ArchUnit Tests

- `domain/` has ZERO Spring/Temporal/JPA imports
- All new workflows in `application/workflow/`
- All new activities in `application/activity/`
- All port implementations in `infrastructure/`
- All Feign clients in `infrastructure/external/`

### 6.5 Coverage Requirements

- All workflows: 100% branch coverage
- All activities: 100% line coverage
- Controller: 100% line coverage
- Router: 100% branch coverage
- Port adapters: 100% line coverage

---

## 7. Implementation Phases

### Phase 1: Foundation (Week 1-2)
- Add 9 new TransactionType enum values
- Add 9 new WorkflowType enum values
- Add 24 new exception types
- Add 7 new port interfaces
- Add 16 new activity interfaces
- Extend WorkflowRouter with 9 new routes
- Extend CompensationRegistry
- Add Feign clients and port adapters
- Extend DomainServiceConfig with new beans

### Phase 2: Simple Workflows (Week 2-3)
- Implement CASHLESS_PAYMENT workflow + tests
- Implement PIN_BASED_PURCHASE workflow + tests
- Integration tests with mock switch adapter

### Phase 3: Medium Workflows (Week 3-5)
- Implement PREPAID_TOPUP workflow + tests
- Implement EWALLET_WITHDRAWAL workflow + tests
- Implement EWALLET_TOPUP workflow + tests
- Implement ESSP_PURCHASE workflow + tests
- Implement PIN_PURCHASE workflow + tests
- Integration tests with mock telco, eWallet, BSN, PIN services

### Phase 4: Complex Workflows (Week 5-7)
- Implement RETAIL_SALE workflow + tests
- Implement HYBRID_CASHBACK workflow + tests
- Integration tests with QR, RTP, MDR calculation
- EOD settlement impact tests

### Phase 5: Validation & Cleanup (Week 7-8)
- ArchUnit architecture tests
- Load testing and performance validation
- OpenAPI spec updates
- Documentation updates
- BDD scenario verification

---

## 8. Cross-Service Dependency Analysis

### 8.1 New Feign Clients Required

The orchestrator needs new Feign clients to communicate with external services for the 9 transaction types:

| Feign Client | Service | Protocol | New/Existing |
|-------------|---------|----------|-------------|
| `TelcoAggregatorClient` | Telco Aggregator Service (CELCOM, M1) | Feign (sync) | **NEW** |
| `EWalletProviderClient` | eWallet Provider Service (Sarawak Pay) | Feign (sync) | **NEW** |
| `ESSPServiceClient` | BSN eSSP Service | Feign (sync) | **NEW** |
| `PINInventoryClient` | PIN Inventory Service | Feign (sync) | **NEW** |
| `SwitchAdapterClient` | Switch Adapter Service | Feign (sync) | **EXTEND** (QR, RTP endpoints) |
| `LedgerServiceClient` | Ledger Service | Feign (sync) | **EXTEND** (merchant transaction, float capacity) |
| `RulesServiceClient` | Rules Service | Feign (sync) | **EXTEND** (MDR config) |

### 8.2 New Endpoints Required

#### Telco Aggregator Service (NEW)
```
POST /internal/validate-phone
Request: { phoneNumber: String, telcoProvider: String }
Response: { valid: boolean, operatorName: String, errorCode: String }

POST /internal/topup
Request: { telcoProvider: String, phoneNumber: String, amount: BigDecimal, idempotencyKey: String }
Response: { success: boolean, telcoReference: String, errorCode: String }
```

#### eWallet Provider Service (NEW)
```
POST /internal/validate-wallet
Request: { provider: String, walletId: String }
Response: { valid: boolean, walletBalance: BigDecimal, errorCode: String }

POST /internal/withdraw
Request: { provider: String, walletId: String, amount: BigDecimal, idempotencyKey: String }
Response: { success: boolean, ewalletReference: String, errorCode: String }

POST /internal/topup
Request: { provider: String, walletId: String, amount: BigDecimal, idempotencyKey: String }
Response: { success: boolean, ewalletReference: String, errorCode: String }
```

#### BSN eSSP Service (NEW)
```
POST /internal/validate-purchase
Request: { amount: BigDecimal }
Response: { valid: boolean, minAmount: BigDecimal, maxAmount: BigDecimal, errorCode: String }

POST /internal/purchase
Request: { amount: BigDecimal, customerMykad: String, idempotencyKey: String }
Response: { success: boolean, certificateNumber: String, errorCode: String }
```

#### PIN Inventory Service (NEW)
```
POST /internal/validate-inventory
Request: { provider: String, faceValue: BigDecimal }
Response: { available: boolean, stockCount: int, errorCode: String }

POST /internal/generate-pin
Request: { provider: String, faceValue: BigDecimal, idempotencyKey: String }
Response: { success: boolean, pinCode: String, serialNumber: String, expiryDate: LocalDate, errorCode: String }
```

#### Switch Adapter Service (EXTEND)
```
POST /internal/qr/generate
Request: { amount: BigDecimal, agentId: UUID, idempotencyKey: String }
Response: { qrCode: String, qrReference: String, errorCode: String }

GET /internal/qr/status/{qrReference}
Response: { status: String, paynetReference: String, errorCode: String }

POST /internal/rtp/send
Request: { proxy: String, amount: BigDecimal, idempotencyKey: String }
Response: { success: boolean, rtpReference: String, errorCode: String }

GET /internal/rtp/status/{rtpReference}
Response: { status: String, paynetReference: String, errorCode: String }
```

#### Ledger Service (EXTEND)
```
POST /internal/merchant-transaction
Request: { transactionId: UUID, merchantType: String, grossAmount: BigDecimal, mdrRate: BigDecimal, mdrAmount: BigDecimal, netCreditToFloat: BigDecimal, receiptType: String }
Response: { success: boolean, recordId: UUID, errorCode: String }

GET /internal/float-capacity/{agentId}
Request: { agentId: UUID }
Response: { hasCapacity: boolean, availableCapacity: BigDecimal, errorCode: String }
```

#### Rules Service (EXTEND)
```
POST /internal/mdr-config
Request: { transactionType: String, agentTier: String, amount: BigDecimal, paymentMethod: String }
Response: { mdrRate: BigDecimal, mdrAmount: BigDecimal, netCreditToFloat: BigDecimal, errorCode: String }
```

### 8.3 Endpoint Mismatches & Resolutions

| Orchestrator Activity | Expected Endpoint | Actual Service Endpoint | Resolution |
|----------------------|------------------|------------------------|------------|
| `TelcoAggregatorClient.validatePhone` | Does NOT exist | N/A | **New endpoint** in telco-aggregator-service |
| `TelcoAggregatorClient.topup` | Does NOT exist | N/A | **New endpoint** in telco-aggregator-service |
| `EWalletProviderClient.validateWallet` | Does NOT exist | N/A | **New endpoint** in ewallet-provider-service |
| `EWalletProviderClient.withdraw` | Does NOT exist | N/A | **New endpoint** in ewallet-provider-service |
| `EWalletProviderClient.topup` | Does NOT exist | N/A | **New endpoint** in ewallet-provider-service |
| `ESSPServiceClient.validatePurchase` | Does NOT exist | N/A | **New endpoint** in bsn-essp-service |
| `ESSPServiceClient.purchase` | Does NOT exist | N/A | **New endpoint** in bsn-essp-service |
| `PINInventoryClient.validateInventory` | Does NOT exist | N/A | **New endpoint** in pin-inventory-service |
| `PINInventoryClient.generatePIN` | Does NOT exist | N/A | **New endpoint** in pin-inventory-service |
| `SwitchAdapterClient.generateQR` | Does NOT exist | N/A | **New endpoint** in switch-adapter-service |
| `SwitchAdapterClient.checkQRStatus` | Does NOT exist | N/A | **New endpoint** in switch-adapter-service |
| `SwitchAdapterClient.sendRTP` | Does NOT exist | N/A | **New endpoint** in switch-adapter-service |
| `SwitchAdapterClient.checkRTPStatus` | Does NOT exist | N/A | **New endpoint** in switch-adapter-service |
| `LedgerServiceClient.createMerchantTransaction` | Does NOT exist | N/A | **New endpoint** in ledger-service |
| `LedgerServiceClient.checkFloatCapacity` | Does NOT exist | N/A | **New endpoint** in ledger-service |
| `RulesServiceClient.calculateMDR` | Does NOT exist | N/A | **New endpoint** in rules-service (or extend fee-config) |

### 8.4 Missing Configuration Parameters

| Parameter | Current Location | Required By | Action |
|-----------|-----------------|-------------|--------|
| Telco aggregator base URL | Not defined | `TelcoAggregatorClient` | Add to `application.yaml`: `telco-aggregator.url` |
| eWallet provider base URL | Not defined | `EWalletProviderClient` | Add to `application.yaml`: `ewallet-provider.url` |
| BSN eSSP base URL | Not defined | `ESSPServiceClient` | Add to `application.yaml`: `essp-service.url` |
| PIN inventory base URL | Not defined | `PINInventoryClient` | Add to `application.yaml`: `pin-inventory.url` |
| MDR rate configuration | Rules Service fee config | `CalculateMDRActivity` | Extend fee-config endpoint or add dedicated MDR config |
| QR payment timeout (120s) | Not defined | `WaitForQRPaymentActivity` | Add to `application.yaml`: `temporal.activity-timeouts.qr-payment` |
| RTP approval timeout (300s) | Not defined | `WaitForRTPApprovalActivity` | Add to `application.yaml`: `temporal.activity-timeouts.rtp-approval` |
| Hybrid cashback max limit | Not defined | `ValidateFloatCapacityActivity` | Add to rules-service or as dedicated config: `hybrid-cashback.max-amount` |
| Telco top-up timeout (30s) | Not defined | `TopUpTelcoActivity` | Add to `application.yaml`: `temporal.activity-timeouts.telco-topup` |
| eWallet operation timeout (15s) | Not defined | `WithdrawFromEWalletActivity`, `TopUpEWalletActivity` | Add to `application.yaml`: `temporal.activity-timeouts.ewallet` |
| BSN eSSP timeout (20s) | Not defined | `PurchaseESSPActivity` | Add to `application.yaml`: `temporal.activity-timeouts.essp` |
| PIN generation timeout (10s) | Not defined | `GeneratePINActivity` | Add to `application.yaml`: `temporal.activity-timeouts.pin-generation` |

### 8.5 Service Dependency Graph

```
orchestrator-service
├── telco-aggregator-service (NEW)
│   ├── validate-phone
│   └── topup
├── ewallet-provider-service (NEW)
│   ├── validate-wallet
│   ├── withdraw
│   └── topup
├── bsn-essp-service (NEW)
│   ├── validate-purchase
│   └── purchase
├── pin-inventory-service (NEW)
│   ├── validate-inventory
│   └── generate-pin
├── switch-adapter-service (EXTEND)
│   ├── qr/generate
│   ├── qr/status/{qrReference}
│   ├── rtp/send
│   └── rtp/status/{rtpReference}
├── ledger-service (EXTEND)
│   ├── merchant-transaction
│   └── float-capacity/{agentId}
└── rules-service (EXTEND)
    └── mdr-config
```

### 8.6 Cross-Service Contract Requirements

Each new service integration requires:

1. **OpenAPI Spec:** Internal API spec at `<service>/docs/openapi-internal.yaml`
2. **Feign Client:** `@FeignClient` interface in `infrastructure/external/`
3. **Port Adapter:** Implements domain port, delegates to Feign client
4. **Error Mapping:** External service errors → typed exceptions (Section 5.4)
5. **Circuit Breaker:** Resilience4j configuration per Feign client
6. **Idempotency:** All financial endpoints accept `idempotencyKey`

### 8.7 Docker Compose Updates

For local development, add service containers:

```yaml
services:
  # Existing services...
  
  # Phase 3+ mock services for development
  telco-aggregator-mock:
    image: wiremock/wiremock:2.35
    ports:
      - "8090:8080"
    volumes:
      - ./mocks/telco-aggregator:/home/wiremock/mappings
  
  ewallet-provider-mock:
    image: wiremock/wiremock:2.35
    ports:
      - "8091:8080"
    volumes:
      - ./mocks/ewallet-provider:/home/wiremock/mappings
  
  bsn-essp-mock:
    image: wiremock/wiremock:2.35
    ports:
      - "8092:8080"
    volumes:
      - ./mocks/bsn-essp:/home/wiremock/mappings
  
  pin-inventory-mock:
    image: wiremock/wiremock:2.35
    ports:
      - "8093:8080"
    volumes:
      - ./mocks/pin-inventory:/home/wiremock/mappings
```

---

## 9. Open Questions

1. **Telco Aggregator Selection:** Should we use a single aggregator for multiple telcos (CELCOM, M1) or separate integrations per telco?
2. **eWallet Provider Contract:** Is Sarawak Pay the only eWallet provider for Phase 3, or should we design for multiple providers?
3. **PIN Inventory Source:** Does the bank maintain PIN inventory, or is it sourced from telco providers on-demand?
4. **MDR Rate Configuration:** Should MDR rates be configurable per merchant category code (MCC) or fixed per payment method?
5. **QR Payment Timeout:** What is the acceptable timeout for QR payment waiting (currently 120s)? Should this be configurable?
6. **RTP Approval Timeout:** What is the acceptable timeout for Request-to-Pay approval (currently 300s)? Should this be configurable?
7. **Hybrid Cashback Limits:** Is there a maximum cashback amount per transaction? (e.g., RM 200 per BNM guidelines)

---

## 9. BDD Traceability Matrix

| BDD Scenario | Transaction Type | Workflow | Status |
|-------------|-----------------|----------|--------|
| BDD-T01 | PREPAID_TOPUP | PrepaidTopupWorkflow | Covered |
| BDD-T01-CARD | PREPAID_TOPUP | PrepaidTopupWorkflow | Covered |
| BDD-T01-EC-01 | PREPAID_TOPUP | PrepaidTopupWorkflow | Covered |
| BDD-T01-EC-02 | PREPAID_TOPUP | PrepaidTopupWorkflow | Covered |
| BDD-T02 | PREPAID_TOPUP | PrepaidTopupWorkflow | Covered |
| BDD-T02-EC-01 | PREPAID_TOPUP | PrepaidTopupWorkflow | Covered |
| BDD-WAL-01 | EWALLET_WITHDRAWAL | EWalletWithdrawalWorkflow | Covered |
| BDD-WAL-01-CARD | EWALLET_WITHDRAWAL | EWalletWithdrawalWorkflow | Covered |
| BDD-WAL-01-EC-01 | EWALLET_WITHDRAWAL | EWalletWithdrawalWorkflow | Covered |
| BDD-WAL-02 | EWALLET_TOPUP | EWalletTopupWorkflow | Covered |
| BDD-WAL-02-CARD | EWALLET_TOPUP | EWalletTopupWorkflow | Covered |
| BDD-ESSP-01 | ESSP_PURCHASE | ESSPPurchaseWorkflow | Covered |
| BDD-ESSP-01-CARD | ESSP_PURCHASE | ESSPPurchaseWorkflow | Covered |
| BDD-ESSP-01-EC-01 | ESSP_PURCHASE | ESSPPurchaseWorkflow | Covered |
| BDD-M01 | RETAIL_SALE | RetailSaleWorkflow | Covered |
| BDD-M01-QR | RETAIL_SALE | RetailSaleWorkflow | Covered |
| BDD-M01-RTP | RETAIL_SALE | RetailSaleWorkflow | Covered |
| BDD-M01-EC-01 | RETAIL_SALE | RetailSaleWorkflow | Covered |
| BDD-M02 | PIN_PURCHASE | PINPurchaseWorkflow | Covered |
| BDD-M02-CARD | PIN_PURCHASE | PINPurchaseWorkflow | Covered |
| BDD-M02-EC-01 | PIN_PURCHASE | PINPurchaseWorkflow | Covered |
| BDD-M03 | HYBRID_CASHBACK | HybridCashbackWorkflow | Covered |
| BDD-M03-EC-01 | HYBRID_CASHBACK | HybridCashbackWorkflow | Covered |
| BDD-M03-NET | HYBRID_CASHBACK | HybridCashbackWorkflow | Covered |
| *New* | CASHLESS_PAYMENT | CashlessPaymentWorkflow | 3 scenarios added |
| *New* | PIN_BASED_PURCHASE | PinBasedPurchaseWorkflow | 2 scenarios added |
