# Behavior-Driven Development Specification — Addendum: Missing Transaction Types (Phase 3+)
## Agent Banking Platform (Malaysia)

**Version:** 1.0
**Date:** 2026-04-06
**Status:** Draft — Pending Review
**Supplements:** `2026-04-05-transaction-bdd-addendum.md` (workflow BDD), `2026-03-25-agent-banking-platform-bdd.md` (original BDD)
**BRD Reference:** `2026-03-25-agent-banking-platform-brd.md` (FR-8, FR-10, FR-11, FR-15)
**Design Reference:** `2026-04-06-missing-transaction-types-design.md`

---

## Scope

This addendum covers BDD scenarios for the 9 missing transaction types implemented in Phase 3+:
- **Simple Group:** CASHLESS_PAYMENT, PIN_BASED_PURCHASE
- **Medium Group:** PREPAID_TOPUP, EWALLET_WITHDRAWAL, EWALLET_TOPUP, ESSP_PURCHASE, PIN_PURCHASE
- **Complex Group:** RETAIL_SALE, HYBRID_CASHBACK

All scenarios follow the SAGA pattern with explicit float operations, compensation steps, and state transitions.

---

## Scenario Classification

- **[HP]** = Happy Path (optimal user flow)
- **[EC]** = Edge Case (boundary, invalid, or extreme scenarios)

---

## 1. Workflow Router Extensions (BDD-TO-EXT)

### BDD-TO-07 [HP] — US-TO01, FR-11.1
```gherkin
Scenario: Router dispatches cashless payment to CashlessPaymentWorkflow
  Given a transaction request with:
    | field              | value              |
    | transactionType    | CASHLESS_PAYMENT   |
    | pan                | 411111******1111   |
    | merchantCategoryCode | 4900             |
  When the WorkflowRouter processes the request
  Then it should select CashlessPaymentWorkflow
  And start a Temporal workflow with workflowId = idempotencyKey
```

### BDD-TO-08 [HP] — US-TO01, FR-11.2
```gherkin
Scenario: Router dispatches PIN-based purchase to PinBasedPurchaseWorkflow
  Given a transaction request with:
    | field              | value                |
    | transactionType    | PIN_BASED_PURCHASE   |
    | pan                | 411111******1111     |
    | merchantCategoryCode | 5411               |
  When the WorkflowRouter processes the request
  Then it should select PinBasedPurchaseWorkflow
  And start a Temporal workflow with workflowId = idempotencyKey
```

### BDD-TO-09 [HP] — US-TO01, FR-8.1
```gherkin
Scenario: Router dispatches prepaid top-up to PrepaidTopupWorkflow
  Given a transaction request with:
    | field              | value              |
    | transactionType    | PREPAID_TOPUP      |
    | telcoProvider      | CELCOM             |
    | phoneNumber        | 0191234567         |
  When the WorkflowRouter processes the request
  Then it should select PrepaidTopupWorkflow
  And start a Temporal workflow with workflowId = idempotencyKey
```

### BDD-TO-10 [HP] — US-TO01, FR-10.1
```gherkin
Scenario: Router dispatches eWallet withdrawal to EWalletWithdrawalWorkflow
  Given a transaction request with:
    | field              | value                |
    | transactionType    | EWALLET_WITHDRAWAL   |
    | ewalletProvider    | SARAWAK_PAY          |
    | ewalletId          | SP-123456            |
  When the WorkflowRouter processes the request
  Then it should select EWalletWithdrawalWorkflow
  And start a Temporal workflow with workflowId = idempotencyKey
```

### BDD-TO-11 [HP] — US-TO01, FR-10.2
```gherkin
Scenario: Router dispatches eWallet top-up to EWalletTopupWorkflow
  Given a transaction request with:
    | field              | value              |
    | transactionType    | EWALLET_TOPUP      |
    | ewalletProvider    | SARAWAK_PAY        |
    | ewalletId          | SP-123456          |
  When the WorkflowRouter processes the request
  Then it should select EWalletTopupWorkflow
  And start a Temporal workflow with workflowId = idempotencyKey
```

### BDD-TO-12 [HP] — US-TO01, FR-10.3
```gherkin
Scenario: Router dispatches eSSP purchase to ESSPPurchaseWorkflow
  Given a transaction request with:
    | field              | value              |
    | transactionType    | ESSP_PURCHASE      |
    | amount             | 100.00             |
  When the WorkflowRouter processes the request
  Then it should select ESSPPurchaseWorkflow
  And start a Temporal workflow with workflowId = idempotencyKey
```

### BDD-TO-13 [HP] — US-TO01, FR-15.2
```gherkin
Scenario: Router dispatches PIN purchase to PINPurchaseWorkflow
  Given a transaction request with:
    | field              | value              |
    | transactionType    | PIN_PURCHASE       |
    | pinProvider        | DIGI               |
    | faceValue          | 10.00              |
  When the WorkflowRouter processes the request
  Then it should select PINPurchaseWorkflow
  And start a Temporal workflow with workflowId = idempotencyKey
```

### BDD-TO-14 [HP] — US-TO01, FR-15.1
```gherkin
Scenario: Router dispatches retail sale to RetailSaleWorkflow
  Given a transaction request with:
    | field              | value              |
    | transactionType    | RETAIL_SALE        |
    | paymentMethod      | CARD               |
    | pan                | 411111******1111   |
  When the WorkflowRouter processes the request
  Then it should select RetailSaleWorkflow
  And start a Temporal workflow with workflowId = idempotencyKey
```

