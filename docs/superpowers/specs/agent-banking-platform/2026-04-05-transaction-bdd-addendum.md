# Behavior-Driven Development Specification — Addendum: Transaction Orchestrator (Temporal)
## Agent Banking Platform (Malaysia)

**Version:** 1.0
**Date:** 2026-04-05
**Status:** Draft — Pending Review
**Supplements:** `2026-03-25-agent-banking-platform-bdd.md` (original BDD)
**BRD Reference:** `2026-04-05-transaction-brd-addendum.md`
**Design Reference:** `2026-04-05-transaction-orchestrator-temporal-design.md`

---

## Scope

This addendum covers **only** BDD scenarios for the Transaction Orchestrator domain: workflow routing, Temporal workflow lifecycle, compensation, safety reversal, human-in-the-loop, polling, and idempotency.

**Unchanged scenarios** (Rules, Ledger, e-KYC, etc.) remain in the original BDD at `2026-03-25-agent-banking-platform-bdd.md`.

---

## Scenario Classification

- **[HP]** = Happy Path (optimal user flow)
- **[EC]** = Edge Case (boundary, invalid, or extreme scenarios)

---

## 1. Workflow Router Dispatch (BDD-TO)

### BDD-TO-01 [HP] — US-TO01, FR-19.1
```gherkin
Scenario: Router dispatches Off-Us withdrawal to WithdrawalWorkflow
  Given a transaction request with:
    | field           | value             |
    | transactionType | CASH_WITHDRAWAL   |
    | targetBIN       | 0123              |
    | pan             | 411111******1111  |
  When the WorkflowRouter processes the request
  Then it should select WithdrawalWorkflow
  And start a Temporal workflow with workflowId = idempotencyKey
  And return 202 Accepted with workflowId and pollUrl
```

### BDD-TO-02 [HP] — US-TO01, FR-19.1
```gherkin
Scenario: Router dispatches On-Us withdrawal to WithdrawalOnUsWorkflow
  Given a transaction request with:
    | field           | value             |
    | transactionType | CASH_WITHDRAWAL   |
    | targetBIN       | 0012              |
    | customerAccount | 1234567890        |
  When the WorkflowRouter processes the request
  Then it should select WithdrawalOnUsWorkflow
  And start a Temporal workflow with workflowId = idempotencyKey
```

### BDD-TO-03 [HP] — US-TO01, FR-19.1
```gherkin
Scenario: Router dispatches deposit to DepositWorkflow
  Given a transaction request with:
    | field              | value          |
    | transactionType    | CASH_DEPOSIT   |
    | destinationAccount | 1234567890     |
  When the WorkflowRouter processes the request
  Then it should select DepositWorkflow
  And start a Temporal workflow with workflowId = idempotencyKey
```

### BDD-TO-04 [HP] — US-TO01, FR-19.1
```gherkin
Scenario: Router dispatches bill payment to BillPaymentWorkflow
  Given a transaction request with:
    | field           | value         |
    | transactionType | BILL_PAYMENT  |
    | billerCode      | TNB           |
    | ref1            | 123456789012  |
  When the WorkflowRouter processes the request
  Then it should select BillPaymentWorkflow
  And start a Temporal workflow with workflowId = idempotencyKey
```

### BDD-TO-05 [HP] — US-TO01, FR-19.1
```gherkin
Scenario: Router dispatches DuitNow transfer to DuitNowTransferWorkflow
  Given a transaction request with:
    | field           | value             |
    | transactionType | DUITNOW_TRANSFER  |
    | proxyType       | MOBILE            |
    | proxyValue      | 0123456789        |
  When the WorkflowRouter processes the request
  Then it should select DuitNowTransferWorkflow
  And start a Temporal workflow with workflowId = idempotencyKey
```

### BDD-TO-06 [EC] — US-TO01, FR-19.1
```gherkin
Scenario: Router rejects unsupported transaction type
  Given a transaction request with:
    | field           | value              |
    | transactionType | UNKNOWN_TYPE       |
  When the WorkflowRouter processes the request
  Then it should return error code "ERR_UNSUPPORTED_TRANSACTION_TYPE"
  And the response status should be 400 Bad Request
```

---

## 2. Workflow Lifecycle (BDD-WF)

### BDD-WF-01 [HP] — US-TO02, FR-19.2
```gherkin
Scenario: Workflow starts and returns PENDING status immediately
  Given a valid withdrawal request with idempotency key "IDEM-001"
  When the Orchestrator starts the WithdrawalWorkflow
  Then the response should be 202 Accepted
  And the response should contain:
    | field      | value                                    |
    | status     | PENDING                                  |
    | workflowId | IDEM-001                                 |
    | pollUrl    | /api/v1/transactions/IDEM-001/status     |
  And the Temporal workflow should be in Running state
```

