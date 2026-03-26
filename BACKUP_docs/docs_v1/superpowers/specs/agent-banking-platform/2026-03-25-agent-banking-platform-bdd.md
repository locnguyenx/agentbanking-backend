# Behavior-Driven Development (BDD) Specification
## Agent Banking Platform (Malaysia)

**Version:** 1.0
**Date:** 2026-03-25
**Status:** Draft — Pending Review
**BRD Reference:** `2026-03-25-agent-banking-platform-brd.md`
**Entity Reference:** BRD Section 4 (Entity Definitions)

---

## Scenario Classification

- **[HP]** = Happy Path (optimal user flow)
- **[EC]** = Edge Case (boundary, invalid, or extreme scenarios)

---

## 1. Rules & Fee Engine

### BDD-R01 [HP] — US-R01, FR-1.1, FR-1.4
```gherkin
Scenario: Configure fee structure for Micro agent cash withdrawal
  Given a FeeConfig exists:
    | field                | value           |
    | transactionType      | CASH_WITHDRAWAL |
    | agentTier            | MICRO           |
    | feeType              | FIXED           |
    | customerFeeValue     | 1.00            |
    | agentCommissionValue | 0.20            |
    | bankShareValue       | 0.80            |
    | dailyLimitAmount     | 5000.00         |
    | dailyLimitCount      | 10              |
  And an Agent exists:
    | field     | value  |
    | agentId   | AGT-01 |
    | tier      | MICRO  |
    | status    | ACTIVE |
  When Agent "AGT-01" processes a CASH_WITHDRAWAL of RM 500.00
  Then the Transaction should have:
    | field            | value   |
    | customerFee      | 1.00    |
    | agentCommission  | 0.20    |
    | bankShare        | 0.80    |
    | status           | PENDING |
```

### BDD-R01-PCT [HP] — US-R01, FR-1.1
```gherkin
Scenario: Percentage-based fee for Premier agent cash withdrawal
  Given a FeeConfig exists:
    | field                | value           |
    | transactionType      | CASH_WITHDRAWAL |
    | agentTier            | PREMIER         |
    | feeType              | PERCENTAGE      |
    | customerFeeValue     | 0.005           |
    | agentCommissionValue | 0.002           |
    | bankShareValue       | 0.003           |
  When Agent "AGT-02" (PREMIER) processes a CASH_WITHDRAWAL of RM 10000.00
  Then the Transaction should have:
    | field            | value  |
    | customerFee      | 50.00  |
    | agentCommission  | 20.00  |
    | bankShare        | 30.00  |
```

### BDD-R01-EC-01 [EC] — US-R01, FR-1.1
```gherkin
Scenario: No fee configuration exists for transaction type and tier
  Given no FeeConfig exists for transactionType "CASH_WITHDRAWAL" and agentTier "MICRO"
  When Agent "AGT-01" (MICRO) processes a CASH_WITHDRAWAL of RM 500.00
  Then the system should return error code "ERR_FEE_CONFIG_NOT_FOUND"
  And the Transaction.status should be FAILED
```

### BDD-R01-EC-02 [EC] — US-R01, FR-1.1
```gherkin
Scenario: Fee configuration is expired (effectiveTo is in the past)
  Given a FeeConfig exists with effectiveTo "2026-01-01" for CASH_WITHDRAWAL MICRO
  And today is "2026-03-25"
  When Agent "AGT-01" (MICRO) processes a CASH_WITHDRAWAL
  Then the system should return error code "ERR_FEE_CONFIG_EXPIRED"
  And the Transaction.status should be FAILED
```

### BDD-R01-EC-03 [EC] — US-R01, FR-1.4
```gherkin
Scenario: Fee component values do not sum correctly
  Given a FeeConfig exists:
    | field                | value |
    | customerFeeValue     | 1.00  |
    | agentCommissionValue | 0.50  |
    | bankShareValue       | 0.30  |
  And the sum (1.00) != (0.50 + 0.30)
  When the FeeConfig is saved
  Then the system should reject the configuration
  And return error code "ERR_FEE_COMPONENTS_MISMATCH"
```

### BDD-R02 [HP] — US-R02, FR-1.2
```gherkin
Scenario: Daily transaction limit check passes
  Given a FeeConfig with dailyLimitAmount "10000.00" for STANDARD CASH_WITHDRAWAL
  And Agent "AGT-03" (STANDARD) has completed RM 5000.00 in CASH_WITHDRAWAL today
  When Agent "AGT-03" attempts a CASH_WITHDRAWAL of RM 3000.00
  Then the limit check should pass
  And the Transaction.status should be PENDING
```

### BDD-R02-EC-01 [EC] — US-R02, FR-1.2
```gherkin
Scenario: Daily transaction limit exceeded — amount boundary
  Given a FeeConfig with dailyLimitAmount "5000.00" for MICRO CASH_WITHDRAWAL
  And Agent "AGT-01" (MICRO) has completed RM 4500.00 in CASH_WITHDRAWAL today
  When Agent "AGT-01" attempts a CASH_WITHDRAWAL of RM 1000.00
  Then the limit check should fail
  And the system should return error code "ERR_LIMIT_EXCEEDED"
  And the Transaction.status should be FAILED
```