### BDD-TO-15 [HP] — US-TO01, FR-15.5
```gherkin
Scenario: Router dispatches hybrid cashback to HybridCashbackWorkflow
  Given a transaction request with:
    | field              | value              |
    | transactionType    | HYBRID_CASHBACK    |
    | saleAmount         | 20.00              |
    | cashbackAmount     | 50.00              |
    | pan                | 411111******1111   |
  When the WorkflowRouter processes the request
  Then it should select HybridCashbackWorkflow
  And start a Temporal workflow with workflowId = idempotencyKey
```

---

## 2. CASHLESS_PAYMENT Scenarios (BDD-WF-CP)

### BDD-WF-HP-CP01 [HP] — US-X01, FR-11.1
```gherkin
Scenario: Cashless payment completes successfully via card
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "5000.00"
  And a FeeConfig for CASHLESS_PAYMENT STANDARD:
    | field            | value  |
    | customerFeeValue | 1.00   |
  And the customer presents a valid EMV card with correct PIN
  And the merchantCategoryCode is "4900" (utilities)
  When the POS terminal requests CASHLESS_PAYMENT of RM 150.00 with idempotency key "IDEM-CP01"
  Then the WorkflowRouter should start CashlessPaymentWorkflow
  And the CheckVelocityActivity should pass
  And the EvaluateStpActivity should return FULL_STP
  And the CalculateFeesActivity should return customerFee=1.00
  And the AuthorizeAtSwitchActivity should return APPROVED with referenceCode "SWITCH-REF-001"
  And the PublishKafkaEventActivity should publish transaction-completed event
  And the TransactionRecord should have:
    | field              | value              |
    | workflowId         | IDEM-CP01          |
    | transactionType    | CASHLESS_PAYMENT   |
    | amount             | 150.00             |
    | customerFee        | 1.00               |
    | status             | COMPLETED          |
    | externalReference  | SWITCH-REF-001     |
  And AgentFloat.balance should remain "5000.00" (no float operation)
```

### BDD-WF-EC-CP01 [EC] — US-X01, FR-11.1
```gherkin
Scenario: Cashless payment — customer card declined — no compensation needed
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer presents a debit card with incorrect PIN
  When the POS terminal requests CASHLESS_PAYMENT of RM 150.00
  Then the CheckVelocityActivity should pass
  And the CalculateFeesActivity should return fees
  And the AuthorizeAtSwitchActivity should return DECLINED with responseCode "55"
  And no compensation should be triggered (no float blocked)
  And the TransactionRecord should have:
    | field     | value            |
    | status    | FAILED           |
    | errorCode | ERR_INVALID_PIN  |
  And AgentFloat.balance should remain "5000.00"
```

### BDD-WF-EC-CP02 [EC] — US-X01, FR-11.1
```gherkin
Scenario: Cashless payment — switch timeout — Safety Reversal triggered
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the switch does not respond within 25 seconds
  When the POS terminal requests CASHLESS_PAYMENT of RM 150.00
  Then the AuthorizeAtSwitchActivity should timeout after 25s
  And the SendReversalToSwitchActivity should be triggered (MTI 0400)
  And the TransactionRecord should have:
    | field     | value               |
    | status    | FAILED              |
    | errorCode | ERR_NETWORK_TIMEOUT |
  And if SendReversalToSwitchActivity fails, it should retry every 60 seconds indefinitely
  And AgentFloat.balance should remain "5000.00"
```

---

## 3. PIN_BASED_PURCHASE Scenarios (BDD-WF-PBP)

### BDD-WF-HP-PBP01 [HP] — US-X02, FR-11.2
```gherkin
Scenario: PIN-based purchase completes successfully
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "5000.00"
  And a FeeConfig for PIN_BASED_PURCHASE STANDARD:
    | field            | value  |
    | customerFeeValue | 0.50   |
  And the customer presents a valid EMV card with correct PIN
  And the merchantCategoryCode is "5411" (grocery)
  When the POS terminal requests PIN_BASED_PURCHASE of RM 75.00 with idempotency key "IDEM-PBP01"
  Then the WorkflowRouter should start PinBasedPurchaseWorkflow
  And the CheckVelocityActivity should pass
  And the EvaluateStpActivity should return FULL_STP
  And the CalculateFeesActivity should return customerFee=0.50
  And the AuthorizeAtSwitchActivity should return APPROVED with referenceCode "SWITCH-REF-002"
  And the TransactionRecord should have:
    | field              | value                |
    | workflowId         | IDEM-PBP01           |
    | transactionType    | PIN_BASED_PURCHASE   |
    | amount             | 75.00                |
    | customerFee        | 0.50                 |
    | status             | COMPLETED            |
    | externalReference  | SWITCH-REF-002       |
  And AgentFloat.balance should remain "5000.00" (no float operation)
```

### BDD-WF-EC-PBP01 [EC] — US-X02, FR-11.2
```gherkin
Scenario: PIN-based purchase — insufficient card balance — no compensation needed
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer's card has insufficient balance
  When the POS terminal requests PIN_BASED_PURCHASE of RM 75.00
  Then the AuthorizeAtSwitchActivity should return DECLINED with responseCode "51"
  And no compensation should be triggered
  And the TransactionRecord should have:
    | field     | value                   |
    | status    | FAILED                  |
    | errorCode | ERR_INSUFFICIENT_FUNDS  |
  And AgentFloat.balance should remain "5000.00"
```

---

## 4. PREPAID_TOPUP Scenarios (BDD-WF-PT)

