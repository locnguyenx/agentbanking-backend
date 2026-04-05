# Business Requirements Document: IT Administrator UI

**Date:** 2026-04-05
**Status:** Approved
**Author:** AI Assistant

## 1. Overview

This document defines the business requirements for the IT Administrator UI in the Agent Banking Platform backoffice system. The feature provides system administration capabilities including service health monitoring, performance metrics viewing, and cross-service audit log querying.

**Target Audience:** IT Operations, DevOps, System Administrators

## 2. Business Goals

| ID | Goal | Description |
|----|------|-------------|
| G1 | Real-time Health Visibility | Provide IT Operations with real-time visibility into system health across all microservices, enabling rapid incident detection and response |
| G2 | Performance Monitoring | Enable performance monitoring through direct access to service metrics, supporting capacity planning and troubleshooting |
| G3 | Comprehensive Audit Trail | Maintain an append-only audit trail of all system operations across all microservices for compliance (Bank Malaysia standards) and security investigation. Audit logs are immutable after creation (database-level append-only constraint). |

## 3. User Stories

| ID | Story | Priority |
|----|-------|----------|
| US-1 | As an IT Administrator, I want to see the real-time health status of all services on a single dashboard, so that I can quickly identify and respond to service outages | High |
| US-2 | As an IT Administrator, I want to view detailed performance metrics for any service, so that I can diagnose performance issues and plan capacity | High |
| US-3 | As an IT Administrator, I want to search and filter audit logs across all services, so that I can investigate security incidents and ensure compliance | High |
| US-4 | As an IT Administrator, I want health status to update automatically or on manual refresh, so that I always have current system status | Medium |
| US-5 | As a Platform, I need a dedicated audit-service to aggregate and store audit events from all microservices, so that cross-service audit queries are possible | High |

## 4. Functional Requirements

### FR-1: System Administration Dashboard (US-1, US-4)
| ID | Requirement |
|----|-------------|
| FR-1.1 | Display overview cards showing health status (UP/DOWN/DEGRADED) for all services listed in Service Registry (§6) |
| FR-1.2 | Show quick stats: total services, healthy count, unhealthy count |
| FR-1.3 | Auto-refresh health status every 30 seconds |
| FR-1.4 | Provide manual refresh button |
| FR-1.5 | Color-code service status: green (UP), red (DOWN), amber (DEGRADED) |
| FR-1.6 | Display service metadata: name, port, purpose, last checked timestamp |

### FR-2: Service Metrics Viewer (US-2)
| ID | Requirement |
|----|-------------|
| FR-2.1 | Allow clicking a service card to view detailed metrics |
| FR-2.2 | Display JVM metrics: heap memory, non-heap memory, thread count, CPU usage, uptime |
| FR-2.3 | Display HTTP metrics: request count, error rate, average response time |
| FR-2.4 | Metrics polled via `/actuator/metrics` endpoint through gateway (point-in-time values, not time-series) |
| FR-2.5 | Display current metric values with actuator-provided measurement metadata |

### FR-3: Audit Log Query & Display (US-3)
| ID | Requirement |
|----|-------------|
| FR-3.1 | Display audit logs in a paginated, sortable table |
| FR-3.2 | Show columns: Timestamp, User ID, Action, Resource, IP Address, Result, Service Source, Failure Reason |
| FR-3.3 | Filter by: date range, user ID, action type, service source, result status |
| FR-3.4 | Search by: free-text search across all fields |
| FR-3.5 | Export audit logs to CSV (max 10,000 rows per export) |
| FR-3.6 | Audit logs aggregated via Kafka from all microservices into dedicated audit-service database |

### FR-4: Audit Log Backend Infrastructure (US-5)
| ID | Requirement |
|----|-------------|
| FR-4.1 | Create audit-service microservice with PostgreSQL database |
| FR-4.2 | Implement Kafka consumer to ingest audit events from all services |
| FR-4.3 | Each microservice (auth-iam, onboarding, ledger, rules, switch, biller, orchestrator) publishes audit events to Kafka topic `audit-logs` using shared `AuditEventProducer` from common module. Events are published for all state-changing operations (CREATE, UPDATE, DELETE) on domain entities. |
| FR-4.4 | Implement `GET /api/v1/admin/audit-logs` endpoint with pagination and filtering |
| FR-4.5 | Gateway routes updated to point audit-log endpoint to audit-service |

### FR-5: Gateway Admin Routes
| ID | Requirement |
|----|-------------|
| FR-5.1 | Add gateway routes for health checks: `/api/v1/admin/health/{service}` |
| FR-5.2 | Add gateway routes for metrics: `/api/v1/admin/metrics/{service}` |
| FR-5.3 | Add gateway route for audit logs: `/api/v1/admin/audit-logs` |
| FR-5.4 | All admin routes protected by JWT with IT_ADMIN role requirement |

## 5. Non-Functional Requirements

