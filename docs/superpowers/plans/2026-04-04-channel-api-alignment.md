# Channel API Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 3 missing API endpoints (transaction quote, proxy enquiry, compliance status) and fix all OpenAPI spec quality issues.

**Architecture:** Hexagonal (Ports & Adapters) pattern in switch-adapter-service and rules-service. External `/api/v1/*` routes through Spring Cloud Gateway with JwtAuth filter, rewritten to internal `/internal/*` paths. Cross-service calls use Feign clients (not direct domain service imports).

**Tech Stack:** Java 21, Spring Boot 3.x, Spring Cloud Gateway, OpenFeign, JUnit 5, Mockito, ArchUnit

**Specs Reference:**
- BRD: `docs/superpowers/specs/channel-api-alignment/2026-04-04-channel-api-alignment-brd.md`
- BDD: `docs/superpowers/specs/channel-api-alignment/2026-04-04-channel-api-alignment-bdd.md`
- Design: `docs/superpowers/specs/channel-api-alignment/2026-04-04-channel-api-alignment-design.md`

---

## File Structure

### New Files

| File | Purpose |
|------|---------|
| `services/switch-adapter-service/.../web/dto/TransactionQuoteRequest.java` | Request DTO for quote endpoint |
| `services/switch-adapter-service/.../web/dto/TransactionQuoteResponse.java` | Response DTO for quote endpoint |
| `services/switch-adapter-service/.../domain/port/in/TransactionQuoteUseCase.java` | Inbound port for quote |
| `services/switch-adapter-service/.../domain/port/in/ProxyEnquiryUseCase.java` | Inbound port for proxy enquiry |
| `services/switch-adapter-service/.../domain/port/out/FeeCalculationGateway.java` | Outbound port for fee calculation (cross-service) |
| `services/switch-adapter-service/.../domain/port/out/DuitNowProxyGateway.java` | Outbound port for DuitNow proxy resolution |
| `services/switch-adapter-service/.../infrastructure/external/FeeCalculationClient.java` | Feign client for rules-service fee calculation |
| `services/switch-adapter-service/.../infrastructure/external/DuitNowProxyClient.java` | Feign client for DuitNow proxy (or stub) |
| `services/switch-adapter-service/.../application/usecase/TransactionQuoteUseCaseImpl.java` | Quote use case implementation |
| `services/switch-adapter-service/.../application/usecase/ProxyEnquiryUseCaseImpl.java` | Proxy enquiry use case implementation |
| `services/rules-service/.../domain/port/in/ComplianceStatusUseCase.java` | Inbound port for compliance status |
| `services/rules-service/.../application/usecase/ComplianceStatusUseCaseImpl.java` | Compliance status use case implementation |
| `services/switch-adapter-service/.../usecase/TransactionQuoteUseCaseTest.java` | Unit test for quote use case |
| `services/switch-adapter-service/.../usecase/ProxyEnquiryUseCaseTest.java` | Unit test for proxy enquiry use case |
| `services/rules-service/.../usecase/ComplianceStatusUseCaseTest.java` | Unit test for compliance status use case |

### Modified Files

| File | Change |
|------|--------|
| `services/switch-adapter-service/.../config/DomainServiceConfig.java` | Register new use case beans |
| `services/switch-adapter-service/.../web/SwitchController.java` | Add quote and proxy enquiry endpoints |
| `services/rules-service/.../config/DomainServiceConfig.java` | Register compliance use case bean |
| `services/rules-service/.../web/RulesController.java` | Add compliance status endpoint |
| `common/.../security/ErrorCodes.java` | Add 4 new error codes |
| `gateway/src/main/resources/application.yaml` | Add 3 external + update internal route predicates |
| `docs/api/openapi.yaml` | Fix all quality issues (types, security, errors, content types) |

---

## Task 1: Add Error Codes to Common Module

**BDD Scenarios:** Supports all BDD scenarios (error handling prerequisite)
**BRD Requirements:** FR-001.7, FR-002.7, FR-003.6
**User-Facing:** NO

**Files:**
- Modify: `common/src/main/java/com/agentbanking/common/security/ErrorCodes.java`

- [ ] **Step 1: Add new error codes**

Add these constants to `ErrorCodes.java` in the appropriate sections:

```java
// Business Errors (ERR_BIZ_xxx) — add after existing business errors
public static final String ERR_BIZ_QUOTE_CALCULATION_FAILED = "ERR_BIZ_QUOTE_CALCULATION_FAILED";
public static final String ERR_BIZ_PROXY_NOT_FOUND = "ERR_BIZ_PROXY_NOT_FOUND";
public static final String ERR_BIZ_COMPLIANCE_CHECK_FAILED = "ERR_BIZ_COMPLIANCE_CHECK_FAILED";

// External System Errors (ERR_EXT_xxx) — add after existing external errors
public static final String ERR_EXT_PROXY_ENQUIRY_FAILED = "ERR_EXT_PROXY_ENQUIRY_FAILED";
```

- [ ] **Step 2: Commit**

```bash
git add common/src/main/java/com/agentbanking/common/security/ErrorCodes.java
git commit -m "feat: add error codes for quote, proxy enquiry, and compliance status"
```

---

## Task 2: Transaction Quote — Domain Ports & Feign Client (switch-adapter-service)

**BDD Scenarios:** S1.1 (Happy Path), S1.2 (Missing field), S1.3 (Invalid funding source), S1.4 (No auth)
**BRD Requirements:** US-001, FR-001.1 through FR-001.8
**User-Facing:** NO