### BDD-WF-HP-PT01 [HP] — US-T01, FR-8.1
```gherkin
Scenario: CELCOM prepaid top-up via cash — SAGA happy path
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "5000.00"
  And a FeeConfig for PREPAID_TOPUP STANDARD:
    | field            | value  |
    | customerFeeValue | 0.50   |
  And the customer provides phone number "0191234567"
  And the CELCOM aggregator validates the number
  When the customer hands RM 30.00 cash and confirms top-up with idempotency key "IDEM-PT01"
  Then the WorkflowRouter should start PrepaidTopupWorkflow
  And the CheckVelocityActivity should pass
  And the EvaluateStpActivity should return FULL_STP
  And the CalculateFeesActivity should return customerFee=0.50
  And the ValidatePhoneNumberActivity should return valid with operatorName "CELCOM"
  And the BlockFloatActivity should reserve RM 30.50 (amount + fees)
  And the TopUpTelcoActivity should return success with telcoReference "CELCOM-REF-001"
  And the CommitFloatActivity should commit RM 30.50
  And the TransactionRecord should have:
    | field              | value            |
    | workflowId         | IDEM-PT01        |
    | transactionType    | PREPAID_TOPUP    |
    | amount             | 30.00            |
    | customerFee        | 0.50             |
    | status             | COMPLETED        |
    | externalReference  | CELCOM-REF-001   |
  And AgentFloat.balance should be "4969.50"
```

### BDD-WF-HP-PT02 [HP] — US-T01, FR-8.1
```gherkin
Scenario: CELCOM prepaid top-up via card — card-funded path
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer provides phone number "0191234567"
  And the customer presents a valid EMV card with correct PIN
  When the POS terminal confirms CELCOM top-up of RM 30.00
  Then the WorkflowRouter should start PrepaidTopupWorkflow
  And the ValidatePhoneNumberActivity should return valid
  And the AuthorizeAtSwitchActivity should return APPROVED
  And the TopUpTelcoActivity should return success
  And the TransactionRecord should have status COMPLETED
  And AgentFloat.balance should remain "5000.00" (card-funded)
```

### BDD-WF-EC-PT01 [EC] — US-T03, FR-8.3
```gherkin
Scenario: CELCOM top-up — invalid phone number — early failure
  Given the customer provides phone number "0199999999"
  And the CELCOM aggregator returns "INVALID_NUMBER"
  When the POS terminal attempts CELCOM top-up
  Then the ValidatePhoneNumberActivity should throw InvalidPhoneNumberException
  And no float should be blocked
  And the TransactionRecord should have:
    | field     | value                     |
    | status    | FAILED                    |
    | errorCode | ERR_INVALID_PHONE_NUMBER  |
  And AgentFloat should not be modified
```

### BDD-WF-EC-PT02 [EC] — US-T01, FR-8.1
```gherkin
Scenario: CELCOM top-up — aggregator timeout — compensation releases float
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the BlockFloatActivity has reserved RM 30.50
  And the CELCOM aggregator does not respond within 30 seconds
  When the TopUpTelcoActivity times out
  Then the ReleaseFloatActivity should release RM 30.50
  And the TransactionRecord should have:
    | field     | value                   |
    | status    | FAILED                  |
    | errorCode | ERR_AGGREGATOR_TIMEOUT  |
  And AgentFloat.balance should be restored to "5000.00"
```

### BDD-WF-HP-PT03 [HP] — US-T02, FR-8.2
```gherkin
Scenario: M1 prepaid top-up via cash — SAGA happy path
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "5000.00"
  And a FeeConfig for PREPAID_TOPUP STANDARD:
    | field            | value  |
    | customerFeeValue | 0.50   |
  And the customer provides phone number "0178765432"
  And the M1 aggregator validates the number
  When the customer hands RM 20.00 cash and confirms top-up
  Then the WorkflowRouter should start PrepaidTopupWorkflow
  And the ValidatePhoneNumberActivity should return valid with operatorName "M1"
  And the BlockFloatActivity should reserve RM 20.50
  And the TopUpTelcoActivity should return success with telcoReference "M1-REF-001"
  And the CommitFloatActivity should commit RM 20.50
  And the TransactionRecord should have status COMPLETED
  And AgentFloat.balance should be "4979.50"
```

### BDD-WF-EC-PT03 [EC] — US-T02, FR-8.3
```gherkin
Scenario: M1 top-up — invalid phone number — early failure
  Given the customer provides phone number "0170000000"
  And the M1 aggregator returns "INVALID_NUMBER"
  When the POS terminal attempts M1 top-up
  Then the ValidatePhoneNumberActivity should throw InvalidPhoneNumberException
  And no float should be blocked
  And the TransactionRecord should have:
    | field     | value                     |
    | status    | FAILED                    |
    | errorCode | ERR_INVALID_PHONE_NUMBER  |
```

---

## 5. EWALLET_WITHDRAWAL Scenarios (BDD-WF-EWW)

### BDD-WF-HP-EWW01 [HP] — US-W01, FR-10.1
```gherkin
Scenario: Sarawak Pay e-Wallet withdrawal via cash — SAGA happy path
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "5000.00"
  And a FeeConfig for EWALLET_WITHDRAWAL STANDARD:
    | field            | value  |
    | customerFeeValue | 1.00   |
  And the customer provides Sarawak Pay ID "SP-123456"
  And the Sarawak Pay system validates the wallet
  When the customer hands RM 200.00 cash and confirms withdrawal with idempotency key "IDEM-EWW01"
  Then the WorkflowRouter should start EWalletWithdrawalWorkflow
  And the CheckVelocityActivity should pass
  And the EvaluateStpActivity should return FULL_STP
  And the CalculateFeesActivity should return customerFee=1.00
  And the ValidateEWalletActivity should return valid with walletBalance=500.00
  And the BlockFloatActivity should reserve RM 201.00 (amount + fees)
  And the WithdrawFromEWalletActivity should return success with ewalletReference "SP-WD-001"
  And the CommitFloatActivity should commit RM 201.00
  And the TransactionRecord should have:
    | field              | value                |
    | workflowId         | IDEM-EWW01           |
    | transactionType    | EWALLET_WITHDRAWAL   |
    | amount             | 200.00               |
    | customerFee        | 1.00                 |
    | status             | COMPLETED            |
    | externalReference  | SP-WD-001            |
  And AgentFloat.balance should be "5201.00" (float increases — agent receives cash)
```

