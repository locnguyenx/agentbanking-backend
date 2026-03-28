# AML/Fraud & Velocity Implementation Plan (US-EFM01-EFM04)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement AML/Fraud screening, velocity rule enforcement, geofence validation, and Early Fraud Monitoring (EFM) event publishing per user stories US-EFM01 through US-EFM04.

**Architecture:** Hexagonal (Ports & Adapters) pattern. AML screening via Feign client to external provider. Velocity rules in Rules Service database. Geofence validation in Ledger Service. EFM events published to Kafka.

**Tech Stack:** Java 21, Spring Boot 3.2.5, Spring Data JPA, PostgreSQL, Kafka, OpenFeign, Resilience4j, JUnit 5, Mockito, ArchUnit.

---

## Task 1: Domain Layer - Velocity Rules [DONE]

**BDD Scenarios:** BDD-R03, BDD-R03-EC-01, BDD-R03-EC-02, BDD-R03-EC-03, BDD-R03-EC-04, BDD-EFM04, BDD-EFM04-EC-01
**BRD Requirements:** US-R03, US-EFM04, FR-1.3, FR-14.5
**User-Facing:** NO

**Files:**
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/model/VelocityRuleRecord.java` ✅
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/service/VelocityCheckService.java` ✅
- Create: `services/rules-service/src/main/java/com/agentbanking/rules/domain/port/out/TransactionCountPort.java` ✅

### Step 1: Create VelocityRule entity

```java
package com.agentbanking.rules.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record VelocityRule(
    Long ruleId,
    String transactionType,
    String customerMykadPattern,
    BigDecimal maxAmountPerTransaction,
    BigDecimal maxAmountPerDay,
    Integer maxCountPerDay,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
```

### Step 2: Create VelocityCheckService domain service

```java
package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.VelocityRule;
import com.agentbanking.rules.domain.port.out.VelocityRuleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class VelocityCheckService {

    private final VelocityRuleRepository velocityRuleRepository;
    private final TransactionCountPort transactionCountPort;

    public VelocityCheckService(VelocityRuleRepository velocityRuleRepository,
                                 TransactionCountPort transactionCountPort) {
        this.velocityRuleRepository = velocityRuleRepository;
        this.transactionCountPort = transactionCountPort;
    }

    public VelocityResult checkVelocity(String customerMykad, String transactionType, BigDecimal amount) {
        List<VelocityRule> rules = velocityRuleRepository
            .findByTransactionTypeAndMykadPattern(transactionType, customerMykad);

        for (VelocityRule rule : rules) {
            if (!rule.active()) continue;

            // Check per-transaction limit
            if (rule.maxAmountPerTransaction() != null
                && amount.compareTo(rule.maxAmountPerTransaction()) > 0) {
                return new VelocityResult(false, "ERR_VELOCITY_AMOUNT_EXCEEDED",
                    BigDecimal.ZERO, 0);
            }

            // Check daily amount limit
            if (rule.maxAmountPerDay() != null) {
                BigDecimal dailyTotal = transactionCountPort
                    .getDailyAmountTotal(customerMykad, transactionType, LocalDate.now());
                if (dailyTotal.add(amount).compareTo(rule.maxAmountPerDay()) > 0) {
                    return new VelocityResult(false, "ERR_VELOCITY_AMOUNT_EXCEEDED",
                        rule.maxAmountPerDay().subtract(dailyTotal), 0);
                }
            }

            // Check daily count limit
            if (rule.maxCountPerDay() != null) {
                int dailyCount = transactionCountPort
                    .getDailyCount(customerMykad, transactionType, LocalDate.now());
                if (dailyCount >= rule.maxCountPerDay()) {
                    return new VelocityResult(false, "ERR_VELOCITY_COUNT_EXCEEDED",
                        BigDecimal.ZERO, 0);
                }
            }
        }

        return new VelocityResult(true, null, null, null);
    }

    public record VelocityResult(
        boolean passed,
        String errorCode,
        BigDecimal remainingAmount,
        int remainingCount
    ) {}
}
```

### Step 3: Create TransactionCountPort

```java
package com.agentbanking.rules.domain.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TransactionCountPort {
    BigDecimal getDailyAmountTotal(String customerMykad, String transactionType, LocalDate date);
    int getDailyCount(String customerMykad, String transactionType, LocalDate date);
}
```

### Step 4: Commit

```bash
git add services/rules-service/src/main/java/com/agentbanking/rules/domain/
git commit -m "feat(rules): add velocity rules and check service"
```

---

## Task 2: Domain Layer - Geofence Validation [DONE]

**BDD Scenarios:** BDD-W01-EC-05, BDD-W01-EC-06
**BRD Requirements:** NFR-4.2, FR-3.3
**User-Facing:** NO