### BDD-R02-EC-02 [EC] — US-R02, FR-1.2
```gherkin
Scenario: Daily transaction count limit exceeded
  Given a FeeConfig with dailyLimitCount "10" for MICRO CASH_WITHDRAWAL
  And Agent "AGT-01" (MICRO) has completed 10 CASH_WITHDRAWAL transactions today
  When Agent "AGT-01" attempts another CASH_WITHDRAWAL
  Then the limit check should fail
  And the system should return error code "ERR_COUNT_LIMIT_EXCEEDED"
```

### BDD-R02-EC-03 [EC] — US-R02, FR-1.2
```gherkin
Scenario: Transaction amount is zero
  Given a FeeConfig exists for CASH_WITHDRAWAL MICRO
  When Agent "AGT-01" (MICRO) attempts a CASH_WITHDRAWAL of RM 0.00
  Then the system should return error code "ERR_INVALID_AMOUNT"
  And the Transaction.status should be FAILED
```

### BDD-R02-EC-04 [EC] — US-R02, FR-1.2
```gherkin
Scenario: Transaction amount is negative
  Given a FeeConfig exists for CASH_WITHDRAWAL MICRO
  When Agent "AGT-01" (MICRO) attempts a CASH_WITHDRAWAL of RM -100.00
  Then the system should return error code "ERR_INVALID_AMOUNT"
  And the Transaction.status should be FAILED
```

### BDD-R04 [HP] — US-R04, FR-1.4
```gherkin
Scenario: Percentage-based fee calculation with rounding
  Given a FeeConfig exists:
    | field                | value           |
    | transactionType      | CASH_WITHDRAWAL |
    | agentTier            | STANDARD        |
    | feeType              | PERCENTAGE      |
    | customerFeeValue     | 0.005           |
    | agentCommissionValue | 0.002           |
    | bankShareValue       | 0.003           |
  When Agent "AGT-03" (STANDARD) processes a CASH_WITHDRAWAL of RM 333.33
  Then the Transaction should have:
    | field            | value |
    | customerFee      | 1.67  |
    | agentCommission  | 0.67  |
    | bankShare        | 1.00  |
  And rounding should use HALF_UP to 2 decimal places
```

### BDD-R03 [HP] — US-R03, FR-1.3
```gherkin
Scenario: Velocity check passes
  Given a VelocityRule exists:
    | field                 | value |
    | maxTransactionsPerDay | 5     |
    | maxAmountPerDay       | 25000 |
    | scope                 | GLOBAL |
    | isActive              | true  |
  And customer MyKad "123456789012" has 2 transactions today totaling RM 5000
  When a transaction is initiated for MyKad "123456789012"
  Then the velocity check should pass
```

### BDD-R03-EC-01 [EC] — US-R03, FR-1.3
```gherkin
Scenario: Velocity check fails — transaction count exceeded
  Given a VelocityRule with maxTransactionsPerDay "5" and isActive "true"
  And customer MyKad "123456789012" has 5 transactions today
  When a transaction is initiated for MyKad "123456789012"
  Then the velocity check should fail
  And the system should return error code "ERR_VELOCITY_COUNT_EXCEEDED"
  And the Transaction.status should be FAILED
```

### BDD-R03-EC-02 [EC] — US-R03, FR-1.3
```gherkin
Scenario: Velocity check fails — amount exceeded
  Given a VelocityRule with maxAmountPerDay "25000" and isActive "true"
  And customer MyKad "123456789012" has transactions totaling RM 24000 today
  When a transaction of RM 2000 is initiated for MyKad "123456789012"
  Then the velocity check should fail
  And the system should return error code "ERR_VELOCITY_AMOUNT_EXCEEDED"
```

### BDD-R03-EC-03 [EC] — US-R03, FR-1.3
```gherkin
Scenario: Velocity rule is inactive — check is skipped
  Given a VelocityRule with maxTransactionsPerDay "5" and isActive "false"
  And customer MyKad "123456789012" has 10 transactions today
  When a transaction is initiated for MyKad "123456789012"
  Then the velocity check should pass (rule not enforced)
```

### BDD-R03-EC-04 [EC] — US-R03, FR-1.3
```gherkin
Scenario: No velocity rule configured — default behavior
  Given no active VelocityRule exists
  When a transaction is initiated for any MyKad
  Then the velocity check should pass (no rule to enforce)
```

---

## 2. Ledger & Float Service

### BDD-L01 [HP] — US-L01, FR-2.1
```gherkin
Scenario: Agent checks wallet balance
  Given an AgentFloat exists:
    | field           | value     |
    | agentId         | AGT-01    |
    | balance         | 10000.00  |
    | reservedBalance | 0.00      |
    | currency        | MYR       |
  When Agent "AGT-01" requests balance inquiry
  Then the response should contain:
    | field            | value     |
    | balance          | 10000.00  |
    | reservedBalance  | 0.00      |
    | availableBalance | 10000.00  |
```