### BDD-WF-HP-EWW02 [HP] — US-W01, FR-10.1
```gherkin
Scenario: Sarawak Pay e-Wallet withdrawal via card — card-funded path
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer presents a valid EMV card with correct PIN
  And the customer provides Sarawak Pay ID "SP-123456"
  When the POS terminal confirms withdrawal of RM 100.00
  Then the WorkflowRouter should start EWalletWithdrawalWorkflow
  And the ValidateEWalletActivity should return valid
  And the AuthorizeAtSwitchActivity should return APPROVED
  And the WithdrawFromEWalletActivity should return success
  And the TransactionRecord should have status COMPLETED
  And AgentFloat.balance should remain "5000.00" (card-funded)
```

### BDD-WF-EC-EWW01 [EC] — US-W01, FR-10.1
```gherkin
Scenario: Sarawak Pay withdrawal — insufficient eWallet balance — compensation releases float
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the BlockFloatActivity has reserved RM 201.00
  And the customer's Sarawak Pay wallet has only RM 50.00
  And the withdrawal amount is RM 200.00
  When the WithdrawFromEWalletActivity executes
  Then it should throw EWalletInsufficientException
  And the ReleaseFloatActivity should release RM 201.00
  And the TransactionRecord should have:
    | field     | value                    |
    | status    | FAILED                   |
    | errorCode | ERR_WALLET_INSUFFICIENT  |
  And AgentFloat.balance should be restored to "5000.00"
```

---

## 6. EWALLET_TOPUP Scenarios (BDD-WF-EWT)

### BDD-WF-HP-EWT01 [HP] — US-W02, FR-10.2
```gherkin
Scenario: Sarawak Pay e-Wallet top-up via cash — SAGA happy path
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "5000.00"
  And a FeeConfig for EWALLET_TOPUP STANDARD:
    | field            | value  |
    | customerFeeValue | 0.50   |
  And the customer provides Sarawak Pay ID "SP-123456"
  And the Sarawak Pay system validates the wallet
  When the customer hands RM 50.00 cash and confirms top-up with idempotency key "IDEM-EWT01"
  Then the WorkflowRouter should start EWalletTopupWorkflow
  And the CheckVelocityActivity should pass
  And the EvaluateStpActivity should return FULL_STP
  And the CalculateFeesActivity should return customerFee=0.50
  And the ValidateEWalletActivity should return valid
  And the BlockFloatActivity should reserve RM 50.50 (amount + fees)
  And the TopUpEWalletActivity should return success with ewalletReference "SP-TP-001"
  And the CommitFloatActivity should commit RM 50.50
  And the TransactionRecord should have:
    | field              | value              |
    | workflowId         | IDEM-EWT01         |
    | transactionType    | EWALLET_TOPUP      |
    | amount             | 50.00              |
    | customerFee        | 0.50               |
    | status             | COMPLETED          |
    | externalReference  | SP-TP-001          |
  And AgentFloat.balance should be "4949.50" (float decreases — agent pays eWallet)
```

### BDD-WF-HP-EWT02 [HP] — US-W02, FR-10.2
```gherkin
Scenario: Sarawak Pay e-Wallet top-up via card — card-funded path
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer presents a valid EMV card with correct PIN
  And the customer provides Sarawak Pay ID "SP-123456"
  When the POS terminal confirms top-up of RM 50.00
  Then the WorkflowRouter should start EWalletTopupWorkflow
  And the ValidateEWalletActivity should return valid
  And the AuthorizeAtSwitchActivity should return APPROVED
  And the TopUpEWalletActivity should return success
  And the TransactionRecord should have status COMPLETED
  And AgentFloat.balance should remain "5000.00" (card-funded)
```

### BDD-WF-EC-EWT01 [EC] — US-W02, FR-10.2
```gherkin
Scenario: Sarawak Pay top-up — eWallet provider timeout — compensation releases float
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the BlockFloatActivity has reserved RM 50.50
  And the Sarawak Pay system does not respond within 15 seconds
  When the TopUpEWalletActivity times out
  Then the ReleaseFloatActivity should release RM 50.50
  And the TransactionRecord should have:
    | field     | value                  |
    | status    | FAILED                 |
    | errorCode | ERR_EWALLET_TIMEOUT    |
  And AgentFloat.balance should be restored to "5000.00"
```

---

## 7. ESSP_PURCHASE Scenarios (BDD-WF-ESSP)

### BDD-WF-HP-ESSP01 [HP] — US-E01, FR-10.3
```gherkin
Scenario: eSSP certificate purchase via cash — SAGA happy path
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "5000.00"
  And a FeeConfig for ESSP_PURCHASE STANDARD:
    | field            | value  |
    | customerFeeValue | 0.00   |
  And the customer requests eSSP purchase of RM 100.00
  And the BSN system validates the purchase amount
  When the customer hands RM 100.00 cash with idempotency key "IDEM-ESSP01"
  Then the WorkflowRouter should start ESSPPurchaseWorkflow
  And the CheckVelocityActivity should pass
  And the EvaluateStpActivity should return FULL_STP
  And the CalculateFeesActivity should return customerFee=0.00
  And the ValidateESSPPurchaseActivity should return valid with minAmount=10.00, maxAmount=50000.00
  And the BlockFloatActivity should reserve RM 100.00
  And the PurchaseESSPActivity should return success with certificateNumber "ESSP-2026-001"
  And the CommitFloatActivity should commit RM 100.00
  And the TransactionRecord should have:
    | field              | value            |
    | workflowId         | IDEM-ESSP01      |
    | transactionType    | ESSP_PURCHASE    |
    | amount             | 100.00           |
    | status             | COMPLETED        |
    | externalReference  | ESSP-2026-001    |
  And AgentFloat.balance should be "4900.00"
  And a receipt with the eSSP certificate details should be generated
```

