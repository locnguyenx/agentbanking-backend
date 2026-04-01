# Gateway Request Transformation Specification

**Version:** 3.0  
**Date:** 2026-03-31  
**Status:** Implemented

---

## 1. Overview

### 1.1 Purpose

This specification defines how the API Gateway transforms external API requests into internal API format before forwarding to backend services.

### 1.2 Design Decisions

| Decision | Choice |
|----------|--------|
| Configuration | YAML-driven (`RequestTransform=withdrawal`) |
| Error handling | Return 400 Bad Request on transformation failure |
| Response transformation | Yes, gateway transforms responses |
| Implementation | Single filter factory with both request and response transforms |

### 1.3 Scope

| External Endpoint | Internal Endpoint | Backend Service | Transform Type |
|-------------------|-------------------|-----------------|----------------|
| POST /api/v1/withdrawal | POST /internal/debit | ledger-service | withdrawal |
| POST /api/v1/deposit | POST /internal/credit | ledger-service | deposit |
| POST /api/v1/balance-inquiry | POST /internal/balance-inquiry | ledger-service | balance-inquiry |
| GET /api/v1/agent/balance | GET /internal/balance/{agentId} | ledger-service | (passthrough) |
| POST /api/v1/topup | POST /internal/topup | biller-service | topup |
| POST /api/v1/bill/pay | POST /internal/pay-bill | biller-service | bill-pay |
| POST /api/v1/billpayment/jompay | POST /internal/jompay | biller-service | jompay |
| POST /api/v1/ewallet/withdraw | POST /internal/ewallet/withdrawal | biller-service | ewallet |
| POST /api/v1/ewallet/topup | POST /internal/ewallet/topup | biller-service | ewallet |
| POST /api/v1/transfer/duitnow | POST /internal/duitnow | switch-adapter-service | duitnow |
| POST /api/v1/retail/sale | POST /internal/merchant/retail-sale | ledger-service | retail-sale |
| POST /api/v1/retail/pin-purchase | POST /internal/merchant/pin-purchase | ledger-service | retail-pin-purchase |
| POST /api/v1/retail/cashback | POST /internal/merchant/cash-back | ledger-service | retail-cashback |
| POST /api/v1/essp/purchase | POST /internal/essp/purchase | biller-service | essp |
| POST /api/v1/rules/fees | POST /internal/fees | rules-service | rules-fees |
| POST /api/v1/kyc/verify | POST /internal/verify-mykad | onboarding-service | kyc-verify |
| POST /api/v1/kyc/biometric | POST /internal/biometric | onboarding-service | kyc-biometric |
| POST /api/v1/onboarding/submit-application | POST /internal/onboarding/application | onboarding-service | onboarding |

---

## 2. Architecture

### 2.1 Processing Flow

```
External Request → JwtAuth → RequestTransform → RewritePath → Internal Service
                                      ↓
                              Response Transform → External Response
```

### 2.2 Component Responsibilities

| Component | Responsibility | Implementation |
|-----------|---------------|----------------|
| JwtAuth | Validates JWT, extracts agentId, adds X-Agent-Id header | `JwtAuthGatewayFilterFactory` |
| RequestTransform | Reads body, transforms fields, rewrites body | `RequestTransformGatewayFilterFactory` |
| RewritePath | Rewrites URL path | Spring built-in filter |

### 2.3 Naming Convention (Critical)

Spring Cloud Gateway strips `GatewayFilterFactory` suffix to match YAML references:

| Class Name | YAML Reference |
|------------|----------------|
| `JwtAuthGatewayFilterFactory` | `JwtAuth` |
| `RequestTransformGatewayFilterFactory` | `RequestTransform` |

**Lesson learned:** Class names MUST end with `GatewayFilterFactory`.

### 2.4 Compact Notation Requirement

Using `- RequestTransform=withdrawal` requires overriding `shortcutFieldOrder()`:

```java
@Override
public List<String> shortcutFieldOrder() {
    return Arrays.asList("type");
}
```