### BDD-L01-EC-01 [EC] — US-L01, FR-2.1
```gherkin
Scenario: Agent float not found
  Given no AgentFloat exists for agentId "AGT-99"
  When Agent "AGT-99" requests balance inquiry
  Then the system should return error code "ERR_AGENT_FLOAT_NOT_FOUND"
```

### BDD-L01-EC-02 [EC] — US-L01, FR-2.1
```gherkin
Scenario: Agent is deactivated — balance inquiry blocked
  Given an Agent exists with agentId "AGT-01" and status "DEACTIVATED"
  When Agent "AGT-01" requests balance inquiry
  Then the system should return error code "ERR_AGENT_DEACTIVATED"
```

### BDD-L02 [HP] — US-L02, FR-2.2
```gherkin
Scenario: Transaction creates double-entry journal
  Given Agent "AGT-01" processes a CASH_WITHDRAWAL of RM 500.00
  When the transaction is completed
  Then two JournalEntry records should be created:
    | entryType | accountCode | amount | description                    |
    | DEBIT     | AGT_FLOAT   | 500.00 | Debit agent float              |
    | CREDIT    | BANK_SETTLE | 500.00 | Credit bank settlement account |
  And both entries should reference the same transactionId
```

### BDD-L03 [HP] — US-L03, FR-2.1, FR-2.3
```gherkin
Scenario: Real-time settlement updates agent float
  Given AgentFloat for "AGT-01" has balance "10000.00"
  When a CASH_WITHDRAWAL of RM 500.00 completes
  Then AgentFloat.balance should be "9500.00"
  And AgentFloat.updatedAt should be the current timestamp
  And no EOD batch processing is required
```

### BDD-L03-EC-01 [EC] — US-L03, FR-2.1, FR-2.3
```gherkin
Scenario: Insufficient agent float for withdrawal
  Given AgentFloat for "AGT-01" has balance "200.00"
  When a CASH_WITHDRAWAL of RM 500.00 is attempted
  Then the system should return error code "ERR_INSUFFICIENT_FLOAT"
  And AgentFloat.balance should remain "200.00"
  And the Transaction.status should be FAILED
```

### BDD-L03-EC-02 [EC] — US-L03, FR-2.3
```gherkin
Scenario: Concurrent withdrawal race condition
  Given AgentFloat for "AGT-01" has balance "600.00"
  And two concurrent CASH_WITHDRAWAL requests of RM 500.00 each
  When both requests are processed simultaneously
  Then only one request should succeed (PESSIMISTIC_WRITE lock)
  And the other should return error code "ERR_INSUFFICIENT_FLOAT"
  And AgentFloat.balance should be "100.00" after both complete
```

### BDD-L04 [HP] — US-L04, FR-5.1
```gherkin
Scenario: Customer balance inquiry via card + PIN
  Given a customer presents card "411111******1111" with valid PIN
  When the POS terminal requests balance inquiry
  Then the system should return the account balance
  And the card number should be masked in all logs
```

### BDD-L04-EC-01 [EC] — US-L04, FR-5.1
```gherkin
Scenario: Balance inquiry with invalid PIN
  Given a customer presents card "411111******1111" with invalid PIN
  When the POS terminal requests balance inquiry
  Then the system should return error code "ERR_INVALID_PIN"
  And no balance information should be returned
```

### BDD-L04-EC-02 [EC] — US-L04, FR-2.4
```gherkin
Scenario: Duplicate balance inquiry with same idempotency key
  Given a balance inquiry was completed with idempotency key "IDEM-123"
  When another balance inquiry is received with idempotency key "IDEM-123"
  Then the system should return the cached response from the first request
  And no new Transaction should be created
```

### BDD-L05-EC-01 is covered by BDD-W01-EC-07 (same idempotency check on withdrawal)

---

## 3. Cash Withdrawal

### BDD-W01 [HP] — US-L05, FR-3.1, FR-3.3, FR-3.5
```gherkin
Scenario: Successful ATM card withdrawal (EMV + PIN)
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "10000.00"
  And a FeeConfig for CASH_WITHDRAWAL STANDARD:
    | field            | value  |
    | customerFeeValue | 1.00   |
    | dailyLimitAmount | 10000  |
  And the customer presents a valid EMV card with correct PIN
  When the POS terminal requests CASH_WITHDRAWAL of RM 500.00
  Then the Switch Adapter should send ISO 8583 authorization to PayNet
  And on approval, the Transaction should be created:
    | field              | value           |
    | transactionType    | CASH_WITHDRAWAL |
    | amount             | 500.00          |
    | customerFee        | 1.00            |
    | agentCommission    | 0.20            |
    | bankShare          | 0.80            |
    | status             | COMPLETED       |
    | customerCardMasked | 411111******1111 |
  And AgentFloat.balance should be "9500.00"
  And two JournalEntry records should exist (debit float, credit settlement)
```

