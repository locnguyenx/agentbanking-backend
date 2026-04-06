# Temporal Integration - Lessons Learned (2026-04-05)

## Overview

This document captures lessons learned while integrating Temporal workflow orchestration into the Agent Banking Platform's Transaction Orchestrator service.

---

## 1. Docker Setup Issues

### 1.1 DB_PORT Environment Variable (CRITICAL)

**Problem:** Temporal auto-setup script defaults `DB_PORT=3306` (MySQL port), but PostgreSQL runs on 5432. The `wait_for_postgres()` function uses `nc -z "${POSTGRES_SEEDS}" "${DB_PORT}"` which fails silently, causing the container to loop indefinitely with "Waiting for PostgreSQL to startup."

**Symptoms:**
- Container logs show endless "Waiting for PostgreSQL to startup." messages
- PostgreSQL is actually running and healthy
- DNS resolution works correctly
- Port 5432 is accessible from inside the container

**Solution:** Always set `DB_PORT=5432` explicitly in docker-compose.yml for PostgreSQL:
```yaml
environment:
  - DB=postgres12
  - DB_PORT=5432  # Critical! Default is 3306 (MySQL)
  - POSTGRES_SEEDS=temporal-postgres
  - POSTGRES_USER=temporal
  - POSTGRES_PWD=temporal
```

**Time Lost:** ~2 hours debugging network issues when the real problem was a wrong port number.

### 1.2 DB Value Must Be "postgres12" Not "postgresql"

**Problem:** Using `DB=postgresql` causes immediate container exit with error:
```
Unsupported driver specified: 'DB=postgresql'. Valid drivers are: mysql8, postgres12, postgres12_pgx, cassandra.
```

**Solution:** Use `DB=postgres12` for PostgreSQL 12+ compatibility.

**Note:** Some online guides use `DB=postgresql` which works with older Temporal versions but fails with `auto-setup:latest` (1.29+).

### 1.3 Healthcheck Must Use Container IP Not localhost

**Problem:** Temporal server binds to the container's IP address (e.g., 192.168.107.14:7233), not localhost. Healthcheck using `temporal operator cluster health --address localhost:7233` fails with "connection refused".

**Symptoms:**
- Container shows as "unhealthy" despite running correctly
- Gateway fails to start due to healthcheck dependency
- `docker ps` shows `(unhealthy)` status

**Solution:** Use network-level check instead of CLI command:
```yaml
healthcheck:
  test: ["CMD-SHELL", "nc -z 0.0.0.0 7233 || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 60s
```

**Why it works:** `nc -z 0.0.0.0 7233` checks if port 7233 is listening on any interface, which matches how Temporal binds.

### 1.4 Image Version Compatibility

**Working configuration:**
```yaml
temporal:
  image: temporalio/auto-setup:latest  # Currently 1.29.x
  # OR use specific version:
  # image: temporalio/auto-setup:1.29.5
```

**Failed versions:**
- `temporalio/auto-setup:1.24` - Stuck in PostgreSQL wait loop (possibly due to config template changes)
- `temporalio/auto-setup:1.25.0` - Same issue

**Note:** The `latest` tag works correctly when combined with the fixes above.

---

## 2. Spring Boot Integration

### 2.1 WorkflowRouter Bean Registration

**Problem:** `WorkflowRouter` class in `domain/service/` is not automatically registered as a Spring bean because domain classes should NOT have `@Service` annotation (per hexagonal architecture rules).

**Error:**
```
No qualifying bean of type 'com.agentbanking.orchestrator.domain.service.WorkflowRouter' available
```

**Solution:** Create explicit bean registration in `DomainServiceConfig.java`:
```java
@Configuration
public class DomainServiceConfig {
    @Bean
    public WorkflowRouter workflowRouter() {
        return new WorkflowRouter();
    }
}
```

**Architecture Note:** This follows the AGENTS.md rule: "EVERY new domain service MUST be registered as a bean" using `@Bean` in config class, not `@Service` annotation on domain classes.

### 2.2 Temporal Auto-Discovery Configuration

**Problem:** Activities and workflows not being discovered by Spring Boot Temporal starter.

**Solution:** Configure both packages in application.yaml:
```yaml
spring:
  temporal:
    workers-auto-discovery:
      packages:
        - com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl
        - com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl
```

**Important:** Must include BOTH packages - one for activities, one for workflows.

### 2.3 Spring Boot Temporal Starter Dependency

**Required dependency:**
```gradle
implementation 'io.temporal:temporal-spring-boot-starter:1.25.1'
```

**Note:** The Spring Boot starter version (1.25.1) can differ from the server image version (1.29.x). This is acceptable as the client and server have separate versioning.

### 2.4 Activity and Workflow Annotations

**Required annotations for auto-discovery:**
- Activities: `@ActivityImpl` (from `io.temporal.spring.boot.ActivityImpl`)
- Workflows: `@WorkflowImpl` (from `io.temporal.spring.boot.WorkflowImpl`)

**Example:**
```java
@ActivityImpl
public class ValidateAgentActivityImpl implements ValidateAgentActivity {
    // implementation
}

@WorkflowImpl(taskQueues = "agent-banking-tasks")
public class WithdrawalWorkflowImpl implements WithdrawalWorkflow {
    // implementation
}
```