### BDD-WF-02 [HP] — US-TO02, FR-19.2
```gherkin
Scenario: Workflow completes successfully and updates TransactionRecord
  Given a WithdrawalWorkflow is running with workflowId "IDEM-001"
  And all Activities complete successfully
  When the workflow finishes
  Then the TransactionRecord should be updated:
    | field      | value     |
    | status     | COMPLETED |
    | completedAt | <current timestamp> |
  And the Temporal workflow should be in Completed state
```

### BDD-WF-03 [HP] — US-TO02, FR-19.2
```gherkin
Scenario: Workflow fails and records error in TransactionRecord
  Given a WithdrawalWorkflow is running with workflowId "IDEM-002"
  And the BlockFloatActivity throws InsufficientFloatException
  When the workflow finishes
  Then the TransactionRecord should be updated:
    | field        | value                       |
    | status       | FAILED                      |
    | errorCode    | ERR_INSUFFICIENT_FLOAT      |
    | completedAt  | <current timestamp>         |
  And the Temporal workflow should be in Completed state (with failure)
```

### BDD-WF-04 [EC] — US-TO02, FR-19.2
```gherkin
Scenario: Workflow survives JVM crash mid-execution and resumes
  Given a WithdrawalWorkflow has completed BlockFloatActivity
  And the JVM crashes before AuthorizeAtSwitchActivity
  When the JVM restarts and the Temporal Worker reconnects
  Then the workflow should resume from AuthorizeAtSwitchActivity
  And the float should remain blocked (not double-blocked)
  And the workflow should complete normally or trigger compensation
```

### BDD-WF-05 [EC] — US-TO02, FR-19.2
```gherkin
Scenario: Workflow survives network partition to downstream service
  Given a WithdrawalWorkflow is executing AuthorizeAtSwitchActivity
  And the network to Switch Adapter is down
  When the Activity times out (25s)
  Then the workflow should trigger Safety Reversal
  And the workflow should not lose state during the partition
  And the workflow should complete with status FAILED
```

### BDD-WF-06 [EC] — US-TO02, FR-19.2
```gherkin
Scenario: Workflow stays in COMPENSATING state when compensation fails
  Given a WithdrawalWorkflow has completed CommitFloatActivity
  And the workflow needs to trigger compensation
  And the ReleaseFloatActivity fails after 3 retries
  Then the workflow should remain in COMPENSATING state
  And the workflow should wait for admin signal (up to 4 hours)
  And an AuditLog entry should be created with action "COMPENSATION_FAILED"
```

---

## 3. WithdrawalWorkflow Happy Path (BDD-WF-HP-W)

### BDD-WF-HP-W01 [HP] — US-L05, FR-20.1
```gherkin
Scenario: Off-Us withdrawal completes successfully via Temporal workflow
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "10000.00"
  And a FeeConfig for CASH_WITHDRAWAL STANDARD:
    | field            | value  |
    | customerFeeValue | 1.00   |
    | dailyLimitAmount | 10000  |
  And the customer presents a valid EMV card with correct PIN
  And the targetBIN is "0123" (non-BSN)
  When the POS terminal requests CASH_WITHDRAWAL of RM 500.00 with idempotency key "IDEM-W01"
  Then the WorkflowRouter should start WithdrawalWorkflow
  And the CheckVelocityActivity should pass
  And the CalculateFeesActivity should return customerFee=1.00, agentCommission=0.20, bankShare=0.80
  And the BlockFloatActivity should reserve RM 501.00 (amount + fees)
  And the AuthorizeAtSwitchActivity should return APPROVED with referenceCode "PAYNET-REF-789"
  And the CommitFloatActivity should commit RM 501.00
  And the PublishKafkaEventActivity should publish transaction-completed event
  And the TransactionRecord should have:
    | field              | value             |
    | workflowId         | IDEM-W01          |
    | transactionType    | CASH_WITHDRAWAL   |
    | amount             | 500.00            |
    | customerFee        | 1.00              |
    | status             | COMPLETED         |
    | externalReference  | PAYNET-REF-789    |
  And AgentFloat.balance should be "9499.00"
```

### BDD-WF-HP-W02 [HP] — US-L06, FR-21.1
```gherkin
Scenario: On-Us withdrawal completes successfully via WithdrawalOnUsWorkflow
  Given Agent "AGT-01" (STANDARD) has AgentFloat balance "10000.00"
  And the customer has BSN account "1234567890" with sufficient balance
  And the targetBIN is "0012" (BSN)
  When the POS terminal requests CASH_WITHDRAWAL of RM 200.00
  Then the WorkflowRouter should start WithdrawalOnUsWorkflow
  And the CheckVelocityActivity should pass
  And the BlockFloatActivity should reserve RM 200.00
  And the AuthorizeAtCBSActivity should return APPROVED
  And the CommitFloatActivity should commit RM 200.00
  And the TransactionRecord should have status COMPLETED
  And AgentFloat.balance should be "9800.00"
```

