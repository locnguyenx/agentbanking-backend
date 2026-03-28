# Ledger Service Enhancements Implementation Plan (US-L06, US-L08)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Enhance Ledger Service to support MyKad-based withdrawals (US-L06) and card-based deposits (US-L08) as specified in the revised BRD v1.1.

**Architecture:** Build upon existing Ledger Service hexagonal architecture. Extend TransactionType enum, update domain services and use cases to handle new transaction types.

**Tech Stack:** Java 21, Spring Boot 3.2.5, Spring Data JPA, PostgreSQL, Redis, Kafka, OpenFeign, Resilience4j, JUnit 5, Mockito.

---

## ✅ Task 1: Domain Layer - Extend Transaction Models

**BDD Scenarios:** BDD-L06, BDD-L08 (MyKad withdrawal, Card deposit)  
**BRD Requirements:** US-L06, US-L08  
**User-Facing:** NO  

**Files:**
- TransactionType.java already includes MYKAD_WITHDRAWAL, CARD_DEPOSIT
- No new files needed

### Step 1: Ensure TransactionType includes required values

✅ Verified - TransactionType.java contains:
```java
MYKAD_WITHDRAWAL,  // US-L06
CARD_DEPOSIT       // US-L08
```

### Step 2: Extend LedgerService with new transaction methods

✅ Created `processWithdrawal` and `processDeposit` methods with full transaction lifecycle:
- Idempotency check
- Float lock (PESSIMISTIC_WRITE)
- Transaction recording
- Double-entry journal entries
- Geofence validation (withdrawal)

### Step 3: Commit

✅ Implementation complete and files committed

---

## ✅ Task 2: Application Layer - New Use Cases

**BDD Scenarios:** BDD-L06, BDD-L08  
**BRD Requirements:** US-L06, US-L08  
**User-Facing:** YES  

**Files Created:**
- `ProcessMyKadWithdrawalUseCase.java` (inbound port)
- `ProcessCardDepositUseCase.java` (inbound port)
- `ProcessMyKadWithdrawalUseCaseImpl.java` (delegates to LedgerService)
- `ProcessCardDepositUseCaseImpl.java` (delegates to LedgerService)

### Step 1-3: Complete

✅ Both use cases implemented to delegate to LedgerService transaction methods
✅ Basic TransactionResult DTOs defined within use case interfaces
✅ Compiled successfully

---

## ✅ Task 3: Infrastructure Layer - REST Controllers

**BDD Scenarios:** BDD-L06, BDD-L08  
**BRD Requirements:** US-L06, US-L08  
**User-Facing:** YES (external API via Gateway)  

**Files Created:**
- `MyKadWithdrawalController.java` - REST endpoint for `/internal/withdrawals/mykad`
- `CardDepositController.java` - REST endpoint for `/internal/deposits/card`

### Step 1-3: Complete

✅ Controllers created with request validation
✅ Endpoints exposed under `/internal/withdrawals/mykad` and `/internal/deposits/card`
✅ Input validation and exception handling

---

## ✅ Task 4: Infrastructure - Geofence Validation Integration

**BDD Scenarios:** BDD-L06-EC-05 (outside geofence), BDD-L06-EC-06 (GPS unavailable)  
**BRD Requirements:** NFR-4.2, FR-3.3 (applies to MyKad withdrawal)  
**User-Facing:** NO  

### Step 1: Verify geofence validation is integrated

✅ Geofence validation already implemented in LedgerService.processWithdrawal():
- Uses `GeofenceChecker.isWithinGeofence()` with 100m radius
- Throws `LedgerException` with `ERR_GEOFENCE_VIOLATION` or `ERR_GPS_UNAVAILABLE`
- GPS coordinates required for withdrawal transactions

### Step 2: Complete

---

## ✅ Task 5: Unit Tests [DONE]

**BDD Scenarios:** BDD-L06, BDD-L06-EC-01 through EC-08, BDD-L08, BDD-L08-EC-01 through EC-04
**BRD Requirements:** US-L06, US-L08
**User-Facing:** NO

**Files:**
- ✅ `ProcessMyKadWithdrawalUseCaseImplTest.java` - rewritten to match current APIs
- ✅ `ProcessCardDepositUseCaseImplTest.java` - rewritten to match current APIs
- ✅ `LedgerServiceTest.java` - updated for new constructor and transaction methods

**Status:** All ledger-service tests pass. Verified with `./gradlew :services:ledger-service:test`.