---

## 3. Implementation

### 3.1 Files

```
gateway/src/main/java/com/agentbanking/gateway/
├── filter/
│   ├── JwtAuthGatewayFilterFactory.java     # JWT authentication
│   └── RequestTransformGatewayFilterFactory.java  # Request/response transforms
└── transform/
    └── Transformers.java                    # Utility functions
```

### 3.2 YAML Configuration Example

```yaml
- id: external-withdrawal
  uri: http://ledger-service:8082
  predicates:
    - Path=/api/v1/withdrawal
  filters:
    - JwtAuth                              # Auth first
    - RequestTransform=withdrawal          # Transform second
    - RewritePath=/api/v1/withdrawal, /internal/debit  # Route last
```

### 3.3 Filter Factory Structure

```java
@Component
public class RequestTransformGatewayFilterFactory 
    extends AbstractGatewayFilterFactory<RequestTransformGatewayFilterFactory.Config> {

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("type");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            // 1. Read request body
            // 2. Transform to internal format
            // 3. Create decorated request with transformed body
            // 4. Create decorated response for response transformation
            // 5. Continue chain
        }, Ordered.HIGHEST_PRECEDENCE + 1);
    }

    public static class Config {
        private String type;
        // getters/setters
    }
}
```

---

## 4. Testing

### 4.1 Unit Tests

| Test File | Tests |
|-----------|-------|
| `JwtAuthGatewayFilterFactoryTest.java` | Token validation, header extraction, expired token |
| `RequestTransformGatewayFilterFactoryTest.java` | Request transformation, response transformation, error handling |

### 4.2 E2E Tests Structure

E2E tests follow a phased execution order using `@TestMethodOrder` and `@Order`:

```
gateway/src/test/java/com/agentbanking/gateway/integration/
├── BaseIntegrationTest.java              # Shared infrastructure
├── setup/
│   ├── TestContext.java                  # Shared state (tokens, agentId)
│   ├── AuthSetupTest.java                # Phase 1: Create users, get tokens
│   ├── AgentOnboardingTest.java          # Phase 2: Create agents, get UUIDs
│   ├── FeeConfigTest.java                # Phase 3: Configure fees
│   └── FloatSetupTest.java              # Phase 4: Ensure float balance
├── transactions/
│   ├── WithdrawalTest.java               # Phase 5a: Cash withdrawal
│   ├── DepositTest.java                  # Phase 5b: Cash deposit
│   ├── TopupTest.java                    # Phase 5c: Prepaid topup
│   ├── BillPayTest.java                  # Phase 5d: Bill payment
│   ├── JomPayTest.java                   # Phase 5e: JomPay
│   ├── DuitNowTest.java                  # Phase 5f: DuitNow transfer
│   ├── EWalletTest.java                  # Phase 5g: E-wallet
│   ├── EsspTest.java                     # Phase 5h: eSSP purchase
│   └── RetailTest.java                   # Phase 5i: Retail/merchant
├── kyc/
│   ├── MyKadVerifyTest.java              # Phase 6a: MyKad verification
│   ├── BiometricTest.java                # Phase 6b: Biometric matching
│   └── AgentApplicationTest.java         # Phase 6c: Agent application
├── backoffice/
│   ├── DashboardTest.java                # Phase 7a: Dashboard metrics
│   ├── SettlementTest.java               # Phase 7b: Settlement reports
│   ├── DiscrepancyTest.java              # Phase 7c: Discrepancy resolution
│   ├── AgentManagementTest.java          # Phase 7d: Agent CRUD
│   ├── AuditLogTest.java                 # Phase 7e: Audit logs
│   └── KycReviewQueueTest.java           # Phase 7f: KYC review queue
└── gateway/
    ├── AuthenticationTest.java           # Phase 8a: JWT validation
    ├── TransformationTest.java           # Phase 8b: Transform verification
    └── ErrorHandlingTest.java            # Phase 8c: Error responses
```

### 4.3 Phase Details

