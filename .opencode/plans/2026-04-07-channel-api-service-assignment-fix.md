# Channel API Alignment - Service Assignment Fix Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix incorrect service assignments in BRD, Design, BDD, and Implementation Plan documents where FR-001.2, FR-002.2, and FR-003.2 point to wrong services, causing cascading errors throughout all downstream documents.

**Architecture:** Documentation fix only - no code changes. All changes are to markdown spec documents.

**Tech Stack:** Markdown documentation

**Root Cause:**
- FR-001.2 (Transaction Quote) wrongly assigned to `switch-adapter-service` → should be `rules-service`
- FR-002.2 (Proxy Enquiry) wrongly assigned to `switch-adapter-service` → should be `biller-service`
- FR-003.2 (Compliance Status) wrongly assigned to `rules-service` → should be `onboarding-service`

**Affected Documents:**
1. BRD: `docs/superpowers/specs/channel-api-alignment/2026-04-04-channel-api-alignment-brd.md`
2. Design: `docs/superpowers/specs/channel-api-alignment/2026-04-04-channel-api-alignment-design.md`
3. BDD: `docs/superpowers/specs/channel-api-alignment/2026-04-04-channel-api-alignment-bdd.md`
4. Plan: `docs/superpowers/plans/2026-04-04-channel-api-alignment.md`

---

## Task 1: Fix BRD Document

**Files:**
- Modify: `docs/superpowers/specs/channel-api-alignment/2026-04-04-channel-api-alignment-brd.md`

- [ ] **Step 1: Fix Module list (line 6)**

Change:
```markdown
**Module:** switch-adapter-service, rules-service, docs/api/openapi.yaml
```
To:
```markdown
**Module:** rules-service, biller-service, onboarding-service, docs/api/openapi.yaml
```

- [ ] **Step 2: Fix FR-001.2 (line 61)**

Change:
```markdown
- **FR-001.2** Service: switch-adapter-service (internal path: `/internal/transactions/quote`)
```
To:
```markdown
- **FR-001.2** Service: rules-service (internal path: `/internal/transactions/quote`)
```

- [ ] **Step 3: Fix FR-002.2 (line 72)**

Change:
```markdown
- **FR-002.2** Service: switch-adapter-service (internal path: `/internal/transfer/proxy/enquiry`)
```
To:
```markdown
- **FR-002.2** Service: biller-service (internal path: `/internal/transfer/proxy/enquiry`)
```

- [ ] **Step 4: Fix FR-003.2 (line 82)**

Change:
```markdown
- **FR-003.2** Service: rules-service (internal path: `/internal/compliance/status`)
```
To:
```markdown
- **FR-003.2** Service: onboarding-service (internal path: `/internal/compliance/status`)
```

- [ ] **Step 5: Review FR-001.5 for consistency**

FR-001.5 already mentions "rules-service fee engine" which is now consistent since FR-001.2 points to rules-service. No change needed.

- [ ] **Step 6: Commit**

```bash
git add docs/superpowers/specs/channel-api-alignment/2026-04-04-channel-api-alignment-brd.md
git commit -m "fix: correct service assignments in BRD - quote→rules, proxy→biller, compliance→onboarding"
```

---

## Task 2: Fix Design Document

**Files:**
- Modify: `docs/superpowers/specs/channel-api-alignment/2026-04-04-channel-api-alignment-design.md`

- [ ] **Step 1: Fix Module list (line 6)**

Change:
```markdown
**Module:** switch-adapter-service, rules-service, gateway, docs/api/openapi.yaml
```
To:
```markdown
**Module:** rules-service, biller-service, onboarding-service, gateway, docs/api/openapi.yaml
```

- [ ] **Step 2: Fix Architecture Overview (line 17)**

Change:
```markdown
Channel App → Gateway (JwtAuth filter) → switch-adapter-service / rules-service
```
To:
```markdown
Channel App → Gateway (JwtAuth filter) → rules-service / biller-service / onboarding-service
```

- [ ] **Step 3: Fix Section 2 - Transaction Quote Component (lines 22-53)**

Change section header and all references:
```markdown
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
```

- [ ] **Step 4: Fix Section 3 - Proxy Enquiry Component (lines 56-83)**

Change section header and all references:
```markdown
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
```

- [ ] **Step 5: Fix Section 4 - Compliance Status Component (lines 86-113)**

Change section header and all references:
```markdown
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
```

- [ ] **Step 6: Fix Section 7 - Gateway Routes (lines 146-194)**

Change all route configurations:
```markdown
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
```

- [ ] **Step 7: Fix Section 8 - Testing Strategy (lines 198-215)**

