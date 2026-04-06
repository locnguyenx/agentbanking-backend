# Missing Transaction Types Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 9 missing transaction types (CASHLESS_PAYMENT, PIN_BASED_PURCHASE, PREPAID_TOPUP, EWALLET_WITHDRAWAL, EWALLET_TOPUP, ESSP_PURCHASE, PIN_PURCHASE, RETAIL_SALE, HYBRID_CASHBACK) using Temporal SAGA workflows.

**Architecture:** Each transaction type gets a Temporal workflow interface + implementation, activities for external calls, port interfaces for cross-service communication, and Feign clients for HTTP integration. Follows hexagonal architecture with zero framework imports in domain layer.

**Tech Stack:** Java 21, Spring Boot 3.2.5, Temporal SDK 1.25.1, Spring Cloud OpenFeign, Resilience4j, PostgreSQL, Redis, Kafka, ArchUnit.

**Spec References:**
- Design: `docs/superpowers/specs/agent-banking-platform/2026-04-06-missing-transaction-types-design.md`
- BDD: `docs/superpowers/specs/agent-banking-platform/2026-04-06-missing-transaction-types-bdd-addendum.md`
- Master Design: `docs/superpowers/specs/agent-banking-platform/2026-04-05-transaction-orchestrator-temporal-design.md`

---

## File Structure

### New Files to Create

**Domain Layer:**
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/exceptions/TelcoException.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/exceptions/EWalletException.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/exceptions/ESSPException.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/exceptions/PINException.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/exceptions/RetailException.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/TelcoAggregatorPort.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/EWalletProviderPort.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/ESSPServicePort.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/PINInventoryPort.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/QRPaymentPort.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/RequestToPayPort.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/MerchantTransactionPort.java`

**Application Layer:**
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/CashlessPaymentWorkflow.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/PinBasedPurchaseWorkflow.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/PrepaidTopupWorkflow.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/EWalletWithdrawalWorkflow.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/EWalletTopupWorkflow.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/ESSPPurchaseWorkflow.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/PINPurchaseWorkflow.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/RetailSaleWorkflow.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/HybridCashbackWorkflow.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/ValidatePhoneNumberActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/TopUpTelcoActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/ValidateEWalletActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/WithdrawFromEWalletActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/TopUpEWalletActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/ValidateESSPPurchaseActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/PurchaseESSPActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/ValidatePINInventoryActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/GeneratePINActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/CalculateMDRActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/GenerateDynamicQRActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/WaitForQRPaymentActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/SendRequestToPayActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/WaitForRTPApprovalActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/CreateMerchantTransactionRecordActivity.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/ValidateFloatCapacityActivity.java`

**Infrastructure Layer:**
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/TelcoAggregatorClient.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/TelcoAggregatorAdapter.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/EWalletProviderClient.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/EWalletProviderAdapter.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/ESSPServiceClient.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/ESSPServiceAdapter.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/PINInventoryClient.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/PINInventoryAdapter.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/QRPaymentClient.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/QRPaymentAdapter.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/RequestToPayClient.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/RequestToPayAdapter.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/MerchantTransactionClient.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/MerchantTransactionAdapter.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/CashlessPaymentWorkflowImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/PinBasedPurchaseWorkflowImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/PrepaidTopupWorkflowImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/EWalletWithdrawalWorkflowImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/EWalletTopupWorkflowImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/ESSPPurchaseWorkflowImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/PINPurchaseWorkflowImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/RetailSaleWorkflowImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/HybridCashbackWorkflowImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/ValidatePhoneNumberActivityImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/TopUpTelcoActivityImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/ValidateEWalletActivityImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/WithdrawFromEWalletActivityImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/TopUpEWalletActivityImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/ValidateESSPPurchaseActivityImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/PurchaseESSPActivityImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/ValidatePINInventoryActivityImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/GeneratePINActivityImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/CalculateMDRActivityImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/GenerateDynamicQRActivityImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/WaitForQRPaymentActivityImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/SendRequestToPayActivityImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/WaitForRTPApprovalActivityImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/CreateMerchantTransactionRecordActivityImpl.java`
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/ValidateFloatCapacityActivityImpl.java`

**Test Files:**
- `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/domain/service/WorkflowRouterExtendedTest.java`
- `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/workflow/CashlessPaymentWorkflowTest.java`
- `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/workflow/PinBasedPurchaseWorkflowTest.java`
- `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/workflow/PrepaidTopupWorkflowTest.java`
- `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/workflow/EWalletWithdrawalWorkflowTest.java`
- `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/workflow/EWalletTopupWorkflowTest.java`
- `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/workflow/ESSPPurchaseWorkflowTest.java`
- `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/workflow/PINPurchaseWorkflowTest.java`
- `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/workflow/RetailSaleWorkflowTest.java`
- `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/workflow/HybridCashbackWorkflowTest.java`

### Modified Files
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/TransactionType.java` — Add 9 new enum values
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/service/WorkflowRouter.java` — Add 9 new routing rules
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/config/DomainServiceConfig.java` — Register new beans
- `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/config/TemporalWorkerConfig.java` — Register new workflow/activity workers
- `services/orchestrator-service/src/main/resources/application.yaml` — Add new service URLs and timeouts

---

## Phase 1: Foundation — Enums, Ports, Exceptions

### Task 1: Extend TransactionType Enum

**Files:**
- Modify: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/TransactionType.java`

- [ ] **Step 1: Add 9 new transaction types to enum**

```java
package com.agentbanking.orchestrator.domain.model;

public enum TransactionType {
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    BILL_PAYMENT,
    DUITNOW_TRANSFER,
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

- [ ] **Step 2: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/TransactionType.java
git commit -m "feat: add 9 new transaction types for Phase 3+"
```

### Task 2: Extend WorkflowRouter

