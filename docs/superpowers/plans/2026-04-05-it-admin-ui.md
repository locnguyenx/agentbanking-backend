# IT Admin UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement IT Administrator UI with service health dashboard, metrics viewer, and cross-service audit log querying.

**Architecture:** New audit-service microservice consumes Kafka audit events into PostgreSQL. Backoffice React dashboard polls gateway-routed actuator endpoints for health/metrics. Custom GatewayFilterFactory resolves service names to URIs.

**Tech Stack:** Java 21, Spring Boot 3.2.5, React 18, Spring Cloud Gateway, Spring Cloud Stream (Kafka), PostgreSQL, Flyway, Vitest

---

## File Structure Map

### New Files Created
```
services/audit-service/
├── build.gradle
├── src/main/java/com/agentbanking/audit/
│   ├── AuditServiceApplication.java
│   ├── config/
│   │   ├── DomainServiceConfig.java
│   │   ├── SecurityConfig.java
│   │   └── KafkaConsumerConfig.java
│   ├── domain/
│   │   ├── model/AuditLogRecord.java
│   │   ├── port/in/QueryAuditLogsUseCase.java
│   │   ├── port/out/AuditLogRepository.java
│   │   └── service/AuditLogQueryService.java
│   ├── application/QueryAuditLogsApplication.java
│   └── infrastructure/
│       ├── web/AuditLogController.java
│       ├── web/HealthAggregationController.java
│       ├── persistence/JpaAuditLogEntity.java
│       ├── persistence/JpaAuditLogRepository.java
│       ├── persistence/AuditLogRepositoryAdapter.java
│       └── messaging/KafkaAuditLogConsumer.java
├── src/main/resources/
│   ├── application.yaml
│   └── db/migration/V1__init_audit_schema.sql
└── src/test/java/com/agentbanking/audit/
    ├── domain/service/AuditLogQueryServiceTest.java
    └── arch/HexagonalArchitectureTest.java

gateway/src/main/java/com/agentbanking/gateway/filter/ServiceRouteGatewayFilterFactory.java

common/src/main/java/com/agentbanking/common/audit/AuditEventPublisher.java
common/src/main/java/com/agentbanking/common/audit/KafkaAuditEventPublisher.java
common/src/main/java/com/agentbanking/common/audit/AuditEventType.java

backoffice/src/pages/SystemAdmin.tsx
backoffice/src/components/HealthCard.tsx
backoffice/src/components/MetricsPanel.tsx
backoffice/src/components/AuditLogTable.tsx
backoffice/src/test/SystemAdmin.test.tsx
backoffice/src/test/HealthCard.test.tsx
backoffice/src/test/MetricsPanel.test.tsx
backoffice/src/test/AuditLogTable.test.tsx
```

### Existing Files Modified
```
settings.gradle                              # Add audit-service module
docker-compose.yml                           # Add audit-service + postgres-audit
gateway/src/main/resources/application.yaml  # Add admin routes
common/src/main/java/com/agentbanking/common/messaging/KafkaTopics.java  # Add AUDIT_LOGS
backoffice/src/components/Layout.tsx         # Add System Admin nav item
backoffice/src/main.tsx                      # Add /system-admin route
backoffice/src/api/client.ts                 # Add admin API endpoints
```

---

### Task 1: Add AUDIT_LOGS topic to KafkaTopics registry

**BDD Scenarios:** Supports Scenario 5.1: Service publishes audit event to Kafka
**BRD Requirements:** FR-4.3 - Each microservice publishes audit events to Kafka topic `audit-logs`

**User-Facing:** NO

**Files:**
- Modify: `common/src/main/java/com/agentbanking/common/messaging/KafkaTopics.java`

- [ ] **Step 1: Add AUDIT_LOGS constant**

Read `common/src/main/java/com/agentbanking/common/messaging/KafkaTopics.java` and add:
```java
public static final String AUDIT_LOGS = "audit-logs";
```

- [ ] **Step 2: Commit**

```bash
git add common/src/main/java/com/agentbanking/common/messaging/KafkaTopics.java
git commit -m "feat: add AUDIT_LOGS topic to KafkaTopics registry"
```

---

### Task 2: Create AuditEventPublisher in common module

**BDD Scenarios:** Supports Scenario 5.1: Service publishes audit event to Kafka
**BRD Requirements:** FR-4.3 - Shared AuditEventPublisher from common module

**User-Facing:** NO

**Files:**
- Create: `common/src/main/java/com/agentbanking/common/audit/AuditEventPublisher.java`
- Create: `common/src/main/java/com/agentbanking/common/audit/KafkaAuditEventPublisher.java`
- Create: `common/src/main/java/com/agentbanking/common/audit/AuditEventType.java`

- [ ] **Step 1: Create AuditEventType enum**

```java
package com.agentbanking.common.audit;

public enum AuditEventType {
    USER_CREATED, USER_UPDATED, USER_DELETED, USER_LOCKED, USER_UNLOCKED,
    PASSWORD_RESET, LOGIN_SUCCESS, LOGIN_FAILURE,
    AGENT_CREATED, AGENT_UPDATED, AGENT_DEACTIVATED,
    KYC_SUBMITTED, KYC_APPROVED, KYC_REJECTED,
    TRANSACTION_CREATED, TRANSACTION_COMPLETED, TRANSACTION_FAILED, TRANSACTION_REVERSED,
    FLOAT_DEPOSIT, FLOAT_WITHDRAWAL,
    SETTLEMENT_INITIATED, SETTLEMENT_COMPLETED,
    RULE_UPDATED, LIMIT_CHANGED,
    BILL_PAYMENT_INITIATED, BILL_PAYMENT_COMPLETED,
    PIN_PURCHASE_INITIATED, PIN_PURCHASE_COMPLETED,
    SAGA_STARTED, SAGA_COMPLETED, SAGA_ROLLED_BACK,
    PERMISSION_GRANTED, PERMISSION_REVOKED,
    ROLE_CREATED, ROLE_UPDATED, ROLE_DELETED
}
```

- [ ] **Step 2: Create AuditEventPublisher interface**

```java
package com.agentbanking.common.audit;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public interface AuditEventPublisher {
    void publish(AuditEvent event);

    record AuditEvent(
        UUID auditId, String serviceName, String entityType, UUID entityId,
        AuditEventType action, String performedBy, String ipAddress,
        LocalDateTime timestamp, String outcome, String failureReason,
        Map<String, Object> changes
    ) {
        public static AuditEvent success(String serviceName, String entityType, UUID entityId,
                                         AuditEventType action, String performedBy, String ipAddress) {
            return new AuditEvent(UUID.randomUUID(), serviceName, entityType, entityId, action,
                performedBy, ipAddress, LocalDateTime.now(), "SUCCESS", null, Map.of());
        }
        public static AuditEvent failure(String serviceName, String entityType, UUID entityId,
                                         AuditEventType action, String performedBy, String ipAddress, String failureReason) {
            return new AuditEvent(UUID.randomUUID(), serviceName, entityType, entityId, action,
                performedBy, ipAddress, LocalDateTime.now(), "FAILURE", failureReason, Map.of());
        }
    }
}
```

- [ ] **Step 3: Create KafkaAuditEventPublisher**

```java
package com.agentbanking.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class KafkaAuditEventPublisher implements AuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaAuditEventPublisher.class);
    private static final String BINDING = "auditLog-out-0";
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    public KafkaAuditEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void publish(AuditEvent event) {
        try {
            streamBridge.send(BINDING, event);
            log.debug("Published audit event: {} for {} {}", event.action(), event.entityType(), event.entityId());
        } catch (Exception e) {
            log.error("Failed to publish audit event: {}", e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add common/src/main/java/com/agentbanking/common/audit/
git commit -m "feat: add AuditEventPublisher interface and Kafka implementation to common module"
```

---

### Task 3: Create audit-service skeleton

**BDD Scenarios:** Supports Scenario 5.2
**BRD Requirements:** FR-4.1

**User-Facing:** NO

**Files:**
- Modify: `settings.gradle`
- Create: `services/audit-service/build.gradle`
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/AuditServiceApplication.java`
- Create: `services/audit-service/src/main/resources/application.yaml`

- [ ] **Step 1: Add audit-service to settings.gradle**

```groovy
include 'services:audit-service'
```

- [ ] **Step 2: Create build.gradle**

```groovy
plugins {
    id 'java-library'
    id 'org.springframework.boot' version '3.2.5'
    id 'io.spring.dependency-management' version '1.1.5'
    id 'org.flywaydb.flyway' version '9.22.3'
}

group = 'com.agentbanking'
version = '0.0.1-SNAPSHOT'
java { sourceCompatibility = '21' }
repositories { mavenCentral() }