### BDD-W01-EC-01 [EC] — US-L05, FR-3.1
```gherkin
Scenario: Withdrawal with invalid card PIN
  Given the customer presents an EMV card with incorrect PIN
  When the POS terminal requests CASH_WITHDRAWAL
  Then the Switch Adapter should receive ISO 8583 decline from PayNet
  And the Transaction.status should be FAILED
  And the Transaction.errorCode should be "ERR_INVALID_PIN"
  And AgentFloat should not be modified
```

### BDD-W01-EC-02 [EC] — US-L05, FR-3.4
```gherkin
Scenario: Terminal printer failure after switch approval — reversal triggered
  Given the Switch Adapter has received approval from PayNet
  And the Transaction.status is COMPLETED
  And AgentFloat was debited RM 500.00
  When the POS terminal reports printer failure
  Then the system should immediately trigger MTI 0400 Reversal
  And the Transaction.status should change to REVERSED
  And AgentFloat.balance should be restored to original amount
```

### BDD-W01-EC-03 [EC] — US-L05, FR-3.4
```gherkin
Scenario: Network drop after switch approval — Store & Forward reversal
  Given the Switch Adapter has received approval from PayNet
  And the network connection to the POS terminal drops
  Then the system should store the reversal request (Store & Forward)
  And retry the MTI 0400 Reversal when network is restored
  And the Transaction.status should eventually change to REVERSED
```

### BDD-W01-EC-04 [EC] — US-L05, FR-3.3
```gherkin
Scenario: Withdrawal amount exceeds daily limit
  Given the daily withdrawal limit for STANDARD is RM 10000
  And Agent "AGT-01" has processed RM 9500 in withdrawals today
  When a CASH_WITHDRAWAL of RM 1000 is attempted
  Then the system should return error code "ERR_LIMIT_EXCEEDED"
  And the Transaction.status should be FAILED
```

### BDD-W01-EC-05 [EC] — US-L05, NFR-4.2
```gherkin
Scenario: Withdrawal outside geofence — location violation
  Given Agent "AGT-01" is registered at GPS (3.1390, 101.6869)
  And the POS terminal reports GPS (3.2000, 101.7000) — 7km away
  When a CASH_WITHDRAWAL is attempted
  Then the system should return error code "ERR_GEOFENCE_VIOLATION"
  And the Transaction.status should be FAILED
```

### BDD-W01-EC-06 [EC] — US-L05, NFR-4.2
```gherkin
Scenario: GPS unavailable on POS terminal
  Given the POS terminal cannot obtain GPS coordinates
  When a CASH_WITHDRAWAL is attempted
  Then the system should return error code "ERR_GPS_UNAVAILABLE"
  And the Transaction.status should be FAILED
```

### BDD-W01-EC-07 [EC] — US-L05, FR-2.4
```gherkin
Scenario: Duplicate withdrawal with same idempotency key
  Given a CASH_WITHDRAWAL was completed with idempotency key "IDEM-789"
  When another withdrawal request arrives with idempotency key "IDEM-789"
  Then the system should return the original response
  And AgentFloat should not be debited again
  And no new Transaction should be created
```

### BDD-W01-EC-08 [EC] — US-L05, FR-3.4
```gherkin
Scenario: Reversal fails after multiple retries
  Given a reversal (MTI 0400) has been retried 3 times and still fails
  Then the system should flag the Transaction for manual investigation
  And create an AuditLog entry with action "FAIL"
  And alert the backoffice operations team
```

---

## 4. Cash Deposit

### BDD-D01 [HP] — US-L07, FR-2.5, FR-4.1, FR-4.3
```gherkin
Scenario: Successful cash deposit with account validation
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer provides destination account "1234567890"
  And ProxyEnquiry confirms the account is valid
  When the customer hands RM 1000.00 cash to the agent
  And the POS terminal confirms cash received
  Then the system should credit the destination account RM 1000.00
  And the Transaction should be created:
    | field            | value        |
    | transactionType  | CASH_DEPOSIT |
    | amount           | 1000.00      |
    | status           | COMPLETED    |
  And AgentFloat.balance should be "6000.00"
```

### BDD-D01-EC-01 [EC] — US-L07, FR-2.5
```gherkin
Scenario: Deposit to invalid account — ProxyEnquiry fails
  Given the customer provides destination account "9999999999"
  And ProxyEnquiry returns "ACCOUNT_NOT_FOUND"
  When the POS terminal attempts CASH_DEPOSIT
  Then the system should return error code "ERR_INVALID_ACCOUNT"
  And the Transaction.status should be FAILED
  And no money should be moved
```

### BDD-D01-EC-02 [EC] — US-L07, FR-4.1
```gherkin
Scenario: Deposit amount is zero
  Given ProxyEnquiry confirms account "1234567890" is valid
  When a CASH_DEPOSIT of RM 0.00 is attempted
  Then the system should return error code "ERR_INVALID_AMOUNT"
  And the Transaction.status should be FAILED
```

### BDD-D01-EC-03 [EC] — US-L07, FR-4.1
```gherkin
Scenario: Deposit amount is negative
  Given ProxyEnquiry confirms account "1234567890" is valid
  When a CASH_DEPOSIT of RM -500.00 is attempted
  Then the system should return error code "ERR_INVALID_AMOUNT"
  And the Transaction.status should be FAILED
```