### BDD-WF-HP-ESSP02 [HP] — US-E01, FR-10.3
```gherkin
Scenario: eSSP certificate purchase via card — card-funded path
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer presents a valid EMV card with correct PIN
  When the POS terminal confirms eSSP purchase of RM 100.00
  Then the WorkflowRouter should start ESSPPurchaseWorkflow
  And the ValidateESSPPurchaseActivity should return valid
  And the AuthorizeAtSwitchActivity should return APPROVED
  And the PurchaseESSPActivity should return success with certificateNumber
  And the TransactionRecord should have status COMPLETED
  And AgentFloat.balance should remain "5000.00" (card-funded)
```

### BDD-WF-EC-ESSP01 [EC] — US-E01, FR-10.3
```gherkin
Scenario: eSSP purchase — BSN system unavailable — compensation releases float
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the BlockFloatActivity has reserved RM 100.00
  And the BSN system returns timeout or 503 error
  When the PurchaseESSPActivity executes
  Then it should throw ESSPServiceUnavailableException
  And the ReleaseFloatActivity should release RM 100.00
  And the TransactionRecord should have:
    | field     | value                        |
    | status    | FAILED                       |
    | errorCode | ERR_ESSP_SERVICE_UNAVAILABLE |
  And AgentFloat.balance should be restored to "5000.00"
```

---

## 8. PIN_PURCHASE Scenarios (BDD-WF-PIN)

### BDD-WF-HP-PIN01 [HP] — US-M02, FR-15.2
```gherkin
Scenario: PIN purchase (digital voucher) via cash — SAGA happy path
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "5000.00"
  And a FeeConfig for PIN_PURCHASE STANDARD:
    | field                | value  |
    | customerFeeValue     | 0.50   |
    | agentCommissionValue | 0.50   |
  And the customer requests a Digi RM 10 prepaid PIN
  And the PIN inventory for Digi RM 10 has stock > 0
  When the customer hands RM 10.50 cash (face value + fee) with idempotency key "IDEM-PIN01"
  Then the WorkflowRouter should start PINPurchaseWorkflow
  And the CheckVelocityActivity should pass
  And the EvaluateStpActivity should return FULL_STP
  And the CalculateFeesActivity should return customerFee=0.50, agentCommission=0.50
  And the ValidatePINInventoryActivity should return available with stockCount=100
  And the BlockFloatActivity should reserve RM 10.00 (face value only)
  And the GeneratePINActivity should return success with pinCode="1234567890123456", serialNumber="SN-001"
  And the CommitFloatActivity should commit RM 10.00
  And the TransactionRecord should have:
    | field              | value         |
    | workflowId         | IDEM-PIN01    |
    | transactionType    | PIN_PURCHASE  |
    | amount             | 10.00         |
    | agentCommission    | 0.50          |
    | status             | COMPLETED     |
  And a 16-digit PIN code should be generated and printed on a slip
  And AgentFloat.balance should be "4990.00"
```

### BDD-WF-HP-PIN02 [HP] — US-M02, FR-15.2
```gherkin
Scenario: PIN purchase via card — card-funded path
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer presents a valid debit card with correct PIN
  And the PIN inventory for Digi RM 25 has stock > 0
  When the POS terminal processes PIN_PURCHASE of RM 25.00
  Then the WorkflowRouter should start PINPurchaseWorkflow
  And the ValidatePINInventoryActivity should return available
  And the AuthorizeAtSwitchActivity should return APPROVED
  And the GeneratePINActivity should return success with pinCode
  And the TransactionRecord should have status COMPLETED
  And AgentFloat.balance should be "4975.00" (agent pays from float, customer pays via card)
```

### BDD-WF-EC-PIN01 [EC] — US-M02, FR-15.2
```gherkin
Scenario: PIN purchase — digital inventory depleted — early failure
  Given the bank's digital PIN inventory for Digi RM 10 is depleted
  When the customer requests a Digi RM 10 PIN
  Then the ValidatePINInventoryActivity should throw PINInventoryDepletedException
  And no float should be blocked
  And the TransactionRecord should have:
    | field     | value                          |
    | status    | FAILED                         |
    | errorCode | ERR_PIN_INVENTORY_DEPLETED     |
  And AgentFloat should not be modified
```

### BDD-WF-EC-PIN02 [EC] — US-M02, FR-15.2
```gherkin
Scenario: PIN purchase — PIN generation fails — compensation releases float
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the BlockFloatActivity has reserved RM 10.00
  And the PIN inventory was available
  And the GeneratePINActivity throws PINGenerationFailedException
  When the workflow processes the failure
  Then the ReleaseFloatActivity should release RM 10.00
  And the TransactionRecord should have:
    | field     | value                       |
    | status    | FAILED                      |
    | errorCode | ERR_PIN_GENERATION_FAILED   |
  And AgentFloat.balance should be restored to "5000.00"
```

---

## 9. RETAIL_SALE Scenarios (BDD-WF-RS)