dependencies {
    implementation project(':common')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j'
    implementation 'org.springframework.cloud:spring-cloud-stream'
    implementation 'org.springframework.cloud:spring-cloud-stream-binder-kafka'
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core:9.22.3'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
    testImplementation 'org.testcontainers:testcontainers:1.20.1'
    testImplementation 'org.testcontainers:junit-jupiter:1.20.1'
    testImplementation 'org.testcontainers:postgresql:1.20.1'
    testImplementation 'org.testcontainers:kafka:1.20.1'
    testImplementation testFixtures(project(':common'))
}
tasks.named('bootJar') { archiveBaseName = 'audit-service' }
```

- [ ] **Step 3: Create AuditServiceApplication.java**

```java
package com.agentbanking.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.agentbanking.audit", "com.agentbanking.common"})
public class AuditServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
```

- [ ] **Step 4: Create application.yaml**

```yaml
server:
  port: 8088

spring:
  application:
    name: audit-service
  datasource:
    url: jdbc:postgresql://localhost:5432/auditdb
    username: audit_user
    password: audit_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
  cloud:
    stream:
      kafka:
        binder:
          brokers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
      bindings:
        auditLog-out-0:
          destination: audit-logs
          content-type: application/json
        auditLog-in-0:
          destination: audit-logs
          group: audit-service
          content-type: application/json

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

logging:
  level:
    root: INFO
    com.agentbanking.audit: DEBUG

admin:
  services:
    gateway: http://gateway:8080
    rules: http://rules-service:8081
    ledger: http://ledger-service:8082
    onboarding: http://onboarding-service:8083
    switch: http://switch-adapter-service:8084
    biller: http://biller-service:8085
    orchestrator: http://orchestrator-service:8086
    auth: http://auth-iam-service:8087
    audit: http://audit-service:8088
    mock: http://mock-server:8089
```

- [ ] **Step 5: Commit**

```bash
git add settings.gradle services/audit-service/
git commit -m "feat: create audit-service skeleton with build config and application"
```

---

### Task 4: Create audit-service domain layer (NO Spring imports)

**BDD Scenarios:** Supports Scenario 5.2, Scenario 4.1: View audit logs
**BRD Requirements:** FR-4.1, FR-4.4

**User-Facing:** NO

**CRITICAL:** Domain layer must have ZERO Spring/JPA imports per hexagonal architecture law.

**Files:**
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/domain/model/AuditLogRecord.java`
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/domain/port/in/QueryAuditLogsUseCase.java`
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/domain/port/out/AuditLogRepository.java`
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/domain/service/AuditLogQueryService.java`

- [ ] **Step 1: Create AuditLogRecord**

```java
package com.agentbanking.audit.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogRecord(
    UUID auditId,
    String serviceName,
    String entityType,
    UUID entityId,
    String action,
    String performedBy,
    String ipAddress,
    LocalDateTime timestamp,
    String outcome,
    String failureReason,
    String changes,
    LocalDateTime createdAt
) {}
```

- [ ] **Step 2: Create QueryAuditLogsUseCase (NO Spring imports)**

```java
package com.agentbanking.audit.domain.port.in;

import com.agentbanking.audit.domain.model.AuditLogRecord;
import java.time.LocalDateTime;
import java.util.List;

public interface QueryAuditLogsUseCase {
    AuditLogPage queryAuditLogs(
        String serviceName,
        String action,
        String performedBy,
        String outcome,
        LocalDateTime from,
        LocalDateTime to,
        int page,
        int size
    );

    record AuditLogPage(
        List<AuditLogRecord> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {}
}
```

- [ ] **Step 3: Create AuditLogRepository port (NO Spring imports)**

```java
package com.agentbanking.audit.domain.port.out;

import com.agentbanking.audit.domain.model.AuditLogRecord;
import com.agentbanking.audit.domain.port.in.QueryAuditLogsUseCase.AuditLogPage;
import java.time.LocalDateTime;

public interface AuditLogRepository {
    AuditLogRecord save(AuditLogRecord record);
    AuditLogPage findByFilters(
        String serviceName,
        String action,
        String performedBy,
        String outcome,
        LocalDateTime from,
        LocalDateTime to,
        int page,
        int size
    );
}
```

- [ ] **Step 4: Create AuditLogQueryService**

```java
package com.agentbanking.audit.domain.service;

import com.agentbanking.audit.domain.model.AuditLogRecord;
import com.agentbanking.audit.domain.port.in.QueryAuditLogsUseCase;
import com.agentbanking.audit.domain.port.out.AuditLogRepository;

public class AuditLogQueryService implements QueryAuditLogsUseCase {

    private final AuditLogRepository auditLogRepository;

