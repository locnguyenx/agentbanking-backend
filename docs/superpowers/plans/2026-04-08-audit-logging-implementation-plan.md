# Audit Logging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add audit logging to all services using common module's AuditLogRecord with comprehensive fields for Bank Malaysia compliance

**Architecture:** Each service implements common's AuditLogRecord, writes to local audit_logs table, exposes /internal/audit-logs endpoint

**Tech Stack:** Spring Boot, JPA, PostgreSQL, common module

---

## Audit Log Fields (Enhanced for Banking Compliance)

| # | Field | Type | Required | Description |
|---|-------|------|----------|-------------|
| 1 | auditId | UUID | ✅ | Unique identifier |
| 2 | entityType | String | ✅ | Type of entity (User, Agent, Transaction) |
| 3 | entityId | UUID | | ID of affected entity |
| 4 | action | AuditAction | ✅ | Action performed |
| 5 | performedBy | String | ✅ | User who performed action |
| 6 | changes | String | | JSON of changed fields |
| 7 | ipAddress | String | | Client IP |
| 8 | timestamp | DateTime | ✅ | When action occurred |
| 9 | outcome | AuditOutcome | ✅ | SUCCESS/FAILURE/PENDING |
| 10 | failureReason | String | | Why action failed |
| 11 | traceId | String | | Distributed tracing ID |
| 12 | sessionId | String | | Session identifier |
| 13 | serviceName | String | ✅ | Source service name |
| 14 | deviceInfo | String | | Device fingerprint/browser |
| 15 | geographicLocation | String | | Geographic location (for fraud detection) |

---

## Prerequisite: Enhance AuditLogRecord in Common Module

### Task 0: Create AuditOutcome enum

**Files:**
- Create: `common/src/main/java/com/agentbanking/common/audit/AuditOutcome.java`

```java
package com.agentbanking.common.audit;

public enum AuditOutcome {
    SUCCESS,
    FAILURE,
    PENDING
}
```

### Task 0b: Enhance AuditLogRecord

**Files:**
- Modify: `common/src/main/java/com/agentbanking/common/audit/AuditLogRecord.java`

```java
package com.agentbanking.common.audit;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogRecord(
    UUID auditId,
    String entityType,
    UUID entityId,
    AuditAction action,
    String performedBy,
    String changes,
    String ipAddress,
    LocalDateTime timestamp,
    AuditOutcome outcome,
    String failureReason,
    String traceId,
    String sessionId,
    String serviceName,
    String deviceInfo,
    String geographicLocation
) {
    public AuditLogRecord {
        if (auditId == null) throw new NullPointerException("auditId cannot be null");
        if (entityType == null) throw new NullPointerException("entityType cannot be null");
        if (action == null) throw new NullPointerException("action cannot be null");
        if (performedBy == null) throw new NullPointerException("performedBy cannot be null");
        if (outcome == null) throw new NullPointerException("outcome cannot be null");
        if (serviceName == null) throw new NullPointerException("serviceName cannot be null");
    }

    public static AuditLogRecord success(...) { ... }
    public static AuditLogRecord failure(...) { ... }
}
```

---

## Per-Service Implementation Pattern

For each service (auth-iam, onboarding, rules, biller, switch, ledger, orchestrator):

### 1. Create AuditLogEntity with all 15 fields

```java
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
    @Id private UUID auditId;
    @Column(nullable = false) private String entityType;
    private UUID entityId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AuditAction action;
    @Column(nullable = false) private String performedBy;
    @Column(columnDefinition = "TEXT") private String changes;
    private String ipAddress;
    @Column(nullable = false) private LocalDateTime timestamp;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AuditOutcome outcome;
    private String failureReason;
    private String traceId;
    private String sessionId;
    @Column(nullable = false) private String serviceName;
    private String deviceInfo;
    private String geographicLocation;
    // getters/setters
}
```

### 2. Create AuditLogRepository port

