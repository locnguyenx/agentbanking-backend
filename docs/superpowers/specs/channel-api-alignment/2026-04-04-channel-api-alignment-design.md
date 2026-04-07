# Design Specification: Channel API Alignment

**Version:** 1.0
**Date:** 2026-04-04
**Status:** Draft
**Module:** rules-service, biller-service, onboarding-service, gateway, docs/api/openapi.yaml
**BRD Reference:** `docs/superpowers/specs/channel-api-alignment/2026-04-04-channel-api-alignment-brd.md`
**BDD Reference:** `docs/superpowers/specs/channel-api-alignment/2026-04-04-channel-api-alignment-bdd.md`

---

## 1. Architecture Overview

All 3 endpoints follow the existing hexagonal architecture pattern in their respective services. The Spring Cloud Gateway routes external `/api/v1/*` paths to internal `/internal/*` endpoints with JWT authentication via the `JwtAuth` filter.

```
Channel App → Gateway (JwtAuth filter) → rules-service / biller-service / onboarding-service
```

---

## 2. Component: Transaction Quote (rules-service)

### 2.1 What it does
Calculates fees and commission for a transaction before execution using the rules-service fee engine.

### 2.2 How to use it
`POST /api/v1/transactions/quote` with `amount`, `serviceCode`, `fundingSource` in request body.

### 2.3 Dependencies
- Existing `FeeCalculationService` domain service
- Agent tier data from JWT claims

### 2.4 Files to create/modify

| File | Action | Purpose |
|------|--------|---------|
| `services/rules-service/.../web/dto/TransactionQuoteRequest.java` | Create | Request DTO record with validation |
| `services/rules-service/.../web/dto/TransactionQuoteResponse.java` | Create | Response DTO record |
| `services/rules-service/.../domain/port/in/TransactionQuoteUseCase.java` | Create | Inbound port interface + result record |
| `services/rules-service/.../application/usecase/TransactionQuoteUseCaseImpl.java` | Create | Use case implementation |
| `services/rules-service/.../infrastructure/web/RulesController.java` | Modify | Add `@PostMapping("/transactions/quote")` |
| `services/rules-service/.../config/DomainServiceConfig.java` | Modify | Register bean if needed |
| `gateway/application.yaml` | Modify | Add external + internal gateway routes |
| `common/security/ErrorCodes.java` | Modify | Add `ERR_BIZ_QUOTE_CALCULATION_FAILED` |

### 2.5 Data Flow
1. Channel App sends POST with quote request
2. Gateway validates JWT, rewrites path to `/internal/transactions/quote`
3. `RulesController` receives request, validates DTO
4. `TransactionQuoteUseCaseImpl` calls `FeeCalculationService`
5. Returns `TransactionQuoteResponse` with quoteId, amount, fee, total, commission

---

## 3. Component: Proxy Enquiry (biller-service)

### 3.1 What it does
Resolves DuitNow proxy IDs (phone, NRIC, passport, business registration) to registered account holder names.

### 3.2 How to use it
`GET /api/v1/transfer/proxy/enquiry?proxyId=...&proxyType=...`

### 3.3 Dependencies
- Downstream DuitNow switch (via biller-service proxy infrastructure)

### 3.4 Files to create/modify

| File | Action | Purpose |
|------|--------|---------|
| `services/biller-service/.../web/dto/ProxyEnquiryResponse.java` | Create | Response DTO record |
| `services/biller-service/.../domain/port/in/ProxyEnquiryUseCase.java` | Create | Inbound port interface + result record |
| `services/biller-service/.../application/usecase/ProxyEnquiryUseCaseImpl.java` | Create | Use case implementation |
| `services/biller-service/.../infrastructure/web/BillerController.java` | Modify | Add `@GetMapping("/transfer/proxy/enquiry")` |
| `services/biller-service/.../config/DomainServiceConfig.java` | Modify | Register bean if needed |
| `gateway/application.yaml` | Modify | Add external + internal gateway routes |
| `common/security/ErrorCodes.java` | Modify | Add `ERR_BIZ_PROXY_NOT_FOUND`, `ERR_EXT_PROXY_ENQUIRY_FAILED` |

### 3.5 Data Flow
1. Channel App sends GET with proxyId and proxyType query params
2. Gateway validates JWT, rewrites path to `/internal/transfer/proxy/enquiry`
3. `BillerController` receives request, validates params
4. `ProxyEnquiryUseCaseImpl` calls downstream DuitNow switch
5. Returns `{ "name": "...", "proxyType": "..." }`

---

## 4. Component: Compliance Status (onboarding-service)

### 4.1 What it does
Returns whether an agent is compliance-locked or unlocked, checking AML flags and regulatory holds.

### 4.2 How to use it
`GET /api/v1/compliance/status` — agent identity extracted from JWT token claims.

### 4.3 Dependencies
- Agent compliance data (from onboarding-service domain - KYC/AML status)

### 4.4 Files to create/modify