---

## 4. WithdrawalWorkflow Failure Scenarios (BDD-WF-EC-W)

### BDD-WF-EC-W01 [EC] — US-L05, FR-20.2
```gherkin
Scenario: Withdrawal declined by switch — compensation releases float
  Given Agent "AGT-01" has AgentFloat balance "10000.00"
  And the BlockFloatActivity has reserved RM 500.00
  And the AuthorizeAtSwitchActivity returns DECLINED with responseCode "51"
  When the workflow processes the decline
  Then the ReleaseFloatActivity should release RM 500.00
  And the TransactionRecord should have:
    | field     | value                  |
    | status    | FAILED                 |
    | errorCode | ERR_INSUFFICIENT_FUNDS |
  And AgentFloat.balance should be restored to "10000.00"
```

### BDD-WF-EC-W02 [EC] — US-L05, FR-20.3
```gherkin
Scenario: Switch timeout — Safety Reversal triggered with Store & Forward
  Given Agent "AGT-01" has AgentFloat balance "10000.00"
  And the BlockFloatActivity has reserved RM 500.00
  And the AuthorizeAtSwitchActivity times out after 25 seconds
  When the workflow processes the timeout
  Then the SendReversalToSwitchActivity should be triggered (MTI 0400)
  And the ReleaseFloatActivity should release RM 500.00
  And the TransactionRecord should have:
    | field     | value              |
    | status    | FAILED             |
    | errorCode | ERR_NETWORK_TIMEOUT |
  And if SendReversalToSwitchActivity fails, it should retry every 60 seconds indefinitely
```

### BDD-WF-EC-W03 [EC] — US-L05, FR-20.4
```gherkin
Scenario: CommitFloat fails after switch approval — Safety Reversal triggered
  Given Agent "AGT-01" has AgentFloat balance "10000.00"
  And the BlockFloatActivity has reserved RM 500.00
  And the AuthorizeAtSwitchActivity returned APPROVED
  And the CommitFloatActivity throws FloatCommitFailedException
  When the workflow processes the failure
  Then the SendReversalToSwitchActivity should be triggered
  And the ReleaseFloatActivity should release RM 500.00
  And the TransactionRecord should have status FAILED
  And AgentFloat.balance should be restored to "10000.00"
```

### BDD-WF-EC-W04 [EC] — US-L05, FR-20.5
```gherkin
Scenario: Insufficient float — workflow fails immediately without compensation
  Given Agent "AGT-01" has AgentFloat balance "200.00"
  When a CASH_WITHDRAWAL of RM 500.00 is requested
  Then the BlockFloatActivity should throw InsufficientFloatException
  And no compensation should be triggered (float was never blocked)
  And the TransactionRecord should have:
    | field     | value                   |
    | status    | FAILED                  |
    | errorCode | ERR_INSUFFICIENT_FLOAT  |
  And AgentFloat.balance should remain "200.00"
```

### BDD-WF-EC-W05 [EC] — US-L05, FR-20.1
```gherkin
Scenario: Velocity check fails — workflow fails before float block
  Given customer MyKad "123456789012" has exceeded daily velocity limit
  When a CASH_WITHDRAWAL is requested
  Then the CheckVelocityActivity should throw VelocityCheckFailedException
  And no float should be blocked
  And the TransactionRecord should have:
    | field     | value                        |
    | status    | FAILED                       |
    | errorCode | ERR_VELOCITY_COUNT_EXCEEDED  |
```

### BDD-WF-EC-W06 [EC] — US-L05
```gherkin
Scenario: Fee config not found — workflow fails before float block
  Given no FeeConfig exists for CASH_WITHDRAWAL and agent tier "MICRO"
  When a CASH_WITHDRAWAL is requested
  Then the CalculateFeesActivity should throw FeeConfigNotFoundException
  And no float should be blocked
  And the TransactionRecord should have:
    | field     | value                      |
    | status    | FAILED                     |
    | errorCode | ERR_FEE_CONFIG_NOT_FOUND   |
```

### BDD-WF-EC-W07 [EC] — US-L06, FR-21.2
```gherkin
Scenario: On-Us withdrawal — CBS authorization fails, float released
  Given Agent "AGT-01" has AgentFloat balance "10000.00"
  And the BlockFloatActivity has reserved RM 500.00
  And the AuthorizeAtCBSActivity returns ACCOUNT_FROZEN
  When the workflow processes the failure
  Then the ReleaseFloatActivity should release RM 500.00
  And the TransactionRecord should have:
    | field     | value              |
    | status    | FAILED             |
    | errorCode | ERR_ACCOUNT_FROZEN |
  And AgentFloat.balance should be restored to "10000.00"
```