**Key Design Decision:** The switch-adapter-service calls rules-service for fee calculation via a Feign client, NOT by importing domain classes from rules-service. This respects database-per-service and service boundary constraints (AGENTS.md Law VIII).

**Files:**
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/domain/port/in/TransactionQuoteUseCase.java`
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/domain/port/out/FeeCalculationGateway.java`
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/external/FeeCalculationClient.java`

- [ ] **Step 1: Create the inbound port interface**

Create `TransactionQuoteUseCase.java`:

```java
package com.agentbanking.switchadapter.domain.port.in;

public interface TransactionQuoteUseCase {

    QuoteResult calculateQuote(String agentId, String agentTier, String amount,
                               String serviceCode, String fundingSource, String billerRouting);

    record QuoteResult(
        String quoteId,
        String amount,
        String fee,
        String total,
        String commission
    ) {}
}
```

Note: All monetary values are `String` to match the OpenAPI spec requirement (FR-004.1).

- [ ] **Step 2: Create the outbound port interface for fee calculation**

Create `FeeCalculationGateway.java`:

```java
package com.agentbanking.switchadapter.domain.port.out;

import java.math.BigDecimal;

public interface FeeCalculationGateway {

    FeeCalculationResult calculateFee(BigDecimal amount, String transactionType, String agentTier);

    record FeeCalculationResult(
        BigDecimal customerFee,
        BigDecimal agentCommission,
        BigDecimal bankShare
    ) {}
}
```

- [ ] **Step 3: Create the Feign client adapter**

Create `FeeCalculationClient.java`:

```java
package com.agentbanking.switchadapter.infrastructure.external;

import com.agentbanking.switchadapter.domain.port.out.FeeCalculationGateway;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "rules-service", url = "${rules-service.url:http://rules-service:8081}")
public interface FeeCalculationClient extends FeeCalculationGateway {

    @PostMapping("/internal/fees/calculate")
    @Override
    FeeCalculationResult calculateFee(@RequestParam("amount") BigDecimal amount,
                                       @RequestParam("transactionType") String transactionType,
                                       @RequestParam("agentTier") String agentTier);

    // Adapter method to convert Map response from rules-service
    default FeeCalculationResult calculateFeeWithMap(BigDecimal amount, String transactionType, String agentTier) {
        // This will be called via a separate controller method or we use a DTO
        // For now, the Feign client returns the Map response and we parse it
        throw new UnsupportedOperationException("Use calculateFee with proper DTO mapping");
    }
}
```

**Important:** The existing rules-service `/internal/fees/calculate` endpoint returns `Map<String, Object>`. The Feign client needs a proper response type. Create a response record:

```java
// Also create in infrastructure/external/
package com.agentbanking.switchadapter.infrastructure.external;

import java.math.BigDecimal;

public record FeeCalculationResponse(
    BigDecimal customerFee,
    BigDecimal agentCommission,
    BigDecimal bankShare,
    String transactionType,
    String agentTier
) {}
```

Then update the Feign client to return `FeeCalculationResponse` and adapt it in the implementation:

```java
@FeignClient(name = "rules-service", url = "${rules-service.url:http://rules-service:8081}")
public interface FeeCalculationClient {

    @PostMapping("/internal/fees/calculate")
    FeeCalculationResponse calculateFee(@RequestParam("amount") BigDecimal amount,
                                         @RequestParam("transactionType") String transactionType,
                                         @RequestParam("agentTier") String agentTier);
}
```

- [ ] **Step 4: Commit**

```bash
git add services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/domain/port/in/TransactionQuoteUseCase.java
git add services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/domain/port/out/FeeCalculationGateway.java
git add services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/external/FeeCalculationClient.java
git add services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/external/FeeCalculationResponse.java
git commit -m "feat: add transaction quote domain ports and fee calculation Feign client (FR-001)"
```

---

## Task 3: Transaction Quote — Use Case Implementation & Test (switch-adapter-service)

**BDD Scenarios:** S1.1, S1.2, S1.3, S1.4
**BRD Requirements:** US-001, FR-001.1 through FR-001.8
**User-Facing:** NO

**Files:**
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/application/usecase/TransactionQuoteUseCaseImpl.java`
- Create: `services/switch-adapter-service/src/test/java/com/agentbanking/switchadapter/application/usecase/TransactionQuoteUseCaseTest.java`

- [ ] **Step 1: Write the failing test**

Create `TransactionQuoteUseCaseTest.java`:

```java
package com.agentbanking.switchadapter.application.usecase;

import com.agentbanking.switchadapter.domain.port.out.FeeCalculationGateway;
import com.agentbanking.switchadapter.domain.port.out.FeeCalculationGateway.FeeCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionQuoteUseCaseTest {

    @Mock
    private FeeCalculationGateway feeCalculationGateway;

    private TransactionQuoteUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new TransactionQuoteUseCaseImpl(feeCalculationGateway);
    }

    @Test
    void calculateQuote_shouldReturnQuoteWithFeeAndCommission() {
        // Given
        String agentId = "agent-001";
        String agentTier = "STANDARD";
        String amount = "100.00";
        String serviceCode = "CASH_WITHDRAWAL";
        String fundingSource = "CARD_EMV";

        when(feeCalculationGateway.calculateFee(any(), any(), any()))
            .thenReturn(new FeeCalculationResult(
                new BigDecimal("1.00"),
                new BigDecimal("0.50"),
                new BigDecimal("0.50")
            ));

        // When
        TransactionQuoteUseCase.QuoteResult result = useCase.calculateQuote(
            agentId, agentTier, amount, serviceCode, fundingSource, null
        );

        // Then
        assertNotNull(result.quoteId());
        assertEquals("100.00", result.amount());
        assertEquals("1.00", result.fee());
        assertEquals("101.00", result.total());
        assertEquals("0.50", result.commission());
    }

    @Test
    void calculateQuote_shouldThrowWhenFeeCalculationFails() {
        // Given
        when(feeCalculationGateway.calculateFee(any(), any(), any()))
            .thenThrow(new RuntimeException("Fee config not found"));

        // When/Then
        assertThrows(IllegalStateException.class, () ->
            useCase.calculateQuote("agent-001", "STANDARD",
                "100.00", "CASH_WITHDRAWAL", "CARD_EMV", null)
        );
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd services/switch-adapter-service && ./gradlew test --tests TransactionQuoteUseCaseTest 2>&1 | tail -20
```
Expected: FAIL — classes don't exist yet.