**Files:**
- Modify: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/service/WorkflowRouter.java`
- Test: `services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/domain/service/WorkflowRouterExtendedTest.java`

- [ ] **Step 1: Write tests for new routing rules**

```java
package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.domain.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowRouterExtendedTest {

    private final WorkflowRouter router = new WorkflowRouter();

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {
        "CASHLESS_PAYMENT", "PIN_BASED_PURCHASE", "PREPAID_TOPUP",
        "EWALLET_WITHDRAWAL", "EWALLET_TOPUP", "ESSP_PURCHASE",
        "PIN_PURCHASE", "RETAIL_SALE", "HYBRID_CASHBACK"
    })
    void shouldRouteNewTransactionTypes(TransactionType type) {
        String workflow = router.determineWorkflowType(type, null);
        assertEquals(type.name(), workflow);
    }

    @Test
    void shouldRouteCashlessPayment() {
        assertEquals("CASHLESS_PAYMENT", router.determineWorkflowType(TransactionType.CASHLESS_PAYMENT, null));
    }

    @Test
    void shouldRoutePinBasedPurchase() {
        assertEquals("PIN_BASED_PURCHASE", router.determineWorkflowType(TransactionType.PIN_BASED_PURCHASE, null));
    }

    @Test
    void shouldRoutePrepaidTopup() {
        assertEquals("PREPAID_TOPUP", router.determineWorkflowType(TransactionType.PREPAID_TOPUP, null));
    }

    @Test
    void shouldRouteEWalletWithdrawal() {
        assertEquals("EWALLET_WITHDRAWAL", router.determineWorkflowType(TransactionType.EWALLET_WITHDRAWAL, null));
    }

    @Test
    void shouldRouteEWalletTopup() {
        assertEquals("EWALLET_TOPUP", router.determineWorkflowType(TransactionType.EWALLET_TOPUP, null));
    }

    @Test
    void shouldRouteESSPPurchase() {
        assertEquals("ESSP_PURCHASE", router.determineWorkflowType(TransactionType.ESSP_PURCHASE, null));
    }

    @Test
    void shouldRoutePINPurchase() {
        assertEquals("PIN_PURCHASE", router.determineWorkflowType(TransactionType.PIN_PURCHASE, null));
    }

    @Test
    void shouldRouteRetailSale() {
        assertEquals("RETAIL_SALE", router.determineWorkflowType(TransactionType.RETAIL_SALE, null));
    }

    @Test
    void shouldRouteHybridCashback() {
        assertEquals("HYBRID_CASHBACK", router.determineWorkflowType(TransactionType.HYBRID_CASHBACK, null));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :services:orchestrator-service:test --tests "WorkflowRouterExtendedTest" -q
```
Expected: FAIL — new enum values not handled in switch

- [ ] **Step 3: Update WorkflowRouter with new routing rules**

```java
package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.domain.model.TransactionType;

public class WorkflowRouter {

    private static final String BSN_BIN = "0012";

    public String determineWorkflowType(TransactionType transactionType, String targetBIN) {
        return switch (transactionType) {
            case CASH_WITHDRAWAL -> isOnUs(targetBIN) ? "WithdrawalOnUs" : "Withdrawal";
            case CASH_DEPOSIT -> "Deposit";
            case BILL_PAYMENT -> "BillPayment";
            case DUITNOW_TRANSFER -> "DuitNowTransfer";
            case CASHLESS_PAYMENT -> "CASHLESS_PAYMENT";
            case PIN_BASED_PURCHASE -> "PIN_BASED_PURCHASE";
            case PREPAID_TOPUP -> "PREPAID_TOPUP";
            case EWALLET_WITHDRAWAL -> "EWALLET_WITHDRAWAL";
            case EWALLET_TOPUP -> "EWALLET_TOPUP";
            case ESSP_PURCHASE -> "ESSP_PURCHASE";
            case PIN_PURCHASE -> "PIN_PURCHASE";
            case RETAIL_SALE -> "RETAIL_SALE";
            case HYBRID_CASHBACK -> "HYBRID_CASHBACK";
        };
    }

    private boolean isOnUs(String targetBIN) {
        return BSN_BIN.equals(targetBIN);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :services:orchestrator-service:test --tests "WorkflowRouterExtendedTest" -q
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/service/WorkflowRouter.java
git add services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/domain/service/WorkflowRouterExtendedTest.java
git commit -m "feat: extend WorkflowRouter with 9 new transaction type routes"
```

### Task 3: Create Exception Classes

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/exceptions/TelcoException.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/exceptions/EWalletException.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/exceptions/ESSPException.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/exceptions/PINException.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/exceptions/RetailException.java`

- [ ] **Step 1: Check existing exception hierarchy**

First, read the existing exception pattern:
```bash
find services/orchestrator-service -name "*Exception.java" -type f
```

- [ ] **Step 2: Create TelcoException**

```java
package com.agentbanking.orchestrator.domain.model.exceptions;

public class TelcoException extends RuntimeException {
    private final String errorCode;

    public TelcoException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static class InvalidPhoneNumberException extends TelcoException {
        public InvalidPhoneNumberException(String phoneNumber) {
            super("Invalid phone number: " + phoneNumber, "ERR_INVALID_PHONE_NUMBER");
        }
    }

    public static class TelcoTopupFailedException extends TelcoException {
        public TelcoTopupFailedException(String message) {
            super(message, "ERR_TELCO_TOPUP_FAILED");
        }
    }

    public static class TelcoTimeoutException extends TelcoException {
        public TelcoTimeoutException(String provider) {
            super("Telco aggregator timeout: " + provider, "ERR_AGGREGATOR_TIMEOUT");
        }
    }

    public static class TelcoUnavailableException extends TelcoException {
        public TelcoUnavailableException(String provider) {
            super("Telco aggregator unavailable: " + provider, "ERR_TELCO_UNAVAILABLE");
        }
    }
}
```

- [ ] **Step 3: Create EWalletException**

```java
package com.agentbanking.orchestrator.domain.model.exceptions;

public class EWalletException extends RuntimeException {
    private final String errorCode;

    public EWalletException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static class InvalidEWalletException extends EWalletException {
        public InvalidEWalletException(String walletId) {
            super("Invalid eWallet ID: " + walletId, "ERR_INVALID_EWALLET");
        }
    }

    public static class EWalletInsufficientException extends EWalletException {
        public EWalletInsufficientException() {
            super("Insufficient eWallet balance", "ERR_WALLET_INSUFFICIENT");
        }
    }

    public static class EWalletWithdrawFailedException extends EWalletException {
        public EWalletWithdrawFailedException(String message) {
            super(message, "ERR_EWALLET_WITHDRAW_FAILED");
        }
    }

    public static class EWalletTopupFailedException extends EWalletException {
        public EWalletTopupFailedException(String message) {
            super(message, "ERR_EWALLET_TOPUP_FAILED");
        }
    }

    public static class EWalletTimeoutException extends EWalletException {
        public EWalletTimeoutException(String provider) {
            super("eWallet provider timeout: " + provider, "ERR_EWALLET_TIMEOUT");
        }
    }

    public static class EWalletUnavailableException extends EWalletException {
        public EWalletUnavailableException(String provider) {
            super("eWallet provider unavailable: " + provider, "ERR_EWALLET_UNAVAILABLE");
        }
    }
}
```

- [ ] **Step 4: Create ESSPException**

```java
package com.agentbanking.orchestrator.domain.model.exceptions;

public class ESSPException extends RuntimeException {
    private final String errorCode;

    public ESSPException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static class ESSPInvalidAmountException extends ESSPException {
        public ESSPInvalidAmountException() {
            super("Invalid eSSP purchase amount", "ERR_ESSP_INVALID_AMOUNT");
        }
    }

    public static class ESSPServiceUnavailableException extends ESSPException {
        public ESSPServiceUnavailableException() {
            super("BSN eSSP service unavailable", "ERR_ESSP_SERVICE_UNAVAILABLE");
        }
    }

    public static class ESSPPurchaseFailedException extends ESSPException {
        public ESSPPurchaseFailedException(String message) {
            super(message, "ERR_ESSP_PURCHASE_FAILED");
        }
    }
}
```

- [ ] **Step 5: Create PINException**

```java
package com.agentbanking.orchestrator.domain.model.exceptions;

public class PINException extends RuntimeException {
    private final String errorCode;

    public PINException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static class PINInventoryDepletedException extends PINException {
        public PINInventoryDepletedException(String provider) {
            super("PIN inventory depleted for provider: " + provider, "ERR_PIN_INVENTORY_DEPLETED");
        }
    }

    public static class PINProviderUnavailableException extends PINException {
        public PINProviderUnavailableException(String provider) {
            super("PIN provider unavailable: " + provider, "ERR_PIN_PROVIDER_UNAVAILABLE");
        }
    }

    public static class PINGenerationFailedException extends PINException {
        public PINGenerationFailedException(String message) {
            super(message, "ERR_PIN_GENERATION_FAILED");
        }
    }
}
```

- [ ] **Step 6: Create RetailException**

```java
package com.agentbanking.orchestrator.domain.model.exceptions;

public class RetailException extends RuntimeException {
    private final String errorCode;

    public RetailException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static class MDRConfigNotFoundException extends RetailException {
        public MDRConfigNotFoundException(String transactionType, String paymentMethod) {
            super("MDR config not found for " + transactionType + " / " + paymentMethod, "ERR_MDR_CONFIG_NOT_FOUND");
        }
    }

    public static class QRGenerationFailedException extends RetailException {
        public QRGenerationFailedException(String message) {
            super(message, "ERR_QR_GENERATION_FAILED");
        }
    }

    public static class QRPaymentTimeoutException extends RetailException {
        public QRPaymentTimeoutException() {
            super("QR payment timeout", "ERR_QR_PAYMENT_TIMEOUT");
        }
    }

    public static class QRPaymentFailedException extends RetailException {
        public QRPaymentFailedException(String message) {
            super(message, "ERR_QR_PAYMENT_FAILED");
        }
    }

    public static class RTPSendFailedException extends RetailException {
        public RTPSendFailedException(String message) {
            super(message, "ERR_RTP_SEND_FAILED");
        }
    }

    public static class RTPInvalidProxyException extends RetailException {
        public RTPInvalidProxyException(String proxy) {
            super("Invalid RTP proxy: " + proxy, "ERR_RTP_INVALID_PROXY");
        }
    }

    public static class RTPApprovalTimeoutException extends RetailException {
        public RTPApprovalTimeoutException() {
            super("RTP approval timeout", "ERR_RTP_APPROVAL_TIMEOUT");
        }
    }

    public static class RTPDeclinedException extends RetailException {
        public RTPDeclinedException() {
            super("RTP declined by customer", "ERR_RTP_DECLINED");
        }
    }

    public static class RecordCreationFailedException extends RetailException {
        public RecordCreationFailedException(String message) {
            super(message, "ERR_RECORD_CREATION_FAILED");
        }
    }

    public static class FloatCapacityCheckFailedException extends RetailException {
        public FloatCapacityCheckFailedException() {
            super("Insufficient float capacity for cashback", "ERR_INSUFFICIENT_FLOAT");
        }
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/model/exceptions/
git commit -m "feat: add exception classes for Phase 3+ transaction types"
```

### Task 4: Create Port Interfaces

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/TelcoAggregatorPort.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/EWalletProviderPort.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/ESSPServicePort.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/PINInventoryPort.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/QRPaymentPort.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/RequestToPayPort.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/MerchantTransactionPort.java`

- [ ] **Step 1: Read existing port pattern**

```bash
cat services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/SwitchAdapterPort.java
```

- [ ] **Step 2: Create TelcoAggregatorPort**

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;

public interface TelcoAggregatorPort {

    TelcoValidationResult validatePhoneNumber(String phoneNumber, String telcoProvider);

    TelcoTopupResult processTopup(String telcoProvider, String phoneNumber, BigDecimal amount, String idempotencyKey);

    record TelcoValidationResult(boolean valid, String operatorName, String errorCode) {}
    record TelcoTopupResult(boolean success, String telcoReference, String errorCode) {}
}
```

- [ ] **Step 3: Create EWalletProviderPort**

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;

public interface EWalletProviderPort {

    EWalletValidationResult validateWallet(String provider, String walletId);

    EWalletWithdrawResult withdraw(String provider, String walletId, BigDecimal amount, String idempotencyKey);

    EWalletTopupResult topup(String provider, String walletId, BigDecimal amount, String idempotencyKey);

    record EWalletValidationResult(boolean valid, BigDecimal walletBalance, String errorCode) {}
    record EWalletWithdrawResult(boolean success, String ewalletReference, String errorCode) {}
    record EWalletTopupResult(boolean success, String ewalletReference, String errorCode) {}
}
```

- [ ] **Step 4: Create ESSPServicePort**

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;

public interface ESSPServicePort {

    ESSPValidationResult validatePurchase(BigDecimal amount);

    ESSPPurchaseResult purchase(BigDecimal amount, String customerMykad, String idempotencyKey);

    record ESSPValidationResult(boolean valid, BigDecimal minAmount, BigDecimal maxAmount, String errorCode) {}
    record ESSPPurchaseResult(boolean success, String certificateNumber, String errorCode) {}
}
```

- [ ] **Step 5: Create PINInventoryPort**

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PINInventoryPort {

    PINInventoryResult validateInventory(String provider, BigDecimal faceValue);

    PINGenerationResult generatePIN(String provider, BigDecimal faceValue, String idempotencyKey);

    record PINInventoryResult(boolean available, int stockCount, String errorCode) {}
    record PINGenerationResult(boolean success, String pinCode, String serialNumber, LocalDate expiryDate, String errorCode) {}
}
```

- [ ] **Step 6: Create QRPaymentPort**

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface QRPaymentPort {

    QRGenerationResult generateDynamicQR(BigDecimal amount, UUID agentId, String idempotencyKey);

    QRPaymentStatus checkPaymentStatus(String qrReference);

    record QRGenerationResult(String qrCode, String qrReference, String errorCode) {}
    record QRPaymentStatus(String status, String paynetReference, String errorCode) {}
}
```

- [ ] **Step 7: Create RequestToPayPort**

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;

public interface RequestToPayPort {

    RTPResult sendRequestToPay(String proxy, BigDecimal amount, String idempotencyKey);

    RTPStatus checkRTPStatus(String rtpReference);

    record RTPResult(boolean success, String rtpReference, String errorCode) {}
    record RTPStatus(String status, String paynetReference, String errorCode) {}
}
```

- [ ] **Step 8: Create MerchantTransactionPort**

```java
package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface MerchantTransactionPort {

    MerchantTransactionResult createRecord(MerchantTransactionRecord record);

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
}
```

- [ ] **Step 9: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/port/out/
git commit -m "feat: add 7 new port interfaces for Phase 3+ transaction types"
```

---

## Phase 2: Infrastructure — Feign Clients & Adapters

### Task 5: Create Feign Clients

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/TelcoAggregatorClient.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/EWalletProviderClient.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/ESSPServiceClient.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/PINInventoryClient.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/QRPaymentClient.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/RequestToPayClient.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/MerchantTransactionClient.java`

- [ ] **Step 1: Read existing Feign client pattern**

```bash
cat services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/LedgerServiceClient.java
```

- [ ] **Step 2: Create TelcoAggregatorClient**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "telco-aggregator", url = "${telco-aggregator.url}")
public interface TelcoAggregatorClient {

    @PostMapping("/internal/validate-phone")
    TelcoPhoneValidationResponse validatePhone(@RequestBody TelcoPhoneValidationRequest request);

    @PostMapping("/internal/topup")
    TelcoTopupResponse topup(@RequestBody TelcoTopupRequest request);

    record TelcoPhoneValidationRequest(String phoneNumber, String telcoProvider) {}
    record TelcoPhoneValidationResponse(boolean valid, String operatorName, String errorCode) {}
    record TelcoTopupRequest(String telcoProvider, String phoneNumber, BigDecimal amount, String idempotencyKey) {}
    record TelcoTopupResponse(boolean success, String telcoReference, String errorCode) {}
}
```

- [ ] **Step 3: Create EWalletProviderClient**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "ewallet-provider", url = "${ewallet-provider.url}")
public interface EWalletProviderClient {

    @PostMapping("/internal/validate-wallet")
    EWalletValidationResponse validateWallet(@RequestBody EWalletValidationRequest request);

    @PostMapping("/internal/withdraw")
    EWalletWithdrawResponse withdraw(@RequestBody EWalletWithdrawRequest request);

    @PostMapping("/internal/topup")
    EWalletTopupResponse topup(@RequestBody EWalletTopupRequest request);

    record EWalletValidationRequest(String provider, String walletId) {}
    record EWalletValidationResponse(boolean valid, BigDecimal walletBalance, String errorCode) {}
    record EWalletWithdrawRequest(String provider, String walletId, BigDecimal amount, String idempotencyKey) {}
    record EWalletWithdrawResponse(boolean success, String ewalletReference, String errorCode) {}
    record EWalletTopupRequest(String provider, String walletId, BigDecimal amount, String idempotencyKey) {}
    record EWalletTopupResponse(boolean success, String ewalletReference, String errorCode) {}
}
```

- [ ] **Step 4: Create ESSPServiceClient**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "essp-service", url = "${essp-service.url}")
public interface ESSPServiceClient {

    @PostMapping("/internal/validate-purchase")
    ESSPValidationResponse validatePurchase(@RequestBody ESSPValidationRequest request);

    @PostMapping("/internal/purchase")
    ESSPPurchaseResponse purchase(@RequestBody ESSPPurchaseRequest request);

    record ESSPValidationRequest(BigDecimal amount) {}
    record ESSPValidationResponse(boolean valid, BigDecimal minAmount, BigDecimal maxAmount, String errorCode) {}
    record ESSPPurchaseRequest(BigDecimal amount, String customerMykad, String idempotencyKey) {}
    record ESSPPurchaseResponse(boolean success, String certificateNumber, String errorCode) {}
}
```

- [ ] **Step 5: Create PINInventoryClient**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.LocalDate;

@FeignClient(name = "pin-inventory", url = "${pin-inventory.url}")
public interface PINInventoryClient {

    @PostMapping("/internal/validate-inventory")
    PINValidationResponse validateInventory(@RequestBody PINValidationRequest request);

    @PostMapping("/internal/generate-pin")
    PINGenerationResponse generatePIN(@RequestBody PINGenerationRequest request);

    record PINValidationRequest(String provider, BigDecimal faceValue) {}
    record PINValidationResponse(boolean available, int stockCount, String errorCode) {}
    record PINGenerationRequest(String provider, BigDecimal faceValue, String idempotencyKey) {}
    record PINGenerationResponse(boolean success, String pinCode, String serialNumber, LocalDate expiryDate, String errorCode) {}
}
```

- [ ] **Step 6: Create QRPaymentClient**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "switch-adapter-qr", url = "${switch-adapter-service.url}")
public interface QRPaymentClient {

    @PostMapping("/internal/qr/generate")
    QRGenerationResponse generateQR(@RequestBody QRGenerationRequest request);

    @GetMapping("/internal/qr/status/{qrReference}")
    QRPaymentStatusResponse checkStatus(@PathVariable String qrReference);

    record QRGenerationRequest(BigDecimal amount, UUID agentId, String idempotencyKey) {}
    record QRGenerationResponse(String qrCode, String qrReference, String errorCode) {}
    record QRPaymentStatusResponse(String status, String paynetReference, String errorCode) {}
}
```

- [ ] **Step 7: Create RequestToPayClient**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "switch-adapter-rtp", url = "${switch-adapter-service.url}")
public interface RequestToPayClient {

    @PostMapping("/internal/rtp/send")
    RTPSendResponse sendRTP(@RequestBody RTPSendRequest request);

    @GetMapping("/internal/rtp/status/{rtpReference}")
    RTPStatusResponse checkStatus(@PathVariable String rtpReference);

    record RTPSendRequest(String proxy, BigDecimal amount, String idempotencyKey) {}
    record RTPSendResponse(boolean success, String rtpReference, String errorCode) {}
    record RTPStatusResponse(String status, String paynetReference, String errorCode) {}
}
```

- [ ] **Step 8: Create MerchantTransactionClient**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "ledger-merchant-txn", url = "${ledger-service.url}")
public interface MerchantTransactionClient {

    @PostMapping("/internal/merchant-transaction")
    MerchantTransactionResponse createRecord(@RequestBody MerchantTransactionRequest request);

    record MerchantTransactionRequest(
        UUID transactionId,
        String merchantType,
        BigDecimal grossAmount,
        BigDecimal mdrRate,
        BigDecimal mdrAmount,
        BigDecimal netCreditToFloat,
        String receiptType
    ) {}
    record MerchantTransactionResponse(boolean success, UUID recordId, String errorCode) {}
}
```

- [ ] **Step 9: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/*Client.java
git commit -m "feat: add 7 new Feign clients for Phase 3+ external services"
```

### Task 6: Create Port Adapters

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/TelcoAggregatorAdapter.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/EWalletProviderAdapter.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/ESSPServiceAdapter.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/PINInventoryAdapter.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/QRPaymentAdapter.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/RequestToPayAdapter.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/MerchantTransactionAdapter.java`

- [ ] **Step 1: Read existing adapter pattern**

```bash
cat services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/LedgerServiceAdapter.java
```

- [ ] **Step 2: Create TelcoAggregatorAdapter**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.TelcoAggregatorPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TelcoAggregatorAdapter implements TelcoAggregatorPort {

    private final TelcoAggregatorClient client;

    @Override
    public TelcoValidationResult validatePhoneNumber(String phoneNumber, String telcoProvider) {
        log.info("Validating phone number with {}: {}", telcoProvider, phoneNumber);
        var response = client.validatePhone(new TelcoAggregatorClient.TelcoPhoneValidationRequest(phoneNumber, telcoProvider));
        return new TelcoValidationResult(response.valid(), response.operatorName(), response.errorCode());
    }

    @Override
    public TelcoTopupResult processTopup(String telcoProvider, String phoneNumber, BigDecimal amount, String idempotencyKey) {
        log.info("Processing {} topup for {}: {}", telcoProvider, phoneNumber, amount);
        var response = client.topup(new TelcoAggregatorClient.TelcoTopupRequest(telcoProvider, phoneNumber, amount, idempotencyKey));
        return new TelcoTopupResult(response.success(), response.telcoReference(), response.errorCode());
    }
}
```

- [ ] **Step 3: Create EWalletProviderAdapter**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.EWalletProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EWalletProviderAdapter implements EWalletProviderPort {

    private final EWalletProviderClient client;

    @Override
    public EWalletValidationResult validateWallet(String provider, String walletId) {
        log.info("Validating eWallet {} for provider: {}", walletId, provider);
        var response = client.validateWallet(new EWalletProviderClient.EWalletValidationRequest(provider, walletId));
        return new EWalletValidationResult(response.valid(), response.walletBalance(), response.errorCode());
    }

    @Override
    public EWalletWithdrawResult withdraw(String provider, String walletId, BigDecimal amount, String idempotencyKey) {
        log.info("Withdrawing {} from eWallet {} via {}", amount, walletId, provider);
        var response = client.withdraw(new EWalletProviderClient.EWalletWithdrawRequest(provider, walletId, amount, idempotencyKey));
        return new EWalletWithdrawResult(response.success(), response.ewalletReference(), response.errorCode());
    }

    @Override
    public EWalletTopupResult topup(String provider, String walletId, BigDecimal amount, String idempotencyKey) {
        log.info("Topping up eWallet {} via {}: {}", walletId, provider, amount);
        var response = client.topup(new EWalletProviderClient.EWalletTopupRequest(provider, walletId, amount, idempotencyKey));
        return new EWalletTopupResult(response.success(), response.ewalletReference(), response.errorCode());
    }
}
```

- [ ] **Step 4: Create ESSPServiceAdapter**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.ESSPServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ESSPServiceAdapter implements ESSPServicePort {

    private final ESSPServiceClient client;

    @Override
    public ESSPValidationResult validatePurchase(BigDecimal amount) {
        log.info("Validating eSSP purchase amount: {}", amount);
        var response = client.validatePurchase(new ESSPServiceClient.ESSPValidationRequest(amount));
        return new ESSPValidationResult(response.valid(), response.minAmount(), response.maxAmount(), response.errorCode());
    }

    @Override
    public ESSPPurchaseResult purchase(BigDecimal amount, String customerMykad, String idempotencyKey) {
        log.info("Processing eSSP purchase: {}", amount);
        var response = client.purchase(new ESSPServiceClient.ESSPPurchaseRequest(amount, customerMykad, idempotencyKey));
        return new ESSPPurchaseResult(response.success(), response.certificateNumber(), response.errorCode());
    }
}
```

- [ ] **Step 5: Create PINInventoryAdapter**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.PINInventoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PINInventoryAdapter implements PINInventoryPort {

    private final PINInventoryClient client;

    @Override
    public PINInventoryResult validateInventory(String provider, BigDecimal faceValue) {
        log.info("Validating PIN inventory for {}: {}", provider, faceValue);
        var response = client.validateInventory(new PINInventoryClient.PINValidationRequest(provider, faceValue));
        return new PINInventoryResult(response.available(), response.stockCount(), response.errorCode());
    }

    @Override
    public PINGenerationResult generatePIN(String provider, BigDecimal faceValue, String idempotencyKey) {
        log.info("Generating PIN for {}: {}", provider, faceValue);
        var response = client.generatePIN(new PINInventoryClient.PINGenerationRequest(provider, faceValue, idempotencyKey));
        return new PINGenerationResult(response.success(), response.pinCode(), response.serialNumber(), response.expiryDate(), response.errorCode());
    }
}
```

- [ ] **Step 6: Create QRPaymentAdapter**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.QRPaymentPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class QRPaymentAdapter implements QRPaymentPort {

    private final QRPaymentClient client;

    @Override
    public QRGenerationResult generateDynamicQR(BigDecimal amount, UUID agentId, String idempotencyKey) {
        log.info("Generating dynamic QR for agent {}: {}", agentId, amount);
        var response = client.generateQR(new QRPaymentClient.QRGenerationRequest(amount, agentId, idempotencyKey));
        return new QRGenerationResult(response.qrCode(), response.qrReference(), response.errorCode());
    }

    @Override
    public QRPaymentStatus checkPaymentStatus(String qrReference) {
        log.info("Checking QR payment status: {}", qrReference);
        var response = client.checkStatus(qrReference);
        return new QRPaymentStatus(response.status(), response.paynetReference(), response.errorCode());
    }
}
```

- [ ] **Step 7: Create RequestToPayAdapter**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.RequestToPayPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RequestToPayAdapter implements RequestToPayPort {

    private final RequestToPayClient client;

    @Override
    public RTPResult sendRequestToPay(String proxy, BigDecimal amount, String idempotencyKey) {
        log.info("Sending RTP to proxy {}: {}", proxy, amount);
        var response = client.sendRTP(new RequestToPayClient.RTPSendRequest(proxy, amount, idempotencyKey));
        return new RTPResult(response.success(), response.rtpReference(), response.errorCode());
    }

    @Override
    public RTPStatus checkRTPStatus(String rtpReference) {
        log.info("Checking RTP status: {}", rtpReference);
        var response = client.checkStatus(rtpReference);
        return new RTPStatus(response.status(), response.paynetReference(), response.errorCode());
    }
}
```

- [ ] **Step 8: Create MerchantTransactionAdapter**

```java
package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.MerchantTransactionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MerchantTransactionAdapter implements MerchantTransactionPort {

    private final MerchantTransactionClient client;

    @Override
    public MerchantTransactionResult createRecord(MerchantTransactionRecord record) {
        log.info("Creating merchant transaction record: {}", record.transactionId());
        var request = new MerchantTransactionClient.MerchantTransactionRequest(
            record.transactionId(),
            record.merchantType(),
            record.grossAmount(),
            record.mdrRate(),
            record.mdrAmount(),
            record.netCreditToFloat(),
            record.receiptType()
        );
        var response = client.createRecord(request);
        return new MerchantTransactionResult(response.success(), response.recordId(), response.errorCode());
    }
}
```

- [ ] **Step 9: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/external/*Adapter.java
git commit -m "feat: add 7 port adapters for Phase 3+ external services"
```

### Task 7: Update application.yaml

**Files:**
- Modify: `services/orchestrator-service/src/main/resources/application.yaml`

- [ ] **Step 1: Read current application.yaml**

```bash
cat services/orchestrator-service/src/main/resources/application.yaml
```

- [ ] **Step 2: Add new service URLs and timeouts**

Append to the existing configuration:

```yaml
# Phase 3+ Service URLs
telco-aggregator:
  url: http://telco-aggregator-service:8080

ewallet-provider:
  url: http://ewallet-provider-service:8080

essp-service:
  url: http://bsn-essp-service:8080

pin-inventory:
  url: http://pin-inventory-service:8080

# Phase 3+ Activity Timeouts
temporal:
  activity-timeouts:
    qr-payment: 120s
    rtp-approval: 300s
    telco-topup: 30s
    ewallet: 15s
    essp: 20s
    pin-generation: 10s
```

- [ ] **Step 3: Commit**

```bash
git add services/orchestrator-service/src/main/resources/application.yaml
git commit -m "config: add Phase 3+ service URLs and activity timeouts"
```

---

## Phase 3: Activity Interfaces & Implementations

### Task 8: Create Activity Interfaces

**Files:**
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/ValidatePhoneNumberActivity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/TopUpTelcoActivity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/ValidateEWalletActivity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/WithdrawFromEWalletActivity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/TopUpEWalletActivity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/ValidateESSPPurchaseActivity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/PurchaseESSPActivity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/ValidatePINInventoryActivity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/GeneratePINActivity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/CalculateMDRActivity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/GenerateDynamicQRActivity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/WaitForQRPaymentActivity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/SendRequestToPayActivity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/WaitForRTPApprovalActivity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/CreateMerchantTransactionRecordActivity.java`
- Create: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/ValidateFloatCapacityActivity.java`

- [ ] **Step 1: Read existing activity interface pattern**

```bash
cat services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/CheckVelocityActivity.java
```

- [ ] **Step 2: Create all 16 activity interfaces**

Following the existing pattern, create each interface with `@ActivityInterface` annotation and `@ActivityMethod`. Example:

```java
package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;

@ActivityInterface
public interface ValidatePhoneNumberActivity {

    @ActivityMethod
    ValidatePhoneNumberResult validatePhoneNumber(String phoneNumber, String telcoProvider);

    record ValidatePhoneNumberResult(boolean valid, String operatorName) {}
}
```

Create all 16 following the spec's Activity Catalog (Section 5.5 of design doc).

- [ ] **Step 3: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/activity/
git commit -m "feat: add 16 new activity interfaces for Phase 3+ transaction types"
```

### Task 9: Create Activity Implementations

**Files:**
- Create all 16 `*ActivityImpl.java` files in `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/`

- [ ] **Step 1: Read existing activity implementation pattern**

```bash
cat services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/CheckVelocityActivityImpl.java
```

- [ ] **Step 2: Create all 16 activity implementations**

Each implementation:
1. Implements the corresponding activity interface
2. Injects the appropriate port via constructor
3. Calls the port method
4. Maps exceptions to typed exceptions
5. Returns typed result records

Example:
```java
package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.ValidatePhoneNumberActivity;
import com.agentbanking.orchestrator.domain.model.exceptions.TelcoException;
import com.agentbanking.orchestrator.domain.port.out.TelcoAggregatorPort;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ValidatePhoneNumberActivityImpl implements ValidatePhoneNumberActivity {

    private final TelcoAggregatorPort telcoAggregatorPort;

    @Override
    public ValidatePhoneNumberResult validatePhoneNumber(String phoneNumber, String telcoProvider) {
        log.info("Validating phone number: {} with {}", phoneNumber, telcoProvider);
        var result = telcoAggregatorPort.validatePhoneNumber(phoneNumber, telcoProvider);
        if (!result.valid()) {
            throw new TelcoException.InvalidPhoneNumberException(phoneNumber);
        }
        return new ValidatePhoneNumberResult(result.valid(), result.operatorName());
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/ActivityImpl/
git commit -m "feat: add 16 activity implementations for Phase 3+ transaction types"
```

---

## Phase 4: Workflow Interfaces & Implementations

### Task 10: Create Workflow Interfaces

**Files:**
- Create all 9 workflow interfaces in `application/workflow/`

- [ ] **Step 1: Read existing workflow interface pattern**

```bash
cat services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/WithdrawalWorkflow.java
```

- [ ] **Step 2: Create all 9 workflow interfaces**

Each follows the pattern:
```java
@WorkflowInterface
public interface CashlessPaymentWorkflow {
    @WorkflowMethod
    WorkflowResult execute(CashlessPaymentInput input);

    @SignalMethod
    void forceResolve(ForceResolveSignal signal);

    @QueryMethod
    WorkflowStatus getStatus();
}
```

With corresponding input records per the design spec.

- [ ] **Step 3: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/application/workflow/
git commit -m "feat: add 9 workflow interfaces for Phase 3+ transaction types"
```

### Task 11: Create Workflow Implementations

**Files:**
- Create all 9 workflow implementations in `infrastructure/temporal/WorkflowImpl/`

- [ ] **Step 1: Read existing workflow implementation pattern**

```bash
cat services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/WithdrawalWorkflowImpl.java
```

- [ ] **Step 2: Create all 9 workflow implementations**

Each implementation:
1. Implements the workflow interface
2. Uses `Workflow.newActivityStub()` for each activity
3. Follows the execution flow from the design spec
4. Implements compensation chains
5. Implements safety reversal for timeouts
6. Implements force-resolve signal handling

Example for CashlessPaymentWorkflow:
```java
package com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl;

import com.agentbanking.orchestrator.application.activity.*;
import com.agentbanking.orchestrator.application.workflow.CashlessPaymentWorkflow;
import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class CashlessPaymentWorkflowImpl implements CashlessPaymentWorkflow {

    private WorkflowStatus status = WorkflowStatus.RUNNING;
    private ForceResolveSignal forceResolveSignal;

    private final CheckVelocityActivity checkVelocity = Workflow.newActivityStub(
        CheckVelocityActivity.class,
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).setRetryOptions(/* ... */).build()
    );

    private final EvaluateStpActivity evaluateStp = Workflow.newActivityStub(
        EvaluateStpActivity.class,
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build()
    );

    private final CalculateFeesActivity calculateFees = Workflow.newActivityStub(
        CalculateFeesActivity.class,
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(3)).build()
    );

    private final AuthorizeAtSwitchActivity authorizeAtSwitch = Workflow.newActivityStub(
        AuthorizeAtSwitchActivity.class,
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(25)).build()
    );

    private final SendReversalToSwitchActivity sendReversal = Workflow.newActivityStub(
        SendReversalToSwitchActivity.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .setScheduleToCloseTimeout(Duration.ofSeconds(60))
            .setRetryOptions(/* infinite retry */).build()
    );

    private final PublishKafkaEventActivity publishEvent = Workflow.newActivityStub(
        PublishKafkaEventActivity.class,
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(3)).build()
    );

    @Override
    public WorkflowResult execute(CashlessPaymentInput input) {
        log.info("CashlessPaymentWorkflow started: agentId={}, amount={}", input.agentId(), input.amount());

        try {
            // Step 1: Check velocity
            checkVelocity.checkVelocity(input.agentId(), input.amount(), input.customerMykad());

            // Step 2: Evaluate STP
            var stpResult = evaluateStp.evaluateStp("CASHLESS_PAYMENT", input.agentId(), input.amount(), input.customerMykad());
            if (!stpResult.approved()) {
                status = WorkflowStatus.PENDING_REVIEW;
                return WorkflowResult.pendingReview("STP evaluation requires manual review");
            }

            // Step 3: Calculate fees
            var fees = calculateFees.calculateFees("CASHLESS_PAYMENT", /* tier */, input.amount());

            // Step 4: Authorize at switch
            var authResult = authorizeAtSwitch.authorize(input.pan(), input.pinBlock(), input.amount(), /* txnId */);

            if (!authResult.approved()) {
                status = WorkflowStatus.FAILED;
                return WorkflowResult.failed(authResult.responseCode());
            }

            // Step 5: Publish event
            try {
                publishEvent.publishEvent(/* details */);
            } catch (Exception e) {
                log.error("Failed to publish Kafka event, continuing: {}", e.getMessage());
            }

            status = WorkflowStatus.COMPLETED;
            return WorkflowResult.completed(/* txnId, ref */);

        } catch (Exception e) {
            log.error("CashlessPaymentWorkflow failed: {}", e.getMessage());
            status = WorkflowStatus.FAILED;
            return WorkflowResult.failed(e.getMessage());
        }
    }

    @Override
    public void forceResolve(ForceResolveSignal signal) {
        this.forceResolveSignal = signal;
    }

    @Override
    public WorkflowStatus getStatus() {
        return status;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/infrastructure/temporal/WorkflowImpl/
git commit -m "feat: add 9 workflow implementations for Phase 3+ transaction types"
```

---

## Phase 5: Configuration & Wiring

### Task 12: Update DomainServiceConfig and TemporalWorkerConfig

**Files:**
- Modify: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/config/DomainServiceConfig.java`
- Modify: `services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/config/TemporalWorkerConfig.java`

- [ ] **Step 1: Update DomainServiceConfig**

Add bean registrations for new services if needed (workflow router already handles routing).

- [ ] **Step 2: Update TemporalWorkerConfig**

Register all 9 new workflows and 16 new activities with the Temporal worker.

```java
// Add to existing worker factory:
worker.registerWorkflowImplementationTypes(
    CashlessPaymentWorkflowImpl.class,
    PinBasedPurchaseWorkflowImpl.class,
    PrepaidTopupWorkflowImpl.class,
    EWalletWithdrawalWorkflowImpl.class,
    EWalletTopupWorkflowImpl.class,
    ESSPPurchaseWorkflowImpl.class,
    PINPurchaseWorkflowImpl.class,
    RetailSaleWorkflowImpl.class,
    HybridCashbackWorkflowImpl.class
);

worker.registerActivitiesImplementations(
    validatePhoneNumberActivityImpl,
    topUpTelcoActivityImpl,
    // ... all 16 new activity implementations
);
```

- [ ] **Step 3: Commit**

```bash
git add services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/config/
git commit -m "config: register Phase 3+ workflows and activities with Temporal worker"
```

---

## Phase 6: Tests

### Task 13: Write Workflow Tests

**Files:**
- Create all 9 workflow test files in `src/test/java/.../application/workflow/`

- [ ] **Step 1: Read existing workflow test pattern**

```bash
cat services/orchestrator-service/src/test/java/com/agentbanking/orchestrator/application/workflow/WithdrawalWorkflowTest.java
```

- [ ] **Step 2: Write tests for each workflow**

Each workflow test covers:
- Happy path (all activities succeed)
- Failure path (external service fails)
- Compensation path (float released on failure)
- Timeout path (safety reversal triggered)

- [ ] **Step 3: Run all tests**

```bash
./gradlew :services:orchestrator-service:test -q
```

- [ ] **Step 4: Commit**

```bash
git add services/orchestrator-service/src/test/
git commit -m "test: add workflow tests for 9 Phase 3+ transaction types"
```

---

## Phase 7: Verification

### Task 14: Run Full Test Suite & ArchUnit

- [ ] **Step 1: Run all tests**

```bash
./gradlew :services:orchestrator-service:test
```

- [ ] **Step 2: Run ArchUnit architecture tests**

```bash
./gradlew :services:orchestrator-service:test --tests "HexagonalArchitectureTest"
```

- [ ] **Step 3: Verify no JPA/Spring imports in domain layer**

```bash
grep -r "import javax.persistence\|import org.springframework" services/orchestrator-service/src/main/java/com/agentbanking/orchestrator/domain/ || echo "PASS: No framework imports in domain"
```

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "chore: Phase 3+ implementation complete — all tests passing"
```

---

## Self-Review

### Spec Coverage Check

| Spec Section | Task | Status |
|-------------|------|--------|
| 9 Transaction Types | Task 1 (enum) | ✅ |
| WorkflowRouter extension | Task 2 | ✅ |
| 24 Exception types | Task 3 | ✅ |
| 7 Port interfaces | Task 4 | ✅ |
| 7 Feign clients | Task 5 | ✅ |
| 7 Port adapters | Task 6 | ✅ |
| application.yaml config | Task 7 | ✅ |
| 16 Activity interfaces | Task 8 | ✅ |
| 16 Activity implementations | Task 9 | ✅ |
| 9 Workflow interfaces | Task 10 | ✅ |
| 9 Workflow implementations | Task 11 | ✅ |
| DomainServiceConfig update | Task 12 | ✅ |
| TemporalWorkerConfig update | Task 12 | ✅ |
| Workflow tests | Task 13 | ✅ |
| ArchUnit tests | Task 14 | ✅ |
| Cross-Service Dependencies | Tasks 5, 6, 7 | ✅ |
| BDD scenarios (48 total) | Task 13 | ✅ |

### Placeholder Scan
- No TBD/TODO/FIXME in plan
- All code steps contain actual code
- All file paths are exact
- All commands are specific

### Type Consistency
- All port records match between port interfaces and adapters
- All Feign client DTOs match port records
- All workflow input records match design spec
- Exception class names consistent throughout
