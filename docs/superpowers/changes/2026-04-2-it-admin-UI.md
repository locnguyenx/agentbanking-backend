# IT Administrator Functions

## Overview

This is requirements for IT Administrator UI in back office system, to covers system administration tasks for the Agent Banking Platform, including service management, monitoring, security configuration, and troubleshooting.

**Target Audience:** IT Operations, DevOps, System Administrators

---

## Requirements

### Add a main menu for System Administrators
Menu name: `System Administration`

Following requirements will be added under thie menu `System Administration`

### Service Health Checks
Show the status of all services. Should be showed in realtime, or manually refresh by Clicking Refresh button to retrieve latest status.

| Service | Health Endpoint |
|---------|-----------------|
| Gateway | `GET /actuator/health` |
| Rules | `GET /actuator/health` |
| Ledger | `GET /actuator/health` |
| Onboarding | `GET /actuator/health` |
| Switch | `GET /actuator/health` |
| Biller | `GET /actuator/health` |

**Services Info**

| Service | Port | Purpose |
|---------|------|---------|
| API Gateway | 8080 | External API entry point |
| Rules Service | 8081 | Transaction rules engine |
| Ledger Service | 8082 | Financial ledger & float |
| Onboarding Service | 8083 | Agent onboarding & KYC |
| Switch Adapter | 8084 | Payment network adapter |
| Biller Service | 8085 | Bill payment processing |
| Mock Server | 8089 | Development/testing |
| PostgreSQL | 5432 | Primary database |
| Redis | 6379 | Caching & session |
| Kafka | 9092 | Message broker |

---

### Monitoring Metrics

For each service, allow administrator to view realtime system metrics.

At this moment, administrator use api to view the metrics like below: All metrics, HTTP request metrics, Prometheus format

please consult the best practices to view metrics via UI.

### Audit Logs

This feature is to view Audit Logs by Querying audit table in db

Show log details for each operation:
- Timestamp (ISO 8601)
- User ID
- Action performed
- Resource accessed
- IP address
- Result (success/failure)
- ...
- 