### BDD-D01-EC-04 [EC] — US-L07
```gherkin
Scenario: Agent float cap exceeded after deposit
  Given Agent "AGT-01" (MICRO) has a float cap of RM 20000.00
  And AgentFloat.balance is "19500.00"
  When a CASH_DEPOSIT of RM 1000.00 is attempted
  Then the system should return error code "ERR_FLOAT_CAP_EXCEEDED"
  And the Transaction.status should be FAILED
```

---

## 5. e-KYC & Onboarding

### BDD-O01 [HP] — US-O01, FR-6.1
```gherkin
Scenario: Successful MyKad verification via JPN
  Given an agent initiates e-KYC for MyKad "123456789012"
  And the JPN API returns:
    | field       | value         |
    | fullName    | AHMAD BIN ABU |
    | dateOfBirth | 1990-05-15    |
    | amlStatus   | CLEAN         |
  When the verification completes
  Then a KycVerification should be created:
    | field              | value           |
    | mykadNumber        | 123456789012    |
    | fullName           | AHMAD BIN ABU   |
    | dateOfBirth        | 1990-05-15      |
    | age                | 35              |
    | amlStatus          | CLEAN           |
    | verificationStatus | AUTO_APPROVED   |
```

### BDD-O02 [HP] — US-O02, FR-6.2, FR-6.3
```gherkin
Scenario: Successful biometric match-on-card with auto-approval
  Given MyKad "123456789012" has been verified via JPN (AML=CLEAN, age=35)
  And the biometric (thumbprint) match returns MATCH
  When the verification completes
  Then KycVerification.verificationStatus should be AUTO_APPROVED
  And the customer can proceed with account opening
```

### BDD-O01-EC-01 [EC] — US-O01, FR-6.1
```gherkin
Scenario: JPN API returns "MyKad not found"
  Given an agent initiates e-KYC for MyKad "000000000000"
  And the JPN API returns "NOT_FOUND"
  When the verification completes
  Then the system should return error code "ERR_MYKAD_NOT_FOUND"
  And KycVerification.verificationStatus should be REJECTED
  And KycVerification.rejectionReason should be "MyKad not found in JPN records"
```

### BDD-O01-EC-02 [EC] — US-O01, FR-6.1
```gherkin
Scenario: JPN API is unavailable
  Given the JPN API returns a timeout or 503 error
  When the verification is attempted
  Then the system should return error code "ERR_KYC_SERVICE_UNAVAILABLE"
  And the verification should be queued for retry
```

### BDD-O01-EC-03 [EC] — US-O01, FR-6.3
```gherkin
Scenario: Customer is under 18 — auto-reject
  Given the JPN API returns dateOfBirth "2012-05-15" (age=13)
  When the verification completes
  Then KycVerification.verificationStatus should be REJECTED
  And KycVerification.rejectionReason should be "Customer age below minimum (18)"
```

### BDD-O03 [HP] — US-O03, FR-6.3, FR-6.4
```gherkin
Scenario: KYC auto-approval decision matrix — all conditions must pass
  Given the JPN API returns:
    | field       | value         |
    | amlStatus   | CLEAN         |
    | dateOfBirth | 1990-05-15    |
  And the biometric match returns MATCH
  When the verification completes
  Then KycVerification.verificationStatus should be AUTO_APPROVED
```

### BDD-O03-EC-01 [EC] — US-O03, FR-6.3, FR-6.4
```gherkin
Scenario: AML=FLAGGED overrides biometric=MATCH — manual review
  Given the JPN API returns amlStatus "FLAGGED"
  And the biometric match returns MATCH
  And the customer age is 35
  When the verification completes
  Then KycVerification.verificationStatus should be MANUAL_REVIEW
  And the rejection reason should reference AML status
```

### BDD-O03-EC-02 [EC] — US-O03, FR-6.3
```gherkin
Scenario: Age < 18 overrides all other passing conditions — reject
  Given the JPN API returns:
    | field       | value      |
    | amlStatus   | CLEAN      |
    | dateOfBirth | 2012-05-15 |
  And the biometric match returns MATCH
  When the verification completes
  Then KycVerification.verificationStatus should be REJECTED
  And KycVerification.rejectionReason should be "Customer age below minimum (18)"
```

### BDD-O02-EC-01 [EC] — US-O02, FR-6.2, FR-6.4
```gherkin
Scenario: Biometric match fails — queue for manual review
  Given MyKad "123456789012" has been verified via JPN (AML=CLEAN, age=35)
  And the biometric match returns NO_MATCH
  When the verification completes
  Then KycVerification.verificationStatus should be MANUAL_REVIEW
  And KycVerification.biometricMatch should be NO_MATCH
  And the case should appear in backoffice manual review queue
```

### BDD-O02-EC-02 [EC] — US-O02, FR-6.4
```gherkin
Scenario: AML status is flagged — queue for manual review regardless of biometric
  Given the JPN API returns amlStatus "FLAGGED"
  And the biometric match returns MATCH
  When the verification completes
  Then KycVerification.verificationStatus should be MANUAL_REVIEW
  And KycVerification.amlStatus should be FLAGGED
```