### BDD-WF-EC-W08 [EC] — US-L05
```gherkin
Scenario: PublishKafkaEvent fails — workflow still completes
  Given a WithdrawalWorkflow has completed all financial activities
  And the PublishKafkaEventActivity throws EventPublishFailedException
  When the workflow processes the failure
  Then the workflow should log the error and continue
  And the TransactionRecord should have status COMPLETED
  And the workflow should NOT trigger compensation (financial steps succeeded)
```

---

## 5. DepositWorkflow Scenarios (BDD-WF-D)

### BDD-WF-HP-D01 [HP] — US-L07, FR-22.1
```gherkin
Scenario: Cash deposit completes successfully
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the destination account "1234567890" is valid
  And the deposit amount RM 1000.00 is below the biometric threshold
  When the POS terminal requests CASH_DEPOSIT of RM 1000.00
  Then the WorkflowRouter should start DepositWorkflow
  And the ValidateAccountActivity should return valid with accountName "MOHD A***D"
  And the CreditAgentFloatActivity should credit RM 1000.00
  And the PostToCBSActivity should succeed
  And the TransactionRecord should have:
    | field           | value        |
    | status          | COMPLETED    |
    | transactionType | CASH_DEPOSIT |
    | amount          | 1000.00      |
  And AgentFloat.balance should be "6000.00"
```

### BDD-WF-EC-D01 [EC] — US-L07, FR-22.4
```gherkin
Scenario: Deposit to invalid account — fails before any money moves
  Given the destination account "9999999999" does not exist
  When a CASH_DEPOSIT is requested
  Then the ValidateAccountActivity should throw InvalidAccountException
  And no compensation should be triggered
  And the TransactionRecord should have:
    | field     | value                |
    | status    | FAILED               |
    | errorCode | ERR_INVALID_ACCOUNT  |
  And AgentFloat.balance should remain unchanged
```

### BDD-WF-EC-D02 [EC] — US-L07, FR-22.2
```gherkin
Scenario: High-value deposit requires biometric verification
  Given the deposit amount is RM 10000.00 (above RM 5000 threshold)
  And the customer MyKad "123456789012" is presented
  When the DepositWorkflow executes
  Then the VerifyBiometricActivity should be invoked
  And on MATCH, the workflow should continue to CreditAgentFloat
  And on NO_MATCH, the workflow should return FAILED with ERR_BIOMETRIC_MISMATCH
```

### BDD-WF-EC-D03 [EC] — US-L07, FR-22.3
```gherkin
Scenario: CBS posting fails after float credited — compensation reverses credit
  Given Agent "AGT-01" has AgentFloat balance "5000.00"
  And the CreditAgentFloatActivity has credited RM 1000.00 (balance = 6000.00)
  And the PostToCBSActivity throws CBSUnavailableException
  When the workflow processes the failure
  Then the ReverseCreditFloatActivity should debit RM 1000.00
  And AgentFloat.balance should be restored to "5000.00"
  And the TransactionRecord should have:
    | field     | value                     |
    | status    | FAILED                    |
    | errorCode | ERR_DOWNSTREAM_UNAVAILABLE |
```

### BDD-WF-EC-D04 [EC] — US-L07
```gherkin
Scenario: Float cap exceeded after deposit credit
  Given Agent "AGT-01" (MICRO) has float cap RM 20000.00
  And AgentFloat.balance is "19500.00"
  When a CASH_DEPOSIT of RM 1000.00 is requested
  Then the CreditAgentFloatActivity should throw FloatCapExceededException
  And the TransactionRecord should have:
    | field     | value                    |
    | status    | FAILED                   |
    | errorCode | ERR_FLOAT_CAP_EXCEEDED   |
  And AgentFloat.balance should remain "19500.00"
```

---

## 6. BillPaymentWorkflow Scenarios (BDD-WF-BP)

### BDD-WF-HP-BP01 [HP] — US-B01, FR-23.1
```gherkin
Scenario: Bill payment completes successfully
  Given Agent "AGT-01" has AgentFloat balance "10000.00"
  And the biller "TNB" with ref1 "123456789012" is valid
  And the bill amount is RM 150.00
  When the POS terminal requests BILL_PAYMENT of RM 150.00
  Then the WorkflowRouter should start BillPaymentWorkflow
  And the ValidateBillActivity should return valid with accountName "TENAGA NASIONAL"
  And the PayBillerActivity should succeed with billerReference "TNB-REF-001"
  And the CommitFloatActivity should commit RM 150.00
  And the NotifyBillerActivity should send payment notification
  And the TransactionRecord should have:
    | field              | value         |
    | status             | COMPLETED     |
    | transactionType    | BILL_PAYMENT  |
    | amount             | 150.00        |
    | externalReference  | TNB-REF-001   |
  And AgentFloat.balance should be "9850.00"
```

