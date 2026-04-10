---
name: e2e-integration-testing
description: Use when testing all external APIs through the API gateway with real JWT tokens, and end to end integration tests covering all BDD scenarios with different user roles
---

# Integration tests for gateway

## Overview

Integration tests use **TestContainers** which automatically manages Docker containers for test dependencies:
- **PostgreSQL**: Started automatically per test class
- **Redis**: Started automatically per test class  
- **Kafka**: Started automatically per test class

The only exception is **Temporal** (workflow engine) which requires manual Docker setup.

## Required Docker Services
Temporal is required for orchestrator-service tests and gateway e2eTest

| Service | Purpose | Port | Reason |
|---------|---------|------|--------|
| Temporal | Workflow engine | 7233 | Not available as TestContainers |

### Starting Temporal for Testing
```bash
# Start Temporal
docker compose up -d temporal temporal-postgres
# or with ui
docker compose up -d temporal temporal-postgres temporal-ui

# Verify Temporal is running
docker ps | grep temporal
```

## TestContainers (Automatic)

Most infrastructure is handled automatically:

```java
// common/src/testFixtures/java/com/agentbanking/common/test/AbstractIntegrationTest.java
static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine");
static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
```

**Note:** TestContainers requires Docker to be running. If tests fail with "Cannot connect to Docker daemon", ensure Docker Desktop is running.

### Test Profile Configuration

Tests use the `tc` (TestContainers) profile:

```yaml
# services/*/src/test/resources/application-tc.yaml
spring:
  main:
    allow-bean-definition-overriding: true
  jpa:
    hibernate:
      ddl-auto: none
  flyway:
    enabled: true
    locations: filesystem:src/main/resources/db/migration
```

## Infra dependencies for integration test

- Default option: user Testcontainers for Postgresql + Redis + Kafka
- Fall back to Postgresql container (i.e in Windows):

**Usage:**
- ./gradlew test → uses tc profile (Testcontainers, default)
- ./gradlew test -PtestProfile=local → uses test profile (localhost Docker Compose)

```bash
# Fallback to localhost Docker Compose
./gradlew test -PtestProfile=local
```
---

## **Integration tests for EXTERNAL APIs**

Test class: ExternalApiIntegrationTest.java

External API Tests:
- balance-inquiry ✓
- agent-balance ✓
- kyc-verify ✓
- kyc-biometric ✓
- ...

Backoffice Tests:
- dashboard ✓
- agents-list ✓
- ledger-transactions ✓
- settlement ✓
- kyc-review-queue ✓
- audit-logs ✓
- backoffice transactions
API Docs Tests:
- openapi-specs ✓
- openapi-specs-trailing-slash ✓

**To run tests:**

```bash
# Run tests
./gradlew :gateway:test --tests "ExternalApiIntegrationTest"
```

## End to end Integration Testing

### Test scopes
- All external APIs in gateway
- Transactions: via orchestrator-service 

### Running Integration Tests

```bash
# Run all integration tests (TestContainers + Temporal must be running)
./gradlew test

# Run specific service tests
./gradlew :services:orchestrator-service:test

# Run orchestrator tests only (requires Temporal)
./gradlew :services:orchestrator-service:test --tests "*Orchestrator*"

# Run tests with coverage
./gradlew test jacocoTestReport

# Run e2e integration tests and automatically runs cleanup first via cleanE2eTestData
# e2e tests required all services running in docker containers
./gradlew :gateway:e2eTest --no-daemon
# can runt only SelfContainedOrchestratorE2ETest to test transaction
```

## Troubleshooting Docker for Tests

```bash
# Check if Docker is running
docker info

# Verify Temporal is running
docker ps | grep temporal

# View Temporal logs
docker logs agentbanking-backend-temporal-1
```

### Clean Up

```bash
# Remove Temporal containers
docker compose down temporal temporal-postgres temporal-ui
```