| ID | Category | Requirement |
|----|----------|-------------|
| NFR-1 | Performance | Health dashboard must load within 2 seconds |
| NFR-2 | Performance | Audit log queries must return within 3 seconds for up to 10,000 records |
| NFR-3 | Security | All admin endpoints require IT_ADMIN role |
| NFR-4 | Security | Audit logs must be immutable (append-only) |
| NFR-5 | Security | No PII in audit log UI display |
| NFR-6 | Scalability | Audit log Kafka topic must support high-throughput ingestion |
| NFR-7 | Reliability | Health check failures must not cascade (circuit breaker pattern) |
| NFR-8 | Reliability | Audit event ingestion must be resilient to Kafka outages |

## 6. Service Registry

### Microservices
| Service | Port | Purpose |
|---------|------|---------|
| API Gateway | 8080 | External API entry point |
| Rules Service | 8081 | Transaction rules engine |
| Ledger Service | 8082 | Financial ledger & float |
| Onboarding Service | 8083 | Agent onboarding & KYC |
| Switch Adapter | 8084 | Payment network adapter |
| Biller Service | 8085 | Bill payment processing |
| Orchestrator Service | 8086 | Transaction Saga coordination |
| Auth/IAM Service | 8087 | Authentication, authorization (audit event publishing only; audit log storage/querying handled by audit-service) |

### Infrastructure Components
| Component | Port | Purpose |
|-----------|------|---------|
| Mock Server | 8089 | Development/testing |
| PostgreSQL | 5432 | Primary database |
| Redis | 6379 | Caching & session |
| Kafka | 9092 | Message broker |

### Kafka Event Schema (audit-logs topic)
| Field | Type | Description |
|-------|------|-------------|
| auditId | UUID | Unique identifier |
| serviceName | String | Source service name (e.g., "auth-iam-service") |
| entityType | String | Type of entity affected (USER, AGENT, TRANSACTION, etc.) |
| entityId | UUID | ID of affected entity |
| action | String | Action performed (from AuditAction enum) |
| performedBy | String | User ID who performed action |
| ipAddress | String | Client IP address |
| timestamp | ISO 8601 | When action occurred |
| outcome | String | SUCCESS or FAILURE |
| failureReason | String (nullable) | Reason if failed |
| changes | JSON string (nullable) | Description of what changed |

The Kafka event schema maps directly to the persisted AuditLog entity with `created_at` added by the audit-service consumer at ingestion time.

### Access Control
All admin endpoints (`/api/v1/admin/*`) require:
1. Valid JWT token (validated by gateway JwtAuth filter)
2. User must have `IT_ADMIN` role (validated by gateway, role claim passed to downstream services via `X-User-Roles` header)
3. Downstream services (audit-service) trust the gateway's `X-User-Roles` header and do not re-validate JWT

## 7. Entity Definitions

### AuditLog (Persisted in audit-service)
| Field | Type | Description |
|-------|------|-------------|
| audit_id | UUID (PK) | Unique identifier |
| service_name | VARCHAR(50) | Source service |
| entity_type | VARCHAR(50) | Type of entity affected |
| entity_id | UUID | ID of affected entity |
| action | VARCHAR(50) | Action performed |
| performed_by | VARCHAR(100) | User ID |
| ip_address | VARCHAR(45) | Client IP |
| timestamp | TIMESTAMP | When action occurred |
| outcome | VARCHAR(20) | SUCCESS or FAILURE |
| failure_reason | VARCHAR(500) | Reason if failed (nullable) |
| changes | TEXT | JSON description of changes (nullable) |
| created_at | TIMESTAMP | When stored in audit-service |

**Recommended Indexes:**
- `idx_audit_logs_timestamp` on `timestamp DESC` (for time-range queries)
- `idx_audit_logs_service` on `service_name` (for service filtering)
- `idx_audit_logs_performed_by` on `performed_by` (for user filtering)
- `idx_audit_logs_outcome` on `outcome` (for result filtering)

## 8. Traceability Matrix

| User Story | Functional Requirements | Business Goal |
|------------|------------------------|---------------|
| US-1 | FR-1.1, FR-1.2, FR-1.5, FR-1.6 | G1 |
| US-2 | FR-2.1, FR-2.2, FR-2.3, FR-2.4, FR-2.5 | G2 |
| US-3 | FR-3.1-FR-3.6 | G3 |
| US-4 | FR-1.3, FR-1.4 | G1 |
| US-5 | FR-4.1-FR-4.5, FR-5.1-FR-5.4 | G3 |

## 9. Constraints & Dependencies

### Constraints
- Must follow hexagonal architecture pattern for all new services
- Must use existing Kafka infrastructure
- Must comply with Bank Malaysia audit trail requirements
- No shared databases between services

### Dependencies
- Existing Spring Boot Actuator endpoints in all services
- Existing Kafka broker (port 9092)
- Gateway routing for admin endpoints
- IT_ADMIN role exists in auth-iam-service