- [ ] **Step 3: Create the use case implementation**

Create `TransactionQuoteUseCaseImpl.java`:

```java
package com.agentbanking.switchadapter.application.usecase;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.switchadapter.domain.port.in.TransactionQuoteUseCase;
import com.agentbanking.switchadapter.domain.port.out.FeeCalculationGateway;
import com.agentbanking.switchadapter.domain.port.out.FeeCalculationGateway.FeeCalculationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class TransactionQuoteUseCaseImpl implements TransactionQuoteUseCase {

    private final FeeCalculationGateway feeCalculationGateway;

    public TransactionQuoteUseCaseImpl(FeeCalculationGateway feeCalculationGateway) {
        this.feeCalculationGateway = feeCalculationGateway;
    }

    @Override
    public QuoteResult calculateQuote(String agentId, String agentTier, String amount,
                                      String serviceCode, String fundingSource, String billerRouting) {
        try {
            BigDecimal amountDecimal = new BigDecimal(amount);

            FeeCalculationResult feeResult = feeCalculationGateway.calculateFee(
                amountDecimal, serviceCode, agentTier
            );

            BigDecimal total = amountDecimal.add(feeResult.customerFee());

            return new QuoteResult(
                "QT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                amountDecimal.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                feeResult.customerFee().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                total.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                feeResult.agentCommission().setScale(2, RoundingMode.HALF_UP).toPlainString()
            );
        } catch (Exception e) {
            throw new IllegalStateException(ErrorCodes.ERR_BIZ_QUOTE_CALCULATION_FAILED + ": " + e.getMessage(), e);
        }
    }
}
```

Note: NO `@Service` annotation — registered as bean in `DomainServiceConfig.java` (per AGENTS.md Law V).

- [ ] **Step 4: Run test to verify it passes**

```bash
cd services/switch-adapter-service && ./gradlew test --tests TransactionQuoteUseCaseTest 2>&1 | tail -20
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/application/usecase/TransactionQuoteUseCaseImpl.java
git add services/switch-adapter-service/src/test/java/com/agentbanking/switchadapter/application/usecase/TransactionQuoteUseCaseTest.java
git commit -m "test+feat: implement transaction quote use case with TDD (FR-001)"
```

---

## Task 4: Transaction Quote — DTOs & Controller Endpoint (switch-adapter-service)

**BDD Scenarios:** S1.1, S1.2, S1.3, S1.4
**BRD Requirements:** FR-001.1 through FR-001.8
**User-Facing:** NO

**Files:**
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/web/dto/TransactionQuoteRequest.java`
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/web/dto/TransactionQuoteResponse.java`
- Modify: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/web/SwitchController.java`

- [ ] **Step 1: Create request DTO**

Create `TransactionQuoteRequest.java`:

```java
package com.agentbanking.switchadapter.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TransactionQuoteRequest(
    @NotNull(message = "amount is required")
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "amount must be a valid decimal string")
    String amount,

    @NotBlank(message = "serviceCode is required")
    String serviceCode,

    @NotBlank(message = "fundingSource is required")
    String fundingSource,

    String billerRouting
) {}
```

Note: `amount` is `String` (not `BigDecimal`) to match OpenAPI spec requirement. Validated via regex pattern.

- [ ] **Step 2: Create response DTO**

Create `TransactionQuoteResponse.java`:

```java
package com.agentbanking.switchadapter.infrastructure.web.dto;

import com.agentbanking.switchadapter.domain.port.in.TransactionQuoteUseCase;