### BDD-O02-EC-03 [EC] — US-O02, FR-6.2
```gherkin
Scenario: Biometric scanner unavailable on POS terminal
  Given the POS terminal biometric scanner returns an error
  When the verification is attempted
  Then the system should return error code "ERR_BIOMETRIC_SCANNER_UNAVAILABLE"
  And the verification should be queued for retry
```

### BDD-O02-EC-04 [EC] — US-O02, FR-6.1
```gherkin
Scenario: MyKad number format invalid
  Given an agent initiates e-KYC for MyKad "12345"
  When the verification is attempted
  Then the system should return error code "ERR_INVALID_MYKAD_FORMAT"
  And the verification should not proceed
```

---

## 6. API Gateway

### BDD-G01 [HP] — US-G01, FR-12.1
```gherkin
Scenario: Gateway routes POS request to correct service
  Given the POS terminal sends a POST to "/api/v1/withdrawal"
  And the request contains valid authentication token
  When the Gateway receives the request
  Then it should route to the Ledger & Float Service
  And the response should be returned to the POS terminal
```

### BDD-G02 [HP] — US-G02, FR-12.2
```gherkin
Scenario: Gateway authenticates external API request
  Given the POS terminal sends a request with a valid Bearer token
  When the Gateway receives the request
  Then the token should be validated
  And the request should be forwarded to the backend service
  And the agentId should be extracted from the token claims
```

### BDD-G01-EC-01 [EC] — US-G02, FR-12.2
```gherkin
Scenario: Gateway rejects request with expired token
  Given the POS terminal sends a request with an expired Bearer token
  When the Gateway receives the request
  Then the Gateway should return HTTP 401
  And the response body should be:
    """
    { "status": "FAILED", "error": { "code": "ERR_TOKEN_EXPIRED" } }
    """
  And the request should NOT be forwarded to any backend service
```

### BDD-G01-EC-02 [EC] — US-G02, FR-12.2
```gherkin
Scenario: Gateway rejects request with no authentication
  Given the POS terminal sends a request without any Authorization header
  When the Gateway receives the request
  Then the Gateway should return HTTP 401
  And the response should be:
    """
    { "status": "FAILED", "error": { "code": "ERR_MISSING_TOKEN" } }
    """
```

### BDD-G01-EC-03 [EC] — US-G01
```gherkin
Scenario: Gateway returns 503 when backend service is down
  Given the Ledger & Float Service is unavailable (circuit breaker open)
  When the POS terminal sends a withdrawal request
  Then the Gateway should return HTTP 503
  And the response should be:
    """
    { "status": "FAILED", "error": { "code": "ERR_SERVICE_UNAVAILABLE" } }
    """
```

---

## 7. Backoffice

### BDD-BO01 [HP] — US-BO01, FR-13.1
```gherkin
Scenario: Bank operator creates a new agent
  Given a bank operator is logged into the backoffice
  When the operator submits:
    | field          | value              |
    | businessName   | Kedai Ali          |
    | tier           | STANDARD           |
    | mykadNumber    | 880101011234       |
    | phoneNumber    | +60123456789       |
    | merchantGpsLat | 3.1390             |
    | merchantGpsLng | 101.6869           |
  Then a new Agent should be created with status ACTIVE
  And an AgentFloat should be created with balance 0.00
  And an AuditLog entry should be created with action CREATE
```

### BDD-BO02 [HP] — US-BO02, FR-13.2
```gherkin
Scenario: Bank operator monitors transaction activity
  Given multiple transactions have been processed today
  When the operator opens the transaction monitoring dashboard
  Then the dashboard should display:
    | field              | visible |
    | transaction list   | yes     |
    | status filter      | yes     |
    | agent filter       | yes     |
    | date range filter  | yes     |
    | real-time updates  | yes     |
```

### BDD-BO01-EC-01 [EC] — US-BO01, FR-13.1
```gherkin
Scenario: Duplicate agent creation — same MyKad
  Given an Agent already exists with mykadNumber "880101011234"
  When the operator attempts to create another agent with the same MyKad
  Then the system should return error code "ERR_DUPLICATE_AGENT"
  And no new Agent should be created
```

### BDD-BO01-EC-02 [EC] — US-BO01, FR-13.1
```gherkin
Scenario: Deactivate agent with pending transactions
  Given Agent "AGT-01" has pending transactions
  When the operator attempts to deactivate the agent
  Then the system should return error code "ERR_AGENT_HAS_PENDING_TRANSACTIONS"
  And the agent status should remain ACTIVE
```

### BDD-BO03 [HP] — US-BO03, FR-13.3
```gherkin
Scenario: Bank operator views settlement report
  Given transactions have been completed today for multiple agents
  When the operator opens the settlement report
  Then the report should display:
    | field                 | visible |
    | agent breakdown       | yes     |
    | total deposits        | yes     |
    | total withdrawals     | yes     |
    | total commissions     | yes     |
    | net settlement amount | yes     |
    | export to CSV         | yes     |
  And amounts should be calculated in real-time (no EOD batch)
```

