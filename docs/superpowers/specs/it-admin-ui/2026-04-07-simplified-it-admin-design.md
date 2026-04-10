# Technical Design: Removing audit-service (Simplified IT Admin)

**Date:** 2026-04-07
**Status:** Draft
**Supersedes:** `2026-04-05-it-admin-ui-design.md`

---

## 1. Problem Statement

Current audit-service has issues:
1. **Slow Health Dashboard** - sequential HTTP calls to each service (5s timeout × 9 services = 45s+)
2. **No Audit Data** - Kafka producers not implemented in services, so no events flow to audit-service
3. **Complex Architecture** - separate service, Kafka consumer, separate DB adds operational overhead

---

## 2. Proposed Solution: Gateway Direct + Service-Local Audit

### Architecture Overview

```
Backoffice UI (React)
├── Health Dashboard → Gateway → Direct parallel calls to each service's /actuator/health
├── Metrics Panel → Gateway → Direct calls to each service's /actuator/metrics
└── Audit Logs → Gateway → Routes to appropriate service's /internal/audit-logs
        ↓
    Each Service (has own audit_logs table)
    └── Writes audit events directly to local DB via JPA (no Kafka)
```

### Key Changes

| Component | Current | New |
|-----------|---------|-----|
| Health Aggregation | audit-service sequential HTTP | Gateway parallel HTTP (already fixed) |
| Metrics Aggregation | audit-service HTTP | Gateway direct (reuse filter) |
| Audit Logs | Kafka → audit-service | Each service writes to local DB |
| Audit Query | audit-service only | Gateway routes to service by filter |

---

## 3. Implementation

### 3.1 Health & Metrics (Gateway)

The `ServiceRouteGatewayFilterFactory` already exists in gateway. We'll enhance it to:

1. **Parallel Health Aggregation** - Add new `/admin/health/all` endpoint in gateway that calls all services in parallel
2. **Metrics Proxy** - Existing `/admin/metrics/{service}` already works via ServiceRoute filter

**Changes to gateway:**
- Add `AdminHealthController` with parallel health aggregation
- Reuse existing `ServiceRouteGatewayFilterFactory` for metrics

```java
// Gateway: AdminHealthController.java
@GetMapping("/admin/health/all")
public Mono<Map<String, Object>> aggregateHealth() {
    // Parallel calls to all services' /actuator/health
    // Timeout: 3s per service, total max: 5s
    // Uses CompletableFuture for parallel execution
}
```

### 3.2 Audit Logs (Service-Local)

Each service maintains its own `audit_logs` table and writes directly via JPA.

**Services needing audit_logs table:**
- `auth-iam-service` - already has (V1__init_auth_schema.sql)
- `onboarding-service` - already has (V4_create_audit_log_table.sql)
- `ledger-service` - add migration
- `rules-service` - add migration
- `biller-service` - add migration
- `switch-adapter-service` - add migration
- `orchestrator-service` - add migration

**Each service implements:**
1. Domain: `AuditLogRecord` (already in common)
2. Port: `AuditLogRepository` interface
3. Service: `AuditService.write(event)` - called after domain actions
4. Infrastructure: JPA entity + repository implementation
5. Controller: `/internal/audit-logs` endpoint for querying

**Gateway routes audit queries:**
```yaml
# Route audit queries to service based on service param
- id: admin-audit-logs
  uri: http://gateway:8080
  predicates:
    - Path=/api/v1/admin/audit-logs
  filters:
    - JwtAuth
    - ServiceRoute  # Resolves service param to target service
```

### 3.3 Audit Event Publishing

Services publish audit events by calling local `AuditService.write()` after domain actions:

```java
// Example: In auth-iam-service after user creation
@PostMapping("/users")
public ResponseEntity<?> createUser(@RequestBody CreateUserRequest req) {
    // ... domain logic ...
    auditService.write(AuditEvent.userCreated(userId, performedBy, ipAddress));
    return ResponseEntity.ok(user);
}
```

**No Kafka needed** - direct JPA write is simple and reliable.

---

## 4. Data Flow

### Health Check (Fast)
```
UI → GET /api/v1/admin/health/all
  → Gateway parallel HTTP to all 9 services /actuator/health (5s timeout each)
  → Response in <5s (not 45s+)
```

### Metrics (Fast)
```
UI → GET /api/v1/admin/metrics/gateway
  → ServiceRoute filter resolves "gateway" → http://gateway:8080
  → GET /actuator/metrics
  → Response in <3s
```

### Audit Logs (Service-Local)
```
UI → GET /api/v1/admin/audit-logs?service=auth&page=0&size=20
  → Gateway routes to auth-iam-service
  → GET /internal/audit-logs?page=0&size=20
  → auth-iam-service queries local audit_logs table
  → Response
```

---

## 5. Database Schema

### Per-Service audit_logs Table (if not exists)

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
```

---

## 6. Gateway Routes

```yaml
# Health - all services (new parallel endpoint in gateway)
- id: admin-health-all
  uri: http://gateway:8080
  predicates:
    - Path=/api/v1/admin/health/all
  filters:
    - JwtAuth
    - RewritePath=/api/v1/admin/health/all, /admin/health/all

# Metrics - individual service (existing, works via ServiceRoute)
- id: admin-metrics
  uri: http://gateway:8080
  predicates:
    - Path=/api/v1/admin/metrics/{service}
  filters:
    - JwtAuth
    - ServiceRoute

# Audit logs - route to service based on ?service= query param
- id: admin-audit-logs
  uri: http://gateway:8080
  predicates:
    - Path=/api/v1/admin/audit-logs
  filters:
    - JwtAuth
    - ServiceRoute  # Maps ?service=auth → auth-iam-service:8087/internal/audit-logs
```

---

## 7. Benefits

| Metric | Current | New |
|--------|---------|-----|
| Health Dashboard Load Time | 45s+ | <5s |
| Architecture Complexity | 1 extra service + Kafka | No extra services |
| Audit Data Availability | None (Kafka not producing) | Each service writes locally |
| Operational Overhead | High (monitor audit-service, Kafka) | Low (standard service监控) |
| Failure Mode | audit-service down = no health/audit | Gateway only path |

---

## 8. Implementation Tasks

### Phase 1: Health/Metrics (Fast)
1. [ ] Move `HealthAggregationService` logic to gateway (parallel, already done)
2. [ ] Add `/admin/health/all` endpoint in gateway
3. [ ] Update gateway routes to remove audit-service dependency
4. [ ] Test health dashboard loads in <5s

### Phase 2: Audit Logs (Service-Local)
1. [ ] Add audit_logs table to services missing it (ledger, rules, biller, switch, orchestrator)
2. [ ] Add audit logging to each service's domain actions
3. [ ] Create `/internal/audit-logs` endpoints in each service
4. [ ] Update gateway route to proxy audit queries
5. [ ] Test audit logs display in UI

---

## 9. Deprecation Path

1. After migration complete, remove audit-service from docker-compose.yml
2. Remove audit-service from gateway routes
3. Optionally delete audit-service code (keep for reference during transition)