# Agent & User Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement agent user auto-creation, staff user management, temporary password policy, self-service password reset with OTP, and user type-based permission enforcement.

**Architecture:** 
- Feign sync primary for onboarding-service → auth-iam-service user creation
- Kafka fallback (agent.lifecycle topic) when Feign fails
- Redis for OTP storage with 10-min TTL
- External Notification Gateway for SMS/email delivery

**Tech Stack:** Java 21, Spring Boot 3.x, Spring Data JPA (Hibernate), PostgreSQL, Redis, Apache Kafka, Spring Cloud Gateway, Feign, Resilience4j

---

## Phase 1: Database Schema Changes

### Task 1: Add UserType, Password Policy, and AgentId to auth-iam-service

**BDD Scenarios:** 
- S1.1 (Feign creates user), S1.2 (temp password + event), S2.1 (admin creates staff)
- S3.1 (first login flag), S3.2 (expired temp password), S3.3 (password change clears)
- S4.1 (OTP via phone), S4.2 (fallback to email)

**BRD Requirements:** FR-1.3, FR-2.1, FR-2.2, FR-2.3, FR-3.1, FR-4.1, FR-4.2, FR-4.3, FR-4.4, FR-5.1, FR-5.2

**User-Facing:** NO

**Files:**
- Create: `services/auth-iam-service/src/main/resources/db/migration/V3__add_user_type_and_password_policy.sql`
- Test: `services/auth-iam-service/src/test/resources/db/migration/V3__test_data.sql`

- [x] **Step 1: Write the migration file**

```sql
-- V3__add_user_type_and_password_policy.sql

-- Add user_type column with default INTERNAL
ALTER TABLE users ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'INTERNAL';

-- Add agent_id for EXTERNAL users
ALTER TABLE users ADD COLUMN agent_id UUID UNIQUE;

-- Add phone for OTP delivery
ALTER TABLE users ADD COLUMN phone VARCHAR(20);

-- Add must_change_password flag
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT false;

-- Add temporary_password_expires_at
ALTER TABLE users ADD COLUMN temporary_password_expires_at TIMESTAMP WITH TIME ZONE;

-- Add constraint: agent_id only for EXTERNAL users
ALTER TABLE users ADD CONSTRAINT chk_agent_id_user_type
  CHECK (
    (user_type = 'EXTERNAL' AND agent_id IS NOT NULL) OR
    (user_type = 'INTERNAL' AND agent_id IS NULL)
  );

-- System parameters table
CREATE TABLE IF NOT EXISTS system_parameters (
    param_key VARCHAR(100) PRIMARY KEY,
    param_value VARCHAR(500) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Insert default temp password expiry (3 days)
INSERT INTO system_parameters (param_key, param_value, description)
VALUES ('temp.password.expiry.days', '3', 'Temporary password expiry in days');

-- Add index for agent_id lookup
CREATE INDEX IF NOT EXISTS idx_users_agent_id ON users(agent_id);
```

- [x] **Step 2: Verify migration syntax**

Run: `psql -U postgres -d auth_iam_db -f services/auth-iam-service/src/main/resources/db/migration/V3__add_user_type_and_password_policy.sql`
Expected: No syntax errors

- [x] **Step 3: Commit**

```bash
git add services/auth-iam-service/src/main/resources/db/migration/V3__add_user_type_and_password_policy.sql
git commit -m "db: add user_type, password policy columns to users table"
```

---

### Task 2: Add User Creation Status to onboarding-service

**BDD Scenarios:** S1.6 (status update on USER_CREATED), S1.7 (status on FAILED), S6.1, S6.2, S6.3, S6.4, S6.5

**BRD Requirements:** FR-7.1, FR-7.2, FR-8.1, FR-8.2, FR-8.3

**User-Facing:** NO

**Files:**
- Create: `services/onboarding-service/src/main/resources/db/migration/V8__add_user_creation_status.sql`

- [x] **Step 1: Write the migration file**

```sql
-- V8__add_user_creation_status.sql

-- Add user creation status to agent table
ALTER TABLE agent ADD COLUMN user_creation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE agent ADD COLUMN user_creation_error VARCHAR(500);

-- Add index for status queries
CREATE INDEX IF NOT EXISTS idx_agent_user_creation_status ON agent(user_creation_status);
```

- [x] **Step 2: Commit**

```bash
git add services/onboarding-service/src/main/resources/db/migration/V8__add_user_creation_status.sql
git commit -m "db: add user_creation_status to agent table"
```

---

## Phase 2: Domain Model & Enum

### Task 3: Create UserType Enum

**BDD Scenarios:** S1.1, S1.2, S2.1, S5.1, S5.2, S5.3, S5.4, S5.5

**BRD Requirements:** FR-1.3, FR-2.1, FR-3.1, FR-6.1, FR-6.2, FR-6.3, FR-6.4

**User-Facing:** NO

**Files:**
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/model/UserType.java`

- [x] **Step 1: Write the enum (ZERO framework imports)**

```java
package com.agentbanking.auth.domain.model;

public enum UserType {
    INTERNAL,
    EXTERNAL
}
```

- [x] **Step 2: Commit**

```bash
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/model/UserType.java
git commit -m "domain: add UserType enum (INTERNAL, EXTERNAL)"
```

---

### Task 4: Extend UserRecord with new fields

**BDD Scenarios:** All scenarios involving users

**BRD Requirements:** FR-1.3, FR-2.1, FR-3.1, FR-4.1, FR-4.2, FR-4.3, FR-4.4, FR-5.1

**User-Facing:** NO

**Files:**
- Modify: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/model/UserRecord.java`

- [x] **Step 1: Read existing UserRecord and write extended version**

```java
package com.agentbanking.auth.domain.model;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserRecord(
    UUID userId,
    String username,
    String email,
    String phone,
    String passwordHash,
    String fullName,
    UserStatus status,
    UserType userType,
    UUID agentId,
    String agentCode,
    Boolean mustChangePassword,
    LocalDateTime temporaryPasswordExpiresAt,
    Set<String> permissions,
    Integer failedLoginAttempts,
    LocalDateTime lockedUntil,
    LocalDateTime passwordChangedAt,
    LocalDateTime passwordExpiresAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime lastLoginAt,
    String createdBy
) {}
```

- [x] **Step 2: Commit**

```bash
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/model/UserRecord.java
git commit -m "domain: extend UserRecord with userType, phone, mustChangePassword, temporaryPasswordExpiresAt"
```

---

## Phase 3: JPA Entity Changes

### Task 5: Update UserEntity with new columns

**BDD Scenarios:** All user scenarios

**BRD Requirements:** FR-1.3, FR-2.1, FR-3.1, FR-4.1, FR-4.2, FR-4.3, FR-4.4

**User-Facing:** NO

**Files:**
- Modify: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/persistence/UserEntity.java`

- [x] **Step 1: Read existing UserEntity, add new fields**

Add after existing fields:
```java
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    private UserType userType = UserType.INTERNAL;

    @Column(name = "agent_id")
    private UUID agentId;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = false;

    @Column(name = "temporary_password_expires_at")
    private LocalDateTime temporaryPasswordExpiresAt;