---

## 3. Docker Networking

### 3.1 Container Communication

**Problem:** Orchestrator service cannot connect to Temporal via `temporal:7233` hostname.

**Root Cause:** Services started separately may not be on the same Docker network.

**Solution:** Use `docker-compose up --profile all` to start all backend services together, ensuring they share the same default network.

**Verification:**
```bash
docker network inspect agentbanking-backend_default | grep -A3 "temporal"
docker network inspect agentbanking-backend_default | grep -A3 "orchestrator"
```

Both should show entries in the same network.

### 3.2 DNS Resolution

Docker's embedded DNS server (127.0.0.11) resolves service names correctly when containers are on the same network:
```bash
docker run --rm --network agentbanking-backend_default alpine sh -c "nslookup temporal"
```

---

## 4. API Testing

### 4.1 Correct API Format

**Endpoint:** `POST /api/v1/transactions`

**Request:**
```json
{
  "transactionType": "CASH_WITHDRAWAL",
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 100.00,
  "idempotencyKey": "TEST-TXN-001"
}
```

**Valid Transaction Types:**
- `CASH_WITHDRAWAL`
- `CASH_DEPOSIT`
- `BILL_PAYMENT`
- `DUITNOW_TRANSFER`

**Response:**
```json
{
  "status": "PENDING",
  "workflowId": "TEST-TXN-001",
  "pollUrl": "/api/v1/transactions/TEST-TXN-001/status"
}
```

**Status Check:** `GET /api/v1/transactions/{workflowId}/status`

### 4.2 Common Mistakes

1. **Wrong field names:** Use `transactionType` not `type`, `agentId` (UUID) not `agentId` (string)
2. **Missing required fields:** `transactionType`, `agentId`, `amount` are mandatory
3. **Testing wrong port:** Orchestrator runs on 8086, Gateway on different port
4. **Wrong HTTP method:** POST for creating transactions, GET for status

---

## 5. Working Configuration Reference

### docker-compose.yml (Temporal section)
```yaml
temporal:
  image: temporalio/auto-setup:latest
  profiles: [backend, all]
  ports:
    - "7233:7233"
  environment:
    - DB=postgres12
    - DB_PORT=5432
    - POSTGRES_SEEDS=temporal-postgres
    - POSTGRES_USER=temporal
    - POSTGRES_PWD=temporal
  depends_on:
    temporal-postgres:
      condition: service_healthy
  healthcheck:
    test: ["CMD-SHELL", "nc -z 0.0.0.0 7233 || exit 1"]
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 60s

temporal-postgres:
  image: postgres:16
  environment:
    - POSTGRES_USER=temporal
    - POSTGRES_PASSWORD=temporal
    - POSTGRES_DB=temporal
  volumes:
    - temporal-postgres-data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U temporal"]
    interval: 5s
    timeout: 5s
    retries: 5

temporal-ui:
  image: temporalio/ui:latest
  ports:
    - "8082:8080"
  environment:
    - TEMPORAL_ADDRESS=temporal:7233
  depends_on:
    - temporal
```

### application.yaml (Spring Boot Temporal config)
```yaml
spring:
  temporal:
    connection:
      target: ${TEMPORAL_ADDRESS:localhost:7233}
    namespace: default
    workers-auto-discovery:
      packages:
        - com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl
        - com.agentbanking.orchestrator.infrastructure.temporal.WorkflowImpl
```

---

## 6. Debugging Commands

### Check Temporal container logs
```bash
docker logs agentbanking-backend-temporal-1 --tail 50
```

### Test Temporal connectivity from another container
```bash
docker run --rm --network agentbanking-backend_default alpine sh -c "nc -zv temporal 7233"
```

### Check Temporal workflows via UI API
```bash
curl http://localhost:8082/api/v1/namespaces/default/workflows
```

### Check container health
```bash
docker ps --format "table {{.Names}}\t{{.Status}}" | grep temporal
```

### Inspect container network
```bash
docker network inspect agentbanking-backend_default
```

---

## 7. Key Takeaways

1. **Always check default values** - The `DB_PORT=3306` default was the root cause of hours of debugging
2. **Read the actual script** - Looking at `auto-setup.sh` source revealed the `wait_for_postgres()` function and its port usage
3. **Healthcheck matters** - An unhealthy container breaks dependent services even if the service is actually running
4. **Version compatibility** - Older Temporal Docker images may have different configuration requirements
5. **Spring Boot auto-discovery** - Requires explicit package configuration and correct annotations
6. **Test connectivity properly** - Use `nc` or similar tools to verify network connectivity between containers

---

## 8. References

- [Temporal Self-Hosted Deployment Guide](https://docs.temporal.io/self-hosted-guide/deployment)
- [Temporal Spring Boot Starter](https://github.com/temporalio/sdk-java/tree/master/temporal-spring-boot-starter)
- [OneUptime: How to Run Temporal in Docker](https://oneuptime.com/blog/post/2026-02-08-how-to-run-temporal-in-docker-for-workflow-orchestration/view)
- [Temporal samples-server repository](https://github.com/temporalio/samples-server)