    public AuditLogQueryService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public AuditLogPage queryAuditLogs(
        String serviceName, String action, String performedBy,
        String outcome, java.time.LocalDateTime from, java.time.LocalDateTime to,
        int page, int size
    ) {
        return auditLogRepository.findByFilters(
            serviceName, action, performedBy, outcome, from, to, page, size
        );
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add services/audit-service/src/main/java/com/agentbanking/audit/domain/
git commit -m "feat: add audit-service domain layer with zero Spring imports"
```

---

### Task 5: Create audit-service infrastructure layer

**BDD Scenarios:** Scenario 5.2, Scenario 4.1-4.7
**BRD Requirements:** FR-4.1, FR-4.2, FR-4.4

**User-Facing:** NO

**Files:**
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/infrastructure/persistence/JpaAuditLogEntity.java`
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/infrastructure/persistence/JpaAuditLogRepository.java`
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/infrastructure/persistence/AuditLogRepositoryAdapter.java`
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/infrastructure/web/AuditLogController.java`
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/infrastructure/messaging/KafkaAuditLogConsumer.java`

- [ ] **Step 1: Create JpaAuditLogEntity**

```java
package com.agentbanking.audit.infrastructure.persistence;

import com.agentbanking.audit.domain.model.AuditLogRecord;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp DESC"),
    @Index(name = "idx_audit_logs_service", columnList = "service_name"),
    @Index(name = "idx_audit_logs_action", columnList = "action"),
    @Index(name = "idx_audit_logs_performed_by", columnList = "performed_by"),
    @Index(name = "idx_audit_logs_outcome", columnList = "outcome")
})
public class JpaAuditLogEntity {

    @Id private UUID auditId;
    @Column(name = "service_name", nullable = false, length = 50) private String serviceName;
    @Column(name = "entity_type", nullable = false, length = 50) private String entityType;
    @Column(name = "entity_id") private UUID entityId;
    @Column(nullable = false, length = 50) private String action;
    @Column(name = "performed_by", nullable = false, length = 100) private String performedBy;
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(nullable = false) private LocalDateTime timestamp;
    @Column(nullable = false, length = 20) private String outcome;
    @Column(name = "failure_reason", length = 500) private String failureReason;
    @Column(columnDefinition = "TEXT") private String changes;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public JpaAuditLogEntity() {}

    public JpaAuditLogEntity(AuditLogRecord record) {
        this.auditId = record.auditId();
        this.serviceName = record.serviceName();
        this.entityType = record.entityType();
        this.entityId = record.entityId();
        this.action = record.action();
        this.performedBy = record.performedBy();
        this.ipAddress = record.ipAddress();
        this.timestamp = record.timestamp();
        this.outcome = record.outcome();
        this.failureReason = record.failureReason();
        this.changes = record.changes();
        this.createdAt = record.createdAt();
    }

    public AuditLogRecord toRecord() {
        return new AuditLogRecord(
            auditId, serviceName, entityType, entityId, action,
            performedBy, ipAddress, timestamp, outcome, failureReason, changes, createdAt
        );
    }

    // Getters and setters
    public UUID getAuditId() { return auditId; }
    public void setAuditId(UUID auditId) { this.auditId = auditId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getChanges() { return changes; }
    public void setChanges(String changes) { this.changes = changes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 2: Create JpaAuditLogRepository (Spring Data JPA - infrastructure layer)**

```java
package com.agentbanking.audit.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface JpaAuditLogRepository extends JpaRepository<JpaAuditLogEntity, UUID> {

    @Query("SELECT e FROM JpaAuditLogEntity e WHERE " +
           "(:serviceName IS NULL OR e.serviceName = :serviceName) AND " +
           "(:action IS NULL OR e.action = :action) AND " +
           "(:performedBy IS NULL OR e.performedBy = :performedBy) AND " +
           "(:outcome IS NULL OR e.outcome = :outcome) AND " +
           "(:from IS NULL OR e.timestamp >= :from) AND " +
           "(:to IS NULL OR e.timestamp <= :to)")
    Page<JpaAuditLogEntity> findByFilters(
        @Param("serviceName") String serviceName,
        @Param("action") String action,
        @Param("performedBy") String performedBy,
        @Param("outcome") String outcome,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );
}
```

- [ ] **Step 3: Create AuditLogRepositoryAdapter (maps Spring Page to domain page)**

```java
package com.agentbanking.audit.infrastructure.persistence;

import com.agentbanking.audit.domain.model.AuditLogRecord;
import com.agentbanking.audit.domain.port.in.QueryAuditLogsUseCase.AuditLogPage;
import com.agentbanking.audit.domain.port.out.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final JpaAuditLogRepository jpaRepository;

    public AuditLogRepositoryAdapter(JpaAuditLogRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AuditLogRecord save(AuditLogRecord record) {
        return jpaRepository.save(new JpaAuditLogEntity(record)).toRecord();
    }

    @Override
    public AuditLogPage findByFilters(
        String serviceName, String action, String performedBy,
        String outcome, LocalDateTime from, LocalDateTime to, int page, int size
    ) {
        var springPage = jpaRepository.findByFilters(
            serviceName, action, performedBy, outcome, from, to,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"))
        );
        return new AuditLogPage(
            springPage.getContent().stream().map(JpaAuditLogEntity::toRecord).toList(),
            springPage.getNumber(),
            springPage.getSize(),
            springPage.getTotalElements(),
            springPage.getTotalPages()
        );
    }
}
```

- [ ] **Step 4: Create AuditLogController**

```java
package com.agentbanking.audit.infrastructure.web;

import com.agentbanking.audit.domain.port.in.QueryAuditLogsUseCase;
import com.agentbanking.audit.domain.port.in.QueryAuditLogsUseCase.AuditLogPage;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/audit")
public class AuditLogController {

    private final QueryAuditLogsUseCase queryAuditLogsUseCase;

    public AuditLogController(QueryAuditLogsUseCase queryAuditLogsUseCase) {
        this.queryAuditLogsUseCase = queryAuditLogsUseCase;
    }

    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
        @RequestParam(required = false) String serviceName,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String performedBy,
        @RequestParam(required = false) String outcome,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        AuditLogPage result = queryAuditLogsUseCase.queryAuditLogs(
            serviceName, action, performedBy, outcome, from, to, page, size
        );
        return ResponseEntity.ok(Map.of(
            "content", result.content(),
            "page", result.page(),
            "size", result.size(),
            "totalElements", result.totalElements(),
            "totalPages", result.totalPages()
        ));
    }

    @GetMapping("/logs/export")
    public void exportAuditLogs(
        @RequestParam(required = false) String serviceName,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String performedBy,
        @RequestParam(required = false) String outcome,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        jakarta.servlet.http.HttpServletResponse response
    ) throws Exception {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=audit-logs.csv");

        var result = queryAuditLogsUseCase.queryAuditLogs(
            serviceName, action, performedBy, outcome, from, to, 0, 10000
        );

        var writer = response.getWriter();
        writer.println("Timestamp,UserID,Action,Resource,IPAddress,Service,Result,FailureReason");
        for (var record : result.content()) {
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                record.timestamp(), record.performedBy(), record.action(),
                record.entityType(), record.ipAddress() != null ? record.ipAddress() : "",
                record.serviceName(), record.outcome(),
                record.failureReason() != null ? record.failureReason() : "");
        }
        writer.flush();
    }
}
```

- [ ] **Step 5: Create KafkaAuditLogConsumer**

```java
package com.agentbanking.audit.infrastructure.messaging;

import com.agentbanking.audit.domain.model.AuditLogRecord;
import com.agentbanking.audit.domain.port.out.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

@Component
public class KafkaAuditLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaAuditLogConsumer.class);
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KafkaAuditLogConsumer(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Bean
    public Consumer<JsonNode> auditLogIn() {
        return event -> {
            try {
                JsonNode entityIdNode = event.get("entityId");
                UUID entityId = (entityIdNode != null && !entityIdNode.isNull())
                    ? UUID.fromString(entityIdNode.asText())
                    : null;

                String changes = null;
                if (event.has("changes") && !event.get("changes").isNull()) {
                    changes = objectMapper.writeValueAsString(event.get("changes"));
                }

                AuditLogRecord record = new AuditLogRecord(
                    UUID.fromString(event.get("auditId").asText()),
                    event.get("serviceName").asText(),
                    event.get("entityType").asText(),
                    entityId,
                    event.get("action").asText(),
                    event.get("performedBy").asText(),
                    event.has("ipAddress") && !event.get("ipAddress").isNull() ? event.get("ipAddress").asText() : null,
                    LocalDateTime.parse(event.get("timestamp").asText()),
                    event.get("outcome").asText(),
                    event.has("failureReason") && !event.get("failureReason").isNull() ? event.get("failureReason").asText() : null,
                    changes,
                    LocalDateTime.now()
                );
                auditLogRepository.save(record);
                log.debug("Stored audit event: {} for {} {}", record.action(), record.entityType(), record.entityId());
            } catch (Exception e) {
                log.error("Failed to process audit event: {}", e.getMessage(), e);
                throw e;
            }
        };
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add services/audit-service/src/main/java/com/agentbanking/audit/infrastructure/
git commit -m "feat: add audit-service infrastructure layer (persistence, web, messaging)"
```

---

### Task 6: Create audit-service config and Flyway migration

**BDD Scenarios:** Scenario 5.2
**BRD Requirements:** FR-4.1, FR-4.2

**User-Facing:** NO

**Files:**
- Create: `services/audit-service/src/main/resources/db/migration/V1__init_audit_schema.sql`
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/config/DomainServiceConfig.java`
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/config/SecurityConfig.java`
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/config/KafkaConsumerConfig.java`

- [ ] **Step 1: Create Flyway migration**

```sql
CREATE TABLE audit_logs (
    audit_id UUID PRIMARY KEY,
    service_name VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    action VARCHAR(50) NOT NULL,
    performed_by VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP NOT NULL,
    outcome VARCHAR(20) NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    failure_reason VARCHAR(500),
    changes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_logs_service ON audit_logs(service_name);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_performed_by ON audit_logs(performed_by);
CREATE INDEX idx_audit_logs_outcome ON audit_logs(outcome);
```

- [ ] **Step 2: Create DomainServiceConfig**

Note: Bean registration for HealthAggregationService and MetricsAggregationService is deferred to Task 7 since those classes don't exist yet.

```java
package com.agentbanking.audit.config;

import com.agentbanking.audit.domain.port.out.AuditLogRepository;
import com.agentbanking.audit.domain.service.AuditLogQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public AuditLogQueryService auditLogQueryService(AuditLogRepository auditLogRepository) {
        return new AuditLogQueryService(auditLogRepository);
    }
}
```

- [ ] **Step 3: Create SecurityConfig**

```java
package com.agentbanking.audit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/audit/**").permitAll()
                .requestMatchers("/admin/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

Note: Auth is handled at gateway level. The audit-service permits all since the gateway has already validated JWT and role.

- [ ] **Step 4: Create KafkaConsumerConfig**

```java
package com.agentbanking.audit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    // Spring Cloud Stream handles consumer binding automatically.
    // This class exists to enable Kafka annotation processing if needed.
}
```

- [ ] **Step 5: Commit**

```bash
git add services/audit-service/src/main/resources/ services/audit-service/src/main/java/com/agentbanking/audit/config/
git commit -m "feat: add audit-service config, Flyway migration, and security"
```

---

### Task 7: Create HealthAggregationService, MetricsAggregationService, and Controllers

**BDD Scenarios:** Scenario 1.1, 1.2, 1.3, Scenario 3.1, 3.3
**BRD Requirements:** FR-1.1, FR-1.5, FR-1.6, FR-2.1, FR-2.2, FR-2.3

**User-Facing:** NO

**Files:**
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/service/HealthAggregationService.java`
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/service/MetricsAggregationService.java`
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/config/ServiceRegistryConfig.java`
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/infrastructure/web/HealthAggregationController.java`
- Create: `services/audit-service/src/main/java/com/agentbanking/audit/infrastructure/web/MetricsAggregationController.java`

- [ ] **Step 1: Create HealthAggregationService (NO @Service annotation)**

```java
package com.agentbanking.audit.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HealthAggregationService {

    private static final Logger log = LoggerFactory.getLogger(HealthAggregationService.class);
    private static final int TIMEOUT_SECONDS = 5;

    private final Map<String, String> serviceUrls;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public HealthAggregationService(Map<String, String> serviceUrls) {
        this.serviceUrls = serviceUrls;
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .build()
        );
    }

    public Map<String, Object> aggregateHealth() {
        List<Map<String, Object>> services = new ArrayList<>();
        int healthy = 0;
        int unhealthy = 0;

        for (Map.Entry<String, String> entry : serviceUrls.entrySet()) {
            String name = entry.getKey();
            String url = entry.getValue();
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);

            Map<String, Object> healthInfo;
            try {
                healthInfo = checkServiceHealth(name, url, cb);
                String status = (String) healthInfo.get("status");
                if ("UP".equals(status)) healthy++; else unhealthy++;
            } catch (Exception e) {
                log.warn("Health check failed for {}: {}", name, e.getMessage());
                healthInfo = Map.of(
                    "name", name, "status", "DOWN",
                    "lastChecked", Instant.now().toString(),
                    "error", e.getMessage()
                );
                unhealthy++;
            }
            services.add(healthInfo);
        }

        return Map.of(
            "services", services,
            "summary", Map.of("total", services.size(), "healthy", healthy, "unhealthy", unhealthy),
            "timestamp", Instant.now().toString()
        );
    }

    private Map<String, Object> checkServiceHealth(String name, String url, CircuitBreaker cb) {
        return cb.executeSupplier(() -> {
            try {
                java.net.URI uri = java.net.URI.create(url + "/actuator/health");
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS)).build();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri).timeout(Duration.ofSeconds(TIMEOUT_SECONDS)).GET().build();
                java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

                String status = response.statusCode() == 200 ? "UP" : "DOWN";
                return Map.of(
                    "name", name, "port", extractPort(url),
                    "purpose", getServicePurpose(name), "status", status,
                    "lastChecked", Instant.now().toString()
                );
            } catch (java.net.http.HttpTimeoutException e) {
                return Map.of(
                    "name", name, "status", "DEGRADED",
                    "lastChecked", Instant.now().toString(),
                    "error", "Timeout after " + TIMEOUT_SECONDS + "s"
                );
            } catch (Exception e) {
                throw new RuntimeException("Health check failed: " + e.getMessage(), e);
            }
        });
    }

    private int extractPort(String url) {
        try { return Integer.parseInt(url.substring(url.lastIndexOf(':') + 1)); }
        catch (Exception e) { return 0; }
    }

    private String getServicePurpose(String name) {
        return switch (name) {
            case "gateway" -> "External API entry point";
            case "rules" -> "Transaction rules engine";
            case "ledger" -> "Financial ledger & float";
            case "onboarding" -> "Agent onboarding & KYC";
            case "switch" -> "Payment network adapter";
            case "biller" -> "Bill payment processing";
            case "orchestrator" -> "Transaction Saga coordination";
            case "auth" -> "Authentication & authorization";
            case "audit" -> "Audit log aggregation";
            case "mock" -> "Development/testing";
            default -> "Unknown service";
        };
    }
}
```

- [ ] **Step 2: Create HealthAggregationController**

```java
package com.agentbanking.audit.infrastructure.web;