**Status:** Geofencing implemented via `GeofenceChecker` utility in `common` module and integrated directly in `LedgerService.processWithdrawal()`. GPS unavailable and geofence violation fraud alerts published via `EfmEventPublisher`.

**Files:**
- ✅ `common/src/main/java/com/agentbanking/common/geofence/GeofenceChecker.java`
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java` (geofence check integrated)

### Step 1: Create GeofenceValidation entity

```java
package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;

public record GeofenceValidation(
    boolean valid,
    BigDecimal distanceMeters,
    String errorCode
) {}
```

### Step 2: Create GeofenceValidationService domain service

```java
package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.GeofenceValidation;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class GeofenceValidationService {

    private static final BigDecimal GEOFENCE_RADIUS_METERS = new BigDecimal("100");
    private static final BigDecimal EARTH_RADIUS_METERS = new BigDecimal("6371000");

    public GeofenceValidation validate(BigDecimal agentLat, BigDecimal agentLng,
                                        BigDecimal posLat, BigDecimal posLng) {
        // Handle null GPS (unavailable)
        if (posLat == null || posLng == null) {
            return new GeofenceValidation(false, BigDecimal.ZERO, "ERR_GPS_UNAVAILABLE");
        }

        // Calculate distance using Haversine formula
        BigDecimal distance = calculateDistance(agentLat, agentLng, posLat, posLng);

        if (distance.compareTo(GEOFENCE_RADIUS_METERS) <= 0) {
            return new GeofenceValidation(true, distance, null);
        } else {
            return new GeofenceValidation(false, distance, "ERR_GEOFENCE_VIOLATION");
        }
    }

    private BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lng1,
                                          BigDecimal lat2, BigDecimal lng2) {
        // Haversine formula implementation
        BigDecimal dLat = lat2.subtract(lat1).multiply(BigDecimal.valueOf(Math.PI / 180));
        BigDecimal dLng = lng2.subtract(lng1).multiply(BigDecimal.valueOf(Math.PI / 180));

        BigDecimal a = BigDecimal.valueOf(Math.sin(dLat.divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP).doubleValue()))
            .pow(2)
            .add(
                BigDecimal.valueOf(Math.cos(lat1.multiply(BigDecimal.valueOf(Math.PI / 180)).doubleValue()))
                    .multiply(BigDecimal.valueOf(Math.cos(lat2.multiply(BigDecimal.valueOf(Math.PI / 180)).doubleValue())))
                    .multiply(BigDecimal.valueOf(Math.sin(dLng.divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP).doubleValue())).pow(2))
            );

        BigDecimal c = BigDecimal.valueOf(2 * Math.atan2(Math.sqrt(a.doubleValue()), Math.sqrt(BigDecimal.ONE.subtract(a).doubleValue())));

        return EARTH_RADIUS_METERS.multiply(c).setScale(2, RoundingMode.HALF_UP);
    }
}
```

### Step 3: Commit

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/GeofenceValidation.java
git add services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/GeofenceValidationService.java
git commit -m "feat(ledger): add geofence validation service"
```

---

## Task 3: Domain Layer - EFM Event Publishing [DONE]

**BDD Scenarios:** BDD-EFM01, BDD-EFM02, BDD-EFM03, BDD-EFM04
**BRD Requirements:** US-EFM01, US-EFM02, US-EFM03, US-EFM04, FR-14.1, FR-14.2
**User-Facing:** NO

**Status:** EFM implemented via `EfmEventPublisher` Spring component in `common` module. Injected into `LedgerService` and used for publishing events on all transaction types (withdrawal, deposit, retail sale, cash-back) plus fraud alerts (GPS unavailable, geofence violation, switch rejections).

**Files:**
- ✅ `common/src/main/java/com/agentbanking/common/efm/EfmEventPublisher.java` (converted to Spring @Component)
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/LedgerService.java` (EFM integrated)

### Step 1: Create EfmEventType enum

```java
package com.agentbanking.ledger.domain.model;

public enum EfmEventType {
    HIGH_VALUE_TRANSACTION,
    VELOCITY_EXCEEDED,
    GEOFENCE_VIOLATION,
    SUSPICIOUS_PATTERN,
    MULTIPLE_FAILED_ATTEMPTS
}
```

### Step 2: Create EfmEvent entity

```java
package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EfmEvent(
    UUID eventId,
    EfmEventType eventType,
    String customerId,
    UUID agentId,
    BigDecimal amount,
    String details,
    Instant timestamp
) {}
```

### Step 3: Create EfmEventService domain service

```java
package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.*;
import com.agentbanking.ledger.domain.port.out.EventPublisherPort;
import java.math.BigDecimal;
import java.util.UUID;