```java
public interface AuditLogRepository {
    Optional<AuditLogRecord> findById(UUID auditLogId);
    AuditLogRecord save(AuditLogRecord auditLogRecord);
    List<AuditLogRecord> findAll(int page, int size);
    long count();
}
```

### 3. Create AuditLogJpaRepository, AuditLogRepositoryImpl

### 4. Create AuditLogServiceImpl

### 5. Register bean in DomainServiceConfig

### 6. Update AuditController with new entity

### 7. Wire audit logging into controllers

### 8. Add Flyway migration for new columns (if table exists)

---

## Implementation Order

1. ✅ Task 0: Expand AuditAction (done)
2. 🔄 Task 0b: Enhance AuditLogRecord (in progress)
3. Auth IAM Service - update to use enhanced record
4. Onboarding Service - verify/update
5. Rules Service - implement
6. Biller Service - implement
7. Switch Adapter Service - implement
8. Ledger Service - implement
9. Orchestrator Service - implement

---

## Verification

After each service implementation:
1. GET /api/v1/admin/audit-logs?service={service} returns 200 with data
2. Create/update operation creates audit log with all fields populated
3. Database has all 15 columns
    
    // Agent (onboarding)
    AGENT_CREATED,
    AGENT_UPDATED,
    AGENT_DELETED,
    AGENT_ACTIVATED,
    AGENT_DEACTIVATED,
    AGENT_KYC_SUBMITTED,
    AGENT_KYC_APPROVED,
    AGENT_KYC_REJECTED,
    
    // Transactions (ledger)
    TRANSACTION_CREATED,
    TRANSACTION_COMPLETED,
    TRANSACTION_FAILED,
    TRANSACTION_REVERSED,
    TRANSACTION_VOIDED,
    DEPOSIT,
    WITHDRAWAL,
    
    // Bill payment (biller)
    PAYMENT_INITIATED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,
    TOPUP_INITIATED,
    TOPUP_COMPLETED,
    TOPUP_FAILED,
    
    // Switch
    SWITCH_INQUIRY,
    SWITCH_TRANSFER,
    SWITCH_REVERSAL,
    
    // Orchestrator
    SAGA_STARTED,
    SAGA_COMPLETED,
    SAGA_FAILED,
    SAGA_COMPENSATED
}
```

- [ ] **Step 2: Build common module**

Run: `cd /Users/me/myprojects/agentbanking-backend && ./gradlew :common:build -x test`

---

## Phase 1: Auth IAM Service (Refactor to use common)

### Task 1: Create AuditLogRepository port

**Files:**
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/port/out/AuditLogRepository.java`

- [ ] **Step 1: Create port interface**

```java
package com.agentbanking.auth.domain.port.out;

import com.agentbanking.common.audit.AuditLogRecord;

public interface AuditLogRepository {
    AuditLogRecord save(AuditLogRecord record);
    java.util.Optional<AuditLogRecord> findById(java.util.UUID id);
    java.util.List<AuditLogRecord> findAll(int page, int size);
    long count();
}
```

---

### Task 2: Create AuditLogEntity and Mapper