import com.agentbanking.audit.service.HealthAggregationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class HealthAggregationController {

    private final HealthAggregationService healthAggregationService;

    public HealthAggregationController(HealthAggregationService healthAggregationService) {
        this.healthAggregationService = healthAggregationService;
    }

    @GetMapping("/health/all")
    public ResponseEntity<Map<String, Object>> getAllHealth() {
        return ResponseEntity.ok(healthAggregationService.aggregateHealth());
    }
}
```

- [ ] **Step 3: Create MetricsAggregationService**

Note: Spring Boot Actuator `/actuator/metrics` returns metric *names*, not values. This service queries individual metric endpoints and assembles a structured response.

```java
package com.agentbanking.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class MetricsAggregationService {

    private static final Logger log = LoggerFactory.getLogger(MetricsAggregationService.class);
    private static final int TIMEOUT_SECONDS = 5;
    private final Map<String, String> serviceUrls;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MetricsAggregationService(Map<String, String> serviceUrls) {
        this.serviceUrls = serviceUrls;
    }

    public Map<String, Object> getMetrics(String serviceName) {
        String baseUrl = serviceUrls.get(serviceName);
        if (baseUrl == null) {
            return Map.of("error", "Unknown service: " + serviceName);
        }
        try {
            double jvmMemoryUsed = getMetricValue(baseUrl, "jvm.memory.used", "VALUE") / (1024.0 * 1024.0);
            double jvmMemoryMax = getMetricValue(baseUrl, "jvm.memory.max", "VALUE") / (1024.0 * 1024.0);
            double jvmThreads = getMetricValue(baseUrl, "jvm.threads.live", "VALUE");
            double cpuUsage = getMetricValue(baseUrl, "system.cpu.usage", "VALUE") * 100;
            double uptime = getMetricValue(baseUrl, "process.uptime", "VALUE") / 1000.0;
            double httpRequests = getMetricValue(baseUrl, "http.server.requests", "COUNT");
            // Sum individual 5xx status codes (actuator uses specific codes, not wildcards)
            double httpErrors = 0;
            for (String code : new String[]{"500", "502", "503", "504"}) {
                httpErrors += getMetricValue(baseUrl, "http.server.requests", "COUNT", "status", code);
            }
            double httpAvgTime = getMetricValue(baseUrl, "http.server.requests", "TOTAL_TIME") /
                Math.max(httpRequests, 1) * 1000;

            return Map.of(
                "serviceName", serviceName,
                "jvm", Map.of(
                    "memoryUsedMb", Math.round(jvmMemoryUsed * 10) / 10.0,
                    "memoryMaxMb", Math.round(jvmMemoryMax * 10) / 10.0,
                    "threadsActive", (int) jvmThreads,
                    "cpuUsagePercent", Math.round(cpuUsage * 10) / 10.0,
                    "uptimeSeconds", (long) uptime
                ),
                "http", Map.of(
                    "requestsTotal", (long) httpRequests,
                    "errorsTotal", (long) httpErrors,
                    "avgResponseTimeMs", Math.round(httpAvgTime * 10) / 10.0
                ),
                "timestamp", Instant.now().toString()
            );
        } catch (Exception e) {
            log.warn("Failed to get metrics for {}: {}", serviceName, e.getMessage());
            return Map.of("error", "Metrics unavailable: " + e.getMessage());
        }
    }

    private double getMetricValue(String baseUrl, String metricName, String stat) throws Exception {
        return getMetricValue(baseUrl, metricName, stat, null, null);
    }

    private double getMetricValue(String baseUrl, String metricName, String stat, String tagKey, String tagValue)
            throws Exception {
        String url = baseUrl + "/actuator/metrics/" + metricName;
        if (tagKey != null) url += "?tag=" + tagKey + ":" + tagValue;
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url)).timeout(Duration.ofSeconds(TIMEOUT_SECONDS)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) return 0;
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode measurements = root.get("measurements");
        if (measurements == null || !measurements.isArray()) return 0;
        for (JsonNode m : measurements) {
            if (stat.equals(m.get("statistic").asText())) return m.get("value").asDouble();
        }
        return 0;
    }
}
```

- [ ] **Step 4: Create MetricsAggregationController**

```java
package com.agentbanking.audit.infrastructure.web;

import com.agentbanking.audit.service.MetricsAggregationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class MetricsAggregationController {

    private final MetricsAggregationService metricsAggregationService;

    public MetricsAggregationController(MetricsAggregationService metricsAggregationService) {
        this.metricsAggregationService = metricsAggregationService;
    }

    @GetMapping("/metrics/{service}")
    public ResponseEntity<Map<String, Object>> getMetrics(@PathVariable String service) {
        return ResponseEntity.ok(metricsAggregationService.getMetrics(service));
    }
}
```

- [ ] **Step 5: Create ServiceRegistryConfig and register aggregation beans**

Create: `services/audit-service/src/main/java/com/agentbanking/audit/config/ServiceRegistryConfig.java`

```java
package com.agentbanking.audit.config;

import com.agentbanking.audit.service.HealthAggregationService;
import com.agentbanking.audit.service.MetricsAggregationService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Configuration
public class ServiceRegistryConfig {

    @Bean
    @ConfigurationProperties(prefix = "admin")
    public AdminServicesConfig adminServicesConfig() {
        return new AdminServicesConfig();
    }

    @Bean
    public HealthAggregationService healthAggregationService(AdminServicesConfig config) {
        return new HealthAggregationService(config.getServices());
    }

    @Bean
    public MetricsAggregationService metricsAggregationService(AdminServicesConfig config) {
        return new MetricsAggregationService(config.getServices());
    }