---

## 8. Phase 2 Transactions (Deferred — Summary)

Phase 2 BDD scenarios will be fully detailed when implementation begins. The following are placeholder scenarios for traceability.

### Bill Payments
| BDD ID | Scenario | US | FR |
|--------|----------|-----|-----|
| BDD-B01 | Successful JomPAY bill payment (OFF-US, Cash) | US-B01 | FR-7.1, FR-7.5 |
| BDD-B01-EC-01 | Biller reference validation fails | US-B05 | FR-7.5 |
| BDD-B02 | Successful ASTRO RPN bill payment | US-B02 | FR-7.2 |
| BDD-B03 | Successful TM RPN bill payment | US-B03 | FR-7.3 |
| BDD-B04 | Successful EPF i-SARAAN payment | US-B04 | FR-7.4 |

### Prepaid Top-up
| BDD ID | Scenario | US | FR |
|--------|----------|-----|-----|
| BDD-T01 | Successful CELCOM prepaid top-up | US-T01 | FR-8.1, FR-8.3 |
| BDD-T01-EC-01 | Invalid phone number for top-up | US-T03 | FR-8.3 |
| BDD-T02 | Successful M1 prepaid top-up | US-T02 | FR-8.2 |

### DuitNow & JomPAY
| BDD ID | Scenario | US | FR |
|--------|----------|-----|-----|
| BDD-DNOW-01 | Successful DuitNow transfer via mobile number | US-D01 | FR-9.1, FR-9.2 |
| BDD-D02 | Successful JomPAY ON-US payment | US-D02 | FR-7.1 |

### e-Wallet & eSSP
| BDD ID | Scenario | US | FR |
|--------|----------|-----|-----|
| BDD-WAL-01 | Successful Sarawak Pay e-Wallet withdrawal | US-W01 | FR-10.1 |
| BDD-WAL-02 | Successful Sarawak Pay e-Wallet top-up | US-W02 | FR-10.2 |
| BDD-ESSP-01 | Successful eSSP certificate purchase | US-E01 | FR-10.3 |

### Other
| BDD ID | Scenario | US | FR |
|--------|----------|-----|-----|
| BDD-X01 | Successful cashless payment | US-X01 | FR-11.1 |
| BDD-X02 | Successful PIN purchase | US-X02 | FR-11.2 |

---

## 9. Traceability Matrix

### Scenario → User Story → Functional Requirement

| BDD ID | Type | User Story | Functional Requirements |
|--------|------|-----------|------------------------|
| BDD-R01 | HP | US-R01 | FR-1.1, FR-1.4 |
| BDD-R01-PCT | HP | US-R01 | FR-1.1 |
| BDD-R01-EC-01 | EC | US-R01 | FR-1.1 |
| BDD-R01-EC-02 | EC | US-R01 | FR-1.1 |
| BDD-R01-EC-03 | EC | US-R01 | FR-1.4 |
| BDD-R04 | HP | US-R04 | FR-1.4 |
| BDD-R02 | HP | US-R02 | FR-1.2 |
| BDD-R02-EC-01 | EC | US-R02 | FR-1.2 |
| BDD-R02-EC-02 | EC | US-R02 | FR-1.2 |
| BDD-R02-EC-03 | EC | US-R02 | FR-1.2 |
| BDD-R02-EC-04 | EC | US-R02 | FR-1.2 |
| BDD-R03 | HP | US-R03 | FR-1.3 |
| BDD-R03-EC-01 | EC | US-R03 | FR-1.3 |
| BDD-R03-EC-02 | EC | US-R03 | FR-1.3 |
| BDD-R03-EC-03 | EC | US-R03 | FR-1.3 |
| BDD-R03-EC-04 | EC | US-R03 | FR-1.3 |
| BDD-L01 | HP | US-L01 | FR-2.1 |
| BDD-L01-EC-01 | EC | US-L01 | FR-2.1 |
| BDD-L01-EC-02 | EC | US-L01 | FR-2.1 |
| BDD-L02 | HP | US-L02 | FR-2.2 |
| BDD-L03 | HP | US-L03 | FR-2.1, FR-2.3 |
| BDD-L03-EC-01 | EC | US-L03 | FR-2.1, FR-2.3 |
| BDD-L03-EC-02 | EC | US-L03 | FR-2.3 |
| BDD-L04 | HP | US-L04 | FR-5.1 |
| BDD-L04-EC-01 | EC | US-L04 | FR-5.1 |
| BDD-L04-EC-02 | EC | US-L04 | FR-2.4 |
| BDD-W01 | HP | US-L05 | FR-3.1, FR-3.3, FR-3.5 |
| BDD-W01-EC-01 | EC | US-L05 | FR-3.1 |
| BDD-W01-EC-02 | EC | US-L05 | FR-3.4 |
| BDD-W01-EC-03 | EC | US-L05 | FR-3.4 |
| BDD-W01-EC-04 | EC | US-L05 | FR-3.3 |
| BDD-W01-EC-05 | EC | US-L05 | NFR-4.2 |
| BDD-W01-EC-06 | EC | US-L05 | NFR-4.2 |
| BDD-W01-EC-07 | EC | US-L05 | FR-2.4 |
| BDD-W01-EC-08 | EC | US-L05 | FR-3.4 |
| BDD-D01 | HP | US-L07 | FR-2.5, FR-4.1, FR-4.3 |
| BDD-D01-EC-01 | EC | US-L07 | FR-2.5 |
| BDD-D01-EC-02 | EC | US-L07 | FR-4.1 |
| BDD-D01-EC-03 | EC | US-L07 | FR-4.1 |
| BDD-D01-EC-04 | EC | US-L07 | FR-2.1 |
| BDD-O01 | HP | US-O01 | FR-6.1 |
| BDD-O02 | HP | US-O02 | FR-6.2, FR-6.3 |
| BDD-O03 | HP | US-O03 | FR-6.3, FR-6.4 |
| BDD-O03-EC-01 | EC | US-O03 | FR-6.3, FR-6.4 |
| BDD-O03-EC-02 | EC | US-O03 | FR-6.3 |
| BDD-O01-EC-01 | EC | US-O01 | FR-6.1 |
| BDD-O01-EC-02 | EC | US-O01 | FR-6.1 |
| BDD-O01-EC-03 | EC | US-O01 | FR-6.3 |
| BDD-O02-EC-01 | EC | US-O02 | FR-6.2, FR-6.4 |
| BDD-O02-EC-02 | EC | US-O02 | FR-6.4 |
| BDD-O02-EC-03 | EC | US-O02 | FR-6.2 |
| BDD-O02-EC-04 | EC | US-O02 | FR-6.1 |
| BDD-G01 | HP | US-G01 | FR-12.1 |
| BDD-G02 | HP | US-G02 | FR-12.2 |
| BDD-G01-EC-01 | EC | US-G02 | FR-12.2 |
| BDD-G01-EC-02 | EC | US-G02 | FR-12.2 |
| BDD-G01-EC-03 | EC | US-G01 | NFR-2.2 |
| BDD-BO01 | HP | US-BO01 | FR-13.1 |
| BDD-BO02 | HP | US-BO02 | FR-13.2 |
| BDD-BO03 | HP | US-BO03 | FR-13.3 |
| BDD-BO01-EC-01 | EC | US-BO01 | FR-13.1 |
| BDD-BO01-EC-02 | EC | US-BO01 | FR-13.1 |