### BDD-WF-EC-BP01 [EC] — US-B01, FR-23.2
```gherkin
Scenario: Invalid biller reference — float released
  Given the biller "TNB" with ref1 "INVALID" does not exist
  And the BlockFloatActivity has reserved RM 150.00
  When the ValidateBillActivity returns invalid
  Then the ReleaseFloatActivity should release RM 150.00
  And the TransactionRecord should have:
    | field     | value                 |
    | status    | FAILED                |
    | errorCode | ERR_INVALID_BILLER_REF |
  And AgentFloat.balance should be restored
```

### BDD-WF-EC-BP02 [EC] — US-B01, FR-23.3
```gherkin
Scenario: Biller payment fails — float released and biller notified of reversal
  Given the BlockFloatActivity has reserved RM 150.00
  And the ValidateBillActivity returned valid
  And the PayBillerActivity throws BillerPaymentFailedException
  When the workflow processes the failure
  Then the ReleaseFloatActivity should release RM 150.00
  And the NotifyBillerReversalActivity should notify the biller
  And the TransactionRecord should have status FAILED
```

### BDD-WF-EC-BP03 [EC] — US-B01, FR-23.4
```gherkin
Scenario: NotifyBiller fails — workflow still completes
  Given the bill payment has been processed successfully
  And the CommitFloatActivity has committed
  And the NotifyBillerActivity throws NotificationFailedException
  When the workflow processes the failure
  Then the workflow should log the error and continue
  And the TransactionRecord should have status COMPLETED
  And no compensation should be triggered (biller already paid)
```

---

## 7. DuitNowTransferWorkflow Scenarios (BDD-WF-DN)

### BDD-WF-HP-DN01 [HP] — US-D01, FR-24.1
```gherkin
Scenario: DuitNow transfer completes successfully via mobile proxy
  Given Agent "AGT-01" has AgentFloat balance "10000.00"
  And the proxy "0123456789" resolves to Maybank account
  When the POS terminal requests DUITNOW_TRANSFER of RM 500.00
  Then the WorkflowRouter should start DuitNowTransferWorkflow
  And the ProxyEnquiryActivity should return valid with recipientName "AHMAD***" and bankCode "MBBEMYKL"
  And the SendDuitNowTransferActivity should return APPROVED with paynetReference "DN-REF-001"
  And the CommitFloatActivity should commit RM 500.00
  And the TransactionRecord should have:
    | field             | value             |
    | status            | COMPLETED         |
    | transactionType   | DUITNOW_TRANSFER  |
    | amount            | 500.00            |
    | externalReference | DN-REF-001        |
  And the workflow should complete in under 15 seconds
```

### BDD-WF-HP-DN02 [HP] — US-D01, FR-24.2
```gherkin
Scenario: DuitNow transfer via MyKad proxy
  Given the proxyType is "MYKAD" and proxyValue is "123456789012"
  When the DuitNowTransferWorkflow executes ProxyEnquiryActivity
  Then it should resolve to the linked bank account
  And the workflow should proceed with the transfer
```

### BDD-WF-HP-DN03 [HP] — US-D01, FR-24.2
```gherkin
Scenario: DuitNow transfer via BRN proxy
  Given the proxyType is "BRN" and proxyValue is "BRN123456"
  When the DuitNowTransferWorkflow executes ProxyEnquiryActivity
  Then it should resolve to the linked business bank account
  And the workflow should proceed with the transfer
```

### BDD-WF-EC-DN01 [EC] — US-D01, FR-24.3
```gherkin
Scenario: Proxy not found — float released
  Given the BlockFloatActivity has reserved RM 500.00
  And the proxy "9999999999" does not exist
  When the ProxyEnquiryActivity throws ProxyNotFoundException
  Then the ReleaseFloatActivity should release RM 500.00
  And the TransactionRecord should have:
    | field     | value                |
    | status    | FAILED               |
    | errorCode | ERR_PROXY_NOT_FOUND  |
```

### BDD-WF-EC-DN02 [EC] — US-D01, FR-24.4
```gherkin
Scenario: DuitNow transfer timeout — Safety Reversal triggered
  Given the BlockFloatActivity has reserved RM 500.00
  And the ProxyEnquiryActivity succeeded
  And the SendDuitNowTransferActivity times out after 25 seconds
  When the workflow processes the timeout
  Then the SendReversalToSwitchActivity should be triggered (MTI 0400)
  And the ReleaseFloatActivity should release RM 500.00
  And the TransactionRecord should have:
    | field     | value              |
    | status    | FAILED             |
    | errorCode | ERR_NETWORK_TIMEOUT |
```