**Files:**
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/persistence/AuditLogEntity.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/persistence/AuditLogMapper.java`

- [ ] **Step 1: Create AuditLogEntity**

```java
package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.common.audit.AuditAction;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
    @Id
    private UUID auditId;
    
    @Column(nullable = false)
    private String entityType;
    
    private UUID entityId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;
    
    @Column(nullable = false)
    private String performedBy;
    
    @Column(columnDefinition = "TEXT")
    private String changes;
    
    private String ipAddress;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    public AuditLogEntity() {}
    
    public UUID getAuditId() { return auditId; }
    public void setAuditId(UUID auditId) { this.auditId = auditId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public String getChanges() { return changes; }
    public void setChanges(String changes) { this.changes = changes; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
```

- [ ] **Step 2: Create AuditLogMapper**

```java
package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.common.audit.AuditLogRecord;
import java.util.List;
import java.util.stream.Collectors;

public class AuditLogMapper {
    
    public static AuditLogRecord toRecord(AuditLogEntity entity) {
        if (entity == null) return null;
        return new AuditLogRecord(
            entity.getAuditId(),
            entity.getEntityType(),
            entity.getEntityId(),
            entity.getAction(),
            entity.getPerformedBy(),
            entity.getChanges(),
            entity.getIpAddress(),
            entity.getTimestamp()
        );
    }
    
    public static AuditLogEntity toEntity(AuditLogRecord record) {
        if (record == null) return null;
        AuditLogEntity entity = new AuditLogEntity();
        entity.setAuditId(record.auditId());
        entity.setEntityType(record.entityType());
        entity.setEntityId(record.entityId());
        entity.setAction(record.action());
        entity.setPerformedBy(record.performedBy());
        entity.setChanges(record.changes());
        entity.setIpAddress(record.ipAddress());
        entity.setTimestamp(record.timestamp());
        return entity;
    }
    
    public static List<AuditLogRecord> toRecordList(List<AuditLogEntity> entities) {
        return entities.stream()
            .map(AuditLogMapper::toRecord)
            .collect(Collectors.toList());
    }
}
```

---

### Task 3: Create AuditLogRepositoryImpl

**Files:**
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/persistence/AuditLogRepositoryImpl.java`

- [ ] **Step 1: Create repository implementation**

```java
package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.domain.port.out.AuditLogRepository;
import com.agentbanking.common.audit.AuditLogRecord;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public AuditLogRecord save(AuditLogRecord record) {
        AuditLogEntity entity = AuditLogMapper.toEntity(record);
        if (entity.getTimestamp() == null) {
            entity.setTimestamp(java.time.LocalDateTime.now());
        }
        entityManager.persist(entity);
        return record;
    }
    
    @Override
    public Optional<AuditLogRecord> findById(UUID id) {
        AuditLogEntity entity = entityManager.find(AuditLogEntity.class, id);
        return Optional.ofNullable(AuditLogMapper.toRecord(entity));
    }
    
    @Override
    public List<AuditLogRecord> findAll(int page, int size) {
        List<AuditLogEntity> entities = entityManager
            .createQuery("SELECT a FROM AuditLogEntity a ORDER BY a.timestamp DESC", AuditLogEntity.class)
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList();
        return AuditLogMapper.toRecordList(entities);
    }
    
    @Override
    public long count() {
        return entityManager
            .createQuery("SELECT COUNT(a) FROM AuditLogEntity a", Long.class)
            .getSingleResult();
    }
}
```

---

### Task 4: Create AuditLogServiceImpl

**Files:**
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/application/usecase/AuditLogServiceImpl.java`

- [ ] **Step 1: Create service implementation**

```java
package com.agentbanking.auth.application.usecase;

import com.agentbanking.auth.domain.port.out.AuditLogRepository;
import com.agentbanking.common.audit.AuditLogRecord;
import com.agentbanking.common.audit.AuditLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuditLogServiceImpl implements AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    
    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
    
    @Override
    public AuditLogRecord log(AuditLogRecord record) {
        if (record.auditId() == null) {
            record = new AuditLogRecord(
                UUID.randomUUID(),
                record.entityType(),
                record.entityId(),
                record.action(),
                record.performedBy(),
                record.changes(),
                record.ipAddress(),
                record.timestamp() != null ? record.timestamp() : LocalDateTime.now()
            );
        }
        return auditLogRepository.save(record);
    }
    
    public List<AuditLogRecord> getAuditLogs(int page, int size) {
        return auditLogRepository.findAll(page, size);
    }
    
    public long getTotalCount() {
        return auditLogRepository.count();
    }
}
```

---

### Task 5: Update DomainServiceConfig

**Files:**
- Modify: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/config/DomainServiceConfig.java`

- [ ] **Step 1: Add AuditLogService bean**

Find the file and add after existing beans:
```java
@Bean
public AuditLogServiceImpl auditLogService(AuditLogRepository auditLogRepository) {
    return new AuditLogServiceImpl(auditLogRepository);
}
```

---

### Task 6: Update AuditController to use common

**Files:**
- Modify: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/AuditController.java`

- [ ] **Step 1: Update controller to use AuditLogServiceImpl**

Replace imports and update class:
```java
package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.application.usecase.AuditLogServiceImpl;
import com.agentbanking.common.audit.AuditLogRecord;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth/audit")
public class AuditController {
    
    private final AuditLogServiceImpl auditLogService;
    
    public AuditController(AuditLogServiceImpl auditLogService) {
        this.auditLogService = auditLogService;
    }
    
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        List<AuditLogRecord> logs = auditLogService.getAuditLogs(page, size);
        long total = auditLogService.getTotalCount();
        
        return ResponseEntity.ok(Map.of(
            "content", logs,
            "totalElements", total,
            "totalPages", (total + size - 1) / size,
            "page", page,
            "size", size
        ));
    }
    
    @GetMapping("/logs/{auditId}")
    public ResponseEntity<AuditLogRecord> getAuditLog(@PathVariable UUID auditId) {
        return auditLogService.getAuditLogs(0, 1).stream()
            .filter(log -> log.auditId().equals(auditId))
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

---

### Task 7: Wire audit into UserController

**Files:**
- Modify: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/UserController.java`

- [ ] **Step 1: Add AuditLogServiceImpl injection**

Add field:
```java
private final AuditLogServiceImpl auditLogService;
```

Add to constructor:
```java
public UserController(ManageUserUseCaseImpl manageUserUseCase, AuditLogServiceImpl auditLogService) {
    this.manageUserUseCase = manageUserUseCase;
    this.auditLogService = auditLogService;
}
```

- [ ] **Step 2: Add audit logging to POST /users**

After creating user, add before return:
```java
auditLogService.log(AuditLogRecord.success(
    "User", userRecord.userId(), AuditAction.USER_CREATED, 
    "system", null
));
```

- [ ] **Step 3: Add audit logging to PUT /users/{id}**

- [ ] **Step 4: Add audit logging to DELETE /users/{id}**

---

### Task 8: Build and test auth-iam-service

- [ ] **Step 1: Build service**

Run: `cd /Users/me/myprojects/agentbanking-backend && ./gradlew :services:auth-iam-service:build -x test`

- [ ] **Step 2: Rebuild container**

Run: `cd /Users/me/myprojects/agentbanking-backend && docker compose build --no-cache auth-iam-service`

- [ ] **Step 3: Restart and test**

Run: `docker compose up -d auth-iam-service`

Test: `curl -H "Authorization: Bearer <token>" "http://localhost:8080/api/v1/admin/audit-logs?service=auth"`

---

## Phase 2: Other Services

The pattern is the same for each service. For brevity, the remaining services follow the same structure:

### Services to implement (same pattern):
- rules-service
- biller-service
- onboarding-service (already has implementation - verify)
- switch-adapter-service (needs common dependency added)
- ledger-service (needs common dependency added)
- orchestrator-service (needs common dependency added)

### Each service needs:
1. AuditLogRepository port interface
2. AuditLogEntity
3. AuditLogMapper
4. AuditLogRepositoryImpl
5. AuditLogServiceImpl implements AuditLogService
6. DomainServiceConfig bean registration
7. AuditController updated
8. Controllers wired with audit logging
9. build.gradle has common dependency

---

## Verification

After each service implementation:
1. GET /api/v1/admin/audit-logs?service={service} returns 200
2. Perform operation in service creates audit log entry
3. GET /internal/audit-logs returns audit data

---

**Plan saved to:** `docs/superpowers/plans/2026-04-08-audit-logging-implementation-plan.md`

**Two execution options:**

1. **Subagent-Driven (recommended)** - Dispatch subagent per task, review between tasks

2. **Inline Execution** - Execute tasks in this session using executing-plans

**Which approach?**