### BDD-WF-HP-RS01 [HP] — US-M01, FR-15.1
```gherkin
Scenario: Retail sale via debit card — SAGA happy path
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "5000.00"
  And a FeeConfig for RETAIL_SALE STANDARD with MDR rate 1%
  And the customer purchases goods worth RM 100.00
  And the customer presents a valid debit card with correct PIN
  When the POS terminal processes RETAIL_SALE of RM 100.00 with idempotency key "IDEM-RS01"
  Then the WorkflowRouter should start RetailSaleWorkflow
  And the CheckVelocityActivity should pass
  And the EvaluateStpActivity should return FULL_STP
  And the CalculateMDRActivity should return mdrRate=0.01, mdrAmount=1.00, netCreditToFloat=99.00
  And the AuthorizeAtSwitchActivity should return APPROVED with referenceCode "SWITCH-REF-003"
  And the CreditAgentFloatActivity should credit RM 99.00 (after 1% MDR)
  And the CreateMerchantTransactionRecordActivity should create record:
    | field            | value          |
    | merchantType     | RETAIL_SALE    |
    | grossAmount      | 100.00         |
    | mdrRate          | 0.01           |
    | mdrAmount        | 1.00           |
    | netCreditToFloat | 99.00          |
    | receiptType      | SALES_RECEIPT  |
  And the TransactionRecord should have:
    | field              | value         |
    | workflowId         | IDEM-RS01     |
    | transactionType    | RETAIL_SALE   |
    | amount             | 100.00        |
    | status             | COMPLETED     |
  And AgentFloat.balance should be "5099.00"
```

### BDD-WF-HP-RS02 [HP] — US-M01, FR-15.1
```gherkin
Scenario: Retail sale via DuitNow QR — SAGA happy path
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "5000.00"
  And a FeeConfig for RETAIL_SALE STANDARD with MDR rate 1.5%
  And the customer purchases goods worth RM 50.00
  When the POS terminal generates a Dynamic QR code
  And the customer scans and confirms via their banking app
  And the QR payment is confirmed
  Then the WorkflowRouter should start RetailSaleWorkflow
  And the CalculateMDRActivity should return mdrRate=0.015, mdrAmount=0.75, netCreditToFloat=49.25
  And the GenerateDynamicQRActivity should return qrCode and qrReference
  And the WaitForQRPaymentActivity should return success with paynetReference
  And the CreditAgentFloatActivity should credit RM 49.25
  And the TransactionRecord should have status COMPLETED
  And AgentFloat.balance should be "5049.25"
```

### BDD-WF-HP-RS03 [HP] — US-M01, FR-15.1
```gherkin
Scenario: Retail sale via DuitNow Request-to-Pay — SAGA happy path
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "5000.00"
  And a FeeConfig for RETAIL_SALE STANDARD with MDR rate 1%
  And the customer provides mobile number "0123456789"
  And the Agent enters RM 45.00 on the POS
  When the API Gateway fires a DuitNow Request to the customer's bank
  And the customer receives a push notification and approves via FaceID
  And the system receives a "Cleared" confirmation
  Then the WorkflowRouter should start RetailSaleWorkflow
  And the CalculateMDRActivity should return mdrRate=0.01, mdrAmount=0.45, netCreditToFloat=44.55
  And the SendRequestToPayActivity should return success with rtpReference
  And the WaitForRTPApprovalActivity should return success
  And the CreditAgentFloatActivity should credit RM 44.55
  And the TransactionRecord should have status COMPLETED
  And AgentFloat.balance should be "5044.55"
```

### BDD-WF-EC-RS01 [EC] — US-M01, FR-15.1
```gherkin
Scenario: Retail sale — customer card declined — no compensation needed
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the customer presents a debit card with incorrect PIN
  When the POS terminal processes RETAIL_SALE of RM 100.00
  Then the AuthorizeAtSwitchActivity should return DECLINED
  And no compensation should be triggered (no float blocked)
  And the TransactionRecord should have:
    | field     | value            |
    | status    | FAILED           |
    | errorCode | ERR_INVALID_PIN  |
  And AgentFloat.balance should remain "5000.00"
```

### BDD-WF-EC-RS02 [EC] — US-M01, FR-15.1
```gherkin
Scenario: Retail sale via QR — payment timeout — no compensation needed
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the Dynamic QR has been generated
  And the customer does not scan the QR within 120 seconds
  When the WaitForQRPaymentActivity times out
  Then it should throw QRPaymentTimeoutException
  And no compensation should be triggered (no float blocked)
  And the TransactionRecord should have:
    | field     | value                   |
    | status    | FAILED                  |
    | errorCode | ERR_QR_PAYMENT_TIMEOUT  |
  And AgentFloat.balance should remain "5000.00"
```

### BDD-WF-EC-RS03 [EC] — US-M01, FR-15.1
```gherkin
Scenario: Retail sale via RTP — customer declines — no compensation needed
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the Request-to-Pay has been sent to customer's bank
  And the customer declines the payment request
  When the WaitForRTPApprovalActivity receives DECLINED status
  Then it should throw RTPDeclinedException
  And no compensation should be triggered
  And the TransactionRecord should have:
    | field     | value                |
    | status    | FAILED               |
    | errorCode | ERR_RTP_DECLINED     |
  And AgentFloat.balance should remain "5000.00"
```

### BDD-WF-EC-RS04 [EC] — US-M01, FR-15.1
```gherkin
Scenario: Retail sale — switch timeout — Safety Reversal triggered
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the switch does not respond within 25 seconds
  When the POS terminal processes RETAIL_SALE of RM 100.00
  Then the AuthorizeAtSwitchActivity should timeout
  And the SendReversalToSwitchActivity should be triggered (MTI 0400)
  And the TransactionRecord should have:
    | field     | value               |
    | status    | FAILED              |
    | errorCode | ERR_NETWORK_TIMEOUT |
  And AgentFloat.balance should remain "5000.00"
```

---

## 10. HYBRID_CASHBACK Scenarios (BDD-WF-HCB)