| File | Action | Purpose |
|------|--------|---------|
| `services/onboarding-service/.../web/dto/ComplianceStatusResponse.java` | Create | Response DTO record |
| `services/onboarding-service/.../domain/port/in/ComplianceStatusUseCase.java` | Create | Inbound port interface + result record |
| `services/onboarding-service/.../application/usecase/ComplianceStatusUseCaseImpl.java` | Create | Use case implementation |
| `services/onboarding-service/.../infrastructure/web/OnboardingController.java` | Modify | Add `@GetMapping("/compliance/status")` |
| `services/onboarding-service/.../config/DomainServiceConfig.java` | Modify | Register bean if needed |
| `gateway/application.yaml` | Modify | Add external + internal gateway routes |
| `common/security/ErrorCodes.java` | Modify | Add `ERR_BIZ_COMPLIANCE_CHECK_FAILED` |

### 4.5 Data Flow
1. Channel App sends GET request
2. Gateway validates JWT, rewrites path to `/internal/compliance/status`
3. `OnboardingController` receives request
4. `ComplianceStatusUseCaseImpl` extracts agent ID from JWT, checks compliance rules
5. Returns `{ "status": "LOCKED" | "UNLOCKED", "reason": "..." }`

---

## 5. Component: OpenAPI Spec Fixes

### 5.1 Changes to `docs/api/openapi.yaml`

| Change | Detail |
|--------|--------|
| Add securitySchemes | `components.securitySchemes.bearerAuth`: type `http`, scheme `bearer`, bearerFormat `JWT` |
| Fix monetary types | All `type: number` for monetary fields → `type: string` |
| Add error responses | Every endpoint gets `400` and `401` with `ErrorResponse` schema |
| Fix content types | Replace all `'*/*'` with `application/json` |
| Add enums | `proxyType` enum in proxy enquiry, `fundingSource` enum in quote request |
| Add security | `security: [{ bearerAuth: [] }]` to all `/api/v1/*` endpoints |

---

## 6. Error Handling

All endpoints use the existing `GlobalExceptionHandler` and `ErrorResponse.of()` from common module.

### New Error Codes

| Error Code | Category | Action Code | Description |
|------------|----------|-------------|-------------|
| `ERR_BIZ_QUOTE_CALCULATION_FAILED` | Business | RETRY | Fee calculation service unavailable or failed |
| `ERR_BIZ_PROXY_NOT_FOUND` | Business | DECLINE | Proxy ID not found in DuitNow network |
| `ERR_EXT_PROXY_ENQUIRY_FAILED` | External | RETRY | Downstream switch error during proxy enquiry |
| `ERR_BIZ_COMPLIANCE_CHECK_FAILED` | Business | RETRY | Compliance status check failed |

---

## 7. Gateway Route Configuration

### 7.1 Transaction Quote
```yaml
# External route
- id: external-transactions-quote
  uri: http://rules-service:8081
  predicates:
    - Path=/api/v1/transactions/quote
  filters:
    - JwtAuth
    - RewritePath=/api/v1/transactions/quote, /internal/transactions/quote

# Update internal route predicate to include new path
- id: rules-service
  uri: http://rules-service:8081
  predicates:
    - Path=/internal/fees/**, /internal/check-velocity, /internal/limits/**, /internal/transactions/quote
```

### 7.2 Proxy Enquiry
```yaml
# External route
- id: external-proxy-enquiry
  uri: http://biller-service:8085
  predicates:
    - Path=/api/v1/transfer/proxy/enquiry
  filters:
    - JwtAuth
    - RewritePath=/api/v1/transfer/proxy/enquiry, /internal/transfer/proxy/enquiry

# Update internal route predicate
- id: biller-service
  uri: http://biller-service:8085
  predicates:
    - Path=/internal/billers/**, /internal/transfer/proxy/enquiry
```

### 7.3 Compliance Status
```yaml
# External route
- id: external-compliance-status
  uri: http://onboarding-service:8082
  predicates:
    - Path=/api/v1/compliance/status
  filters:
    - JwtAuth
    - RewritePath=/api/v1/compliance/status, /internal/compliance/status

# Update internal route predicate
- id: onboarding-service
  uri: http://onboarding-service:8082
  predicates:
    - Path=/internal/onboarding/**, /internal/kyc/**, /internal/compliance/status
```

---

## 8. Testing Strategy

### 8.1 Unit Tests
- `TransactionQuoteUseCaseImplTest` — in rules-service, mock fee calculation, verify quote response
- `ProxyEnquiryUseCaseImplTest` — in biller-service, mock switch adapter, verify name resolution
- `ComplianceStatusUseCaseImplTest` — in onboarding-service, mock compliance data, verify lock/unlock

### 8.2 Controller Tests
- `RulesControllerTest` — verify request/response mapping for transaction quote
- `BillerControllerTest` — verify request/response mapping for proxy enquiry
- `OnboardingControllerTest` — verify compliance status endpoint

### 8.3 Architecture Tests
- ArchUnit tests to verify hexagonal architecture compliance (no Spring imports in domain layer)

### 8.4 OpenAPI Spec Validation
- Test that no monetary fields use `type: number`
- Test that all `/api/v1/*` endpoints have security requirements
- Test that no endpoint uses `'*/*'` content type