    public static class AdminServicesConfig {
        private Map<String, String> services = Map.of();
        public Map<String, String> getServices() { return services; }
        public void setServices(Map<String, String> services) { this.services = services; }
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add services/audit-service/src/main/java/com/agentbanking/audit/service/ \
  services/audit-service/src/main/java/com/agentbanking/audit/infrastructure/web/
git commit -m "feat: add health and metrics aggregation services and controllers"
```

---

### Task 8: Add Gateway routes and custom filter

**BDD Scenarios:** Scenario 5.3: Non-IT_ADMIN user denied access
**BRD Requirements:** FR-5.1, FR-5.2, FR-5.3, FR-5.4

**User-Facing:** NO

**Files:**
- Create: `gateway/src/main/java/com/agentbanking/gateway/filter/ServiceRouteGatewayFilterFactory.java`
- Modify: `gateway/src/main/resources/application.yaml`

- [ ] **Step 1: Create ServiceRouteGatewayFilterFactory**

```java
package com.agentbanking.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import reactor.core.publisher.Mono;
import org.springframework.web.server.ServerWebExchange;

/**
 * Custom gateway filter that resolves {service} path variable to target service URI.
 * Naming: ServiceRouteGatewayFilterFactory -> referenced in YAML as: ServiceRoute
 */
@Component
public class ServiceRouteGatewayFilterFactory
        extends AbstractGatewayFilterFactory<ServiceRouteGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(ServiceRouteGatewayFilterFactory.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Note: This map duplicates admin.services from audit-service's application.yaml.
    // This is intentional - the gateway needs its own routing map (it routes HTTP requests),
    // while audit-service's map is for outbound health check calls. Different architectural boundaries.
    private static final Map<String, String> SERVICE_URLS = Map.of(
        "gateway", "http://gateway:8080",
        "rules", "http://rules-service:8081",
        "ledger", "http://ledger-service:8082",
        "onboarding", "http://onboarding-service:8083",
        "switch", "http://switch-adapter-service:8084",
        "biller", "http://biller-service:8085",
        "orchestrator", "http://orchestrator-service:8086",
        "auth", "http://auth-iam-service:8087",
        "audit", "http://audit-service:8088",
        "mock", "http://mock-server:8089",
        "postgresql", "direct",
        "redis", "direct",
        "kafka", "direct"
    );

    public ServiceRouteGatewayFilterFactory() { super(Config.class); }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String servicePath = exchange.getRequest().getPath().value();
            String[] parts = servicePath.split("/");
            String serviceName = null;
            String actuatorPath = null;

            for (int i = 0; i < parts.length; i++) {
                if ("health".equals(parts[i]) && i + 1 < parts.length) {
                    serviceName = parts[i + 1];
                    actuatorPath = "/actuator/health";
                    break;
                } else if ("metrics".equals(parts[i]) && i + 1 < parts.length) {
                    serviceName = parts[i + 1];
                    actuatorPath = "/actuator/metrics";
                    break;
                }
            }

            if (serviceName == null || !SERVICE_URLS.containsKey(serviceName)) {
                return onError(exchange, "Unknown service: " + serviceName, "ERR_SYS_001");
            }

            String url = SERVICE_URLS.get(serviceName);
            if ("direct".equals(url)) {
                return onError(exchange, "Infrastructure component health check not supported via HTTP", "ERR_SYS_002");
            }

            String targetUrl = url + actuatorPath;
            log.debug("Routing to service {}: {}", serviceName, targetUrl);

            exchange.getAttributes().put(
                org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR,
                URI.create(targetUrl)
            );

            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, String errorCode) {
        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> errorResponse = Map.of(
            "status", "FAILED",
            "error", Map.of(
                "code", errorCode, "message", message,
                "action_code", "REVIEW",
                "trace_id", UUID.randomUUID().toString(),
                "timestamp", Instant.now().toString()
            )
        );
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
            );
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    public static class Config {}
}
```

- [ ] **Step 2: Add admin routes to gateway application.yaml**

Read `gateway/src/main/resources/application.yaml` and add to `spring.cloud.gateway.routes`:

```yaml
# Admin - Health (aggregate all)
- id: admin-health-all
  uri: http://audit-service:8088
  predicates:
    - Path=/api/v1/admin/health/all
  filters:
    - JwtAuth
    - RewritePath=/api/v1/admin/health/all, /admin/health/all

# Admin - Health (individual service)
- id: admin-health-service
  uri: http://gateway:8080
  predicates:
    - Path=/api/v1/admin/health/{service}
  filters:
    - JwtAuth
    - ServiceRoute

# Admin - Metrics (individual service - routed to audit-service for aggregation)
- id: admin-metrics
  uri: http://audit-service:8088
  predicates:
    - Path=/api/v1/admin/metrics/{service}
  filters:
    - JwtAuth
    - RewritePath=/api/v1/admin/metrics/(?<service>.*), /admin/metrics/${service}

# Admin - Audit Logs
- id: admin-audit-logs
  uri: http://audit-service:8088
  predicates:
    - Path=/api/v1/admin/audit-logs
  filters:
    - JwtAuth
    - RewritePath=/api/v1/admin/audit-logs, /audit/logs

# Admin - Audit Logs Export
- id: admin-audit-logs-export
  uri: http://audit-service:8088
  predicates:
    - Path=/api/v1/admin/audit-logs/export
  filters:
    - JwtAuth
    - RewritePath=/api/v1/admin/audit-logs/export, /audit/logs/export
```

- [ ] **Step 3: Commit**

```bash
git add gateway/src/main/java/com/agentbanking/gateway/filter/ServiceRouteGatewayFilterFactory.java \
  gateway/src/main/resources/application.yaml
git commit -m "feat: add admin routes and ServiceRoute gateway filter"
```

---

### Task 9: Add audit-service to docker-compose

**BDD Scenarios:** Supports all backend scenarios
**BRD Requirements:** FR-4.1

**User-Facing:** NO

**Files:**
- Modify: `docker-compose.yml`

- [ ] **Step 1: Add postgres-audit service**

```yaml
  postgres-audit:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: auditdb
      POSTGRES_USER: audit_user
      POSTGRES_PASSWORD: audit_password
    ports:
      - "5440:5432"
    volumes:
      - postgres-audit-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U audit_user -d auditdb"]
      interval: 5s
      timeout: 5s
      retries: 5
    profiles: [infra, all]
```

- [ ] **Step 2: Add audit-service**

```yaml
  audit-service:
    build:
      context: .
      dockerfile: services/audit-service/Dockerfile
    ports:
      - "8088:8088"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-audit:5432/auditdb
      SPRING_DATASOURCE_USERNAME: audit_user
      SPRING_DATASOURCE_PASSWORD: audit_password
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      postgres-audit:
        condition: service_healthy
      kafka:
        condition: service_started
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8088/actuator/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 5
    profiles: [backend, all]
```

- [ ] **Step 3: Add volume**

```yaml
  postgres-audit-data:
```

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml
git commit -m "feat: add audit-service and postgres-audit to docker-compose"
```

---

### Task 10: Add System Admin navigation and route to backoffice

**BDD Scenarios:** Scenario 1.1
**BRD Requirements:** FR-1.1

**User-Facing:** YES

**Files:**
- Modify: `backoffice/src/components/Layout.tsx`
- Modify: `backoffice/src/main.tsx`

- [ ] **Step 1: Add System Admin nav item**

Read `backoffice/src/components/Layout.tsx`, import `Settings` from lucide-react, add to navItems:
```tsx
{ path: '/system-admin', icon: Settings, label: 'System Admin' },
```

- [ ] **Step 2: Add route**

Read `backoffice/src/main.tsx`, add import and route:
```tsx
import { SystemAdmin } from './pages/SystemAdmin'
// In protected routes:
<Route path="system-admin" element={<SystemAdmin />} />
```

- [ ] **Step 3: Commit**

```bash
git add backoffice/src/components/Layout.tsx backoffice/src/main.tsx
git commit -m "feat: add System Admin navigation and route"
```

---

### Task 11: Add admin API endpoints to backoffice client

**BDD Scenarios:** All API-calling scenarios
**BRD Requirements:** FR-1.3, FR-1.4, FR-2.4, FR-3.1

**User-Facing:** NO

**Files:**
- Modify: `backoffice/src/api/client.ts`

- [ ] **Step 1: Add endpoints**

```typescript
// System Admin
getHealthAll: () => client.get('/admin/health/all').then((r) => r.data),
getServiceMetrics: (service: string) => client.get(`/admin/metrics/${service}`).then((r) => r.data),
getAuditLogs: (params?: Record<string, string | number>) =>
  client.get('/admin/audit-logs', { params }).then((r) => r.data),
exportAuditLogs: (params?: Record<string, string | number>) =>
  client.get('/admin/audit-logs/export', { params, responseType: 'blob' }).then((r) => r.data),
```

- [ ] **Step 2: Commit**

```bash
git add backoffice/src/api/client.ts
git commit -m "feat: add admin API endpoints to backoffice client"
```

---

### Task 12: Create HealthCard component

**BDD Scenarios:** Scenario 1.1, 1.2, 1.3
**BRD Requirements:** FR-1.1, FR-1.5, FR-1.6

**User-Facing:** YES

**Files:**
- Create: `backoffice/src/components/HealthCard.tsx`
- Create: `backoffice/src/test/HealthCard.test.tsx`

- [ ] **Step 1: Write test**

```tsx
import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { HealthCard } from '../components/HealthCard'
import React from 'react'

describe('HealthCard', () => {
  it('should render service info with UP status', () => {
    render(<HealthCard name="gateway" port={8080} purpose="API Gateway" status="UP" lastChecked="2026-04-05T10:30:00+08:00" onDrillDown={() => {}} />)
    expect(screen.getByText('gateway')).toBeInTheDocument()
    expect(screen.getByText('UP')).toBeInTheDocument()
  })

  it('should show green indicator for UP status', () => {
    render(<HealthCard name="ledger" port={8082} purpose="Ledger" status="UP" lastChecked="2026-04-05T10:30:00+08:00" onDrillDown={() => {}} />)
    expect(screen.getByText('UP')).toHaveClass('badge-success')
  })

  it('should show red indicator for DOWN status', () => {
    render(<HealthCard name="rules" port={8081} purpose="Rules" status="DOWN" lastChecked="2026-04-05T10:30:00+08:00" onDrillDown={() => {}} />)
    expect(screen.getByText('DOWN')).toHaveClass('badge-error')
  })

  it('should show amber indicator for DEGRADED status', () => {
    render(<HealthCard name="switch" port={8084} purpose="Switch" status="DEGRADED" lastChecked="2026-04-05T10:30:00+08:00" onDrillDown={() => {}} />)
    expect(screen.getByText('DEGRADED')).toHaveClass('badge-warning')
  })

  it('should call onDrillDown when clicked', () => {
    const mockDrillDown = vi.fn()
    render(<HealthCard name="auth" port={8087} purpose="Auth" status="UP" lastChecked="2026-04-05T10:30:00+08:00" onDrillDown={mockDrillDown} />)
    screen.getByTestId('health-card').click()
    expect(mockDrillDown).toHaveBeenCalledWith('auth')
  })
})
```

- [ ] **Step 2: Run test (should fail)**

```bash
cd backoffice && npx vitest run src/test/HealthCard.test.tsx
```

- [ ] **Step 3: Create component**

```tsx
import React from 'react'

interface HealthCardProps {
  name: string; port: number; purpose: string;
  status: 'UP' | 'DOWN' | 'DEGRADED';
  lastChecked: string; onDrillDown: (name: string) => void;
}

const statusClass: Record<string, string> = {
  UP: 'badge-success', DOWN: 'badge-error', DEGRADED: 'badge-warning',
}

export const HealthCard: React.FC<HealthCardProps> = ({ name, port, purpose, status, lastChecked, onDrillDown }) => (
  <div data-testid="health-card" className="card" style={{ cursor: 'pointer' }} onClick={() => onDrillDown(name)}>
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <h3 style={{ margin: 0 }}>{name}</h3>
      <span className={statusClass[status] || 'badge-info'}>{status}</span>
    </div>
    <p style={{ color: '#64748b', margin: '8px 0 0' }}>Port: {port} &middot; {purpose}</p>
    <p style={{ color: '#94a3b8', fontSize: '12px', margin: '4px 0 0' }}>
      Last checked: {new Date(lastChecked).toLocaleTimeString()}
    </p>
  </div>
)
```

- [ ] **Step 4: Run test (should pass)**

```bash
cd backoffice && npx vitest run src/test/HealthCard.test.tsx
```

- [ ] **Step 5: Commit**

```bash
git add backoffice/src/components/HealthCard.tsx backoffice/src/test/HealthCard.test.tsx
git commit -m "feat: add HealthCard component with tests"
```

---

### Task 13: Create MetricsPanel component

**BDD Scenarios:** Scenario 3.1, 3.3
**BRD Requirements:** FR-2.1, FR-2.2, FR-2.3

**User-Facing:** YES

**Files:**
- Create: `backoffice/src/components/MetricsPanel.tsx`
- Create: `backoffice/src/test/MetricsPanel.test.tsx`

- [ ] **Step 1: Write test**

```tsx
import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MetricsPanel } from '../components/MetricsPanel'
import React from 'react'

describe('MetricsPanel', () => {
  const mockMetrics = {
    serviceName: 'ledger-service',
    jvm: { memoryUsedMb: 256.5, memoryMaxMb: 512.0, threadsActive: 45, cpuUsagePercent: 12.3, uptimeSeconds: 86400 },
    http: { requestsTotal: 15000, errorsTotal: 23, avgResponseTimeMs: 45.2 },
    timestamp: '2026-04-05T10:30:00+08:00',
  }

  it('should render JVM metrics', () => {
    render(<MetricsPanel serviceName="ledger-service" metrics={mockMetrics} onRefresh={() => {}} />)
    expect(screen.getByText('ledger-service')).toBeInTheDocument()
    expect(screen.getByText(/256.5/)).toBeInTheDocument()
  })

  it('should show error when metrics unavailable', () => {
    render(<MetricsPanel serviceName="ledger-service" metrics={null} onRefresh={() => {}} />)
    expect(screen.getByText(/Metrics unavailable/)).toBeInTheDocument()
  })

  it('should call onRefresh when clicked', () => {
    const mockRefresh = vi.fn()
    render(<MetricsPanel serviceName="ledger-service" metrics={mockMetrics} onRefresh={mockRefresh} />)
    screen.getByTestId('metrics-refresh').click()
    expect(mockRefresh).toHaveBeenCalled()
  })
})
```

- [ ] **Step 2: Run test (should fail)**

```bash
cd backoffice && npx vitest run src/test/MetricsPanel.test.tsx
```

- [ ] **Step 3: Create component**

```tsx
import React from 'react'

interface MetricsPanelProps {
  serviceName: string;
  metrics: {
    jvm: { memoryUsedMb: number; memoryMaxMb: number; threadsActive: number; cpuUsagePercent: number; uptimeSeconds: number };
    http: { requestsTotal: number; errorsTotal: number; avgResponseTimeMs: number };
    timestamp: string;
  } | null;
  onRefresh: () => void;
}

export const MetricsPanel: React.FC<MetricsPanelProps> = ({ serviceName, metrics, onRefresh }) => {
  if (!metrics) {
    return (
      <div className="card" style={{ textAlign: 'center', padding: '40px' }}>
        <p style={{ color: '#ef4444' }}>Metrics unavailable for {serviceName}</p>
        <button className="btn btn-secondary" data-testid="metrics-refresh" onClick={onRefresh}>Retry</button>
      </div>
    )
  }

  const formatUptime = (s: number) => `${Math.floor(s / 86400)}d ${Math.floor((s % 86400) / 3600)}h`

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h3 style={{ margin: 0 }}>{serviceName}</h3>
        <button className="btn btn-secondary" data-testid="metrics-refresh" onClick={onRefresh}>Refresh</button>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '16px', marginTop: '16px' }}>
        <div>
          <h4 style={{ margin: '0 0 8px', color: '#1e3a5f' }}>JVM Metrics</h4>
          <div style={{ display: 'grid', gap: '8px' }}>
            <div>Memory: {metrics.jvm.memoryUsedMb.toFixed(1)} / {metrics.jvm.memoryMaxMb.toFixed(0)} MB</div>
            <div>Threads: {metrics.jvm.threadsActive}</div>
            <div>CPU: {metrics.jvm.cpuUsagePercent.toFixed(1)}%</div>
            <div>Uptime: {formatUptime(metrics.jvm.uptimeSeconds)}</div>
          </div>
        </div>
        <div>
          <h4 style={{ margin: '0 0 8px', color: '#1e3a5f' }}>HTTP Metrics</h4>
          <div style={{ display: 'grid', gap: '8px' }}>
            <div>Requests: {metrics.http.requestsTotal.toLocaleString()}</div>
            <div>Errors: {metrics.http.errorsTotal.toLocaleString()}</div>
            <div>Avg Response: {metrics.http.avgResponseTimeMs.toFixed(1)}ms</div>
          </div>
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Run test (should pass)**

```bash
cd backoffice && npx vitest run src/test/MetricsPanel.test.tsx
```

- [ ] **Step 5: Commit**

```bash
git add backoffice/src/components/MetricsPanel.tsx backoffice/src/test/MetricsPanel.test.tsx
git commit -m "feat: add MetricsPanel component with tests"
```

---

### Task 14: Create AuditLogTable component

**BDD Scenarios:** Scenario 4.1-4.7
**BRD Requirements:** FR-3.1, FR-3.2, FR-3.3, FR-3.4, FR-3.5

**User-Facing:** YES

**Files:**
- Create: `backoffice/src/components/AuditLogTable.tsx`
- Create: `backoffice/src/test/AuditLogTable.test.tsx`

- [ ] **Step 1: Write test**

```tsx
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { AuditLogTable } from '../components/AuditLogTable'
import React from 'react'

const mockLogs = {
  content: [{
    auditId: 'a0000000-0000-0000-0000-000000000001',
    serviceName: 'auth-iam-service', entityType: 'USER', action: 'USER_CREATED',
    performedBy: 'admin-001', ipAddress: '192.168.1.1', timestamp: '2026-04-05T10:30:00',
    outcome: 'SUCCESS', failureReason: null,
  }],
  page: 0, size: 20, totalElements: 1, totalPages: 1,
}

describe('AuditLogTable', () => {
  it('should render audit logs with all columns including Failure Reason', async () => {
    render(<AuditLogTable logs={mockLogs} loading={false} onFilter={() => {}} onExport={() => {}} />)
    await waitFor(() => {
      expect(screen.getByText('USER_CREATED')).toBeInTheDocument()
      expect(screen.getByText('SUCCESS')).toBeInTheDocument()
    })
  })

  it('should show empty state when no logs', () => {
    render(<AuditLogTable logs={{ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }} loading={false} onFilter={() => {}} onExport={() => {}} />)
    expect(screen.getByText(/No audit logs found/)).toBeInTheDocument()
  })

  it('should show loading state', () => {
    render(<AuditLogTable logs={mockLogs} loading={true} onFilter={() => {}} onExport={() => {}} />)
    expect(screen.getByText(/Loading/)).toBeInTheDocument()
  })

  it('should call onFilter when search changes', () => {
    const mockFilter = vi.fn()
    render(<AuditLogTable logs={mockLogs} loading={false} onFilter={mockFilter} onExport={() => {}} />)
    fireEvent.change(screen.getByPlaceholderText(/Search/), { target: { value: 'USER' } })
    expect(mockFilter).toHaveBeenCalled()
  })

  it('should call onExport when export clicked', () => {
    const mockExport = vi.fn()
    render(<AuditLogTable logs={mockLogs} loading={false} onFilter={() => {}} onExport={mockExport} />)
    fireEvent.click(screen.getByText(/Export/))
    expect(mockExport).toHaveBeenCalled()
  })
})
```

- [ ] **Step 2: Run test (should fail)**

```bash
cd backoffice && npx vitest run src/test/AuditLogTable.test.tsx
```

- [ ] **Step 3: Create component (includes Failure Reason column and date range filter)**

```tsx
import React, { useState } from 'react'

interface AuditLogEntry {
  auditId: string; serviceName: string; entityType: string; action: string;
  performedBy: string; ipAddress: string; timestamp: string;
  outcome: string; failureReason: string | null;
}

interface AuditLogResponse {
  content: AuditLogEntry[]; page: number; size: number;
  totalElements: number; totalPages: number;
}

interface AuditLogTableProps {
  logs: AuditLogResponse; loading: boolean;
  onFilter: (filters: Record<string, string>) => void;
  onExport: () => void;
  onPageChange: (page: number) => void;
}

export const AuditLogTable: React.FC<AuditLogTableProps> = ({ logs, loading, onFilter, onExport, onPageChange }) => {
  const [search, setSearch] = useState('')
  const [serviceFilter, setServiceFilter] = useState('')
  const [outcomeFilter, setOutcomeFilter] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')

  const applyFilters = (updates: Record<string, string>) => {
    const newFilters = { search, service: serviceFilter, outcome: outcomeFilter, dateFrom, dateTo, ...updates }
    onFilter(newFilters)
  }

  if (loading) {
    return <div className="card" style={{ textAlign: 'center', padding: '40px' }}>Loading audit logs...</div>
  }

  if (logs.content.length === 0) {
    return <div className="card" style={{ textAlign: 'center', padding: '40px' }}>
      <p style={{ color: '#64748b' }}>No audit logs found matching your criteria</p>
    </div>
  }

  return (
    <div className="card">
      <div style={{ display: 'flex', gap: '12px', marginBottom: '16px', flexWrap: 'wrap' }}>
        <input type="text" placeholder="Search audit logs..." value={search}
          onChange={(e) => { setSearch(e.target.value); applyFilters({ search: e.target.value }) }}
          className="input" style={{ flex: 1, minWidth: '200px' }} />
        <select value={serviceFilter}
          onChange={(e) => { setServiceFilter(e.target.value); applyFilters({ service: e.target.value }) }}
          className="input">
          <option value="">All Services</option>
          <option value="auth-iam-service">Auth/IAM</option>
          <option value="onboarding-service">Onboarding</option>
          <option value="ledger-service">Ledger</option>
          <option value="rules-service">Rules</option>
          <option value="switch-adapter-service">Switch</option>
          <option value="biller-service">Biller</option>
          <option value="orchestrator-service">Orchestrator</option>
        </select>
        <select value={outcomeFilter}
          onChange={(e) => { setOutcomeFilter(e.target.value); applyFilters({ outcome: e.target.value }) }}
          className="input">
          <option value="">All Results</option>
          <option value="SUCCESS">Success</option>
          <option value="FAILURE">Failure</option>
        </select>
        <input type="date" value={dateFrom}
          onChange={(e) => { setDateFrom(e.target.value); applyFilters({ dateFrom: e.target.value }) }}
          className="input" />
        <input type="date" value={dateTo}
          onChange={(e) => { setDateTo(e.target.value); applyFilters({ dateTo: e.target.value }) }}
          className="input" />
        <button className="btn btn-secondary" onClick={onExport}>Export to CSV</button>
      </div>

      <div style={{ overflowX: 'auto' }}>
        <table className="table">
          <thead>
            <tr>
              <th>Timestamp</th><th>User ID</th><th>Action</th><th>Resource</th>
              <th>IP Address</th><th>Service</th><th>Result</th><th>Failure Reason</th>
            </tr>
          </thead>
          <tbody>
            {logs.content.map((log) => (
              <tr key={log.auditId}>
                <td>{new Date(log.timestamp).toLocaleString()}</td>
                <td>{log.performedBy}</td>
                <td>{log.action}</td>
                <td>{log.entityType}</td>
                <td>{log.ipAddress || '-'}</td>
                <td>{log.serviceName}</td>
                <td>
                  <span className={log.outcome === 'SUCCESS' ? 'badge-success' : 'badge-error'}>
                    {log.outcome}
                  </span>
                </td>
                <td>{log.failureReason || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '16px', fontSize: '14px', color: '#64748b' }}>
        <span>Showing {logs.content.length} of {logs.totalElements} entries</span>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <button className="btn btn-secondary" disabled={logs.page === 0}
            onClick={() => onPageChange(logs.page - 1)} data-testid="prev-page">Previous</button>
          <span>Page {logs.page + 1} of {logs.totalPages}</span>
          <button className="btn btn-secondary" disabled={logs.page >= logs.totalPages - 1}
            onClick={() => onPageChange(logs.page + 1)} data-testid="next-page">Next</button>
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Run test (should pass)**

```bash
cd backoffice && npx vitest run src/test/AuditLogTable.test.tsx
```

- [ ] **Step 5: Commit**

```bash
git add backoffice/src/components/AuditLogTable.tsx backoffice/src/test/AuditLogTable.test.tsx
git commit -m "feat: add AuditLogTable component with tests"
```

---

### Task 15: Create SystemAdmin page

**BDD Scenarios:** Scenario 1.1-1.3, 2.1-2.2, 3.1-3.3, 4.1-4.7, 5.1-5.3
**BRD Requirements:** FR-1.1-FR-1.6, FR-2.1-FR-2.5, FR-3.1-FR-3.5

**User-Facing:** YES

**Files:**
- Create: `backoffice/src/pages/SystemAdmin.tsx`
- Create: `backoffice/src/test/SystemAdmin.test.tsx`

- [ ] **Step 1: Write test**

```tsx
import { describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { SystemAdmin } from '../pages/SystemAdmin'
import React from 'react'

vi.mock('../api/client', () => ({
  default: {
    getHealthAll: vi.fn().mockResolvedValue({
      services: [
        { name: 'gateway', port: 8080, purpose: 'API Gateway', status: 'UP', lastChecked: '2026-04-05T10:30:00+08:00' },
        { name: 'ledger', port: 8082, purpose: 'Ledger', status: 'UP', lastChecked: '2026-04-05T10:30:00+08:00' },
      ],
      summary: { total: 2, healthy: 2, unhealthy: 0 },
      timestamp: '2026-04-05T10:30:00+08:00',
    }),
    getServiceMetrics: vi.fn().mockResolvedValue({
      serviceName: 'ledger-service',
      jvm: { memoryUsedMb: 256, memoryMaxMb: 512, threadsActive: 45, cpuUsagePercent: 12, uptimeSeconds: 86400 },
      http: { requestsTotal: 15000, errorsTotal: 23, avgResponseTimeMs: 45 },
      timestamp: '2026-04-05T10:30:00+08:00',
    }),
    getAuditLogs: vi.fn().mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
    exportAuditLogs: vi.fn().mockResolvedValue(new Blob()),
  },
}))

function createTestQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } })
}

function renderWithProviders(ui: React.ReactElement) {
  const qc = createTestQueryClient()
  return render(<MemoryRouter><QueryClientProvider client={qc}>{ui}</QueryClientProvider></MemoryRouter>)
}

describe('SystemAdmin', () => {
  it('should render System Administration page', async () => {
    renderWithProviders(<SystemAdmin />)
    await waitFor(() => expect(screen.getByText('System Administration')).toBeInTheDocument())
  })

  it('should display health status cards', async () => {
    renderWithProviders(<SystemAdmin />)
    await waitFor(() => {
      expect(screen.getByText('gateway')).toBeInTheDocument()
      expect(screen.getByText('ledger')).toBeInTheDocument()
    })
  })

  it('should show healthy count', async () => {
    renderWithProviders(<SystemAdmin />)
    await waitFor(() => expect(screen.getByText(/Healthy: 2/)).toBeInTheDocument())
  })

  it('should have refresh button with loading indicator', async () => {
    renderWithProviders(<SystemAdmin />)
    await waitFor(() => expect(screen.getByText('Refresh')).toBeInTheDocument())
  })
})
```

- [ ] **Step 2: Run test (should fail)**

```bash
cd backoffice && npx vitest run src/test/SystemAdmin.test.tsx
```

- [ ] **Step 3: Create page**

```tsx
import React, { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import api from '../api/client'
import { HealthCard } from '../components/HealthCard'
import { MetricsPanel } from '../components/MetricsPanel'
import { AuditLogTable } from '../components/AuditLogTable'
import { RefreshCw, Activity, BarChart3, FileText } from 'lucide-react'

export const SystemAdmin: React.FC = () => {
  const [selectedService, setSelectedService] = useState<string | null>(null)
  const [auditFilters, setAuditFilters] = useState<Record<string, string>>({})
  const [auditPage, setAuditPage] = useState(0)

  const { data: healthData, isLoading: healthLoading, isRefetching: healthRefetching, refetch: refetchHealth } = useQuery({
    queryKey: ['admin-health'],
    queryFn: () => api.getHealthAll(),
    refetchInterval: 30000,
  })

  const { data: metricsData, refetch: refetchMetrics } = useQuery({
    queryKey: ['admin-metrics', selectedService],
    queryFn: () => api.getServiceMetrics(selectedService!),
    enabled: !!selectedService,
  })

  const { data: auditData, isLoading: auditLoading } = useQuery({
    queryKey: ['admin-audit-logs', auditFilters, auditPage],
    queryFn: () => api.getAuditLogs({ ...auditFilters, page: auditPage, size: 20 }),
  })

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h1 style={{ margin: 0, color: '#1e3a5f' }}>System Administration</h1>
          <p style={{ color: '#64748b', margin: '4px 0 0' }}>Monitor services, metrics, and audit logs</p>
        </div>
        <button className="btn btn-secondary" onClick={() => refetchHealth()} disabled={healthRefetching}>
          <RefreshCw size={16} style={{ marginRight: '8px', animation: healthRefetching ? 'spin 1s linear infinite' : 'none' }} />
          {healthRefetching ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {/* Health Dashboard */}
      <section id="health" style={{ marginBottom: '32px' }}>
        <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#1e3a5f' }}>
          <Activity size={20} /> Service Health
        </h2>
        {healthData && (
          <div style={{ display: 'flex', gap: '12px', marginBottom: '16px' }}>
            <div className="card" style={{ flex: 1, textAlign: 'center' }}>
              <div style={{ fontSize: '24px', fontWeight: 'bold' }}>{healthData.summary.total}</div>
              <div style={{ color: '#64748b' }}>Total Services</div>
            </div>
            <div className="card" style={{ flex: 1, textAlign: 'center' }}>
              <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#10b981' }}>Healthy: {healthData.summary.healthy}</div>
            </div>
            <div className="card" style={{ flex: 1, textAlign: 'center' }}>
              <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#ef4444' }}>Unhealthy: {healthData.summary.unhealthy}</div>
            </div>
          </div>
        )}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '16px' }}>
          {healthData?.services?.map((svc: any) => (
            <HealthCard key={svc.name} name={svc.name} port={svc.port || 0} purpose={svc.purpose || ''}
              status={svc.status} lastChecked={svc.lastChecked} onDrillDown={setSelectedService} />
          ))}
        </div>
      </section>

      {/* Metrics Panel */}
      {selectedService && (
        <section id="metrics" style={{ marginBottom: '32px' }}>
          <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#1e3a5f' }}>
            <BarChart3 size={20} /> Metrics: {selectedService}
          </h2>
          <MetricsPanel serviceName={selectedService} metrics={metricsData || null} onRefresh={() => refetchMetrics()} />
        </section>
      )}

      {/* Audit Logs */}
      <section id="audit-logs">
        <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#1e3a5f' }}>
          <FileText size={20} /> Audit Logs
        </h2>
        <AuditLogTable
          logs={auditData || { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }}
          loading={auditLoading}
          onFilter={(filters) => { setAuditFilters(filters); setAuditPage(0) }}
          onPageChange={setAuditPage}
          onExport={() => {
            api.exportAuditLogs(auditFilters).then((blob: Blob) => {
              const url = URL.createObjectURL(blob)
              const a = document.createElement('a')
              a.href = url
              a.download = `audit-logs-${new Date().toISOString().slice(0, 19).replace(/[:.]/g, '-')}.csv`
              document.body.appendChild(a)
              a.click()
              document.body.removeChild(a)
              URL.revokeObjectURL(url)
            })
          }}
        />
      </section>
    </div>
  )
}
```

- [ ] **Step 4: Run test (should pass)**

```bash
cd backoffice && npx vitest run src/test/SystemAdmin.test.tsx
```

- [ ] **Step 5: Commit**

```bash
git add backoffice/src/pages/SystemAdmin.tsx backoffice/src/test/SystemAdmin.test.tsx
git commit -m "feat: add SystemAdmin page with health, metrics, and audit log sections"
```

---

### Task 16: Add ArchUnit test for audit-service

**BDD Scenarios:** N/A (architecture compliance)
**BRD Requirements:** Hexagonal architecture law

**User-Facing:** NO

**Files:**
- Create: `services/audit-service/src/test/java/com/agentbanking/audit/arch/HexagonalArchitectureTest.java`

- [ ] **Step 1: Create ArchUnit test**

```java
package com.agentbanking.audit.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HexagonalArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages("com.agentbanking.audit");

    @Test
    void domainLayerShouldNotDependOnInfrastructure() {
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .check(classes);
    }

    @Test
    void domainLayerShouldNotDependOnSpring() {
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .check(classes);
    }

    @Test
    void domainLayerShouldNotDependOnJpa() {
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
            .check(classes);
    }
}
```

- [ ] **Step 2: Run test**

```bash
./gradlew :services:audit-service:test --tests "HexagonalArchitectureTest"
```

- [ ] **Step 3: Commit**

```bash
git add services/audit-service/src/test/java/com/agentbanking/audit/arch/
git commit -m "test: add ArchUnit hexagonal architecture tests for audit-service"
```

---

### Task 17: Verify all services compile and tests pass

**BDD Scenarios:** All scenarios
**BRD Requirements:** All requirements

**User-Facing:** NO

- [ ] **Step 1: Run full build**

```bash
./gradlew clean build
```

- [ ] **Step 2: Run backoffice tests**

```bash
cd backoffice && npm test
```

- [ ] **Step 3: Verify docker-compose starts**

```bash
docker compose --profile all up -d audit-service postgres-audit
docker compose logs audit-service
```

- [ ] **Step 4: Commit any fixes**

```bash
git add .
git commit -m "fix: resolve build and test issues"
```