| Phase | Test Class | Purpose |
|-------|-----------|---------|
| 1 | `AuthSetupTest` | Bootstrap admin, create users (agent001, operator001, etc.), obtain JWT tokens, store in `TestContext` |
| 2 | `AgentOnboardingTest` | Verify MyKad, create agent via `/backoffice/agents`, store real UUID in `TestContext.agentId` |
| 3 | `FeeConfigTest` | Create fee configs via `/internal/fees`, verify fee calculation works |
| 4 | `FloatSetupTest` | Check balance, top up via deposit API if needed |
| 5a | `WithdrawalTest` | Happy path, daily limit, duplicate idempotency, auth checks |
| 5b | `DepositTest` | Happy path, duplicate idempotency, auth checks |
| 5c | `TopupTest` | Celcom topup, auth checks |
| 5d | `BillPayTest` | Astro bill pay, auth checks |
| 5e | `JomPayTest` | JomPAY payment, auth checks |
| 5f | `DuitNowTest` | DuitNow transfer, auth checks |
| 5g | `EWalletTest` | E-wallet withdrawal/topup, auth checks |
| 5h | `EsspTest` | eSSP purchase, auth checks |
| 5i | `RetailTest` | Retail sale, PIN purchase, cashback, auth checks |
| 6a | `MyKadVerifyTest` | Valid/invalid MyKad, gateway passthrough, auth checks |
| 6b | `BiometricTest` | Biometric match, gateway passthrough, auth checks |
| 6c | `AgentApplicationTest` | Micro agent, standard agent application, gateway, auth checks |
| 7a | `DashboardTest` | Dashboard metrics, transactions, agents list, auth checks |
| 7b | `SettlementTest` | Today/yesterday settlement, auth checks |
| 7c | `DiscrepancyTest` | Maker/checker discrepancy resolution, auth checks |
| 7d | `AgentManagementTest` | List/get/update agents, auth checks |
| 7e | `AuditLogTest` | Audit logs, filtered audit logs, auth checks |
| 7f | `KycReviewQueueTest` | KYC review queue, auth checks |
| 8a | `AuthenticationTest` | Missing token, invalid token, malformed token, valid token, backoffice auth |
| 8b | `TransformationTest` | Request/response format verification |
| 8c | `ErrorHandlingTest` | Invalid JSON, empty body, error format, 404, public endpoints |

### 4.4 Shared State (TestContext)

```java
public class TestContext {
    public static final String GATEWAY_URL = "http://localhost:8080";
    
    // Phase 1: Auth tokens
    public static String adminToken;
    public static String agentToken;
    public static String makerToken;
    public static String checkerToken;
    // ... more tokens
    
    // Phase 1: User IDs
    public static UUID adminUserId;
    public static UUID agentUserId;
    
    // Phase 2: Agent IDs (from onboarding service)
    public static UUID agentId;
    public static String agentCode;
    
    // Phase 4: Float balance
    public static BigDecimal agentFloatBalance;
}
```

### 4.5 Gradle Tasks

```bash
./gradlew :gateway:test              # Unit tests only (excludes e2e)
./gradlew :gateway:e2eTest           # E2E tests (requires Docker)
./gradlew :gateway:cleanE2eTestData  # Clean up test data
```

---

## 5. Lessons Learned

See `docs\lessons-learned\2028-03-31-gateway-filter.md` for detailed lessons from implementation.

Key lessons:
1. Class names must end with `GatewayFilterFactory`
2. Must override `shortcutFieldOrder()` for compact YAML notation
3. Use SLF4J Logger, not `System.out.println` (Docker drops stdout)
4. Request decorator may interfere with `RewritePath` filter
5. Always release `DataBuffer` after reading
6. Response transformation requires intercepting `writeWith()`

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-03-30 | Initial approved version |
| 2.0 | 2026-03-31 | Updated with implementation details, added all transformers, naming convention lessons |
| 3.0 | 2026-03-31 | Added E2E test structure: 25 test classes, phase execution order, shared state, gradle tasks |