### BDD-WF-HP-HCB01 [HP] — US-M03, FR-15.5
```gherkin
Scenario: Hybrid cashback (purchase + withdrawal) — SAGA happy path
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "5000.00"
  And a FeeConfig for RETAIL_SALE STANDARD with MDR rate 1%
  And the customer purchases RM 20.00 of bread
  And the customer requests RM 50.00 cash back
  And the customer presents a valid debit card with correct PIN
  When the POS terminal processes the hybrid transaction (total RM 70.00) with idempotency key "IDEM-HCB01"
  Then the WorkflowRouter should start HybridCashbackWorkflow
  And the CheckVelocityActivity should pass (totalAmount=70.00)
  And the EvaluateStpActivity should return FULL_STP
  And the CalculateMDRActivity should return mdrRate=0.01, mdrAmount=0.20, netSaleCredit=19.80
  And the ValidateFloatCapacityActivity should return hasCapacity=true
  And the AuthorizeAtSwitchActivity should return APPROVED for RM 70.00
  And the CreditAgentFloatActivity should credit RM 19.80 (sale portion after MDR)
  And the CreditAgentFloatActivity should credit RM 50.00 (cashback portion)
  And the CreateMerchantTransactionRecordActivity should create record:
    | field            | value          |
    | merchantType     | CASH_BACK      |
    | grossAmount      | 70.00          |
    | saleAmount       | 20.00          |
    | cashbackAmount   | 50.00          |
    | mdrRate          | 0.01           |
    | mdrAmount        | 0.20           |
    | netCreditToFloat | 69.80          |
    | receiptType      | CASHBACK_RECEIPT |
  And the TransactionRecord should have:
    | field              | value         |
    | workflowId         | IDEM-HCB01    |
    | transactionType    | HYBRID_CASHBACK |
    | amount             | 70.00         |
    | status             | COMPLETED     |
  And AgentFloat.balance should be "5069.80"
  And the agent should hand RM 50.00 physical cash to the customer
```

### BDD-WF-EC-HCB01 [EC] — US-M03, FR-15.5
```gherkin
Scenario: Hybrid cashback — insufficient agent float capacity — early failure
  Given AgentFloat for "AGT-01" has balance "30.00"
  And the customer requests RM 50.00 cash back
  And the ValidateFloatCapacityActivity checks capacity
  When the POS terminal attempts the hybrid transaction
  Then the ValidateFloatCapacityActivity should throw FloatCapacityCheckFailedException
  And no authorization should be attempted
  And the TransactionRecord should have:
    | field     | value                   |
    | status    | FAILED                  |
    | errorCode | ERR_INSUFFICIENT_FLOAT  |
  And AgentFloat.balance should remain "30.00"
```

### BDD-WF-EC-HCB02 [EC] — US-M03, FR-15.5
```gherkin
Scenario: Hybrid cashback — card declined after float capacity check — no compensation
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the ValidateFloatCapacityActivity returned hasCapacity=true
  And the customer presents a debit card with incorrect PIN
  When the POS terminal processes the hybrid transaction
  Then the AuthorizeAtSwitchActivity should return DECLINED
  And no compensation should be triggered (no float blocked)
  And the TransactionRecord should have:
    | field     | value            |
    | status    | FAILED           |
    | errorCode | ERR_INVALID_PIN  |
  And AgentFloat.balance should remain "5000.00"
```

### BDD-WF-EC-HCB03 [EC] — US-M03, FR-15.5
```gherkin
Scenario: Hybrid cashback — switch timeout — Safety Reversal triggered
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the ValidateFloatCapacityActivity returned hasCapacity=true
  And the switch does not respond within 25 seconds
  When the POS terminal processes the hybrid transaction
  Then the AuthorizeAtSwitchActivity should timeout
  And the SendReversalToSwitchActivity should be triggered (MTI 0400)
  And the TransactionRecord should have:
    | field     | value               |
    | status    | FAILED              |
    | errorCode | ERR_NETWORK_TIMEOUT |
  And AgentFloat.balance should remain "5000.00"
```

### BDD-WF-EC-HCB04 [EC] — US-M03, FR-15.5
```gherkin
Scenario: Hybrid cashback — credit float fails after authorization — compensation reverses both credits
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the AuthorizeAtSwitchActivity returned APPROVED for RM 70.00
  And the CreditAgentFloatActivity credited RM 19.80 (sale portion)
  And the CreditAgentFloatActivity throws FloatCommitFailedException for cashback portion
  When the workflow processes the failure
  Then the ReverseCreditFloatActivity should debit RM 19.80 (sale portion reversal)
  And the SendReversalToSwitchActivity should be triggered
  And the TransactionRecord should have status FAILED
  And AgentFloat.balance should be restored to "5000.00"
```

---

## 11. Cross-Workflow Scenarios (BDD-XWF-EXT)

### BDD-XWF-04 [EC] — US-TO02, FR-19.2
```gherkin
Scenario: PREPAID_TOPUP and EWALLET_TOPUP concurrent workflows do not cause race conditions
  Given Agent "AGT-01" has AgentFloat balance "1000.00"
  And a PREPAID_TOPUP request of RM 30.00 with idempotency key "IDEM-PT01"
  And an EWALLET_TOPUP request of RM 50.00 with idempotency key "IDEM-EWT01"
  When both workflows execute simultaneously
  Then both BlockFloatActivity should succeed (PESSIMISTIC_WRITE lock serializes)
  And both workflows should complete successfully
  And AgentFloat.balance should be "920.00" after both complete (1000 - 30.50 - 50.50)
```