Change:
```markdown
### 8.1 Unit Tests
- `TransactionQuoteUseCaseImplTest` — in rules-service, mock fee calculation, verify quote response
- `ProxyEnquiryUseCaseImplTest` — in biller-service, mock switch adapter, verify name resolution
- `ComplianceStatusUseCaseImplTest` — in onboarding-service, mock compliance data, verify lock/unlock

### 8.2 Controller Tests
- `RulesControllerTest` — verify request/response mapping for transaction quote
- `BillerControllerTest` — verify request/response mapping for proxy enquiry
- `OnboardingControllerTest` — verify compliance status endpoint
```

- [ ] **Step 8: Commit**

```bash
git add docs/superpowers/specs/channel-api-alignment/2026-04-04-channel-api-alignment-design.md
git commit -m "fix: correct service assignments in design doc - quote→rules, proxy→biller, compliance→onboarding"
```

---

## Task 3: Fix BDD Document

**Files:**
- Modify: `docs/superpowers/specs/channel-api-alignment/2026-04-04-channel-api-alignment-bdd.md`

- [ ] **Step 1: Fix Module list (line 6)**

Change:
```markdown
**Module:** switch-adapter-service, rules-service
```
To:
```markdown
**Module:** rules-service, biller-service, onboarding-service
```

- [ ] **Step 2: Commit**

```bash
git add docs/superpowers/specs/channel-api-alignment/2026-04-04-channel-api-alignment-bdd.md
git commit -m "fix: correct module list in BDD document"
```

---

## Task 4: Fix Implementation Plan Document

**Files:**
- Modify: `docs/superpowers/plans/2026-04-04-channel-api-alignment.md`

- [ ] **Step 1: Fix Architecture description (line 7)**