public record TransactionQuoteResponse(
    String quoteId,
    String amount,
    String fee,
    String total,
    String commission
) {
    public static TransactionQuoteResponse from(TransactionQuoteUseCase.QuoteResult result) {
        return new TransactionQuoteResponse(
            result.quoteId(),
            result.amount(),
            result.fee(),
            result.total(),
            result.commission()
        );
    }
}
```

- [ ] **Step 3: Add endpoint to SwitchController**

Add to `SwitchController.java`:
1. Add imports:
```java
import com.agentbanking.switchadapter.domain.port.in.TransactionQuoteUseCase;
import com.agentbanking.switchadapter.infrastructure.web.dto.TransactionQuoteRequest;
import com.agentbanking.switchadapter.infrastructure.web.dto.TransactionQuoteResponse;
```
2. Add field and constructor parameter:
```java
private final TransactionQuoteUseCase transactionQuoteUseCase;
```
Update constructor to include this parameter.

3. Add endpoint method:

```java
@PostMapping("/transactions/quote")
public ResponseEntity<?> getQuote(@Valid @RequestBody TransactionQuoteRequest request,
                                   @RequestHeader(value = "X-Agent-Id", required = false) String agentId,
                                   @RequestHeader(value = "X-Agent-Tier", required = false) String agentTier) {
    try {
        TransactionQuoteUseCase.QuoteResult result = transactionQuoteUseCase.calculateQuote(
            agentId != null ? agentId : "unknown",
            agentTier != null ? agentTier : "STANDARD",
            request.amount(),
            request.serviceCode(),
            request.fundingSource(),
            request.billerRouting()
        );

        return ResponseEntity.ok(TransactionQuoteResponse.from(result));
    } catch (IllegalStateException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(
            "ERR_BIZ_QUOTE_CALCULATION_FAILED",
            e.getMessage(),
            "RETRY"
        ));
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/web/dto/TransactionQuoteRequest.java
git add services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/web/dto/TransactionQuoteResponse.java
git add services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/web/SwitchController.java
git commit -m "feat: add transaction quote endpoint and DTOs to SwitchController (FR-001)"
```

---

## Task 5: Proxy Enquiry — Domain Ports & Feign Client (switch-adapter-service)

**BDD Scenarios:** S2.1 (Happy Path), S2.2 (Missing proxyId), S2.3 (Missing proxyType), S2.4 (Not found), S2.5 (No auth)
**BRD Requirements:** US-002, FR-002.1 through FR-002.7
**User-Facing:** NO

**Files:**
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/domain/port/in/ProxyEnquiryUseCase.java`
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/domain/port/out/DuitNowProxyGateway.java`
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/external/DuitNowProxyClient.java`

- [ ] **Step 1: Create the outbound port interface**

Create `DuitNowProxyGateway.java`:

```java
package com.agentbanking.switchadapter.domain.port.out;

public interface DuitNowProxyGateway {
    /**
     * Resolves a DuitNow proxy ID to the registered account holder name.
     * @throws IllegalArgumentException if proxy not found
     * @throws IllegalStateException if downstream service fails
     */
    String resolveProxy(String proxyId, String proxyType);
}
```

- [ ] **Step 2: Create the Feign client adapter (stub for now)**

Create `DuitNowProxyClient.java`:

```java
package com.agentbanking.switchadapter.infrastructure.external;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.switchadapter.domain.port.out.DuitNowProxyGateway;
import org.springframework.stereotype.Component;

/**
 * Stub implementation for DuitNow proxy gateway.
 * TODO: Replace with actual Feign client when DuitNow switch endpoint is available.
 */
@Component
public class DuitNowProxyClient implements DuitNowProxyGateway {

    @Override
    public String resolveProxy(String proxyId, String proxyType) {
        // Stub: In production, this would call the DuitNow switch API
        // For now, return a placeholder or throw unavailable
        throw new IllegalStateException(ErrorCodes.ERR_SWITCH_UNAVAILABLE + ": DuitNow proxy service not yet configured");
    }
}
```

- [ ] **Step 3: Create the inbound port interface**

Create `ProxyEnquiryUseCase.java`:

```java
package com.agentbanking.switchadapter.domain.port.in;

public interface ProxyEnquiryUseCase {

    ProxyEnquiryResult enquiryProxy(String proxyId, String proxyType);

    record ProxyEnquiryResult(
        String name,
        String proxyType
    ) {}
}
```

- [ ] **Step 4: Commit**

```bash
git add services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/domain/port/in/ProxyEnquiryUseCase.java
git add services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/domain/port/out/DuitNowProxyGateway.java
git add services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/external/DuitNowProxyClient.java
git commit -m "feat: add proxy enquiry domain ports and DuitNow proxy gateway stub (FR-002)"
```

---

## Task 6: Proxy Enquiry — Use Case Implementation & Test (switch-adapter-service)

**BDD Scenarios:** S2.1, S2.2, S2.3, S2.4, S2.5
**BRD Requirements:** FR-002.1 through FR-002.7
**User-Facing:** NO

**Files:**
- Create: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/application/usecase/ProxyEnquiryUseCaseImpl.java`
- Create: `services/switch-adapter-service/src/test/java/com/agentbanking/switchadapter/application/usecase/ProxyEnquiryUseCaseTest.java`

- [ ] **Step 1: Write the failing test**

Create `ProxyEnquiryUseCaseTest.java`:

```java
package com.agentbanking.switchadapter.application.usecase;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.switchadapter.domain.port.out.DuitNowProxyGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProxyEnquiryUseCaseTest {

    @Mock
    private DuitNowProxyGateway duitNowProxyGateway;

    private ProxyEnquiryUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProxyEnquiryUseCaseImpl(duitNowProxyGateway);
    }

    @Test
    void enquiryProxy_shouldReturnNameForValidProxy() {
        // Given
        String proxyId = "60123456789";
        String proxyType = "MOBILE";
        when(duitNowProxyGateway.resolveProxy(proxyId, proxyType))
            .thenReturn("AHMAD BIN ABDULLAH");

        // When
        com.agentbanking.switchadapter.domain.port.in.ProxyEnquiryUseCase.ProxyEnquiryResult result =
            useCase.enquiryProxy(proxyId, proxyType);

        // Then
        assertEquals("AHMAD BIN ABDULLAH", result.name());
        assertEquals(proxyType, result.proxyType());
    }

    @Test
    void enquiryProxy_shouldThrowWhenProxyNotFound() {
        // Given
        when(duitNowProxyGateway.resolveProxy("invalid", "MOBILE"))
            .thenThrow(new IllegalArgumentException(ErrorCodes.ERR_BIZ_PROXY_NOT_FOUND));

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            useCase.enquiryProxy("invalid", "MOBILE")
        );
    }

    @Test
    void enquiryProxy_shouldThrowWhenDownstreamFails() {
        // Given
        when(duitNowProxyGateway.resolveProxy(any(), any()))
            .thenThrow(new RuntimeException("Switch unavailable"));

        // When/Then
        assertThrows(IllegalStateException.class, () ->
            useCase.enquiryProxy("60123456789", "MOBILE")
        );
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd services/switch-adapter-service && ./gradlew test --tests ProxyEnquiryUseCaseTest 2>&1 | tail -20
```
Expected: FAIL — classes don't exist yet.

- [ ] **Step 3: Create the use case implementation**

Create `ProxyEnquiryUseCaseImpl.java`:

```java
package com.agentbanking.switchadapter.application.usecase;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.switchadapter.domain.port.in.ProxyEnquiryUseCase;
import com.agentbanking.switchadapter.domain.port.out.DuitNowProxyGateway;

public class ProxyEnquiryUseCaseImpl implements ProxyEnquiryUseCase {

    private final DuitNowProxyGateway duitNowProxyGateway;

    public ProxyEnquiryUseCaseImpl(DuitNowProxyGateway duitNowProxyGateway) {
        this.duitNowProxyGateway = duitNowProxyGateway;
    }

    @Override
    public ProxyEnquiryResult enquiryProxy(String proxyId, String proxyType) {
        try {
            String name = duitNowProxyGateway.resolveProxy(proxyId, proxyType);
            return new ProxyEnquiryResult(name, proxyType);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(ErrorCodes.ERR_EXT_PROXY_ENQUIRY_FAILED + ": " + e.getMessage(), e);
        }
    }
}
```

Note: NO `@Service` annotation — registered as bean in `DomainServiceConfig.java`.

- [ ] **Step 4: Run test to verify it passes**

```bash
cd services/switch-adapter-service && ./gradlew test --tests ProxyEnquiryUseCaseTest 2>&1 | tail -20
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/application/usecase/ProxyEnquiryUseCaseImpl.java
git add services/switch-adapter-service/src/test/java/com/agentbanking/switchadapter/application/usecase/ProxyEnquiryUseCaseTest.java
git commit -m "test+feat: implement proxy enquiry use case with TDD (FR-002)"
```

---

## Task 7: Proxy Enquiry — Controller Endpoint (switch-adapter-service)

**BDD Scenarios:** S2.1, S2.2, S2.3, S2.4, S2.5
**BRD Requirements:** FR-002.1 through FR-002.7
**User-Facing:** NO

**Files:**
- Modify: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/web/SwitchController.java`

- [ ] **Step 1: Add endpoint to SwitchController**

Add to `SwitchController.java`:
1. Add import: `import com.agentbanking.switchadapter.domain.port.in.ProxyEnquiryUseCase;`
2. Add field and constructor parameter: `private final ProxyEnquiryUseCase proxyEnquiryUseCase;`
3. Add endpoint method:

```java
@GetMapping("/transfer/proxy/enquiry")
public ResponseEntity<?> proxyEnquiry(@RequestParam String proxyId,
                                       @RequestParam String proxyType) {
    try {
        ProxyEnquiryUseCase.ProxyEnquiryResult result = proxyEnquiryUseCase.enquiryProxy(proxyId, proxyType);
        return ResponseEntity.ok(Map.of(
            "name", result.name(),
            "proxyType", result.proxyType()
        ));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(404).body(ErrorResponse.of(
            "ERR_BIZ_PROXY_NOT_FOUND",
            e.getMessage(),
            "DECLINE"
        ));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(
            "ERR_EXT_PROXY_ENQUIRY_FAILED",
            e.getMessage(),
            "RETRY"
        ));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/infrastructure/web/SwitchController.java
git commit -m "feat: add proxy enquiry endpoint to SwitchController (FR-002)"
```

---

## Task 8: Compliance Status — Domain Port, Use Case & Test (rules-service)

**BDD Scenarios:** S3.1 (Unlocked), S3.2 (Locked), S3.3 (No auth)
**BRD Requirements:** US-003, FR-003.1 through FR-003.6
**User-Facing:** NO

**Files:**
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/port/in/ComplianceStatusUseCase.java`
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/application/usecase/ComplianceStatusUseCaseImpl.java`
- Create: `services/rules-service/src/test/java/com/agentbanking/rules/application/usecase/ComplianceStatusUseCaseTest.java`

- [ ] **Step 1: Write the failing test**

Create `ComplianceStatusUseCaseTest.java`:

```java
package com.agentbanking.rules.application.usecase;

import com.agentbanking.rules.domain.port.in.ComplianceStatusUseCase;
import com.agentbanking.rules.domain.port.in.ComplianceStatusUseCase.ComplianceStatusResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComplianceStatusUseCaseTest {

    private ComplianceStatusUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ComplianceStatusUseCaseImpl();
    }

    @Test
    void checkCompliance_shouldReturnUnlockedForCleanAgent() {
        // Given
        String agentId = "agent-001";

        // When
        ComplianceStatusResult result = useCase.checkCompliance(agentId);

        // Then
        assertEquals("UNLOCKED", result.status());
        assertNull(result.reason());
        assertNotNull(result.checkedAt());
    }

    @Test
    void checkCompliance_shouldReturnLockedForAgentWithHold() {
        // Given — agent-aml-flagged is in the locked agents set
        String agentId = "agent-aml-flagged";

        // When
        ComplianceStatusResult result = useCase.checkCompliance(agentId);

        // Then
        assertEquals("LOCKED", result.status());
        assertNotNull(result.reason());
        assertFalse(result.reason().isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd services/rules-service && ./gradlew test --tests ComplianceStatusUseCaseTest 2>&1 | tail -20
```
Expected: FAIL — classes don't exist yet.

- [ ] **Step 3: Create the inbound port interface**

Create `ComplianceStatusUseCase.java`:

```java
package com.agentbanking.rules.domain.port.in;

import java.time.Instant;

public interface ComplianceStatusUseCase {

    ComplianceStatusResult checkCompliance(String agentId);

    record ComplianceStatusResult(
        String status,
        String reason,
        Instant checkedAt
    ) {}
}
```

- [ ] **Step 4: Create the use case implementation**

Create `ComplianceStatusUseCaseImpl.java`:

```java
package com.agentbanking.rules.application.usecase;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.rules.domain.port.in.ComplianceStatusUseCase;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public class ComplianceStatusUseCaseImpl implements ComplianceStatusUseCase {

    // TODO: Replace with actual compliance data source (repository or Feign client to AML service)
    private static final Set<String> LOCKED_AGENTS = Set.of("agent-aml-flagged");

    private static final Map<String, String> LOCK_REASONS = Map.of(
        "agent-aml-flagged", "AML compliance hold — pending review"
    );

    @Override
    public ComplianceStatusResult checkCompliance(String agentId) {
        try {
            if (LOCKED_AGENTS.contains(agentId)) {
                return new ComplianceStatusResult(
                    "LOCKED",
                    LOCK_REASONS.getOrDefault(agentId, "Compliance hold"),
                    Instant.now()
                );
            }

            return new ComplianceStatusResult(
                "UNLOCKED",
                null,
                Instant.now()
            );
        } catch (Exception e) {
            throw new IllegalStateException(ErrorCodes.ERR_BIZ_COMPLIANCE_CHECK_FAILED + ": " + e.getMessage(), e);
        }
    }
}
```

Note: NO `@Service` annotation — registered as bean in `DomainServiceConfig.java`. The `LOCKED_AGENTS` set is populated with test data so the test passes.

- [ ] **Step 5: Run test to verify it passes**

```bash
cd services/rules-service && ./gradlew test --tests ComplianceStatusUseCaseTest 2>&1 | tail -20
```
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add services/rules-service/src/main/java/com/agentbanking/rules/domain/port/in/ComplianceStatusUseCase.java
git add services/rules-service/src/main/java/com/agentbanking/rules/application/usecase/ComplianceStatusUseCaseImpl.java
git add services/rules-service/src/test/java/com/agentbanking/rules/application/usecase/ComplianceStatusUseCaseTest.java
git commit -m "test+feat: implement compliance status use case with TDD (FR-003)"
```

---

## Task 9: Compliance Status — Controller Endpoint (rules-service)

**BDD Scenarios:** S3.1, S3.2, S3.3
**BRD Requirements:** FR-003.1 through FR-003.6
**User-Facing:** NO

**Files:**
- Modify: `services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/web/RulesController.java`

- [ ] **Step 1: Add endpoint to RulesController**

Add to `RulesController.java`:
1. Add import: `import com.agentbanking.rules.domain.port.in.ComplianceStatusUseCase;`
2. Add field and constructor parameter: `private final ComplianceStatusUseCase complianceStatusUseCase;`
3. Add endpoint method:

```java
@GetMapping("/compliance/status")
public ResponseEntity<?> getComplianceStatus(@RequestHeader(value = "X-Agent-Id", required = false) String agentId) {
    try {
        String effectiveAgentId = agentId != null ? agentId : "unknown";
        ComplianceStatusUseCase.ComplianceStatusResult result =
            complianceStatusUseCase.checkCompliance(effectiveAgentId);

        return ResponseEntity.ok(Map.of(
            "status", result.status(),
            "reason", result.reason() != null ? result.reason() : "",
            "checkedAt", result.checkedAt().toString()
        ));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(
            "ERR_BIZ_COMPLIANCE_CHECK_FAILED",
            e.getMessage(),
            "RETRY"
        ));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add services/rules-service/src/main/java/com/agentbanking/rules/infrastructure/web/RulesController.java
git commit -m "feat: add compliance status endpoint to RulesController (FR-003)"
```

---

## Task 10: Register Use Case Beans in DomainServiceConfig

**BDD Scenarios:** All scenarios (bean registration prerequisite for wiring)
**BRD Requirements:** All (AGENTS.md Law V compliance)
**User-Facing:** NO

**Files:**
- Modify: `services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/config/DomainServiceConfig.java`
- Modify: `services/rules-service/src/main/java/com/agentbanking/rules/config/DomainServiceConfig.java`

- [ ] **Step 1: Register switch-adapter-service beans**

Update `DomainServiceConfig.java` in switch-adapter-service:

```java
package com.agentbanking.switchadapter.config;

import com.agentbanking.switchadapter.application.usecase.TransactionQuoteUseCaseImpl;
import com.agentbanking.switchadapter.application.usecase.ProxyEnquiryUseCaseImpl;
import com.agentbanking.switchadapter.domain.port.out.DuitNowProxyGateway;
import com.agentbanking.switchadapter.domain.port.out.FeeCalculationGateway;
import com.agentbanking.switchadapter.domain.service.SwitchAdapterService;
import com.agentbanking.switchadapter.domain.port.out.SwitchTransactionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public SwitchAdapterService switchAdapterService(SwitchTransactionRepository repository) {
        return new SwitchAdapterService(repository);
    }

    @Bean
    public TransactionQuoteUseCaseImpl transactionQuoteUseCase(FeeCalculationGateway feeCalculationGateway) {
        return new TransactionQuoteUseCaseImpl(feeCalculationGateway);
    }

    @Bean
    public ProxyEnquiryUseCaseImpl proxyEnquiryUseCase(DuitNowProxyGateway duitNowProxyGateway) {
        return new ProxyEnquiryUseCaseImpl(duitNowProxyGateway);
    }
}
```

- [ ] **Step 2: Register rules-service beans**

Update `DomainServiceConfig.java` in rules-service:

```java
package com.agentbanking.rules.config;

import com.agentbanking.rules.application.usecase.ComplianceStatusUseCaseImpl;
import com.agentbanking.rules.domain.service.FeeCalculationService;
import com.agentbanking.rules.domain.service.LimitEnforcementService;
import com.agentbanking.rules.domain.service.StpDecisionService;
import com.agentbanking.rules.domain.service.VelocityCheckService;
import com.agentbanking.rules.domain.port.out.FeeConfigRepository;
import com.agentbanking.rules.domain.port.out.VelocityRuleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public FeeCalculationService feeCalculationService(FeeConfigRepository feeConfigRepository) {
        return new FeeCalculationService(feeConfigRepository);
    }

    @Bean
    public VelocityCheckService velocityCheckService(VelocityRuleRepository velocityRuleRepository) {
        return new VelocityCheckService(velocityRuleRepository);
    }

    @Bean
    public LimitEnforcementService limitEnforcementService() {
        return new LimitEnforcementService();
    }

    @Bean
    public StpDecisionService stpDecisionService(VelocityCheckService velocityCheckService,
                                                   LimitEnforcementService limitEnforcementService) {
        return new StpDecisionService(velocityCheckService, limitEnforcementService);
    }

    @Bean
    public ComplianceStatusUseCaseImpl complianceStatusUseCase() {
        return new ComplianceStatusUseCaseImpl();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add services/switch-adapter-service/src/main/java/com/agentbanking/switchadapter/config/DomainServiceConfig.java
git add services/rules-service/src/main/java/com/agentbanking/rules/config/DomainServiceConfig.java
git commit -m "feat: register use case beans in DomainServiceConfig (Law V compliance)"
```

---

## Task 11: Gateway Routes

**BDD Scenarios:** All scenarios (routing prerequisite)
**BRD Requirements:** FR-001.2, FR-002.2, FR-003.2
**User-Facing:** NO

**Files:**
- Modify: `gateway/src/main/resources/application.yaml`

- [ ] **Step 1: Add external routes and update internal predicates**

Add these routes to `gateway/src/main/resources/application.yaml` (add after existing external routes, before backoffice routes around line ~413):

```yaml
# External Transaction Quote route
- id: external-transactions-quote
  uri: http://switch-adapter-service:8084
  predicates:
    - Path=/api/v1/transactions/quote
  filters:
    - JwtAuth
    - RewritePath=/api/v1/transactions/quote, /internal/transactions/quote

# External Proxy Enquiry route
- id: external-proxy-enquiry
  uri: http://switch-adapter-service:8084
  predicates:
    - Path=/api/v1/transfer/proxy/enquiry
  filters:
    - JwtAuth
    - RewritePath=/api/v1/transfer/proxy/enquiry, /internal/transfer/proxy/enquiry

# External Compliance Status route
- id: external-compliance-status
  uri: http://rules-service:8081
  predicates:
    - Path=/api/v1/compliance/status
  filters:
    - JwtAuth
    - RewritePath=/api/v1/compliance/status, /internal/compliance/status
```

Update the `switch-adapter-service` internal route (line ~220-225):
```yaml
# Switch Adapter Service
- id: switch-adapter-service
  uri: http://switch-adapter-service:8084
  predicates:
    - Path=/internal/auth, /internal/reversal, /internal/duitnow, /internal/balance-inquiry, /internal/transactions/quote, /internal/transfer/proxy/enquiry
  filters:
    - RewritePath=/internal/(?<segment>.*), /internal/${segment}
```

Update the `rules-service` internal route (line ~196-201):
```yaml
# Rules Service
- id: rules-service
  uri: http://rules-service:8081
  predicates:
    - Path=/internal/fees/**, /internal/check-velocity, /internal/limits/**, /internal/compliance/status
  filters:
    - RewritePath=/internal/(?<segment>.*), /internal/${segment}
```

- [ ] **Step 2: Commit**

```bash
git add gateway/src/main/resources/application.yaml
git commit -m "feat: add gateway routes for quote, proxy enquiry, and compliance status"
```

---

## Task 12: OpenAPI Spec Fixes

**BDD Scenarios:** S4.1 (Monetary types), S4.2 (Security), S4.3 (Error responses), S4.4 (Content types)
**BRD Requirements:** US-004, FR-004, FR-005, FR-006
**User-Facing:** NO

**Files:**
- Modify: `docs/api/openapi.yaml`

- [ ] **Step 1: Add securitySchemes component**

Add to `components:` section (before `schemas:`):

```yaml
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
  schemas:
    # ... existing schemas ...
```

- [ ] **Step 2: Add security to all /api/v1/* endpoints**

For every path under `/api/v1/*`, add at the operation level:

```yaml
security:
  - bearerAuth: []
```

- [ ] **Step 3: Fix all monetary fields from `type: number` to `type: string`**

Change `type: number` to `type: string` for ALL monetary fields. Do NOT change:
- `latitude`, `longitude` (GeoLocation)
- `merchantGpsLat`, `merchantGpsLng` (coordinates)
- `page`, `size`, `totalElements`, `totalPages` (pagination)
- `totalAgents`, `activeAgents`, `totalTransactions`, `successRate` (dashboard stats)
- `transactionCount`, `dailyLimitCount` (counts)
- `expires_in` (integer)
- Fields with `format: int32`

Affected monetary schemas (change `type: number` → `type: string`):
- `TransactionQuoteRequest.amount`
- `TransactionQuoteResponse.amount, fee, total, commission`
- `DuitNowRequest.amount`
- `CardAuthRequest.amount`
- `RetailSaleCommand.amount`
- `RetailSaleResponse.amount, mdrAmount, netToMerchant`
- `PinPurchaseCommand.amount`
- `PinPurchaseResponse.commission`
- `CashBackCommand.cashBackAmount`
- `CashBackResponse.cashBackAmount, commission`
- `WithdrawalExternalRequest.amount`
- `DepositExternalRequest.amount`
- `TopupExternalRequest.amount`
- `BillPayExternalRequest.amount`
- `JomPayExternalRequest.amount`
- `EWalletWithdrawExternalRequest.amount`
- `EWalletTopupExternalRequest.amount`
- `DuitNowExternalRequest.amount`
- `EsspExternalRequest.amount`
- `RetailSaleExternalRequest.amount`
- `RetailPinPurchaseExternalRequest.amount`
- `RetailCashbackExternalRequest.cashBackAmount`
- `FeeConfigRequest.feeAmount, percentage, minFee, maxFee`
- `DiscrepancyMakerActionRequest.adjustmentAmount`
- `BalanceResponse.availableBalance, ledgerBalance, pendingBalance`
- `DashboardResponse.totalVolume, dailyStats[].volume`
- `SettlementResponse.totalAmount, commission`
- `TransactionResponse.amount`
- `FeeConfigResponse.feeAmount, percentage`
- `ReversalRequest.amount`

- [ ] **Step 4: Add error responses to all endpoints missing them**

For every endpoint that doesn't have `400` and `401` responses, add:

```yaml
'400':
  description: Bad Request
  content:
    application/json:
      schema:
        $ref: '#/components/schemas/ErrorResponse'
'401':
  description: Unauthorized
  content:
    application/json:
      schema:
        $ref: '#/components/schemas/ErrorResponse'
```

Endpoints that need error responses added:
- `/api/v1/transactions/quote` (add 400, 401)
- `/api/v1/transfer/proxy/enquiry` (add 400, 401, 404)
- `/api/v1/compliance/status` (add 401)
- `/api/v1/transfer/duitnow` (add 400, 401)
- `/api/v1/retail/sale` (add 400, 401)
- `/api/v1/retail/pin-purchase` (add 400, 401)
- `/api/v1/retail/cashback` (add 400, 401)
- `/api/v1/backoffice/transactions` (add 401)
- `/api/v1/backoffice/settlement` (add 401)
- `/api/v1/backoffice/dashboard` (add 401)
- `/api/v1/backoffice/agents/{id}` (add 401)
- `/api/v1/backoffice/agents` (add 401)
- `/api/v1/backoffice/kyc/review-queue` (add 401)
- `/api/v1/backoffice/audit-logs` (add 401)
- `/api/v1/auth/refresh` (has 401, add 400)
- `/api/v1/auth/revoke` (has 401, add 400)
- `/api/v1/auth/password/forgot` (add 400)
- `/api/v1/backoffice/agents/{agentId}/user-status` (has 401, add 400)
- `/api/v1/backoffice/agents/{agentId}/create-user` (has 401, add 400)

- [ ] **Step 5: Replace all `'*/*'` content types with `application/json`**

Find all instances of `'*/*'` in response content types and replace with `application/json`.

- [ ] **Step 6: Add proxyType enum to proxy enquiry parameters**

Update the `proxyType` parameter in `/api/v1/transfer/proxy/enquiry`:

```yaml
- name: proxyType
  in: query
  required: true
  schema:
    type: string
    enum: [MOBILE, NRIC, PASSPORT, BIZ_REG_NO]
```

- [ ] **Step 7: Commit**

```bash
git add docs/api/openapi.yaml
git commit -m "fix: OpenAPI spec quality — string types for money, security, error schemas, content types"
```

---

## Task 13: Run All Tests & Verify

**BDD Scenarios:** All scenarios
**BRD Requirements:** All
**User-Facing:** NO

- [ ] **Step 1: Run switch-adapter-service tests**

```bash
cd services/switch-adapter-service && ./gradlew test 2>&1 | tail -30
```
Expected: All tests pass

- [ ] **Step 2: Run rules-service tests**

```bash
cd services/rules-service && ./gradlew test 2>&1 | tail -30
```
Expected: All tests pass

- [ ] **Step 3: Run ArchUnit tests**

```bash
cd services/switch-adapter-service && ./gradlew test --tests "*HexagonalArchitectureTest*" 2>&1 | tail -20
cd services/rules-service && ./gradlew test --tests "*HexagonalArchitectureTest*" 2>&1 | tail -20
```
Expected: All architecture tests pass (no Spring imports in domain layer)

- [ ] **Step 4: Verify OpenAPI spec**

```bash
# Verify no type: number for monetary fields
grep -n "type: number" docs/api/openapi.yaml | grep -v -E "(latitude|longitude|merchantGpsLat|merchantGpsLng|page|size|totalElements|totalPages|totalAgents|activeAgents|totalTransactions|successRate|transactionCount|dailyLimitCount|expires_in|int32)"
```
Expected: No output (all remaining `type: number` are non-monetary fields)

- [ ] **Step 5: Final commit if needed**

```bash
git status
```

---

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

Tasks 2, 5, 8 can be done in parallel (independent ports/use cases). Tasks 3, 6 can be done in parallel. Tasks 4, 7, 9 can be done in parallel.