### BDD-WF-EC-DN03 [EC] — US-D01
```gherkin
Scenario: DuitNow transfer declined by recipient bank
  Given the BlockFloatActivity has reserved RM 500.00
  And the SendDuitNowTransferActivity returns DECLINED with responseCode "AC04"
  When the workflow processes the decline
  Then the ReleaseFloatActivity should release RM 500.00
  And the TransactionRecord should have:
    | field     | value               |
    | status    | FAILED              |
    | errorCode | ERR_ACCOUNT_INACTIVE |
```

---

## 8. Safety Reversal with Store & Forward (BDD-SR)

### BDD-SR-01 [HP] — US-V01, FR-19.4
```gherkin
Scenario: Safety Reversal succeeds on first attempt
  Given a WithdrawalWorkflow has triggered Safety Reversal
  And the SendReversalToSwitchActivity is invoked
  When PayNet acknowledges the MTI 0400 reversal
  Then the reversal should be marked as SUCCESS
  And the ReleaseFloatActivity should proceed
  And the TransactionRecord should be updated with status FAILED
```

### BDD-SR-02 [EC] — US-V01, FR-19.4
```gherkin
Scenario: Safety Reversal retries until PayNet acknowledges
  Given the SendReversalToSwitchActivity has failed 5 times
  And 300 seconds have elapsed (5 retries × 60s interval)
  When the 6th retry is attempted
  And PayNet acknowledges the reversal
  Then the reversal should be marked as SUCCESS
  And the ReleaseFloatActivity should proceed
  And an AuditLog entry should record: 5 failed attempts, 1 success
```

### BDD-SR-03 [EC] — US-V01, FR-19.4
```gherkin
Scenario: Safety Reversal persists across JVM restarts
  Given the SendReversalToSwitchActivity has failed 3 times
  And the JVM crashes
  When the JVM restarts and the Temporal Worker reconnects
  Then the SendReversalToSwitchActivity should resume retrying
  And the retry counter should be preserved
  And the reversal should eventually succeed or remain pending
```

### BDD-SR-04 [EC] — US-V01, FR-19.4
```gherkin
Scenario: Safety Reversal flagged for manual investigation after extended failure
  Given the SendReversalToSwitchActivity has been retrying for 24 hours
  And PayNet has not acknowledged
  Then the workflow should flag the transaction for manual investigation
  And create an AuditLog entry with action "REVERSAL_STUCK"
  And alert the backoffice operations team
```

---

## 9. Human-in-the-Loop Signal Handling (BDD-HITL)

### BDD-HITL-01 [HP] — US-TO05, FR-19.5
```gherkin
Scenario: Admin force-resolves stuck workflow with COMMIT action
  Given a WithdrawalWorkflow is in COMPENSATING state
  And 4 hours have elapsed since the workflow entered COMPENSATING
  And an admin with role "ADMIN" sends ForceResolveSignal with action "COMMIT"
  When the workflow receives the signal
  Then Workflow.await() should unblock
  And the workflow should execute the commit action
  And the TransactionRecord should have status COMPLETED
  And an AuditLog entry should record:
    | field     | value                    |
    | action    | FORCE_COMMIT             |
    | adminId   | <admin user ID>          |
    | reason    | <provided reason>        |
    | timestamp | <current timestamp>      |
```

### BDD-HITL-02 [HP] — US-TO05, FR-19.5
```gherkin
Scenario: Admin force-resolves stuck workflow with REVERSE action
  Given a WithdrawalWorkflow is in COMPENSATING state
  And an admin sends ForceResolveSignal with action "REVERSE"
  When the workflow receives the signal
  Then the workflow should execute the reverse action
  And the TransactionRecord should have status REVERSED
  And an AuditLog entry should record the admin action
```

### BDD-HITL-03 [EC] — US-TO05, FR-19.5
```gherkin
Scenario: Unauthorized user cannot send force-resolve signal
  Given a user with role "VIEWER" attempts to send ForceResolveSignal
  When the request reaches the backoffice endpoint
  Then the system should return error code "ERR_UNAUTHORIZED"
  And the signal should NOT be sent to the workflow
  And the workflow should remain in COMPENSATING state
```

---

## 10. Polling Endpoint (BDD-POLL)

### BDD-POLL-01 [HP] — US-TO06, FR-19.6
```gherkin
Scenario: Poll returns COMPLETED status with transaction details
  Given a WithdrawalWorkflow has completed successfully
  And the TransactionRecord has status COMPLETED
  When the POS terminal polls GET /api/v1/transactions/IDEM-001/status
  Then the response should be 200 OK
  And the response should contain:
    | field             | value             |
    | status            | COMPLETED         |
    | workflowId        | IDEM-001          |
    | transactionId     | TXN-uuid-123      |
    | amount            | 500.00            |
    | customerFee       | 1.00              |
    | referenceNumber   | PAYNET-REF-789    |
    | completedAt       | <timestamp>       |
```