### Summary

| Category | Count |
|----------|-------|
| Happy Path (MVP) | 17 |
| Edge Case (MVP) | 33 |
| Phase 2 (Placeholder) | 14 |
| **Total** | **64** |

### Error Code Reference

| Error Code | Used In |
|-----------|---------|
| ERR_FEE_CONFIG_NOT_FOUND | BDD-R01-EC-01 |
| ERR_FEE_CONFIG_EXPIRED | BDD-R01-EC-02 |
| ERR_FEE_COMPONENTS_MISMATCH | BDD-R01-EC-03 |
| ERR_LIMIT_EXCEEDED | BDD-R02-EC-01, BDD-W01-EC-04 |
| ERR_COUNT_LIMIT_EXCEEDED | BDD-R02-EC-02 |
| ERR_INVALID_AMOUNT | BDD-R02-EC-03, BDD-R02-EC-04, BDD-D01-EC-02, BDD-D01-EC-03 |
| ERR_VELOCITY_COUNT_EXCEEDED | BDD-R03-EC-01 |
| ERR_VELOCITY_AMOUNT_EXCEEDED | BDD-R03-EC-02 |
| ERR_AGENT_FLOAT_NOT_FOUND | BDD-L01-EC-01 |
| ERR_AGENT_DEACTIVATED | BDD-L01-EC-02 |
| ERR_INSUFFICIENT_FLOAT | BDD-L03-EC-01, BDD-L03-EC-02 |
| ERR_INVALID_PIN | BDD-L04-EC-01, BDD-W01-EC-01 |
| ERR_INVALID_ACCOUNT | BDD-D01-EC-01 |
| ERR_FLOAT_CAP_EXCEEDED | BDD-D01-EC-04 |
| ERR_MYKAD_NOT_FOUND | BDD-O01-EC-01 |
| ERR_KYC_SERVICE_UNAVAILABLE | BDD-O01-EC-02 |
| ERR_BIOMETRIC_SCANNER_UNAVAILABLE | BDD-O02-EC-03 |
| ERR_INVALID_MYKAD_FORMAT | BDD-O02-EC-04 |
| ERR_TOKEN_EXPIRED | BDD-G01-EC-01 |
| ERR_MISSING_TOKEN | BDD-G01-EC-02 |
| ERR_SERVICE_UNAVAILABLE | BDD-G01-EC-03 |
| ERR_GEOFENCE_VIOLATION | BDD-W01-EC-05 |
| ERR_GPS_UNAVAILABLE | BDD-W01-EC-06 |
| ERR_DUPLICATE_AGENT | BDD-BO01-EC-01 |
| ERR_AGENT_HAS_PENDING_TRANSACTIONS | BDD-BO01-EC-02 |
