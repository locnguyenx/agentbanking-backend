# Technical Design: IT Administrator UI

**Date:** 2026-04-05
**Status:** Approved

## 1. Architecture Overview

### Component Architecture
```
Backoffice UI (React)
├── Health Dashboard (auto-refresh, color-coded cards)
├── Metrics Viewer (drill-down from health cards)
└── Audit Log Table (filterable, paginated, exportable)
        ↓
Spring Cloud Gateway
├── /api/v1/admin/health/all → Aggregates all service health
├── /api/v1/admin/health/{service} → {service}/actuator/health
├── /api/v1/admin/metrics/{service} → {service}/actuator/metrics
└── /api/v1/admin/audit-logs → audit-service:8088/audit/logs
        ↓
Domain Services + Audit Service
├── Services expose actuator endpoints + publish audit events to Kafka
└── Audit Service consumes Kafka → PostgreSQL
```

## 2. Frontend Design

### New Files
```
backoffice/src/
├── pages/SystemAdmin.tsx
├── components/
│   ├── HealthCard.tsx
│   ├── MetricsPanel.tsx
│   └── AuditLogTable.tsx
└── api/client.ts (add admin endpoints)
```

### Navigation Changes
- Add `System Administration` menu item with `Settings` icon
- Route: `/system-admin`
- UI Structure: Single page with three sections (Health Dashboard, Metrics Panel, Audit Logs) displayed as scrollable sections with anchor navigation

### Key Components

**HealthCard:**
- Props: serviceName, port, purpose, status, lastChecked, onDrillDown
- Status badges: badge-success (UP), badge-error (DOWN), badge-warning (DEGRADED or UNREACHABLE)
- Auto-refresh: 30s interval with cleanup

**MetricsPanel:**
- Props: serviceName, metrics, onRefresh
- Metrics: JVM (heap, non-heap, threads, CPU, uptime), HTTP (requests, errors, avg response time)
- Displays current point-in-time values from actuator

**AuditLogTable:**
- Features: Pagination, sorting, date range picker, service filter, free-text search, CSV export
- Columns: Timestamp, User ID, Action, Resource, IP, Result, Service Source, Failure Reason

## 3. Backend Design

### New Service: audit-service (Port 8088)

**Structure:**
```
services/audit-service/
├── domain/
│   ├── model/AuditLogRecord.java
│   ├── port/in/QueryAuditLogsUseCase.java
│   ├── port/out/AuditLogRepository.java
│   └── service/AuditLogQueryService.java
├── application/QueryAuditLogsApplication.java
├── infrastructure/
│   ├── web/AuditLogController.java, HealthAggregationController.java
│   ├── persistence/JpaAuditLogEntity.java, JpaAuditLogRepository.java
│   ├── external/ServiceHealthFeignClient.java (per service)
│   └── messaging/KafkaAuditLogConsumer.java
├── service/HealthAggregationService.java
└── config/DomainServiceConfig.java, KafkaConsumerConfig.java
```

Note: `HealthAggregationService` is placed in `service/` (not `domain/service/`) because it orchestrates external HTTP calls to other services' actuator endpoints, which is an infrastructure concern. It is registered as a bean in `DomainServiceConfig.java` per Law V.

### Gateway Routes

**Custom GatewayFilterFactory Implementation:**
- Location: `gateway/src/main/java/com/agentbanking/gateway/filter/ServiceRouteGatewayFilterFactory.java`
- Contract: Extends `AbstractGatewayFilterFactory`, resolves `{service}` path variable to target URI from static config map
- Unknown service: Returns 404 with error code `ERR_SYS_001` ("Unknown service: {service}")
- The filter rewrites the request path to `/{service}/actuator/health` or `/{service}/actuator/metrics` and sets the target URI

```yaml
# Health check - aggregate all services (via audit-service)
- id: admin-health-all
  uri: http://audit-service:8088
  predicates: Path=/api/v1/admin/health/all
  filters:
    - JwtAuth
    - RewritePath=/api/v1/admin/health/all, /admin/health/all

# Health check - individual service (custom filter resolves service name)
- id: admin-health-service
  uri: http://gateway:8080
  predicates: Path=/api/v1/admin/health/{service}
  filters:
    - JwtAuth
    - ServiceRoute  # Custom filter: resolves {service} → service:port/actuator/health

# Metrics - individual service (same custom filter, different path suffix)
- id: admin-metrics
  uri: http://gateway:8080
  predicates: Path=/api/v1/admin/metrics/{service}
  filters:
    - JwtAuth
    - ServiceRoute  # Custom filter: resolves {service} → service:port/actuator/metrics

# Audit logs
- id: admin-audit-logs
  uri: http://audit-service:8088
  predicates: Path=/api/v1/admin/audit-logs
  filters:
    - JwtAuth
    - RewritePath=/api/v1/admin/audit-logs, /audit/logs
```

**Service Name Mapping** (used by custom GatewayFilterFactory):
| Path Param | Target Service | URI |
|------------|---------------|-----|
| gateway | API Gateway | http://gateway:8080 |
| rules | Rules Service | http://rules-service:8081 |
| ledger | Ledger Service | http://ledger-service:8082 |
| onboarding | Onboarding Service | http://onboarding-service:8083 |
| switch | Switch Adapter | http://switch-adapter-service:8084 |
| biller | Biller Service | http://biller-service:8085 |
| orchestrator | Orchestrator Service | http://orchestrator-service:8086 |
| auth | Auth/IAM Service | http://auth-iam-service:8087 |
| audit | Audit Service | http://audit-service:8088 |
| mock | Mock Server | http://mock-server:8089 |
| postgresql | PostgreSQL | (direct TCP check on port 5432) |
| redis | Redis | (direct TCP check on port 6379) |
| kafka | Kafka | (direct TCP check on port 9092) |