### BDD-POLL-02 [HP] — US-TO06, FR-19.6
```gherkin
Scenario: Poll returns FAILED status with error details
  Given a WithdrawalWorkflow has failed with ERR_INSUFFICIENT_FLOAT
  When the POS terminal polls GET /api/v1/transactions/IDEM-002/status
  Then the response should be 200 OK
  And the response should contain:
    | field      | value                  |
    | status     | FAILED                 |
    | workflowId | IDEM-002               |
    | error      |                        |
    |   code     | ERR_INSUFFICIENT_FLOAT |
    |   message  | Agent float balance insufficient |
    |   action_code | DECLINE             |
```

### BDD-POLL-03 [HP] — US-TO06, FR-19.6
```gherkin
Scenario: Poll returns PENDING status for running workflow
  Given a WithdrawalWorkflow is still executing (Activities in progress)
  When the POS terminal polls GET /api/v1/transactions/IDEM-003/status
  Then the response should be 200 OK
  And the response should contain:
    | field      | value   |
    | status     | PENDING |
    | workflowId | IDEM-003 |
    | message    | Transaction is being processed |
```

### BDD-POLL-04 [EC] — US-TO06
```gherkin
Scenario: Poll for non-existent workflowId returns 404
  When the POS terminal polls GET /api/v1/transactions/INVALID-ID/status
  Then the response should be 404 Not Found
  And the response should contain error code "ERR_WORKFLOW_NOT_FOUND"
```

### BDD-POLL-05 [EC] — US-TO06, FR-19.6
```gherkin
Scenario: Poll endpoint caches response to reduce Temporal load
  Given a TransactionRecord has status COMPLETED
  And the response has been cached in Redis with TTL 5s
  When the POS terminal polls the same workflowId twice within 5 seconds
  Then both responses should be identical
  And only the first request should query the database
  And the second request should return the cached response
```

---

## 11. Idempotency at Temporal Level (BDD-IDE)

### BDD-IDE-01 [HP] — US-TO07, FR-19.7
```gherkin
Scenario: Duplicate workflow start is rejected by Temporal
  Given a WithdrawalWorkflow was started with idempotency key "IDEM-001"
  And the workflow is still running
  When another request arrives with the same idempotency key "IDEM-001"
  Then Temporal should reject the duplicate start with WorkflowExecutionAlreadyStartedError
  And the controller should catch this error
  And return the existing workflow's status (PENDING)
  And no new workflow should be created
```

### BDD-IDE-02 [HP] — US-TO07, FR-19.7
```gherkin
Scenario: Duplicate request for completed workflow returns cached result
  Given a WithdrawalWorkflow completed with status COMPLETED
  And the response was cached in Redis with TTL 24h
  When another request arrives with the same idempotency key
  Then the system should return the cached response
  And no new workflow should be started
  And the agent float should not be modified again
```

### BDD-IDE-03 [EC] — US-TO07, FR-19.7
```gherkin
Scenario: Duplicate request for failed workflow returns original error
  Given a WithdrawalWorkflow failed with ERR_INSUFFICIENT_FLOAT
  And the error response was cached in Redis with TTL 24h
  When another request arrives with the same idempotency key
  Then the system should return the cached error response
  And no new workflow should be started
```

---

## 12. Cross-Workflow Scenarios

### BDD-XWF-01 [EC] — US-TO02, FR-19.2
```gherkin
Scenario: Multiple concurrent workflows for same agent do not cause race conditions
  Given Agent "AGT-01" has AgentFloat balance "1000.00"
  And two concurrent withdrawal requests of RM 500.00 each with different idempotency keys
  When both WithdrawalWorkflows execute simultaneously
  Then only one BlockFloatActivity should succeed (PESSIMISTIC_WRITE lock)
  And the other should fail with ERR_INSUFFICIENT_FLOAT
  And AgentFloat.balance should be "500.00" after both complete
```

### BDD-XWF-02 [EC] — US-TO03, FR-19.3
```gherkin
Scenario: Compensation executes in reverse order of completed steps
  Given a BillPaymentWorkflow has completed:
    | Step | Activity     | Status    |
    | 1    | BlockFloat   | COMPLETED |
    | 2    | ValidateBill | COMPLETED |
    | 3    | PayBiller    | FAILED    |
  When compensation is triggered
  Then the compensation should execute in reverse order:
    | Order | Activity              |
    | 1     | ReleaseFloat          |
  And no NotifyBillerReversal is needed (biller was not paid)
```