// Add getters and setters for all new fields
public UserType getUserType() { return userType; }
public void setUserType(UserType userType) { this.userType = userType; }
public UUID getAgentId() { return agentId; }
public void setAgentId(UUID agentId) { this.agentId = agentId; }
public String getPhone() { return phone; }
public void setPhone(String phone) { this.phone = phone; }
public Boolean getMustChangePassword() { return mustChangePassword; }
public void setMustChangePassword(Boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
public LocalDateTime getTemporaryPasswordExpiresAt() { return temporaryPasswordExpiresAt; }
public void setTemporaryPasswordExpiresAt(LocalDateTime temporaryPasswordExpiresAt) { this.temporaryPasswordExpiresAt = temporaryPasswordExpiresAt; }
```

- [x] **Step 2: Run build to verify compilation**

Run: `cd services/auth-iam-service && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [x] **Step 3: Commit**

```bash
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/persistence/UserEntity.java
git commit -m "persistence: add userType, agentId, phone, mustChangePassword, temporaryPasswordExpiresAt to UserEntity"
```

---

### Task 6: Update UserMapper

**BDD Scenarios:** All user scenarios

**BRD Requirements:** All FRs related to users

**User-Facing:** NO

**Files:**
- Modify: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/persistence/UserMapper.java`

- [x] **Step 1: Read existing UserMapper, update toMap and toEntity**

Add new fields to toMap and toEntity mappings.

- [x] **Step 2: Run build to verify**

```bash
cd services/auth-iam-service && ./mvnw compile -q
```

- [x] **Step 3: Commit**

```bash
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/persistence/UserMapper.java
git commit -m "persistence: update UserMapper with new UserRecord fields"
```

---

### Task 7: Update AgentEntity with user creation status

**BDD Scenarios:** S1.6, S1.7, S6.1, S6.2, S6.3, S6.4, S6.5

**BRD Requirements:** FR-7.1, FR-7.2, FR-8.1, FR-8.2, FR-8.3

**User-Facing:** NO

**Files:**
- Modify: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/persistence/entity/AgentEntity.java`

- [x] **Step 1: Add new fields to AgentEntity**

```java
    @Enumerated(EnumType.STRING)
    @Column(name = "user_creation_status", nullable = false, length = 20)
    private UserCreationStatus userCreationStatus = UserCreationStatus.PENDING;

    @Column(name = "user_creation_error", length = 500)
    private String userCreationError;

// Add getters and setters
public UserCreationStatus getUserCreationStatus() { return userCreationStatus; }
public void setUserCreationStatus(UserCreationStatus userCreationStatus) { this.userCreationStatus = userCreationStatus; }
public String getUserCreationError() { return userCreationError; }
public void setUserCreationError(String userCreationError) { this.userCreationError = userCreationError; }
```

- [x] **Step 2: Create UserCreationStatus enum**

Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/model/UserCreationStatus.java`
```java
package com.agentbanking.onboarding.domain.model;

public enum UserCreationStatus {
    PENDING,
    CREATED,
    FAILED
}
```

- [x] **Step 3: Update AgentMapper**

Modify: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/persistence/mapper/AgentMapper.java`

- [x] **Step 4: Run build**

```bash
cd services/onboarding-service && ./mvnw compile -q
```

- [x] **Step 5: Commit**

```bash
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/persistence/entity/AgentEntity.java
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/model/UserCreationStatus.java
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/persistence/mapper/AgentMapper.java
git commit -m "persistence: add userCreationStatus to AgentEntity"
```

---

## Phase 4: Outbound Ports (Hexagonal)

### Task 8: Create OtpStore port and Redis adapter

**BDD Scenarios:** S4.1, S4.2, S4.3, S4.4, S4.5, S4.6, S4.8

**BRD Requirements:** FR-5.1, FR-5.2, FR-5.3, FR-5.4

**User-Facing:** NO

**Files:**
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/port/out/OtpStore.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/model/OtpData.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/persistence/OtpStoreRedisAdapter.java`

- [x] **Step 1: Write OtpStore port interface**

```java
package com.agentbanking.auth.domain.port.out;

import com.agentbanking.auth.domain.model.OtpData;

public interface OtpStore {
    void storeOtp(String username, String hashedOtp, int ttlSeconds);
    OtpData retrieveOtp(String username);
    void deleteOtp(String username);
    void incrementAttempts(String username);
}
```

- [x] **Step 2: Write OtpData record**

```java
package com.agentbanking.auth.domain.model;

public record OtpData(
    String hashedOtp,
    int attempts,
    java.time.LocalDateTime createdAt
) {}
```

- [x] **Step 3: Write Redis adapter**

```java
package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.domain.model.OtpData;
import com.agentbanking.auth.domain.port.out.OtpStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;

@Repository
public class OtpStoreRedisAdapter implements OtpStore {

    private static final String OTP_KEY_PREFIX = "otp:reset:";
    private static final int MAX_ATTEMPTS = 3;

    private final StringRedisTemplate redisTemplate;

    public OtpStoreRedisAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void storeOtp(String username, String hashedOtp, int ttlSeconds) {
        String key = OTP_KEY_PREFIX + username;
        redisTemplate.opsForHash().put(key, "otp", hashedOtp);
        redisTemplate.opsForHash().put(key, "attempts", "0");
        redisTemplate.opsForHash().put(key, "createdAt", LocalDateTime.now().toString());
        redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public OtpData retrieveOtp(String username) {
        String key = OTP_KEY_PREFIX + username;
        Object otp = redisTemplate.opsForHash().get(key, "otp");
        Object attempts = redisTemplate.opsForHash().get(key, "attempts");
        Object createdAt = redisTemplate.opsForHash().get(key, "createdAt");

        if (otp == null) {
            return null;
        }

        return new OtpData(
            (String) otp,
            attempts != null ? Integer.parseInt((String) attempts) : 0,
            createdAt != null ? LocalDateTime.parse((String) createdAt) : null
        );
    }

    @Override
    public void deleteOtp(String username) {
        redisTemplate.delete(OTP_KEY_PREFIX + username);
    }

    @Override
    public void incrementAttempts(String username) {
        String key = OTP_KEY_PREFIX + username;
        redisTemplate.opsForHash().increment(key, "attempts", 1);
    }
}
```

- [ ] **Step 4: Run build**

```bash
cd services/auth-iam-service && ./mvnw compile -q
```

- [x] **Step 5: Commit**

```bash
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/port/out/OtpStore.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/model/OtpData.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/persistence/OtpStoreRedisAdapter.java
git commit -m "persistence: add OtpStore port and Redis adapter"
```

---

### Task 9: Create NotificationPublisher port and Kafka adapter

**BDD Scenarios:** S1.2 (USER_CREATED), S1.4 (Kafka fallback), S4.7 (PASSWORD_RESET_CONFIRMED)

**BRD Requirements:** FR-2.2, FR-2.3, FR-5.6

**User-Facing:** NO

**Files:**
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/port/out/NotificationPublisher.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/model/UserCreatedEvent.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/model/UserCreationFailedEvent.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/model/PasswordResetConfirmedEvent.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/messaging/NotificationPublisherKafkaAdapter.java`

- [x] **Step 1: Write NotificationPublisher port**

```java
package com.agentbanking.auth.domain.port.out;

import com.agentbanking.auth.domain.model.UserCreatedEvent;
import com.agentbanking.auth.domain.model.UserCreationFailedEvent;
import com.agentbanking.auth.domain.model.PasswordResetConfirmedEvent;
import com.agentbanking.auth.domain.model.OtpRequestedEvent;

public interface NotificationPublisher {
    void publishUserCreated(UserCreatedEvent event);
    void publishUserCreationFailed(UserCreationFailedEvent event);
    void publishPasswordResetConfirmed(PasswordResetConfirmedEvent event);
    void publishOtpRequested(OtpRequestedEvent event);
}
```

- [x] **Step 2: Write event records**

```java
// UserCreatedEvent.java
package com.agentbanking.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record UserCreatedEvent(
    UUID eventId,
    String eventType,
    Instant timestamp,
    UserCreatedData data
) {
    public record UserCreatedData(
        UUID userId,
        String username,
        String email,
        String phone,
        String fullName,
        String userType,
        UUID agentId,
        String notificationChannel,
        String temporaryPassword
    ) {}
    
    public static UserCreatedEvent create(UserCreatedData data) {
        return new UserCreatedEvent(UUID.randomUUID(), "USER_CREATED", Instant.now(), data);
    }
}
```

```java
// UserCreationFailedEvent.java
package com.agentbanking.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record UserCreationFailedEvent(
    UUID eventId,
    String eventType,
    Instant timestamp,
    UserCreationFailedData data
) {
    public record UserCreationFailedData(UUID agentId, String agentCode, String error) {}
    
    public static UserCreationFailedEvent create(UUID agentId, String agentCode, String error) {
        return new UserCreationFailedEvent(UUID.randomUUID(), "USER_CREATION_FAILED", Instant.now(),
            new UserCreationFailedData(agentId, agentCode, error));
    }
}
```

```java
// PasswordResetConfirmedEvent.java
package com.agentbanking.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record PasswordResetConfirmedEvent(
    UUID eventId,
    String eventType,
    Instant timestamp,
    PasswordResetConfirmedData data
) {
    public record PasswordResetConfirmedData(UUID userId, String username, String email, String phone) {}
    
    public static PasswordResetConfirmedEvent create(UUID userId, String username, String email, String phone) {
        return new PasswordResetConfirmedEvent(UUID.randomUUID(), "PASSWORD_RESET_CONFIRMED", Instant.now(),
            new PasswordResetConfirmedData(userId, username, email, phone));
    }
}

// OtpRequestedEvent.java
package com.agentbanking.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record OtpRequestedEvent(
    UUID eventId,
    String eventType,
    Instant timestamp,
    OtpRequestedData data
) {
    public record OtpRequestedData(
        UUID userId,
        String username,
        String email,
        String phone,
        String otp,
        String channel
    ) {}
}
```

- [ ] **Step 3: Write Kafka adapter**

```java
package com.agentbanking.auth.infrastructure.messaging;

import com.agentbanking.auth.domain.model.UserCreatedEvent;
import com.agentbanking.auth.domain.model.UserCreationFailedEvent;
import com.agentbanking.auth.domain.model.PasswordResetConfirmedEvent;
import com.agentbanking.auth.domain.model.OtpRequestedEvent;
import com.agentbanking.auth.domain.port.out.NotificationPublisher;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationPublisherKafkaAdapter implements NotificationPublisher {

    private final StreamBridge streamBridge;

    public NotificationPublisherKafkaAdapter(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public void publishUserCreated(UserCreatedEvent event) {
        streamBridge.send("userCreated-out-0", event);
    }

    @Override
    public void publishUserCreationFailed(UserCreationFailedEvent event) {
        streamBridge.send("userCreationFailed-out-0", event);
    }

    @Override
    public void publishPasswordResetConfirmed(PasswordResetConfirmedEvent event) {
        streamBridge.send("passwordResetConfirmed-out-0", event);
    }

    @Override
    public void publishOtpRequested(OtpRequestedEvent event) {
        streamBridge.send("otpRequested-out-0", event);
    }
}
```

- [ ] **Step 4: Update application.yaml with Kafka bindings**

Modify: `services/auth-iam-service/src/main/resources/application.yaml`

```yaml
spring:
  cloud:
    stream:
      bindings:
        userCreated-out-0:
          destination: user.lifecycle
          content-type: application/json
        userCreationFailed-out-0:
          destination: user.lifecycle
          content-type: application/json
        passwordResetConfirmed-out-0:
          destination: user.lifecycle
          content-type: application/json
        otpRequested-out-0:
          destination: user.lifecycle
          content-type: application/json
```

- [ ] **Step 5: Run build**

```bash
cd services/auth-iam-service && ./mvnw compile -q
```

- [ ] **Step 6: Commit**

```bash
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/port/out/NotificationPublisher.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/model/UserCreatedEvent.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/model/UserCreationFailedEvent.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/model/PasswordResetConfirmedEvent.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/messaging/NotificationPublisherKafkaAdapter.java
git commit -m "messaging: add NotificationPublisher port and Kafka adapter"
```

---

### Task 10: Create CreateAgentUserUseCase inbound port

**BDD Scenarios:** S1.1, S1.4, S1.5

**BRD Requirements:** FR-2.1, FR-2.4, FR-2.7

**User-Facing:** NO

**Files:**
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/port/in/CreateAgentUserUseCase.java`

- [ ] **Step 1: Write the use case interface**

```java
package com.agentbanking.auth.domain.port.in;

import com.agentbanking.auth.domain.model.UserRecord;
import java.util.UUID;

public interface CreateAgentUserUseCase {
    UserRecord createAgentUser(UUID agentId, String agentCode, String phone, String email, String businessName);
}
```

- [ ] **Step 2: Commit**

```bash
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/port/in/CreateAgentUserUseCase.java
git commit -m "domain: add CreateAgentUserUseCase port"
```

---

## Phase 5: Domain Services

### Task 11: Create PasswordResetService domain service

**BDD Scenarios:** S4.1, S4.2, S4.3, S4.4, S4.5, S4.6, S4.7, S4.8

**BRD Requirements:** FR-5.1, FR-5.2, FR-5.3, FR-5.4, FR-5.6

**User-Facing:** NO

**Files:**
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/service/PasswordResetService.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/service/TemporaryPasswordGenerator.java`

- [ ] **Step 1: Write TemporaryPasswordGenerator**

```java
package com.agentbanking.auth.domain.service;

import java.security.SecureRandom;

public class TemporaryPasswordGenerator {
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final int MIN_LENGTH = 8;
    
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder password = new StringBuilder();
        password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        
        String allChars = UPPERCASE + LOWERCASE + DIGITS;
        for (int i = 3; i < MIN_LENGTH; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        return shuffle(password.toString());
    }
    
    private String shuffle(String s) {
        char[] chars = s.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}
```

- [ ] **Step 2: Write PasswordResetService**

```java
package com.agentbanking.auth.domain.service;

import com.agentbanking.auth.domain.model.OtpData;
import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.model.PasswordResetConfirmedEvent;
import com.agentbanking.auth.domain.port.out.NotificationPublisher;
import com.agentbanking.auth.domain.port.out.OtpStore;
import com.agentbanking.auth.domain.port.out.UserRepository;
import com.agentbanking.auth.domain.port.out.PasswordHasher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

public class PasswordResetService {
    
    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_SECONDS = 600; // 10 minutes
    private static final int MAX_ATTEMPTS = 3;
    
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final NotificationPublisher notificationPublisher;
    private final OtpStore otpStore;
    private final TemporaryPasswordGenerator tempPasswordGenerator;

    public PasswordResetService(UserRepository userRepository,
                                PasswordHasher passwordHasher,
                                NotificationPublisher notificationPublisher,
                                OtpStore otpStore) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.notificationPublisher = notificationPublisher;
        this.otpStore = otpStore;
        this.tempPasswordGenerator = new TemporaryPasswordGenerator();
    }

    public void requestReset(String username) {
        // Non-existent user: return generic response (no user enumeration)
        UserRecord user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return;
        }
        
        // Generate 6-digit OTP
        String otp = String.format("%06d", new SecureRandom().nextInt(999999));
        String hashedOtp = hashOtp(otp);
        
        // Store in Redis
        otpStore.storeOtp(username, hashedOtp, OTP_TTL_SECONDS);
        
        // Publish event for notification gateway
        String channel = user.phone() != null ? "SMS" : "EMAIL";
        
        // Publish OTP_REQUESTED event for notification gateway
        notificationPublisher.publishOtpRequested(new OtpRequestedEvent(
            UUID.randomUUID(),
            "OTP_REQUESTED",
            Instant.now(),
            new OtpRequestedEvent.OtpRequestedData(
                user.userId(),
                user.username(),
                user.email(),
                user.phone(),
                otp,
                channel
            )
        ));
    }

    public void verifyReset(String username, String otp, String newPassword) {
        OtpData otpData = otpStore.retrieveOtp(username);
        
        if (otpData == null) {
            throw new AuthBusinessException("ERR_AUTH_OTP_EXPIRED", "OTP has expired or not found");
        }
        
        if (otpData.attempts() >= MAX_ATTEMPTS) {
            otpStore.deleteOtp(username);
            throw new AuthBusinessException("ERR_AUTH_OTP_MAX_ATTEMPTS", "Maximum OTP attempts exceeded");
        }
        
        String hashedOtp = hashOtp(otp);
        if (!hashedOtp.equals(otpData.hashedOtp())) {
            otpStore.incrementAttempts(username);
            throw new AuthBusinessException("ERR_AUTH_OTP_INVALID", "Invalid OTP provided");
        }
        
        // Reset password
        UserRecord user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AuthBusinessException("ERR_AUTH_USER_NOT_FOUND", "User not found"));
        
        String hashedPassword = passwordHasher.hash(newPassword);
        userRepository.updatePassword(user.userId(), hashedPassword, LocalDateTime.now());
        
        // Invalidate OTP
        otpStore.deleteOtp(username);
        
        // Publish confirmation event
        notificationPublisher.publishPasswordResetConfirmed(
            PasswordResetConfirmedEvent.create(user.userId(), user.username(), user.email(), user.phone())
        );
    }
    
    public String generateTemporaryPassword() {
        return tempPasswordGenerator.generate();
    }
    
    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    public static class AuthBusinessException extends RuntimeException {
        private final String code;
        public AuthBusinessException(String code, String message) {
            super(message);
            this.code = code;
        }
        public String getCode() { return code; }
    }
}
```

- [ ] **Step 3: Register bean in DomainServiceConfig**

Modify: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/config/DomainServiceConfig.java`

```java
@Bean
public PasswordResetService passwordResetService(UserRepository userRepository,
                                                  PasswordHasher passwordHasher,
                                                  NotificationPublisher notificationPublisher,
                                                  OtpStore otpStore) {
    return new PasswordResetService(userRepository, passwordHasher, notificationPublisher, otpStore);
}
```

- [ ] **Step 4: Run build**

```bash
cd services/auth-iam-service && ./mvnw compile -q
```

- [ ] **Step 5: Commit**

```bash
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/service/PasswordResetService.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/service/TemporaryPasswordGenerator.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/config/DomainServiceConfig.java
git commit -m "domain: add PasswordResetService with OTP verification"
```

---

### Task 12: Update UserManagementService

**BDD Scenarios:** S1.1, S1.2, S1.3, S2.1, S2.2, S2.3, S2.4, S2.5, S3.1, S3.2, S3.3, S3.4

**BRD Requirements:** FR-1.3, FR-1.5, FR-2.1, FR-2.2, FR-2.3, FR-2.7, FR-3.1, FR-3.2, FR-3.3, FR-4.1, FR-4.2, FR-4.3, FR-4.4

**User-Facing:** NO

**Files:**
- Modify: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/service/UserManagementService.java`

- [ ] **Step 1: Read existing UserManagementService, extend with new methods**

Add the following methods to UserManagementService:

```java
public UserRecord createAgentUser(UUID agentId, String agentCode, String phone, String email, String businessName) {
    // Check if user already exists for this agent (idempotent)
    userRepository.findByAgentId(agentId).ifPresent(u -> {
        throw new UserManagementService.UserAlreadyExistsException("User already exists for this agent");
    });
    
    // Generate temporary password
    String tempPassword = tempPasswordGenerator.generate();
    String hashedPassword = passwordHasher.hash(tempPassword);
    
    // Get temp password expiry from system parameter (default 3 days)
    LocalDateTime tempPasswordExpiresAt = LocalDateTime.now().plusDays(3);
    
    // Create UserRecord with EXTERNAL type
    UserRecord user = new UserRecord(
        UUID.randomUUID(),
        agentCode,  // username = agentCode
        email,
        phone,
        hashedPassword,
        businessName,  // fullName = businessName
        UserStatus.ACTIVE,
        UserType.EXTERNAL,  // NEW: userType
        agentId,  // NEW: agentId
        agentCode,  // NEW: agentCode
        true,  // NEW: mustChangePassword
        tempPasswordExpiresAt,  // NEW: temporaryPasswordExpiresAt
        Set.of("AGENT"),  // permissions - AGENT role
        0,  // failedLoginAttempts
        null,  // lockedUntil
        LocalDateTime.now(),  // passwordChangedAt
        null,  // passwordExpiresAt
        LocalDateTime.now(),  // createdAt
        LocalDateTime.now(),  // updatedAt
        null,  // lastLoginAt
        "SYSTEM"  // createdBy
    );
    
    userRepository.save(user);
    
    // Return user with plaintext temp password (for notification)
    return new UserRecord(
        user.userId(), user.username(), user.email(), user.phone(), tempPassword,  // Note: tempPassword instead of hash
        user.fullName(), user.status(), user.userType(), user.agentId(), user.agentCode(),
        user.mustChangePassword(), user.temporaryPasswordExpiresAt(), user.permissions(),
        user.failedLoginAttempts(), user.lockedUntil(), user.passwordChangedAt(),
        user.passwordExpiresAt(), user.createdAt(), user.updatedAt(), user.lastLoginAt(), user.createdBy()
    );
}

public Optional<UserRecord> findByAgentId(UUID agentId) {
    return userRepository.findByAgentId(agentId);
}

public void changePassword(UUID userId, String currentPassword, String newPassword) {
    UserRecord user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found"));
    
    // Verify current password
    if (!passwordHasher.verify(currentPassword, user.passwordHash())) {
        throw new InvalidPasswordException("Current password is incorrect");
    }
    
    // Update password
    String hashedPassword = passwordHasher.hash(newPassword);
    userRepository.updatePassword(userId, hashedPassword, LocalDateTime.now());
    
    // Clear mustChangePassword and temporaryPasswordExpiresAt
    userRepository.clearTempPasswordFlags(userId);
}
```

Also update `createUser` method to:
1. Accept `userType` parameter
2. Generate temp password if not provided
3. Set `mustChangePassword = true` for new users
4. Set `temporaryPasswordExpiresAt` based on system parameter

- [ ] **Step 2: Add repository methods**

Modify: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/port/out/UserRepository.java`

Add:
```java
Optional<UserRecord> findByAgentId(UUID agentId);
void updatePassword(UUID userId, String passwordHash, LocalDateTime changedAt);
void clearTempPasswordFlags(UUID userId);
```

- [ ] **Step 3: Implement repository methods**

Modify: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/persistence/UserRepositoryImpl.java`

Add implementations for:
- `findByAgentId` - query by agent_id column
- `updatePassword` - update password_hash, password_changed_at
- `clearTempPasswordFlags` - set must_change_password = false, temporary_password_expires_at = null

- [ ] **Step 4: Run build**

```bash
cd services/auth-iam-service && ./mvnw compile -q
```

- [ ] **Step 5: Commit**

```bash
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/service/UserManagementService.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/port/out/UserRepository.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/persistence/UserRepositoryImpl.java
git commit -m "domain: extend UserManagementService with agent user creation and password policy"
```

---

### Task 13: Create AgentUserSyncService (Kafka consumer)

**BDD Scenarios:** S1.4, S1.5

**BRD Requirements:** FR-2.4, FR-2.7

**User-Facing:** NO

**Files:**
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/model/AgentCreatedEvent.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/service/AgentUserSyncService.java`

- [ ] **Step 1: Write AgentCreatedEvent**

```java
package com.agentbanking.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AgentCreatedEvent(
    UUID eventId,
    String eventType,
    Instant timestamp,
    AgentCreatedData data
) {
    public record AgentCreatedData(
        UUID agentId,
        String agentCode,
        String phoneNumber,
        String email,
        String businessName
    ) {}
}
```

- [ ] **Step 2: Write AgentUserSyncService**

```java
package com.agentbanking.auth.domain.service;

import com.agentbanking.auth.domain.model.AgentCreatedEvent;
import com.agentbanking.auth.domain.model.UserCreatedEvent;
import com.agentbanking.auth.domain.model.UserCreationFailedEvent;
import com.agentbanking.auth.domain.port.in.CreateAgentUserUseCase;
import com.agentbanking.auth.domain.port.out.NotificationPublisher;
import com.agentbanking.auth.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AgentUserSyncService {
    
    private static final Logger log = LoggerFactory.getLogger(AgentUserSyncService.class);
    
    private final CreateAgentUserUseCase createAgentUserUseCase;
    private final UserRepository userRepository;
    private final NotificationPublisher notificationPublisher;

    public AgentUserSyncService(CreateAgentUserUseCase createAgentUserUseCase,
                                UserRepository userRepository,
                                NotificationPublisher notificationPublisher) {
        this.createAgentUserUseCase = createAgentUserUseCase;
        this.userRepository = userRepository;
        this.notificationPublisher = notificationPublisher;
    }

    public void handleAgentCreated(AgentCreatedEvent event) {
        var data = event.data();
        
        // Idempotent check - skip if user already exists for this agent
        Optional<?> existingUser = userRepository.findByAgentId(data.agentId());
        if (existingUser.isPresent()) {
            log.info("User already exists for agent {}, skipping", data.agentCode());
            return;
        }
        
        try {
            var user = createAgentUserUseCase.createAgentUser(
                data.agentId(),
                data.agentCode(),
                data.phoneNumber(),
                data.email(),
                data.businessName()
            );
            
            notificationPublisher.publishUserCreated(
                UserCreatedEvent.create(new UserCreatedEvent.UserCreatedData(
                    user.userId(),
                    user.username(),
                    user.email(),
                    user.phone(),
                    user.fullName(),
                    user.userType().name(),
                    user.agentId(),
                    data.phoneNumber() != null ? "SMS" : "EMAIL",
                    null // temp password not included in event
                ))
            );
        } catch (Exception e) {
            log.error("Failed to create user for agent {}: {}", data.agentCode(), e.getMessage());
            notificationPublisher.publishUserCreationFailed(
                UserCreationFailedEvent.create(data.agentId(), data.agentCode(), e.getMessage())
            );
        }
    }
}
```

- [ ] **Step 3: Register bean**

Modify: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/config/DomainServiceConfig.java`

```java
@Bean
public AgentUserSyncService agentUserSyncService(CreateAgentUserUseCase createAgentUserUseCase,
                                                  UserRepository userRepository,
                                                  NotificationPublisher notificationPublisher) {
    return new AgentUserSyncService(createAgentUserUseCase, userRepository, notificationPublisher);
}
```

- [ ] **Step 4: Run build**

```bash
cd services/auth-iam-service && ./mvnw compile -q
```

- [ ] **Step 5: Commit**

```bash
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/model/AgentCreatedEvent.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/service/AgentUserSyncService.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/config/DomainServiceConfig.java
git commit -m "domain: add AgentUserSyncService for Kafka consumer"
```

---

### Task 14: Create Kafka consumer for AGENT_CREATED

**BDD Scenarios:** S1.4, S1.5

**BRD Requirements:** FR-2.4, FR-2.7

**User-Facing:** NO

**Files:**
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/messaging/AgentCreatedEventConsumer.java`

- [ ] **Step 1: Write Kafka consumer**

```java
package com.agentbanking.auth.infrastructure.messaging;

import com.agentbanking.auth.domain.model.AgentCreatedEvent;
import com.agentbanking.auth.domain.service.AgentUserSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class AgentCreatedEventConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(AgentCreatedEventConsumer.class);
    
    private final AgentUserSyncService agentUserSyncService;

    public AgentCreatedEventConsumer(AgentUserSyncService agentUserSyncService) {
        this.agentUserSyncService = agentUserSyncService;
    }

    @Bean
    public Consumer<AgentCreatedEvent> agentCreatedIn() {
        return event -> {
            log.info("Received AGENT_CREATED event for agent: {}", event.data().agentCode());
            agentUserSyncService.handleAgentCreated(event);
        };
    }
}
```

- [ ] **Step 2: Update application.yaml**

Modify: `services/auth-iam-service/src/main/resources/application.yaml`

```yaml
spring:
  cloud:
    stream:
      bindings:
        agentCreated-in-0:
          destination: agent.lifecycle
          group: auth-iam-service
          content-type: application/json
```

- [ ] **Step 3: Run build**

```bash
cd services/auth-iam-service && ./mvnw compile -q
```

- [ ] **Step 4: Commit**

```bash
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/messaging/AgentCreatedEventConsumer.java
git add services/auth-iam-service/src/main/resources/application.yaml
git commit -m "messaging: add AGENT_CREATED Kafka consumer"
```

---

## Phase 6: REST Controllers & DTOs

### Task 15: Create Password Reset DTOs and Controller

**BDD Scenarios:** S4.1, S4.2, S4.3, S4.4, S4.5, S4.6, S4.7, S4.8

**BRD Requirements:** FR-5.1, FR-5.2, FR-5.3, FR-5.4, FR-5.6

**User-Facing:** YES

**Files:**
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/ForgotPasswordRequest.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/ForgotPasswordResponse.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/ResetPasswordRequest.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/ResetPasswordResponse.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/ChangePasswordRequest.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/ChangePasswordResponse.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/PasswordController.java`

- [ ] **Step 1: Write DTOs (use Jakarta validation)**

```java
// ForgotPasswordRequest.java
package com.agentbanking.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
    @NotBlank(message = "Username is required")
    String username
) {}
```

```java
// ForgotPasswordResponse.java
package com.agentbanking.auth.infrastructure.web.dto;

public record ForgotPasswordResponse(String message) {}
```

```java
// ResetPasswordRequest.java
package com.agentbanking.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
    @NotBlank(message = "Username is required")
    String username,
    @NotBlank(message = "OTP is required")
    String otp,
    @NotBlank(message = "New password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", 
             message = "Password must be 8+ chars with uppercase, lowercase, and digit")
    String newPassword
) {}
```

```java
// ResetPasswordResponse.java
package com.agentbanking.auth.infrastructure.web.dto;

public record ResetPasswordResponse(String message) {}
```

```java
// ChangePasswordRequest.java
package com.agentbanking.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
    @NotBlank(message = "Current password is required")
    String currentPassword,
    @NotBlank(message = "New password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", 
             message = "Password must be 8+ chars with uppercase, lowercase, and digit")
    String newPassword
) {}
```

```java
// ChangePasswordResponse.java
package com.agentbanking.auth.infrastructure.web.dto;

public record ChangePasswordResponse(String message) {}
```

- [ ] **Step 2: Write PasswordController**

```java
package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.domain.port.in.ManageUserUseCase;
import com.agentbanking.auth.domain.service.PasswordResetService;
import com.agentbanking.auth.infrastructure.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/password")
public class PasswordController {

    private final PasswordResetService passwordResetService;
    private final ManageUserUseCase manageUserUseCase;

    public PasswordController(PasswordResetService passwordResetService,
                              ManageUserUseCase manageUserUseCase) {
        this.passwordResetService = passwordResetService;
        this.manageUserUseCase = manageUserUseCase;
    }

    @PostMapping("/forgot")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.username());
        // Generic response regardless of user existence
        return ResponseEntity.ok(new ForgotPasswordResponse("If the user exists, an OTP has been sent"));
    }

    @PostMapping("/reset")
    public ResponseEntity<ResetPasswordResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.verifyReset(request.username(), request.otp(), request.newPassword());
        return ResponseEntity.ok(new ResetPasswordResponse("Password reset successfully"));
    }

    @PostMapping("/change")
    public ResponseEntity<ChangePasswordResponse> changePassword(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        manageUserUseCase.changePassword(java.util.UUID.fromString(userId), 
                                         request.currentPassword(), 
                                         request.newPassword());
        return ResponseEntity.ok(new ChangePasswordResponse("Password changed successfully"));
    }
}
```

- [ ] **Step 3: Add routes to gateway**

Modify: `gateway/src/main/resources/application.yaml`

```yaml
# Password endpoints (public)
- id: auth-password-forgot
  uri: http://auth-iam-service:8087
  predicates:
    - Path=/auth/password/forgot

- id: auth-password-reset
  uri: http://auth-iam-service:8087
  predicates:
    - Path=/auth/password/reset

# Password change (protected)
- id: auth-password-change
  uri: http://auth-iam-service:8087
  predicates:
    - Path=/auth/password/change
  filters:
    - JwtAuthFilter

# Internal password endpoints for onboarding-service Feign calls
- id: internal-auth-agent-user
  uri: http://auth-iam-service:8087
  predicates:
    - Path=/internal/users/agent
  filters:
    - RewritePath=/internal/users/agent, /internal/users/agent
```

- [ ] **Step 4: Run build**

```bash
cd services/auth-iam-service && ./mvnw compile -q
cd gateway && ./mvnw compile -q
```

- [ ] **Step 5: Commit**

```bash
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/ForgotPasswordRequest.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/ForgotPasswordResponse.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/ResetPasswordRequest.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/ResetPasswordResponse.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/ChangePasswordRequest.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/ChangePasswordResponse.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/PasswordController.java
git add gateway/src/main/resources/application.yaml
git commit -m "api: add password reset/change endpoints"
```

---

### Task 16: Create Agent User Status API

**BDD Scenarios:** S6.1, S6.2, S6.3, S6.4, S6.5

**BRD Requirements:** FR-7.1, FR-7.2

**User-Facing:** YES

**Files:**
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/AgentUserStatusResponse.java`
- Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/AgentUserController.java`

- [ ] **Step 1: Write DTO**

```java
package com.agentbanking.auth.infrastructure.web.dto;

import java.util.UUID;

public record AgentUserStatusResponse(
    UUID agentId,
    String status,
    UUID userId,
    String error
) {}
```

- [ ] **Step 2: Write Controller**

```java
package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.domain.port.in.CreateAgentUserUseCase;
import com.agentbanking.auth.domain.port.out.UserRepository;
import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.infrastructure.web.dto.AgentUserStatusResponse;
import com.agentbanking.auth.infrastructure.web.dto.UserResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth/users/agent")
public class AgentUserController {

    private final UserRepository userRepository;
    private final CreateAgentUserUseCase createAgentUserUseCase;

    public AgentUserController(UserRepository userRepository,
                                CreateAgentUserUseCase createAgentUserUseCase) {
        this.userRepository = userRepository;
        this.createAgentUserUseCase = createAgentUserUseCase;
    }

    @GetMapping("/{agentId}/status")
    public ResponseEntity<AgentUserStatusResponse> getUserStatus(@PathVariable UUID agentId) {
        return userRepository.findByAgentId(agentId)
            .map(user -> ResponseEntity.ok(new AgentUserStatusResponse(
                agentId,
                "CREATED",
                user.userId(),
                null
            )))
            .orElseGet(() -> ResponseEntity.ok(new AgentUserStatusResponse(
                agentId,
                "PENDING",
                null,
                null
            )));
    }

    @PostMapping("/{agentId}/create")
    public ResponseEntity<UserResponseDto> createUser(@PathVariable UUID agentId) {
        // Manual trigger - check if user already exists
        return userRepository.findByAgentId(agentId)
            .map(user -> ResponseEntity.ok(new UserResponseDto(
                user.userId(),
                user.username(),
                user.email(),
                user.fullName(),
                user.status().name()
            )))
            .orElseGet(() -> ResponseEntity.badRequest().build());
    }
}
```

- [ ] **Step 3: Create /internal/users/agent endpoint for Feign client**

Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/InternalAgentUserController.java`

```java
package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.domain.port.in.CreateAgentUserUseCase;
import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.infrastructure.web.dto.UserResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/users/agent")
public class InternalAgentUserController {

    private final CreateAgentUserUseCase createAgentUserUseCase;

    public InternalAgentUserController(CreateAgentUserUseCase createAgentUserUseCase) {
        this.createAgentUserUseCase = createAgentUserUseCase;
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createAgentUser(@RequestBody InternalCreateAgentUserRequest request) {
        UserRecord user = createAgentUserUseCase.createAgentUser(
            request.agentId(),
            request.agentCode(),
            request.phone(),
            request.email(),
            request.businessName()
        );
        
        return ResponseEntity.ok(new UserResponseDto(
            user.userId(),
            user.username(),
            user.email(),
            user.fullName(),
            user.status().name()
        ));
    }
}
```

Create: `services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/InternalCreateAgentUserRequest.java`

```java
package com.agentbanking.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InternalCreateAgentUserRequest(
    @NotNull UUID agentId,
    @NotBlank String agentCode,
    String phone,
    String email,
    @NotBlank String businessName
) {}
```

- [ ] **Step 4: Add gateway routes**

Modify: `gateway/src/main/resources/application.yaml`

```yaml
# Agent user status (must be BEFORE /auth/users/* wildcard)
- id: backoffice-agent-user-status
  uri: http://auth-iam-service:8087
  predicates:
    - Path=/api/v1/backoffice/agents/*/user-status
  filters:
    - JwtAuthFilter
    - RewritePath=/api/v1/backoffice/agents/(?<agentId>.*)/user-status, /auth/users/agent/${agentId}/status

- id: backoffice-agent-user-create
  uri: http://auth-iam-service:8087
  predicates:
    - Path=/api/v1/backoffice/agents/*/create-user
  filters:
    - JwtAuthFilter
    - RewritePath=/api/v1/backoffice/agents/(?<agentId>.*)/create-user, /auth/users/agent/${agentId}/create

# Internal agent user creation (for Feign client from onboarding-service)
- id: internal-auth-agent-user
  uri: http://auth-iam-service:8087
  predicates:
    - Path=/internal/users/agent
  filters:
    - RewritePath=/internal/users/agent, /internal/users/agent
```

- [ ] **Step 5: Run build**

```bash
cd services/auth-iam-service && ./mvnw compile -q
cd gateway && ./mvnw compile -q
```

- [ ] **Step 6: Commit**

```bash
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/AgentUserStatusResponse.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/AgentUserController.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/InternalAgentUserController.java
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/infrastructure/web/dto/InternalCreateAgentUserRequest.java
git add gateway/src/main/resources/application.yaml
git commit -m "api: add agent user status and internal creation endpoints"
```

---

## Phase 7: Onboarding Service Integration

### Task 17: Create Feign Client in onboarding-service

**BDD Scenarios:** S1.1, S1.3

**BRD Requirements:** FR-2.1, FR-2.7

**User-Facing:** NO

**Files:**
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/port/out/AuthServiceClient.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/external/AuthUserFeignClient.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/external/AuthServiceFallbackFactory.java`
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/web/dto/CreateAgentUserRequest.java`

- [ ] **Step 1: Write AuthServiceClient port**

```java
package com.agentbanking.onboarding.domain.port.out;

import com.agentbanking.onboarding.infrastructure.web.dto.CreateAgentUserRequest;
import com.agentbanking.onboarding.infrastructure.web.dto.UserResponseDto;

public interface AuthServiceClient {
    UserResponseDto createUserForAgent(CreateAgentUserRequest request);
}
```

- [ ] **Step 2: Write CreateAgentUserRequest DTO**

```java
package com.agentbanking.onboarding.infrastructure.web.dto;

import java.util.UUID;

public record CreateAgentUserRequest(
    UUID agentId,
    String agentCode,
    String phone,
    String email,
    String businessName
) {}
```

- [ ] **Step 3: Write UserResponseDto**

```java
package com.agentbanking.onboarding.infrastructure.web.dto;

import java.util.UUID;

public record UserResponseDto(
    UUID userId,
    String username,
    String email,
    String fullName,
    String status
) {}
```

- [ ] **Step 4: Write Feign client**

```java
package com.agentbanking.onboarding.infrastructure.external;

import com.agentbanking.onboarding.infrastructure.web.dto.CreateAgentUserRequest;
import com.agentbanking.onboarding.infrastructure.web.dto.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", url = "${auth.service.url}",
             fallbackFactory = AuthServiceFallbackFactory.class)
public interface AuthUserFeignClient {
    
    @PostMapping("/internal/users/agent")
    UserResponseDto createAgentUser(@RequestBody CreateAgentUserRequest request);
}
```

- [ ] **Step 5: Write Fallback factory**

```java
package com.agentbanking.onboarding.infrastructure.external;

import com.agentbanking.onboarding.domain.port.out.AuthServiceClient;
import com.agentbanking.onboarding.infrastructure.web.dto.CreateAgentUserRequest;
import com.agentbanking.onboarding.infrastructure.web.dto.UserResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthServiceFallbackFactory implements FallbackFactory<AuthUserFeignClient> {
    
    private static final Logger log = LoggerFactory.getLogger(AuthServiceFallbackFactory.class);

    @Override
    public AuthUserFeignClient create(Throwable throwable) {
        return request -> {
            log.error("Feign call to auth-service failed: {}", throwable.getMessage());
            // Return null to trigger Kafka fallback in AgentService
            return null;
        };
    }
}
```

- [ ] **Step 6: Add auth.service.url to application.yaml**

Modify: `services/onboarding-service/src/main/resources/application.yaml`

```yaml
auth:
  service:
    url: http://auth-iam-service:8087
```

- [ ] **Step 7: Run build**

```bash
cd services/onboarding-service && ./mvnw compile -q
```

- [ ] **Step 8: Commit**

```bash
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/port/out/AuthServiceClient.java
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/external/AuthUserFeignClient.java
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/external/AuthServiceFallbackFactory.java
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/web/dto/CreateAgentUserRequest.java
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/web/dto/UserResponseDto.java
git add services/onboarding-service/src/main/resources/application.yaml
git commit -m "integration: add Feign client for auth-service user creation"
```

---

### Task 18: Modify AgentService to create user on agent creation

**BDD Scenarios:** S1.1, S1.3, S1.6

**BRD Requirements:** FR-2.1, FR-2.7, FR-8.1, FR-8.2

**User-Facing:** NO

**Files:**
- Modify: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/service/AgentService.java`

- [ ] **Step 1: Read existing AgentService, add user creation call**

Modify `createAgent` method to:
1. Persist agent entity (with userCreationStatus = PENDING)
2. Call auth-iam-service via Feign
3. On success: update userCreationStatus = CREATED
4. On failure (Feign returns null): publish AGENT_CREATED to Kafka

- [ ] **Step 2: Add Kafka producer to onboarding-service**

Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/messaging/AgentEventProducer.java`

```java
package com.agentbanking.onboarding.infrastructure.messaging;

import com.agentbanking.onboarding.domain.model.AgentCreatedEvent;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public class AgentEventProducer {
    
    private final StreamBridge streamBridge;

    public AgentEventProducer(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void publishAgentCreated(UUID agentId, String agentCode, String phone, String email, String businessName) {
        var event = new AgentCreatedEvent(
            UUID.randomUUID(),
            "AGENT_CREATED",
            Instant.now(),
            new AgentCreatedEvent.AgentCreatedData(agentId, agentCode, phone, email, businessName)
        );
        streamBridge.send("agentCreated-out-0", event);
    }
}
```

- [ ] **Step 3: Add Kafka bindings to application.yaml**

Modify: `services/onboarding-service/src/main/resources/application.yaml`

```yaml
spring:
  cloud:
    stream:
      bindings:
        agentCreated-out-0:
          destination: agent.lifecycle
          content-type: application/json
```

- [ ] **Step 4: Run build**

```bash
cd services/onboarding-service && ./mvnw compile -q
```

- [ ] **Step 5: Commit**

```bash
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/service/AgentService.java
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/messaging/AgentEventProducer.java
git add services/onboarding-service/src/main/resources/application.yaml
git commit -m "domain: add user creation on agent creation with Feign + Kafka fallback"
```

---

### Task 19: Create Kafka consumer for USER_CREATED in onboarding-service

**BDD Scenarios:** S1.6, S1.7

**BRD Requirements:** FR-8.1, FR-8.2, FR-8.3

**User-Facing:** NO

**Files:**
- Create: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/messaging/UserLifecycleEventConsumer.java`

- [ ] **Step 1: Write Kafka consumer**

```java
package com.agentbanking.onboarding.infrastructure.messaging;

import com.agentbanking.auth.domain.model.UserCreatedEvent;
import com.agentbanking.auth.domain.model.UserCreationFailedEvent;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;
import com.agentbanking.onboarding.domain.model.UserCreationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class UserLifecycleEventConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(UserLifecycleEventConsumer.class);
    
    private final AgentRepository agentRepository;

    public UserLifecycleEventConsumer(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Bean
    public Consumer<UserCreatedEvent> userCreatedIn() {
        return event -> {
            log.info("Received USER_CREATED event for agent: {}", event.data().agentId());
            agentRepository.updateUserCreationStatus(
                event.data().agentId(),
                UserCreationStatus.CREATED,
                null
            );
        };
    }

    @Bean
    public Consumer<UserCreationFailedEvent> userCreationFailedIn() {
        return event -> {
            log.error("Received USER_CREATION_FAILED for agent: {}, error: {}", 
                      event.data().agentId(), event.data().error());
            agentRepository.updateUserCreationStatus(
                event.data().agentId(),
                UserCreationStatus.FAILED,
                event.data().error()
            );
        };
    }
}
```

- [ ] **Step 2: Add Kafka bindings**

Modify: `services/onboarding-service/src/main/resources/application.yaml`

```yaml
spring:
  cloud:
    stream:
      bindings:
        userCreated-in-0:
          destination: user.lifecycle
          group: onboarding-service
          content-type: application/json
        userCreationFailed-in-0:
          destination: user.lifecycle
          group: onboarding-service
          content-type: application/json
```

- [ ] **Step 3: Add repository method**

Modify: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/port/out/AgentRepository.java`

```java
void updateUserCreationStatus(UUID agentId, UserCreationStatus status, String error);
```

- [ ] **Step 4: Implement repository method**

Modify: `services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/persistence/repository/AgentRepositoryImpl.java`

- [ ] **Step 5: Run build**

```bash
cd services/onboarding-service && ./mvnw compile -q
```

- [ ] **Step 6: Commit**

```bash
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/messaging/UserLifecycleEventConsumer.java
git add services/onboarding-service/src/main/resources/application.yaml
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/domain/port/out/AgentRepository.java
git add services/onboarding-service/src/main/java/com/agentbanking/onboarding/infrastructure/persistence/repository/AgentRepositoryImpl.java
git commit -m "messaging: add USER_CREATED consumer in onboarding-service"
```

---

### Task 20: Add UserType validation in role assignment

**BDD Scenarios:** S5.1, S5.2, S5.3, S5.4, S5.5

**BRD Requirements:** FR-6.1, FR-6.2, FR-6.3, FR-6.4

**User-Facing:** NO

**Files:**
- Modify: `services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/service/AuthorizationService.java`

- [ ] **Step 1: Add user type permission validation method**

Add method to validate role permissions match user type:
- INTERNAL users: permissions with "user:", "role:", "audit:", "backoffice:" prefixes allowed
- EXTERNAL users: permissions with "transaction:", "agent:", "merchant:", "kyc:" prefixes allowed

- [ ] **Step 2: Modify role assignment to validate**

In `assignRole` method, add validation before assignment.

- [ ] **Step 3: Run build**

```bash
cd services/auth-iam-service && ./mvnw compile -q
```

- [ ] **Step 4: Commit**

```bash
git add services/auth-iam-service/src/main/java/com/agentbanking/auth/domain/service/AuthorizationService.java
git commit -m "domain: add user type permission validation for role assignment"
```

---

## Phase 8: Integration Tests

### Task 21: Write integration tests

**BDD Scenarios:** All scenarios should have corresponding tests

**BRD Requirements:** All FRs

**User-Facing:** NO

**Files:**
- Create: `services/auth-iam-service/src/test/java/com/agentbanking/auth/domain/service/PasswordResetServiceTest.java`
- Create: `services/auth-iam-service/src/test/java/com/agentbanking/auth/domain/service/UserManagementServiceTest.java`
- Create: `services/onboarding-service/src/test/java/com/agentbanking/onboarding/domain/service/AgentUserCreationTest.java`

- [ ] **Step 1: Write PasswordResetServiceTest**

Test: OTP generation, storage, verification, attempt limits

- [ ] **Step 2: Write UserManagementServiceTest**

Test: User creation with userType, temp password generation, mustChangePassword flag

- [ ] **Step 3: Write AgentUserCreationTest**

Test: Feign call, Kafka fallback, idempotency

- [ ] **Step 4: Run tests**

```bash
cd services/auth-iam-service && ./mvnw test -q
cd services/onboarding-service && ./mvnw test -q
```

- [ ] **Step 5: Commit**

```bash
git add services/auth-iam-service/src/test/java/com/agentbanking/auth/domain/service/PasswordResetServiceTest.java
git add services/auth-iam-service/src/test/java/com/agentbanking/auth/domain/service/UserManagementServiceTest.java
git add services/onboarding-service/src/test/java/com/agentbanking/onboarding/domain/service/AgentUserCreationTest.java
git commit -m "test: add integration tests for agent user management"
```

---

## Summary

| Phase | Task | Description |
|-------|------|-------------|
| 1 | 1-2 | Database migrations |
| 2 | 3-4 | Domain models (UserType, UserRecord) |
| 3 | 5-7 | JPA entities |
| 4 | 8-10 | Outbound ports (OtpStore, NotificationPublisher) |
| 5 | 11-14 | Domain services |
| 6 | 15-16 | REST controllers |
| 7 | 17-20 | Onboarding integration |
| 8 | 21 | Tests |