public class EfmEventService {

    private final EventPublisherPort eventPublisher;
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("5000.00");

    public EfmEventService(EventPublisherPort eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void checkAndPublishEfmEvents(String customerId, UUID agentId,
                                          BigDecimal amount, String transactionType) {
        // Check for high value transaction
        if (amount.compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            publishEfmEvent(EfmEventType.HIGH_VALUE_TRANSACTION, customerId, agentId,
                amount, "Transaction amount exceeds RM 5,000 threshold");
        }
    }

    public void publishVelocityExceeded(String customerId, UUID agentId, String errorCode) {
        publishEfmEvent(EfmEventType.VELOCITY_EXCEEDED, customerId, agentId,
            BigDecimal.ZERO, "Velocity check failed: " + errorCode);
    }

    public void publishGeofenceViolation(String customerId, UUID agentId, BigDecimal distance) {
        publishEfmEvent(EfmEventType.GEOFENCE_VIOLATION, customerId, agentId,
            distance, "Transaction attempted outside geofence radius");
    }

    private void publishEfmEvent(EfmEventType eventType, String customerId,
                                  UUID agentId, BigDecimal amount, String details) {
        EfmEvent event = new EfmEvent(
            UUID.randomUUID(), eventType, customerId, agentId,
            amount, details, java.time.Instant.now()
        );
        eventPublisher.publishEfmEvent(event);
    }
}
```

### Step 4: Commit

```bash
git add services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/EfmEvent.java
git add services/ledger-service/src/main/java/com/agentbanking/ledger/domain/model/EfmEventType.java
git add services/ledger-service/src/main/java/com/agentbanking/ledger/domain/service/EfmEventService.java
git commit -m "feat(ledger): add EFM event types and service"
```

---

## Task 4: Infrastructure Layer - AML Screening Adapter [DONE]

**BDD Scenarios:** BDD-EFM01, BDD-EFM02, BDD-EFM03
**BRD Requirements:** US-EFM01, US-EFM02, US-EFM03, FR-14.1, FR-14.2
**User-Facing:** NO

**Files:**
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/external/AmlScreeningFeignClient.java`
- ✅ `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/external/AmlScreeningAdapter.java`

### Step 1: Create AML screening Feign client

### Step 2: Create adapter implementing AmlScreeningPort

### Step 3: Run tests and commit

```bash
./gradlew :onboarding-service:test
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/external/AmlScreeningFeignClient.java
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/external/AmlScreeningAdapter.java
git commit -m "feat(onboarding): add AML screening Feign client and adapter"
```

---

## Task 5: Infrastructure - Kafka EFM Event Publisher [DONE]

**BDD Scenarios:** BDD-EFM01, BDD-EFM02, BDD-EFM03, BDD-EFM04
**BRD Requirements:** US-EFM01, US-EFM02, US-EFM03, US-EFM04
**User-Facing:** NO

**Files:**
- ✅ `services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/messaging/EfmEventPublisherAdapter.java`

### Step 1: Create Kafka publisher for EFM events

### Step 2: Wire into EfmEventService

### Step 3: Run tests and commit

```bash
./gradlew :ledger-service:test
git add services/ledger-service/src/main/java/com/agentbanking/ledger/infrastructure/messaging/EfmEventPublisherAdapter.java
git commit -m "feat(ledger): add Kafka publisher for EFM events"
```

---

## Task 6: Unit Tests [DONE]

**BDD Scenarios:** BDD-R03, BDD-EFM01-EFM04, BDD-W01-EC-05, BDD-W01-EC-06
**BRD Requirements:** US-R03, US-EFM01-EFM04, NFR-4.2
**User-Facing:** NO

**Files:**
- ✅ `services/rules-service/src/test/java/com/agentbanking/rules/domain/service/VelocityCheckServiceTest.java`
- ✅ `common/src/test/java/com/agentbanking/common/geofence/GeofenceCheckerTest.java`
- ✅ `common/src/test/java/com/agentbanking/common/efm/EfmEventPublisherTest.java`

### Step 1: Write unit tests for velocity check service

### Step 2: Write unit tests for geofence validation service

### Step 3: Write unit tests for EFM event service

### Step 4: Run all tests and commit

```bash
./gradlew :rules-service:test :ledger-service:test
git add services/rules-service/src/test/java/com/agentbanking/rules/domain/service/VelocityCheckServiceTest.java
git add services/ledger-service/src/test/java/com/agentbanking/ledger/domain/service/GeofenceValidationServiceTest.java
git add services/ledger-service/src/test/java/com/agentbanking/ledger/domain/service/EfmEventServiceTest.java
git commit -m "test: add unit tests for velocity, geofence, and EFM services"
```

---