Change:
```markdown
**Architecture:** Hexagonal (Ports & Adapters) pattern in switch-adapter-service and rules-service. External `/api/v1/*` routes through Spring Cloud Gateway with JwtAuth filter, rewritten to internal `/internal/*` paths. Cross-service calls use Feign clients (not direct domain service imports).
```
To:
```markdown
**Architecture:** Hexagonal (Ports & Adapters) pattern in rules-service, biller-service, and onboarding-service. External `/api/v1/*` routes through Spring Cloud Gateway with JwtAuth filter, rewritten to internal `/internal/*` paths. Cross-service calls use Feign clients (not direct domain service imports).
```

- [ ] **Step 2: Fix File Structure - New Files table (lines 22-38)**

Change all service paths:
```markdown
| File | Purpose |
|------|---------|
| `services/rules-service/.../web/dto/TransactionQuoteRequest.java` | Request DTO for quote endpoint |
| `services/rules-service/.../web/dto/TransactionQuoteResponse.java` | Response DTO for quote endpoint |
| `services/rules-service/.../domain/port/in/TransactionQuoteUseCase.java` | Inbound port for quote |
| `services/biller-service/.../domain/port/in/ProxyEnquiryUseCase.java` | Inbound port for proxy enquiry |
| `services/biller-service/.../domain/port/out/DuitNowProxyGateway.java` | Outbound port for DuitNow proxy resolution |
| `services/biller-service/.../infrastructure/external/DuitNowProxyClient.java` | Feign client for DuitNow proxy (or stub) |
| `services/rules-service/.../application/usecase/TransactionQuoteUseCaseImpl.java` | Quote use case implementation |
| `services/biller-service/.../application/usecase/ProxyEnquiryUseCaseImpl.java` | Proxy enquiry use case implementation |
| `services/onboarding-service/.../domain/port/in/ComplianceStatusUseCase.java` | Inbound port for compliance status |
| `services/onboarding-service/.../application/usecase/ComplianceStatusUseCaseImpl.java` | Compliance status use case implementation |
| `services/rules-service/.../usecase/TransactionQuoteUseCaseTest.java` | Unit test for quote use case |
| `services/biller-service/.../usecase/ProxyEnquiryUseCaseTest.java` | Unit test for proxy enquiry use case |
| `services/onboarding-service/.../usecase/ComplianceStatusUseCaseTest.java` | Unit test for compliance status use case |
```

- [ ] **Step 3: Fix File Structure - Modified Files table (lines 40-51)**

Change:
```markdown
| File | Change |
|------|--------|
| `services/rules-service/.../config/DomainServiceConfig.java` | Register quote use case bean |
| `services/rules-service/.../web/RulesController.java` | Add transaction quote endpoint |
| `services/biller-service/.../config/DomainServiceConfig.java` | Register proxy enquiry use case bean |
| `services/biller-service/.../web/BillerController.java` | Add proxy enquiry endpoint |
| `services/onboarding-service/.../config/DomainServiceConfig.java` | Register compliance use case bean |
| `services/onboarding-service/.../web/OnboardingController.java` | Add compliance status endpoint |
| `common/.../security/ErrorCodes.java` | Add 4 new error codes |
| `gateway/src/main/resources/application.yaml` | Add 3 external + update internal route predicates |
| `docs/api/openapi.yaml` | Fix all quality issues (types, security, errors, content types) |
```

- [ ] **Step 4: Fix Task 2 - Transaction Quote (lines 86-215)**

Change ALL file paths from `services/switch-adapter-service/` to `services/rules-service/`:
- Line 95: `services/rules-service/src/main/java/com/agentbanking/rules/domain/port/in/TransactionQuoteUseCase.java`
- Line 96: `services/rules-service/src/main/java/com/agentbanking/rules/domain/port/out/FeeCalculationGateway.java`
- Line 97: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/external/FeeCalculationClient.java`
- All package references: `com.agentbanking.rules.*` instead of `com.agentbanking.switchadapter.*`
- Line 210-213: git add paths to rules-service
- Line 214: commit message stays the same

- [ ] **Step 5: Fix Task 3 - Transaction Quote Use Case (lines 219-379)**

Change ALL file paths from `services/switch-adapter-service/` to `services/rules-service/`:
- Line 226: `services/rules-service/src/main/java/com/agentbanking/rules/application/usecase/TransactionQuoteUseCaseImpl.java`
- Line 227: `services/rules-service/src/test/java/com/agentbanking/rules/application/usecase/TransactionQuoteUseCaseTest.java`
- All package references: `com.agentbanking.rules.*` instead of `com.agentbanking.switchadapter.*`
- Line 376-377: git add paths to rules-service

- [ ] **Step 6: Fix Task 4 - Transaction Quote DTOs & Controller (lines 383-500)**

Change ALL file paths and references:
- Line 390: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/web/dto/TransactionQuoteRequest.java`
- Line 391: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/web/dto/TransactionQuoteResponse.java`
- Line 392: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/web/RulesController.java`
- All package references: `com.agentbanking.rules.*` instead of `com.agentbanking.switchadapter.*`
- Controller class: `RulesController` instead of `SwitchController`
- Line 496-498: git add paths to rules-service

- [ ] **Step 7: Fix Task 5 - Proxy Enquiry Ports (lines 504-584)**

Change ALL file paths from `services/switch-adapter-service/` to `services/biller-service/`:
- Line 511: `services/biller-service/src/main/java/com/agentbanking/biller/domain/port/in/ProxyEnquiryUseCase.java`
- Line 512: `services/biller-service/src/main/java/com/agentbanking/biller/domain/port/out/DuitNowProxyGateway.java`
- Line 513: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/external/DuitNowProxyClient.java`
- All package references: `com.agentbanking.biller.*` instead of `com.agentbanking.switchadapter.*`
- Line 580-582: git add paths to biller-service

- [ ] **Step 8: Fix Task 6 - Proxy Enquiry Use Case (lines 588-727)**

Change ALL file paths from `services/switch-adapter-service/` to `services/biller-service/`:
- Line 595: `services/biller-service/src/main/java/com/agentbanking/biller/application/usecase/ProxyEnquiryUseCaseImpl.java`
- Line 596: `services/biller-service/src/test/java/com/agentbanking/biller/application/usecase/ProxyEnquiryUseCaseTest.java`
- All package references: `com.agentbanking.biller.*` instead of `com.agentbanking.switchadapter.*`
- Line 724-725: git add paths to biller-service

- [ ] **Step 9: Fix Task 7 - Proxy Enquiry Controller (lines 731-778)**

Change ALL file paths and references:
- Line 738: `services/biller-service/src/main/java/com/agentbanking/biller/infrastructure/web/BillerController.java`
- All package references: `com.agentbanking.biller.*` instead of `com.agentbanking.switchadapter.*`
- Controller class: `BillerController` instead of `SwitchController`
- Line 776: git add path to biller-service

- [ ] **Step 10: Fix Task 8 - Compliance Status (lines 782-936)**

Change ALL file paths from `services/rules-service/` to `services/onboarding-service/`:
- Line 789: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/port/in/ComplianceStatusUseCase.java`
- Line 790: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/application/usecase/ComplianceStatusUseCaseImpl.java`
- Line 791: `services/onboarding-service/src/test/java/com/agentbanking/onboarding/application/usecase/ComplianceStatusUseCaseTest.java`
- All package references: `com.agentbanking.onboarding.*` instead of `com.agentbanking.rules.*`
- Line 932-934: git add paths to onboarding-service

- [ ] **Step 11: Fix Task 9 - Compliance Status Controller (lines 940-984)**

Change ALL file paths and references:
- Line 947: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/web/OnboardingController.java`
- All package references: `com.agentbanking.onboarding.*` instead of `com.agentbanking.rules.*`
- Controller class: `OnboardingController` instead of `RulesController`
- Line 982: git add path to onboarding-service

- [ ] **Step 12: Fix Task 10 - Bean Registration (lines 988-1088)**

Change ALL file paths and package references:
- Line 995: `services/rules-service/src/main/java/com/agentbanking/rules/config/DomainServiceConfig.java`
- Line 996: `services/biller-service/src/main/java/com/agentbanking/biller/config/DomainServiceConfig.java`
- Line 997: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/config/DomainServiceConfig.java`
- Update all package imports in the code examples
- Remove switch-adapter-service bean registration, add biller-service and onboarding-service

- [ ] **Step 13: Fix Task 11 - Gateway Routes (lines 1092-1161)**

Change ALL service URIs:
- Lines 1108, 1117: `http://rules-service:8081` for transactions/quote (was switch-adapter-service:8084)
- Lines 1118, 1170: `http://biller-service:8085` for proxy/enquiry (was switch-adapter-service:8084)
- Lines 1126, 1181: `http://onboarding-service:8082` for compliance/status (was rules-service:8081)
- Update internal route predicates accordingly

- [ ] **Step 14: Fix Task 13 - Test Commands (lines 1313-1333)**

Change test execution paths:
- Line 1316: `cd services/rules-service && ./gradlew test` (was switch-adapter-service)
- Line 1323: `cd services/biller-service && ./gradlew test` (was rules-service)
- Line 1330: `cd services/rules-service && ./gradlew test --tests "*HexagonalArchitectureTest*"` (transaction quote tests)
- Add: `cd services/biller-service && ./gradlew test --tests "*HexagonalArchitectureTest*"` (proxy enquiry tests)
- Add: `cd services/onboarding-service && ./gradlew test --tests "*HexagonalArchitectureTest*"` (compliance tests)

- [ ] **Step 15: Fix Task Ordering diagram (lines 1351-1361)**

Change:
```markdown
## Task Ordering & Dependencies

```
Task 1 (Error Codes) → Task 2 (Quote Ports/Feign) → Task 3 (Quote Use Case) → Task 4 (Quote Controller)
                    → Task 5 (Proxy Ports/Feign) → Task 6 (Proxy Use Case) → Task 7 (Proxy Controller)
                    → Task 8 (Compliance Use Case) → Task 9 (Compliance Controller)
Tasks 1-9 → Task 10 (Bean Registration)
Task 10 → Task 11 (Gateway Routes)
Tasks 1-11 → Task 12 (OpenAPI Fixes)
Tasks 1-12 → Task 13 (Verify All)
```

Tasks 2, 5, 8 can be done in parallel (independent ports/use cases in different services). Tasks 3, 6 can be done in parallel. Tasks 4, 7, 9 can be done in parallel.
```

- [ ] **Step 16: Commit**

```bash
git add docs/superpowers/plans/2026-04-04-channel-api-alignment.md
git commit -m "fix: correct all service paths in implementation plan - quote→rules, proxy→biller, compliance→onboarding"
```

---

## Verification Checklist

After all changes, verify:

- [ ] BRD Module line lists: `rules-service, biller-service, onboarding-service`
- [ ] BRD FR-001.2: `rules-service`
- [ ] BRD FR-002.2: `biller-service`
- [ ] BRD FR-003.2: `onboarding-service`
- [ ] Design Module line lists: `rules-service, biller-service, onboarding-service`
- [ ] Design §2: Transaction Quote in `rules-service`
- [ ] Design §3: Proxy Enquiry in `biller-service`
- [ ] Design §4: Compliance Status in `onboarding-service`
- [ ] Design §7: Gateway routes point to correct services
- [ ] BDD Module line lists: `rules-service, biller-service, onboarding-service`
- [ ] Plan all file paths reference correct services
- [ ] Plan all package names match service conventions
- [ ] Plan all gateway URIs are correct
- [ ] Plan test commands reference correct service directories

---

## Service Port Reference

| Service | Port | Package Base |
|---------|------|--------------|
| rules-service | 8081 | `com.agentbanking.rules` |
| onboarding-service | 8082 | `com.agentbanking.onboarding` |
| biller-service | 8085 | `com.agentbanking.biller` |