### BDD-XWF-03 [EC] — US-TO03, FR-19.3
```gherkin
Scenario: Compensation after switch approval includes Safety Reversal
  Given a WithdrawalWorkflow has completed:
    | Step | Activity          | Status    |
    | 1    | BlockFloat        | COMPLETED |
    | 2    | AuthorizeAtSwitch | COMPLETED |
    | 3    | CommitFloat       | FAILED    |
  When compensation is triggered
  Then the compensation should execute:
    | Order | Activity              |
    | 1     | SendReversalToSwitch  |
    | 2     | ReleaseFloat          |
```

---

## 13. Traceability: BDD Scenarios → BRD User Stories → Functional Requirements

| Scenario | User Story | Functional Requirement |
|----------|-----------|----------------------|
| BDD-TO-01 to BDD-TO-05 | US-TO01 | FR-19.1 |
| BDD-TO-06 | US-TO01 | FR-19.1 |
| BDD-WF-01 to BDD-WF-06 | US-TO02 | FR-19.2 |
| BDD-WF-HP-W01 | US-L05 | FR-20.1 |
| BDD-WF-HP-W02 | US-L06 | FR-21.1 |
| BDD-WF-EC-W01 | US-L05 | FR-20.2 |
| BDD-WF-EC-W02 | US-L05, US-V01 | FR-20.3, FR-19.4 |
| BDD-WF-EC-W03 | US-L05 | FR-20.4 |
| BDD-WF-EC-W04 | US-L05 | FR-20.1 |
| BDD-WF-EC-W05 | US-L05 | FR-20.1 |
| BDD-WF-EC-W06 | US-L05 | FR-20.1 |
| BDD-WF-EC-W07 | US-L06 | FR-21.2 |
| BDD-WF-EC-W08 | US-L05 | FR-20.1 |
| BDD-WF-HP-D01 | US-L07 | FR-22.1 |
| BDD-WF-EC-D01 | US-L07 | FR-22.4 |
| BDD-WF-EC-D02 | US-L07 | FR-22.2 |
| BDD-WF-EC-D03 | US-L07 | FR-22.3 |
| BDD-WF-EC-D04 | US-L07 | FR-22.1 |
| BDD-WF-HP-BP01 | US-B01 | FR-23.1 |
| BDD-WF-EC-BP01 | US-B01 | FR-23.2 |
| BDD-WF-EC-BP02 | US-B01 | FR-23.3 |
| BDD-WF-EC-BP03 | US-B01 | FR-23.4 |
| BDD-WF-HP-DN01 | US-D01 | FR-24.1 |
| BDD-WF-HP-DN02 | US-D01 | FR-24.2 |
| BDD-WF-HP-DN03 | US-D01 | FR-24.2 |
| BDD-WF-EC-DN01 | US-D01 | FR-24.3 |
| BDD-WF-EC-DN02 | US-D01 | FR-24.4 |
| BDD-WF-EC-DN03 | US-D01 | FR-24.1 |
| BDD-SR-01 to BDD-SR-04 | US-V01, US-TO04 | FR-19.4 |
| BDD-HITL-01 to BDD-HITL-03 | US-TO05 | FR-19.5 |
| BDD-POLL-01 to BDD-POLL-05 | US-TO06 | FR-19.6 |
| BDD-IDE-01 to BDD-IDE-03 | US-TO07 | FR-19.7 |
| BDD-XWF-01 | US-TO02 | FR-19.2 |
| BDD-XWF-02 | US-TO03 | FR-19.3 |
| BDD-XWF-03 | US-TO03 | FR-19.3 |

---

## 14. Scenario Count Summary

| Category | Happy Path | Edge Cases | Total |
|----------|-----------|------------|-------|
| Workflow Router (BDD-TO) | 5 | 1 | 6 |
| Workflow Lifecycle (BDD-WF) | 3 | 3 | 6 |
| Withdrawal HP (BDD-WF-HP-W) | 2 | — | 2 |
| Withdrawal EC (BDD-WF-EC-W) | — | 8 | 8 |
| Deposit (BDD-WF-D) | 1 | 4 | 5 |
| Bill Payment (BDD-WF-BP) | 1 | 3 | 4 |
| DuitNow Transfer (BDD-WF-DN) | 3 | 3 | 6 |
| Safety Reversal (BDD-SR) | 1 | 3 | 4 |
| Human-in-the-Loop (BDD-HITL) | 2 | 1 | 3 |
| Polling (BDD-POLL) | 3 | 2 | 5 |
| Idempotency (BDD-IDE) | 2 | 1 | 3 |
| Cross-Workflow (BDD-XWF) | — | 3 | 3 |
| **Total** | **23** | **32** | **55** |