### Kafka Configuration
- Topic: `audit-logs`
- Partitions: 3, Replication: 1 (dev), 3 (prod)
- Retention: 7 days (Kafka), 90 days (PostgreSQL)

**Producer Services** (all services must publish audit events):
- auth-iam-service (user actions, login attempts, permission changes)
- onboarding-service (agent creation, KYC reviews)
- ledger-service (transactions, float changes, settlements)
- rules-service (rule changes, limit updates)
- switch-adapter-service (payment network interactions)
- biller-service (bill payments, PIN purchases)
- orchestrator-service (transaction saga events)

**Integration Approach:** Each service adds a Kafka producer that publishes audit events after domain actions. A shared `AuditEventPublisher` component in the `common` module provides a standard interface for services to publish events. Services integrate by:
1. Adding `common` module dependency (already present)
2. Calling `auditEventPublisher.publish(event)` after domain actions
3. Resilience: Failed publishes log error and retry via Spring Kafka's built-in retry (no blocking of domain operations)

### API Response DTOs

**HealthAggregationResponse:**
```json
{
  "services": [
    {
      "name": "gateway",
      "port": 8080,
      "purpose": "External API entry point",
      "status": "UP",
      "lastChecked": "2026-04-05T10:30:00+08:00",
      "details": {}
    }
  ],
  "summary": { "total": 13, "healthy": 12, "unhealthy": 1 },
  "timestamp": "2026-04-05T10:30:00+08:00"
}
```

**ServiceMetricsResponse:**
```json
{
  "serviceName": "ledger-service",
  "jvm": {
    "memoryUsedMb": 256.5,
    "memoryMaxMb": 512.0,
    "threadsActive": 45,
    "cpuUsagePercent": 12.3,
    "uptimeSeconds": 86400
  },
  "http": {
    "requestsTotal": 15000,
    "errorsTotal": 23,
    "avgResponseTimeMs": 45.2
  },
  "timestamp": "2026-04-05T10:30:00+08:00"
}
```

**AuditLogQueryResponse:**
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 1500,
  "totalPages": 75
}
```

### Audit Event Schema
```json
{
  "auditId": "uuid",
  "serviceName": "auth-iam-service",
  "entityType": "USER",
  "entityId": "uuid",
  "action": "USER_CREATED",
  "performedBy": "user-id",
  "ipAddress": "192.168.1.1",
  "timestamp": "2026-04-05T10:30:00+08:00",
  "outcome": "SUCCESS",
  "failureReason": null,
  "changes": "{\"field\": \"old_value\", \"newField\": \"new_value\"}"
}
```

**Field Notes:**
- `entityId`: The primary identifier of the affected resource (maps to `entity_id` in DB). For user creation, this is the user UUID.
- `changes`: JSON string representing key-value pairs of what changed (nullable for read-only actions)

## 4. Database Schema

### audit_logs Table

**Flyway Migration:** `V1__init_audit_schema.sql`

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

-- Indexes for common query patterns
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_logs_service ON audit_logs(service_name);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_performed_by ON audit_logs(performed_by);
CREATE INDEX idx_audit_logs_outcome ON audit_logs(outcome);
```

## 5. Data Flow

### Health Check
UI → GET /api/v1/admin/health/all → Gateway → audit-service aggregates health from all services → UI

### Metrics
UI → GET /api/v1/admin/metrics/{service} → Gateway (custom filter resolves service) → Service /actuator/metrics → UI

### Audit Logs
Services → Kafka (audit-logs topic) → audit-service consumer → PostgreSQL → UI queries via API

## 6. Access Control

All admin endpoints (`/api/v1/admin/*`) require:
1. Valid JWT token (validated by gateway JwtAuth filter)
2. User must have `IT_ADMIN` role (gateway validates role claim, passes via `X-User-Roles` header)
3. Non-IT_ADMIN users receive 403 Forbidden

## 7. Error Handling & Resilience

### Health Check Resilience (NFR-7)
- Health aggregation uses Resilience4j circuit breaker per service
- Circuit breaker config: failure threshold 50%, wait duration 30s, sliding window 10 calls
- Timeout per service health check: 5 seconds
- If circuit is open: service displays "DEGRADED" with amber indicator
- If timeout: service displays "UNREACHABLE" with amber indicator

### Kafka Producer Resilience
- Spring Kafka built-in retry: 3 attempts with exponential backoff (1s, 2s, 4s)
- Failed events after retries: logged with ERROR level (no dead-letter queue for MVP)
- Domain operations must NOT block on audit event publish failures

### Kafka Consumer Resilience (NFR-8)
- Consumer retry with exponential backoff on processing failures
- Failed events after max retries: sent to `audit-logs-dlq` topic for manual investigation
- Consumer offset committed only after successful persistence

### API Error Responses
- Metrics unavailable: Error toast with retry option
- Audit query failure: Error message with trace ID (from Global Error Schema)

## 8. Testing Strategy
- Frontend: Unit tests for components, integration tests for SystemAdmin page
- Backend: Unit tests for AuditLogQueryService, Kafka consumer tests, ArchUnit compliance
- Contract tests for admin API endpoints
- RBAC tests: Verify non-IT_ADMIN users are denied access to admin endpoints