### BDD-XWF-05 [EC] — US-TO03, FR-19.3
```gherkin
Scenario: RETAIL_SALE compensation reverses credit in correct order
  Given a RetailSaleWorkflow has completed:
    | Step | Activity           | Status    |
    | 1    | AuthorizeAtSwitch  | COMPLETED |
    | 2    | CreditAgentFloat   | FAILED    |
  When compensation is triggered
  Then no ReverseCreditFloat is needed (credit never succeeded)
  And the SendReversalToSwitchActivity should be triggered
  And the TransactionRecord should have status FAILED
```

### BDD-XWF-06 [EC] — US-TO03, FR-19.3
```gherkin
Scenario: HYBRID_CASHBACK compensation reverses both credits in reverse order
  Given a HybridCashbackWorkflow has completed:
    | Step | Activity           | Status    |
    | 1    | AuthorizeAtSwitch  | COMPLETED |
    | 2    | CreditAgentFloat (sale)     | COMPLETED |
    | 3    | CreditAgentFloat (cashback) | FAILED    |
  When compensation is triggered
  Then the compensation should execute:
    | Order | Activity                        |
    | 1     | ReverseCreditFloat (sale)       |
    | 2     | SendReversalToSwitch            |
  And the TransactionRecord should have status FAILED
```

---

## 12. Traceability: BDD Scenarios → BRD User Stories → Functional Requirements

| Scenario | User Story | Functional Requirement |
|----------|-----------|----------------------|
| BDD-TO-07 to BDD-TO-15 | US-TO01 | FR-11.1, FR-11.2, FR-8.1, FR-10.1, FR-10.2, FR-10.3, FR-15.1, FR-15.2, FR-15.5 |
| BDD-WF-HP-CP01 | US-X01 | FR-11.1 |
| BDD-WF-EC-CP01 | US-X01 | FR-11.1 |
| BDD-WF-EC-CP02 | US-X01 | FR-11.1 |
| BDD-WF-HP-PBP01 | US-X02 | FR-11.2 |
| BDD-WF-EC-PBP01 | US-X02 | FR-11.2 |
| BDD-WF-HP-PT01 | US-T01 | FR-8.1 |
| BDD-WF-HP-PT02 | US-T01 | FR-8.1 |
| BDD-WF-EC-PT01 | US-T03 | FR-8.3 |
| BDD-WF-EC-PT02 | US-T01 | FR-8.1 |
| BDD-WF-HP-PT03 | US-T02 | FR-8.2 |
| BDD-WF-EC-PT03 | US-T02 | FR-8.3 |
| BDD-WF-HP-EWW01 | US-W01 | FR-10.1 |
| BDD-WF-HP-EWW02 | US-W01 | FR-10.1 |
| BDD-WF-EC-EWW01 | US-W01 | FR-10.1 |
| BDD-WF-HP-EWT01 | US-W02 | FR-10.2 |
| BDD-WF-HP-EWT02 | US-W02 | FR-10.2 |
| BDD-WF-EC-EWT01 | US-W02 | FR-10.2 |
| BDD-WF-HP-ESSP01 | US-E01 | FR-10.3 |
| BDD-WF-HP-ESSP02 | US-E01 | FR-10.3 |
| BDD-WF-EC-ESSP01 | US-E01 | FR-10.3 |
| BDD-WF-HP-PIN01 | US-M02 | FR-15.2 |
| BDD-WF-HP-PIN02 | US-M02 | FR-15.2 |
| BDD-WF-EC-PIN01 | US-M02 | FR-15.2 |
| BDD-WF-EC-PIN02 | US-M02 | FR-15.2 |
| BDD-WF-HP-RS01 | US-M01 | FR-15.1 |
| BDD-WF-HP-RS02 | US-M01 | FR-15.1 |
| BDD-WF-HP-RS03 | US-M01 | FR-15.1 |
| BDD-WF-EC-RS01 | US-M01 | FR-15.1 |
| BDD-WF-EC-RS02 | US-M01 | FR-15.1 |
| BDD-WF-EC-RS03 | US-M01 | FR-15.1 |
| BDD-WF-EC-RS04 | US-M01 | FR-15.1 |
| BDD-WF-HP-HCB01 | US-M03 | FR-15.5 |
| BDD-WF-EC-HCB01 | US-M03 | FR-15.5 |
| BDD-WF-EC-HCB02 | US-M03 | FR-15.5 |
| BDD-WF-EC-HCB03 | US-M03 | FR-15.5 |
| BDD-WF-EC-HCB04 | US-M03 | FR-15.5 |
| BDD-XWF-04 | US-TO02 | FR-19.2 |
| BDD-XWF-05 | US-TO03 | FR-19.3 |
| BDD-XWF-06 | US-TO03 | FR-19.3 |

---

## 13. Scenario Count Summary

| Category | Happy Path | Edge Cases | Total |
|----------|-----------|------------|-------|
| Workflow Router Extensions (BDD-TO-EXT) | 9 | — | 9 |
| CASHLESS_PAYMENT (BDD-WF-CP) | 1 | 2 | 3 |
| PIN_BASED_PURCHASE (BDD-WF-PBP) | 1 | 1 | 2 |
| PREPAID_TOPUP (BDD-WF-PT) | 3 | 3 | 6 |
| EWALLET_WITHDRAWAL (BDD-WF-EWW) | 2 | 1 | 3 |
| EWALLET_TOPUP (BDD-WF-EWT) | 2 | 1 | 3 |
| ESSP_PURCHASE (BDD-WF-ESSP) | 2 | 1 | 3 |
| PIN_PURCHASE (BDD-WF-PIN) | 2 | 2 | 4 |
| RETAIL_SALE (BDD-WF-RS) | 3 | 4 | 7 |
| HYBRID_CASHBACK (BDD-WF-HCB) | 1 | 4 | 5 |
| Cross-Workflow Extensions (BDD-XWF-EXT) | — | 3 | 3 |
| **Total** | **26** | **22** | **48** |
