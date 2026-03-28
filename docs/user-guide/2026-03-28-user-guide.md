# Agent Banking Platform - User Guide

> **Version:** 1.0  
> **Last Updated:** March 2026

## Table of Contents

1. [Platform Overview](#platform-overview)
2. [Getting Started](#getting-started)
3. [Backoffice User Guide](#backoffice-user-guide)
4. [IT Administrator Guide](#it-administrator-guide)
5. [Transaction Services](#transaction-services)
6. [API Reference](#api-reference)
7. [Troubleshooting](#troubleshooting)

---

## Platform Overview

The **Agent Banking Platform** is a microservices-based banking solution enabling third-party agents to provide banking services. This implementation covers the backend services and backoffice administration UI.

### Implemented Components

| Component | Description | Status |
|-----------|-------------|--------|
| **Rules Service** | Fee configuration, velocity checks, limit enforcement | ✅ Implemented |
| **Ledger Service** | Float management, transactions, settlements | ✅ Implemented |
| **Onboarding Service** | Agent registration, KYC verification | ✅ Implemented |
| **Switch Adapter** | Card/ATM network integration | ✅ Implemented |
| **Biller Service** | Bill payments, top-ups, e-wallet | ✅ Implemented |
| **Orchestrator Service** | Saga-based transaction orchestration | ✅ Implemented |
| **Backoffice UI** | Admin dashboard (React) | ✅ Implemented |

### Not in Scope (Channel Layer)

- POS Terminal (Android/Flutter) applications
- Direct customer terminal operations

---

## Getting Started

### Access Points

| Interface | URL | Purpose |
|-----------|-----|---------|
| **Backoffice UI** | http://localhost:3000 | Admin operations |
| **API Gateway** | http://localhost:8080 | Service API |

### Default Ports

| Service | Port |
|---------|------|
| Gateway | 8080 |
| Rules Service | 8081 |
| Ledger Service | 8082 |
| Onboarding Service | 8083 |
| Switch Adapter | 8084 |
| Biller Service | 8085 |
| Orchestrator | 8086 |
| PostgreSQL (Rules) | 5433 |
| PostgreSQL (Ledger) | 5434 |
| PostgreSQL (Onboarding) | 5435 |
| PostgreSQL (Switch) | 5436 |
| PostgreSQL (Biller) | 5437 |
| PostgreSQL (Orchestrator) | 5438 |
| Redis | 6379 |
| Kafka | 9092 |

---

## Backoffice User Guide

The Backoffice UI provides administrative functions for bank operations staff.

### Accessing Backoffice

1. Open browser to `http://localhost:3000`
2. Login with admin credentials
3. Navigate using the sidebar

### Dashboard

The dashboard provides an overview of platform operations.

**Metrics Displayed:**
- Total Agents
- Today's Transaction Volume
- Transaction Count
- Pending KYC Reviews

**Features:**
- Transaction trend chart (weekly)
- Quick navigation to modules

---

### Agent Management (US-BO01)

Manage agent registration and profiles.

#### List Agents

View all registered agents with filtering options.

| Filter | Description |
|--------|-------------|
| Status | Active, Inactive, Suspended |
| Tier | MICRO, STANDARD, PREMIER |
| Date Range | Registration date |

#### Create Agent

Register a new agent location.

**Required Fields:**
- Agent Code (unique identifier)
- Business Name
- Agent Tier
- Merchant GPS Coordinates
- Phone Number

#### Edit Agent

Update agent details:
- Business information
- Tier changes
- GPS coordinates

#### Deactivate Agent

Suspend agent operations:
1. Select agent from list
2. Click Deactivate
3. Confirm action

---

### Transaction Monitoring (US-BO02)

View and search all financial transactions.

#### Transaction List

| Column | Description |
|--------|-------------|
| Transaction ID | Unique identifier |
| Date/Time | Transaction timestamp |
| Agent | Agent code and name |
| Type | Transaction category |
| Amount | Transaction value (MYR) |
| Status | Completed, Pending, Failed |

#### Search Filters

- **Date Range:** Custom date selection
- **Agent:** Filter by agent
- **Type:** Transaction category
- **Status:** Transaction status
- **Amount Range:** Min/Max values

#### Transaction Details

Click any transaction to view full details:
- Customer information (masked)
- Fee breakdown
- Commission details
- Geofence data
- Switch reference

---

### Settlement Management (US-BO03)

Daily settlement and reconciliation.

#### Settlement Summary

| Field | Description |
|-------|-------------|
| Settlement Date | Business date |
| Total Deposits | Sum of all deposits |
| Total Withdrawals | Sum of all withdrawals |
| Net Settlement | Deposits - Withdrawals + Commission |
| Agent Count | Number of settled agents |
| Status | FINAL, PENDING, DISPUTED |

#### Generate Settlement File

Generate CSV file for CBS upload:

1. Select date
2. Click "Generate Report"
3. Download CSV file

#### Reconciliation

View reconciliation status per agent:

| Status | Meaning |
|--------|---------|
| MATCHED | Records align |
| DISCREPANCY | Amount mismatch |
| PENDING | Awaiting CBS confirmation |

#### Dispute Cases

Handle settlement discrepancies:

1. View discrepancy details
2. Investigate root cause
3. Process adjustment or rejection

---

### KYC Review Queue (US-BO04)

Review pending identity verifications.

#### Review Queue

| Field | Description |
|-------|-------------|
| Reference ID | Verification ID |
| MyKad Number | Encrypted (viewable by authorized) |
| Submitted Date | Application date |
| Status | PENDING, UNDER_REVIEW |

#### KYC Verification Steps

1. **MyKad Verification** - Validates identity via JPN
2. **Biometric Match** - Fingerprint matching
3. **AML Screening** - Anti-money laundering check
4. **Decision** - Auto-approve or Manual review

#### Actions

| Action | Description |
|--------|-------------|
| **Approve** | Accept agent registration |
| **Reject** | Decline with reason |
| **Request Info** | Ask for additional documents |

---

### Configuration (US-BO05)

System parameter management.

#### Fee Configuration

Configure transaction fees per agent tier.

**Fee Types:**
- FIXED - Flat fee per transaction
- PERCENTAGE - Percentage of transaction amount

**Configuration Fields:**
- Transaction Type
- Agent Tier
- Customer Fee
- Agent Commission
- Bank Share
- Daily Limit Amount
- Daily Limit Count

#### Velocity Rules

Configure fraud prevention limits.

**Rule Types:**
- Max Amount per Transaction
- Max Amount per Day (per MyKad)
- Max Count per Day (per MyKad)

---

### Audit Logs (US-BO06)

System audit trail for compliance.

#### Log Fields

| Field | Description |
|-------|-------------|
| Timestamp | Action date/time |
| User | Who performed action |
| Action | What was done |
| Resource | Affected entity |
| Details | Additional context |

#### Export

Download audit logs for external review:
- CSV format
- Date range selection

---

## IT Administrator Guide

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Backoffice (React)                       │
│                    http://localhost:3000                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   API Gateway (Spring Cloud)                 │
│                    http://localhost:8080                     │
└─────────────────────────────────────────────────────────────┘
          │        │        │        │        │        │
    ┌─────┴────┐ ┌──┴──┐ ┌──┴──┐ ┌──┴──┐ ┌──┴──┐ ┌──┴──┐
    │  Rules   │ │Ledger│ │ On-  │ │Switch│ │Biller│ │Orch-│
    │ :8081    │ │:8082 │ │board │ │:8084 │ │:8085 │ │estr │
    │          │ │      │ │:8083 │ │      │ │      │ │:8086│
    └─────┬────┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘
          │        │        │        │        │        │
          ▼        ▼        ▼        ▼        ▼        ▼
    ┌──────────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐
    │PostgreSQL│ │Redis  │ │Kafka  │ │Postgr-│ │Postgr-│ │Postgr-│
    │ :5433    │ │:6379  │ │:9092  │ │eSQL   │ │eSQL   │ │eSQL   │
    │          │ │Cache/ │ │Events │ │:5434  │ │:5435  │ │:5436+ │
    └──────────┘ │Idempot│ │       │ │       │ │       │ │       │
                 └───────┘ └───────┘ └───────┘ └───────┘ └───────┘
```

### Service Dependencies

| Service | Dependencies |
|---------|--------------|
| Rules Service | PostgreSQL (rules_db) |
| Ledger Service | PostgreSQL (ledger_db), Redis, Kafka, Rules, Switch |
| Onboarding | PostgreSQL (onboarding_db) |
| Switch Adapter | PostgreSQL (switch_db), Mock Server |
| Biller Service | PostgreSQL (biller_db), Mock Server |
| Orchestrator | PostgreSQL (orchestrator_db), Redis, Kafka, Ledger, Rules, Switch |

### Docker Management

#### Start All Services

```bash
docker-compose --profile all up -d
```

#### Stop All Services

```bash
docker-compose --profile all down
```

#### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f ledger-service
```

#### Health Checks

All services expose health endpoints:
```bash
curl http://localhost:8081/actuator/health
```

### Database Management

#### Connect to PostgreSQL

```bash
# Rules DB
docker exec -it agentbanking-backend-postgres-rules-1 psql -U postgres -d rules_db

# Ledger DB
docker exec -it agentbanking-backend-postgres-ledger-1 psql -U postgres -d ledger_db
```

#### Run Flyway Migrations

Migrations run automatically on service startup. Manual run:

```bash
# Via service
docker exec -it agentbanking-backend-rules-service-1 java -jar app.jar
```

### Redis Operations

#### Access Redis CLI

```bash
docker exec -it agentbanking-backend-redis-1 redis-cli
```

#### Common Commands

```bash
# Check keys
KEYS *

# Get value
GET <key>

# Delete key
DEL <key>
```

### Kafka Management

#### List Topics

```bash
docker exec -it agentbanking-backend-kafka-1 kafka-topics --list --bootstrap-server localhost:9092
```

#### View Consumer Groups

```bash
docker exec -it agentbanking-backend-kafka-1 kafka-consumer-groups --list --bootstrap-server localhost:9092
```

### Configuration Management

#### Application Properties

Each service has `application.yaml` in:
```
services/<service>/src/main/resources/application.yaml
```

#### Environment Variables (Docker)

Set in `docker-compose.yml`:
```yaml
environment:
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-rules:5432/rules_db
  - SPRING_DATA_REDIS_HOST=redis
```

### Monitoring

#### Service Health

| Service | Health Endpoint |
|---------|-----------------|
| Rules | http://localhost:8081/actuator/health |
| Ledger | http://localhost:8082/actuator/health |
| Onboarding | http://localhost:8083/actuator/health |
| Switch | http://localhost:8084/actuator/health |
| Biller | http://localhost:8085/actuator/health |
| Orchestrator | http://localhost:8086/actuator/health |

### Backup & Recovery

#### Database Backup

```bash
# Backup specific database
docker exec agentbanking-backend-postgres-ledger-1 pg_dump -U postgres ledger_db > ledger_backup.sql
```

#### Restore Database

```bash
docker exec -i agentbanking-backend-postgres-ledger-1 psql -U postgres ledger_db < ledger_backup.sql
```

---

## Transaction Services

### Rules Service

**Purpose:** Fee calculation, velocity checks, limit enforcement

#### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/internal/fees/{transactionType}/{agentTier}` | GET | Get fee configuration |
| `/internal/velocity/check` | POST | Check velocity limits |
| `/internal/limit/check` | POST | Check daily limits |
| `/internal/stp/evaluate` | POST | Evaluate STP decision |

#### Fee Calculation

```
Customer Fee = Transaction Amount × Fee Percentage (or Fixed)
Agent Commission = Fee × Commission Split
Bank Share = Fee - Agent Commission
```

---

### Ledger Service

**Purpose:** Float management, transaction processing, settlement

#### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/ledger/withdrawal` | POST | Process cash withdrawal |
| `/api/ledger/deposit` | POST | Process cash deposit |
| `/api/ledger/balance/{agentId}` | GET | Get float balance |
| `/api/ledger/transactions` | GET | List transactions |
| `/api/ledger/settlement/summary` | GET | Settlement summary |
| `/api/ledger/reconciliation` | GET | Reconciliation status |

#### Transaction Flow

1. Idempotency check (Redis)
2. Validate agent float
3. Check geofence
4. Process transaction
5. Create journal entries
6. Publish EFM event

---

### Onboarding Service

**Purpose:** Agent registration, KYC verification

#### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/onboarding/agents` | GET/POST | List/Create agents |
| `/api/onboarding/agents/{id}` | GET/PUT | Get/Update agent |
| `/api/onboarding/agents/{id}/deactivate` | POST | Deactivate agent |
| `/api/onboarding/kyc/verify-mykad` | POST | Verify MyKad |
| `/api/onboarding/kyc/biometric-match` | POST | Biometric match |
| `/api/onboarding/audit-logs` | GET | Audit logs |

---

### Switch Adapter Service

**Purpose:** Card/ATM network integration (ISO 8583)

#### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/switch/authorize` | POST | Authorize card transaction |
| `/api/switch/balance-inquiry` | POST | Balance inquiry |
| `/api/switch/reversal` | POST | Transaction reversal |

#### Supported Operations

- Card authorization
- PIN verification
- Account enquiry
- Reversal (Store & Forward)

---

### Biller Service

**Purpose:** Bill payments, top-ups, e-wallet

#### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/biller/validate` | POST | Validate bill reference |
| `/api/biller/pay` | POST | Process bill payment |
| `/api/biller/topup` | POST | Prepaid top-up |
| `/api/biller/ewallet/withdrawal` | POST | E-wallet cash out |
| `/api/biller/ewallet/topup` | POST | E-wallet top-up |

#### Supported Billers

- JomPAY
- Astro RPN
- TM RPN
- EPF (i-SARAAN/i-SURI)
- CELCOM Prepaid
- M1 Prepaid
- Sarawak Pay

---

### Orchestrator Service

**Purpose:** Saga-based transaction coordination

#### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/orchestrator/withdrawal` | POST | Complete withdrawal saga |
| `/api/orchestrator/deposit` | POST | Complete deposit saga |

#### Saga Steps

1. Rules validation
2. Float reservation
3. Switch authorization
4. Float commit
5. Event publishing

---

## API Reference

### Authentication

All API requests require JWT token in header:
```
Authorization: Bearer <token>
```

### Common Headers

| Header | Value |
|--------|-------|
| Content-Type | application/json |
| Authorization | Bearer <token> |
| X-Idempotency-Key | <unique-key> |

### Response Format

**Success:**
```json
{
  "status": "SUCCESS",
  "data": { ... }
}
```

**Error:**
```json
{
  "status": "FAILED",
  "error": {
    "code": "ERR_xxx",
    "message": "Human readable message",
    "action_code": "DECLINE|RETRY|REVIEW",
    "trace_id": "distributed-trace-id"
  }
}
```

### Error Codes

| Code | Description | Action |
|------|-------------|--------|
| ERR_INSUFFICIENT_FLOAT | Agent float too low | RETRY after top-up |
| ERR_GPS_UNAVAILABLE | No GPS coordinates | DECLINE |
| ERR_GEOFENCE_VIOLATION | Outside merchant location | DECLINE |
| ERR_INVALID_CURRENCY | Non-MYR currency | DECLINE |
| ERR_SWITCH_DECLINED | Card network rejected | DECLINE |
| ERR_VELOCITY_EXCEEDED | Daily limit hit | RETRY tomorrow |
| ERR_KYC_PENDING | KYC not approved | REVIEW |
| ERR_AML_FLAGGED | AML check failed | REVIEW |

---

## Troubleshooting

### Service Won't Start

1. Check dependencies running:
   ```bash
   docker ps
   ```

2. Check logs:
   ```bash
   docker-compose logs <service-name>
   ```

3. Verify database connectivity

### Database Connection Errors

1. Check PostgreSQL running:
   ```bash
   docker exec -it agentbanking-backend-postgres-ledger-1 pg_isready
   ```

2. Verify credentials in application.yaml

### High Latency

1. Check Kafka consumer lag:
   ```bash
   docker exec -it agentbanking-backend-kafka-1 kafka-consumer-groups --bootstrap-server localhost:9092 --group <group> --describe
   ```

2. Check Redis memory:
   ```bash
   docker exec -it agentbanking-backend-redis-1 redis-cli INFO memory
   ```

### Transaction Failures

1. Check idempotency cache:
   ```bash
   docker exec -it agentbanking-backend-redis-1 redis-cli KEYS "*idempotency*"
   ```

2. Review EFM events for fraud flags

---

## Appendices

### Appendix A: Service URLs

| Service | Internal | External (via Gateway) |
|---------|----------|------------------------|
| Rules | http://rules-service:8081 | http://localhost:8080 |
| Ledger | http://ledger-service:8082 | http://localhost:8080 |
| Onboarding | http://onboarding-service:8083 | http://localhost:8080 |
| Switch | http://switch-adapter-service:8084 | http://localhost:8080 |
| Biller | http://biller-service:8085 | http://localhost:8080 |
| Orchestrator | http://orchestrator-service:8086 | http://localhost:8080 |

### Appendix B: Kafka Topics

| Topic | Purpose |
|-------|---------|
| transaction-events | Transaction lifecycle events |
| reversal-events | Reversal notifications |
| efm-events | Fraud monitoring events |
| settlement-events | Settlement triggers |

### Appendix C: Redis Keys Pattern

| Pattern | Purpose |
|---------|---------|
| `idempotency:*` | Idempotency cache |
| `float:*` | Agent float cache |
| `session:*` | User session data |

### Appendix D: Support

| Issue | Contact |
|-------|---------|
| Technical | support@agentbanking.com |
| Fraud | fraud@agentbanking.com |
| Help Desk | +603-XXXX-XXXX |
